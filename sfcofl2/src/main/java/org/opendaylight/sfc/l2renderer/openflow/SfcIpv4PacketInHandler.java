/*
 * Copyright (c) 2015 Ericsson, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer.openflow;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Arrays;

import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PacketIn rule will be triggered by the TransportEgress table
 * when a TCP Proxy SF is being used.
 * This class listens for IPv4 packets and will populate the PathMapperAcl
 * table with 2 rules using the Packet's source/dest IP addresses:
 *   Rule 1: if(IpSrc == PacketInIpSrc AND IpDst == PacketInIpDst)
 *           then (set metadata to uplink RSP pathId and goto TransportEgress table)
 *   Rule 2: if(IpSrc == PacketInIpDst AND IpDst == PacketInIpSrc)
 *           then (set metadata to downlink RSP pathId and goto TransportEgress table)
 *
 * Since a TCP Proxy SF will generate packets, the SFF wont know what to do with them
 * unless we add the above rules. Upon receiving a TCP Syn from the client, the SF will
 * establish a connection with the client (send TCP SynAck to client), and then establish
 * a separate connection with the server (send TCP Syn to server).
 */

public class SfcIpv4PacketInHandler implements PacketProcessingListener, AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(SfcIpv4PacketInHandler.class);
    // TODO should this instead be the SfcL2FlowProgrammerInterface
    private SfcL2FlowProgrammerOFimpl flowProgrammer;
    private final static int PACKET_OFFSET_ETHERTYPE = 12;
    private final static int PACKET_OFFSET_IP = 14;
    private final static int PACKET_OFFSET_IP_SRC = PACKET_OFFSET_IP+12;
    private final static int PACKET_OFFSET_IP_DST = PACKET_OFFSET_IP+16;
    public static final int ETHERTYPE_IPV4 = 0x0800;

    public SfcIpv4PacketInHandler(SfcL2FlowProgrammerOFimpl flowProgrammer) {
        this.flowProgrammer = flowProgrammer;
    }

    /**
     * The handler function for IPv4 packets.
     *
     * @param packetIn The incoming packet.
     */
    @Override
    public void onPacketReceived(PacketReceived packetIn) {
        if(packetIn == null) {
            return;
        }

        // Make sure the PacketIn is due to our Classification table pktInAction
        if(!this.flowProgrammer.compareClassificationTableCookie(packetIn.getFlowCookie())) {
            LOG.debug("SfcIpv4PacketInHandler discarding packet by Flow Cookie");
            return;
        }

        // TODO figure out how to get the IDataPacketService which will parse the packet for us

        final byte[] rawPacket = packetIn.getPayload();

        // Get the EtherType and check that its an IP packet
        if(getEtherType(rawPacket) != ETHERTYPE_IPV4) {
            LOG.debug("SfcIpv4PacketInHandler discarding NON-IPv4");
            return;
        }

        // Get the SrcIp and DstIp Addresses
        String pktSrcIpStr = getSrcIpStr(rawPacket);
        if(pktSrcIpStr == null) {
            LOG.error("SfcIpv4PacketInHandler Cant get Src IP address, discarding packet");
            return;
        }

        String pktDstIpStr = getDstIpStr(rawPacket);
        if(pktDstIpStr == null) {
            LOG.error("SfcIpv4PacketInHandler Cant get Src IP address, discarding packet");
            return;
        }

        // Get the metadata
        if(packetIn.getMatch() == null) {
            LOG.error("SfcIpv4PacketInHandler Cant get packet flow match");
            return;
        }
        if(packetIn.getMatch().getMetadata() == null) {
            LOG.error("SfcIpv4PacketInHandler Cant get packet flow match metadata");
            return;
        }

        Metadata pktMatchMetadata = packetIn.getMatch().getMetadata();
        BigInteger metadata = pktMatchMetadata.getMetadata();

        short ulPathId = metadata.shortValue();
        // TODO Assuming the RSP is symmetric
        short dlPathId = (short) (ulPathId + 1);

        LOG.info("SfcIpv4PacketInHandler Src IP [{}] Dst IP [{}] ulPathId [{}] dlPathId [{}]",
                pktSrcIpStr, pktDstIpStr, ulPathId, dlPathId);

        // Get the Node name, by getting the following
        // - Ingress nodeConnectorRef
        // - instanceID for the Node in the tree above us
        // - instance identifier for the nodeConnectorRef
        final String nodeName =
                packetIn.getIngress()
                .getValue()
                .firstKeyOf(Node.class, NodeKey.class)
                .getId().getValue();

        // Configure the uplink packet
        if(ulPathId >= 0) {
            this.flowProgrammer.setFlowRspId(new Long(ulPathId));
            this.flowProgrammer.configurePathMapperAclFlow(nodeName, pktSrcIpStr, pktDstIpStr, ulPathId);
        }

        // Configure the downlink packet
        if(dlPathId >= 0) {
            this.flowProgrammer.setFlowRspId(new Long(dlPathId));
            this.flowProgrammer.configurePathMapperAclFlow(nodeName, pktDstIpStr, pktSrcIpStr, dlPathId);
        }
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    private short getEtherType(final byte[] rawPacket) {
        final byte[] etherTypeBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_ETHERTYPE, PACKET_OFFSET_ETHERTYPE+2);
        return packShort(etherTypeBytes);
    }

    private String getSrcIpStr(final byte[] rawPacket) {
        final byte[] ipSrcBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_SRC, PACKET_OFFSET_IP_SRC+4);
        String pktSrcIpStr = null;
        try {
            pktSrcIpStr = InetAddress.getByAddress(ipSrcBytes).getHostAddress();
        } catch(Exception e) {
            LOG.error("Exception getting Src IP address [{}]", e.getMessage(), e);
        }

        return pktSrcIpStr;
    }

    private String getDstIpStr(final byte[] rawPacket) {
        final byte[] ipDstBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_IP_DST, PACKET_OFFSET_IP_DST+4);
        String pktDstIpStr = null;
        try {
            pktDstIpStr = InetAddress.getByAddress(ipDstBytes).getHostAddress();
        } catch(Exception e) {
            LOG.error("Exception getting Dst IP address [{}]", e.getMessage(), e);
        }

        return pktDstIpStr;
    }

    // Simple internal utility function to convert from a 2-byte array to a short
    private short packShort(byte[] bytes) {
        short val = (short) 0;
        for (int i = 0; i < 2; i++) {
          val <<= 8;
          val |= bytes[i] & 0xff;
        }

        return val;
    }
}

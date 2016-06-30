/*
 * Copyright (c) 2015 Ericsson, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
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
    private final static int PACKET_OFFSET_ETHERTYPE = 12;
    private final static int PACKET_OFFSET_IP = 14;
    private final static int PACKET_OFFSET_IP_SRC = PACKET_OFFSET_IP+12;
    private final static int PACKET_OFFSET_IP_DST = PACKET_OFFSET_IP+16;
    public  final static int ETHERTYPE_IPV4 = 0x0800;
    private final static int DEFAULT_MAX_BUFFER_TIME = 60000; // 60 milliseconds
    private final static int DEFAULT_PACKET_COUNT_PURGE = 100;

    private SfcOfFlowProgrammerImpl flowProgrammer;
    private Map<String, Long> pktInBuffer;
    private int maxBufferTime;
    private int packetCountPurge;
    private int packetCount;

    public SfcIpv4PacketInHandler(SfcOfFlowProgrammerImpl flowProgrammer) {
        this.flowProgrammer = flowProgrammer;
        pktInBuffer = new HashMap<String, Long>();
        maxBufferTime = DEFAULT_MAX_BUFFER_TIME;
        packetCountPurge = DEFAULT_PACKET_COUNT_PURGE;
        packetCount = 0;
    }

    public int getMaxBufferTime() {
        return maxBufferTime;
    }

    public void setMaxBufferTime(int maxBufferTime) {
        this.maxBufferTime = maxBufferTime;
    }

    public int getPacketCountPurge() {
        return packetCountPurge;
    }

    public void setPacketCountPurge(int packetCountPurge) {
        this.packetCountPurge = packetCountPurge;
    }

    public int getBufferSize() {
        return pktInBuffer.size();
    }

    /**
     * The handler function for IPv4 PktIn packets.
     *
     * @param packetIn The incoming packet.
     */
    @Override
    public void onPacketReceived(PacketReceived packetIn) {
        if(packetIn == null) {
            return;
        }

        ++packetCount;
        if(packetCount > packetCountPurge) {
            packetCount = 0;
            purgePktInBuffer();
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

        // Since all packets sent to SF are PktIn, only need to handle the first one
        // In OpenFlow 1.5 we'll be able to do the PktIn on TCP Syn only
        if(bufferPktIn(pktSrcIpStr, pktDstIpStr)) {
            LOG.info("SfcIpv4PacketInHandler PacketIn buffered");
            return;
        }
        LOG.info("SfcIpv4PacketInHandler PacketIn NOT buffered");

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
        // Assuming the RSP is symmetric
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
    }

    /**
     * Given a raw packet, return the EtherType
     *
     * @param rawPacket
     * @return etherType
     */
    private short getEtherType(final byte[] rawPacket) {
        final byte[] etherTypeBytes = Arrays.copyOfRange(rawPacket, PACKET_OFFSET_ETHERTYPE, PACKET_OFFSET_ETHERTYPE+2);
        return packShort(etherTypeBytes);
    }

    /**
     * Given a raw packet, return the SrcIp
     *
     * @param rawPacket
     * @return srcIp String
     */
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

    /**
     * Given a raw packet, return the DstIp
     *
     * @param rawPacket
     * @return dstIp String
     */
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

    /**
     * Simple internal utility function to convert from a 2-byte array to a short
     *
     * @param bytes
     * @return the bytes packed into a short
     */
    private short packShort(byte[] bytes) {
        short val = (short) 0;
        for (int i = 0; i < 2; i++) {
          val <<= 8;
          val |= bytes[i] & 0xff;
        }

        return val;
    }

    /**
     * Decide if packets with the same src/dst IP have already been processed.
     * If they havent been processed, store the IPs so they will be considered processed.
     *
     * @param srcIpStr
     * @param dstIpStr
     * @return True if the src/dst IP has already been processed, False otherwise
     */
    private boolean bufferPktIn(final String srcIpStr, final String dstIpStr) {
        String key = new StringBuilder().append(srcIpStr).append(dstIpStr).toString();
        long currentMillis = System.currentTimeMillis();

        Long bufferedTime = pktInBuffer.get(key);

        // If the entry does not exist, add it and return false indicating the packet needs to be processed
        if(bufferedTime == null) {
            // Add the entry
            pktInBuffer.put(key, new Long(currentMillis));
            return false;
        }

        // If the entry is old, update it and return false indicating the packet needs to be processed
        if((currentMillis - bufferedTime.longValue()) > maxBufferTime) {
            // Update the entry
            pktInBuffer.put(key, new Long(currentMillis));
            return false;
        }

        return true;
    }

    /**
     * Purge packets that have been in the PktIn buffer too long.
     */
    private void purgePktInBuffer() {
        long currentMillis = System.currentTimeMillis();
        Set<String> keySet = pktInBuffer.keySet();
        for(String key : keySet) {
            Long bufferedTime = pktInBuffer.get(key);
            if((currentMillis - bufferedTime.longValue()) > maxBufferTime) {
                // This also removes the entry from the pktInBuffer map and doesnt invalidate iteration
                keySet.remove(key);
            }
        }
    }
}

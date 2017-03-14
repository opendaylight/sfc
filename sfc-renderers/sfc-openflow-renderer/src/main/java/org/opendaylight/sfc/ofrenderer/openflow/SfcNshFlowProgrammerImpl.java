/*
 * Copyright (c) 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;

import com.google.common.net.InetAddresses;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/*
 * This class writes Openflow Flow Entries to the SFF once an SFF has been configured.
 *
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 *
 * @version 0.1
 *
 * @since 2014-08-07
 */
public class SfcNshFlowProgrammerImpl extends SfcFlowProgrammerBase {

    protected static final String VXGPE_SF_WORKAROUND_FLOW = "vxgpe_sf_workaround_Flow";

    public SfcNshFlowProgrammerImpl(SfcOfFlowWriterInterface sfcOfFlowWriter, SfcOpenFlowConfig openFlowConfig) {
        super(sfcOfFlowWriter, openFlowConfig);
    }

    /**
     * SF Workaround flow for OPNFV to send packets to SF.
     *
     * @param sffNodeName - where to write the flows
     * @param sfIp - the IP of the SF egressing packets to
     * @param vxlanUdpPort - the Vxgpe tunnel UPD port
     * @param sffPort - the switch port where the SF is connected
     */
    public void configureNshVxgpeSfLoopbackEncapsulatedEgressFlow(final String sffNodeName, final String sfIp,
            final short vxlanUdpPort, final long sffPort) {

        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchIpProtocol(match, SfcOpenflowUtils.IP_PROTOCOL_UDP);
        SfcOpenflowUtils.addMatchDstIpv4(match, sfIp, 32);
        SfcOpenflowUtils.addMatchDstUdpPort(match, vxlanUdpPort);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionOutPort((int) sffPort, 0));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        FlowBuilder sfFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                FLOW_PRIORITY_ARP_TRANSPORT_INGRESS, VXGPE_SF_WORKAROUND_FLOW, match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, sfFlow);
    }

    /**
     * SF Workaround flow for OPNFV for receiving packets from SF.
     *
     * @param sffNodeName - where to write the flows
     * @param vxlanUdpPort - the Vxgpe tunnel UPD port
     * @param sffPort - the switch port where the SF is connected
     */
    public void configureNshVxgpeSfReturnLoopbackIngressFlow(final String sffNodeName, final short vxlanUdpPort,
            final long sffPort) {
        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchIpProtocol(match, SfcOpenflowUtils.IP_PROTOCOL_UDP);
        SfcOpenflowUtils.addMatchDstUdpPort(match, vxlanUdpPort);
        SfcOpenflowUtils.addMatchInPort(match, new NodeId(sffNodeName), sffPort);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.LOCAL.toString(), 0));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        FlowBuilder sfFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                FLOW_PRIORITY_ARP_TRANSPORT_INGRESS, VXGPE_SF_WORKAROUND_FLOW, match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, sfFlow);
    }


    //
    // Configure Table 1, Transport Ingress
    //

    /**
     * Configure a NshVxgpe Transport Ingress flow, by matching on EtherType
     * IPv4.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    public void configureNshVxgpeTransportIngressFlow(final String sffNodeName, final long nshNsp, final short nshNsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp);

        FlowBuilder transportIngressFlow = configureTransportIngressFlow(match, getTableId(TABLE_INDEX_NEXT_HOP));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    //
    // Table 4, NextHop
    //

    /**
     * Configure the NshVxgpe NSH Next Hop by matching on the NSH pathId and
     * index stored in the NSH header.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param dstIp
     *            - the VxLan GPE tunnel destination IP
     * @param nshNsp
     *            - NSH Service Path to match on
     * @param nshNsi
     *            - NSH Index to match on
     */
    public void configureNshVxgpeNextHopFlow(final String sffNodeName, final String dstIp, final String nshProxyIp,
            final long nshNsp,
            final short nshNsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        List<Action> actionList = new ArrayList<>();
        if (nshProxyIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(nshProxyIp, actionList.size());
            actionList.add(actionSetNwDst);
            if (dstIp != null) {
                int ip = InetAddresses.coerceToInteger(InetAddresses.forString(dstIp));
                long ipl = ip & 0xffffffffL;
                Action actionSetNshC3 = SfcOpenflowUtils.createActionNxLoadNshc3(ipl, actionList.size());
                actionList.add(actionSetNshC3);
            }
        } else if (dstIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, actionList.size());
            actionList.add(actionSetNwDst);
        }

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, FLOW_PRIORITY_NEXT_HOP);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    /**
     * Configure the NshEth Next Hop by matching on the NSH pathId and index
     * stored in the NSH header.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source Mac
     * @param dstMac
     *            - the destination Mac
     * @param nsp
     *            - NSH Service Path to match on
     * @param nsi
     *            - NSH Index to match on
     */
    public void configureNshEthNextHopFlow(String sffNodeName, String srcMac, String dstMac, long nsp, short nsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nsp, nsi);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxLoadEncapEthSrc(srcMac, actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxLoadEncapEthDst(dstMac, actionList.size()));

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, FLOW_PRIORITY_NEXT_HOP);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    //
    // Table 10, Transport Egress
    //

    /**
     * Configure the VxLAN GPE NSH Transport Egress flow by matching on the NSP
     * and NSI.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param port
     *            - the switch port to send the packet out on
     */
    public void configureNshVxgpeLastHopTransportEgressFlow(final String sffNodeName, final long nshNsp,
            final short nshNsi, String port) {
        // When outputing to an outport, if inport==outport, then according to
        // the openflow spec, the packet will be dropped. To avoid this, outport
        // must be set to INPORT. This method writes 2 flows to avoid this
        // situation:
        // flow1:
        // if inport==port, actions=output:INPORT (higher priority than flow2)
        // flow2:
        // actions=output:port (flow2 is basically the else condition)

        Long vxgpePort = SfcOvsUtil.getVxlanGpeOfPort(sffNodeName);
        String inportStr = OutputPortValues.INPORT.toString();

        if (vxgpePort != null) {
            String vxgpePortStr = "output:" + vxgpePort.toString();
            configureNshVxgpeLastHopTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, vxgpePortStr, inportStr);
            configureNshVxgpeLastHopTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, inportStr, vxgpePortStr);
        } else {
            configureNshVxgpeLastHopTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, port, port);
        }
    }

    /**
     * Simple call through for configureNshVxgpeLastHopTransportEgressFlow().
     */
    private void configureNshVxgpeLastHopTransportEgressFlowPorts(final String sffNodeName, final long nshNsp,
            final short nshNsi, String inport, String outport) {
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        String theOutPortToSet = outport;
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        if (!inport.startsWith(OutputPortValues.INPORT.toString())) {
            // if we output to a port that's the same as the import, the packet
            // will be dropped
            SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(inport));
            theOutPortToSet = OutputPortValues.INPORT.toString();
            flowPriority += 5;
        }

        // On the last hop Copy/Move Nsi, Nsp, Nsc1=>TunIpv4Dst, and
        // Nsc2=>TunId(Vnid)
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(actionList.size()));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, theOutPortToSet, flowPriority,
                TRANSPORT_EGRESS_NSH_VXGPE_LASTHOP_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the last hop VxLAN GPE NSH Transport Egress flow by matching on
     * the NSP and NSI.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param macAddress
     *            - mac address to set as source mac address after removing
     *            Eth-NSH encapsulation
     */
    public void configureNshEthLastHopTransportEgressFlow(final String sffNodeName, final long nshNsp,
            final short nshNsi, MacAddress macAddress) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        // On the last hop:
        // 1. remove nsh header
        // 2. Change src mac to the mac of the last SF
        // 3. resubmit to the dispatcher table
        List<Action> actionList = new ArrayList<>();

        // Pop NSH
        Action popNsh = SfcOpenflowUtils.createActionNxPopNsh(actionList.size());
        actionList.add(popNsh);

        // Change source address
        Action changeSourceMac = SfcOpenflowUtils.createActionSetDlSrc(macAddress.getValue(), actionList.size());
        actionList.add(changeSourceMac);

        // Proceed with other services
        actionList
                .add(SfcOpenflowUtils.createActionResubmitTable(NwConstants.LPORT_DISPATCHER_TABLE, actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Make the cookie
        BigInteger cookie = new BigInteger(TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                FLOW_PRIORITY_TRANSPORT_EGRESS, cookie, "last hop egress flow", match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, fb);
    }

    public void configureNshVxgpeTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port) {

        // When outputing to an outport, if inport==outport, then according to
        // the openflow spec, the packet will be dropped. To avoid this, outport
        // must be set to INPORT. This method writes 2 flows to avoid this
        // situation:
        // flow1:
        // if inport==port, actions=output:INPORT (higher priority than flow2)
        // flow2:
        // actions=output:port (flow2 is basically the else condition)

        Long vxgpePort = SfcOvsUtil.getVxlanGpeOfPort(sffNodeName);
        String inportStr = OutputPortValues.INPORT.toString();

        if (vxgpePort != null) {
            String vxgpePortStr = "output:" + vxgpePort.toString();
            configureNshVxgpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, vxgpePortStr, inportStr);
            configureNshVxgpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, inportStr, vxgpePortStr);
        } else {
            configureNshVxgpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, port, port);
        }
    }

    /**
     * Simple call-through for configureNshVxgpeTransportEgressFlow().
     */
    private void configureNshVxgpeTransportEgressFlowPorts(final String sffNodeName, final long nshNsp,
            final short nshNsi, String inport, String outport) {
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        String theOutPortToSet = outport;

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        if (!inport.startsWith(OutputPortValues.INPORT.toString())) {
            // if we output to a port that's the same as the import, the packet
            // will be dropped
            SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(inport));
            theOutPortToSet = OutputPortValues.INPORT.toString();
            flowPriority += 5;
        }

        List<Action> actionList = new ArrayList<>();
        // Copy/Move Nsc1/Nsc2 to the next hop
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc3(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc4(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveTunIdRegister(actionList.size()));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, theOutPortToSet, flowPriority,
                TRANSPORT_EGRESS_NSH_VXGPE_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * For NSH, Return the packet to INPORT if the NSH Nsc1 Register is not
     * present (==0) If it is present, it will be handled by the flow created in
     * ConfigureTransportEgressFlowThread() This flow will have a higher
     * priority than the flow created in ConfigureTransportEgressFlowThread().
     * The NSH Nsc1 Register is usually set by the ingress classifier to
     * communicate how to reach the egress classifier.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param port
     *            - the switch port to send the packet out on
     */
    public void configureNshNscTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port) {
        LOG.debug("ConfigureNshNscTransportEgressFlowThread, sff [{}] nsp [{}] nsi [{}] port [{}]", sffNodeName, nshNsp,
                nshNsi, port);

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, 0L);

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        List<Action> actionList = new ArrayList<>();
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, port,
                FLOW_PRIORITY_TRANSPORT_EGRESS + 10, TRANSPORT_EGRESS_NSH_VXGPE_NSC_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    public void configureNshVxgpeAppCoexistTransportEgressFlow(final String sffNodeName, final long nshNsp,
            final short nshNsi, final String sffIp) {

        // This flow only needs to be created if App Coexistence is being used
        if (getTableEgress() == APP_COEXISTENCE_NOT_SET) {
            LOG.debug("configureNshVxgpeAppCoexistTransportEgressFlow NO AppCoexistence configured, skipping flow");
            return;
        }

        // Create a match checking if C1 is set to this SFF
        // Assuming IPv4
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(sffIp));
        long ipl = ip & 0xffffffffL;

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, ipl);

        // Copy/Move Nsi, Nsp, Nsc1=>TunIpv4Dst, and Nsc2=>TunId(Vnid)
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(actionList.size()));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, EMPTY_SWITCH_PORT,
                FLOW_PRIORITY_TRANSPORT_EGRESS + 10, TRANSPORT_EGRESS_NSH_VXGPE_APPCOEXIST_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the NSH Ethernet Transport Egress flow by matching on the NSP
     * and NSI.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param port
     *            - the switch port to send the packet out on
     */
    public void configureNshEthTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi, String port) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        List<Action> actionList = new ArrayList<>();
        // Copy/Move Nsc1/Nsc2/Nsi/Nsp to the next hop
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));

        // Dont need to set Ethernet EtherType

        // Set NSH NextProtocol to Ethernet
        actionList.add(SfcOpenflowUtils.createActionNxLoadNshNp(OpenflowConstants.NSH_NP_ETH, actionList.size()));

        // Ethernet encap is performed in configureNshEthNextHopFlow()
        // while setting the next hop outer MAC addresses

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, port,
                FLOW_PRIORITY_TRANSPORT_EGRESS, TRANSPORT_EGRESS_NSH_ETH_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    public void configureNshEthTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi,
            List<Action> actionList) {

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Make the cookie
        BigInteger cookie = new BigInteger(TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LOGICAL_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);

        FlowBuilder transportEgressFlowBuilder = SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_TRANSPORT_EGRESS), FLOW_PRIORITY_TRANSPORT_EGRESS, cookie,
                "default egress flow", match, isb);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlowBuilder);
    }

}

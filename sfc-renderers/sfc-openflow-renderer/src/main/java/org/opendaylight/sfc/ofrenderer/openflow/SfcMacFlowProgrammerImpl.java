/*
 * Copyright (c) 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

/*
 * This class contains common implementations for MPLS and VLAN transports.
 *
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 *
 * @version 0.1
 *
 * @since 2014-08-07
 */
public class SfcMacFlowProgrammerImpl extends SfcFlowProgrammerBase {

    protected static final int PKTIN_IDLE_TIMEOUT = 60;
    protected static final String INGRESS_TRANSPORT_ARP_FLOW_NAME_LITERAL = "ingress_Transport_Arp_Flow";

    public SfcMacFlowProgrammerImpl(SfcOfFlowWriterInterface sfcOfFlowWriter, SfcOpenFlowConfig openFlowConfig) {
        super(sfcOfFlowWriter, openFlowConfig);
    }

    /**
     * Create an ARP responder flow in the Transport Ingress table. This flow is
     * intended to respond to SF ARP messages, and is only created for SFs of
     * type TCP-Proxy.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param mac
     *            - the SFF mac
     */
    public void configureArpTransportIngressFlow(final String sffNodeName, final String mac) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportArpIngressThread, sff [{}] mac [{}]", sffNodeName, mac);

        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_ARP);
        SfcOpenflowUtils.addMatchArpRequest(match);

        int order = 0;
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveEthSrcToEthDstAction(order++));
        actionList.add(SfcOpenflowUtils.createActionSetDlSrc(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpOpAction(SfcOpenflowUtils.ARP_REPLY, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpShaAction(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpShaToArpThaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpTpaToRegAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpSpaToArpTpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveRegToArpSpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++));

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        FlowBuilder arpTransportIngressFlow = SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_TRANSPORT_INGRESS), FLOW_PRIORITY_ARP_TRANSPORT_INGRESS,
                INGRESS_TRANSPORT_ARP_FLOW_NAME_LITERAL, match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, arpTransportIngressFlow);
    }

    //
    // Configure Table 2, PathMapper
    //

    /**
     * Simple pass through for SF Path Mapper flows.
     *
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    protected FlowBuilder configurePathMapperSfFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        SfcOpenflowUtils.addMatchDscp(match, (short) pathId);
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER + 10);
    }

    /**
     * Simple pass-through with default arg for flowPriority.
     *
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    protected FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER);
    }

    /**
     * Simple pass-through method used by the
     * configurePathMapperFlow() methods.
     *
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    protected FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList,
            int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.configurePathMapperFlow sff [{}] pathId [{}]", pathId);

        // Apply actions take actions instantly - so, does not matter which
        // order they take - let's assume first
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);
        SfcOpenflowUtils.appendMetadataInstruction(isb, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        SfcOpenflowUtils.appendGotoTableInstruction(isb, getTableId(TABLE_INDEX_NEXT_HOP));

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_PATH_MAPPER), flowPriority,
                NEXT_HOP_FLOW_NAME_LITERAL, match, isb);
    }

    //
    // Table 3, PathMapper ACL
    //

    /**
     * This table is populated as a result of PktIn for TCP Proxy SFs. It
     * matches on Src/Dst IP and writes the path ID to the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param pktSrcIpStr
     *            - Src IP to match on
     * @param pktDstIpStr
     *            - Src IP to match on
     * @param pathId
     *            - the RSP ID to write to the metadata
     */
    public void configurePathMapperAclFlow(final String sffNodeName, final String pktSrcIpStr, final String pktDstIpStr,
            short pathId) {
        LOG.debug("SfcProviderSffFlowWriter.configurePathMapperAclFlow sff [{}] srcIp [{}] dstIp [{}] pathId [{}]",
                sffNodeName, pktSrcIpStr, pktDstIpStr, pathId);

        // Match on the Src and Dst IPs
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchSrcIpv4(match, pktSrcIpStr, 32);
        SfcOpenflowUtils.addMatchDstIpv4(match, pktDstIpStr, 32);

        InstructionsBuilder isb = SfcOpenflowUtils.appendMetadataInstruction(new InstructionsBuilder(),
                getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        SfcOpenflowUtils.appendGotoTableInstruction(isb, getTableId(TABLE_INDEX_NEXT_HOP));

        // Create and configure the FlowBuilder
        FlowBuilder ingressFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                FLOW_PRIORITY_PATH_MAPPER_ACL, NEXT_HOP_FLOW_NAME_LITERAL, match, isb);
        // Set an idle timeout on this flow
        ingressFlow.setIdleTimeout(PKTIN_IDLE_TIMEOUT);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, ingressFlow);
    }

    //
    // Table 4, NextHop
    //

    /**
     * Configure the MAC/VLAN Next Hop by matching on the SrcMac and on the
     * pathId stored in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param srcMac
     *            - Src mac to match on
     * @param dstMac
     *            - Dst mac to set on the packet
     */
    public void configureMacNextHopFlow(final String sffNodeName, final long pathId, final String srcMac,
            final String dstMac) {
        int flowPriority = FLOW_PRIORITY_NEXT_HOP;
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        if (srcMac != null) {
            SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
        } else {
            // If the srcMac is null, then the packet is entering SFC and we
            // dont know
            // from where. Make it a lower priority, and only match on the
            // pathId
            flowPriority -= 10;
        }

        List<Action> actionList = new ArrayList<>();
        if (dstMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            actionList.add(SfcOpenflowUtils.createActionSetDlDst(dstMac, actionList.size()));
        }

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, flowPriority);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }


    //
    // Table 10, Transport Egress
    //


    /**
     * Simple pass-through with logic for src/dstMac.
     *
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the dest MAC to match against
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    protected FlowBuilder configureMacTransportEgressFlow(MatchBuilder match, List<Action> actionList, String port,
            final long pathId, final String srcMac, final String dstMac, String cookieStr) {

        // Optionally match on the dstMac
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        if (dstMac != null) {
            SfcOpenflowUtils.addMatchDstMac(match, dstMac);
            // If the dstMac is null, then the packet is leaving SFC and
            // we dont know to where. Make it a lower priority, and only
            // match on the pathId
            flowPriority += 10;
        }

        // Set the macSrc
        if (srcMac != null) {
            actionList.add(SfcOpenflowUtils.createActionSetDlSrc(srcMac, actionList.size()));
        }

        return configureTransportEgressFlow(match, actionList, port, flowPriority, cookieStr);
    }
}

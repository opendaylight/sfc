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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

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
public class SfcMplsFlowProgrammerImpl extends SfcMacFlowProgrammerImpl {

    public SfcMplsFlowProgrammerImpl(SfcOfFlowWriterInterface sfcOfFlowWriter, SfcOpenFlowConfig openFlowConfig) {
        super(sfcOfFlowWriter, openFlowConfig);
    }


    //
    // Configure Table 1, Transport Ingress
    //

    /**
     * Configure an MPLS Transport Ingress flow, by matching on EtherType MPLS
     * Ucast.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    public void configureMplsTransportIngressFlow(final String sffNodeName) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        FlowBuilder transportIngressFlow = configureTransportIngressFlow(match, getTableId(TABLE_INDEX_PATH_MAPPER));

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    //
    // Configure Table 2, PathMapper
    //

    /**
     * Create an MPLS Path Mapper flow. This flow will match on the MPLS label,
     * and set the RSP ID in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param mplsLabel
     *            - the mplsLabel to match on
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param isSf
     *            - if the flow is for an SF or SFF
     */
    public void configureMplsPathMapperFlow(final String sffNodeName, final long mplsLabel, long pathId, boolean isSf) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMplsLabel(match, mplsLabel);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionPopMpls(0));

        FlowBuilder pathMapperFlow;
        if (isSf) {
            pathMapperFlow = configurePathMapperSfFlow(pathId, match, actionList);
        } else {
            pathMapperFlow = configurePathMapperFlow(pathId, match, actionList);
        }
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, pathMapperFlow);
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
    @Override
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
    @Override
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
     * Configure the MPLS Last Hop Transport Egress flow by matching on the RSP
     * path ID in the metadata. The only difference between this method and
     * configureMplsTransportEgressFlow() is that this method checks for App
     * Coexistence.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param mplsLabel
     *            - the MPLS label tag to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     */
    public void configureMplsLastHopTransportEgressFlow(final String sffNodeName, final String srcMac,
            final String dstMac, final long mplsLabel, final String port, final long pathId) {

        // App coexistence
        String switchPort = port;
        if (getTableEgress() > APP_COEXISTENCE_NOT_SET) {
            switchPort = EMPTY_SWITCH_PORT;
        }

        configureMplsTransportEgressFlow(sffNodeName, srcMac, dstMac, mplsLabel, switchPort, pathId,
                TRANSPORT_EGRESS_MPLS_LASTHOP_COOKIE);
    }

    /**
     * Configure the MPLS Transport Egress flow by matching on the RSP path ID
     * in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param mplsLabel
     *            - the MPLS label tag to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     */
    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId) {
        configureMplsTransportEgressFlow(sffNodeName, srcMac, dstMac, mplsLabel, port, pathId,
                TRANSPORT_EGRESS_MPLS_COOKIE);
    }

    /**
     * Simple pass-through method used for above MPLS Transport
     * Egress methods.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param mplsLabel
     *            - the mpls label to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     * @param cookieStr
     *            - the cookie to set on the flow
     */
    private void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId, final String cookieStr) {
        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionPushMpls(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionSetMplsLabel(mplsLabel, actionList.size()));

        FlowBuilder transportEgressFlow = configureMacTransportEgressFlow(match, actionList, port, pathId, srcMac,
                dstMac, cookieStr);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }
}

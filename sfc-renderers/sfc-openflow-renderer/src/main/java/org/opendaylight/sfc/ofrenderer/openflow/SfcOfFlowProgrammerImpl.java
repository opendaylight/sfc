/*
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.sfc.ofrenderer.sfg.GroupBucketInfo;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SfcOfFlowProgrammerImpl implements SfcOfFlowProgrammerInterface {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfFlowProgrammerImpl.class);

    public static final int COOKIE_BIGINT_HEX_RADIX = 16;

    // A common SFC Transport Egress Cookie Base String, allowing
    // all SFC flows to be matched by using cookieStr.startsWith()
    public static final String TRANSPORT_EGRESS_COOKIE_STR_BASE         = "BA5EBA11";
    // The 000001** cookies are for NSH VXGPE Transport Egress flows
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_COOKIE            = "00000101";
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_NSC_COOKIE        = "00000102";
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_LASTHOP_COOKIE    = "00000103";
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_APPCOEXIST_COOKIE = "00000104";
    // The 000002** cookies are for NSH Eth Transport Egress flows (coming soon)
    public static final String TRANSPORT_EGRESS_NSH_ETH_COOKIE   = "00000201";
    // The 000003** cookies are for VXGEP NSH Transport Egress flows
    public static final String TRANSPORT_EGRESS_VLAN_COOKIE         = "00000301";
    public static final String TRANSPORT_EGRESS_VLAN_SF_COOKIE      = "00000302";
    public static final String TRANSPORT_EGRESS_VLAN_LASTHOP_COOKIE = "00000303";
    // The 000004** cookies are for VXGEP NSH Transport Egress flows
    public static final String TRANSPORT_EGRESS_MPLS_COOKIE         = "00000401";
    public static final String TRANSPORT_EGRESS_MPLS_LASTHOP_COOKIE = "00000402";
    public static final String TRANSPORT_EGRESS_MAX_COOKIE          = "00000FFF";

    // Which bits in the metadata field to set, Assuming 4095 PathId's
    public static final BigInteger METADATA_MASK_SFP_MATCH =
            new BigInteger("FFFFFFFFFFFFFFFF", COOKIE_BIGINT_HEX_RADIX);

    public static final short TABLE_INDEX_CLASSIFIER = 0;
    public static final short TABLE_INDEX_TRANSPORT_INGRESS = 1;
    public static final short TABLE_INDEX_PATH_MAPPER = 2;
    public static final short TABLE_INDEX_PATH_MAPPER_ACL = 3;
    public static final short TABLE_INDEX_NEXT_HOP = 4;
    public static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;
    public static final short TABLE_INDEX_MAX_OFFSET = TABLE_INDEX_TRANSPORT_EGRESS;

    public static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 250;
    public static final int FLOW_PRIORITY_ARP_TRANSPORT_INGRESS = 300;
    public static final int FLOW_PRIORITY_PATH_MAPPER = 350;
    public static final int FLOW_PRIORITY_PATH_MAPPER_ACL = 450;
    public static final int FLOW_PRIORITY_NEXT_HOP = 550;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 650;
    public static final int FLOW_PRIORITY_MATCH_ANY = 5;

    private static final int PKTIN_IDLE_TIMEOUT = 60;
    private static final String EMPTY_SWITCH_PORT = "";
    public static final short APP_COEXISTENCE_NOT_SET = -1;
    private static final short TUN_GPE_NP_NSH = 0x4;

    // Instance variables
    private short tableBase;
    // Used for app-coexistence
    private short tableEgress;
    private Long flowRspId;
    private SfcOfFlowWriterInterface sfcOfFlowWriter = null;

    public SfcOfFlowProgrammerImpl(SfcOfFlowWriterInterface sfcOfFlowWriter) {
        this.tableBase = APP_COEXISTENCE_NOT_SET;
        this.tableEgress = APP_COEXISTENCE_NOT_SET;
        this.flowRspId = new Long(0);
        this.sfcOfFlowWriter = sfcOfFlowWriter;
    }

    @Override
    public void setFlowWriter(SfcOfFlowWriterInterface sfcOfFlowWriter) {
        this.sfcOfFlowWriter = sfcOfFlowWriter;
    };

    // This method should only be called by SfcOfRenderer.close()
    @Override
    public void shutdown() throws ExecutionException, InterruptedException {
        this.sfcOfFlowWriter.shutdown();
    }

    //
    // Getters/Setters
    //

    @Override
    public short getTableBase() {
        return tableBase;
    }

    @Override
    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    @Override
    public short getTableEgress() {
        return tableEgress;
    }

    @Override
    public void setTableEgress(short tableEgress) {
        this.tableEgress = tableEgress;
    }

    @Override
    public short getMaxTableOffset() {
        return TABLE_INDEX_MAX_OFFSET;
    }

    @Override
    public void setFlowRspId(Long rspId) {
        this.flowRspId = rspId;
    }

    @Override
    public Set<NodeId> deleteRspFlows(final Long rspId) {
        sfcOfFlowWriter.deleteRspFlows(rspId);
        Set<NodeId> nodes = sfcOfFlowWriter.clearSffsIfNoRspExists();
        sfcOfFlowWriter.deleteFlowSet();
        return nodes;
    }

    @Override
    public void flushFlows() {
        this.sfcOfFlowWriter.flushFlows();
    }

    @Override
    public void purgeFlows() {
        this.sfcOfFlowWriter.purgeFlows();
    }

    /**
     * Check if the given cookie belongs to the Classification table
     *
     * @param cookie - the cookie to compare
     * @return true if the cookie belongs to the Classification table, false otherwise
     */
    public boolean compareClassificationTableCookie(FlowCookie cookie) {
        if (cookie == null || cookie.getValue() == null) {
            return false;
        }

        return cookie.toString().toUpperCase().startsWith(TRANSPORT_EGRESS_COOKIE_STR_BASE);
    }

    //
    // Configure the MatchAny entry specifying if it should drop or goto the next table
    // Classifier MatchAny will go to TransportIngress
    // TransportIngress MatchAny will drop
    // PathMapper MatchAny will go to PathMapperAcl
    // PathMapperAcl MatchAny will go to NextHop
    // NextHop MatchAny will go to TransportEgress
    // TransportEgress MatchAny will drop
    //

    /**
     * Set the match any flow in the Classifier table to go to the
     * Transport Ingress table.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureClassifierTableMatchAny(final String sffNodeName) {
        if(getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We dont need this flow with App Coexistence.
            return;
        }

        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_CLASSIFIER),
                        getTableId(TABLE_INDEX_TRANSPORT_INGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Transport Ingress table to drop.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureTransportIngressTableMatchAny(final String sffNodeName) {
        if(getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We dont need this flow with App Coexistence.
            return;
        }

        FlowBuilder flowBuilder =
                configureTableMatchAnyDropFlow(
                        getTableId(TABLE_INDEX_TRANSPORT_INGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Path Mapper table to go to the
     * Path Mapper ACL table.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configurePathMapperTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_PATH_MAPPER),
                        getTableId(TABLE_INDEX_PATH_MAPPER_ACL));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Path Mapper ACL table to go to the
     * Next Hop table.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configurePathMapperAclTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                        getTableId(TABLE_INDEX_NEXT_HOP));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Next Hop table to go to the
     * Transport Egress table.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureNextHopTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_NEXT_HOP),
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Transport Egress table to drop.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureTransportEgressTableMatchAny(final String sffNodeName) {
        // This is the last table, cant set next table AND doDrop should be false
        FlowBuilder flowBuilder =
                configureTableMatchAnyDropFlow(
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Internal util method to create the Match Any Drop flow
     *
     * @param tableId - the table to write to
     *
     * @return the created flow
     */
    private FlowBuilder configureTableMatchAnyDropFlow(short tableId) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyDropFlow tableId [{}]",
                tableId);

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionDropPacket(0));

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        int order = 0;
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order++);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());

        // Match any
        MatchBuilder match = new MatchBuilder();

        // Finish up the instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);
    }

    /**
     * Internal util method to create the Match Any flow
     *
     * @param tableId - the table to write to
     * @param nextTableId - the next table to go to
     *
     * @return the created flow
     */
    private FlowBuilder configureTableMatchAnyFlow(short tableId, short nextTableId) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyFlow, tableId [{}] nextTableId [{}]",
                tableId, nextTableId);

        // Action, goto next table
        GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(nextTableId);

        InstructionBuilder ib = new InstructionBuilder();
        int order = 0;
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order++);
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());

        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());

        // Match any
        MatchBuilder match = new MatchBuilder();

        // Finish up the instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);
    }

    //
    // Configure Table 1, Transport Ingress
    //

    /**
     * Configure IPv4 Transport Ingress flows. 2 flows will be created, one
     * for TCP and another for UDP.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureIpv4TransportIngressFlow(final String sffNodeName) {
        FlowBuilder transportIngressFlowTcp =
                configureTransportIngressFlow(
                        SfcOpenflowUtils.ETHERTYPE_IPV4,
                        SfcOpenflowUtils.IP_PROTOCOL_TCP);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlowTcp);

        FlowBuilder transportIngressFlowUdp =
                configureTransportIngressFlow(
                        SfcOpenflowUtils.ETHERTYPE_IPV4,
                        SfcOpenflowUtils.IP_PROTOCOL_UDP);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlowUdp);
    }

    /**
     * Configure a VLAN Transport Ingress flow.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureVlanTransportIngressFlow(final String sffNodeName) {
        // vlan match
        // For some reason it didnt match setting etherType=0x8100
        VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        vlanIdBuilder.setVlanIdPresent(true);
        vlanBuilder.setVlanId(vlanIdBuilder.build());

        MatchBuilder match = new MatchBuilder();
        match.setVlanMatch(vlanBuilder.build());

        FlowBuilder transportIngressFlow = configureTransportIngressFlow(match);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    /**
     * Configure a VxlanGpe Transport Ingress flow, by matching on EtherType IPv4.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureVxlanGpeTransportIngressFlow(final String sffNodeName, final long nshNsp, final short nshNsi) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);

        FlowBuilder transportIngressFlow =
                configureTransportIngressFlow(match, getTableId(TABLE_INDEX_NEXT_HOP));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    /**
     * Configure an MPLS Transport Ingress flow, by matching on EtherType MPLS Ucast.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureMplsTransportIngressFlow(final String sffNodeName) {
        FlowBuilder transportIngressFlow =
                configureTransportIngressFlow(SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    /**
     * Simple pass through with default args for ipProtocol and nextTable
     *
     * @param etherType - the etherType protocol to set in the match
     *
     * @return a FlowBuilder with the created Transport Ingress flow
     */
    private FlowBuilder configureTransportIngressFlow(long etherType) {
        return configureTransportIngressFlow(etherType, (short) -1, getTableId(TABLE_INDEX_PATH_MAPPER));
    }

    /**
     * Simple pass through with default args for nextTable
     *
     * @param etherType - the etherType protocol to set in the match
     * @param ipProtocol - the IP protocol to set in the match
     *
     * @return a FlowBuilder with the created Transport Ingress flow
     */
    private FlowBuilder configureTransportIngressFlow(long etherType, short ipProtocol) {
        return configureTransportIngressFlow(etherType, ipProtocol, getTableId(TABLE_INDEX_PATH_MAPPER));
    }

    private FlowBuilder configureTransportIngressFlow(long etherType, short ipProtocol, short nextTable) {
        MatchBuilder match = new MatchBuilder();
        if (ipProtocol > 0) {
            SfcOpenflowUtils.addMatchIpProtocol(match, ipProtocol);
        }
        SfcOpenflowUtils.addMatchEtherType(match, etherType);

        return configureTransportIngressFlow(match, nextTable);
    }

    private FlowBuilder configureTransportIngressFlow(MatchBuilder match) {
        return configureTransportIngressFlow(match, getTableId(TABLE_INDEX_PATH_MAPPER));
    }

    /**
     * Internal util method used by the previously defined configureTransportIngressFlow()
     * methods.
     *
     * @param etherType - the etherType protocol to set in the match
     * @param ipProtocol - the IP protocol to set in the match
     * @param nextTable - the nextTable to jump to upon matching
     *
     * @return a FlowBuilder with the created Transport Ingress flow
     */
    private FlowBuilder configureTransportIngressFlow(MatchBuilder match, short nextTable) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow");

        // Action, goto the nextTable
        GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(nextTable);

        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());
        ib.setKey(new InstructionKey(1));
        ib.setOrder(0);
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(ib);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                FLOW_PRIORITY_TRANSPORT_INGRESS,
                "ingress_Transport_Flow", match, isb);
    }

    /**
     * Create an ARP responder flow in the Transport Ingress table. This flow is
     * intended to respond to SF ARP messages, and is only created for SFs of type
     * TCP-Proxy.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param mac - the SFF mac
     */
    @Override
    public void configureArpTransportIngressFlow(final String sffNodeName, final String mac) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportArpIngressThread, sff [{}] mac [{}]",
                sffNodeName, mac);

        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_ARP);
        SfcOpenflowUtils.addMatchArpRequest(match);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveEthSrcToEthDstAction(order++));
        actionList.add(SfcOpenflowUtils.createActionSetDlSrc(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpOpAction(SfcOpenflowUtils.ARP_REPLY, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpShaAction(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpShaToArpThaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpTpaToRegAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpSpaToArpTpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveRegToArpSpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++));

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        int ibOrder = 0;
        InstructionBuilder actionsIb = new InstructionBuilder();
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(ibOrder));
        actionsIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(actionsIb);

        // Create and configure the FlowBuilder
        FlowBuilder arpTransportIngressFlow =
                SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                        FLOW_PRIORITY_ARP_TRANSPORT_INGRESS,
                        "ingress_Transport_Arp_Flow",
                        match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, arpTransportIngressFlow);
    }

    @Override
    public void configureVxlanGpeSfLoopbackEncapsulatedEgressFlow(
            final String sffNodeName, final String sfIp, final short vxlanUdpPort, final long sffPort) {

        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchIpProtocol(match, SfcOpenflowUtils.IP_PROTOCOL_UDP);
        SfcOpenflowUtils.addMatchDstIpv4(match, sfIp, 32);
        SfcOpenflowUtils.addMatchDstUdpPort(match, vxlanUdpPort);

        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionOutPort((int) sffPort, 0));

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        int ibOrder = 0;
        InstructionBuilder actionsIb = new InstructionBuilder();
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(ibOrder));
        actionsIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(actionsIb);

        // Create and configure the FlowBuilder
        FlowBuilder sfFlow =
                SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                        FLOW_PRIORITY_ARP_TRANSPORT_INGRESS,
                        "ingress_Transport_Arp_Flow",
                        match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, sfFlow);
    }

    @Override
    public void configureVxlanGpeSfReturnLoopbackIngressFlow(final String sffNodeName, final short vxlanUdpPort, final long sffPort) {
        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchIpProtocol(match, SfcOpenflowUtils.IP_PROTOCOL_UDP);
        SfcOpenflowUtils.addMatchDstUdpPort(match, vxlanUdpPort);
        SfcOpenflowUtils.addMatchInPort(match, new NodeId(sffNodeName), sffPort);

        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.LOCAL.toString(), 0));

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        int ibOrder = 0;
        InstructionBuilder actionsIb = new InstructionBuilder();
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(ibOrder));
        actionsIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(actionsIb);

        // Create and configure the FlowBuilder
        FlowBuilder sfFlow =
                SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                        FLOW_PRIORITY_ARP_TRANSPORT_INGRESS,
                        "ingress_Transport_Arp_Flow",
                        match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, sfFlow);
    }


    //
    // Configure Table 2, PathMapper
    //

    /**
     * Create an MPLS Path Mapper flow. This flow will match on the MPLS label,
     * and set the RSP ID in the metadata.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param mplsLabel - the mplsLabel to match on
     * @param pathId - the RSP ID to write to the metadata
     * @param isSf - if the flow is for an SF or SFF
     */
    @Override
    public void configureMplsPathMapperFlow(final String sffNodeName, final long mplsLabel, long pathId, boolean isSf) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMplsLabel(match, mplsLabel);

        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPopMpls(0));

        FlowBuilder pathMapperFlow;
        if(isSf) {
            pathMapperFlow = configurePathMapperSfFlow(pathId, match, actionList);
        } else {
            pathMapperFlow = configurePathMapperFlow(pathId, match, actionList);
        }
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, pathMapperFlow);
    }

    /**
     * Create a VLAN Path Mapper flow. This flow will match on the VLAN tag,
     * and set the RSP ID in the metadata.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param vlan - the vlan tag to match on
     * @param pathId - the RSP ID to write to the metadata
     * @param isSf - if the flow is for an SF or SFF
     */
    @Override
    public void configureVlanPathMapperFlow(final String sffNodeName, final int vlan, long pathId, boolean isSf) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchVlan(match, vlan);

        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPopVlan(0));

        FlowBuilder pathMapperFlow;
        if(isSf) {
            pathMapperFlow = configurePathMapperSfFlow(pathId, match, actionList);
        } else {
            pathMapperFlow = configurePathMapperFlow(pathId, match, actionList);
        }
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, pathMapperFlow);
    }

    /**
     * Simple pass through for SF Path Mapper flows.
     *
     * @param pathId - the RSP ID to write to the metadata
     * @param match -already created matches
     * @param actionList - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configurePathMapperSfFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        SfcOpenflowUtils.addMatchDscp(match, (short) pathId);
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER+10);
    }

    /**
     * Simple pass through with default arg for flowPriority.
     *
     * @param pathId - the RSP ID to write to the metadata
     * @param match -already created matches
     * @param actionList - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER);
    }

    /**
     * Internal util method used by the previously defined configurePathMapperFlow()
     * methods.
     *
     * @param pathId - the RSP ID to write to the metadata
     * @param match -already created matches
     * @param actionList - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList, int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.configurePathMapperFlow sff [{}] pathId [{}]",
                pathId);

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();

        int ibOrder = 0;
        int actionOrder = 0;
        InstructionBuilder metadataIb = new InstructionBuilder();
        metadataIb.setInstruction(
                SfcOpenflowUtils.createInstructionMetadata(
                        actionOrder++,
                        getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH));
        metadataIb.setKey(new InstructionKey(ibOrder));
        metadataIb.setOrder(ibOrder++);
        instructions.add(metadataIb.build());

        InstructionBuilder actionsIb = new InstructionBuilder();
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(ibOrder));
        actionsIb.setOrder(ibOrder++);

        GoToTableBuilder gotoNextHop = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_NEXT_HOP));
        InstructionBuilder gotoNextHopIb = new InstructionBuilder();
        gotoNextHopIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoNextHop.build()).build());
        gotoNextHopIb.setKey(new InstructionKey(ibOrder));
        gotoNextHopIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        instructions.add(actionsIb.build());
        instructions.add(gotoNextHopIb.build());
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_PATH_MAPPER),
                flowPriority,
                "nextHop", match, isb);
    }

    //
    // Table 3, PathMapper ACL
    //

    /**
     * This table is populated as a result of PktIn for TCP Proxy SFs. It matches
     * on Src/Dst IP and writes the path ID to the metadata.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param pktSrcIpStr - Src IP to match on
     * @param pktDstIpStr - Src IP to match on
     * @param pathId - the RSP ID to write to the metadata
     */
    public void configurePathMapperAclFlow(final String sffNodeName, final String pktSrcIpStr, final String pktDstIpStr,
            short pathId) {
        LOG.debug(
                "SfcProviderSffFlowWriter.configurePathMapperAclFlow sff [{}] srcIp [{}] dstIp [{}] pathId [{}]",
                sffNodeName, pktSrcIpStr, pktDstIpStr, pathId);

        //
        // Match on the Src and Dst IPs
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchSrcIpv4(match, pktSrcIpStr, 32);
        SfcOpenflowUtils.addMatchDstIpv4(match, pktDstIpStr, 32);

        // Set the PathId in the metadata and goto the TransportEgress table
        int ibOrder = 0;
        InstructionBuilder metadataIb = new InstructionBuilder();
        metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(ibOrder,
                getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH));
        metadataIb.setKey(new InstructionKey(ibOrder));
        metadataIb.setOrder(ibOrder++);

        GoToTableBuilder gotoNextHop = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_NEXT_HOP));
        InstructionBuilder gotoNextHopIb = new InstructionBuilder();
        gotoNextHopIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoNextHop.build()).build());
        gotoNextHopIb.setKey(new InstructionKey(ibOrder));
        gotoNextHopIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(metadataIb.build());
        instructions.add(gotoNextHopIb.build());

        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        FlowBuilder ingressFlow = SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                FLOW_PRIORITY_PATH_MAPPER_ACL,
                "nextHop",
                match, isb);
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
     * @param sffNodeName - the SFF to write the flow to
     * @param pathId - the RSP ID to write to the metadata
     * @param srcMac - Src mac to match on
     * @param dstMac - Dst mac to set on the packet
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
            // If the srcMac is null, then the packet is entering SFC and we dont know
            // from where. Make it a lower priority, and only match on the pathId
            flowPriority -= 10;
        }

        List<Action> actionList = new ArrayList<Action>();
        if (dstMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            actionList.add(SfcOpenflowUtils.createActionSetDlDst(dstMac, 0));
        }

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, flowPriority);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    /**
     * Configure the VxLanGPE NSH Next Hop by matching on the NSH pathId and
     * index stored in the NSH header.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param dstIp - the VxLan GPE tunnel destination IP
     * @param nshNsp - NSH Service Path to match on
     * @param nshNsi - NSH Index to match on
     */
    @Override
    public void configureVxlanGpeNextHopFlow(final String sffNodeName, final String dstIp, final long nshNsp,
            final short nshNsi) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        if (dstIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);
            actionList.add(actionSetNwDst);
        }

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    /**
     * Simple pass through with default arg for flowPriority.
     *
     * @param match -already created matches
     * @param actionList - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureNextHopFlow(MatchBuilder match, List<Action> actionList) {
        return configureNextHopFlow(match, actionList, FLOW_PRIORITY_NEXT_HOP);
    }

    /**
     * Internal util method used by the previously defined configureNextHopFlow()
     * methods.
     *
     * @param match -already created matches
     * @param actionList - a list of actions already created
     * @param flowPriority - the priority to set on the flow
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureNextHopFlow(MatchBuilder match, List<Action> actionList, int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.configureNextHopFlow");

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        GoToTableBuilder gotoTb = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_TRANSPORT_EGRESS));

        InstructionBuilder gotoTbIb = new InstructionBuilder();
        gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
        gotoTbIb.setKey(new InstructionKey(1));
        gotoTbIb.setOrder(1);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setKey(new InstructionKey(0));
        ib.setOrder(0);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        instructions.add(gotoTbIb.build());
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_NEXT_HOP),
                flowPriority, "nextHop", match, isb);
    }


    //
    // Table 10, Transport Egress
    //

    @Override
    public void configureVlanSfTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId, boolean doPktin) {
        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        // In order to set the IP DSCP, we need to match IPv4
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionWriteDscp((short) pathId, order++));
        actionList.add(SfcOpenflowUtils.createActionPushVlan(order++));
        actionList.add(SfcOpenflowUtils.createActionSetVlanId(dstVlan, order++));

        if(doPktin) {
            // Notice TCP SYN matching is only supported in OpenFlow 1.5
            SfcOpenflowUtils.addMatchTcpSyn(match);
            actionList.add(SfcOpenflowUtils.createActionPktIn(SfcOpenflowUtils.PKT_LENGTH_IP_HEADER, order++));
        }

        FlowBuilder transportEgressFlow =
                configureMacTransportEgressFlow(
                        match, actionList, port, order, pathId, srcMac, dstMac,
                        TRANSPORT_EGRESS_VLAN_SF_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the VLAN LastHop Transport Egress flow by matching on the
     * RSP path ID in the metadata. The only difference between this method and
     * configureVlanTransportEgressFlow() is that this method checks for App
     * Coexistence.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param srcMac - the source MAC to write to the packet
     * @param dstMac - the destination MAC to match on
     * @param dstVlan - the VLAN tag to write to the packet
     * @param port - the switch port to send the packet out on
     * @param pathId - the RSP path id to match on
     */
    public void configureVlanLastHopTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, final String port, final long pathId) {

        // App coexistence
        String switchPort = port;
        if(getTableEgress() > APP_COEXISTENCE_NOT_SET) {
            switchPort = EMPTY_SWITCH_PORT;
        }

        configureVlanTransportEgressFlow(
                sffNodeName, srcMac, dstMac, dstVlan, switchPort, pathId,
                TRANSPORT_EGRESS_VLAN_LASTHOP_COOKIE);
    }

    /**
     * Configure the VLAN Transport Egress flow by matching on the RSP path ID
     * in the metadata.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param srcMac - the source MAC to write to the packet
     * @param dstMac - the destination MAC to match on
     * @param dstVlan - the VLAN tag to write to the packet
     * @param port - the switch port to send the packet out on
     * @param pathId - the RSP path id to match on
     */
    @Override
    public void configureVlanTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId) {
        configureVlanTransportEgressFlow(
                sffNodeName, srcMac, dstMac, dstVlan, port, pathId,
                TRANSPORT_EGRESS_VLAN_COOKIE);
    }

    /**
     * Internal Util method used for above VLAN Transport Egress methods
     *
     * @param sffNodeName
     * @param srcMac
     * @param dstMac
     * @param dstVlan
     * @param port
     * @param pathId
     * @param cookieStr
     */
    public void configureVlanTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId, final String cookieStr) {

        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPushVlan(order++));
        actionList.add(SfcOpenflowUtils.createActionSetVlanId(dstVlan, order++));

        FlowBuilder transportEgressFlow =
                configureMacTransportEgressFlow(
                        match, actionList, port, order, pathId, srcMac, dstMac, cookieStr);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the MPLS Last Hop Transport Egress flow by matching on the
     * RSP path ID in the metadata. The only difference between this method and
     * configureMplsTransportEgressFlow() is that this method checks for App
     * Coexistence.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param srcMac - the source MAC to write to the packet
     * @param dstMac - the destination MAC to match on
     * @param mplsLabel - the MPLS label tag to write to the packet
     * @param port - the switch port to send the packet out on
     * @param pathId - the RSP path id to match on
     */
    @Override
    public void configureMplsLastHopTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, final String port, final long pathId) {

        // App coexistence
        String switchPort = port;
        if(getTableEgress() > APP_COEXISTENCE_NOT_SET) {
            switchPort = EMPTY_SWITCH_PORT;
        }

        configureMplsTransportEgressFlow(
                sffNodeName, srcMac, dstMac, mplsLabel, switchPort, pathId,
                TRANSPORT_EGRESS_MPLS_LASTHOP_COOKIE);
    }

    /**
     * Configure the MPLS Transport Egress flow by matching on the RSP path ID
     * in the metadata.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param srcMac - the source MAC to write to the packet
     * @param dstMac - the destination MAC to match on
     * @param mplsLabel - the MPLS label tag to write to the packet
     * @param port - the switch port to send the packet out on
     * @param pathId - the RSP path id to match on
     */
    @Override
    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId) {
        configureMplsTransportEgressFlow(
                sffNodeName, srcMac, dstMac, mplsLabel, port, pathId,
                TRANSPORT_EGRESS_MPLS_COOKIE);
    }

    /**
     * Internal Util method used for above MPLS Transport Egress methods
     *
     * @param sffNodeName
     * @param srcMac
     * @param dstMac
     * @param mplsLabel
     * @param port
     * @param pathId
     * @param cookieStr
     */
    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId, final String cookieStr) {
        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPushMpls(order++));
        actionList.add(SfcOpenflowUtils.createActionSetMplsLabel(mplsLabel, order++));

        FlowBuilder transportEgressFlow =
                configureMacTransportEgressFlow(
                        match, actionList, port, order, pathId, srcMac, dstMac, cookieStr);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the VxLAN GPE NSH Transport Egress flow by matching on the
     * NSP and NSI.
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param nshNsp - the NSH Service Path to match on
     * @param nshNsi - the NSH Service Index to match on
     * @param port - the switch port to send the packet out on
     */
    @Override
    public void configureVxlanGpeLastHopTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);

        // On the last hop Copy/Move Nsi, Nsp, Nsc1=>TunIpv4Dst, and Nsc2=>TunId(Vnid)
        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(order++));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList.add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++));

        FlowBuilder transportEgressFlow =
                configureTransportEgressFlow(match, actionList, port, order,
                        FLOW_PRIORITY_TRANSPORT_EGRESS,
                        TRANSPORT_EGRESS_NSH_VXGPE_LASTHOP_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    @Override
    public void configureVxlanGpeTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi, String port) {

        // When outputing to an outport, if inport==outport, then according to the
        // openflow spec, the packet will be dropped. To avoid this, outport must
        // be set to INPORT. This method writes 2 flows to avoid this situation:
        //   flow1: if inport==port, actions=output:INPORT (higher priority than flow2)
        //   flow2: actions=output:port (flow2 is basically the else condition)

        Long vxgpePort = null;
        try {
            vxgpePort = SfcOvsUtil.getVxlanGpeOfPort(sffNodeName);
        } catch(Exception e) {
            // getVxlanGpeOfPort throws NPE in Unit Tests, which is ok
        }

        if(vxgpePort != null) {
            String vxgpePortStr = "output:" + vxgpePort.toString();
            configureVxlanGpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, vxgpePortStr, OutputPortValues.INPORT.toString());
            configureVxlanGpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, OutputPortValues.INPORT.toString(), vxgpePortStr);
        } else {
            configureVxlanGpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, port, port);
        }
    }

    /**
     * Simple call through for configureVxlanGpeTransportEgressFlow()
     */
    private void configureVxlanGpeTransportEgressFlowPorts(
            final String sffNodeName, final long nshNsp, final short nshNsi, String inport, String outport) {
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;

        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);
        if(!inport.startsWith(OutputPortValues.INPORT.toString())) {
            // if we output to a port that's the same as the inport, the pkt will be dropped
            SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(inport));
            outport = OutputPortValues.INPORT.toString();
            flowPriority += 5;
        }

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        // Copy/Move Nsc1/Nsc2 to the next hop
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveTunIdRegister(order++));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList.add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++));

        FlowBuilder transportEgressFlow =
                configureTransportEgressFlow(match, actionList, outport, order,
                        flowPriority,
                        TRANSPORT_EGRESS_NSH_VXGPE_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * For NSH, Return the packet to INPORT if the NSH Nsc1 Register is not present (==0)
     * If it is present, it will be handled by the flow created in ConfigureTransportEgressFlowThread()
     * This flow will have a higher priority than the flow created in
     * ConfigureTransportEgressFlowThread()
     *
     * @param sffNodeName - the SFF to write the flow to
     * @param nshNsp - the NSH Service Path to match on
     * @param nshNsi - the NSH Service Index to match on
     * @param port - the switch port to send the packet out on
     */
    @Override
    public void configureNshNscTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi, String port) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureNshNscTransportEgressFlowThread, sff [{}] nsp [{}] nsi [{}] port [{}]",
                sffNodeName, nshNsp, nshNsi, port);

        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, 0l);

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++));

        FlowBuilder transportEgressFlow =
                configureTransportEgressFlow(
                        match, new ArrayList<Action>(), port,
                        order, FLOW_PRIORITY_TRANSPORT_EGRESS + 10,
                        TRANSPORT_EGRESS_NSH_VXGPE_NSC_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    @Override
    public void configureVxlanGpeAppCoexistTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi, final String sffIp) {

        // This flow only needs to be created if App Coexistence is being used
        if(getTableEgress() == APP_COEXISTENCE_NOT_SET) {
            LOG.debug("configureVxlanGpeAppCoexistTransportEgressFlow NO AppCoexistence configured, skipping flow");
            return;
        }

        // Create a match checking if C1 is set to this SFF
        // Assuming IPv4
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(sffIp));
        long ipl = ip & 0xffffffffL;

        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, ipl);

        // Copy/Move Nsi, Nsp, Nsc1=>TunIpv4Dst, and Nsc2=>TunId(Vnid)
        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(order++));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList.add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++));

        FlowBuilder transportEgressFlow =
                configureTransportEgressFlow(
                        match, actionList, EMPTY_SWITCH_PORT,
                        order, FLOW_PRIORITY_TRANSPORT_EGRESS + 10,
                        TRANSPORT_EGRESS_NSH_VXGPE_APPCOEXIST_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Simple pass through with logic for src/dstMac.
     *
     * @param match -already created matches
     * @param actionList - a list of actions already created
     * @param port - the switch port to send the packet out on
     * @param order - order to use when writing to the actionList
     * @param pathId - the RSP path id to match on
     * @param srcMac - the source MAC to write to the packet
     * @param dstMac - the dest MAC to match against
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureMacTransportEgressFlow(MatchBuilder match, List<Action> actionList,
            String port, int order, final long pathId, final String srcMac, final String dstMac,
            String cookieStr) {

        //Optionally match on the dstMac
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        if (dstMac != null) {
            SfcOpenflowUtils.addMatchDstMac(match, dstMac);
            // If the dstMac is null, then the packet is leaving SFC and we dont know
            // to where. Make it a lower priority, and only match on the pathId
            flowPriority += 10;
        }

        // Set the macSrc
        if (srcMac != null) {
            actionList.add(SfcOpenflowUtils.createActionSetDlSrc(srcMac, order++));
        }

        return configureTransportEgressFlow(
                match, actionList, port, order,
                flowPriority,
                cookieStr);
    }

    /**
     * Internal util method used by the previously defined configureTransportEgressFlow()
     * methods.
     *
     * @param match -already created matches
     * @param actionList - a list of actions already created
     * @param port - the switch port to send the packet out on
     * @param order - order to use when writing to the actionList
     * @param flowPriority - the priority to set on the flow
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureTransportEgressFlow(
            MatchBuilder match, List<Action> actionList, String port, int order, int flowPriority, String cookieStr) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportEgressFlow");

        if(port.equals(EMPTY_SWITCH_PORT) && getTableEgress() > APP_COEXISTENCE_NOT_SET) {
            // Application Coexistence:
            // Instead of egressing the packet out a port, send it to
            // a different application pipeline on this same switch
            actionList.add(SfcOpenflowUtils.createActionResubmitTable(getTableEgress(), order++));

        } else {
            actionList.add(SfcOpenflowUtils.createActionOutPort(port, order++));
        }

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

        // Put our Instruction in a list of Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        // Make the cookie
        BigInteger cookie =
                new BigInteger(
                        new String(TRANSPORT_EGRESS_COOKIE_STR_BASE + cookieStr),
                        COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                flowPriority,
                cookie,
                "default_egress_flow", match, isb);
    }

    @Override
    public void configureGroup(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
            List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {

        LOG.debug("configuring group: sffName {}, groupName {}, ofNodeId {}, id {}, type {}",
                sffNodeName, sfgName, openflowNodeId, sfgId, groupType);
        GroupBuilder gb = new GroupBuilder();
        BucketsBuilder bbs = new BucketsBuilder();
        gb.setBarrier(true);
        gb.setGroupType(GroupTypes.forValue(groupType));
        gb.setGroupName(sfgName);
        gb.setGroupId(new GroupId(sfgId));

        List<Bucket> buckets = new ArrayList<Bucket>();
        BucketBuilder bb = new BucketBuilder();
        for (GroupBucketInfo bucketInfo : bucketInfos) {
            LOG.debug("building bucket {}", bucketInfo);
            buckets.add(buildBucket(bb, bucketInfo));
        }
        bbs.setBucket(buckets);
        gb.setBuckets(bbs.build());
        String nodeName = openflowNodeId != null ? openflowNodeId : sffNodeName;
        sfcOfFlowWriter.writeGroupToDataStore(nodeName, gb, isAddGroup);
    }

    private Bucket buildBucket(BucketBuilder bb, GroupBucketInfo bucketInfo) {
        int order = 0;
        BucketId bucketId = new BucketId((long) bucketInfo.getIndex());
        bb.setBucketId(bucketId);
        bb.setKey(new BucketKey(bucketId));
        String sfMac = bucketInfo.getSfMac();
        String sfIp = bucketInfo.getSfIp();
        List<Action> actionList = new ArrayList<Action>();
        if (sfMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            Action actionDstMac = SfcOpenflowUtils.createActionSetDlDst(sfMac, order);
            order++;
            actionList.add(actionDstMac);
        }

        if (sfIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionSetNwDst(sfIp, 32, order);
            order++;
            actionList.add(actionSetNwDst);
        }

        Action actionOutPort = SfcOpenflowUtils.createActionOutPort(bucketInfo.getOutPort(), order);
        actionList.add(actionOutPort);

        bb.setAction(actionList);
        return bb.build();
    }

    @Override
    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId,
            String groupName) {
        LOG.debug(
                "SfcProviderSffFlowWriter.ConfigureGroupNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] groupId[{}]",
                sffNodeName, sfpId, srcMac, groupId);

        int flowPriority = FLOW_PRIORITY_NEXT_HOP;

        //
        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();

        // Match on the either the metadata sfpId or the NSH NSP and NSI
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);

        // match on the src mac
        if (srcMac != null) {
            SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
        } else {
            // If the srcMac is null, then the packet is entering SFC and we dont know
            // from where. Make it a lower priority, and only match on the pathId
            flowPriority -= 10;
        }

        //
        // Create the Actions
        List<Action> actionList = new ArrayList<Action>();

        GroupActionBuilder groupActionBuilder = new GroupActionBuilder();
        groupActionBuilder.setGroupId(groupId);
        groupActionBuilder.setGroup(groupName);

        GroupActionCaseBuilder groupActionCaseBuilder = new GroupActionCaseBuilder();
        groupActionCaseBuilder.setGroupAction(groupActionBuilder.build());

        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setAction(groupActionCaseBuilder.build());
        actionBuilder.setOrder(0);
        actionBuilder.setKey(new ActionKey(0));
        Action groupAction = actionBuilder.build();

        actionList.add(groupAction);

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setKey(new InstructionKey(0));
        ib.setOrder(0);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);

        //
        // Create and configure the FlowBuilder
        FlowBuilder nextHopFlow =
                SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_NEXT_HOP),
                        flowPriority, "nextHop", match, isb);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    private static BigInteger getMetadataSFP(long sfpId) {
        return (BigInteger.valueOf(sfpId).and(METADATA_MASK_SFP_MATCH));
    }

    /**
     * getTableId
     * Having a TableBase allows us to "offset" the SFF tables by this.tableBase
     * tables. This is used for App Coexistence.
     *
     * @param tableIndex - the table to offset
     * @return the resulting table id
     */
    private short getTableId(short tableIndex) {
        if(getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // App Coexistence
            if(tableIndex == TABLE_INDEX_TRANSPORT_INGRESS) {
                // With AppCoexistence the TransportIngress table is now table 0
                return 0;
            } else {
                // Need to subtract 2 to compensate for:
                // - TABLE_INDEX_CLASSIFIER=0 - which is not used for AppCoexistence
                // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for AppCoexistence
                // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should return 20
                return (short) (getTableBase() + tableIndex-2);
            }
        } else {
            return tableIndex;
        }
    }
}

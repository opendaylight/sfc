/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.l2renderer.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.mpls.action._case.PushMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

public class SfcOpenflowUtils {

    public static final int ETHERTYPE_IPV4 = 0x0800;
    public static final int ETHERTYPE_VLAN = 0x8100;
    public static final int ETHERTYPE_MPLS_UCAST = 0x8847;

    private static final int DEFAULT_SB_CAPACITY = 16;
    private static final int FLOWREF_CAPACITY = 256;
    private static final String FLOWID_PREFIX = "SFC";
    private static final String FLOWID_SEPARATOR = ".";
    private static final int COOKIE_BIGINT_INT_RADIX = 10;

    public static FlowBuilder createFlowBuilder(final short table, final int priority, final String flowName, MatchBuilder match, InstructionsBuilder isb) {
        FlowBuilder flow = new FlowBuilder();
        flow.setId(new FlowId(SfcOpenflowUtils.getFlowRef(table)));
        flow.setKey(new FlowKey(new FlowId(SfcOpenflowUtils.getFlowRef(table))));
        flow.setTableId(table);
        flow.setFlowName(flowName);
        BigInteger cookieValue = new BigInteger("20", COOKIE_BIGINT_INT_RADIX);
        flow.setCookie(new FlowCookie(cookieValue));
        flow.setCookieMask(new FlowCookie(cookieValue));
        flow.setContainerName(null);
        flow.setStrict(false);
        flow.setMatch(match.build());
        flow.setInstructions(isb.build());
        flow.setPriority(priority);
        flow.setHardTimeout(0);
        flow.setIdleTimeout(0);
        flow.setFlags(new FlowModFlags(false, false, false, false, false));
        if (null == flow.isBarrier()) {
            flow.setBarrier(Boolean.FALSE);
        }

        return flow;
    }


    //
    // Add Match methods
    //

    /**
     * Add an etherType match to an existing MatchBuilder
     * @param match
     * @param etherType
     */
    public static void addMatchEtherType(MatchBuilder match, final long etherType) {
        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(etherType));
        eth.setEthernetType(ethTypeBuilder.build());

        match.setEthernetMatch(eth.build());
    }

    public static void addMatchMplsLabel(MatchBuilder match, long label) {
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType((long) ETHERTYPE_MPLS_UCAST));

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        eth.setEthernetType(ethTypeBuilder.build());
        match.setEthernetMatch(eth.build());

        ProtocolMatchFieldsBuilder protomatch = new ProtocolMatchFieldsBuilder();
        protomatch.setMplsLabel((long) label);
        match.setProtocolMatchFields(protomatch.build());
    }

    public static void addMatchVlan(MatchBuilder match, int vlan) {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        VlanId vlanId = new VlanId(vlan);
        vlanIdBuilder.setVlanId(vlanId);
        vlanIdBuilder.setVlanIdPresent(true);
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());

        match.setVlanMatch(vlanMatchBuilder.build());
    }

    public static void addMatchSrcMac(MatchBuilder match, final String srcMac) {
        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethSourceBuilder.setAddress(new MacAddress(srcMac));
        ethernetMatch.setEthernetSource(ethSourceBuilder.build());

        match.setEthernetMatch(ethernetMatch.build());
    }

    public static void addMatchDstMac(MatchBuilder match, final String dstMac) {
        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(dstMac));
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());

        match.setEthernetMatch(ethernetMatch.build());
    }

    public static void addMatchMetada(MatchBuilder match, BigInteger metadataValue, BigInteger metadataMask) {
        MetadataBuilder metadata = new MetadataBuilder();
        metadata.setMetadata(metadataValue);
        metadata.setMetadataMask(metadataMask);

        match.setMetadata(metadata.build());
    }


    //
    // Create Action methods
    //

    private static ActionBuilder createActionBuilder(int order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setOrder(order);
        ab.setKey(new ActionKey(order));

        return ab;
    }

    public static GoToTableBuilder createActionGotoTable(final short toTable) {
        GoToTableBuilder gotoTb = new GoToTableBuilder();
        gotoTb.setTableId(toTable);

        return gotoTb;
    }

    public static Action createActionOutPort(final String portUri, final int order) {
        OutputActionBuilder output = new OutputActionBuilder();
        Uri value = new Uri(portUri);
        output.setOutputNodeConnector(value);
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());

        return ab.build();
    }

    public static Action createActionSetDlSrc(String srcMac, int order) {
        EthernetSourceBuilder ethSrc = new EthernetSourceBuilder();
        ethSrc.setAddress(new MacAddress(srcMac));

        EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
        ethMatchBuilder.setEthernetSource(ethSrc.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(
                new SetFieldBuilder().setEthernetMatch(ethMatchBuilder.build())
                .build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionSetDlDst(String dstMac, int order) {
        EthernetDestinationBuilder ethDst = new EthernetDestinationBuilder();
        ethDst.setAddress(new MacAddress(dstMac));

        EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
        ethMatchBuilder.setEthernetDestination(ethDst.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(
                new SetFieldBuilder().setEthernetMatch(ethMatchBuilder.build())
                .build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionPopVlan(int order) {
        PopVlanActionCaseBuilder popVlanBuilder = new PopVlanActionCaseBuilder();
        popVlanBuilder.setPopVlanAction(
                new PopVlanActionBuilder().build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(popVlanBuilder.build());

        return ab.build();
    }

    public static Action createActionPushVlan(int order) {
        PushVlanActionBuilder pushVlanBuilder = new PushVlanActionBuilder();
        pushVlanBuilder.setEthernetType(ETHERTYPE_VLAN);

        PushVlanActionCaseBuilder pushVlanActionCase = new PushVlanActionCaseBuilder();
        pushVlanActionCase.setPushVlanAction(pushVlanBuilder.build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(pushVlanActionCase.build());

        return ab.build();
    }

    public static Action createActionSetVlanId(int vlan, int order) {
        VlanIdBuilder vlanBuilder = new VlanIdBuilder();
        vlanBuilder.setVlanId(new VlanId(vlan));
        vlanBuilder.setVlanIdPresent(true);

        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        vlanMatchBuilder.setVlanId(vlanBuilder.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(
                new SetFieldBuilder().setVlanMatch(vlanMatchBuilder.build())
                .build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionPushMpls(int order) {
        PushMplsActionBuilder push = new PushMplsActionBuilder();
        push.setEthernetType(new Integer(ETHERTYPE_MPLS_UCAST));

        PushMplsActionCaseBuilder pushMplsCase = new PushMplsActionCaseBuilder();
        pushMplsCase.setPushMplsAction(push.build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(pushMplsCase.build());

        return ab.build();
    }

    public static Action createActionSetMplsLabel(long label, int order) {
        ProtocolMatchFieldsBuilder protomatch = new ProtocolMatchFieldsBuilder();
        protomatch.setMplsLabel(label);

        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();
        setFieldBuilder.setProtocolMatchFields(protomatch.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(setFieldBuilder.build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionPopMpls(int order) {
        PopMplsActionBuilder popMplsActionBuilder = new PopMplsActionBuilder();
        // TODO, is this ethertype correct?
        popMplsActionBuilder.setEthernetType(0XB);

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new PopMplsActionCaseBuilder().setPopMplsAction(popMplsActionBuilder.build()).build());

        return ab.build();
    }

    public static WriteMetadataCase createInstructionMetadata(int order, BigInteger metadataVal, BigInteger metadataMask) {
        WriteMetadataBuilder wmb = new WriteMetadataBuilder();
        wmb.setMetadata(metadataVal);
        wmb.setMetadataMask(metadataMask);
        WriteMetadataCaseBuilder wmcb = new WriteMetadataCaseBuilder().setWriteMetadata(wmb.build());

        return wmcb.build();
    }

    public static InstructionsBuilder createInstructionsBuilder(InstructionBuilder ib) {
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);

        return isb;
    }


    // Only configure OpenFlow Capable SFFs
    public static boolean isSffOpenFlowCapable(final String sffName) {
        InstanceIdentifier<FlowCapableNode> nodeInstancIdentifier =
                InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, new NodeKey(new NodeId(sffName)))
                    .augmentation(FlowCapableNode.class)
                    .build();

        // If its not a Flow Capable Node, this should return NULL
        // TODO need to verify this, once SFC can connect to simple OVS nodes that arent flow capable
        FlowCapableNode node = SfcDataStoreAPI.readTransactionAPI(nodeInstancIdentifier, LogicalDatastoreType.OPERATIONAL);
        if(node != null) {
            return true;
        }
        return false;
    }

    public static String getFlowRef(final String srcIp, final short srcMask, final String dstIp, final short dstMask,
            final short srcPort, final short dstPort, final byte protocol, final long sfpId) {
        return new StringBuilder(FLOWREF_CAPACITY).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(sfpId)
                .append(FLOWID_SEPARATOR).append(srcIp).append(FLOWID_SEPARATOR).append(srcMask)
                .append(FLOWID_SEPARATOR).append(dstIp).append(FLOWID_SEPARATOR).append(dstMask)
                .append(FLOWID_SEPARATOR).append(srcPort).append(FLOWID_SEPARATOR).append(dstPort)
                .append(FLOWID_SEPARATOR).append(protocol).append(FLOWID_SEPARATOR).append(sfpId).toString();
    }

    public static String getFlowRef(final long sfpId, final String dstMac, final int dstVlan) {
        return new StringBuilder(FLOWREF_CAPACITY).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(sfpId)
                .append(FLOWID_SEPARATOR).append(dstMac).append(FLOWID_SEPARATOR).append(dstVlan).toString();
    }

    public static String getFlowRef(final String dstMac, final int dstVlan) {
        return new StringBuilder(FLOWREF_CAPACITY).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(dstMac)
                .append(FLOWID_SEPARATOR).append(dstVlan).toString();
    }

    public static String getFlowRef(final int vlan) {
        return new StringBuilder(FLOWREF_CAPACITY).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(vlan).toString();
    }

    public static String getFlowRef(final long sfpId, final String srcMac, final String dstMac, final int dstVlan) {
        return new StringBuilder(FLOWREF_CAPACITY).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(sfpId)
                .append(FLOWID_SEPARATOR).append(srcMac).append(FLOWID_SEPARATOR).append(dstMac)
                .append(FLOWID_SEPARATOR).append(dstVlan).toString();
    }

    public static String getFlowRef(final short tableId) {
        return new StringBuilder(FLOWREF_CAPACITY).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append("default")
                .append(FLOWID_SEPARATOR).append(tableId).toString();
    }

    public static String longToIp(String ip, short mask) {
        StringBuilder sb = new StringBuilder(DEFAULT_SB_CAPACITY);
        sb.append(ip).append("/").append(mask);
        return sb.toString();
    }

}

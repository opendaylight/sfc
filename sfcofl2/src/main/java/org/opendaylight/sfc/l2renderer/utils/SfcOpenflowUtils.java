/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.l2renderer.utils;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;


public class SfcOpenflowUtils {

    private static final int ETHERTYPE_VLAN = 0x8100;
    private static final int DEFAULT_SB_CAPACITY = 16;
    private static final int FLOWREF_CAPACITY = 256;
    private static final String FLOWID_PREFIX = "SFC";
    private static final String FLOWID_SEPARATOR = ".";

    public static Action createSetDlSrcAction(String mac, int order) {
        ActionBuilder ab = createActionBuilder(order);

        MacAddress addr = new MacAddress(mac);
        SetDlSrcActionBuilder actionBuilder = new SetDlSrcActionBuilder();
        SetDlSrcAction action = actionBuilder.setAddress(addr).build();
        ab.setAction(new SetDlSrcActionCaseBuilder().setSetDlSrcAction(action).build());
        return ab.build();
    }

    public static Action createSetDlDstAction(String mac, int order) {
        MacAddress macAddr = new MacAddress(mac);
        return new ActionBuilder()
                .setAction(
                        new SetFieldCaseBuilder().setSetField(
                                new SetFieldBuilder().setEthernetMatch(
                                        new EthernetMatchBuilder().setEthernetDestination(
                                                new EthernetDestinationBuilder().setAddress(macAddr).build()).build())
                                        .build()).build()).setOrder(order).setKey(new ActionKey(order)).build();

    }

    public static Action createOutputAction(Uri uri, int order) {
        ActionBuilder ab = createActionBuilder(order);
        OutputActionBuilder oab = new OutputActionBuilder();
        OutputAction action = oab
                .setOutputNodeConnector(uri)
                .build();
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(action).build());
        return ab.build();
    }

    public static Action createPushVlanAction(int order) {
        return new ActionBuilder()
                .setAction(
                        new PushVlanActionCaseBuilder().setPushVlanAction(
                                new PushVlanActionBuilder().setEthernetType(Integer.valueOf(ETHERTYPE_VLAN)).build()).build())
                .setOrder(order).setKey(new ActionKey(order)).build();
    }

    public static Action createSetDstVlanAction(int vlan, int order) {
        return new ActionBuilder()
                .setAction(
                        new SetFieldCaseBuilder().setSetField(
                                new SetFieldBuilder().setVlanMatch(
                                        new VlanMatchBuilder().setVlanId(
                                                new VlanIdBuilder().setVlanId(new VlanId(vlan)).setVlanIdPresent(true)
                                                        .build()).build()).build()).build()).setOrder(order)
                .setKey(new ActionKey(order)).build();

    }

    public static Action createPopVlanAction(int order) {
        return new ActionBuilder()
                .setAction(new PopVlanActionCaseBuilder().setPopVlanAction(new PopVlanActionBuilder().build()).build())
                .setOrder(order).setKey(new ActionKey(order)).build();
    }

    public static Action createSetGroupAction(long nextHopGroupId, int order) {
        ActionBuilder ab = createActionBuilder(order);

        GroupActionBuilder groupActionBuilder = new GroupActionBuilder();
        groupActionBuilder.setGroupId(nextHopGroupId);

        ab.setAction(new GroupActionCaseBuilder().setGroupAction(groupActionBuilder.build()).build());
        return ab.build();
    }

    private static ActionBuilder createActionBuilder(int order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setOrder(order);
        ab.setKey(new ActionKey(order));
        return ab;
    }

    public static VlanMatch createVlanMatch(int vlan) {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        VlanId vlanId = new VlanId(vlan);
        vlanIdBuilder.setVlanId(vlanId);
        vlanIdBuilder.setVlanIdPresent(true);
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());

        return vlanMatchBuilder.build();
    }

    // Only configure OpenFlow Capable SFFs
    public static boolean isSffOpenFlowCapable(final String sffName) {
        InstanceIdentifier<FlowCapableNode> nodeInstancIdentifier =
                InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, new NodeKey(new NodeId(sffName)))
                    .augmentation(FlowCapableNode.class)
                    .build();

        FlowCapableNode node = SfcDataStoreAPI.readTransactionAPI(nodeInstancIdentifier, LogicalDatastoreType.CONFIGURATION);
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

/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ofsfc.provider.utils;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.ofsfc.provider.OpenflowSfcRenderer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInput;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableOutput;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsInput;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsOutput;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeConnectorRemovedNotification;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeConnectorUpdatedNotification;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeRemovedNotification;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeUpdatedNotification;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.OpendaylightInventoryNotificationListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeDisconnectInputBuilder;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeDisconnectOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.SalTableService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.UpdateTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.table.update.UpdatedTableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOpenflowUtils {

    private static final Logger s_logger = LoggerFactory.getLogger(SfcOpenflowUtils.class);

    private SalFlowService m_salFlowService;

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
        OutputAction action = oab //
                .setOutputNodeConnector(uri) //
                .build();
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(action).build());
        return ab.build();
    }

    public static Action createPushVlanAction(int order) {
        return new ActionBuilder()
                .setAction(
                        new PushVlanActionCaseBuilder().setPushVlanAction(
                                new PushVlanActionBuilder().setEthernetType(Integer.valueOf(0x8100)).build()).build())
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

    public static FlowBuilder buildFlow(String flowId, short tableId, String flowName, BigInteger cookieValue,
            MatchBuilder matchBuilder, InstructionsBuilder instructionsBuilder, int flowPriority) {
        FlowBuilder newFlow = new FlowBuilder();
        newFlow.setId(new FlowId(flowId));
        newFlow.setKey(new FlowKey(new FlowId(flowId)));
        newFlow.setTableId(tableId);
        newFlow.setFlowName(flowName);
        newFlow.setCookie(new FlowCookie(cookieValue));
        newFlow.setCookieMask(new FlowCookie(cookieValue));
        newFlow.setContainerName(null);
        newFlow.setStrict(false);
        newFlow.setMatch(matchBuilder.build());
        newFlow.setInstructions(instructionsBuilder.build());
        newFlow.setPriority(flowPriority);
        newFlow.setHardTimeout(0);
        newFlow.setIdleTimeout(0);
        newFlow.setFlags(new FlowModFlags(false, false, false, false, false));
        if (null == newFlow.isBarrier()) {
            newFlow.setBarrier(Boolean.FALSE);
        }
        newFlow.setInstallHw(true);
        return newFlow;
    }

    public static BigInteger getMetadataSFP(long sfpId) {
        return (BigInteger.valueOf(sfpId).and(new BigInteger("FFFF", 16)));
    }

    public static BigInteger getCookieSFP(long sfpId) {
        return SfcOfL2Constants.COOKIE_SFC_BASE.add(new BigInteger("0120000", 16)).add(BigInteger.valueOf(sfpId));
    }

    public static BigInteger getCookieIngress(long vlanId) {
        return SfcOfL2Constants.COOKIE_SFC_BASE.add(new BigInteger("0130000", 16)).add(BigInteger.valueOf(vlanId));
    }

    public static BigInteger getCookieDefault(int tableId) {
        return SfcOfL2Constants.COOKIE_SFC_BASE.add(new BigInteger("0100000", 16)).add(BigInteger.valueOf(tableId));
    }
}

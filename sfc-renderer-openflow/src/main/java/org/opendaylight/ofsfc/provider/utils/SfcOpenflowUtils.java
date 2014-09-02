/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ofsfc.provider.utils;

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

public class SfcOpenflowUtils {

    public static Action createSetDlSrcAction(String mac, int order) {
        ActionBuilder ab = createActionBuilder(order);

        MacAddress addr = new MacAddress(mac);
        SetDlSrcActionBuilder actionBuilder = new SetDlSrcActionBuilder();
        SetDlSrcAction action = actionBuilder.setAddress(addr).build();
        ab.setAction(new SetDlSrcActionCaseBuilder().setSetDlSrcAction(action)
                .build());
        return ab.build();
    }

    public static Action createSetDlDstAction(String mac, int order) {
        ActionBuilder ab = createActionBuilder(order);

        MacAddress addr = new MacAddress(mac);
        return new ActionBuilder().setAction(
                new SetFieldCaseBuilder().setSetField(
                        new SetFieldBuilder().setEthernetMatch(
                                new EthernetMatchBuilder().setEthernetDestination(
                                        new EthernetDestinationBuilder().setAddress(addr).build())
                                        .build()).build()).build()).setKey(new ActionKey(0)).build();
    }

    public static Action createOutputAction(Uri uri, int order) {
        ActionBuilder ab = createActionBuilder(order);
        OutputActionBuilder oab = new OutputActionBuilder();
        OutputAction action = oab //
                .setOutputNodeConnector(uri) //
                .build();
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(action)
                .build());
        return ab.setKey(new ActionKey(0)).build();
    }

    public static Action createPushVlanAction(int order){
        ActionBuilder ab = createActionBuilder(order);
        return new ActionBuilder().setAction(
                new PushVlanActionCaseBuilder().setPushVlanAction(
                        new PushVlanActionBuilder().setEthernetType(
                                Integer.valueOf(0x8100)).build()).build()).setKey(new ActionKey(0)).build();
    }

    public static Action createSetDstVlanAction(int vlan, int order) {
        ActionBuilder ab = createActionBuilder(order);

        SetVlanIdActionBuilder vlanIdActionBuilder = new SetVlanIdActionBuilder();
        VlanId vlanId = new VlanId(vlan);
        vlanIdActionBuilder.setVlanId(vlanId);
        ab.setAction(new SetVlanIdActionCaseBuilder().setSetVlanIdAction(
                vlanIdActionBuilder.build()).build());
        return ab.setKey(new ActionKey(0)).build();

    }

    public static Action createPopVlanAction(int order){
        ActionBuilder ab = createActionBuilder(order);
        PopVlanActionBuilder popVlanActionBuilder = new PopVlanActionBuilder();
        ab.setAction(new PopVlanActionCaseBuilder().setPopVlanAction(popVlanActionBuilder.build()).build());
        return ab.setKey(new ActionKey(0)).build();
    }

    public static Action createSetGroupAction(long nextHopGroupId, int order) {
        ActionBuilder ab = createActionBuilder(order);

        GroupActionBuilder groupActionBuilder = new GroupActionBuilder();
        groupActionBuilder.setGroupId(nextHopGroupId);

        ab.setAction(new GroupActionCaseBuilder().setGroupAction(
                groupActionBuilder.build()).build());
        return ab.build();
    }

    private static ActionBuilder createActionBuilder(int order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setOrder(order);
        ab.setKey(new ActionKey(order));
        return ab;
    }

    public static VlanMatch createVlanMatch(int vlan)
    {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        VlanId vlanId = new VlanId(vlan);
        vlanIdBuilder.setVlanId(vlanId);
        vlanIdBuilder.setVlanIdPresent(true);
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());

        return vlanMatchBuilder.build();
    }
}

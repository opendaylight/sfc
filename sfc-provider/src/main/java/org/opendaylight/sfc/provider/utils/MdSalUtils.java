/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.utils;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;

public class MdSalUtils {

    public static Action createSetDlSrcAction(String mac, int order) {
        ActionBuilder ab = createActionBuilder(order);

        MacAddress addr = new MacAddress(mac);
        SetDlSrcActionBuilder actionBuilder = new SetDlSrcActionBuilder();
        SetDlSrcAction action = actionBuilder.setAddress(addr).build();
        ab.setAction(new SetDlSrcActionCaseBuilder().setSetDlSrcAction(action).build());
        return ab.build();
    }

    public static Action createSetDlDstAction(String mac, int order) {
        ActionBuilder ab = createActionBuilder(order);

        MacAddress addr = new MacAddress(mac);
        SetDlDstActionBuilder actionBuilder = new SetDlDstActionBuilder();
        SetDlDstAction action = actionBuilder.setAddress(addr).build();
        ab.setAction(new SetDlDstActionCaseBuilder().setSetDlDstAction(action).build());
        return ab.build();
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

    public static Action createSetDstVlanAction(int vlan, int order) {
        ActionBuilder ab = createActionBuilder(order);

        SetVlanIdActionBuilder vlanIdActionBuilder = new SetVlanIdActionBuilder();
        VlanId vlanId = new VlanId(vlan);
        vlanIdActionBuilder.setVlanId(vlanId);
        ab.setAction(new SetVlanIdActionCaseBuilder().setSetVlanIdAction(vlanIdActionBuilder.build()).build());
        return ab.build();

    }

    private static ActionBuilder createActionBuilder(int order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setOrder(order);
        ab.setKey(new ActionKey(order));
        return ab;
    }

}

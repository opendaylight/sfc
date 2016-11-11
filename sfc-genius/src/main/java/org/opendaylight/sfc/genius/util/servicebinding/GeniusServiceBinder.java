/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util.servicebinding;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class GeniusServiceBinder {

    public CompletableFuture<Void> bindService(WriteTransaction theTx,
                                                String theInterfaceName,
                                                short theServiceId,
                                                short theDestTable,
                                                BigInteger theCookie,
                                                int theServicePriority,
                                                String theServiceName) {

        InstanceIdentifier<BoundServices> id = InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(theInterfaceName, ServiceModeIngress.class))
                .child(BoundServices.class, new BoundServicesKey(theServiceId))
                .build();

        StypeOpenflow stypeOpenflow = new StypeOpenflowBuilder()
                .setFlowCookie(theCookie)
                .setFlowPriority(theServicePriority)
                .setInstruction(Collections.singletonList(MDSALUtil.buildAndGetGotoTableInstruction(theDestTable, 0)))
                .build();
        BoundServices boundServices = new BoundServicesBuilder()
                .setServiceName(theServiceName)
                .setServicePriority(theServiceId)
                .setServiceType(ServiceTypeFlowBased.class)
                .addAugmentation(StypeOpenflow.class, stypeOpenflow)
                .build();
        theTx.put(LogicalDatastoreType.CONFIGURATION, id, boundServices);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> unbindService(WriteTransaction theTx,
                                                 String theInterfaceName,
                                                 short theServiceId) {
        InstanceIdentifier<BoundServices> id = InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(theInterfaceName, ServiceModeIngress.class))
                .child(BoundServices.class, new BoundServicesKey(theServiceId))
                .build();
        theTx.delete(LogicalDatastoreType.CONFIGURATION, id);
        return CompletableFuture.completedFuture(null);
    }

    public static BigInteger getSfcIngressCookie() {
        return SfcGeniusConstants.COOKIE_SFC_INGRESS_TABLE;
    }

    public static int getSfcServicePriority() {
        return SfcGeniusConstants.SFC_SERVICE_PRIORITY;
    }
}

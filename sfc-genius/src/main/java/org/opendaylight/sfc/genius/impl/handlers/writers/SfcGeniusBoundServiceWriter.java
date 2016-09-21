/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper class to handle SFC service binding through the Genius interface
 * manager data store API.
 */
public class SfcGeniusBoundServiceWriter {

    private final WriteTransaction transaction;

    /**
     * Constructs a {@code SfcGeniusBoundServiceWriter} using the
     * provided {@link WriteTransaction} to perform the required data
     * store modifications.
     *
     * @param transaction the write transaction
     */
    public SfcGeniusBoundServiceWriter(WriteTransaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Bind SFC service from an interface.
     *
     * @param interfaceName the interface name.
     *
     * @return future signaling completion of the operation.
     */
    public CompletableFuture<Void> bindService(String interfaceName) {
        InstanceIdentifier<BoundServices> id = InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, ServiceModeIngress.class))
                .child(BoundServices.class, new BoundServicesKey(NwConstants.SFC_SERVICE_INDEX))
                .build();
        short offset = NwConstants.SFC_TRANSPORT_INGRESS_TABLE;
        StypeOpenflow stypeOpenflow = new StypeOpenflowBuilder()
                .setFlowCookie(SfcGeniusConstants.COOKIE_SFC_INGRESS_TABLE)
                .setFlowPriority(SfcGeniusConstants.SFC_SERVICE_PRIORITY)
                .setInstruction(Collections.singletonList(MDSALUtil.buildAndGetGotoTableInstruction(offset, 0)))
                .build();
        BoundServices boundServices = new BoundServicesBuilder()
                .setServiceName(NwConstants.SFC_SERVICE_NAME)
                .setServicePriority(NwConstants.SFC_SERVICE_INDEX)
                .setServiceType(ServiceTypeFlowBased.class)
                .addAugmentation(StypeOpenflow.class, stypeOpenflow)
                .build();
        transaction.put(LogicalDatastoreType.CONFIGURATION, id, boundServices);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Unbind SFC service from an interface.
     *
     * @param interfaceName the interface name.
     * @return future signaling completion of the operation.
     */
    public CompletableFuture<Void> unbindService(String interfaceName) {
        InstanceIdentifier<BoundServices> id = InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, ServiceModeIngress.class))
                .child(BoundServices.class, new BoundServicesKey(NwConstants.SFC_SERVICE_INDEX))
                .build();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, id);
        return CompletableFuture.completedFuture(null);
    }
}

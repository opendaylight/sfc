/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import java.util.concurrent.CompletableFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper class to handle the SFC bound service for an interface through
 * the Genius interface manager data store API.
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
     * Unbind SFC service from an interface.
     *
     * @param interfaceName the interface name.
     * @return future signaling completion of the operation.
     */
    public CompletableFuture<Void> unbindService(String interfaceName) {
        InstanceIdentifier<BoundServices> id = InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, ServiceModeIngress.class))
                .child(BoundServices.class, new BoundServicesKey(NwConstants.SCF_SERVICE_INDEX))
                .build();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, id);
        return CompletableFuture.completedFuture(null);
    }
}

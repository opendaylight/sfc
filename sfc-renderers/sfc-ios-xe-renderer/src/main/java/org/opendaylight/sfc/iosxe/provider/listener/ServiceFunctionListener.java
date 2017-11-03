/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.iosxe.provider.listener;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.iosxe.provider.renderer.IosXeServiceFunctionMapper;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class ServiceFunctionListener extends AbstractSyncDataTreeChangeListener<ServiceFunctions> {

    private final IosXeServiceFunctionMapper sfManager;

    @Inject
    public ServiceFunctionListener(DataBroker dataBroker, IosXeServiceFunctionMapper sfManager) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.builder(ServiceFunctions.class).build());
        this.sfManager = sfManager;
    }

    @Override
    public void add(@Nonnull ServiceFunctions serviceFunctions) {
        update(serviceFunctions, serviceFunctions);
    }

    @Override
    public void remove(@Nonnull ServiceFunctions serviceFunctions) {
        if (serviceFunctions.getServiceFunction() != null) {
            sfManager.syncFunctions(serviceFunctions.getServiceFunction(), true);
        }
    }

    @Override
    public void update(@Nonnull ServiceFunctions originalServiceFunctions, ServiceFunctions updatedServiceFunctions) {
        if (updatedServiceFunctions.getServiceFunction() != null) {
            sfManager.syncFunctions(originalServiceFunctions.getServiceFunction(), false);
        }
    }
}

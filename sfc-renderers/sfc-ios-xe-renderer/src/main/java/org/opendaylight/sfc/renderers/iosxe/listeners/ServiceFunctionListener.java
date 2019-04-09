/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.iosxe.listeners;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.renderers.iosxe.IosXeServiceFunctionMapper;
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
    public void add(@NonNull InstanceIdentifier<ServiceFunctions> instanceIdentifier,
                    @NonNull ServiceFunctions serviceFunctions) {
        update(instanceIdentifier, serviceFunctions, serviceFunctions);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<ServiceFunctions> instanceIdentifier,
                       @NonNull ServiceFunctions serviceFunctions) {
        if (serviceFunctions.getServiceFunction() != null) {
            sfManager.syncFunctions(serviceFunctions.getServiceFunction(), true);
        }
    }

    @Override
    public void update(@NonNull InstanceIdentifier<ServiceFunctions> instanceIdentifier,
                       @NonNull ServiceFunctions originalServiceFunctions,
                       @NonNull ServiceFunctions updatedServiceFunctions) {
        if (updatedServiceFunctions.getServiceFunction() != null) {
            sfManager.syncFunctions(originalServiceFunctions.getServiceFunction(), false);
        }
    }
}

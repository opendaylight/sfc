/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfgTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestSfgEntryDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionGroup> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfgEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestSfgEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.SFG_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@NonNull InstanceIdentifier<ServiceFunctionGroup> instanceIdentifier,
                    @NonNull ServiceFunctionGroup serviceFunctionGroup) {
        update(instanceIdentifier, serviceFunctionGroup, serviceFunctionGroup);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<ServiceFunctionGroup> instanceIdentifier,
                       @NonNull ServiceFunctionGroup serviceFunctionGroup) {
        LOG.debug("Deleted Service Function Name: {}", serviceFunctionGroup.getName());
        new SbRestSfgTask(RestOperation.DELETE, serviceFunctionGroup, executorService).run();
    }

    @Override
    public void update(@NonNull InstanceIdentifier<ServiceFunctionGroup> instanceIdentifier,
                       @NonNull ServiceFunctionGroup originalServiceFunctionGroup,
                       @NonNull ServiceFunctionGroup updatedServiceFunctionGroup) {
        LOG.debug("Modified Service Function Name: {}", updatedServiceFunctionGroup.getName());
        new SbRestSfgTask(RestOperation.PUT, updatedServiceFunctionGroup, executorService).run();
    }
}

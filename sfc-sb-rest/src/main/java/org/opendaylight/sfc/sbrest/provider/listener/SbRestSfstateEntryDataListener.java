/*
 * Copyright (c) 2015, 2017 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfstateTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestSfstateEntryDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionState> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfstateEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestSfstateEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.SFSTATE_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                    @Nonnull ServiceFunctionState serviceFunctionState) {
        update(instanceIdentifier, serviceFunctionState, serviceFunctionState);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                       @Nonnull ServiceFunctionState serviceFunctionState) {
        LOG.debug("Deleted Service Function State Name: {}", serviceFunctionState.getName());
        new SbRestSfstateTask(RestOperation.DELETE, serviceFunctionState, executorService).run();
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                       @Nonnull ServiceFunctionState originalServiceFunctionState,
                       @Nonnull ServiceFunctionState updatedServiceFunctionState) {
        LOG.debug("Updated Service Function State Name: {}", updatedServiceFunctionState.getName());
        new SbRestSfstateTask(RestOperation.PUT, updatedServiceFunctionState, executorService).run();
    }
}

/*
 * Copyright (c) 2015, 2017 Intel .Ltd, and others.  All rights reserved.
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
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfstTask;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestSfstEntryDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionSchedulerType> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfstEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestSfstEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.SFST_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@Nonnull ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        update(serviceFunctionSchedulerType, serviceFunctionSchedulerType);
    }

    @Override
    public void remove(@Nonnull ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        LOG.debug("Deleted Service Function Schedule Type Name: {}", serviceFunctionSchedulerType.getName());
        new SbRestSfstTask(RestOperation.DELETE, serviceFunctionSchedulerType, executorService).run();
    }

    @Override
    public void update(@Nonnull ServiceFunctionSchedulerType originalServiceFunctionSchedulerType,
                       @Nonnull ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType) {
        LOG.debug("Updated Service Function Schedule Type Name: {}", updatedServiceFunctionSchedulerType.getName());
        new SbRestSfstTask(RestOperation.PUT, updatedServiceFunctionSchedulerType, executorService).run();
    }
}

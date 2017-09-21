/*
 * Copyright (c) 2015, 2017 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfstTask;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestSfstEntryDataListener extends SbRestAbstractDataListener<ServiceFunctionSchedulerType> {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfstEntryDataListener.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public SbRestSfstEntryDataListener() {
        setInstanceIdentifier(SfcInstanceIdentifiers.SFST_ENTRY_IID);
    }

    public void setDataProvider(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceFunctionSchedulerType>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<ServiceFunctionSchedulerType> change: changes) {
            DataObjectModification<ServiceFunctionSchedulerType> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType = rootNode.getDataAfter();
                    LOG.debug("\nUpdated Service Function Schedule Type Name: {}",
                            updatedServiceFunctionSchedulerType.getName());

                    executor.execute(new SbRestSfstTask(RestOperation.PUT, updatedServiceFunctionSchedulerType,
                            executor));
                    break;
                case DELETE:
                    ServiceFunctionSchedulerType originalServiceFunctionSchedulerType = rootNode.getDataBefore();
                    LOG.debug("\nDeleted Service Function Schedule Type Name: {}",
                            originalServiceFunctionSchedulerType.getName());

                    executor.execute(new SbRestSfstTask(RestOperation.DELETE, originalServiceFunctionSchedulerType,
                            executor));
                    break;
                default:
                    break;
            }
        }

        printTraceStop(LOG);
    }
}

/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfstTask;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestSfstEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfstEntryDataListener.class);

    public SbRestSfstEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SFST_ENTRY_IID);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionSchedulerType) {
                ServiceFunctionSchedulerType originalServiceFunctionScheduleType = (ServiceFunctionSchedulerType) entry.getValue();
                LOG.debug("\nOriginal Service Function Schedule Type Name: {}", originalServiceFunctionScheduleType.getName());
            }
        }

        // SF Schedule Type CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionSchedulerType) {
                ServiceFunctionSchedulerType createdServiceFunctionScheduleType = (ServiceFunctionSchedulerType) entry.getValue();
                LOG.debug("\nCreated Service Function Schedule Type Name: {}", createdServiceFunctionScheduleType.getName());

                Runnable task = new SbRestSfstTask(RestOperation.PUT, createdServiceFunctionScheduleType, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        // SF Schedule Type UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionSchedulerType)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType = (ServiceFunctionSchedulerType) entry.getValue();
                LOG.debug("\nModified Service Function Schedule Type Name: {}", updatedServiceFunctionSchedulerType.getName());

                Runnable task = new SbRestSfstTask(RestOperation.PUT, updatedServiceFunctionSchedulerType, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        // SF Schedule Type DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionSchedulerType) {

                ServiceFunctionSchedulerType originalServiceFunctionSchedulerType = (ServiceFunctionSchedulerType) dataObject;
                LOG.debug("\nDeleted Service Function Schedule Type Name: {}", originalServiceFunctionSchedulerType.getName());

                Runnable task = new SbRestSfstTask(RestOperation.DELETE, originalServiceFunctionSchedulerType, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        printTraceStop(LOG);
    }
}

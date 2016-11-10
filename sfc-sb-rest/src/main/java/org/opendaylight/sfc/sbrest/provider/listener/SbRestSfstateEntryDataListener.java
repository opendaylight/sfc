/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfstateTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestSfstateEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfstateEntryDataListener.class);
    protected static ExecutorService executor = Executors.newFixedThreadPool(5);

    public SbRestSfstateEntryDataListener() {
        setInstanceIdentifier(SfcInstanceIdentifiers.SFSTATE_ENTRY_IID);
    }

    public void setDataProvider(DataBroker r){
       setDataBroker(r);
       registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionState) {
                ServiceFunctionState originalServiceFunctionState = (ServiceFunctionState) entry.getValue();
                LOG.debug("\nOriginal Service Function State Name: {}", originalServiceFunctionState.getName());
            }
        }

        // Service Function State CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionState) {
                ServiceFunctionState createdServiceFunctionState = (ServiceFunctionState) entry.getValue();
                LOG.debug("\nCreated Service Function State Name: {}", createdServiceFunctionState.getName());

                Runnable task = new SbRestSfstateTask(RestOperation.PUT, createdServiceFunctionState, executor);
                executor.submit(task);
            }
        }

        // Service Function State UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionState)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionState updatedServiceFunctionState = (ServiceFunctionState) entry.getValue();
                LOG.debug("\nModified Service Function State Name: {}", updatedServiceFunctionState.getName());

                Runnable task = new SbRestSfstateTask(RestOperation.PUT, updatedServiceFunctionState, executor);
                executor.submit(task);
            }
        }

        // Service Function State DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionState) {

                ServiceFunctionState originalServiceFunctionState = (ServiceFunctionState) dataObject;
                LOG.debug("\nDeleted Service Function State Name: {}", originalServiceFunctionState.getName());

                Runnable task = new SbRestSfstateTask(RestOperation.DELETE, originalServiceFunctionState, executor);
                executor.submit(task);
            }
        }

        printTraceStop(LOG);
    }
}

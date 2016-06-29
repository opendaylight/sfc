/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfgTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestSfgEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfgEntryDataListener.class);

    public SbRestSfgEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SFG_ENTRY_IID);
        registerAsDataChangeListener();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionGroup) {
                ServiceFunctionGroup originalServiceFunction = (ServiceFunctionGroup) entry.getValue();
                LOG.debug("\nOriginal Service Function Group Name: {}", originalServiceFunction.getName());
            }
        }

        // SFG CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionGroup) {
                ServiceFunctionGroup createdServiceFunctionGroup = (ServiceFunctionGroup) entry.getValue();
                LOG.debug("\nCreated Service Function Group Name: {}", createdServiceFunctionGroup.getName());

                Runnable task = new SbRestSfgTask(RestOperation.PUT, createdServiceFunctionGroup, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        // SFG UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionGroup) && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionGroup updatedServiceFunctionGroup = (ServiceFunctionGroup) entry.getValue();
                LOG.debug("\nModified Service Function Name: {}", updatedServiceFunctionGroup.getName());

                Runnable task = new SbRestSfgTask(RestOperation.PUT, updatedServiceFunctionGroup, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        // SFG DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionGroup) {

                ServiceFunctionGroup originalServiceFunctionGroup = (ServiceFunctionGroup) dataObject;
                LOG.debug("\nDeleted Service Function Name: {}", originalServiceFunctionGroup.getName());

                Runnable task = new SbRestSfgTask(RestOperation.DELETE, originalServiceFunctionGroup, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }
        printTraceStop(LOG);
    }

}

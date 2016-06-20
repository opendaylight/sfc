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
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestSfEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfEntryDataListener.class);

    public SbRestSfEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SF_ENTRY_IID);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                LOG.debug("\nOriginal Service Function Name: {}", originalServiceFunction.getName());
            }
        }

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();


        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();
                LOG.debug("\nCreated Service Function Name: {}", createdServiceFunction.getName());

                Runnable task = new SbRestSfTask(RestOperation.PUT, createdServiceFunction, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        // SF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunction)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();
                LOG.debug("\nModified Service Function Name: {}", updatedServiceFunction.getName());

                Runnable task = new SbRestSfTask(RestOperation.PUT, updatedServiceFunction, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }


        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunction) {

                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                LOG.debug("\nDeleted Service Function Name: {}", originalServiceFunction.getName());

                Runnable task = new SbRestSfTask(RestOperation.DELETE, originalServiceFunction, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }
        printTraceStop(LOG);
    }


}

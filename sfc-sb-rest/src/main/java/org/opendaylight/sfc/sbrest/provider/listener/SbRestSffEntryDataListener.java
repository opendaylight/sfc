/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSffTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestSffEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSffEntryDataListener.class);

    public SbRestSffEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SFF_ENTRY_IID);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder originalServiceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\n########## Original Sff: {}",
                        originalServiceFunctionForwarder.getName());
            }
        }

        // SFF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();


        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder createdServiceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("*** created Sff: {}", createdServiceFunctionForwarder.getName());

                Runnable task = new SbRestSffTask(RestOperation.POST, createdServiceFunctionForwarder, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        // SFF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionForwarder)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionForwarder updatedServiceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\n########## Modified Sff Name {}",
                        updatedServiceFunctionForwarder.getName());

                Runnable task = new SbRestSffTask(RestOperation.PUT, updatedServiceFunctionForwarder, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }


        /* I'm not sure this should be possible
        // SFF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionForwarder) {

                ServiceFunctionForwarder originalServiceFunctionForwarder = (ServiceFunctionForwarder) dataObject;
                LOG.error("XXXXXXX Sff Name is {}", originalServiceFunctionForwarder.getName());

                Runnable task = new SbRestSffTask(RestOperation.DELETE, originalServiceFunctionForwarder, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);

            }
        }
        */
        printTraceStop(LOG);
    }


}

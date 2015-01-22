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
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

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

        System.out.println("\n***SB REST sf listener***\n");
        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                System.out.println("*** sb-Original Service function: " +
                        originalServiceFunction.getName());
                LOG.debug("\n########## Original Service function: {}",
                        originalServiceFunction.getName());
            }
        }

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        System.out.println("*** entrySet size:"+dataCreatedObject.entrySet().size());

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();
                LOG.debug("*** created Service Function: {}", createdServiceFunction.getName());
                System.out.println("*** sb-created Service Function: " + createdServiceFunction.getName());

                // Deprecated, this cycle goes to SbRest*Task
                /*
                Object result = null;
                try {
                    result = opendaylightSfc.getExecutor().submit(SfcProviderServiceForwarderAPI
                            .getReadAll(new Object[]{}, new Class[]{})).get();
                    ServiceFunctionForwarders serviceFunctionForwarders = (ServiceFunctionForwarders) result;

                    for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarders.getServiceFunctionForwarder()) {
                        Uri uri = serviceFunctionForwarder.getRestUri();
                        //String urlMgmt = uri.getValue();
                        String urlMgmt = "127.0.0.100";
                        LOG.info("PUT url:{}", urlMgmt);
                        //SbRestPutSfTask putSfTask = new SbRestPutSfTask(createdServiceFunction, urlMgmt); // Deprecated
                        SbRestSfTask task = new SbRestSfTask(RestOperation.POST, createdServiceFunction, opendaylightSfc.getExecutor());
                        opendaylightSfc.getExecutor().submit(task);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                */
                Runnable task = new SbRestSfTask(RestOperation.POST, createdServiceFunction, opendaylightSfc.getExecutor());
                System.out.println("*** submitting task: " + RestOperation.POST + " " + createdServiceFunction.getName());
                opendaylightSfc.getExecutor().submit(task);
                System.out.println("*** task submitted");
            }
        }

        // SF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunction)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();
                LOG.debug("\n########## Modified Service Function Name {}",
                        updatedServiceFunction.getName());
                System.out.println("*** sb-updated Service Function: " + updatedServiceFunction.getName());

                Runnable task = new SbRestSfTask(RestOperation.PUT, updatedServiceFunction, opendaylightSfc.getExecutor());
                System.out.println("*** submitting task: " + RestOperation.PUT + " " + updatedServiceFunction.getName());
                opendaylightSfc.getExecutor().submit(task);
                System.out.println("*** task submitted");
            }
        }


        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunction) {

                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                LOG.error("XXXXXXX Service Function Name is {}", originalServiceFunction.getName());

                System.out.println("*** sb-deleted Service Function: " + originalServiceFunction.getName());

                Runnable task = new SbRestSfTask(RestOperation.DELETE, originalServiceFunction, opendaylightSfc.getExecutor());
                System.out.println("*** submitting task: " + RestOperation.DELETE + " " + originalServiceFunction.getName());
                opendaylightSfc.getExecutor().submit(task);
                System.out.println("*** task submitted");

            }
        }
        printTraceStop(LOG);
    }


}

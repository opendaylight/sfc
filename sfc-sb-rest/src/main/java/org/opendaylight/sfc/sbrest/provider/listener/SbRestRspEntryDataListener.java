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
import org.opendaylight.sfc.sbrest.provider.task.SbRestRspTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestRspEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfEntryDataListener.class);

    public SbRestRspEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SF_ENTRY_IID);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        System.out.println("\n***SB REST RSP listener***\n");
        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath originalRenderedServicePath = (RenderedServicePath) entry.getValue();
                System.out.println("*** sb-Original RSP: " +
                        originalRenderedServicePath.getName());
                LOG.debug("\n########## Original RSP: {}",
                        originalRenderedServicePath.getName());
            }
        }

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        System.out.println("*** entrySet size:" + dataCreatedObject.entrySet().size());

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath createdRenderedServicePath = (RenderedServicePath) entry.getValue();
                LOG.debug("*** created RSP: {}", createdRenderedServicePath.getName());
                System.out.println("*** sb-created RSP: " + createdRenderedServicePath.getName());

                Runnable task = new SbRestRspTask(RestOperation.POST, createdRenderedServicePath, opendaylightSfc.getExecutor());
                System.out.println("*** submitting task: " + RestOperation.POST + " " + createdRenderedServicePath.getName());
                opendaylightSfc.getExecutor().submit(task);
                System.out.println("*** task submitted");
            }
        }

        // SF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof RenderedServicePath)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                RenderedServicePath updatedRenderedServicePath = (RenderedServicePath) entry.getValue();
                LOG.debug("\n########## Modified RSP Name {}",
                        updatedRenderedServicePath.getName());
                System.out.println("*** sb-updated RSP: " + updatedRenderedServicePath.getName());

                Runnable task = new SbRestRspTask(RestOperation.PUT, updatedRenderedServicePath, opendaylightSfc.getExecutor());
                System.out.println("*** submitting task: " + RestOperation.PUT + " " + updatedRenderedServicePath.getName());
                opendaylightSfc.getExecutor().submit(task);
                System.out.println("*** task submitted");
            }
        }


        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof RenderedServicePath) {

                RenderedServicePath originalRenderedServicePath = (RenderedServicePath) dataObject;
                LOG.error("XXXXXXX RSP Name is {}", originalRenderedServicePath.getName());

                System.out.println("*** sb-deleted RSP: " + originalRenderedServicePath.getName());

                Runnable task = new SbRestRspTask(RestOperation.DELETE, originalRenderedServicePath, opendaylightSfc.getExecutor());
                System.out.println("*** submitting task: " + RestOperation.DELETE + " " + originalRenderedServicePath.getName());
                opendaylightSfc.getExecutor().submit(task);
                System.out.println("*** task submitted");

            }
        }
        printTraceStop(LOG);
    }


}

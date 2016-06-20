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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestRspTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestRspEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestRspEntryDataListener.class);

    public SbRestRspEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.RSP_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath originalRenderedServicePath = (RenderedServicePath) entry.getValue();
                LOG.debug("\nOriginal Rendered Service Path: {}", originalRenderedServicePath.getName());
            }
        }

        // RSP CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath createdRenderedServicePath = (RenderedServicePath) entry.getValue();
                LOG.debug("\nCreated Rendered Service Path: {}", createdRenderedServicePath.getName());

                Runnable task = new SbRestRspTask(RestOperation.POST, createdRenderedServicePath, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }

        // RSP UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof RenderedServicePath)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                RenderedServicePath updatedRenderedServicePath = (RenderedServicePath) entry.getValue();
                LOG.debug("\nModified Rendered Service Path Name: {}", updatedRenderedServicePath.getName());

                Runnable task = new SbRestRspTask(RestOperation.PUT, updatedRenderedServicePath, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }


        // RSP DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof RenderedServicePath) {

                RenderedServicePath originalRenderedServicePath = (RenderedServicePath) dataObject;
                LOG.debug("\nDeleted Rendered Service Path Name: {}", originalRenderedServicePath.getName());

                Runnable task = new SbRestRspTask(RestOperation.DELETE, originalRenderedServicePath, opendaylightSfc.getExecutor());
                opendaylightSfc.getExecutor().submit(task);
            }
        }
        printTraceStop(LOG);
    }


}

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestAclTask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestAclEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestAclEntryDataListener.class);
    protected static ExecutorService executor = Executors.newFixedThreadPool(10);

    public SbRestAclEntryDataListener() {
        setInstanceIdentifier(SfcInstanceIdentifiers.ACL_ENTRY_IID);
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
            if (entry.getValue() instanceof Acl) {
                Acl originalAcl = (Acl) entry.getValue();
                LOG.debug("\nOriginal Access List Name: {}", originalAcl.getAclName());
            }
        }

        // ACL CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof Acl) {
                Acl createdAcl = (Acl) entry.getValue();
                LOG.debug("\nCreated Access List Name: {}", createdAcl.getAclName());

                Runnable task = new SbRestAclTask(RestOperation.POST, createdAcl, executor);
                executor.submit(task);
            }
        }

        // ACL UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof Acl)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                Acl updatedAcl = (Acl) entry.getValue();
                LOG.debug("\nModified Access List Name: {}", updatedAcl.getAclName());

                Runnable task = new SbRestAclTask(RestOperation.PUT, updatedAcl, executor);
                executor.submit(task);
            }
        }

        // ACL DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof Acl) {

                Acl originalAcl = (Acl) dataObject;
                LOG.debug("\nDeleted Access List Name: {}", originalAcl.getAclName());

                Runnable task = new SbRestAclTask(RestOperation.DELETE, originalAcl, executor);
                executor.submit(task);
            }
        }
        printTraceStop(LOG);
    }


}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcAbstractDataListener;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestAclTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestScfEntryDataListener extends SfcAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestScfEntryDataListener.class);

    public SbRestScfEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SCF_ENTRY_IID);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier originalServiceClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Original Service Classifier: {}",
                        originalServiceClassifier.getName());
            }
        }

        // Classifier CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier createdServiceClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Created Service Classifier Name: {}", createdServiceClassifier.getName());

                if (createdServiceClassifier.getAccessList() != null && !createdServiceClassifier.getAccessList().isEmpty()){
                    AccessList accessList = SfcProviderAclAPI.readAccessListExecutor(createdServiceClassifier.getAccessList());

                    Runnable task = new SbRestAclTask(RestOperation.POST, accessList,
                            createdServiceClassifier.getSclServiceFunctionForwarder(), opendaylightSfc.getExecutor());
                    opendaylightSfc.getExecutor().submit(task);
                }
            }
        }

        // Classifier UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionClassifier)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionClassifier updatedServiceClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\n########## Modified Service Classifier Name: {}", updatedServiceClassifier.getName());

                if (updatedServiceClassifier.getAccessList() != null && !updatedServiceClassifier.getAccessList().isEmpty()){
                    AccessList accessList = SfcProviderAclAPI.readAccessListExecutor(updatedServiceClassifier.getAccessList());

                    Runnable task = new SbRestAclTask(RestOperation.PUT, accessList,
                            updatedServiceClassifier.getSclServiceFunctionForwarder(), opendaylightSfc.getExecutor());
                    opendaylightSfc.getExecutor().submit(task);
                }
            }
        }

        // Classifier DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionClassifier) {

                ServiceFunctionClassifier deletedServiceClassifier = (ServiceFunctionClassifier) dataObject;
                LOG.debug("\n########## Deleted Service Classifier Name: {}", deletedServiceClassifier.getName());

                if (deletedServiceClassifier.getAccessList() != null && !deletedServiceClassifier.getAccessList().isEmpty()){
                    AccessList accessList = SfcProviderAclAPI.readAccessListExecutor(deletedServiceClassifier.getAccessList());

                    Runnable task = new SbRestAclTask(RestOperation.PUT, accessList,
                            deletedServiceClassifier.getSclServiceFunctionForwarder(), opendaylightSfc.getExecutor());
                    opendaylightSfc.getExecutor().submit(task);
                }
            }
        }
        printTraceStop(LOG);
    }


}

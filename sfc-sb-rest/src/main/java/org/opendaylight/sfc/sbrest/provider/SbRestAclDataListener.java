/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider;

import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestAclDataListener extends SbRestAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestAclDataListener.class);

    public SbRestAclDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.ACL_IID);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof AccessLists) {

                AccessLists updatedAccessLists =
                        (AccessLists) entry.getValue();

/*                DataBroker dataBroker = getDataBroker();
                ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
                Optional<ServiceFunctionClassifiers> serviceFunctionClassifiersObject = null;
                List<ServiceFunctionClassifier> serviceFunctionClassifierList = new ArrayList<>();
                try {
                    serviceFunctionClassifiersObject =
                            readTx.read(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.SCF_IID).get();
                    if (serviceFunctionClassifiersObject != null) {
                        serviceFunctionClassifierList =
                                serviceFunctionClassifiersObject.get().getServiceFunctionClassifier();
                    } else {
                        LOG.error("\n########## Failed to get Service Function Classifiers: {}",
                                Thread.currentThread().getStackTrace()[1]);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("\n########## Failed to get Service Function Classifiers: {}",
                            e.getMessage());
                }

                for (ServiceFunctionClassifier serviceFunctionClassifier : serviceFunctionClassifierList) {
                    for (org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.
                            service.function.classifiers.service.function.classifier.ServiceFunctionForwarder serviceFunctionForwarderRef
                            : serviceFunctionClassifier.getServiceFunctionForwarder()) {

                        Object[] serviceForwarderObj = {serviceFunctionForwarderRef.getName()};
                        Class[] serviceForwarderClass = {String.class};
                        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI =
                                SfcProviderServiceForwarderAPI.getRead(serviceForwarderObj, serviceForwarderClass);

                        Future<Object> future = opendaylightSfc.getExecutor().submit(sfcProviderServiceForwarderAPI);
                        ServiceFunctionForwarder serviceFunctionForwarder = null;
                        try {
                            serviceFunctionForwarder = (ServiceFunctionForwarder) future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }

                        if (serviceFunctionForwarder != null) {
                            Uri uri = serviceFunctionForwarder.getRestUri();
                            String urlMgmt = uri.getValue();
                            LOG.info("PUT ACL to url: {}", urlMgmt);
                            SbRestPutAclTask putAclTask = new SbRestPutAclTask(serviceFunctionClassifier.getAccessList(), urlMgmt);
                            opendaylightSfc.getExecutor().submit(putAclTask);
                        }
                    }
                }*/

            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
}

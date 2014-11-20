/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sbrest.provider;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SbRestSfpDataListener extends SbRestAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSffDataListener.class);

    public SbRestSfpDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.sfpIID);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        boolean isValid = true;

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet())
        {
            if (entry.getValue() instanceof ServiceFunctionPaths) {

                ServiceFunctionPaths updatedServiceFunctionPaths =
                        (ServiceFunctionPaths) entry.getValue();

                DataBroker dataBroker = getDataBroker();
                ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
                Optional<ServiceFunctionForwarders> serviceFunctionForwardersObject = null;
                List<ServiceFunctionForwarder> serviceFunctionForwarderList = new ArrayList<>();
                try {
                    serviceFunctionForwardersObject =
                            readTx.read(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.sffIID).get();
                    if (serviceFunctionForwardersObject != null) {
                        serviceFunctionForwarderList =
                                serviceFunctionForwardersObject.get().getServiceFunctionForwarder();
                    } else {
                        LOG.error("\n########## Failed to get Service Function Forwarders: {}",
                                Thread.currentThread().getStackTrace()[1]);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("\n########## Failed to get Service Function Forwarders: {}",
                            e.getMessage());
                }

                for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
                    Uri uri = serviceFunctionForwarder.getRestUri();
                    String urlMgmt = uri.getValue();
                    System.out.println("PUT url:" + urlMgmt);
                    SbRestPutSfpTask putSfpTask = new SbRestPutSfpTask(updatedServiceFunctionPaths, urlMgmt);
                    opendaylightSfc.executor.submit(putSfpTask);
                }

            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
}

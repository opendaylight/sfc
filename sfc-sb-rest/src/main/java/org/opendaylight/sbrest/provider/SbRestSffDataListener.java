/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sbrest.provider;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SbRestSffDataListener extends SbRestAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSffDataListener.class);

    public SbRestSffDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.sffIID);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        boolean isValid = true;

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionForwarders) {

                ServiceFunctionForwarders updatedServiceFunctionForwarders =
                        (ServiceFunctionForwarders) entry.getValue();
                List<ServiceFunctionForwarder> serviceFunctionForwarderList =
                        updatedServiceFunctionForwarders.getServiceFunctionForwarder();
                for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
                    isValid = isValid
                            && serviceFunctionForwarder.getName() != null
                            && serviceFunctionForwarder.getServiceFunctionDictionary() != null;
                }
                if (isValid) {
                    for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
                        Uri uri = serviceFunctionForwarder.getRestUri();
                        String urlMgmt = uri.getValue();
                        System.out.println("PUT url:" + urlMgmt);
                        SbRestPutSffTask putSffTask = new SbRestPutSffTask(updatedServiceFunctionForwarders, urlMgmt);
                        opendaylightSfc.executor.submit(putSffTask);
                    }
                }
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
}

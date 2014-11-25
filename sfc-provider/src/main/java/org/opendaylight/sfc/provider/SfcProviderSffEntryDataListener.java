/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class is the DataListener for SFF changes.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderSffEntryDataListener implements DataChangeListener  {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSffEntryDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        printTraceStart(LOG);

        // SF ORIGINAL
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunctionForwarder)
            {
                ServiceFunctionForwarder originalServiceFunctionForwarder =
                        (ServiceFunctionForwarder) entry.getValue();
            }
        }

        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder delServiceFunctionForwarder = (ServiceFunctionForwarder) dataObject;
                Object[] serviceForwarderObj = {delServiceFunctionForwarder};
                Class[] serviceForwarderClass = {ServiceFunctionForwarder.class};

                SfcProviderRestAPI sfcProviderRestAPI = SfcProviderRestAPI
                        .getDeleteServiceFunctionForwarder(serviceForwarderObj, serviceForwarderClass);
                odlSfc.executor.submit(sfcProviderRestAPI);

                // SFF deletion is a critical event. If a SFF is deleted we delete all associated SFPs
                serviceForwarderObj[0] = delServiceFunctionForwarder;
                serviceForwarderClass[0] = ServiceFunctionForwarder.class;
                SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                        .getDeletePathsUsedByServiceForwarder(serviceForwarderObj, serviceForwarderClass);
                odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
            }
        }

        // SFF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder createdServiceFunctionForwarder =
                        (ServiceFunctionForwarder) entry.getValue();
                Object[] serviceForwarderObj = {createdServiceFunctionForwarder};
                Class[] serviceForwarderClass = {ServiceFunctionForwarder.class};

                //Send to SB REST
                SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                        .getCheckServiceForwarderAPI(serviceForwarderObj, serviceForwarderClass);
                odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
            }
        }

        printTraceStop(LOG);
    }

}

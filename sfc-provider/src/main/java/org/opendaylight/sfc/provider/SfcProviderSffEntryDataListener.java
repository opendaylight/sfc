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
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        printTraceStart(LOG);
        ODL_SFC.getLock();

        // SFF ORIGINAL
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunctionForwarder)
            {
                ServiceFunctionForwarder originalServiceFunctionForwarder =
                        (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("Original SFF: {}", originalServiceFunctionForwarder.getName());
            }
        }

        // SFF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) dataObject;

                /* Before removing RSPs used by this Service Function, we need to remove all
                 * references in the SFF/SF operational trees
                 */

                String sffName = serviceFunctionForwarder.getName();
                List<String> rspList = new ArrayList<>();
                List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffStateExecutor(sffName);
                if ((sffServicePathList != null) && !sffServicePathList.isEmpty()) {
                    if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderStateExecutor(sffName)) {

                    } else {
                        LOG.error("{}: Failed to delete SFF {} operational state",
                                Thread.currentThread().getStackTrace()[1], sffName);
                    }
                    for (SffServicePath sffServicePath : sffServicePathList) {
                        String rspName = sffServicePath.getName();
                        SfcProviderServiceFunctionAPI
                                .deleteServicePathFromServiceFunctionStateExecutor(rspName);
                        rspList.add(rspName);
                    }
                    SfcProviderRenderedPathAPI.deleteRenderedServicePathsExecutor(rspList);
                }

                //REST
                Object[] serviceForwarderObj = {serviceFunctionForwarder};
                Class[] serviceForwarderClass = {ServiceFunctionForwarder.class};
                SfcProviderRestAPI sfcProviderRestAPI = SfcProviderRestAPI
                        .getDeleteServiceFunctionForwarder(serviceForwarderObj, serviceForwarderClass);
                ODL_SFC.getExecutor().submit(sfcProviderRestAPI);
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
                ODL_SFC.getExecutor().submit(sfcProviderServiceForwarderAPI);
            }
        }

        // SFF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject
                = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionForwarder) &&
                    (!(dataCreatedObject.containsKey(entry.getKey())))) {

                ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();

                /* Before removing RSPs used by this Service Function, we need to remove all
                 * references in the SFF/SF operational trees
                 */

                String sffName = serviceFunctionForwarder.getName();
                SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderStateExecutor(sffName);
                List<String> rspList = new ArrayList<>();
                List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffStateExecutor(sffName);
                if ((sffServicePathList != null) && !sffServicePathList.isEmpty()) {
                    for (SffServicePath sffServicePath : sffServicePathList) {
                        String rspName = sffServicePath.getName();
                        SfcProviderServiceFunctionAPI
                                .deleteServicePathFromServiceFunctionStateExecutor(rspName);
                        rspList.add(rspName);
                    }
                    SfcProviderRenderedPathAPI.deleteRenderedServicePathsExecutor(rspList);
                }

                //Send to SB REST
                Object[] serviceForwarderObj = {serviceFunctionForwarder};
                Class[] serviceForwarderClass = {ServiceFunctionForwarder.class};
                SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                        .getCheckServiceForwarderAPI(serviceForwarderObj, serviceForwarderClass);
                ODL_SFC.getExecutor().submit(sfcProviderServiceForwarderAPI);
            }
        }
        ODL_SFC.releaseLock();
        printTraceStop(LOG);
    }

}

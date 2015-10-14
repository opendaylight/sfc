/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.api.SfcConcurrencyAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the DataListener for SFF changes.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
public class SfcProviderSffEntryDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSffEntryDataListener.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);
        if (SfcConcurrencyAPI.getLock()) {
            try {

                // SFF ORIGINAL

                Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
                    if (entry.getValue() instanceof ServiceFunctionForwarder) {
                        ServiceFunctionForwarder originalServiceFunctionForwarder =
                                (ServiceFunctionForwarder) entry.getValue();
                        LOG.debug("Original SFF: {}", originalServiceFunctionForwarder.getName());
                    }
                }

                // SFF DELETION
                Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
                for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
                    DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
                    if (dataObject instanceof ServiceFunctionForwarder) {
                        ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) dataObject;

                        /*
                         * Before removing RSPs used by this Service Function, we need to remove all
                         * references in the SFF/SF operational trees
                         */

                        LOG.debug("{}: SFF {} deletion", Thread.currentThread().getStackTrace()[1],
                                serviceFunctionForwarder.getName());
                        String sffName = serviceFunctionForwarder.getName();
                        List<String> rspList = new ArrayList<>();
                        List<SffServicePath> sffServicePathList =
                                SfcProviderServiceForwarderAPI.readSffState(sffName);
                        if ((sffServicePathList != null) && !sffServicePathList.isEmpty()) {
                            if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderState(sffName)) {

                            } else {
                                LOG.error("{}: Failed to delete SFF {} operational state",
                                        Thread.currentThread().getStackTrace()[1], sffName);
                            }
                            for (SffServicePath sffServicePath : sffServicePathList) {
                                String rspName = sffServicePath.getName();
                                SfcProviderServiceFunctionAPI
                                    .deleteServicePathFromServiceFunctionState(rspName);
                                rspList.add(rspName);
                            }
                            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
                        }
                    }
                }

                // SFF CREATION

                Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
                    if (entry.getValue() instanceof ServiceFunctionForwarder) {
                        ServiceFunctionForwarder createdServiceFunctionForwarder =
                                (ServiceFunctionForwarder) entry.getValue();
                        LOG.debug("{}: SFF {} create", Thread.currentThread().getStackTrace()[1],
                                createdServiceFunctionForwarder.getName());

                    }
                }

                // SFF UPDATE
                Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
                for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
                    if ((entry.getValue() instanceof ServiceFunctionForwarder)
                            && (!(dataCreatedObject.containsKey(entry.getKey())))) {

                        ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();

                        /*
                         * Before removing RSPs used by this Service Function, we need to remove all
                         * references in the SFF/SF operational trees
                         */

                        String sffName = serviceFunctionForwarder.getName();
                        LOG.debug("{}: SFF {} update", Thread.currentThread().getStackTrace()[1], sffName);
                        List<SffServicePath> sffServicePathList =
                                SfcProviderServiceForwarderAPI.readSffState(sffName);
                        List<String> rspList = new ArrayList<>();
                        if ((sffServicePathList != null) && !sffServicePathList.isEmpty()) {
                            if (!SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderState(sffName)) {
                                LOG.error("{}: Failed to delete SFF {} operational state",
                                        Thread.currentThread().getStackTrace()[1], sffName);
                            }
                            for (SffServicePath sffServicePath : sffServicePathList) {
                                String rspName = sffServicePath.getName();
                                SfcProviderServiceFunctionAPI
                                    .deleteServicePathFromServiceFunctionState(rspName);
                                rspList.add(rspName);
                            }
                            SfcProviderRenderedPathAPI.deleteRenderedServicePaths(rspList);
                        }

                    }
                }
            } finally {
                SfcConcurrencyAPI.releaseLock();
            }
        } else {
            LOG.error("{}: Failed to Acquire Lock", Thread.currentThread().getStackTrace()[1]);
        }
        printTraceStop(LOG);
    }

}

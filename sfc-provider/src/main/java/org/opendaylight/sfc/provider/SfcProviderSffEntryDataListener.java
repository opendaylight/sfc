/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.md.features.rev151010.SffVxlanClassifierType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.md.features.rev151010.service.function.forwarders.service.function.forwarder.VxlanClassifierType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

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
                        SffName sffName = serviceFunctionForwarder.getName();
                        List<RspName> rspList = new ArrayList<>();
                        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
                        if ((sffServicePathList != null) && !sffServicePathList.isEmpty()) {
                            if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderState(sffName)) {

                            } else {
                                LOG.error("{}: Failed to delete SFF {} operational state",
                                        Thread.currentThread().getStackTrace()[1], sffName);
                            }
                            for (SffServicePath sffServicePath : sffServicePathList) {
                                // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah

                                RspName rspName = new RspName(sffServicePath.getName().getValue());
                                // XXX Another example of Method Overloading confusion brought about
                                // by Strings
                                SfcProviderServiceFunctionAPI
                                    .deleteServicePathFromServiceFunctionState(new SfpName(rspName.getValue()));
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
                        SffVxlanClassifierType1 sffVxlanOverlayClassifierType1 =
                                createdServiceFunctionForwarder.getAugmentation(SffVxlanClassifierType1.class);
                        if (sffVxlanOverlayClassifierType1 != null) {
                            VxlanClassifierType1 vxlanClassifierType1 =
                                    sffVxlanOverlayClassifierType1.getVxlanClassifierType1();
                        }
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

                        SffName sffName = serviceFunctionForwarder.getName();
                        LOG.debug("{}: SFF {} update", Thread.currentThread().getStackTrace()[1], sffName);
                        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
                        List<RspName> rspList = new ArrayList<>();
                        if ((sffServicePathList != null) && !sffServicePathList.isEmpty()) {
                            if (!SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderState(sffName)) {
                                LOG.error("{}: Failed to delete SFF {} operational state",
                                        Thread.currentThread().getStackTrace()[1], sffName);
                            }
                            for (SffServicePath sffServicePath : sffServicePathList) {
                                // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah

                                RspName rspName = new RspName(sffServicePath.getName().getValue());
                                // XXX Another example of Method Overloading confusion brought about
                                // by Strings
                                SfcProviderServiceFunctionAPI
                                    .deleteServicePathFromServiceFunctionState(new SfpName(rspName.getValue()));
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

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the SFC SFF config datastore
 *
 * <p>
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsSffEntryDataListener extends SfcOvsAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsSffEntryDataListener.class);

    public static final InstanceIdentifier<ServiceFunctionForwarder> SFF_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class).build();

    public SfcOvsSffEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(SFF_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.CONFIGURATION);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();


        // SFF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {

            if (entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\nCreated Service Function Forwarder: {}", serviceFunctionForwarder.toString());

                //TODO: somehow we need to distinguish regular SFF from OVS SFF,
                //I propose to look at SffDatePlaneLocator, whether OVS augmentation is present.

                //The question is, what to do when SFF is mixed - has some regular DP locators + some OVS locators

                List<SffDataPlaneLocator> sffDataPlaneLocatorList = serviceFunctionForwarder.getSffDataPlaneLocator();
                if (sffDataPlaneLocatorList != null) {
                    for (SffDataPlaneLocator sffDataPlaneLocator : sffDataPlaneLocatorList) {
                        SffDataPlaneLocator1 sffDataPlaneLocator1 = sffDataPlaneLocator.getAugmentation(SffDataPlaneLocator1.class);
                        OvsBridge ovsBridge = sffDataPlaneLocator1.getOvsBridge();
                        if (ovsBridge != null) {
                            //TODO: process SFF OVS locator
                        }
                    }
                }

            }
        }

/*        // SFF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionForwarder)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionForwarder updatedServiceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\nModified Service Function Forwarder : {}", updatedServiceFunctionForwarder.toString());

            }
        }


        // SFF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder deletedServiceFunctionForwarder = (ServiceFunctionForwarder) dataObject;
                LOG.debug("\nDeleted Service Function Forwarder: {}", deletedServiceFunctionForwarder.toString());

            }
        }*/
        printTraceStop(LOG);
    }

}

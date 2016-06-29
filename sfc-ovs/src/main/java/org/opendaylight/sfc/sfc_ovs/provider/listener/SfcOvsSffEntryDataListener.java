/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the SFC SFF config datastore
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfcOvsSffEntryDataListener extends SfcOvsAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsSffEntryDataListener.class);

    public static final InstanceIdentifier<ServiceFunctionForwarder> SFF_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class).build();

    public SfcOvsSffEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(SFF_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.CONFIGURATION);
        registerAsDataChangeListener(DataBroker.DataChangeScope.ONE);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        // SFF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {

            if (entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\nCreated Service Function Forwarder: {}", serviceFunctionForwarder.toString());
                // add augmentations for serviceFunctionForwarder
                addOvsdbAugmentations(serviceFunctionForwarder, opendaylightSfc.getExecutor());
            }
        }

        // SFF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionForwarder)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionForwarder updatedServiceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\nModified Service Function Forwarder : {}", updatedServiceFunctionForwarder.toString());
                // rewrite augmentations for serviceFunctionForwarder
                addOvsdbAugmentations(updatedServiceFunctionForwarder, opendaylightSfc.getExecutor());
            }
        }

        // SFF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder deletedServiceFunctionForwarder = (ServiceFunctionForwarder) dataObject;
                LOG.debug("\nDeleted Service Function Forwarder: {}", deletedServiceFunctionForwarder.toString());

                KeyedInstanceIdentifier keyedInstanceIdentifier =
                        (KeyedInstanceIdentifier) instanceIdentifier.firstIdentifierOf(ServiceFunctionForwarder.class);
                if (keyedInstanceIdentifier != null) {
                    ServiceFunctionForwarderKey sffKey = (ServiceFunctionForwarderKey) keyedInstanceIdentifier.getKey();
                    SffName sffName = sffKey.getName();

                    // delete OvsdbNode
                    SfcOvsUtil.deleteOvsdbNode(SfcOvsUtil.buildOvsdbNodeIID(sffName.getValue()),
                            opendaylightSfc.getExecutor());
                }

            } else if (dataObject instanceof SffDataPlaneLocator) {
                SffDataPlaneLocator sffDataPlaneLocator = (SffDataPlaneLocator) dataObject;
                LOG.debug("Deleted SffDataPlaneLocator: {}", sffDataPlaneLocator.getName());

                KeyedInstanceIdentifier keyedInstanceIdentifier =
                        (KeyedInstanceIdentifier) instanceIdentifier.firstIdentifierOf(ServiceFunctionForwarder.class);
                if (keyedInstanceIdentifier != null) {
                    ServiceFunctionForwarderKey sffKey = (ServiceFunctionForwarderKey) keyedInstanceIdentifier.getKey();
                    String sffNameAsString = sffKey.getName().getValue();
                    String sffDataPlaneLocatorNameAsString = sffDataPlaneLocator.getName().getValue();

                    // delete OvsdbTerminationPoint
                    SfcOvsUtil.deleteOvsdbTerminationPoint(
                            SfcOvsUtil.buildOvsdbTerminationPointIID(sffNameAsString, sffDataPlaneLocatorNameAsString),
                            opendaylightSfc.getExecutor());
                }
            }
        }
        printTraceStop(LOG);
    }

    /**
     * @param sff ServiceFunctionForwarder Object
     * @param executor ExecutorService Object
     */
    static void addOvsdbAugmentations(ServiceFunctionForwarder sff, ExecutorService executor) {

        OvsdbBridgeAugmentation ovsdbBridge = SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(sff, executor);

        if (ovsdbBridge != null) {
            // put Bridge
            SfcOvsUtil.putOvsdbBridge(ovsdbBridge, executor);

            // put Termination Points
            SfcOvsUtil.putOvsdbTerminationPoints(ovsdbBridge, sff.getSffDataPlaneLocator(), executor);
        }
    }
}

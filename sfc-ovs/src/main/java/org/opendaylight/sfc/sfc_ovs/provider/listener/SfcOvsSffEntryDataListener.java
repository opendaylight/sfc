/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the SFC SFF config datastore
 * <p/>
 * <p/>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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

    private void createOvsdbNodeAndTp(ServiceFunctionForwarder serviceFunctionForwarder,
            List<SffDataPlaneLocator> sffDataPlaneLocatorList) {

        OvsdbBridgeAugmentation ovsdbBridge =
                SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarder, opendaylightSfc.getExecutor());

        if (ovsdbBridge != null) {
            LOG.debug("Creating OVSDB infrastructre: {}", serviceFunctionForwarder.toString());

            //put Bridge
            SfcOvsUtil.putOvsdbBridge(ovsdbBridge, opendaylightSfc.getExecutor());

            //put Termination Points
            SfcOvsUtil.putOvsdbTerminationPoints(ovsdbBridge,
                    sffDataPlaneLocatorList, opendaylightSfc.getExecutor());
        }
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<String, List<SffDataPlaneLocator>> sffDplMap = new HashMap<>();
        Map<String, ServiceFunctionForwarder> sffMap = new HashMap<>();

        printTraceStart(LOG);
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        // SFF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        /*
         * The objects in the notification come separately -- data plane, SFFs, etc.
         * We create mappings of the objects we care about (SffDataPlaneLocator and
         * ServiceFunctionForwarder)
         */
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {

            if (entry.getValue() instanceof SffDataPlaneLocator) {
                SffDataPlaneLocator sffDataPlaneLocator = (SffDataPlaneLocator)entry.getValue();
                String sffName = entry.getKey().
                        firstKeyOf(ServiceFunctionForwarder.class, ServiceFunctionForwarderKey.class).getName();
                List<SffDataPlaneLocator> sffDplList = sffDplMap.get(sffName);
                if (sffDplList == null) {
                    sffDplList = new ArrayList<SffDataPlaneLocator>();
                    sffDplMap.put(sffName, sffDplList);
                }
                sffDplList.add(sffDataPlaneLocator);
                if (sffMap.containsKey(sffName)) {
                    createOvsdbNodeAndTp(sffMap.get(sffName), sffDplList);
                }
            }
            if (entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\nCreated Service Function Forwarder: {}", serviceFunctionForwarder.toString());

                if (sffDplMap.containsKey(serviceFunctionForwarder.getName())) {
                    createOvsdbNodeAndTp(serviceFunctionForwarder, sffDplMap.get(serviceFunctionForwarder.getName()));
                } else {
                    sffMap.put(serviceFunctionForwarder.getName(), serviceFunctionForwarder);
                }
            }
        }


        // SFF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionForwarder)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionForwarder updatedServiceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                LOG.debug("\nModified Service Function Forwarder : {}", updatedServiceFunctionForwarder.toString());

                //build OvsdbBridge
                OvsdbBridgeAugmentation ovsdbBridge =
                        SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(updatedServiceFunctionForwarder, opendaylightSfc.getExecutor());

                if (ovsdbBridge != null) {
                    //put Bridge
                    SfcOvsUtil.putOvsdbBridge(ovsdbBridge, opendaylightSfc.getExecutor());

                    //put Termination Points
                    SfcOvsUtil.putOvsdbTerminationPoints(ovsdbBridge,
                            updatedServiceFunctionForwarder.getSffDataPlaneLocator(), opendaylightSfc.getExecutor());
                }
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
                    String sffName = sffKey.getName();

                    //delete OvsdbNode
                    SfcOvsUtil.deleteOvsdbNode(SfcOvsUtil.buildOvsdbNodeIID(sffName), opendaylightSfc.getExecutor());
                }

            } else if (dataObject instanceof SffDataPlaneLocator) {
                SffDataPlaneLocator sffDataPlaneLocator = (SffDataPlaneLocator) dataObject;
                LOG.debug("Deleted SffDataPlaneLocator: {}", sffDataPlaneLocator.getName());

                KeyedInstanceIdentifier keyedInstanceIdentifier =
                        (KeyedInstanceIdentifier) instanceIdentifier.firstIdentifierOf(ServiceFunctionForwarder.class);
                if (keyedInstanceIdentifier != null) {
                    ServiceFunctionForwarderKey sffKey = (ServiceFunctionForwarderKey) keyedInstanceIdentifier.getKey();
                    String sffName = sffKey.getName();

                    //delete OvsdbTerminationPoint
                    SfcOvsUtil.deleteOvsdbTerminationPoint(SfcOvsUtil.buildOvsdbTerminationPointIID(
                            sffName, sffDataPlaneLocator.getName()), opendaylightSfc.getExecutor());
                }
            }
        }
        printTraceStop(LOG);
    }
}

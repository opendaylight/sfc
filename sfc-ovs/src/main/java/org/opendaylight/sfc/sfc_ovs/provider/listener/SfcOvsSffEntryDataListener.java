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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
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

                //build OvsdbBridge
                OvsdbBridgeAugmentation ovsdbBridge =
                        SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarder);

                //put Bridge
                putOvsdbBridge(ovsdbBridge);

                //put Termination Points
                putOvsdbTerminationPoints(ovsdbBridge, serviceFunctionForwarder);
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
                        SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(updatedServiceFunctionForwarder);

                //put Bridge
                putOvsdbBridge(ovsdbBridge);

                //put Termination Points
                putOvsdbTerminationPoints(ovsdbBridge, updatedServiceFunctionForwarder);
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
                    deleteOvsdbNode(SfcOvsUtil.buildOvsdbNodeIID(sffName));
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
                    deleteOvsdbTerminationPoint(SfcOvsUtil.buildOvsdbTerminationPointIID(sffName, sffDataPlaneLocator.getName()));
                }
            }
        }
        printTraceStop(LOG);
    }

    private boolean putOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge) {
        Object[] methodParameters = {ovsdbBridge};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIPutBridge =
                new SfcOvsDataStoreAPI(
                        SfcOvsDataStoreAPI.Method.PUT_OVSDB_BRIDGE,
                        methodParameters
                );
        return SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIPutBridge, getOpendaylightSfc().getExecutor());
    }

    private boolean deleteOvsdbNode(InstanceIdentifier<Node> ovsdbNodeIID) {
        Object[] methodParameters = {ovsdbNodeIID};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIDeleteNode = new SfcOvsDataStoreAPI(
                SfcOvsDataStoreAPI.Method.DELETE_OVSDB_NODE,
                methodParameters
        );
        return SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIDeleteNode, getOpendaylightSfc().getExecutor());
    }

    private boolean deleteOvsdbTerminationPoint(InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID) {
        Object[] methodParameters = {ovsdbTerminationPointIID};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIDeleteTerminationPoint= new SfcOvsDataStoreAPI(
                SfcOvsDataStoreAPI.Method.DELETE_OVSDB_TERMINATION_POINT,
                methodParameters
        );
        return SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIDeleteTerminationPoint, getOpendaylightSfc().getExecutor());
    }

    private boolean putOvsdbTerminationPoints(OvsdbBridgeAugmentation ovsdbBridge,  ServiceFunctionForwarder serviceFunctionForwarder) {
        boolean result = true;
        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList =
                SfcSffToOvsMappingAPI.buildTerminationPointAugmentationList(ovsdbBridge, serviceFunctionForwarder);

        for (OvsdbTerminationPointAugmentation ovsdbTerminationPoint: ovsdbTerminationPointList) {
            Object[] methodParameters = {ovsdbBridge, ovsdbTerminationPoint};
            SfcOvsDataStoreAPI sfcOvsDataStoreAPIPutTerminationPoint =
                    new SfcOvsDataStoreAPI(
                            SfcOvsDataStoreAPI.Method.PUT_OVSDB_TERMINATION_POINT,
                            methodParameters
                    );
            boolean partialResult =
                    SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIPutTerminationPoint, getOpendaylightSfc().getExecutor());

            //once result is false, we will keep it false (it will be not overwritten with next partialResults)
            if (result) {
                result = partialResult;
            }
        }

        return result;
    }
}

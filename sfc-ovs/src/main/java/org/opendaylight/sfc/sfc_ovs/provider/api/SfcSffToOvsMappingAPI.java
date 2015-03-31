/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder2;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map SFC Service Function Forwarder to OVS Bridge
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see SfcSffToOvsMappingAPI
 * <p/>
 * <p/>
 * <p/>
 * @since 2015-03-23
 */
public class SfcSffToOvsMappingAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcSffToOvsMappingAPI.class);

    public static OvsdbBridgeAugmentation buildOvsdbBridgeAugmentation (ServiceFunctionForwarder serviceFunctionForwarder) {
        Preconditions.checkNotNull(serviceFunctionForwarder);

        OvsdbBridgeAugmentationBuilder ovsdbBridgeBuilder = new OvsdbBridgeAugmentationBuilder();

        ServiceFunctionForwarder2 serviceForwarderOvsBridgeAugmentation =
                serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder2.class);
        try {
            Preconditions.checkNotNull(serviceForwarderOvsBridgeAugmentation);
            OvsBridge serviceForwarderOvsBridge = serviceForwarderOvsBridgeAugmentation.getOvsBridge();

            Preconditions.checkNotNull(serviceForwarderOvsBridge);
            ovsdbBridgeBuilder.setBridgeName(new OvsdbBridgeName(serviceForwarderOvsBridge.getBridgeName()));
            ovsdbBridgeBuilder.setBridgeUuid(serviceForwarderOvsBridge.getUuid());

        } catch (NullPointerException e) {
            LOG.warn("Cannot build OvsdbBridgeAugmentation. Missing OVS Bridge augmentation on SFF {}", serviceFunctionForwarder.getName());
            return null;
        }

        ServiceFunctionForwarder1 serviceForwarderOvsNodeAugmentation =
                serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder1.class);
        try {
            Preconditions.checkNotNull(serviceForwarderOvsNodeAugmentation);
            OvsNode serviceForwarderOvsNode = serviceForwarderOvsNodeAugmentation.getOvsNode();

            Preconditions.checkNotNull(serviceForwarderOvsNode);
            ovsdbBridgeBuilder.setManagedBy(serviceForwarderOvsNode.getNodeId());

        } catch (NullPointerException e) {
            LOG.warn("Cannot build OvsdbBridgeAugmentation. Missing OVS Node augmentation on SFF {}", serviceFunctionForwarder.getName());
            return null;
        }

        return ovsdbBridgeBuilder.build();
    }

    public static List<OvsdbBridgeAugmentation> getOvsdbBridgeListFromServiceForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {
        Preconditions.checkNotNull(serviceFunctionForwarder);

        List<OvsdbBridgeAugmentation> ovsdbBridgeAugmentationList = new ArrayList<>();
        for (Map.Entry<String, List<SffDataPlaneLocator>> mapEntry : getBridgeNameSffDpLocatorMap(serviceFunctionForwarder).entrySet()) {

            ServiceFunctionForwarder1 ovsServiceForwarderAugmentation = serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder1.class);
            if (ovsServiceForwarderAugmentation != null) {
                OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
                //We can use name provided by user - it does not have to be UUID. UUID will be created by OVSDB itself
                ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(mapEntry.getKey()));


//                NodeId nodeId = new NodeId(ovsServiceForwarderAugmentation.getOvsNode().getNodeId());
//
//                //Get reference to parent OVS node
//                InstanceIdentifier<Node> ovsdbNodeIID =
//                        InstanceIdentifier
//                                .builder(NetworkTopology.class)
//                                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
//                                .child(Node.class, new NodeKey(nodeId)).build();
//
//                ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(ovsdbNodeIID));

                //TODO: process SffDataPlaneLocators (set port/interface)

                ovsdbBridgeAugmentationList.add(ovsdbBridgeAugmentationBuilder.build());
            }
        }

        return ovsdbBridgeAugmentationList;
    }

    public static OvsdbTerminationPointAugmentation getTerminationPointFromSffDatePlaneLocator(SffDataPlaneLocator sffDataPlaneLocator) {
        Preconditions.checkNotNull(sffDataPlaneLocator);

        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder
                = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(sffDataPlaneLocator.getName());

        ovsdbTerminationPointAugmentationBuilder = getTerminationPointBuilderFromDataPlaneLocator(
                sffDataPlaneLocator.getDataPlaneLocator(), ovsdbTerminationPointAugmentationBuilder);

        return ovsdbTerminationPointAugmentationBuilder.build();
    }

    private static OvsdbTerminationPointAugmentationBuilder getTerminationPointBuilderFromDataPlaneLocator(
            DataPlaneLocator dataPlaneLocator, OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder) {
        Preconditions.checkNotNull(dataPlaneLocator);
        Preconditions.checkNotNull(ovsdbTerminationPointAugmentationBuilder);

        if (dataPlaneLocator.getTransport() == VxlanGpe.class) {
            ovsdbTerminationPointAugmentationBuilder.setInterfaceType(InterfaceTypeVxlan.class);
        }

        return ovsdbTerminationPointAugmentationBuilder;
    }

    private static Map<String, List<SffDataPlaneLocator>> getBridgeNameSffDpLocatorMap(ServiceFunctionForwarder serviceFunctionForwarder) {
        Map<String, List<SffDataPlaneLocator>> ovsdbBridgeNameSffDpLocatorMap = new HashMap<>();

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = serviceFunctionForwarder.getSffDataPlaneLocator();
        if (sffDataPlaneLocatorList != null) {

            for (SffDataPlaneLocator sffDataPlaneLocator : sffDataPlaneLocatorList) {
                SffDataPlaneLocator1 sffDataPlaneLocator1 = sffDataPlaneLocator.getAugmentation(SffDataPlaneLocator1.class);

                if (sffDataPlaneLocator1 != null) {
                    OvsBridge ovsBridge = sffDataPlaneLocator1.getOvsBridge();

                    if (ovsBridge != null && ovsBridge.getBridgeName() != null && !ovsBridge.getBridgeName().isEmpty()) {
                        if (ovsdbBridgeNameSffDpLocatorMap.get(ovsBridge.getBridgeName()) == null) {
                            ArrayList<SffDataPlaneLocator> dplList = new ArrayList<>();
                            ovsdbBridgeNameSffDpLocatorMap.put(ovsBridge.getBridgeName(), dplList);
                            dplList.add(sffDataPlaneLocator);
                        } else {
                            ovsdbBridgeNameSffDpLocatorMap.get(ovsBridge.getBridgeName()).add(sffDataPlaneLocator);
                        }
                    }
                }
            }
        }
        return ovsdbBridgeNameSffDpLocatorMap;
    }

    public static void putOvsdbBridgeAugmentation(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot PUT new record into OVS configuration store, OvsdbBridgeAugmentation is null.");

        String bridgeName = (ovsdbBridge.getBridgeName().getValue());
        InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) ovsdbBridge.getManagedBy().getValue();

        KeyedInstanceIdentifier keyedInstanceIdentifier = (KeyedInstanceIdentifier) nodeIID.firstIdentifierOf(Node.class);
        if (keyedInstanceIdentifier != null) {
            NodeKey nodeKey = (NodeKey) keyedInstanceIdentifier.getKey();
            String nodeId = nodeKey.getNodeId().getValue();
            nodeId = nodeId.concat("/" + bridgeName);

            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID =
                    InstanceIdentifier
                            .builder(NetworkTopology.class)
                            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                            .child(Node.class, new NodeKey(new NodeId(nodeId)))
                            .augmentation(OvsdbBridgeAugmentation.class).build();

            SfcDataStoreAPI.writePutTransactionAPI(bridgeEntryIID, ovsdbBridge, LogicalDatastoreType.CONFIGURATION);
        }
    }
}

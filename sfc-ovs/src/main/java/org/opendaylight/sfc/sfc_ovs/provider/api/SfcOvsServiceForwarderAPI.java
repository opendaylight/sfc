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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map OVS Bridge to SFC Service Function Forwarder
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsServiceForwarderAPI
 * <p/>
 * <p/>
 * <p/>
 * @since 2015-03-10
 */
public class SfcOvsServiceForwarderAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsServiceForwarderAPI.class);


    /**
     * Returns an Service Function Forwarder object which can be stored
     * in DataStore. The returned object is built on basis of OVS Bridge.
     * The ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param ovsdbBridgeAugmentation ovsdbBridgeAugmentation Object
     * @return ServiceFunctionForwarder Object
     */
    public static ServiceFunctionForwarder getServiceForwarderFromOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        Preconditions.checkNotNull(ovsdbBridgeAugmentation);

        OvsdbBridgeName bridgeName = ovsdbBridgeAugmentation.getBridgeName();
        Uuid uuid = ovsdbBridgeAugmentation.getBridgeUuid();

        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(bridgeName.getValue());
        ovsBridgeBuilder.setUuid(uuid);

        SffDataPlaneLocator1Builder sffDataPlaneLocator1Builder = new SffDataPlaneLocator1Builder();
        sffDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());

        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        //TODO: should be replaced once OVS interface name will be available
        sffDataPlaneLocatorBuilder.setName("interfaceName");
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator1.class, sffDataPlaneLocator1Builder.build());

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(bridgeName.getValue() + " (" + uuid.getValue() + ")");
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(sffDataPlaneLocatorList);

        return serviceFunctionForwarderBuilder.build();
    }

    /**
     * Returns an Service Function Forwarder name. The name is based
     * on OVS Bridge Name and Uuid.
     * The ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param ovsdbBridgeAugmentation ovsdbBridgeAugmentation Object
     * @return Service Function Forwarder name
     */
    public static String getServiceForwarderNameFromOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        Preconditions.checkNotNull(ovsdbBridgeAugmentation);

        OvsdbBridgeName bridgeName = ovsdbBridgeAugmentation.getBridgeName();
        Uuid uuid = ovsdbBridgeAugmentation.getBridgeUuid();

        return bridgeName.getValue() + " (" + uuid.getValue() + ")";
    }

    public static List<OvsdbBridgeAugmentation> getOvsdbBridgeListFromServiceForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {
        Preconditions.checkNotNull(serviceFunctionForwarder);

        List<OvsdbBridgeAugmentation> ovsdbBridgeAugmentationList = new ArrayList<>();
        for (Map.Entry<String, List<SffDataPlaneLocator>> entry : getBridgeNameSffDpLocatorMap(serviceFunctionForwarder).entrySet()) {
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            //We can use name provided by user - it does not have to be UUID. UUID will be created by OVSDB itself
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(entry.getKey()));

            //Set managed by OVS node
            ServiceFunctionForwarder1 ovsServiceForwarderAugmentation = serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder1.class);
            if (ovsServiceForwarderAugmentation != null) {
                NodeId nodeId = new NodeId(ovsServiceForwarderAugmentation.getOvsNode().getNodeId());

                InstanceIdentifier<Node> ovsdbNodeIID =
                        InstanceIdentifier
                                .builder(NetworkTopology.class)
                                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                                .child(Node.class, new NodeKey(nodeId)).build();

                ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(ovsdbNodeIID));
            }

            //TODO: process SffDataPlaneLocators (set port/interface)

            ovsdbBridgeAugmentationList.add(ovsdbBridgeAugmentationBuilder.build());
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
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID =
                InstanceIdentifier
                        .builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(new NodeId(ovsdbBridge.getBridgeName().getValue())))
                        .augmentation(OvsdbBridgeAugmentation.class).build();

        //need to change, sfc-ovs should be independent
        SfcDataStoreAPI.writePutTransactionAPI(bridgeEntryIID, ovsdbBridge, LogicalDatastoreType.CONFIGURATION);
    }
}

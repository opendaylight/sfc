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
 * @since 2015-04-13
 */
package org.opendaylight.sfc.sfc_ovs.provider.api;


import java.util.concurrent.Callable;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
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

public class SfcOvsDataStoreAPI implements Callable {

    public enum Method {
        PUT_OVSDB_BRIDGE, DELETE_OVSDB_BRIDGE
    }

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsDataStoreAPI.class);

    private Method methodToCall;
    private Object methodParameter;

    public SfcOvsDataStoreAPI(Method methodToCall, Object methodParameter) {
        this.methodToCall = methodToCall;
        this.methodParameter = methodParameter;
    }

    @Override
    public Object call() throws Exception {
        Object result = null;

        switch (methodToCall) {
            case PUT_OVSDB_BRIDGE:
                try {
                    OvsdbBridgeAugmentation ovsdbBridge = (OvsdbBridgeAugmentation) methodParameter;
                    result = putOvsdbBridgeAugmentation(ovsdbBridge);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call putOvsdbBridgeAugmentation, passed method argument " +
                            "is not instance of OvsdbBridgeAugmentation: {}", methodParameter.toString());
                }
                break;
            case DELETE_OVSDB_BRIDGE:
                try {
                    OvsdbBridgeAugmentation ovsdbBridge = (OvsdbBridgeAugmentation) methodParameter;
                    result = deleteOvsdbBridgeAugmentation(ovsdbBridge);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call deleteOvsdbBridgeAugmentation, passed method argument " +
                            "is not instance of OvsdbBridgeAugmentation: {}", methodParameter.toString());
                }
                break;
        }

        return result;
    }

    private boolean putOvsdbBridgeAugmentation(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot PUT new record into OVS configuration store, OvsdbBridgeAugmentation is null.");

        return SfcDataStoreAPI.writePutTransactionAPI(buildOvsdbBridgeIID(ovsdbBridge), ovsdbBridge, LogicalDatastoreType.CONFIGURATION);
    }

    private boolean deleteOvsdbBridgeAugmentation(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot DELETE record from OVS configuration store, OvsdbBridgeAugmentation is null.");

        return SfcDataStoreAPI.deleteTransactionAPI(buildOvsdbBridgeIID(ovsdbBridge), LogicalDatastoreType.CONFIGURATION);
    }

    private InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot build OvsdbBridge InstanceIdentifier, OvsdbBridgeAugmentation is null.");

        Preconditions.checkNotNull(ovsdbBridge.getBridgeName(), "Cannot build OvsdbBridge InstanceIdentifier, BridgeName is null.");
        Preconditions.checkNotNull(ovsdbBridge.getManagedBy(), "Cannot build OvsdbBridge InstanceIdentifier, ManagedBy is null.");
        String bridgeName = (ovsdbBridge.getBridgeName().getValue());
        InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) ovsdbBridge.getManagedBy().getValue();

        KeyedInstanceIdentifier keyedInstanceIdentifier = (KeyedInstanceIdentifier) nodeIID.firstIdentifierOf(Node.class);
        Preconditions.checkNotNull(keyedInstanceIdentifier, "Cannot build OvsdbBridge InstanceIdentifier, parent OVS Node is null.");

        NodeKey nodeKey = (NodeKey) keyedInstanceIdentifier.getKey();
        String nodeId = nodeKey.getNodeId().getValue();
        nodeId = nodeId.concat("/" + bridgeName);

        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID =
                InstanceIdentifier
                        .builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(new NodeId(nodeId)))
                        .augmentation(OvsdbBridgeAugmentation.class).build();

        return bridgeEntryIID;
    }
}

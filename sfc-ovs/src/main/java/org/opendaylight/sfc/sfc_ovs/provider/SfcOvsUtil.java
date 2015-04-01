/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * SfcOvsUtil class contains various wrapper and utility methods
 * <p/>
 * <p/>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-04-01
 */

package org.opendaylight.sfc.sfc_ovs.provider;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
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

public class SfcOvsUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsUtil.class);

    /**
     * Submits callable for execution by given ExecutorService.
     * Thanks to this wrapper method, boolean result will be returned instead of Future.
     *
     * <p>
     * @param callable Callable
     * @param executor ExecutorService
     * @return true if callable completed successfully, otherwise false.
     */
    public static boolean submitCallable(Callable callable, ExecutorService executor) {
        Future future = null;
        boolean result = false;

        future = executor.submit(callable);

        try {
            result = (boolean) future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("{} failed to: {}", callable.toString(), e);
        }

        return result;
    }

    /**
     * Method builds OVS Bridge InstanceIdentifier which is based on:
     * 1. OVS Node InstanceIdentifier which manages the OVS Bridge
     * 2. OVS Bridge name
     *
     * If the two aforementioned fields are missing, NullPointerException is raised.
     *
     * <p>
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return InstanceIdentifier<OvsdbBridgeAugmentation>
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(OvsdbBridgeAugmentation ovsdbBridge) {
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

    public static InstanceIdentifier<OvsdbTerminationPointAugmentation> buildOvsdbTerminationPointIID(
            OvsdbTerminationPointAugmentation terminationPoint) {

        //TODO: implement method

        return null;
    }
}

/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others. All rights reserved.
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
 * @since 2015-04-13
 */
package org.opendaylight.sfc.ovs.api;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SfcOvsDataStoreAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsDataStoreAPI.class);

    private SfcOvsDataStoreAPI() {
    }

    public static boolean putOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge,
                "Cannot PUT new OVS Bridge into OVS configuration store, OvsdbBridgeAugmentation is null.");

        return SfcDataStoreAPI.writeMergeTransactionAPI(SfcOvsUtil.buildOvsdbBridgeIID(ovsdbBridge), ovsdbBridge,
                LogicalDatastoreType.CONFIGURATION);
    }

    public static boolean deleteOvsdbNode(InstanceIdentifier<Node> ovsdbNodeIID) {
        Preconditions.checkNotNull(ovsdbNodeIID,
                "Cannot DELETE OVS Node from OVS configuration store, InstanceIdentifier<Node> is null.");

        return SfcDataStoreAPI.deleteTransactionAPI(ovsdbNodeIID, LogicalDatastoreType.CONFIGURATION);
    }

    public static boolean putOvsdbTerminationPoint(OvsdbBridgeAugmentation ovsdbBridge,
            OvsdbTerminationPointAugmentation ovsdbTerminationPoint) {
        Preconditions.checkNotNull(ovsdbTerminationPoint,
                "Cannot PUT Termination Point into OVS configuration store,"
                + "OvsdbTerminationPointAugmentation is null.");

        return SfcDataStoreAPI.writePutTransactionAPI(
                SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(ovsdbBridge, ovsdbTerminationPoint),
                ovsdbTerminationPoint, LogicalDatastoreType.CONFIGURATION);
    }

    public static boolean deleteOvsdbTerminationPoint(InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID) {
        Preconditions.checkNotNull(ovsdbTerminationPointIID,
                "Cannot DELETE Termination Point from OVS configuration store,"
                + "InstanceIdentifier<TerminationPoint> is null.");

        return SfcDataStoreAPI.deleteTransactionAPI(ovsdbTerminationPointIID, LogicalDatastoreType.CONFIGURATION);
    }

    public static Node readOvsdbNodeByIp(String ipAddress) {
        Preconditions.checkNotNull(ipAddress,
                "Cannot READ Node for given ipAddress from OVS operational store, ipAddress is null.");

        Topology topology = SfcDataStoreAPI.readTransactionAPI(SfcOvsUtil.buildOvsdbTopologyIID(),
                LogicalDatastoreType.OPERATIONAL);

        if (topology == null) {
            LOG.warn("Cannot READ the Topology from the operational store");
            return null;
        }

        if (topology.getNode() == null) {
            LOG.warn("Cannot READ Node for ipAddress [{}] from OVS operational store,"
                    + "Topology does not contain any Node.", ipAddress);
            return null;
        }

        for (Node node : topology.getNode()) {
            OvsdbNodeAugmentation ovsdbNodeAug = node.augmentation(OvsdbNodeAugmentation.class);
            if (ovsdbNodeAug != null && ovsdbNodeAug.getConnectionInfo() != null) {
                IpAddress connectionIp = ovsdbNodeAug.getConnectionInfo().getRemoteIp();
                if (connectionIp == null) {
                    return null;
                }
                if (connectionIp.getIpv4Address() != null
                        && connectionIp.getIpv4Address().getValue().equals(ipAddress)) {
                    return node;
                } else if (connectionIp.getIpv6Address() != null
                        && connectionIp.getIpv6Address().getValue().equals(ipAddress)) {
                    return node;
                }
            }
        }

        return null;
    }

    public static OvsdbBridgeAugmentation readOvsdbBridge(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID) {
        Preconditions.checkNotNull(bridgeIID,
                "Cannot READ OVS Bridge from OVS operational store, "
                + "InstanceIdentifier<OvsdbBridgeAugmentation> is null.");

        return SfcDataStoreAPI.readTransactionAPI(bridgeIID, LogicalDatastoreType.OPERATIONAL);
    }

    public static Node readOvsdbNodeByRef(OvsdbNodeRef nodeRef) {
        Preconditions.checkNotNull(nodeRef, "Cannot READ OVS Node from OVSDB operational store, nodeRef is null.");
        InstanceIdentifier<Node> bridgeIID = (InstanceIdentifier<Node>) nodeRef.getValue();

        return SfcDataStoreAPI.readTransactionAPI(bridgeIID, LogicalDatastoreType.OPERATIONAL);
    }
}

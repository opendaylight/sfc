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
 * @since 2015-04-13
 */
package org.opendaylight.sfc.sfc_ovs.provider.api;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
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

public class SfcOvsDataStoreAPI implements Callable {

    public enum Method {
        READ_OVSDB_BRIDGE, PUT_OVSDB_BRIDGE, PUT_OVSDB_TERMINATION_POINT, DELETE_OVSDB_TERMINATION_POINT, DELETE_OVSDB_NODE, READ_OVSDB_NODE_BY_IP, READ_OVSDB_NODE_BY_REF
    }

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsDataStoreAPI.class);

    private Method methodToCall;
    private Object[] methodParameters;

    public SfcOvsDataStoreAPI(Method methodToCall, Object[] newMethodParameters) {
        this.methodToCall = methodToCall;
        if (newMethodParameters == null) {
            this.methodParameters = null;
        } else {
            this.methodParameters = Arrays.copyOf(newMethodParameters, newMethodParameters.length);
        }
    }

    @Override
    public Object call() throws Exception {
        Object result = null;

        switch (methodToCall) {
            case READ_OVSDB_BRIDGE:
                try {
                    InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID =
                            (InstanceIdentifier<OvsdbBridgeAugmentation>) methodParameters[0];
                    result = readOvsdbBridge(bridgeIID);
                } catch (ClassCastException e) {
                    LOG.error(
                            "Cannot call readOvsdbBridge, passed method argument "
                                    + "is not instance of InstanceIdentifier<OvsdbBridgeAugmentation>: {}",
                            methodParameters[0].toString());
                }
                break;
            case PUT_OVSDB_BRIDGE:
                try {
                    OvsdbBridgeAugmentation ovsdbBridge = (OvsdbBridgeAugmentation) methodParameters[0];
                    result = putOvsdbBridge(ovsdbBridge);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call putOvsdbBridge, passed method argument "
                            + "is not instance of OvsdbBridgeAugmentation: {}", methodParameters[0].toString());
                }
                break;
            case DELETE_OVSDB_NODE:
                try {
                    InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) methodParameters[0];
                    result = deleteOvsdbNode(nodeIID);
                } catch (ClassCastException e) {
                    LOG.error(
                            "Cannot call deleteOvsdbNode, passed method argument "
                                    + "is not instance of InstanceIdentifier<Node>: {}",
                            methodParameters[0].toString());
                }
                break;
            case PUT_OVSDB_TERMINATION_POINT:
                try {
                    OvsdbBridgeAugmentation ovsdbBridge = (OvsdbBridgeAugmentation) methodParameters[0];
                    OvsdbTerminationPointAugmentation ovsdbTerminationPoint =
                            (OvsdbTerminationPointAugmentation) methodParameters[1];
                    result = putOvsdbTerminationPoint(ovsdbBridge, ovsdbTerminationPoint);
                } catch (ClassCastException e) {
                    LOG.error(
                            "Cannot call putOvsdbTerminationPoint, passed method arguments "
                                    + "are not instances of OvsdbBridgeAugmentation{} and OvsdbTerminationPointAugmentation: {}",
                            methodParameters[0].toString(), methodParameters[1].toString());
                }
                break;
            case DELETE_OVSDB_TERMINATION_POINT:
                try {
                    InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID =
                            (InstanceIdentifier<TerminationPoint>) methodParameters[0];
                    result = deleteOvsdbTerminationPoint(ovsdbTerminationPointIID);
                } catch (ClassCastException e) {
                    LOG.error(
                            "Cannot call deleteOvsdbTerminationPoint, passed method argument "
                                    + "is not instance of InstanceIdentifier<TerminationPoint>: {}",
                            methodParameters[0].toString());
                }
                break;
            case READ_OVSDB_NODE_BY_IP:
                try {
                    result = readOvsdbNodeByIp((String) methodParameters[0]);
                } catch (ClassCastException e) {
                    LOG.error(
                            "Cannot call readOvsdbNodeByIp, passed method argument " + "is not instance of String: {}",
                            methodParameters[0].toString());
                }
                break;
            case READ_OVSDB_NODE_BY_REF:
                try {
                    result = readOvsdbNodeByRef((OvsdbNodeRef) methodParameters[0]);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call readOvsdbNodeByIp, passed method argument "
                            + "is not instance of OvsdbNodeRef: {}", methodParameters[0]);
                }
        }

        return result;
    }

    private boolean putOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge,
                "Cannot PUT new OVS Bridge into OVS configuration store, OvsdbBridgeAugmentation is null.");

        return SfcDataStoreAPI.writeMergeTransactionAPI(SfcOvsUtil.buildOvsdbBridgeIID(ovsdbBridge), ovsdbBridge,
                LogicalDatastoreType.CONFIGURATION);
    }

    private boolean deleteOvsdbNode(InstanceIdentifier<Node> ovsdbNodeIID) {
        Preconditions.checkNotNull(ovsdbNodeIID,
                "Cannot DELETE OVS Node from OVS configuration store, InstanceIdentifier<Node> is null.");

        return SfcDataStoreAPI.deleteTransactionAPI(ovsdbNodeIID, LogicalDatastoreType.CONFIGURATION);
    }

    private boolean putOvsdbTerminationPoint(OvsdbBridgeAugmentation ovsdbBridge,
            OvsdbTerminationPointAugmentation ovsdbTerminationPoint) {
        Preconditions.checkNotNull(ovsdbTerminationPoint,
                "Cannot PUT Termination Point into OVS configuration store, OvsdbTerminationPointAugmentation is null.");

        return SfcDataStoreAPI.writePutTransactionAPI(
                SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(ovsdbBridge, ovsdbTerminationPoint),
                ovsdbTerminationPoint, LogicalDatastoreType.CONFIGURATION);
    }

    private boolean deleteOvsdbTerminationPoint(InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID) {
        Preconditions.checkNotNull(ovsdbTerminationPointIID,
                "Cannot DELETE Termination Point from OVS configuration store, InstanceIdentifier<TerminationPoint> is null.");

        return SfcDataStoreAPI.deleteTransactionAPI(ovsdbTerminationPointIID, LogicalDatastoreType.CONFIGURATION);
    }

    private Node readOvsdbNodeByIp(String ipAddress) {
        Preconditions.checkNotNull(ipAddress,
                "Cannot READ Node for given ipAddress from OVS operational store, ipAddress is null.");

        Topology topology = SfcDataStoreAPI.readTransactionAPI(SfcOvsUtil.buildOvsdbTopologyIID(),
                LogicalDatastoreType.OPERATIONAL);
        if (topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                OvsdbNodeAugmentation ovsdbNodeAug = node.getAugmentation(OvsdbNodeAugmentation.class);
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
        } else {
            LOG.warn(
                    "Cannot READ Node for given ipAddress from OVS operational store, Topology does not contain any Node.");
        }
        return null;
    }

    private OvsdbBridgeAugmentation readOvsdbBridge(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID) {
        Preconditions.checkNotNull(bridgeIID,
                "Cannot READ OVS Bridge from OVS operational store, InstanceIdentifier<OvsdbBridgeAugmentation> is null.");

        return SfcDataStoreAPI.readTransactionAPI(bridgeIID, LogicalDatastoreType.OPERATIONAL);
    }

    private Node readOvsdbNodeByRef(OvsdbNodeRef nodeRef) {
        Preconditions.checkNotNull(nodeRef, "Cannot READ OVS Node from OVSDB operational store, nodeRef is null.");
        InstanceIdentifier<Node> bridgeIID = (InstanceIdentifier<Node>) nodeRef.getValue();

        return SfcDataStoreAPI.readTransactionAPI(bridgeIID, LogicalDatastoreType.OPERATIONAL);
    }
}

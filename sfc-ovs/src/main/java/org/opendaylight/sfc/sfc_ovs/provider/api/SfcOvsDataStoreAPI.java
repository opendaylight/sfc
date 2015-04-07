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
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsDataStoreAPI implements Callable {

    public enum Method {
        PUT_OVSDB_BRIDGE, DELETE_OVSDB_NODE,
        PUT_OVSDB_TERMINATION_POINT, DELETE_OVSDB_TERMINATION_POINT
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
                    result = putOvsdbBridge(ovsdbBridge);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call putOvsdbBridge, passed method argument " +
                            "is not instance of OvsdbBridgeAugmentation: {}", methodParameter.toString());
                }
                break;
            case DELETE_OVSDB_NODE:
                try {
                    InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) methodParameter;
                    result = deleteOvsdbNode(nodeIID);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call deleteOvsdbNode, passed method argument " +
                            "is not instance of InstanceIdentifier<Node>: {}", methodParameter.toString());
                }
                break;
            case PUT_OVSDB_TERMINATION_POINT:
                try {
                    OvsdbTerminationPointAugmentation ovsdbTerminationPoint = (OvsdbTerminationPointAugmentation) methodParameter;
                    result = putOvsdbTerminationPoint(ovsdbTerminationPoint);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call putOvsdbTerminationPoint, passed method argument " +
                            "is not instance of OvsdbTerminationPointAugmentation: {}", methodParameter.toString());
                }
                break;
            case DELETE_OVSDB_TERMINATION_POINT:
                try {
                    InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID =
                            (InstanceIdentifier<TerminationPoint>) methodParameter;
                    result = deleteOvsdbTerminationPoint(ovsdbTerminationPointIID);
                } catch (ClassCastException e) {
                    LOG.error("Cannot call deleteOvsdbTerminationPoint, passed method argument " +
                            "is not instance of InstanceIdentifier<TerminationPoint>: {}", methodParameter.toString());
                }
                break;
        }

        return result;
    }

    private boolean putOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot PUT new OVS Bridge into OVS configuration store, OvsdbBridgeAugmentation is null.");

        return SfcDataStoreAPI.writePutTransactionAPI(
                SfcOvsUtil.buildOvsdbBridgeIID(ovsdbBridge), ovsdbBridge, LogicalDatastoreType.CONFIGURATION);
    }

    private boolean deleteOvsdbNode(InstanceIdentifier<Node> ovsdbNodeIID) {
        Preconditions.checkNotNull(ovsdbNodeIID, "Cannot DELETE OVS Node from OVS configuration store, InstanceIdentifier<Node> is null.");

        return SfcDataStoreAPI.deleteTransactionAPI(ovsdbNodeIID, LogicalDatastoreType.CONFIGURATION);
    }

    private boolean putOvsdbTerminationPoint(OvsdbTerminationPointAugmentation ovsdbTerminationPoint) {
        Preconditions.checkNotNull(ovsdbTerminationPoint,
                "Cannot PUT Termination Point into OVS configuration store, OvsdbTerminationPointAugmentation is null.");

        return SfcDataStoreAPI.writePutTransactionAPI(
                SfcOvsUtil.buildOvsdbTerminationPointIID(ovsdbTerminationPoint),
                ovsdbTerminationPoint, LogicalDatastoreType.CONFIGURATION
        );
    }

    private boolean deleteOvsdbTerminationPoint(InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID) {
        Preconditions.checkNotNull(ovsdbTerminationPointIID,
                "Cannot DELETE Termination Point from OVS configuration store, InstanceIdentifier<TerminationPoint> is null.");

        return SfcDataStoreAPI.deleteTransactionAPI(ovsdbTerminationPointIID, LogicalDatastoreType.CONFIGURATION);
    }
}

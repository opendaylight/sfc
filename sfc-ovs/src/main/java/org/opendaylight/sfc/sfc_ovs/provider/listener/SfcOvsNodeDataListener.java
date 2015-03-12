/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the OVSDB southbound operational datastore
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsToSffMappingAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfcOvsNodeDataListener extends SfcOvsAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsNodeDataListener.class);

/*    public static final InstanceIdentifier<OvsdbNodeAugmentation>  OVSDB_NODE_AUGMENTATION_INSTANCE_IDENTIFIER =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class)
                    .augmentation(OvsdbNodeAugmentation.class);*/

    public static final InstanceIdentifier<Node> OVSDB_NODE_AUGMENTATION_INSTANCE_IDENTIFIER =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class);

    public SfcOvsNodeDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OVSDB_NODE_AUGMENTATION_INSTANCE_IDENTIFIER);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        // NODE CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {

            if (entry.getValue() instanceof Node) {
                Node createdNode = (Node) entry.getValue();
                LOG.debug("\nCreated OVS Node: {}", createdNode.toString());
                SfcProviderServiceForwarderAPI.updateServiceFunctionForwarderExecutor(
                        SfcOvsToSffMappingAPI.buildServiceFunctionForwarderFromNode(createdNode));

            }
        }

        // NODE UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof Node)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                Node updatedNode = (Node) entry.getValue();
                LOG.debug("\nModified OVS Node : {}", updatedNode.toString());
                SfcProviderServiceForwarderAPI.updateServiceFunctionForwarderExecutor(
                        SfcOvsToSffMappingAPI.buildServiceFunctionForwarderFromNode(updatedNode));
            }
        }

        // NODE DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);

            if (dataObject instanceof Node) {
                Node deletedNode = (Node) dataObject;
                LOG.debug("\nDeleted OVS Node: {}", deletedNode.toString());
                SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderExecutor(
                        SfcOvsToSffMappingAPI.getServiceForwarderNameFromNode(deletedNode));

            } else if (dataObject instanceof OvsdbBridgeAugmentation) {
                OvsdbBridgeAugmentation deletedBridge = (OvsdbBridgeAugmentation) dataObject;
                LOG.debug("\nDeleted OVS Bridge: {}", deletedBridge.toString());
                KeyedInstanceIdentifier keyedInstanceIdentifier = (KeyedInstanceIdentifier) instanceIdentifier.firstIdentifierOf(Node.class);
                if (keyedInstanceIdentifier != null) {
                    NodeKey nodeKey = (NodeKey) keyedInstanceIdentifier.getKey();
                    String nodeId = nodeKey.getNodeId().getValue();
                }

                //TODO: delete OVS Bridge - all Bridge DP locators from SFF

            } else if (dataObject instanceof OvsdbTerminationPointAugmentation) {
                OvsdbTerminationPointAugmentation deletedTerminationPoint = (OvsdbTerminationPointAugmentation) dataObject;
                LOG.debug("\nDeleted OVS Termination Point: {}", deletedTerminationPoint.toString());
                //TODO: delete OVS Termination Point - corresponding DP locator from SFF
            }
        }

        printTraceStop(LOG);
    }

}

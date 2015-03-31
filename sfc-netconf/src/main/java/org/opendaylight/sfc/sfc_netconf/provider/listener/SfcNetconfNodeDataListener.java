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
 * @since       2015-02-13
 */

package org.opendaylight.sfc.sfc_netconf.provider.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfcNetconfNodeDataListener extends SfcNetconfAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfNodeDataListener.class);

    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    public SfcNetconfNodeDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(NETCONF_TOPO_IID);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();


        // Node CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof Node) {
                Node node = (Node) entry.getValue();
                String nodeName = node.getNodeId().getValue();
                LOG.debug("\nCreated NetconfNodeAugmentation: {}", node.toString());

            }
        }


        //enum connecting;

        //enum connected;

        //enum unable-to-connect;

        for ( Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            Node node = null;
            if (entry.getKey().getTargetType() == NetconfNode.class) {
                // We have a Netconf device
                NodeId nodeId = getNodeId(entry);
                String nodeName = nodeId.getValue();
                NetconfNode nnode = (NetconfNode)entry.getValue();
                LOG.info("NETCONF Listener created event: {}", nodeName);


/*                NetconfNodeFields.ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == NetconfNodeFields.ConnectionStatus.Connected) {
                    List<String> capabilities = nnode.getAvailableCapabilities()
                            .getAvailableCapability();
                    LOG.info("Capabilities: {}", capabilities);
                }
                if (SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(
                        SfcNetconfServiceForwarderAPI.buildServiceForwarderFromNetonf(nodeName, nnode))) {
                    LOG.info("Successfully created SFF from Netconf node {}", nodeName);
                } else {
                    LOG.error("Error creating SFF from Netconf node {}", nodeName);
                }*/
            }
        }


        // React to data changes in Netconf nodes present in the Netcon topology
        for ( Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if ((entry.getKey().getTargetType() == NetconfNode.class) &&
                    (!(dataCreatedObject.containsKey(entry.getKey())))) {
                NodeId nodeId = getNodeId(entry);
                String nodeName = nodeId.getValue();

                // We bypass the internal Netconf server
                if (!nodeName.equals("controller-config")) {
                    NetconfNode nnode = (NetconfNode) entry.getValue();

                    NetconfNodeFields.ConnectionStatus csts = nnode.getConnectionStatus();

                    switch (csts) {
                        case Connected: {
                            // Fully connected, all services for remote device available from MountPointService
                            LOG.debug("NETCONF Node: {} is fully connected", nodeId.getValue());
                            List<String> capabilities =
                                    nnode.getAvailableCapabilities().getAvailableCapability();
                            LOG.debug("Capabilities: {}", capabilities);
                            if (SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(
                                    SfcNetconfServiceForwarderAPI.buildServiceForwarderFromNetconf(nodeName, nnode))) {
                                LOG.info("Successfully created SFF from Netconf node {}", nodeName);
                            } else {
                                LOG.error("Error creating SFF from Netconf node {}", nodeName);
                            }
                            break;
                        }
                        case Connecting: {
                            // Connecting state is set initially but netconf device can get back to it after a disconnect
                            // Note that device could jump back and forth between connected and connecting for various reasons:
                            // disconnect from remote device, network connectivity loss etc.
                            LOG.info("Netconf device disconnected, deleting SFF {}", nodeName);
                            if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderExecutor(nodeName)) {
                                LOG.info("SFF {} deleted successfully", nodeName);
                            } else {
                                LOG.error("Failed to delete SFF {}", nodeName);
                            }
                            break;
                        }
                        case UnableToConnect: {
                            // Its over for the device, no more reconnects
                            LOG.info("Unable to connected to Netconf device, deleting SFF {}", nodeName);
                            if (SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderExecutor(nodeName)) {
                                LOG.info("SFF {} deleted successfully", nodeName);
                            } else {
                                LOG.error("Failed to delete SFF {}", nodeName);
                            }
                            break;
                        }
                    }
                }
            }
        }

        printTraceStop(LOG);
    }

    private NodeId getNodeId(final Map.Entry<InstanceIdentifier<?>, DataObject> entry) {
        NodeId nodeId = null;
        for (InstanceIdentifier.PathArgument pathArgument : entry.getKey().getPathArguments()) {
            if (pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {

                final Identifier key = ((InstanceIdentifier.IdentifiableItem) pathArgument).getKey();
                if(key instanceof NodeKey) {
                    nodeId = ((NodeKey) key).getNodeId();
                }
            }
        }
        return nodeId;
    }


}

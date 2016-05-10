/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.renderer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.sfc.sfc_ios_xe.provider.SfcIosXeDataProvider;
import org.opendaylight.sfc.sfc_ios_xe.provider.listener.NodeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NodeManager {

    private static final Logger LOG = LoggerFactory.getLogger(NodeManager.class);

    private final NodeListener nodeListener;
    private final SfcIosXeDataProvider dataProvider;

    // Data
    private final Map<NodeId, Node> connectedNodes = new HashMap<>();
    private final Map<NodeId, DataBroker> activeMountPoints = new HashMap<>();

    public NodeManager(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        // Register provider
        dataProvider = SfcIosXeDataProvider.GetNetconfDataProvider();
        ProviderContext providerContext = bindingAwareBroker.registerProvider(dataProvider);
        dataProvider.onSessionInitiated(providerContext);
        // Node listener
        nodeListener = new NodeListener(dataBroker, this);
    }

    public void updateNode(Topology topology, Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        // Check connection status
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        NodeId netconfNodeId = node.getNodeId();
        switch (connectionStatus) {
            case Connected: {
                connectedNodes.put(netconfNodeId, node);
                // Get mountpoint
                InstanceIdentifier mountPointIid = getMountPointIid(topology.getTopologyId(), netconfNodeId);
                DataBroker dataBroker = getNetconfNodeDataBroker(mountPointIid);
                if (dataBroker != null) {
                    activeMountPoints.put(netconfNodeId, dataBroker);
                } else {
                    LOG.debug("Cannot obtain data broker for netconf node {}", netconfNodeId.getValue());
                    connectedNodes.remove(netconfNodeId);
                }
            }
        }
    }

    public void removeNode(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        NodeId netconfNodeId = node.getNodeId();
        switch (connectionStatus) {
            case Connected: {
                connectedNodes.remove(netconfNodeId);
                activeMountPoints.remove(netconfNodeId);
                LOG.info("Netconf node {} removed", netconfNodeId.getValue());
            }
        }
    }

    private DataBroker getNetconfNodeDataBroker(InstanceIdentifier mountPointIid) {
        MountPointService service = dataProvider.getMountService();
        if (service == null) {
            LOG.debug("Mount service does not exist for mountpoint {}", mountPointIid);
            return null;
        }
        Optional<MountPoint> optionalObject = service.getMountPoint(mountPointIid);
        MountPoint mountPoint;
        if (optionalObject.isPresent()) {
            mountPoint = optionalObject.get();
            if (mountPoint != null) {
                Optional<DataBroker> optionalDataBroker = mountPoint.getService(DataBroker.class);
                if (optionalDataBroker.isPresent()) {
                    return optionalDataBroker.get();
                } else {
                    LOG.debug("Cannot obtain data broker from mountpoint {}", mountPoint);
                }
            } else {
                LOG.debug("Cannot obtain mountpoint with IID {}", mountPointIid);
            }
        }
        return null;
    }

    private InstanceIdentifier getMountPointIid(TopologyId topologyId, NodeId nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(nodeId)).build();
    }

    DataBroker getMountpointFromIpAddress(IpAddress ipAddress) {
        for (Node node : connectedNodes.values()) {
            if (ipAddress.equals(getNetconfNodeIp(node))) {
                return activeMountPoints.get(node.getNodeId());
            }
        }
        return null;
    }

    IpAddress getNetconfNodeIp(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        return netconfNode.getHost().getIpAddress();
    }

    Map<NodeId, Node> getConnectedNodes() {
        return connectedNodes;
    }

    Map<NodeId, DataBroker> getActiveMountPoints() {
        return activeMountPoints;
    }

    public void unregisterNodeListener() {
        nodeListener.getRegistrationObject().close();
    }
}

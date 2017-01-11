/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vpp_renderer.renderer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.Credentials;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.credentials.LoginPasswordBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppNodeManager implements BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(VppNodeManager.class);

    private MountPointService mountService;
    private final TopologyId topologyId = new TopologyId("topology-netconf");
    private List<String> requiredCapabilities = new ArrayList<>();
    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY_IID = InstanceIdentifier
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()))).build();

    // Data
    private final Map<NodeId, Node> connectedNodes = new HashMap<>();
    private final Map<NodeId, DataBroker> activeMountPoints = new HashMap<>();

    public VppNodeManager(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        // Register provider
        ProviderContext providerContext = bindingAwareBroker.registerProvider(this);
        onSessionInitiated(providerContext);
        // Capabilities
        requiredCapabilities = initializeRequiredCapabilities();
    }

    public void updateNode(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        // Check connection status
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        NodeId netconfNodeId = node.getNodeId();
        if (connectionStatus.equals(ConnectionStatus.Connected)) {
            // Get mountpoint
            InstanceIdentifier<Node> mountPointIid = getMountPointIid(netconfNodeId);
            DataBroker dataBroker = getNetconfNodeDataBroker(mountPointIid);
            if (dataBroker != null) {
                LOG.info("Node {} registered by SFC", node.getNodeId().getValue());
                connectedNodes.put(netconfNodeId, node);
                activeMountPoints.put(netconfNodeId, dataBroker);
            } else {
                LOG.debug("Cannot obtain data broker for netconf node {}", netconfNodeId.getValue());
            }
        } else {
            LOG.debug("Node {} isn't connected", node.getNodeId().getValue());
        }
    }

    public void removeNode(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode, "Netconf is null");
        NodeId netconfNodeId = node.getNodeId();
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        switch (connectionStatus) {
        case Connected:
            connectedNodes.remove(netconfNodeId);
            activeMountPoints.remove(netconfNodeId);
            LOG.info("Netconf node {} removed", netconfNodeId.getValue());
            break;
        case Connecting:
        case UnableToConnect:
        default:
            break;
        }
    }

    public boolean mountNode(final String deviceId, final String deviceIp, final String devicePort,
            final String username, final String password, final boolean isTcpOnly) {
        final Credentials credentials = new LoginPasswordBuilder().setPassword(password).setUsername(username).build();

        final NetconfNode netconfNode = new NetconfNodeBuilder()
                .setHost(new Host(new IpAddress(new Ipv4Address(deviceIp))))
                .setPort(new PortNumber(Integer.decode(devicePort))).setTcpOnly(isTcpOnly).setCredentials(credentials)
                .build();

        final NodeId nodeId = new NodeId(deviceId);
        final Node node = new NodeBuilder().setKey(new NodeKey(nodeId)).setNodeId(nodeId)
                .addAugmentation(NetconfNode.class, netconfNode).build();
        InstanceIdentifier<Node> netconfNodeIid = NETCONF_TOPOLOGY_IID.child(Node.class,
                new NodeKey(new NodeId(nodeId)));

        return SfcDataStoreAPI.writeMergeTransactionAPI(netconfNodeIid, node, LogicalDatastoreType.CONFIGURATION);
    }

    public boolean unmountNode(final String deviceId) {
        final NodeId nodeId = new NodeId(deviceId);
        InstanceIdentifier<Node> netconfNodeIid = NETCONF_TOPOLOGY_IID.child(Node.class,
                new NodeKey(new NodeId(nodeId)));

        return SfcDataStoreAPI.deleteTransactionAPI(netconfNodeIid, LogicalDatastoreType.CONFIGURATION);
    }

    public boolean isCapableNetconfDevice(Node node) {
        NetconfNode netconfAugmentation = node.getAugmentation(NetconfNode.class);
        if (netconfAugmentation == null) {
            LOG.debug("Node {} is not a netconf device", node.getNodeId().getValue());
            return false;
        }
        AvailableCapabilities capabilities = netconfAugmentation.getAvailableCapabilities();
        if (capabilities != null) {
            List<String> availCapabilities = capabilities.getAvailableCapability().stream()
                    .map(AvailableCapability::getCapability).collect(Collectors.toList());
            return availCapabilities.containsAll(requiredCapabilities);
        }
        LOG.debug("Node {} hasn't capabilities vpp node requires", node.getNodeId().getValue());
        return false;
    }

    private DataBroker getNetconfNodeDataBroker(InstanceIdentifier<Node> mountPointIid) {
        Optional<MountPoint> optionalObject = mountService.getMountPoint(mountPointIid);
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

    private InstanceIdentifier<Node> getMountPointIid(NodeId nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(nodeId)).build();
    }

    private List<String> initializeRequiredCapabilities() {
        final String netconfTcp = "(urn:opendaylight:params:xml:ns:yang:controller:netconf:northbound:tcp?revision=2015-04-23)netconf-northbound-tcp";
        final String vppNsh = "(urn:opendaylight:params:xml:ns:yang:vpp:nsh?revision=2016-06-24)vpp-nsh";
        final String v3po = "(urn:opendaylight:params:xml:ns:yang:v3po?revision=2015-01-05)v3po";
        final String ietfInterfaces = "(urn:ietf:params:xml:ns:yang:ietf-interfaces?revision=2014-05-08)ietf-interfaces";
        String capabilityEntries[] = { netconfTcp, vppNsh, v3po, ietfInterfaces };
        return Arrays.asList(capabilityEntries);
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

    @Override
    public void onSessionInitiated(ProviderContext session) {
        mountService = session.getSALService(MountPointService.class);
        Preconditions.checkNotNull(mountService);
    }
}

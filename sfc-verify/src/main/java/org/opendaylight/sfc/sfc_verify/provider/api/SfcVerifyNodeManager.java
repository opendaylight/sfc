/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider.api;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.IoamScvListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
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
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
//import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.Map;

public class SfcVerifyNodeManager implements BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVerifyNodeManager.class);

    private MountPointService mountService;
    private final TopologyId topologyId = new TopologyId("topology-netconf");
    private final String sfcvStr;

    // Data
    private final Map<NodeId, Node> connectedNodes = new HashMap<>();
    private final Map<NodeId, DataBroker> activeMountPoints = new HashMap<>();

    public SfcVerifyNodeManager(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker, String sfcvStr) {
        // Register provider
        ProviderContext providerContext = bindingAwareBroker.registerProvider(this);
        onSessionInitiated(providerContext);
        this.sfcvStr = sfcvStr;
    }

    public void updateNode(Node node, boolean ioamSfcvCapable) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        // Check connection status
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        NodeId netconfNodeId = node.getNodeId();
        if (connectionStatus.equals(ConnectionStatus.Connected)) {
            connectedNodes.put(netconfNodeId, node);
            // Get mountpoint
            InstanceIdentifier mountPointIid = getMountPointIid(netconfNodeId);

            //If ioamSfcv capable, register notifications and subscribe
            if (ioamSfcvCapable) {
                Optional<MountPoint> optionalObject = mountService.getMountPoint(mountPointIid);
                MountPoint mountPoint;
                if (optionalObject.isPresent()) {
                    mountPoint = optionalObject.get();
                    if (mountPoint != null) {
                        // Instantiate notification listener
                        final IoamScvListener sfcvListener;
                        final Optional<NotificationService> notiService;
                        final ListenerRegistration<IoamScvListener> aListenerRegistration;
                        final String streamName = sfcvStr;
                        final Optional<RpcConsumerRegistry> rpcService;
                        final NotificationsService rpcNotiService;
                        CreateSubscriptionInputBuilder createSubscriptionInputBuilder =
                                new CreateSubscriptionInputBuilder();

                        sfcvListener = new SfcVerificationListener(netconfNodeId);

                        // Register notification listener
                        notiService = mountPoint.getService(NotificationService.class);
                        notiService.get().registerNotificationListener(sfcvListener);

                        //Do a create-subscription RPC.
                        rpcService = mountPoint.getService(RpcConsumerRegistry.class);
                        rpcNotiService = rpcService.get().getRpcService(NotificationsService.class);

                        createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
                        rpcNotiService.createSubscription(createSubscriptionInputBuilder.build());
                        LOG.debug("Created notifications subscription for Netconf node {}...",
                                    netconfNodeId.getValue());
                    }
                }
            }

            DataBroker dataBroker = getNetconfNodeDataBroker(mountPointIid);
            if (dataBroker != null) {
                activeMountPoints.put(netconfNodeId, dataBroker);
            } else {
                LOG.debug("Cannot obtain data broker for netconf node {}", netconfNodeId.getValue());
                connectedNodes.remove(netconfNodeId);
            }
        }
    }

    public void removeNode(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        NodeId netconfNodeId = node.getNodeId();
        switch (connectionStatus) {
            case Connected:
                connectedNodes.remove(netconfNodeId);
                activeMountPoints.remove(netconfNodeId);
                LOG.info("Netconf node {} removed", netconfNodeId.getValue());
                break;
            default:
                break;
        }
    }

    private DataBroker getNetconfNodeDataBroker(InstanceIdentifier mountPointIid) {
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

    private InstanceIdentifier getMountPointIid(NodeId nodeId) {
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

    public NodeId getNodeIdFromIpAddress(IpAddress ipAddress) {
        for (Node node : connectedNodes.values()) {
            if (ipAddress.equals(getNetconfNodeIp(node))) {
                return node.getNodeId();
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

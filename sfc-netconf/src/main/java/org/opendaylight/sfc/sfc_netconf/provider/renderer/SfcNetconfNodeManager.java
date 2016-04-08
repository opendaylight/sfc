package org.opendaylight.sfc.sfc_netconf.provider.renderer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.sfc.sfc_netconf.provider.SfcNetconfRenderer;
import org.opendaylight.sfc.sfc_netconf.provider.listener.SfcNetconfNodeDataChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
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

public class SfcNetconfNodeManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfNodeManager.class);

    private SfcNetconfNodeDataChangeListener nodeListener;
    private SfcNetconfRenderer renderer;

    // Data
    protected Map<NodeId, Node> connectedNodes = new HashMap<>();
    protected Map<NodeId, DataBroker> activeMountPoints = new HashMap<>();

    public SfcNetconfNodeManager(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker,
                                 SfcNetconfRenderer renderer) {
        this.renderer = renderer;
        // Node listener
        nodeListener = new SfcNetconfNodeDataChangeListener(dataBroker, bindingAwareBroker, this);
    }

    public void updateNodes(ProviderContext providerContext, Topology topology, Node node) {
        NetconfNode netconfNode = getNetconfNodeAugmentation(node);
        Preconditions.checkNotNull(netconfNode);
        // Check connection status
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        switch (connectionStatus) {
            case Connecting:
                // TODO implement
            case Connected: {
                NodeId netconfNodeId = node.getNodeId();
                LOG.info("Netconf node {} connected", netconfNodeId.getValue());
                connectedNodes.put(netconfNodeId, node);
                LOG.info("Connected nodes: {}", connectedNodes);    // remove
                // Get mountpoint
                InstanceIdentifier mountPointIid = getMountPointIid(topology.getTopologyId(), netconfNodeId);
                MountPointService service = providerContext.getSALService(MountPointService.class);
                MountPoint mountPoint = getMountPointObject(service, mountPointIid);
                LOG.info("Mountpoint: {} ", mountPoint);   //remove
                if(mountPoint != null) {
                    DataBroker dataBroker = getMountPointDataBrokerObject(mountPoint);
                    if (dataBroker != null) {
                        LOG.info("DataBroker: {}", dataBroker); // remove
                        activeMountPoints.put(netconfNodeId, dataBroker);
                    }
                }
            }
            case UnableToConnect:
                // TODO implement
        }
    }

    private NetconfNode getNetconfNodeAugmentation(Node node) {
        return node.getAugmentation(NetconfNode.class);
    }

    private InstanceIdentifier getMountPointIid(TopologyId topologyId, NodeId nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(nodeId)).build();
    }

    private MountPoint getMountPointObject(MountPointService service, InstanceIdentifier iid) {
        Optional<MountPoint> optionalMountpoint = service.getMountPoint(iid);
        if(optionalMountpoint.isPresent()) {
            return optionalMountpoint.get();
        }
        else {
            LOG.warn("Unable to retrieve mountpoint with IID {}", iid);
            return null;
        }
    }

    public DataBroker getMountpointFromIp(IpAddress ipAddress) {
        for (Node node : connectedNodes.values()) {
            if (ipAddress.equals(getNetconfNodeIp(node))) {
                return activeMountPoints.get(node.getNodeId());
            }
        }
        return null;
    }

    public DataBroker getMountPointDataBrokerObject(MountPoint mountPoint) {
        Optional<DataBroker> optionalDataBroker = mountPoint.getService(DataBroker.class);
        if(optionalDataBroker.isPresent()) {
            return optionalDataBroker.get();
        }
        else {
            LOG.warn("Unable to retrieve data broker from mountpoint {}", mountPoint);
            return null;
        }
    }

    // TODO this method is in every manager
    public IpAddress getNetconfNodeIp(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        return netconfNode.getHost().getIpAddress();
    }

    public Map<NodeId, Node> getConnectedNodes() {
        return connectedNodes;
    }

    public Map<NodeId, DataBroker> getActiveMountPoints() {
        return activeMountPoints;
    }

    public void unregisterNodeListener() {
        nodeListener.getRegistrationObject().close();
    }

}

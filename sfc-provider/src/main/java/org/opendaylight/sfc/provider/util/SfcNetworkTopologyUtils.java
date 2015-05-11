/**
 * Copyright (c) 2015 Intel Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.util;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;

import java.util.List;

/**
 * This class provides some static APIs which are used to execute
 * some read operations on MD-SAL network topology and returns
 * what a caller needs.
 *
 * <p>
 *
 * @author Yi Yang (yi.y.yang@intel.com)
 *
 * <p>
 * @since 2015-05-08
 */
public class SfcNetworkTopologyUtils {
    private static final String FLOW_TOPO_ID = "flow:1";
    private static final String OVSDB_TOPO_ID = "ovsdb:1";
    private static final String HOST_NODE_PREFIX = "host:";
    private static final String OPENFLOW_NODE_PREFIX = "openflow:";

    private static Topology getTopology(String topoID) {
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topoID)));
        Topology topo = SfcDataStoreAPI.readTransactionAPI(topoIID, LogicalDatastoreType.OPERATIONAL);
        return topo;
    }

    public static Topology getOpenflowTopology() {
        return getTopology(FLOW_TOPO_ID);
    }

    public static Topology getOvsdbTopology() {
        return getTopology(OVSDB_TOPO_ID);
    }

    public static Node getOvsdbNode(String nodeId) {
        InstanceIdentifier<Node> nodeIID = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(OVSDB_TOPO_ID)))
            .child(Node.class, new NodeKey(new NodeId(nodeId)));
        Node node = SfcDataStoreAPI.readTransactionAPI(nodeIID, LogicalDatastoreType.OPERATIONAL);
        return node;
    }

    private static String getTpIdByHostAddress(Object obj, boolean isIp) {
        Topology topo = getOpenflowTopology();
        String nodeId = null;
        String tpId = null;
        HostNode hostNode = null;
        List<Addresses> hostAddresses = null;
        List<AttachmentPoints> hostAttachmentPoints = null;
        boolean found = false;
        List<Node> nodes = topo.getNode();
        Node baseNode = null;

        for (Node node : nodes) {
            // Check if it is HostNode by node ID prefix
            nodeId = node.getNodeId().getValue();
            if (nodeId.startsWith(HOST_NODE_PREFIX) != true) {
                continue;
            }

            // Check if it is HostNode by Augmentation
            hostNode = node.getAugmentation(HostNode.class);
            if (hostNode == null) {// Not HostNode
                continue;
            }

            hostAddresses = hostNode.getAddresses();
            if (hostAddresses.size() == 0) {
                return null;
            }

            found = false;
            for (Addresses addrs : hostAddresses) {
                if (isIp == true) {
                    if (addrs.getIp().equals((IpAddress)obj)) {
                        found = true;
                        break;
                    }
                } else {
                    if (addrs.getMac().equals((MacAddress)obj)) {
                        found = true;
                        break;
                    }
                }
            }

            if (found == true) {
                baseNode = node;
                break;
            }
        }

        if (found == false) {
            return null;
        }

        hostAttachmentPoints = hostNode.getAttachmentPoints();
        for (AttachmentPoints attachmentPoints : hostAttachmentPoints) {
            if (attachmentPoints.isActive()) {
                List<TerminationPoint> tpList = baseNode.getTerminationPoint();
                if (attachmentPoints.getCorrespondingTp().equals(tpList.get(0).getTpId())) {
                    /*
                     * Openflow switch termination point format:
                     *     openflow:1:1
                     *              | |
                     *              | +-- Switch port ID
                     *              +---- Switch ID
                     */
                    tpId = attachmentPoints.getTpId().getValue();
                    break;
                }
            }
        }
        return tpId;
    }

    private static String getOfPortByHostAddress(Object obj, boolean isIp) {
        String tpId = getTpIdByHostAddress(obj, isIp);
        if (tpId == null || tpId.isEmpty()) {
            return null;
        }
        return tpId.split(":")[2];
    }

    public static String getOfPortByHostIp(IpAddress ip) {
        return getOfPortByHostAddress(ip, true);
    }

    public static String getOfPortByHostMac(MacAddress mac) {
        return getOfPortByHostAddress(mac, false);
    }

    public static Link getLink(String srcNodeId, String dstNodeId) {
        Topology topo = getOpenflowTopology();
        List<Link> linkList = topo.getLink();
        Link foundLink = null;
        for (Link link : linkList) {
            Source src = link.getSource();
            Destination dst = link.getDestination();
            if (!src.getSourceNode().getValue().equals(srcNodeId)) {
                continue;
            }
            if (!dst.getDestNode().getValue().equals(dstNodeId)) {
                continue;
            }
            foundLink = link;
            break;
        }
        return foundLink;
    }

    public static String getLinkSrcPort(Link link) {
        String port = null;
        String srcTpId = link.getSource().getSourceTp().getValue();
        if (srcTpId.startsWith(OPENFLOW_NODE_PREFIX)) {
            port = srcTpId.split(":")[2];
        }
        return port;
    }

    public static String getLinkDstPort(Link link) {
        String port = null;
        String dstTpId = link.getDestination().getDestTp().getValue();
        if (dstTpId.startsWith(OPENFLOW_NODE_PREFIX)) {
            port = dstTpId.split(":")[2];
        }
        return port;
    }

    public static String getOfNodeIdByDpid(String dpid) {
        String HEX = "0x";
        String[] addressInBytes = dpid.split(":");
        Long address =
                (Long.decode(HEX + addressInBytes[2]) << 40) |
                (Long.decode(HEX + addressInBytes[3]) << 32) |
                (Long.decode(HEX + addressInBytes[4]) << 24) |
                (Long.decode(HEX + addressInBytes[5]) << 16) |
                (Long.decode(HEX + addressInBytes[6]) << 8 ) |
                (Long.decode(HEX + addressInBytes[7]));
        return OPENFLOW_NODE_PREFIX + String.valueOf(address);
    }

    public static String getDpidByOfNodeId(String ofNodeId) {
        String dpid = ofNodeId;
        if (dpid.startsWith(OPENFLOW_NODE_PREFIX)) {
            dpid = dpid.split(":")[1];
        }
        String longStr = Long.toHexString(Long.valueOf(dpid).longValue());
        int i;
        dpid = "";

        // Add preamble 0 if needed
        if (longStr.length() % 2 != 0) {
            longStr = "0" + longStr;
        }

        // Add "00" for the highest order if needed
        for (i = 0; i < (16 - longStr.length())/2; i++) {
            if (!dpid.isEmpty()) {
                dpid += ":";
            }
            dpid += "00";
        }

        for (i = 0; i < longStr.length(); i += 2) {
            if (!dpid.isEmpty()) {
                dpid += ":";
            }
            dpid += longStr.substring(i, i+2);
        }
        return dpid;
    }

    public static String getOfNodeIdByOvsdbNodeId(String ovsdbNodeId) {
        Node node = getOvsdbNode(ovsdbNodeId);
        if (node == null) {
            return null;
        }

        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation == null) {
            return null;
        }
        String datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
        if (datapathId == null) {
            return null;
        }
        return getOfNodeIdByDpid(datapathId);
    }

    private static String getOfNodeIdByHostAddress(Object obj, boolean isIp) {
        String tpId = getTpIdByHostAddress(obj, isIp);
        if (tpId == null || tpId.isEmpty()) {
            return null;
        }
        String[] tpIdItems = tpId.split(":");
        String ofNodeId = tpIdItems[0] + ":" + tpIdItems[1];
        return ofNodeId;
    }

    public static String getOfNodeIdByVMAddress(IpAddress ip) {
        return getOfNodeIdByHostAddress(ip, true);
    }

    public static String getOfNodeIdByVMAddress(MacAddress mac) {
        return getOfNodeIdByHostAddress(mac, false);
    }

    public static String getOvsdbNodeIdByDpid(String dpid) {
        Topology topo = getOvsdbTopology();
        for (Node node : topo.getNode()) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
            if (ovsdbBridgeAugmentation == null) {
                continue;
            }
            String datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
            if (datapathId == null) {
                continue;
            }
            if (!datapathId.equals(dpid)) {
                continue;
            }
            return node.getNodeId().getValue();
        }
        return null;
    }

    public static String getOvsdbNodeIdByVMAddress(IpAddress ip) {
        String ofNodeId = getOfNodeIdByVMAddress(ip);
        String dpid = getDpidByOfNodeId(ofNodeId);
        return getOvsdbNodeIdByDpid(dpid);
    }

    public static String getOvsdbNodeIdByVMAddress(MacAddress mac) {
        String ofNodeId = getOfNodeIdByVMAddress(mac);
        String dpid = getDpidByOfNodeId(ofNodeId);
        return getOvsdbNodeIdByDpid(dpid);
    }
}

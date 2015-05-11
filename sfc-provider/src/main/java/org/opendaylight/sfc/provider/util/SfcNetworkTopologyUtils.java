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

    private static String getPortByHostAddress(Topology topo, Object obj, boolean isIp) {
        String nodeId = null;
        String port = null;
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
                    String tpId = attachmentPoints.getTpId().getValue();
                    port = tpId.split(":")[2];
                    break;
                }
            }
        }
        return port;
    }

    public static String getPortByHostIp(Topology topo, IpAddress ip) {
        return getPortByHostAddress(topo, ip, true);
    }

    public static String getPortByHostMac(Topology topo, MacAddress mac) {
        return getPortByHostAddress(topo, mac, false);
    }

    public static Link getLink(Topology topo, String srcNodeId, String dstNodeId) {
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
}

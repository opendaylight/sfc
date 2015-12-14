/*
 * Copyright (c) 2014, 2015 Intel Corporation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.topology;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * This class represents a topology graph, node/vertex
 * is a SF (Service Function) or SFF (Service Function Forwarder),
 * edge is a unidirect and direct connection between two
 * nodes/vertexes, it is mainly used to implement Dijkstra
 * shortest path algorithm, method getShortestPath can find
 * the shortest path between 'from' node and 'to' node in a graph.
 * <p>
 *
 * @author Hongjun Ni (hongjun.ni@intel.com)
 *
 * <p>
 * @since 2015-12-05
 */
public class SfcProviderTopologyUtil {
    private static final String FLOW_TOPO_ID = "flow:1";

    /**
     * This method gets a link (edge) in network topology per
     * the given source node ID and destination node ID.
     * <p>
     *
     * @param srcNodeId source node ID
     * @param dstNodeId destination node ID
     * @return Link the link corresponding to srcNodeId and dstNodeId
     */
    public static Link getLink(String srcNodeId, String dstNodeId) {
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(new TopologyId("flow:1")));
        Topology topo = SfcDataStoreAPI.readTransactionAPI(topoIID, LogicalDatastoreType.OPERATIONAL);

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
}

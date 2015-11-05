/*
* Copyright (c) 2014 Intel Corporation. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.sfc.provider.topology;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This class represents a topology graph, node/vertex
 * is a SF (Service Function) or SFF (Service Function Forwarder),
 * edge is a unidirect and direct connection between two
 * nodes/vertexes, it is mainly used to implement weighted Dijkstra
 * shortest path algorithm, method getWeightedShortestPath can find
 * the weighted shortest path between 'from' node and 'to' node in a graph.
 * <p>
 *
 * @author Hongjun Ni (hongjun.ni@intel.com)
 *
 * <p>
 * @since 2015-10-23
 */
public class SfcProviderWeightedGraph {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderWeightedGraph.class);
    private static final HashMap<SfcProviderTopologyNode, Integer> EMPTY_MAP = new HashMap<SfcProviderTopologyNode, Integer>();
    private static final int WHITE     = 2;
    private static final int GRAY     = 1;
    private static final int BLACK    = 0;
    private static final int INFINITY    = Integer.MAX_VALUE;

    private HashMap<String, SfcProviderTopologyNode> sfcProviderTopoNodes;
    private HashMap<SfcProviderTopologyNode, HashMap<SfcProviderTopologyNode, Integer>> sfcProviderTopoEdges;
    private HashMap<SfcProviderTopologyNode, Integer> sfcProviderShortestPath;
    private int nodeNum;
    private int edgeNum;

    public SfcProviderWeightedGraph() {
        sfcProviderTopoNodes = new HashMap<String, SfcProviderTopologyNode>();
        sfcProviderTopoEdges = new HashMap<SfcProviderTopologyNode, HashMap<SfcProviderTopologyNode, Integer>>();
        sfcProviderShortestPath = new HashMap<SfcProviderTopologyNode, Integer>();
        nodeNum = 0;
        edgeNum = 0;
    }

    public SfcProviderTopologyNode addNode(String nodeName) {
        SfcProviderTopologyNode node;
        node = sfcProviderTopoNodes.get(nodeName);
        if (node == null) {
            node = new SfcProviderTopologyNode(nodeName);
            sfcProviderTopoNodes.put(nodeName, node);
            sfcProviderTopoEdges.put(node, new HashMap<SfcProviderTopologyNode, Integer>());
            nodeNum++;
        }
        return node;
    }

    public SfcProviderTopologyNode getNode(String nodeName) {
        return sfcProviderTopoNodes.get(nodeName);
    }

    public boolean hasNode(String nodeName) {
        return sfcProviderTopoNodes.containsKey(nodeName);
    }

    public boolean hasEdge(String fromNodeName, String toNodeName) {
        SfcProviderTopologyNode fromNode;
        SfcProviderTopologyNode toNode;
        if (!hasNode(fromNodeName) || !hasNode(toNodeName)) {
            return false;
        }
        fromNode = sfcProviderTopoNodes.get(fromNodeName);
        toNode = sfcProviderTopoNodes.get(toNodeName);
        return sfcProviderTopoEdges.get(fromNode).containsKey(toNode);
    }

    public boolean addWeightedEdge(String fromNodeName, String toNodeName, Integer Weight) {
        SfcProviderTopologyNode fromNode;
        SfcProviderTopologyNode toNode;

        if (!hasEdge(fromNodeName, toNodeName)) {
            fromNode = getNode(fromNodeName);
            if (fromNode == null) {
                fromNode = addNode(fromNodeName);
            }
            toNode = getNode(toNodeName);
            if (toNode == null) {
                toNode = addNode(toNodeName);
            }
            sfcProviderTopoEdges.get(fromNode).put(toNode, Weight);
            sfcProviderTopoEdges.get(toNode).put(fromNode, Weight);
        }
        return true;
    }

    public HashMap<SfcProviderTopologyNode, Integer> getWeightedNeighborNodes(String nodeName) {
        if (!hasNode(nodeName)) {
            return EMPTY_MAP;
        }
        return sfcProviderTopoEdges.get(getNode(nodeName));
    }

    public Iterable<SfcProviderTopologyNode> getAllNodes() {
        return sfcProviderTopoNodes.values();
    }

    private void breadthWeightedFirstSearch(String fromNodeName) {
        /* Reset all nodes' color, dist, parent */
        for (SfcProviderTopologyNode sfcNode : getAllNodes()) {
            sfcNode.setColor(WHITE);
            sfcNode.setDist(INFINITY);
            sfcNode.setParent(null);
        }

        /* Mark fromNode as GRAY */
        SfcProviderTopologyNode startNode = getNode(fromNodeName);
        startNode.setColor(GRAY);
        startNode.setDist(0);
        startNode.setParent(null);

        Queue<SfcProviderTopologyNode> queue = new LinkedList<SfcProviderTopologyNode>();
        queue.offer(startNode);
        /* set up initial distance, copied from Edge */
        for (Map.Entry<SfcProviderTopologyNode, Integer> entry: getWeightedNeighborNodes(startNode.getName()).entrySet() ) {
            SfcProviderTopologyNode sfcNode = entry.getKey();
            if ( sfcNode.getColor() == WHITE ) {
                sfcNode.setDist(entry.getValue());
                sfcNode.setParent(sfcNode);
            }
        }
        while (!queue.isEmpty()) {
            /* get entry with minimum distance */
            int minDist = INFINITY;
            SfcProviderTopologyNode CurNode = null;
            SfcProviderTopologyNode qSfcNode = queue.poll();
            for (Map.Entry<SfcProviderTopologyNode, Integer> entry: getWeightedNeighborNodes(qSfcNode.getName()).entrySet() ) {
                SfcProviderTopologyNode sfcNode = entry.getKey();
                if ( (sfcNode.getColor() == WHITE) && (sfcNode.getDist() < minDist) ) {
                    CurNode = sfcNode;
                    minDist = sfcNode.getDist();
                }
            }
            if (minDist == INFINITY)
            {
                break;
            }
            else
            {
                CurNode.setColor(GRAY);
                queue.offer(CurNode);
                CurNode.setParent(qSfcNode);
            }
            //update distance
            for (Map.Entry<SfcProviderTopologyNode, Integer> entry: getWeightedNeighborNodes(CurNode.getName()).entrySet() ) {
                SfcProviderTopologyNode csfcNode = entry.getKey();
                if(csfcNode.getColor() == WHITE) {
                    int newDist = CurNode.getDist() + entry.getValue();
                    if(csfcNode.getDist() > newDist) {
                        csfcNode.setDist(newDist);
                        csfcNode.setParent(CurNode);
                    }
                }
            }
            qSfcNode.setColor(BLACK);
        }
        return;
    }

    public int getWeightedShortestPath(String fromNodeName, String toNodeName) {
        SfcProviderTopologyNode fromNode = getNode(fromNodeName);
        SfcProviderTopologyNode toNode = getNode(toNodeName);
        if (fromNode == null || toNode == null) {
            LOG.error(" Node {} or {} doesn't exist in topology graph!", fromNodeName, toNodeName);
            return INFINITY;
        }

        List<SfcProviderTopologyNode> sfcProviderTopologyNodePath = new ArrayList<SfcProviderTopologyNode>();
        if (fromNodeName.equals(toNodeName)) {
            sfcProviderTopologyNodePath.add(0, fromNode);
            return 0;
        }

        breadthWeightedFirstSearch(fromNodeName);

        SfcProviderTopologyNode sfcProviderTopologyNode = getNode(toNodeName);
        while (sfcProviderTopologyNode != null) {
            sfcProviderTopologyNodePath.add(0, sfcProviderTopologyNode);
            sfcProviderTopologyNode = sfcProviderTopologyNode.getParent();
        }

        /* No path if the first node isn't fromNode, so clear it. */
        if (sfcProviderTopologyNodePath.size() != 0
            && !sfcProviderTopologyNodePath.get(0).equals(fromNode)) {
            sfcProviderTopologyNodePath.clear();
        }

        return getNode(toNodeName).getDist();
    }
}

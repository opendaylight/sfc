/*
 * Copyright (c) 2014, 2015 Intel Corporation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class represents a topology graph, node/vertex
 * is a SF (Service Function) or SFF (Service Function Forwarder),
 * edge is a unidirect and direct connection between two
 * nodes/vertexes, it is mainly used to implement Dijkstra
 * shortest path algorithm, method getShortestPath can find
 * the shortest path between 'from' node and 'to' node in a graph.
 * <p>
 *
 * @author Shuqiang Zhao (shuqiangx.zhao@intel.com)
 * @author Yi Yang (yi.y.yang@intel.com)
 *
 * <p>
 * @since 2015-03-13
 */
public class SfcProviderGraph {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderGraph.class);
    private static final TreeSet<SfcProviderTopologyNode> EMPTY_SET = new TreeSet<SfcProviderTopologyNode>();
    private static final int WHITE     = 2;
    private static final int GRAY     = 1;
    private static final int BLACK    = 0;
    private static final int MAX    = 10000;

    private HashMap<SfcProviderTopologyNode, TreeSet<SfcProviderTopologyNode>> sfcProviderTopoEdges;
    private HashMap<String, SfcProviderTopologyNode> sfcProviderTopoNodes;
    private int nodeNum;
    private int edgeNum;

    public SfcProviderGraph() {
        sfcProviderTopoEdges = new HashMap<SfcProviderTopologyNode, TreeSet<SfcProviderTopologyNode>>();
        sfcProviderTopoNodes = new HashMap<String, SfcProviderTopologyNode>();
        nodeNum = 0;
        edgeNum = 0;
    }

    public SfcProviderTopologyNode addNode(String nodeName) {
        SfcProviderTopologyNode node;
        node = sfcProviderTopoNodes.get(nodeName);
        if (node == null) {
            node = new SfcProviderTopologyNode(nodeName);
            sfcProviderTopoNodes.put(nodeName, node);
            sfcProviderTopoEdges.put(node, new TreeSet<SfcProviderTopologyNode>());
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
        return sfcProviderTopoEdges.get(fromNode).contains(toNode);
    }

    public boolean addEdge(String fromNodeName, String toNodeName) {
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
            sfcProviderTopoEdges.get(fromNode).add(toNode);
            sfcProviderTopoEdges.get(toNode).add(fromNode);
        }
        return true;
    }

    public Iterable<SfcProviderTopologyNode> getNeighborNodes(String nodeName) {
        if (!hasNode(nodeName)) {
            return EMPTY_SET;
        }
        return sfcProviderTopoEdges.get(getNode(nodeName));
    }

    public Iterable<SfcProviderTopologyNode> getAllNodes() {
        return sfcProviderTopoNodes.values();
    }

    private void breadthFirstSearch(String fromNodeName) {
        /* Reset all nodes' color, dist, parent */
        for (SfcProviderTopologyNode sfcNode : getAllNodes()) {
            sfcNode.setColor(WHITE);
            sfcNode.setDist(0);
            sfcNode.setParent(null);
        }

        /* Mark fromNode as GRAY */
        SfcProviderTopologyNode sfcProviderTopologyNode = getNode(fromNodeName);
        sfcProviderTopologyNode.setColor(GRAY);
        sfcProviderTopologyNode.setDist(0);
        sfcProviderTopologyNode.setParent(null);

        Queue<SfcProviderTopologyNode> queue = new LinkedList<SfcProviderTopologyNode>();
        queue.offer(sfcProviderTopologyNode);

        while (!queue.isEmpty()) {
            SfcProviderTopologyNode qSfcNode = queue.poll();
            for (SfcProviderTopologyNode sfcNode : getNeighborNodes(qSfcNode.getName())) {
                if (sfcNode.getColor() == WHITE) {
                    sfcNode.setColor(GRAY);
                    sfcNode.setDist(qSfcNode.getDist() + 1);
                    queue.offer(sfcNode);
                    sfcNode.setParent(qSfcNode);
                }
            }
            qSfcNode.setColor(BLACK);
        }
        return;
    }

    public List<SfcProviderTopologyNode> getShortestPath(String fromNodeName, String toNodeName) {
        SfcProviderTopologyNode fromNode = getNode(fromNodeName);
        SfcProviderTopologyNode toNode = getNode(toNodeName);
        if (fromNode == null || toNode == null) {
            LOG.error(" Node {} or {} doesn't exist in topology graph!", fromNodeName, toNodeName);
            return null;
        }

        List<SfcProviderTopologyNode> sfcProviderTopologyNodePath = new ArrayList<SfcProviderTopologyNode>();
        if (fromNodeName.equals(toNodeName)) {
            sfcProviderTopologyNodePath.add(0, fromNode);
            return sfcProviderTopologyNodePath;
        }

        breadthFirstSearch(fromNodeName);
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
        return sfcProviderTopologyNodePath;
    }
}

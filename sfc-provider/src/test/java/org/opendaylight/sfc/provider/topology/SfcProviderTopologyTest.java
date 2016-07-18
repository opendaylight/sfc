/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;

/**
 * This class contains unit tests for SfcProviderGraph
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-06-30
 */
public class SfcProviderTopologyTest {

    private static final String NODE_NAME = "nodeName";
    private SfcProviderGraph sfcProviderGraph;

    @Before
    public void init() {
        sfcProviderGraph = new SfcProviderGraph();
    }

    /*
     * create a topology graph, topology nodes are created first
     * then edges (connections) are created between them
     * at last, test shortest path
     */
    @Test
    public void testTopology() {
        SfcProviderTopologyNode sfcProviderTopologyNode;

        //add two nodes
        sfcProviderTopologyNode = sfcProviderGraph.addNode(NODE_NAME + 2);
        assertNotNull("Must not be null", sfcProviderTopologyNode);
        sfcProviderTopologyNode = sfcProviderGraph.addNode(NODE_NAME + 3);
        assertNotNull("Must not be null", sfcProviderTopologyNode);

        //"get" node whether a correct result is returned
        sfcProviderTopologyNode = sfcProviderGraph.getNode(NODE_NAME + 2);

        assertNotNull("Must be not null", sfcProviderTopologyNode);
        assertEquals("Must be equal", sfcProviderTopologyNode.getName(), NODE_NAME + 2);

        //"has" node, whether node exists or not
        assertTrue("Must be true", sfcProviderGraph.hasNode(NODE_NAME + 3));

        //this node does not exist
        assertFalse("Must be false", sfcProviderGraph.hasNode(NODE_NAME + 1));

        //now create an edge (connection) between node 2 & node 3
        boolean edgeAdded = sfcProviderGraph.addEdge(NODE_NAME + 2, NODE_NAME + 3);
        assertTrue("Must be true", edgeAdded);

        //edge between node 3 & node 4 - node 4 does not exist, so will be created
        edgeAdded = sfcProviderGraph.addEdge(NODE_NAME + 3, NODE_NAME + 4);
        assertTrue("Must be true", edgeAdded);

        //edge between node 1 & node 2 - node 1 does not exist, will be created
        edgeAdded = sfcProviderGraph.addEdge(NODE_NAME + 1, NODE_NAME + 2);
        assertTrue("Must be true", edgeAdded);

        //test if edge really exists
        boolean hasEdge = sfcProviderGraph.hasEdge(NODE_NAME + 1, NODE_NAME + 2);
        assertTrue("Must be true", hasEdge);

        //edge does not exist
        hasEdge = sfcProviderGraph.hasEdge(NODE_NAME + 1, NODE_NAME + 3);
        assertFalse("Must be false", hasEdge);

        //node does not exist
        hasEdge = sfcProviderGraph.hasEdge(NODE_NAME + 1, NODE_NAME + 5);
        assertFalse("Must be false", hasEdge);

        //try to find neighbor of non-existing node, should return empty tree set
        TreeSet<?> treeSet = (TreeSet) sfcProviderGraph.getNeighborNodes(NODE_NAME + 5);
        assertTrue("Must be true", treeSet.isEmpty());

        //four nodes are created now, there is also edge like 1-2-3-4
        sfcProviderTopologyNode = sfcProviderGraph.getNode(NODE_NAME + 1);
        assertEquals("Must be equal", sfcProviderTopologyNode.getName(), NODE_NAME + 1);
        sfcProviderTopologyNode = sfcProviderGraph.getNode(NODE_NAME + 2);
        assertEquals("Must be equal", sfcProviderTopologyNode.getName(), NODE_NAME + 2);
        sfcProviderTopologyNode = sfcProviderGraph.getNode(NODE_NAME + 3);
        assertEquals("Must be equal", sfcProviderTopologyNode.getName(), NODE_NAME + 3);
        sfcProviderTopologyNode = sfcProviderGraph.getNode(NODE_NAME + 4);
        assertEquals("Must be equal", sfcProviderTopologyNode.getName(), NODE_NAME + 4);

        //create additional edge between 2-4, so shortest path will be 1-2-4
        edgeAdded = sfcProviderGraph.addEdge(NODE_NAME + 2, NODE_NAME + 4);
        assertTrue("Must be true", edgeAdded);

        //test shortest path
        List<SfcProviderTopologyNode> sfcProviderTopologyNodeList = sfcProviderGraph.getShortestPath(NODE_NAME + 1, NODE_NAME + 4);

        //created path should have three entries - nodes 1, 2 & 4
        assertNotNull("Must be not null", sfcProviderTopologyNodeList);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.size(), 3);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.get(0).getName(), NODE_NAME + 1);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.get(0).getDist(), 0);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.get(1).getName(), NODE_NAME + 2);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.get(1).getDist(), 1);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.get(2).getName(), NODE_NAME + 4);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.get(2).getDist(), 2);

        //create path with non-existing node, should return null
        sfcProviderTopologyNodeList = sfcProviderGraph.getShortestPath(NODE_NAME + 1, NODE_NAME + 5);
        assertNull("Must be null", sfcProviderTopologyNodeList);

        //create path, when the src node is equal dst node, should return that node
        sfcProviderTopologyNodeList = sfcProviderGraph.getShortestPath(NODE_NAME + 1, NODE_NAME + 1);
        assertEquals("Must be equal", sfcProviderTopologyNodeList.get(0).getName(), NODE_NAME + 1);
    }
}

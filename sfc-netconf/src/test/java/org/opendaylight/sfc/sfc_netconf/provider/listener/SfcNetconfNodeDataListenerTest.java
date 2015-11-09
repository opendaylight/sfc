/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_netconf.provider.listener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SfcNetconfNodeDataListenerTest Tester.
 *
 * @author Hongli Chen (honglix.chen@intel.com)
 * @version 1.0
 * @since 2015-11-04
 */

public class SfcNetconfNodeDataListenerTest  extends AbstractDataBrokerTest {
    private SfcNetconfNodeDataListener sfcNetconfNodeDataListener;
    private ListenerRegistration<DataChangeListener> sfcNetconfNodeDataListenerRegistration;
    public InstanceIdentifier<Topology> NETCONF_TOPO_IID;
    public Logger LOG;
    public String notification_capability_prefix;

    private static boolean executorSet = false;

    protected DataBroker dataBroker;
    protected static ExecutorService executor;
    protected final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    @Before
    public void before() throws Exception {
        setOpendaylightSfc();
        sfcNetconfNodeDataListener =
            new SfcNetconfNodeDataListener(opendaylightSfc);

        LOG = LoggerFactory.getLogger(SfcNetconfNodeDataListenerTest.class);
        notification_capability_prefix = "(urn:ietf:params:xml:ns:netconf:notification";
    }

    @After
    public void after() throws Exception {
    }

    protected void setOpendaylightSfc() {
        if(!executorSet) {
            executor = opendaylightSfc.getExecutor();
            executorSet = true;
        }
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
    }

    public ListenerRegistration<DataChangeListener> registerAsDataChangeListener() {
        NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

        return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID,
                sfcNetconfNodeDataListener, DataBroker.DataChangeScope.SUBTREE);
    }
    /**
     * Creates NetconfNode object, call listeners explicitly
     * cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_CreateData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        Topology topology = build_topology();
        TopologyId topologyId = new TopologyId(TopologyNetconf.QNAME.getLocalName());
        assertTrue(putTopology(topology));

        NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

        createdData.put(NETCONF_TOPO_IID, topology);

        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        // Empty MAPs below
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);

        sfcNetconfNodeDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);

        Topology topologyRead = readTopology(topologyId);
        assertNotNull(topologyRead);
        assertEquals(topology, topologyRead);
        Thread.sleep(500);
        // Clean-up
        assertTrue(deleteTopology(topologyId));
        Thread.sleep(500);
    }





    /**
     * In order to simulate an update from the data store this test does the following:
     * - creates NetconfNode object and commits to data store
     * - Creates a copy of the original NetconfNode and updates the NAME
     * - Feeds the original and updated NetconfNode to the listener
     * - Asserts that the listener has removed the original and created a new entry
     * - Cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_UpdateData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        /* Create and commit NetconfNode */
        Topology topology = build_topology();
        TopologyId topologyId = new TopologyId(TopologyNetconf.QNAME.getLocalName());
        assertTrue(putTopology(topology));

        NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

        originalData.put(NETCONF_TOPO_IID, topology);

        Node nodeUpdate = getNetconfNode("NodeId2", "nodeTest2", ConnectionStatus.Connected,
                    notification_capability_prefix);
        List<Node> nodeListUpdate = new ArrayList<Node>();
        nodeListUpdate.add(nodeUpdate);
        TopologyKey topologyKey = new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()));
        Topology topologyUpdate = new TopologyBuilder()
            .setNode(nodeListUpdate)
            .setKey(topologyKey)
            .setTopologyId(topologyId).build();
        assertTrue(putTopology(topologyUpdate));
        updatedData.put(NETCONF_TOPO_IID, topologyUpdate);

        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

        /*
         * The listener will remove the Original NetconfNode and create a new one
         * with the new type
         */
        sfcNetconfNodeDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        Topology topologyRead = readTopology(topologyId);
        assertNotNull(topologyRead);

        List<Node> nodeListRead = topologyRead.getNode();
        Node nodeRead = new NodeBuilder().build();
        NodeId nodeId = new NodeId("NodeId2");
        NodeKey nk = new NodeKey(nodeId);
        for(Node nd: nodeListRead) {
            if(nd.getKey().equals(nk)) {
                nodeRead = nd;
            }
        }
        assertEquals(nodeUpdate, nodeRead);

        // Clean-up
        assertTrue(deleteTopology(topologyId));
        Thread.sleep(500);
    }

    /**
     * In order to simulate a removal from the data store this test does the following:
     * - creates NetconfNode object and inserts it into an MAP data structure representing the original
     * data
     * - creates a IID and add to removed data structure. This IID points to the NetconfNode objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_RemoveData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        Topology topology = build_topology();
        TopologyId topologyId = new TopologyId(TopologyNetconf.QNAME.getLocalName());

        NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

        originalData.put(NETCONF_TOPO_IID, topology);
        removedPaths.add(NETCONF_TOPO_IID);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);

        sfcNetconfNodeDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        assertNull(readTopology(topologyId));
    }

    public Node getNetconfNode(String nodeIdent, String hostName, ConnectionStatus cs,
        String notificationCapabilityPrefix) {
        DomainName dn = new DomainName(hostName);
        Host host = new Host(dn);
        List<String> avCapList = new ArrayList<>();
        avCapList.add(notificationCapabilityPrefix + "_availableCapabilityString1");

        AvailableCapabilities avCaps = new AvailableCapabilitiesBuilder().setAvailableCapability(avCapList).build();

        NetconfNode nn = new NetconfNodeBuilder().setConnectionStatus(cs).setHost(host).setAvailableCapabilities(avCaps)
            .build();

        NodeId nodeId = new NodeId(nodeIdent);
        NodeKey nk = new NodeKey(nodeId);
        NodeBuilder nb = new NodeBuilder();
        nb.setKey(nk);

        nb.addAugmentation(NetconfNode.class, nn);
        return nb.build();
    }

    public Topology build_topology() {
        Node node = getNetconfNode("NodeId1", "nodeTest1", ConnectionStatus.Connected,
                    notification_capability_prefix);
        List<Node> nodeList = new ArrayList<Node>();
        nodeList.add(node);
        TopologyKey topologyKey = new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()));
        TopologyId topologyId = new TopologyId(TopologyNetconf.QNAME.getLocalName());
        Topology topology = new TopologyBuilder()
            .setNode(nodeList)
            .setKey(topologyKey)
            .setTopologyId(topologyId).build();
        return topology;
    }

    public boolean putTopology(Topology topology) {
        boolean ret;

        NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
        ret = SfcDataStoreAPI.writePutTransactionAPI(NETCONF_TOPO_IID, topology, LogicalDatastoreType.OPERATIONAL);

        return ret;
    }

    public Topology readTopology(TopologyId topologyId) {
        Topology topology;
        TopologyKey topologyKey = new TopologyKey(topologyId);

        NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
        topology = SfcDataStoreAPI.readTransactionAPI(NETCONF_TOPO_IID, LogicalDatastoreType.OPERATIONAL);

        return topology;
    }

    public boolean deleteTopology(TopologyId topologyId) {
        boolean ret = false;
        TopologyKey topologyKey = new TopologyKey(topologyId);

        NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
        if (SfcDataStoreAPI.deleteTransactionAPI(NETCONF_TOPO_IID, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            System.out.println("Could not delete topology: "+topologyId);
        }
        return ret;
    }

}

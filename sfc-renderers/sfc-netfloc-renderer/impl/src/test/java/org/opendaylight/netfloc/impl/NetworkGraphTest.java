/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;

import org.opendaylight.neutron.spi.NeutronPort;
import ch.icclab.netfloc.iface.IBridgeIterator;
import ch.icclab.netfloc.iface.IBridgeOperator;
import ch.icclab.netfloc.iface.ILinkPort;
import ch.icclab.netfloc.iface.INetworkPath;
import ch.icclab.netfloc.iface.INetworkPathListener;
import ch.icclab.netfloc.iface.INodeOperator;
import ch.icclab.netfloc.iface.IPortOperator;
import ch.icclab.netfloc.iface.IHostPort;
import ch.icclab.netfloc.iface.ITraversableBridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.neutron.spi.Neutron_IPs;

import java.util.List;
import java.util.LinkedList;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

public class NetworkGraphTest {
	
	final NetworkGraph network = new NetworkGraph();
	IBridgeOperator aggregationBridge;
	List<IBridgeOperator> hostBridges = new LinkedList<IBridgeOperator>();
	List<IHostPort> vmPorts = new LinkedList<IHostPort>();
	
	@Before
	public void setUp() {

		// memorize the link ports
		List<IPortOperator> linkPorts = new LinkedList<IPortOperator>();

		// add host bridges
		for (int i = 0; i < 3; i++) {
			Node topologyNodeNode = mock(Node.class);

			when(topologyNodeNode.getNodeId()).thenReturn(new NodeId("node:" + i));

			OvsdbNodeAugmentation nodeAugmentation = mock(OvsdbNodeAugmentation.class);
			INodeOperator node = new Datapath(network, topologyNodeNode, nodeAugmentation);
			network.addNode(node);

			Node topologyNodeBridge = mock(Node.class);

			when(topologyNodeBridge.getNodeId()).thenReturn(new NodeId("node:" + i + ":bridge:" + i));

			OvsdbBridgeAugmentation bridgeAugmentation = mock(OvsdbBridgeAugmentation.class);
			DatapathId dpId = mock(DatapathId.class);
			when(dpId.getValue()).thenReturn("00:00:00:00:0" + i);
			when(bridgeAugmentation.getDatapathId()).thenReturn(dpId);
			IBridgeOperator bridge = new Bridge(node, topologyNodeBridge, bridgeAugmentation);
			node.addBridge(bridge);

			// add vm's ports
			for (int j = 0; j < 5; j++) {
				NeutronPort neutronPort = mock(NeutronPort.class);
				OvsdbTerminationPointAugmentation terminationPointAugmentation1 = mock(OvsdbTerminationPointAugmentation.class);
				when(terminationPointAugmentation1.getOfport()).thenReturn(new Long(j));
				IHostPort vmPort = new HostPort(bridge, mock(TerminationPoint.class), terminationPointAugmentation1, neutronPort);
				bridge.addHostPort(vmPort);
				vmPorts.add(vmPort);
			}
			// add a link port
			OvsdbTerminationPointAugmentation terminationPointAugmentation2 = mock(OvsdbTerminationPointAugmentation.class);
			when(terminationPointAugmentation2.getOfport()).thenReturn(new Long(i));
			IPortOperator linkPort = new LinkPort(bridge, mock(TerminationPoint.class), terminationPointAugmentation2);
			bridge.addPort(linkPort);

			// memorize link ports
			linkPorts.add(linkPort);

			hostBridges.add(bridge);
		}

		// add an aggregation bridge
		Node topologyNode = mock(Node.class);
		when(topologyNode.getNodeId()).thenReturn(new NodeId("node:" + "a" + ":bridge:" + "a"));
		OvsdbNodeAugmentation nodeAugmentation = mock(OvsdbNodeAugmentation.class);
		INodeOperator node = new Datapath(network, topologyNode, nodeAugmentation);
		network.addNode(node);

		OvsdbBridgeAugmentation bridgeAugmentation = mock(OvsdbBridgeAugmentation.class);
		DatapathId dpId = mock(DatapathId.class);
		when(dpId.getValue()).thenReturn("00:00:10:10:10");
		when(bridgeAugmentation.getDatapathId()).thenReturn(dpId);
		aggregationBridge = new Bridge(node, topologyNode, bridgeAugmentation);
		node.addBridge(aggregationBridge);

		// add link ports to aggregation bridge
		for (int i = 0; i < 3; i++) {
			OvsdbTerminationPointAugmentation terminationPointAugmentation = mock(OvsdbTerminationPointAugmentation.class);
			when(terminationPointAugmentation.getOfport()).thenReturn(new Long(i));
			ILinkPort linkPort = new LinkPort(aggregationBridge, mock(TerminationPoint.class), terminationPointAugmentation);
			aggregationBridge.addPort(linkPort);
			IPortOperator linkedPort = linkPorts.get(i);
			linkPort.setLinkedPort((ILinkPort)linkedPort);
		}
	}

	@Test
	public void testGetAdjacentBridges() {
		List<ITraversableBridge> adjacentBridges =  network.getAdjacentBridges(new TraversableBridge(aggregationBridge));
		assertTrue(adjacentBridges.size() == 3);
	}
	
	@Test
	public void getNetworkGraphs() {
		List<INetworkPath> networkPaths = network.getNetworkPaths(); 
		for(INetworkPath path : networkPaths){
			assertTrue(!path.getBeginPort().equals(path.getEndPort()));
		
		}
	
		
		assertTrue(networkPaths!= null);
	}

	@Test
	public void testTraverse() {

		// test abortion condition
		IBridgeIterator<Integer> iterator1 = new IBridgeIterator<Integer>() {

			int result = 0;

			public boolean visitBridge(ITraversableBridge bridge) {
				result++;
				if (result == 4) {
					return false;
				}
				return true;
			}

			public Integer getResult() {
				return new Integer(result);
			}
		};
		network.traverse(iterator1);
		assertTrue(iterator1.getResult() == 4);

		// test BFS condition
		IBridgeIterator<Integer> iterator2 = new IBridgeIterator<Integer>() {

			int result = 0;

			public boolean visitBridge(ITraversableBridge bridge) {
				result++;

				// first bridge does not have root bridge
				if (bridge.getRoot() == null) {
					return true;
				}
				List<ITraversableBridge> adjBr =  network.getAdjacentBridges(bridge.getRoot());
				assertTrue(adjBr.contains(bridge));
				return true;
			}

			public Integer getResult() {
				return new Integer(result);
			}
		};
		network.traverse(iterator2);
		assertTrue(iterator2.getResult() == 4);
	}

	@Test
	public void testGetNetworkPath() {
		IBridgeOperator beginBridge = hostBridges.get(0);
		IBridgeOperator endBridge = hostBridges.get(1);

		IHostPort beginPort = beginBridge.getHostPorts().get(0);
		IHostPort endPort = endBridge.getHostPorts().get(0);
		
		assertTrue(beginPort != null);
		assertTrue(endPort != null);
		assertTrue(network != null);
		
		INetworkPath networkPath = network.getNetworkPath(beginPort, endPort);

		assertTrue(networkPath != null);
		assertTrue("length of nw path is " + networkPath.getLength() + " instead of 3.", networkPath.getLength() == 3);
		assertTrue(networkPath.getBegin().equals(beginBridge));
		assertTrue(networkPath.getEnd().equals(endBridge));
		assertTrue(networkPath.getPrevious(endBridge).equals(aggregationBridge));
		assertTrue(networkPath.getPrevious(aggregationBridge).equals(beginBridge));
	}

	@Test
	public void testAddPort() {
		// A and B are on bridge 1
		// C is on bridge 2
		// Whitelist
		// A <-> B can talk
		// C <-> B can talk

		// register path listeners so we can verify path creation
		INetworkPathListener nplm = mock(INetworkPathListener.class);
		final List<INetworkPath> netpaths = new LinkedList<INetworkPath>();
		INetworkPathListener nplr = new INetworkPathListener() {

			public void networkPathCreated(INetworkPath np) {
				// outer frame reference
				netpaths.add(np);
			}

			public void networkPathUpdated(INetworkPath oldNp, INetworkPath nNp){
				// noop
			}

			public void networkPathDeleted(INetworkPath np) {
				// noop
			}
		};
		network.registerNetworkPathListener(nplm);
		network.registerNetworkPathListener(nplr);

		// bridges
		Node bridgeNode1 = hostBridges.get(0).getNode();
		Node bridgeNode2 = hostBridges.get(1).getNode();

		// mock neutron subnet ips
		Neutron_IPs nip_AB = mock(Neutron_IPs.class);
		when(nip_AB.getSubnetUUID()).thenReturn("idAB");
		Neutron_IPs nip_BC = mock(Neutron_IPs.class);
		when(nip_BC.getSubnetUUID()).thenReturn("idBC");

		assertTrue(network.getHostPorts().size() == 15);

		// PORT A
		NeutronPort np_A = mock(NeutronPort.class);
		List<Neutron_IPs> nipList_A = new LinkedList<Neutron_IPs>();
		nipList_A.add(nip_AB);
		when(np_A.getFixedIPs()).thenReturn(nipList_A);

		OvsdbTerminationPointAugmentation otpa_A = mock(OvsdbTerminationPointAugmentation.class);
		when(otpa_A.getOfport()).thenReturn(11L);
		TerminationPoint tp_A = mock(TerminationPoint.class);

		network.addPort(bridgeNode1, tp_A, otpa_A, np_A);

		// PORT B
		NeutronPort np_B = mock(NeutronPort.class);
		List<Neutron_IPs> nipList_B = new LinkedList<Neutron_IPs>();
		nipList_B.add(nip_AB);
		nipList_B.add(nip_BC);
		when(np_B.getFixedIPs()).thenReturn(nipList_B);

		OvsdbTerminationPointAugmentation otpa_B = mock(OvsdbTerminationPointAugmentation.class);
		when(otpa_B.getOfport()).thenReturn(12L);
		TerminationPoint tp_B = mock(TerminationPoint.class);

		network.addPort(bridgeNode1, tp_B, otpa_B, np_B);

		// PORT C
		NeutronPort np_C = mock(NeutronPort.class);
		List<Neutron_IPs> nipList_C = new LinkedList<Neutron_IPs>();
		nipList_C.add(nip_BC);
		when(np_C.getFixedIPs()).thenReturn(nipList_C);

		OvsdbTerminationPointAugmentation otpa_C = mock(OvsdbTerminationPointAugmentation.class);
		when(otpa_C.getOfport()).thenReturn(13L);
		TerminationPoint tp_C = mock(TerminationPoint.class);

		network.addPort(bridgeNode2, tp_C, otpa_C, np_C);

		// check if listener was called exactly 2 times for A <-> B and B <-> C
		assertTrue("there should be 18 host ports at this point", network.getHostPorts().size() == 18);
		verify(nplm, times(2)).networkPathCreated(any(INetworkPath.class));

		// examine the paths
		assertTrue(netpaths.size() == 2);
		INetworkPath np_AB = netpaths.get(0);
		INetworkPath np_BC = netpaths.get(1);

		assertTrue(np_AB.getLength() == 1);
		assertTrue(np_BC.getLength() == 3);

		assertTrue(np_AB.getBegin().equals(hostBridges.get(0)));
		assertTrue(np_AB.getEnd().equals(hostBridges.get(0)));
		assertTrue(np_BC.getBegin().equals(hostBridges.get(0)) && np_BC.getEnd().equals(hostBridges.get(1)) || np_BC.getBegin().equals(hostBridges.get(1)) && np_BC.getEnd().equals(hostBridges.get(0)));
	}

	@Test
	public void testDeleteLink() {
		Link link = mock(Link.class);
		Source src = mock(Source.class);
		TpId srcTpId = mock(TpId.class);
		Destination dst = mock(Destination.class);
		TpId dstTpId = mock(TpId.class);

		when(srcTpId.getValue()).thenReturn("openflow:" + Long.parseLong("00:00:10:10:10".replace(":", ""), 16) + ":" + 1L);
		when(dstTpId.getValue()).thenReturn("openflow:" + Long.parseLong("00:00:00:00:01".replace(":", ""), 16) + ":" + 1L);
		when(src.getSourceTp()).thenReturn(srcTpId);
		when(dst.getDestTp()).thenReturn(dstTpId);
		when(link.getSource()).thenReturn(src);
		when(link.getDestination()).thenReturn(dst);

		List<INetworkPath> paths = new LinkedList<INetworkPath>();
		for (IHostPort srcPort : network.getHostPorts()) {
			for (IHostPort dstPort : network.getHostPorts()) {
				if (!srcPort.equals(dstPort)) {
					INetworkPath path = network.getNetworkPath(srcPort, dstPort);
					if (path != null) {
						paths.add(path);
					}
				}
			}
		}
		assertTrue("path number should be 210 instead of " + paths.size(), paths.size() == 210);

		network.deleteLink(link);

		List<ITraversableBridge> adjacentBridges = network.getAdjacentBridges(new TraversableBridge(aggregationBridge));
		assertTrue("adjacentBridges should be 2 instead of " + adjacentBridges.size(), adjacentBridges.size() == 2);

		List<INetworkPath> paths2 = new LinkedList<INetworkPath>();
		for (IHostPort srcPort : network.getHostPorts()) {
			for (IHostPort dstPort : network.getHostPorts()) {
				if (!srcPort.equals(dstPort)) {
					INetworkPath path = network.getNetworkPath(srcPort, dstPort);
					if (path != null) {
						paths2.add(path);
					}
				}
			}
		}
		assertTrue("path number should be 90 instead of " + paths2.size(), paths2.size() == 110);

		network.createLink(link);

		List<ITraversableBridge> adjacentBridges2 = network.getAdjacentBridges(new TraversableBridge(aggregationBridge));
		assertTrue("adjacentBridges should be 3 instead of " + adjacentBridges2.size(), adjacentBridges2.size() == 3);
	}
}
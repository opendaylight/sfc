/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;

import org.opendaylight.neutron.spi.NeutronPort;
import ch.icclab.netfloc.iface.IBridgeOperator;
import ch.icclab.netfloc.iface.ILinkPort;
import ch.icclab.netfloc.iface.INetworkPath;
import ch.icclab.netfloc.iface.INodeOperator;
import ch.icclab.netfloc.iface.IHostPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import java.util.List;
import java.util.LinkedList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkPathTest {

	// @Test
	// public void testCleanupPorts() {
	// 	IHostPort beginPort = new HostPort(mock(NeutronPort.class));
	// 	IHostPort endPort = new HostPort(mock(NeutronPort.class));
	// 	IHostPort peripheralPortTerm = new HostPort(mock(NeutronPort.class));

	// 	Node nodeMock1 = mock(Node.class);
	// 	when(nodeMock1.getNodeId()).thenReturn(new NodeId("1"));
	// 	IBridgeOperator beginBridge = new Bridge(mock(INodeOperator.class), nodeMock1, mock(OvsdbBridgeAugmentation.class));
	// 	beginBridge.addHostPort(beginPort);

	// 	Node nodeMock2 = mock(Node.class);
	// 	when(nodeMock2.getNodeId()).thenReturn(new NodeId("2"));
	// 	IBridgeOperator endBridge = new Bridge(mock(INodeOperator.class), nodeMock2, mock(OvsdbBridgeAugmentation.class));
	// 	endBridge.addHostPort(endPort);
	// 	endBridge.addHostPort(peripheralPortTerm);

	// 	ILinkPort expBegin = new LinkPort(beginBridge, mock(TerminationPoint.class), mock(OvsdbTerminationPointAugmentation.class));
	// 	ILinkPort peripheralPortExt = new LinkPort(beginBridge, mock(TerminationPoint.class), mock(OvsdbTerminationPointAugmentation.class));
	// 	beginBridge.addLinkPort(expBegin);
	// 	beginBridge.addLinkPort(peripheralPortExt);

	// 	ILinkPort expEnd = new LinkPort(endBridge, mock(TerminationPoint.class), mock(OvsdbTerminationPointAugmentation.class));
	// 	endBridge.addLinkPort(expEnd);

	// 	expBegin.setLinkedPort(expEnd);

	// 	INetworkPath np = new NetworkPath(beginPort, endPort);

	// 	np.append(beginBridge);
	// 	np.append(endBridge);
	// 	np.close();

	// 	assertTrue("path should have length 2, has " + np.getLength(), np.getLength() == 2);
	// 	assertTrue("two termination ports before cleanup", np.getEnd().getHostPorts().size() == 2);
	// 	assertTrue("two link ports before cleanup", np.getBegin().getLinkPorts().size() == 2);

	// 	INetworkPath cnp = np.getCleanPath();

	// 	assertTrue("path has same length", cnp.getLength() == 2);
	// 	assertTrue("only one termination port after cleanup", cnp.getEnd().getHostPorts().size() == 1);
	// 	assertTrue("only one link port after cleanup", cnp.getBegin().getLinkPorts().size() == 1);
	// }
}
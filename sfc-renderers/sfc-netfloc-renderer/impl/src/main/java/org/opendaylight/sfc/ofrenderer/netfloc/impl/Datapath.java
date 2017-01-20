/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INetworkOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INodeOperator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import java.util.List;
import java.util.LinkedList;

public class Datapath implements INodeOperator {

	private INetworkOperator network;
	private Node node;
	private OvsdbNodeAugmentation ovsdbNodeAugmentation;
	private List<IBridgeOperator> bridges = new LinkedList<IBridgeOperator>();

	public Datapath(INetworkOperator network, Node node, OvsdbNodeAugmentation ovsdbNodeAugmentation) {
		this.network = network;
		this.node = node;
		this.ovsdbNodeAugmentation = ovsdbNodeAugmentation;
	}

	public static String getDatapathId(Node node) {
    OvsdbBridgeAugmentation baug = node.getAugmentation(OvsdbBridgeAugmentation.class);
    if (baug == null) {
    	throw new IllegalStateException("Bridge augmentation is null for node");
    }
    if (baug.getDatapathId() == null) {
    	throw new IllegalStateException("Datapath id is null for bridge augmentation");
    }
    return baug.getDatapathId().getValue();
  }

  public void update(Node node, OvsdbNodeAugmentation aug) {
  	this.node = node;
  	this.ovsdbNodeAugmentation = aug;
  }

	public INetworkOperator getNetwork() {
		return this.network;
	}

	public String getDatapathId() {
		return Datapath.getDatapathId(this.node);
	}

	public NodeId getNodeId() {
		return this.node.getNodeId();
	}

	public List<IBridgeOperator> getBridges() {
		return this.bridges;
	}

	public void addBridge(IBridgeOperator bridge) {
		this.bridges.add(bridge);
	}

	public void removeBridge(IBridgeOperator bridge) {
		this.bridges.remove(bridge);
	}

	public boolean equals(Object o) {
		return (o instanceof INodeOperator) && ((INodeOperator)o).getNodeId().getValue().equals(this.getNodeId().getValue());
	}
}

/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;

import java.util.List;

public interface INodeOperator {
	public INetworkOperator getNetwork();
	public String getDatapathId();
	public NodeId getNodeId();
	public void update(Node node, OvsdbNodeAugmentation aug);
	public List<IBridgeOperator> getBridges();
	public void addBridge(IBridgeOperator bridge);
	public void removeBridge(IBridgeOperator bridge);
}

/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

public interface INetworkOperator {
	public List<INodeOperator> getNodes();
	public INodeOperator getNode(NodeId id);
	public void addNode(INodeOperator node);
	public void removeNode(INodeOperator node);
	public void removeNode(NodeId id);
	public List<IBridgeOperator> getBridges();
	public ILinkPort getLinkPort(TpId tpid);
}

/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public interface IBridgeOperator {
	public INodeOperator getParentNode();
	public Node getNode();
	public NodeId getNodeId();
	public String getDatapathId();
	public IPortOperator getPort(Uuid id);
	public void addPort(IPortOperator port);
	public void removePort(IPortOperator port);
	public List<IPortOperator> getPorts();
	public IInternalPort getInternalPort(Uuid id);
	public IHostPort getHostPort(Uuid id);
	public ILinkPort getLinkPort(Uuid id);
	public IHostPort getHostPort(String id);
	public List<IInternalPort> getInternalPorts();
	public List<IHostPort> getHostPorts();
	public List<ILinkPort> getLinkPorts();
	public void addInternalPort(IInternalPort internalPort);
	public void addHostPort(IHostPort hostPort);
	public void addLinkPort(ILinkPort linkPort);
	public void removeInternalPort(IInternalPort internalPort);
	public void removeHostPort(IHostPort hostPort);
	public void removeLinkPort(ILinkPort linkPort);
	public OvsdbBridgeAugmentation getAugmentation();
	public void update(Node node, OvsdbBridgeAugmentation ovsdbBridgeAugmentation);
}

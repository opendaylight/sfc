/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.ILinkPort;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IInternalPort;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INodeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IPortOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IHostPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;

public class Bridge implements IBridgeOperator{

	List<IInternalPort> internalPorts = new LinkedList<IInternalPort>();
	List<IHostPort> hostPorts = new LinkedList<IHostPort>();
	List<ILinkPort> linkPorts = new LinkedList<ILinkPort>();
	private INodeOperator parentNode;
	private Node node;
	private OvsdbBridgeAugmentation ovsdbBridgeAugmentation;

	public Bridge(INodeOperator parentNode, Node node, OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
		this.node = node;
		this.parentNode = parentNode;
		this.ovsdbBridgeAugmentation = ovsdbBridgeAugmentation;
	}

	public void update(Node node, OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
		this.node = node;
		this.ovsdbBridgeAugmentation = ovsdbBridgeAugmentation;
	}

	public INodeOperator getParentNode() {
		return this.parentNode;
	}

	public Node getNode() {
		return this.node;
	}

	public NodeId getNodeId() {
		return this.node.getNodeId();
	}

	public String getDatapathId() {
		return Bridge.getDatapathId(this.ovsdbBridgeAugmentation);
	}

	public static String getDatapathId(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
    String datapathId = null;
    if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
        datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
    }
    return datapathId;
  }

  public OvsdbBridgeAugmentation getAugmentation() {
  	return this.ovsdbBridgeAugmentation;
  }

	public List<IInternalPort> getInternalPorts() {
		return this.internalPorts;
	}

	public List<IHostPort> getHostPorts() {
		return this.hostPorts;
	}

	public List<ILinkPort> getLinkPorts() {
		return this.linkPorts;
	}

	public IPortOperator getPort(Uuid id) {
		IPortOperator port;
		port = this.getInternalPort(id);
		if (port != null) {
			return port;
		}
		port = this.getHostPort(id);
		if (port != null) {
			return port;
		}
		port = this.getLinkPort(id);
		if (port != null) {
			return port;
		}
		return port;
	}

	public void addPort(IPortOperator port) {
		if (port instanceof IInternalPort) {
			this.addInternalPort((IInternalPort)port);
			return;
		}
		if (port instanceof IHostPort) {
			this.addHostPort((IHostPort)port);
			return;
		}
		if (port instanceof ILinkPort) {
			this.addLinkPort((ILinkPort)port);
			return;
		}
	}

	public void removePort(IPortOperator port) {
		if (port instanceof IInternalPort) {
			this.removeInternalPort((IInternalPort)port);
			return;
		}
		if (port instanceof IHostPort) {
			this.removeHostPort((IHostPort)port);
			return;
		}
		if (port instanceof ILinkPort) {
			this.removeLinkPort((ILinkPort)port);
			return;
		}
	}

	public List<IPortOperator> getPorts() {
		List<IPortOperator> ports = new LinkedList<IPortOperator>();
		for (IPortOperator port : this.getInternalPorts()) {
			ports.add(port);
		}
		for (IPortOperator port : this.getHostPorts()) {
			ports.add(port);
		}
		for (IPortOperator port : this.getLinkPorts()) {
			ports.add(port);
		}
		return ports;
	}

	public IInternalPort getInternalPort(Uuid id) {
		for (IInternalPort port : this.getInternalPorts()) {
			if (port.getPortUuid().equals(id)) {
				return port;
			}
		}
		return null;
	}

	public IHostPort getHostPort(Uuid id) {
		for (IHostPort port : this.getHostPorts()) {
			if (port.getPortUuid().equals(id)) {
				return port;
			}
		}
		return null;
	}

	public ILinkPort getLinkPort(Uuid id) {
		for (ILinkPort port : this.getLinkPorts()) {
			if (port.getPortUuid().equals(id)) {
				return port;
			}
		}
		return null;
	}

	public IHostPort getHostPort(String id) {
		for (IHostPort port : this.getHostPorts()) {
			if (port.getNeutronUuid().equals(id)) {
				return port;
			}
		}
		return null;
	}

	public void addInternalPort(IInternalPort internalPort) {
		this.internalPorts.add(internalPort);
	}

	public void addHostPort(IHostPort hostPort) {
		this.hostPorts.add(hostPort);
	}

	public void addLinkPort(ILinkPort linkPort) {
		this.linkPorts.add(linkPort);
	}

	public void removeInternalPort(IInternalPort internalPort) {
		this.internalPorts.remove(internalPort);
	}

	public void removeHostPort(IHostPort hostPort) {
		this.hostPorts.remove(hostPort);
	}

	public void removeLinkPort(ILinkPort linkPort) {
		this.linkPorts.remove(linkPort);
	}

	public boolean equals(Object o) {
		if (!(o instanceof Bridge)) {
			return false;
		}
		assert ((Bridge)o).getNodeId() != null : "Node id cannot be null";
		assert ((Bridge)o).getNodeId().getValue() != null : "Node id value cannot be null";
		assert this.getNodeId() != null : "Node id cannot be null";
		assert this.getNodeId().getValue() != null : "Node id value cannot be null";
		return ((Bridge)o).getNodeId().getValue().equals(this.getNodeId().getValue());
	}

}

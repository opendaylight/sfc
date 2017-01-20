/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import ch.icclab.netfloc.iface.IBridgeOperator;
import ch.icclab.netfloc.iface.ILinkPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

public class LinkPort extends Port implements ILinkPort {

	private ILinkPort linkedPort;

	public LinkPort(IBridgeOperator bridge, TerminationPoint tp, OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation) {
		super(bridge, tp, ovsdbTerminationPointAugmentation);
	}

	public ILinkPort getLinkedPort() {
		return linkedPort;
	}

	public void setLinkedPort(ILinkPort linkedPort) {
		this.linkedPort = linkedPort;
		if (linkedPort.getLinkedPort() == null) {
			linkedPort.setLinkedPort(this);
		}
	}

	public void removeLinkedPort(ILinkPort linkedPort) {
		this.linkedPort = null;
		if (linkedPort == null) {
			return;
		}
		if (linkedPort.getLinkedPort() != null) {
			linkedPort.removeLinkedPort(this);
		}
	}
}

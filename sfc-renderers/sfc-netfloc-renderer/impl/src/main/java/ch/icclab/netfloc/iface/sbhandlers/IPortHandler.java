/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface.sbhandlers;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

public interface IPortHandler {
	public void handlePortCreate(Node node, TerminationPoint tp, OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation);
	public void handlePortDelete(Node node, OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation);
	public void handlePortUpdate(Node node, TerminationPoint tp, OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation);
}
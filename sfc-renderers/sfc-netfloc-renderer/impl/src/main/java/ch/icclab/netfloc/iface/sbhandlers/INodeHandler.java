/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface.sbhandlers;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public interface INodeHandler {
	public void handleNodeConnect(Node node, OvsdbNodeAugmentation ovsdbNodeAugmentation);
	public void handleNodeDisconnect(Node node, OvsdbNodeAugmentation ovsdbNodeAugmentation);
	public void handleNodeConnectionAttributeChange(Node node, OvsdbNodeAugmentation ovsdbNodeAugmentation);
}
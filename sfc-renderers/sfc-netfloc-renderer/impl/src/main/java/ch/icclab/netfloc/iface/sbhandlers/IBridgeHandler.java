/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface.sbhandlers;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public interface IBridgeHandler {
	public void handleBridgeCreate(Node node, OvsdbBridgeAugmentation ovsdbBridgeAugmentation);
	public void handleBridgeDelete(Node node, OvsdbBridgeAugmentation ovsdbBridgeAugmentation);
	public void handleBridgeUpdate(Node node, OvsdbBridgeAugmentation ovsdbBridgeAugmentation);
}
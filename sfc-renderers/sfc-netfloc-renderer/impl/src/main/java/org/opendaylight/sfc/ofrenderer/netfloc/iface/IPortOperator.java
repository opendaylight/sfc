/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

import org.opendaylight.neutron.spi.NeutronPort;

public interface IPortOperator {
	public IBridgeOperator getBridge();
	public Uuid getPortUuid();
	public boolean isOvsPort(TpId tpid);
	public void update(TerminationPoint tp, OvsdbTerminationPointAugmentation changes);
	public java.lang.Long getOfport();
	public void relateSouthbound(IBridgeOperator bridge, TerminationPoint tp, OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation);
	public String getOFTpIdValue();
}

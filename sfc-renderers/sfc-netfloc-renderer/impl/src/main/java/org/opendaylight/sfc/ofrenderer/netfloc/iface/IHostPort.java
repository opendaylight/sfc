/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface;

import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;

import java.util.List;

public interface IHostPort extends IPortOperator {
	public String getMacAddress();
	public List<String> getIPAddresses();
	public void update(NeutronPort changes);
	public String getNeutronUuid();
	public boolean canConnectTo(IHostPort dst);
	public NeutronPort getNeutronPort();
	public NeutronFloatingIP getNeutronFloatingIP();
	public void setNeutronFloatingIP(NeutronFloatingIP neutronFloatingIP);
}

/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import ch.icclab.netfloc.iface.IBridgeOperator;
import ch.icclab.netfloc.iface.IHostPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.LinkedList;

public class HostPort extends Port implements IHostPort {

	static final Logger logger = LoggerFactory.getLogger(HostPort.class);
	private NeutronPort neutronPort;
	private NeutronFloatingIP neutronFloatingIP;

	public HostPort(IBridgeOperator bridge, TerminationPoint tp, OvsdbTerminationPointAugmentation tpa, NeutronPort neutronPort) {
		super(bridge, tp, tpa);
		this.neutronPort = neutronPort;
	}

	public String getMacAddress() {
		return this.neutronPort.getMacAddress();
	}

	public List<String> getIPAddresses() {
		List<String> ipAddr = new LinkedList<String>();
		for (Neutron_IPs nip : this.neutronPort.getFixedIPs()) {
			ipAddr.add(nip.getIpAddress());
		}
		return ipAddr;
	}

	public void update(NeutronPort changes) {
		this.neutronPort = changes;
	}

	public String getNeutronUuid() {
		return this.neutronPort.getPortUUID();
	}

	public NeutronPort getNeutronPort() {
		return this.neutronPort;
	}

	public NeutronFloatingIP getNeutronFloatingIP() {
		return this.neutronFloatingIP;
	}

	public void setNeutronFloatingIP(NeutronFloatingIP neutronFloatingIP) {
		this.neutronFloatingIP = neutronFloatingIP;
	}

	public boolean canConnectTo(IHostPort dst) {
		if (dst == null || this.equals(dst)) {
			logger.info("HostPort Cannot connect port {}, destination port {} is null/equal", this, dst);
			return false;
		}

		if (this.getOfport() == null || dst.getOfport() == null || this.getOfport().equals(dst.getOfport())) {
			logger.info("HostPort Cannot connect OF port {}, destination port {} is null/equal", this.getOfport(), dst.getOfport());
			return false;
		}

		List<String> dstSubnets = new LinkedList<String>();
		for (Neutron_IPs ipDst : dst.getNeutronPort().getFixedIPs()) {
			dstSubnets.add(ipDst.getSubnetUUID());
		}

		for (Neutron_IPs ip : this.getNeutronPort().getFixedIPs()) {
			if (dstSubnets.contains(ip.getSubnetUUID())) {
				logger.info("HostPort Can connect OF port {} and destination port {} - in same subnet {}", this.getOfport(), dst.getOfport(), ip.getSubnetUUID());
				return true;
			}
		}
		logger.info("HostPort Cannot connect OF port {} and destination port {} - not in same subnet.", this.getOfport(), dst.getOfport());
		return false;
	}
}

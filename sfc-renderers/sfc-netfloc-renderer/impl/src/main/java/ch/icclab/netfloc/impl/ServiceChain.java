/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import java.util.List;
import ch.icclab.netfloc.iface.IBridgeOperator;
import ch.icclab.netfloc.iface.ILinkPort;
import ch.icclab.netfloc.iface.INetworkPath;
import ch.icclab.netfloc.iface.IServiceChain;
import ch.icclab.netfloc.iface.IHostPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;

public class ServiceChain implements IServiceChain {

	private List<INetworkPath> paths;
	private List<IHostPort> ports;
	private int chainID;
	private List<String> neutronPortIDs;
	private static final Logger logger = LoggerFactory.getLogger(ServiceChain.class);

	public ServiceChain(List<INetworkPath> paths, int chainID) {
		if (paths == null || paths.isEmpty()) {
			throw new IllegalStateException("ServiceChain paths cannot be null or empty.");
		}
		this.chainID = chainID;
		this.paths = paths;
		logger.info("ServiceChain chainPath: {}", paths);
	}

	public int getChainId() {
		return this.chainID;
	}

	public int getNumberHops() {
		return this.paths.size() - 1;
	}

	public INetworkPath getBegin() {
		return paths.get(0);
	}

	public INetworkPath getEnd() {
		return paths.get(paths.size()-1);
	}

	public INetworkPath getPrevious(INetworkPath np) {
		int index = paths.lastIndexOf(np);
		if (index > 0) {
			return paths.get(index - 1);
		}
		return null;
	}

	public INetworkPath getNext(INetworkPath np) {
		int index = paths.lastIndexOf(np);
		if (index < paths.size() - 1) {
			return paths.get(index + 1);
		}
		return null;
	}

	public void append(INetworkPath np) {
		this.paths.add(np);
	}

	public void setNeutronPortsList(List<String> neutronPortIDs) {
		this.neutronPortIDs = neutronPortIDs;
	}

	public List<String> getNeutronPortsList(){
		return neutronPortIDs;
	}

	public void addPaths(List<INetworkPath> nps) {
		this.paths.addAll(nps);
	}

	public boolean isEqualConnectionChain(IServiceChain sc) {
		return sc.getChainId() == this.getChainId();
	}

	public IHostPort getNext(IHostPort np) {
		int index = ports.lastIndexOf(np);
		if (index < ports.size() - 1) {
			return ports.get(index + 1);
		}
		return null;
	}

	public IHostPort getLast() {
		return ports.get(ports.size()-1);
	}

}

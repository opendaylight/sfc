/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import java.util.List;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.ILinkPort;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INetworkPath;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IHostPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;
import java.util.Collections;

public class NetworkPath implements INetworkPath {

	static final Logger logger = LoggerFactory.getLogger(NetworkPath.class);
	private List<IBridgeOperator> bridges = new LinkedList<IBridgeOperator>();
	private IHostPort beginPort;
	private IHostPort endPort;
	private boolean closed = false;

	public NetworkPath(IHostPort beginPort, IHostPort endPort) {
		this.beginPort = beginPort;
		this.endPort = endPort;
	}

	public void close() {
		this.closed = true;
	}

	public boolean isClosed() {
		return this.closed;
	}

	public int getLength() {
		return bridges.size();
	}

	public INetworkPath getReversePath() {
		IHostPort newEndPort = this.getBeginPort();
		IHostPort newBeginPort = this.getEndPort();
		INetworkPath reversePath = new NetworkPath(newBeginPort, newEndPort);
		List<IBridgeOperator> originBridges = this.getBridges();
		List<IBridgeOperator> bridges = (LinkedList)((LinkedList)originBridges).clone();
		Collections.reverse(bridges);
		reversePath.addBridges(bridges);
		return reversePath;
	}

	public IBridgeOperator getBegin() {
		return bridges.get(0);
	}

	public IBridgeOperator getEnd() {
		return bridges.get(bridges.size()-1);
	}

	public IHostPort getBeginPort() {
		return this.beginPort;
	}

	public IHostPort getEndPort() {
		return this.endPort;
	}

	public void append(IBridgeOperator bridge) {
		bridges.add(bridge);
	}

	public IBridgeOperator getPrevious(IBridgeOperator bridge) {
		int index = bridges.lastIndexOf(bridge);
		if (index > 0) {
			return bridges.get(index - 1);
		}
		return null;
	}

	public IBridgeOperator getNext(IBridgeOperator bridge) {
		int index = bridges.lastIndexOf(bridge);
		if (index < bridges.size() - 1) {
			return bridges.get(index + 1);
		}
		return null;
	}

	public ILinkPort getPreviousLink(IBridgeOperator bridge) {
		logger.info("NetworkPath Searching previous link for bridge {} between bridge {} and bridge {}", bridge.getDatapathId(), this.getBegin().getDatapathId(), this.getEnd().getDatapathId());
		IBridgeOperator previousBridge = this.getPrevious(bridge);
		if (previousBridge == null) {
			return null;
		}
		List<ILinkPort> possiblyLinkedPorts = previousBridge.getLinkPorts();
		for (ILinkPort port : bridge.getLinkPorts()) {
			if (possiblyLinkedPorts.contains(port.getLinkedPort())) {
				return port;
			}
		}
		return null;
	}

	public ILinkPort getNextLink(IBridgeOperator bridge) {
		IBridgeOperator nextBridge = this.getNext(bridge);
		if (nextBridge == null) {
			return null;
		}
		List<ILinkPort> possiblyLinkedPorts = nextBridge.getLinkPorts();
		for (ILinkPort port : bridge.getLinkPorts()) {
			if (port.getLinkedPort() == null) {
				continue;
			}
			for (ILinkPort otherPort : possiblyLinkedPorts) {
				if (otherPort.getOFTpIdValue().equals(port.getLinkedPort().getOFTpIdValue())) {
					return port;
				}
			}
		}
		logger.info("NetworkPath getNextLink: Compared link ports {} and {}", possiblyLinkedPorts, bridge.getLinkPorts());
		return null;
	}

	public List<IBridgeOperator> getBridges() {
		return bridges;
	}

	public void addBridges(List<IBridgeOperator> bridges) {
		this.bridges.addAll(bridges);
	}

	public INetworkPath getCleanPath() {
		if (!this.closed) {
			return null;
		}
		INetworkPath cleanPath = new NetworkPath(this.beginPort, this.endPort);
		List<IBridgeOperator> cleanBridges = new LinkedList<IBridgeOperator>();
		IBridgeOperator bridge = this.getBegin();
		boolean started = false;
		do {
			if (started) {
				bridge = this.getNext(bridge);
			}
			started = true;
			IBridgeOperator cbr = new Bridge(bridge.getParentNode(), bridge.getNode(), bridge.getAugmentation());

			// Add vm ports
			if (bridge.equals(this.getBegin())) {
				cbr.addHostPort(this.beginPort);
			}

			if (bridge.equals(this.getEnd())) {
				cbr.addHostPort(this.endPort);
			}

			// Add link port
			if (!bridge.equals(this.getEnd())) {
				cbr.addLinkPort(this.getNextLink(bridge));
			}
			cleanBridges.add(cbr);

		} while (!bridge.equals(this.getEnd()));
		cleanPath.addBridges(cleanBridges);
		cleanPath.close();
		return cleanPath;
	}

	public boolean isEqualConnection(INetworkPath np) {
		return (this.getBeginPort().equals(np.getBeginPort()) &&
			this.getEndPort().equals(np.getEndPort())) ||
			(this.getBeginPort().equals(np.getEndPort()) &&
			this.getEndPort().equals(np.getBeginPort()));
	}

	public boolean hasLinkPort(ILinkPort link) {
		for (IBridgeOperator bridge : this.getBridges()) {
			ILinkPort checkLinkNext = this.getNextLink(bridge);
			if (checkLinkNext != null && checkLinkNext.equals(link)) {
				return true;
			}

			ILinkPort checkLinkPrev = this.getPreviousLink(bridge);
			if (checkLinkPrev != null && checkLinkPrev.equals(link)) {
				return true;
			}
		}
		return false;
	}

	public boolean equals(Object o) {
		if (!(o instanceof NetworkPath)) {
			return false;
		}
		NetworkPath np = (NetworkPath)o;
		return this.isEqualConnection(np) && this.getBridges().equals(np.getBridges());
	}

	public int hashCode() {
		return 13 * (this.getBeginPort().hashCode() +
			this.getEndPort().hashCode() +
			this.getBridges().hashCode());
	}

	public String toString() {
		String str = "";
		for (IBridgeOperator bridge : this.getBridges()) {
			String inPort = ""; String outPort = "";
			if (bridge.equals(this.getBegin())) {
				inPort = this.getBeginPort().getOfport().toString();
			} else {
				inPort = this.getPreviousLink(bridge).getOfport().toString();
			}
			if (bridge.equals(this.getEnd())) {
				outPort = this.getEndPort().getOfport().toString();
			} else {
				outPort = this.getNextLink(bridge).getOfport().toString();
			}
			str += "=[" + inPort + "]->(" + bridge.getDatapathId() + ")->[" + outPort + "]=";
			logger.info("NetworkPath (inPort, bridge, outPort) {} ", str);
		}
		return str;
	}
}

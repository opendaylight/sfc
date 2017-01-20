/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import ch.icclab.netfloc.iface.IFlowBroadcastPattern;
import ch.icclab.netfloc.iface.IBridgeOperator;
import ch.icclab.netfloc.iface.INetworkPath;
import ch.icclab.netfloc.iface.IPortOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

public class FlowBroadcastPattern implements IFlowBroadcastPattern {

	static final Logger logger = LoggerFactory.getLogger(FlowBroadcastPattern.class);
	private static final int BROADCAST_PRIORITY = 10;

	public Map<IBridgeOperator, List<Flow>> apply(Set<INetworkPath> paths) {
		Map<IBridgeOperator, List<Flow>> flows = new HashMap<IBridgeOperator, List<Flow>>();
		String srcMac = this.getSrcMac(paths);
		for (IBridgeOperator bridge : this.getBridgeSet(paths)) {
			List<Flow> bridgeFlows = new LinkedList<Flow>();
			bridgeFlows.add(OpenFlowUtil.createBroadcastFlow(bridge, this.getInPort(bridge, paths), this.getDstPorts(bridge, paths), srcMac, BROADCAST_PRIORITY));
			flows.put(bridge, bridgeFlows);
		}
		return flows;
	}

	private String getSrcMac(Set<INetworkPath> paths) {
		String srcMac = null;
		for (INetworkPath path : paths) {
			String pathMac = path.getBeginPort().getMacAddress();
			if (srcMac == null) {
				srcMac = pathMac;
			}
			if (!srcMac.equals(pathMac)) {
				throw new IllegalStateException("Source MAC address have to be equal for broadcast paths.");
			}
		}
		return srcMac;
	}

	private Set<IBridgeOperator> getBridgeSet(Set<INetworkPath> paths) {
		Set<IBridgeOperator> bridgeSet = new HashSet<IBridgeOperator>();
		for (INetworkPath path : paths) {
			bridgeSet.addAll(path.getBridges());
		}
		return bridgeSet;
	}

	private IPortOperator getInPort(IBridgeOperator bridge, Set<INetworkPath> paths) {
		IPortOperator inPort = null;
		for (INetworkPath path : paths) {
			if (!path.getBridges().contains(bridge)) {
				continue;
			}
			IPortOperator pathInPort = null;
			if (bridge.equals(path.getBegin())) {
				pathInPort = path.getBeginPort();
			} else {
				pathInPort = path.getPreviousLink(bridge);
			}
			if (inPort == null) {
				inPort = pathInPort;
			} else {
				if (!inPort.equals(pathInPort)) {
					throw new IllegalStateException("Ingress ports of broadcast paths have to be equal per bridge.");
				}
			}
		}
		return inPort;
	}

	private Set<IPortOperator> getDstPorts(IBridgeOperator bridge, Set<INetworkPath> paths) {
		Set<IPortOperator> dstPorts = new HashSet<IPortOperator>();
		logger.info("finding destination ports for {}", bridge.getDatapathId());
		for (INetworkPath path : paths) {
			if (!path.getBridges().contains(bridge)) {
				continue;
			}
			IBridgeOperator begin = path.getBegin();
			IBridgeOperator end = path.getEnd();
			logger.info("finding destination port for {}", path);
			if (bridge.equals(end)) {
				dstPorts.add(path.getEndPort());
			} else {
				dstPorts.add(path.getNextLink(bridge));
			}
		}
		return dstPorts;
	}
}

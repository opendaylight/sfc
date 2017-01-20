/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;

import org.opendaylight.sfc.ofrenderer.netfloc.iface.IFlowPathPattern;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INetworkPath;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IPortOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

public class FlowPathPattern implements IFlowPathPattern {

	static final Logger logger = LoggerFactory.getLogger(FlowPathPattern.class);
	private static final int FORWARDING_PRIORITY = 10;
	private static final int BROADCAST_PRIORITY = 10;

	public Map<IBridgeOperator, List<Flow>> apply(INetworkPath path) {

		Map<IBridgeOperator, List<Flow>> flows = new HashMap<IBridgeOperator, List<Flow>>();

		// Begin & End bridges
		IBridgeOperator begin = path.getBegin();
		IBridgeOperator end = path.getEnd();
		if (begin.equals(end)) {
			flows.put(begin, this.createBidirectionalFlows(begin, path.getBeginPort(), path.getEndPort(), path.getBeginPort().getMacAddress(), path.getEndPort().getMacAddress()));
			return flows;
		}
		logger.info("FlowPathPattern Adding begin-bridge flow");
		flows.put(begin, this.createBidirectionalFlows(begin, path.getBeginPort(), path.getNextLink(begin), path.getBeginPort().getMacAddress(), path.getEndPort().getMacAddress()));
		logger.info("FlowPathPattern Adding end-bridge flow");
		flows.put(end, this.createBidirectionalFlows(end, path.getPreviousLink(end), path.getEndPort(), path.getBeginPort().getMacAddress(), path.getEndPort().getMacAddress()));

		// Aggregation bridges
		IBridgeOperator bridge = path.getNext(begin);
		while (bridge != null && !bridge.equals(end)) {
			logger.info("FlowPathPattern Adding aggregation bridge flow");
			flows.put(bridge, this.createBidirectionalFlows(bridge, path.getPreviousLink(bridge), path.getNextLink(bridge), path.getBeginPort().getMacAddress(), path.getEndPort().getMacAddress()));
			bridge = path.getNext(bridge);
		}
		return flows;
	}

	private List<Flow> createBidirectionalFlows(IBridgeOperator bridge, IPortOperator srcPort, IPortOperator dstPort, String srcMac, String dstMac) {
		List<Flow> flows = new LinkedList<Flow>();
		flows.add(OpenFlowUtil.createForwardFlow(bridge, srcPort, dstPort, srcMac, dstMac, FORWARDING_PRIORITY));
		flows.add(OpenFlowUtil.createForwardFlow(bridge, dstPort, srcPort, dstMac, srcMac, FORWARDING_PRIORITY));
		return flows;
	}
}

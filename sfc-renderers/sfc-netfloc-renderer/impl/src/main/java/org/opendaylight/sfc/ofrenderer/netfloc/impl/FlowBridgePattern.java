/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IFlowBridgePattern;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import java.util.List;
import java.util.LinkedList;

public class FlowBridgePattern implements IFlowBridgePattern {
	public List<Flow> apply(IBridgeOperator bo) {
		List<Flow> flows = new LinkedList<Flow>();
		flows.add(OpenFlowUtil.createNormalFlow(bo, 0));
		flows.add(OpenFlowUtil.createLLDPFlow(bo, 10));
		return flows;
	}
}

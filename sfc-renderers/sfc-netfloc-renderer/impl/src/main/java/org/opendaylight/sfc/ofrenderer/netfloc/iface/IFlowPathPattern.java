/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INetworkPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import java.util.Map;
import java.util.List;

public interface IFlowPathPattern {
	public Map<IBridgeOperator, List<Flow>> apply(INetworkPath path);
}

/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface;

import ch.icclab.netfloc.iface.INetworkPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import java.util.Map;
import java.util.List;
import java.util.Set;

public interface IFlowBroadcastPattern {
	public Map<IBridgeOperator, List<Flow>> apply(Set<INetworkPath> paths);	
}

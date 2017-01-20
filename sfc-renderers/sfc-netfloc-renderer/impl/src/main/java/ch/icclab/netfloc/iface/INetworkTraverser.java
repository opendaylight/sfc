/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface;

import java.util.List;

public interface INetworkTraverser {
	public void traverse(IBridgeIterator bridgeIterator);
	public List<INetworkPath> getNetworkPaths();
	public INetworkPath getNetworkPath(IHostPort begin, IHostPort end);
}

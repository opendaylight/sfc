/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface;

import java.util.List;

public interface INetworkPath {
	public int getLength();
	public IBridgeOperator getBegin();
	public IBridgeOperator getEnd();
	public IBridgeOperator getPrevious(IBridgeOperator bridge);
	public IBridgeOperator getNext(IBridgeOperator bridge);
	public ILinkPort getPreviousLink(IBridgeOperator bridge);
	public ILinkPort getNextLink(IBridgeOperator bridge);
	public List<IBridgeOperator> getBridges();
	public IHostPort getBeginPort();
	public IHostPort getEndPort();
	public boolean isEqualConnection(INetworkPath np);
	public INetworkPath getReversePath();
	public void append(IBridgeOperator bridge);
	public void addBridges(List<IBridgeOperator> bridges);
	public INetworkPath getCleanPath();
	public void close();
	public boolean isClosed();
	public boolean hasLinkPort(ILinkPort linkPort);
}

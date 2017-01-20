/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface;

import java.util.List;
import java.util.Map;

public interface IServiceChain {
	public int getChainId();
	public int getNumberHops();
	public INetworkPath getBegin();
	public INetworkPath getEnd();
	public INetworkPath getPrevious(INetworkPath np);
	public INetworkPath getNext(INetworkPath np);
	public void append(INetworkPath np);
	public void setNeutronPortsList(List<String> neutronPortIDs);
	public List<String> getNeutronPortsList();
	public void addPaths(List<INetworkPath> nps);
	public boolean isEqualConnectionChain(IServiceChain sc);
	public IHostPort getNext(IHostPort np);
	public IHostPort getLast();
}

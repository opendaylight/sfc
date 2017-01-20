/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface;

import java.util.Set;

public interface IBroadcastListener {
	public void broadcastCreated(Set<INetworkPath> np);
	public void broadcastDeleted(Set<INetworkPath> np);
}

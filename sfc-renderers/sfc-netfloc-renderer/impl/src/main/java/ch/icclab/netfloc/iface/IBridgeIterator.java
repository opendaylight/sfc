/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface;

// Interface to be called during tenant network traversal when visiting a new bridge
public interface IBridgeIterator<T> {
	public boolean visitBridge(ITraversableBridge bridge);
	public T getResult();
}

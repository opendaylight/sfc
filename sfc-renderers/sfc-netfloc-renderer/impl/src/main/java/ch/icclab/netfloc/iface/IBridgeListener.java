/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface;

public interface IBridgeListener {
	public void bridgeCreated(IBridgeOperator bo);
	public void bridgeUpdated(IBridgeOperator oldBo, IBridgeOperator nBo);
	public void bridgeDeleted(IBridgeOperator bo);
}
/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.provider;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;

public class SfcPotTimerQueue {
    private ConcurrentLinkedQueue<RspName> sfcPotTimerQueueObj;
    private static final SfcPotTimerQueue sfcPotTimerQueueInstance = new SfcPotTimerQueue();

    private SfcPotTimerQueue() {
        sfcPotTimerQueueObj = new ConcurrentLinkedQueue();
    }

    public static SfcPotTimerQueue getInstance() {
        return sfcPotTimerQueueInstance;
    }

    public boolean addElement(RspName rspName) {
        return (sfcPotTimerQueueObj.add(rspName));
    }

    public RspName removeElement() {
        return sfcPotTimerQueueObj.poll();
    }

    public void clearTimerQueue() {
        sfcPotTimerQueueObj.clear();
        return;
    }

    public boolean hasElements() {
        return (!sfcPotTimerQueueObj.isEmpty());
    }
}

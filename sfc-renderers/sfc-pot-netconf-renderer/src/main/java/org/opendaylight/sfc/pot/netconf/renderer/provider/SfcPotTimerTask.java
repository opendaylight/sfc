/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.provider;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;

public class SfcPotTimerTask implements TimerTask {
    public static final SfcPotTimerQueue SFC_POT_TMR_WORK_Q = SfcPotTimerQueue.getInstance();
    private RspName rspName;

    public SfcPotTimerTask(RspName rspName) {
        this.rspName = rspName;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        SFC_POT_TMR_WORK_Q.addElement(this.rspName);
        SfcPotTimerThread sfcPotTimerThread = SfcPotTimerThread.getInstance();
        sfcPotTimerThread.triggerConfigRefresher();
    }
}

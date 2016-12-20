/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.provider;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SfcPotTimerWheel {
    private final Timer sfcPotTimerWheelObj;
    public static final SfcPotTimerWheel sfcPotTimerWheelInstance = new SfcPotTimerWheel();

    private SfcPotTimerWheel() {
        sfcPotTimerWheelObj = new HashedWheelTimer();
    }

    public static SfcPotTimerWheel getInstance() {
        return sfcPotTimerWheelInstance;
    }

    public Timeout setTimerContext(TimerTask task, long delay, TimeUnit unit) {
        Timeout timeout = null;
        synchronized (sfcPotTimerWheelObj) {
            timeout  = sfcPotTimerWheelObj.newTimeout(task, delay, unit);
        }
        return timeout;
    }

    public void clearTimerContext(Timeout timeout) {
        if (timeout != null) {
            synchronized (sfcPotTimerWheelObj) {
                if (!timeout.isExpired()) {
                    timeout.cancel();
                }
            }
        }
    }
}

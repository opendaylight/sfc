/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider;

import io.netty.util.Timeout;

import java.util.concurrent.TimeUnit;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcPotTimerThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotTimerThread.class);
    public static final SfcPotTimerThread sfcPotTimerThread = new SfcPotTimerThread();
    private static final int SLEEP_TIME = 1000; //timeunit in ms
    private static Thread configRefresher;
    private static SfcPotTimerQueue sfcPotTimerQueue;
    private boolean doLoop = true;

    private SfcPotTimerThread() {
        sfcPotTimerQueue = SfcPotTimerQueue.getInstance();
        configRefresher = new Thread(this);
        configRefresher.start();
        LOG.debug("Started SFC PoT config refresher thread");
    }

    public static SfcPotTimerThread getInstance() {
        return sfcPotTimerThread;
    }

    public void triggerConfigRefresher() {
        LOG.debug("in triggerConfigRefresher");
        configRefresher.interrupt();
    }

    public void stopConfigRefresher() {
        doLoop = false;
        triggerConfigRefresher();
    }

    @Override
    public void run() {
        while (doLoop) {
            boolean processRsp = sfcPotTimerQueue.hasElements();

            if (processRsp) {
                RspName rspName = sfcPotTimerQueue.removeElement();
                SfcPotTimerData rspData = SfcPotTimerData.getInstance();
                Long refreshTimerValue;
                int  currActiveIndex, newActiveIndex;
                /*
                 * Get RSP info,
                 * Do config regen for the non-active config
                 * Mark it as active
                 * update RSP with both config
                 * restart timer.
                 */
                if (rspData != null) {
                    refreshTimerValue = rspData.getRspDataConfigRefreshValue(rspName);
                    currActiveIndex = rspData.getRspDataConfigActiveIndex(rspName);

                    newActiveIndex = SfcPotRspProcessor.refreshSfcPot(rspName,
                                                                      currActiveIndex,
                                                                      refreshTimerValue);
                    /* Now, restart timers and store state */
                    SfcPotTimerWheel potTimerWheel = SfcPotTimerWheel.getInstance();
                    Timeout potTimeout = potTimerWheel.setTimerContext(rspData.getRspDataTimerTask(rspName),
                                                                       refreshTimerValue,
                                                                       TimeUnit.MILLISECONDS);
                    rspData.setRspDataTimeout(rspName, potTimeout);
                    rspData.setRspDataConfigActiveIndex(rspName, newActiveIndex);
                }
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                LOG.debug("SfcPotTimerThread interrupted. Continuing...");
            }
        }
    }
}

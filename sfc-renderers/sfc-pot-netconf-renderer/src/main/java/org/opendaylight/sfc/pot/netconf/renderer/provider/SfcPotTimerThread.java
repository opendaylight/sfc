/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.provider;

import io.netty.util.Timeout;

import java.util.concurrent.TimeUnit;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcPotTimerThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotTimerThread.class);
    private static final int SLEEP_TIME_MS = 1000;
    private Thread configRefresher;
    private SfcPotTimerQueue sfcPotTimerQueue;
    private SfcPotNetconfIoam sfcPotNetconfIoam;
    private boolean doLoop = true;
    public static final SfcPotTimerThread sfcPotTimerThread = new SfcPotTimerThread();

    private SfcPotTimerThread() {
        sfcPotTimerQueue = SfcPotTimerQueue.getInstance();
        configRefresher = new Thread(this);
        configRefresher.start();
    }

    public static SfcPotTimerThread getInstance() {
        return sfcPotTimerThread;
    }

    public void setSfcPotRspProcessor(SfcPotNetconfIoam sfcPotNetconfIoam) {
        this.sfcPotNetconfIoam = sfcPotNetconfIoam;
    }

    public void triggerConfigRefresher() {
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
                SfcPotTimerData potTimerData = SfcPotTimerData.getInstance();
                Long refreshTimerValue;
                int  currActiveIndex, newActiveIndex = 0, sfcSize;
                /*
                 * Get stored RSP info,
                 * Do configuration regeneration for the non-active config
                 * restart timer and store new active index.
                 */
                if (potTimerData != null) {
                    refreshTimerValue = potTimerData.getRspDataConfigRefreshValue(rspName);
                    currActiveIndex = potTimerData.getRspDataConfigActiveIndex(rspName);
                    sfcSize = potTimerData.getRspDataSfcSize(rspName);
                    if (sfcPotNetconfIoam != null) {
                        newActiveIndex = sfcPotNetconfIoam.refreshSfcPot(rspName, currActiveIndex,
                                                                         sfcSize,
                                                                         refreshTimerValue);
                    }

                    if (newActiveIndex >= 0) {
                        /* Now, restart timers and store state */
                        SfcPotTimerWheel potTimerWheel = SfcPotTimerWheel.getInstance();
                        Timeout potTimeout = potTimerWheel.setTimerContext(
                            potTimerData.getRspDataTimerTask(rspName), refreshTimerValue,
                            TimeUnit.MILLISECONDS);
                        potTimerData.setRspDataTimeout(rspName, potTimeout);
                        potTimerData.setRspDataConfigActiveIndex(rspName, newActiveIndex);
                    }
                }
            }

            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException e) {
                LOG.warn("Thread interrupted while sleeping... {} ", e);
            }
        }
    }
}

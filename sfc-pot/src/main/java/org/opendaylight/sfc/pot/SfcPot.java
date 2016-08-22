/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;

import org.opendaylight.sfc.pot.listener.SfcPotRspListener;

import org.opendaylight.sfc.pot.provider.SfcPotRspProcessor;
import org.opendaylight.sfc.pot.provider.SfcPotTimerThread;
import org.opendaylight.sfc.pot.provider.SfcPotTimerQueue;

/*
 * Initialize all necessary SFC Proof of Transit components related to
 * north-bound and config generation aspects.
 * <p>
 *
 * @author  Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since   2016-09-01
 * @see     https://github.com/CiscoDevNet/iOAM
 */
public class SfcPot {

    final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();

    private final SfcPotRspProcessor sfcPotRspProcessor;
    private final SfcPotRspListener  sfcPotRspListener;

    public SfcPot(DataBroker dataBroker,
                  BindingAwareBroker bindingAwareBroker) {
        /*
         * Handler for RPCs for SFC PoT enable on an RSP.
         */
        sfcPotRspProcessor = new SfcPotRspProcessor(dataBroker);

        /* Add a listener to mainly handle RSP deletes for internal cleanups */
        sfcPotRspListener = new SfcPotRspListener(opendaylightSfc,
                                                  sfcPotRspProcessor);

        /* Kick off a thread for periodic config refresh handling */
        SfcPotTimerThread sfcPotTimerThread = SfcPotTimerThread.getInstance();
        sfcPotTimerThread.triggerConfigRefresher();
    }

    public void unregisterListeners() {
        sfcPotRspListener.getDataChangeListenerRegistration().close();
    }

    public void close() {
        sfcPotRspListener.closeDataChangeListener();

        SfcPotTimerQueue queue = SfcPotTimerQueue.getInstance();
        queue.clearTimerQueue();
        SfcPotTimerThread sfcPotTimerThread = SfcPotTimerThread.getInstance();
        sfcPotTimerThread.stopConfigRefresher();
    }
}

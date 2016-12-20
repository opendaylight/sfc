/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

import org.opendaylight.sfc.pot.netconf.renderer.listener.SfcPotNetconfNodeListener;
import org.opendaylight.sfc.pot.netconf.renderer.listener.SfcPotNetconfRSPListener;

import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfNodeManager;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfIoam;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotTimerThread;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotTimerQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialize all necessary SFC iOAM Proof of Transit south-bound
 * (https://github.com/CiscoDevNet/iOAM) Netconf Renderer components.
 *
 * @author  Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since   2016-12-01
 */
public class SfcPotNetconfRenderer {

    private SfcPotNetconfRSPListener sfcPotNetconfRSPListener;
    private SfcPotNetconfIoam sfcPotNetconfIoam;
    private SfcPotNetconfNodeManager nodeManager;
    private SfcPotNetconfNodeListener nodeListener;

    private final DataBroker dataBroker;
    private final BindingAwareBroker bindingAwareBroker;

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfRenderer.class);

    public SfcPotNetconfRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        this.dataBroker = dataBroker;
        this.bindingAwareBroker = bindingAwareBroker;
    }

    public void initialize() {
        /* Netconf node manager and listener */
        nodeManager = new SfcPotNetconfNodeManager(dataBroker, bindingAwareBroker);
        nodeListener = new SfcPotNetconfNodeListener(dataBroker, nodeManager);

        /* SB configuration generator and netconf handler */
        sfcPotNetconfIoam = new SfcPotNetconfIoam(nodeManager);

        /* Add a listener to handle RSP updates */
        sfcPotNetconfRSPListener = new SfcPotNetconfRSPListener(dataBroker, sfcPotNetconfIoam);

        /* kick off a thread for periodic SB configuration refresh handling */
        SfcPotTimerThread sfcPotTimerThread = SfcPotTimerThread.getInstance();
        sfcPotTimerThread.setSfcPotRspProcessor(sfcPotNetconfIoam);
        sfcPotTimerThread.triggerConfigRefresher();

        LOG.info("iOAM:PoT:SB:Netconf renderer started.");
    }

    public void unregisterListeners() {
        SfcPotTimerQueue queue = SfcPotTimerQueue.getInstance();
        queue.clearTimerQueue();

        SfcPotTimerThread sfcPotTimerThread = SfcPotTimerThread.getInstance();
        sfcPotTimerThread.stopConfigRefresher();

        sfcPotNetconfRSPListener.closeDataChangeListener();
        nodeListener.closeListenerRegistration();

        LOG.info("iOAM:PoT:SB:Netconf renderer stopped.");
    }
}

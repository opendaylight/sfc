/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfIoam;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotTimerQueue;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotTimerThread;
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
@Singleton
public class SfcPotNetconfRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfRenderer.class);

    private final SfcPotNetconfIoam sfcPotNetconfIoam;

    @Inject
    public SfcPotNetconfRenderer(SfcPotNetconfIoam sfcPotNetconfIoam) {
        this.sfcPotNetconfIoam = sfcPotNetconfIoam;
    }

    @PostConstruct
    public void initialize() {
        /* kick off a thread for periodic SB configuration refresh handling */
        SfcPotTimerThread sfcPotTimerThread = SfcPotTimerThread.getInstance();
        sfcPotTimerThread.setSfcPotRspProcessor(sfcPotNetconfIoam);
        sfcPotTimerThread.triggerConfigRefresher();

        LOG.info("iOAM:PoT:SB:Netconf renderer started.");
    }

    @PreDestroy
    public void unregisterListeners() {
        SfcPotTimerQueue queue = SfcPotTimerQueue.getInstance();
        queue.clearTimerQueue();

        SfcPotTimerThread sfcPotTimerThread = SfcPotTimerThread.getInstance();
        sfcPotTimerThread.stopConfigRefresher();

        LOG.info("iOAM:PoT:SB:Netconf renderer stopped.");
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.sfc_sb_rest_provider.impl;

import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.sfc.sbrest.provider.keepalive.SbRestKeepAliveSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcSbRestProviderModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcSbRestProviderModule.class);
    private static ThreadPool threadPool;

    public SfcSbRestProviderModule(ThreadPool threadPool) {
        SfcSbRestProviderModule.threadPool = threadPool;
    }

    public void startSbRestKeepAliveSocket() {
        threadPool.getExecutor().execute(new SbRestKeepAliveSocket());
        LOG.info("Started SbRestKeepAliveSocket");
    }
}
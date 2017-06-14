/**
 * Copyright (c) 2017 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsHandlerTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsHandlerTask.class);
    private SfcStatisticsHandlerBase statsHandler;

    public SfcStatisticsHandlerTask(SfcStatisticsHandlerBase statsHandler) {
        this.statsHandler = statsHandler;
    }

    @Override
    public void run() {
        LOG.debug("SfcStatisticsTask firing");

        this.statsHandler.writeStatistics();

        LOG.debug("SfcStatisticsTask completed");
    }
}

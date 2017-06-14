/**
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.sfc.statistics.handlers.RspStatisticsHandler;
import org.opendaylight.sfc.statistics.handlers.SfcStatisticsHandlerTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;

public class SfcStatisticsManager implements AutoCloseable {

    private static final long DEFAULT_STATISTICS_PERIOD_SECONDS = 60 * 15; // 15 minutes
    private static final int DEFAULT_MAX_NUM_PERIODS = 48; // 12 hours worth using the default period
    private final ScheduledExecutorService scheduler;
    private final Map<Long, SfcStatisticsHandlerTask> rspToStatsTask;
    private long statisticsPeriodSeconds;
    private int maxNumPeriodsToStore;

    public SfcStatisticsManager() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.statisticsPeriodSeconds = DEFAULT_STATISTICS_PERIOD_SECONDS;
        this.maxNumPeriodsToStore = DEFAULT_MAX_NUM_PERIODS;
        rspToStatsTask = new HashMap<>();
    }

    public SfcStatisticsManager(long statisticsPeriod, int maxNumPeriods) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.statisticsPeriodSeconds = statisticsPeriod;
        this.maxNumPeriodsToStore = maxNumPeriods;
        rspToStatsTask = new HashMap<>();
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    public long getStatisticsPeriod() {
        return this.statisticsPeriodSeconds;
    }

    public void setStatisticsPeriod(long statsPeriodSeconds) {
        this.statisticsPeriodSeconds = statsPeriodSeconds;
    }

    public int getMaxNumPeriodsToStore() {
        return this.maxNumPeriodsToStore;
    }

    public void setMaxNumPeriodsToStore(int maxNumPeriods) {
        this.maxNumPeriodsToStore = maxNumPeriods;
    }

    public void scheduleRspStatistics(RenderedServicePath rsp) {
        if (rspToStatsTask.get(rsp.getPathId()) != null) {
            // Dont launch duplicate tasks
            return;
        }

        RspStatisticsHandler rspStatsInfo = new RspStatisticsHandler(rsp, this.maxNumPeriodsToStore);
        SfcStatisticsHandlerTask statsTask = new SfcStatisticsHandlerTask(rspStatsInfo);
        rspToStatsTask.put(rsp.getPathId(), statsTask);

        /* TODO
         * Something to consider: do we want to schedule each RSP or should we just process all the RSPs with a
         * single schedule task? Pros/Cons If just using one schedule task, then there could be a big bang
         * performance hit each time it fires, but it might be simpler.
         */
        scheduler.scheduleAtFixedRate(statsTask, statisticsPeriodSeconds, statisticsPeriodSeconds, TimeUnit.SECONDS);
    }

    // public void scheduleSfStatistics(ServiceFunction sf)

    // public void scheduleSffStatistics(ServiceFunctionForwarder sff)
}

/**
 * Copyright (c) 2017 Inocybe Technologies Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsManagerImpl implements SfcStatisticsManagerInterface, AutoCloseable  {

    public static final long DEFAULT_STATISTICS_PERIOD_SECONDS = 60 * 15; // 15 minutes
    public static final int DEFAULT_MAX_NUM_PERIODS = 48; // 12 hours worth using the default period
    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsManagerImpl.class);
    private static final int CORE_POOL_SIZE = 1;

    private long statisticsPeriodSeconds;
    private int maxNumPeriodsToStore;
    private final ScheduledExecutorService scheduler;
    private final Map<Long, SfcStatisticsHandlerTask> rspToStatsTask;

    public SfcStatisticsManagerImpl() {
        this.statisticsPeriodSeconds = DEFAULT_STATISTICS_PERIOD_SECONDS;
        this.maxNumPeriodsToStore = DEFAULT_MAX_NUM_PERIODS;
        this.scheduler = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
        rspToStatsTask = new HashMap<>();
    }

    public SfcStatisticsManagerImpl(long statisticsPeriod, int maxNumPeriods) {
        this.statisticsPeriodSeconds = statisticsPeriod;
        this.maxNumPeriodsToStore = maxNumPeriods;
        this.scheduler = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
        rspToStatsTask = new HashMap<>();
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

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    public void scheduleRspStatistics(RenderedServicePath rsp, SfcOfTableOffsets sfcOfTableOffsets) {
        if (rspToStatsTask.get(rsp.getPathId()) != null) {
            // Dont launch duplicate tasks
            LOG.info("SfcStatisticsManagerImpl::scheduleRspStatistics not repeating task for rsp ID [{}]",
                    rsp.getPathId());
            return;
        }

        RspStatisticsHandler rspStatsInfo = new RspStatisticsHandler(maxNumPeriodsToStore, rsp, sfcOfTableOffsets);
        SfcStatisticsHandlerTask statsTask = new SfcStatisticsHandlerTask(rspStatsInfo);
        rspToStatsTask.put(rsp.getPathId(), statsTask);

        /* TODO
         * Something to consider: do we want to schedule each RSP or should we just process all the RSPs with a
         * single schedule task? Pros/Cons If just using one schedule task, then there could be a big bang
         * performance hit each time it fires, but it might be simpler.
         */
        scheduler.scheduleAtFixedRate(statsTask, statisticsPeriodSeconds, statisticsPeriodSeconds, TimeUnit.SECONDS);
    }

    public void scheduleSfStatistics(ServiceFunction sf) {
        // Not implemented for now
    }

    public void scheduleSffStatistics(ServiceFunctionForwarder sff) {
        // Not implemented for now
    }
}

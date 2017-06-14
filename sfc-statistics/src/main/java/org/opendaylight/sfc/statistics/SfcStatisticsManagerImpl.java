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

import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.utils.concurrent.ThreadFactoryProvider;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.statistics.handlers.RspStatisticsHandler;
import org.opendaylight.sfc.statistics.handlers.SfStatisticsHandler;
import org.opendaylight.sfc.statistics.handlers.SfcStatisticsHandlerTask;
import org.opendaylight.sfc.statistics.handlers.SffStatisticsHandler;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.statistics.configuration.rev171130.SfcStatisticsConfiguration;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.statistics.configuration.rev171130.SfcStatisticsConfigurationBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SfcStatisticsManagerImpl implements SfcStatisticsManagerInterface, AutoCloseable  {

    private static final long DEFAULT_STATISTICS_PERIOD_SECONDS = 120; // 2 minutes
    private static final int CORE_POOL_SIZE = 1;
    private static final String RSP_STATS_THREAD_FACTORY_NAME = "SFC RSP statistics scheduler";
    private static final String SFF_STATS_THREAD_FACTORY_NAME = "SFC SFF statistics scheduler";
    private static final String SF_STATS_THREAD_FACTORY_NAME = "SFC SF statistics scheduler";
    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsManagerImpl.class);

    private long rspStatisticsPeriodSeconds;
    private long sffStatisticsPeriodSeconds;
    private long sfStatisticsPeriodSeconds;

    // Using 3 different Scheduled Threads so they can be shutdown and
    // restarted individually if the respective (RSP, SFF, or SF)
    // statistics collection period changes.
    private ScheduledExecutorService rspScheduler;
    private ScheduledExecutorService sffScheduler;
    private ScheduledExecutorService sfScheduler;

    // Store the stats tasks to be able to restart them if the period changes
    private Map<String, SfcStatisticsHandlerTask> rspToStatsTask;
    private Map<String, SfcStatisticsHandlerTask> sffToStatsTask;
    private Map<String, SfcStatisticsHandlerTask> sfToStatsTask;

    public SfcStatisticsManagerImpl() {
        // Lazy initialization, these will be initialized when the stats are requested
        this.rspScheduler = null;
        this.sffScheduler = null;
        this.sfScheduler  = null;

        rspToStatsTask = new HashMap<>();
        sffToStatsTask = new HashMap<>();
        sfToStatsTask  = new HashMap<>();
    }

    @Override
    public void close() {
        rspToStatsTask.clear();
        sffToStatsTask.clear();
        sfToStatsTask.clear();

        if (rspScheduler != null) {
            rspScheduler.shutdownNow();
        }

        if (sffScheduler != null) {
            sffScheduler.shutdownNow();
        }

        if (sfScheduler != null) {
            sfScheduler.shutdownNow();
        }
    }

    public void scheduleRspStatistics(RenderedServicePath rsp, SfcOfTableOffsets sfcOfTableOffsets) {
        if (rspToStatsTask.get(rsp.getName().getValue()) != null) {
            // Dont launch duplicate tasks
            LOG.info("SfcStatisticsManagerImpl::scheduleRspStatistics not repeating task for rsp ID [{}]",
                    rsp.getPathId());
            return;
        }

        RspStatisticsHandler rspStatsHandler = new RspStatisticsHandler(rsp, sfcOfTableOffsets);
        SfcStatisticsHandlerTask statsTask = new SfcStatisticsHandlerTask(rspStatsHandler);
        rspToStatsTask.put(rsp.getName().getValue(), statsTask);

        /* Something to consider:
         * do we want to schedule each RSP or should we just process all the RSPs with a
         * single schedule task? Pros/Cons If just using one schedule task, then there
         * could be a big bang performance hit each time it fires, but it might be simpler.
         */
        rspScheduler = initStatistics(rspScheduler, RSP_STATS_THREAD_FACTORY_NAME);
        rspScheduler.scheduleAtFixedRate(
                statsTask, rspStatisticsPeriodSeconds, rspStatisticsPeriodSeconds, TimeUnit.SECONDS);
    }

    public void scheduleSfStatistics(ServiceFunction sf, SfcOfTableOffsets sfcOfTableOffsets) {
        if (sfToStatsTask.get(sf.getName().getValue()) != null) {
            // Dont launch duplicate tasks
            LOG.info("SfcStatisticsManagerImpl::scheduleRspStatistics not repeating task for sf [{}]",
                    sf.getName().getValue());
            return;
        }

        SfStatisticsHandler sfStatsHandler = new SfStatisticsHandler(sf, sfcOfTableOffsets);
        SfcStatisticsHandlerTask statsTask = new SfcStatisticsHandlerTask(sfStatsHandler);
        sfToStatsTask.put(sf.getName().getValue(), statsTask);

        sfScheduler = initStatistics(sfScheduler, SF_STATS_THREAD_FACTORY_NAME);
        sfScheduler.scheduleAtFixedRate(
                statsTask, sfStatisticsPeriodSeconds, sfStatisticsPeriodSeconds, TimeUnit.SECONDS);
    }

    public void scheduleSffStatistics(ServiceFunctionForwarder sff, SfcOfTableOffsets sfcOfTableOffsets) {
        if (sffToStatsTask.get(sff.getName().getValue()) != null) {
            // Dont launch duplicate tasks
            LOG.info("SfcStatisticsManagerImpl::scheduleRspStatistics not repeating task for sff [{}]",
                    sff.getName().getValue());
            return;
        }

        SffStatisticsHandler sffStatsHandler = new SffStatisticsHandler(sff, sfcOfTableOffsets);
        SfcStatisticsHandlerTask statsTask = new SfcStatisticsHandlerTask(sffStatsHandler);
        sffToStatsTask.put(sff.getName().getValue(), statsTask);

        sffScheduler = initStatistics(sffScheduler, SFF_STATS_THREAD_FACTORY_NAME);
        sffScheduler.scheduleAtFixedRate(
                statsTask, sffStatisticsPeriodSeconds, sffStatisticsPeriodSeconds, TimeUnit.SECONDS);
    }

    // Will be called by the SfcStatisticsConfiguration listener upon change
    public void updateSfcStatisticsConfiguration() {
        // Store the previous values to later see if they changed
        final long rspStatsPeriod = this.rspStatisticsPeriodSeconds;
        final long sfStatsPeriod  = this.sfStatisticsPeriodSeconds;
        final long sffStatsPeriod = this.sffStatisticsPeriodSeconds;

        // Reads the periods from the data store
        getSfcStatisticsConfiguration();

        // RSP stats
        if (rspStatsPeriod != this.rspStatisticsPeriodSeconds) {
            initScheduler(rspScheduler, RSP_STATS_THREAD_FACTORY_NAME);
            updateStatsConfiguration(rspScheduler, rspToStatsTask, rspStatisticsPeriodSeconds);
        }

        // SFF stats
        if (sffStatsPeriod != this.sffStatisticsPeriodSeconds) {
            initScheduler(sffScheduler, SFF_STATS_THREAD_FACTORY_NAME);
            updateStatsConfiguration(sffScheduler, sffToStatsTask, sffStatisticsPeriodSeconds);
        }

        // SF stats
        if (sfStatsPeriod != this.sfStatisticsPeriodSeconds) {
            initScheduler(sfScheduler, SF_STATS_THREAD_FACTORY_NAME);
            updateStatsConfiguration(sfScheduler, sfToStatsTask, sfStatisticsPeriodSeconds);
        }
    }

    private void updateStatsConfiguration(ScheduledExecutorService statsScheduler,
                                          Map<String, SfcStatisticsHandlerTask> statsTasks,
                                          long periodSeconds) {
        // Stop the statistics collection
        statsScheduler.shutdownNow();

        // If the period is 0, dont restart these stats
        if (periodSeconds > 0) {
            for (SfcStatisticsHandlerTask statsTask : statsTasks.values()) {
                statsScheduler.scheduleAtFixedRate(statsTask, periodSeconds, periodSeconds, TimeUnit.SECONDS);
            }
        }
    }

    private ScheduledExecutorService initStatistics(ScheduledExecutorService statsScheduler, String threadFactoryName) {
        if (statsScheduler != null) {
            return statsScheduler;
        } else {
            // Retrieve the statistics periods from the config data store
            getSfcStatisticsConfiguration();

            return initScheduler(null, threadFactoryName);
        }
    }

    private ScheduledExecutorService initScheduler(ScheduledExecutorService statsScheduler, String threadFactoryName) {
        if (statsScheduler != null) {
            return statsScheduler;
        }

        return Executors.newScheduledThreadPool(CORE_POOL_SIZE,
                ThreadFactoryProvider.builder().namePrefix(threadFactoryName).logger(LOG).build().get());
    }

    private void getSfcStatisticsConfiguration() {
        InstanceIdentifier<SfcStatisticsConfiguration> sfcStatsIid =
                InstanceIdentifier.builder(SfcStatisticsConfiguration.class).build();
        SfcStatisticsConfiguration sfcStatisticsConfig =
                SfcDataStoreAPI.readTransactionAPI(sfcStatsIid, LogicalDatastoreType.CONFIGURATION);

        if (sfcStatisticsConfig != null) {
            this.rspStatisticsPeriodSeconds = sfcStatisticsConfig.getSfcRspStatisticsPeriod();
            this.sffStatisticsPeriodSeconds = sfcStatisticsConfig.getSfcSffStatisticsPeriod();
            this.sfStatisticsPeriodSeconds  = sfcStatisticsConfig.getSfcSfStatisticsPeriod();
        } else {
            LOG.info("SfcStatisticsManagerImpl SfcStatisticsConfiguration doesnt exist in the data store,"
                    + " using default values");

            // Use default values
            this.rspStatisticsPeriodSeconds = DEFAULT_STATISTICS_PERIOD_SECONDS;
            this.sffStatisticsPeriodSeconds = DEFAULT_STATISTICS_PERIOD_SECONDS;
            this.sfStatisticsPeriodSeconds  = DEFAULT_STATISTICS_PERIOD_SECONDS;

            // Populate the Data Store with the values being used
            SfcStatisticsConfigurationBuilder builder = new SfcStatisticsConfigurationBuilder();
            builder.setSfcRspStatisticsPeriod(DEFAULT_STATISTICS_PERIOD_SECONDS);
            builder.setSfcSffStatisticsPeriod(DEFAULT_STATISTICS_PERIOD_SECONDS);
            builder.setSfcSfStatisticsPeriod(DEFAULT_STATISTICS_PERIOD_SECONDS);
            SfcDataStoreAPI.writePutTransactionAPI(sfcStatsIid, builder.build(), LogicalDatastoreType.CONFIGURATION);
        }
    }
}

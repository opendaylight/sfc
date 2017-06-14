/**
 * Copyright (c) 2017 Inocybe Technologies Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;

public abstract class SfcStatisticsManagerBase implements AutoCloseable {

    public static final long DEFAULT_STATISTICS_PERIOD_SECONDS = 60 * 15; // 15 minutes
    public static final int DEFAULT_MAX_NUM_PERIODS = 48; // 12 hours worth using the default period

    protected long statisticsPeriodSeconds;
    protected int maxNumPeriodsToStore;

    public SfcStatisticsManagerBase(long statisticsPeriod, int maxNumPeriods) {
        this.statisticsPeriodSeconds = statisticsPeriod;
        this.maxNumPeriodsToStore = maxNumPeriods;
    }

    public abstract void scheduleRspStatistics(RenderedServicePath rsp, SfcOfTableOffsets sfcOfTableOffsets);

    public abstract void scheduleSfStatistics(ServiceFunction sf);

    public abstract void scheduleSffStatistics(ServiceFunctionForwarder sff);

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

}

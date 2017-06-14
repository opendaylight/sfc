/**
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.handlers;

import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestampKey;

public abstract class SfcStatisticsHandlerBase {

    protected StatisticByTimestampKey timestampKey;
    protected int maxNumPeriods;

    public SfcStatisticsHandlerBase() {
        this.timestampKey = new StatisticByTimestampKey(new BigInteger(String.valueOf(System.currentTimeMillis())));
        this.maxNumPeriods = 0;
    }

    public SfcStatisticsHandlerBase(int maxNumPeriods) {
        this.timestampKey = new StatisticByTimestampKey(new BigInteger(String.valueOf(System.currentTimeMillis())));
        this.maxNumPeriods = maxNumPeriods;
    }

    public abstract void writeStatistics();

    public StatisticByTimestampKey getTimestampKey() {
        return timestampKey;
    }

    public void setTimestampKey(StatisticByTimestampKey timestampKey) {
        this.timestampKey = timestampKey;
    }

    public int getMaxNumPeriods() {
        return maxNumPeriods;
    }

    public void setMaxNumPeriods(int maxNumPeriods) {
        this.maxNumPeriods = maxNumPeriods;
    }

}

/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.handlers;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.sfc.statistics.readers.SfcStatisticsReaderBase;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestampKey;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class SfcStatisticsHandlerBase {

    private final SfcStatisticsReaderBase statsReader;

    // The default constructor cant be instantiated
    private SfcStatisticsHandlerBase() {
        this.statsReader = null;
    }

    public SfcStatisticsHandlerBase(SfcStatisticsReaderBase statsReader) {
        this.statsReader = statsReader;
    }

    public abstract <T extends DataObject> List<StatisticByTimestamp> getStatistics(T data);

    public SfcStatisticsReaderBase getStatsReader() {
        return this.statsReader;
    }

    protected StatisticByTimestampKey getTimestampKey() {
        return new StatisticByTimestampKey(new BigInteger(String.valueOf(System.currentTimeMillis())));
    }
}

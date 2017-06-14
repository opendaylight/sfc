/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
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

    public SfcStatisticsHandlerBase() {
        updateTimestamp();
    }

    // Template method design pattern.
    // Common entry point to writing statistics:
    // - perform common operations here
    // - delegate specific operations to sub-classes.
    public void writeStatistics() {
        updateTimestamp();

        doWriteStatistics();
    }

    protected abstract void doWriteStatistics();

    protected void updateTimestamp() {
        this.timestampKey = new StatisticByTimestampKey(new BigInteger(String.valueOf(System.currentTimeMillis())));
    }
}

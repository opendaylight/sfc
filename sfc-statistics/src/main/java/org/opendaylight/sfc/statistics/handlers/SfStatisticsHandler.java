/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.handlers;

import java.util.Collections;
import java.util.List;

import org.opendaylight.sfc.statistics.readers.SfcStatisticsReaderBase;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfStatisticsHandler extends SfcStatisticsHandlerBase {

    private static final Logger LOG = LoggerFactory.getLogger(SfStatisticsHandler.class);
    private ServiceFunction sf;

    public SfStatisticsHandler(SfcStatisticsReaderBase statsReader) {
        super(statsReader);
    }

    @Override
    protected <T extends org.opendaylight.yangtools.yang.binding.DataObject> List<StatisticByTimestamp>
        doGetStatistics(T data) {
        LOG.warn("SfStatisticsHandler::writeStatistics NOT implemented yet");

        return Collections.emptyList();
    }
}

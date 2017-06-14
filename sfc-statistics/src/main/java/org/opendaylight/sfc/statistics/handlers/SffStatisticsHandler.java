/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.handlers;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SffStatisticsHandler extends SfcStatisticsHandlerBase {

    private static final Logger LOG = LoggerFactory.getLogger(SffStatisticsHandler.class);
    private final SfcOfTableOffsets sfcOfTableOffsets;
    private ServiceFunctionForwarder sff;

    public SffStatisticsHandler(ServiceFunctionForwarder sff, SfcOfTableOffsets sfcOfTableOffsets) {
        this.sff = sff;
        this.sfcOfTableOffsets = sfcOfTableOffsets;
    }

    @Override
    protected void doWriteStatistics() {
        LOG.warn("SffStatisticsHandler::writeStatistics NOT implemented yet");
    }
}

/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics;

import org.opendaylight.sfc.statistics.handlers.RspStatisticsHandler;
import org.opendaylight.sfc.statistics.handlers.SfStatisticsHandler;
import org.opendaylight.sfc.statistics.handlers.SfcStatisticsHandlerBase;
import org.opendaylight.sfc.statistics.handlers.SffStatisticsHandler;
import org.opendaylight.sfc.statistics.readers.SfcIosXeStatisticsReader;
import org.opendaylight.sfc.statistics.readers.SfcOpenFlowStatisticsReader;
import org.opendaylight.sfc.statistics.readers.SfcStatisticsReaderBase;
import org.opendaylight.sfc.statistics.readers.SfcVppStatisticsReader;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.vpp.rev160706.SffNetconfAugmentation;

/**
 * This class serves as a factory that returns the requested
 * implementation of SfcStatisticsHandlerBase. Internally an
 * implementation of SfcStatisticsReaderInterface will be
 * determined (based on the SFF used) that will be passed
 * into the StatsHandler upon construction.
 *
 *<p>
 * This implements a Strategy design pattern, whereby the
 * k,
 * stats reader impl (OpenFlow, Vpp, IOS-XE) is separated
 * from the type of stats (RSP, SFF, or SF) being collected,
 * thus increasing the cohesion of both the stats reader
 * classes and the stats handler classes.
 *
 */
public class SfcStatisticsFactory {

    // This class can not be instantiated
    private SfcStatisticsFactory() {
    }

    public static SfcStatisticsHandlerBase getRspHandler(ServiceFunctionForwarder sff) {
        return new RspStatisticsHandler(getStatsReader(sff));
    }

    public static SfcStatisticsHandlerBase getSffHandler(ServiceFunctionForwarder sff) {
        return new SffStatisticsHandler(getStatsReader(sff));
    }

    public static SfcStatisticsHandlerBase getSfHandler(ServiceFunctionForwarder sff) {
        return new SfStatisticsHandler(getStatsReader(sff));
    }

    private static SfcStatisticsReaderBase getStatsReader(ServiceFunctionForwarder sff) {
        SffOvsBridgeAugmentation sffOvsBridgeAugmentation =
                sff.getAugmentation(SffOvsBridgeAugmentation.class);
        if (sffOvsBridgeAugmentation != null) {
            // OVS-based SFF
            return new SfcOpenFlowStatisticsReader(sff);
        }

        SffNetconfAugmentation sffNetconfAugmentation = sff.getAugmentation(SffNetconfAugmentation.class);
        if (sffNetconfAugmentation != null) {
            // VPP-based SFF
            return new SfcVppStatisticsReader(sff);
        }

        // Assuming if its not OVS nor VPP based, then its ios-xe based
        return new SfcIosXeStatisticsReader(sff);
    }
}

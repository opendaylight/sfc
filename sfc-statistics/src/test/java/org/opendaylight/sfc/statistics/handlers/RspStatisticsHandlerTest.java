/*
 * Copyright (c) 2018 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.statistics.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.opendaylight.sfc.statistics.SfcStatisticsFactory;
import org.opendaylight.sfc.statistics.readers.SfcOpenFlowStatisticsReader;
import org.opendaylight.sfc.statistics.testutils.AbstractDataStoreManager;
import org.opendaylight.sfc.statistics.testutils.SfcStatisticsTestUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;

public class RspStatisticsHandlerTest extends AbstractDataStoreManager {

    private SfcStatisticsTestUtils sfcStatsTestUtils;

    @Before
    public void setUp() {
        setupSfc();
        sfcStatsTestUtils = new SfcStatisticsTestUtils();
    }

    @Test
    public void noSfcOfTableOffsetsTest() {
        List<RenderedServicePath> rspList = sfcStatsTestUtils.createOperationalRsps(1, false);
        assertEquals(1, rspList.size());
        RenderedServicePath rsp = rspList.get(0);
        ServiceFunctionForwarder sff = sfcStatsTestUtils.getSffFromRsp(rsp, 1);
        assertNotNull(sff);


        RspStatisticsHandler rspStatsHandler =
                (RspStatisticsHandler) SfcStatisticsFactory.getRspHandler(rsp, sff);
        assertTrue(rspStatsHandler.getStatsReader() instanceof SfcOpenFlowStatisticsReader);
        List<StatisticByTimestamp> statsList = rspStatsHandler.getStatistics(rsp);
        assertEquals(1, statsList.size());
    }
}

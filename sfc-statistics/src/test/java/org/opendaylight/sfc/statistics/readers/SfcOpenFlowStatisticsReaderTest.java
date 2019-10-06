/*
 * Copyright (c) 2018 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.statistics.readers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import org.opendaylight.sfc.statistics.testutils.AbstractDataStoreManager;
import org.opendaylight.sfc.statistics.testutils.SfcStatisticsTestUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatistic;


public class SfcOpenFlowStatisticsReaderTest extends AbstractDataStoreManager {

    private SfcStatisticsTestUtils sfcStatsTestUtils;

    @Before
    public void setUp() {
        setupSfc();
        sfcStatsTestUtils = new SfcStatisticsTestUtils();
    }

    @Test
    public void noSfcOfTableOffsetsTest() {
        List<ServiceFunctionForwarder> sffList = sfcStatsTestUtils.createBaseSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());

        try {
            // This should throw an IllegalArgumentException exception since
            // the sfcOfTableOffsets havent been written to the data store
            new SfcOpenFlowStatisticsReader(sffList.get(0));
            fail("SfcOpenFlowStatisticsReader constructor should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void getNextHopStatisticsTest() {
        List<RenderedServicePath> rspList = sfcStatsTestUtils.createOperationalRsps(1, false);
        assertEquals(1, rspList.size());
        RenderedServicePath rsp = rspList.get(0);

        // Get the SFF from the first hop in the chain to be used in the SfcOpenFlowStatisticsReader()
        List<RenderedServicePathHop> rspHops = rsp.getRenderedServicePathHop();
        assertFalse(rspHops.isEmpty());

        ServiceFunctionForwarder sff = sfcStatsTestUtils.getSffFromRsp(rsp, 0);
        SfcOpenFlowStatisticsReader sfcOpenFlowStatsReader = new SfcOpenFlowStatisticsReader(sff);
        // Check the input stats
        Optional<ServiceStatistic> stats =
                sfcOpenFlowStatsReader.getNextHopStatistics(
                        true, sff, rsp.getPathId().toJava(), rsp.getStartingIndex().toJava());
        assertTrue(stats.isPresent());
        sfcStatsTestUtils.checkStatistics(stats.get(), true,
                sfcStatsTestUtils.STATS_COUNTER_BYTES, sfcStatsTestUtils.STATS_COUNTER_PACKETS);

        // Check the output stats
        stats = sfcOpenFlowStatsReader.getNextHopStatistics(
                false, sff, rsp.getPathId().toJava(), rsp.getStartingIndex().toJava());
        assertTrue(stats.isPresent());
        sfcStatsTestUtils.checkStatistics(stats.get(), false,
                sfcStatsTestUtils.STATS_COUNTER_BYTES, sfcStatsTestUtils.STATS_COUNTER_PACKETS);
    }

    @Test
    public void getTransportIngressStatisticsTest() {
        List<ServiceFunctionForwarder> sffList =
                sfcStatsTestUtils.createOpenflowSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());

        SfcOpenFlowStatisticsReader sfcOpenFlowStatsReader = new SfcOpenFlowStatisticsReader(sffList.get(0));
        Optional<ServiceStatistic> stats = sfcOpenFlowStatsReader.getTransportIngressStatistics(sffList.get(0));

        // sfcOpenFlowStatsReader.getTransportIngressStatistics() isnt implemented yet, so it should be empty
        assertFalse(stats.isPresent());
    }

    @Test
    public void getTransportEgressStatisticsTest() {
        List<ServiceFunctionForwarder> sffList =
                sfcStatsTestUtils.createOpenflowSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());

        SfcOpenFlowStatisticsReader sfcOpenFlowStatsReader = new SfcOpenFlowStatisticsReader(sffList.get(0));
        Optional<ServiceStatistic> stats = sfcOpenFlowStatsReader.getTransportEgressStatistics(sffList.get(0));

        // sfcOpenFlowStatsReader.getTransportEgressStatistics() isnt implemented yet, so it should be empty
        assertFalse(stats.isPresent());
    }
}

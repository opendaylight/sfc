/*
 * Copyright (c) 2018 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.opendaylight.sfc.statistics.handlers.RspStatisticsHandler;
import org.opendaylight.sfc.statistics.handlers.SfStatisticsHandler;
import org.opendaylight.sfc.statistics.handlers.SfcStatisticsHandlerBase;
import org.opendaylight.sfc.statistics.handlers.SffStatisticsHandler;
import org.opendaylight.sfc.statistics.readers.SfcIosXeStatisticsReader;
import org.opendaylight.sfc.statistics.readers.SfcOpenFlowLogicalSffStatisticsReader;
import org.opendaylight.sfc.statistics.readers.SfcOpenFlowStatisticsReader;
import org.opendaylight.sfc.statistics.readers.SfcVppStatisticsReader;
import org.opendaylight.sfc.statistics.testutils.AbstractDataStoreManager;
import org.opendaylight.sfc.statistics.testutils.SfcStatisticsTestUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;

public class SfcStatisticsFactoryTest extends AbstractDataStoreManager {

    private SfcStatisticsTestUtils sfcStatsTestUtils;

    @Before
    public void setUp() {
        setupSfc();
        sfcStatsTestUtils = new SfcStatisticsTestUtils();
    }

    @Test
    public void getRspHandlerOpenFlowTest() {
        List<ServiceFunctionForwarder> sffList =
                sfcStatsTestUtils.createOpenflowSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());

        SfcStatisticsHandlerBase base = SfcStatisticsFactory.getRspHandler(null, sffList.get(0));

        assertTrue(base.getStatsReader() instanceof SfcOpenFlowStatisticsReader);
        assertTrue(base instanceof RspStatisticsHandler);
    }

    @Test
    public void getRspHandlerOpenFlowLogicalTest() {
        RenderedServicePath rsp = sfcStatsTestUtils.createRspWithLogicalSff();
        List<ServiceFunctionForwarder> sffList = sfcStatsTestUtils.createBaseSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());

        ServiceFunctionForwarder sff = sffList.get(0);
        sfcStatsTestUtils.setTableOffsets(sff.getName(), 0);
        SfcStatisticsHandlerBase base = SfcStatisticsFactory.getRspHandler(rsp, sff);

        assertTrue(base.getStatsReader() instanceof SfcOpenFlowLogicalSffStatisticsReader);
        assertTrue(base instanceof RspStatisticsHandler);
    }

    @Test
    public void getRspHandlerVppTest() {
        List<ServiceFunctionForwarder> sffList = sfcStatsTestUtils.createVppSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());

        // For now, just create an empty RSP
        SfcStatisticsHandlerBase base =
                SfcStatisticsFactory.getRspHandler(new RenderedServicePathBuilder().build(), sffList.get(0));

        assertTrue(base.getStatsReader() instanceof SfcVppStatisticsReader);
        assertTrue(base instanceof RspStatisticsHandler);
    }

    @Test
    public void getRspHandlerIosXeTest() {
        List<ServiceFunctionForwarder> sffList = sfcStatsTestUtils.createIosXeSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());

        // For now, just create an empty RSP
        SfcStatisticsHandlerBase base =
                SfcStatisticsFactory.getRspHandler(new RenderedServicePathBuilder().build(), sffList.get(0));

        assertTrue(base.getStatsReader() instanceof SfcIosXeStatisticsReader);
        assertTrue(base instanceof RspStatisticsHandler);
    }

    @Test
    public void getRspHandlerEmptyStatsReaderTest() {
        RenderedServicePath rsp = sfcStatsTestUtils.createRspWithLogicalSff();
        List<ServiceFunctionForwarder> sffList = sfcStatsTestUtils.createBaseSffs(1, Collections.emptyList());

        assertEquals(1, sffList.size());
        assertNull(SfcStatisticsFactory.getRspHandler(rsp, sffList.get(0)));
    }

    @Test
    public void getSffHandlerOpenFlowTest() {
        List<ServiceFunctionForwarder> sffList =
                sfcStatsTestUtils.createOpenflowSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());
        SfcStatisticsHandlerBase base = SfcStatisticsFactory.getSffHandler(null, sffList.get(0));

        assertTrue(base.getStatsReader() instanceof SfcOpenFlowStatisticsReader);
        assertTrue(base instanceof SffStatisticsHandler);
    }

    @Test
    public void getSfHandlerTest() {
        List<ServiceFunctionForwarder> sffList =
                sfcStatsTestUtils.createOpenflowSffs(1, Collections.emptyList());
        assertEquals(1, sffList.size());
        SfcStatisticsHandlerBase base = SfcStatisticsFactory.getSfHandler(null, sffList.get(0));

        assertTrue(base.getStatsReader() instanceof SfcOpenFlowStatisticsReader);
        assertTrue(base instanceof SfStatisticsHandler);
    }
}

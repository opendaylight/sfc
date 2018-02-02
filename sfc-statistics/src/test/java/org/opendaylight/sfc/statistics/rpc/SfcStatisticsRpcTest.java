/**
 * Copyright (c) 2018 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import org.opendaylight.sfc.statistics.testutils.AbstractDataStoreManager;
import org.opendaylight.sfc.statistics.testutils.SfcStatisticsTestUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetRspStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetRspStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSfStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSfStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSffStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSffStatisticsOutput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;


public class SfcStatisticsRpcTest extends AbstractDataStoreManager {

    private static String RSP_NAME_NON_EXISTANT = "RspDoesntExist";

    private SfcStatisticsRpc sfcStatisticsRpc;
    private SfcStatisticsTestUtils sfcStatsTestUtils;

    @Before
    public void setUp() {
        setupSfc();
        sfcStatisticsRpc = new SfcStatisticsRpc();
        sfcStatsTestUtils = new SfcStatisticsTestUtils();
    }

    // Internal util method to check GetRspStatisticsOutput failures
    private void assertRspApplicationError(Future<RpcResult<GetRspStatisticsOutput>> resultFuture)
            throws InterruptedException, ExecutionException {
        assertNotNull(resultFuture);
        RpcResult<GetRspStatisticsOutput> rpcResult = resultFuture.get();

        assertNotNull(rpcResult);
        assertNull(rpcResult.getResult());
        assertFalse(rpcResult.isSuccessful());

        Collection<RpcError> rpcErrors = rpcResult.getErrors();
        assertNotNull(rpcErrors);
        assertEquals(1, rpcErrors.size());
        assertEquals(RpcError.ErrorType.APPLICATION, rpcErrors.iterator().next().getErrorType());
    }

    @Test
    public void getRspStatisticsNoRspsTest() throws Exception {
        GetRspStatisticsInputBuilder getRspStatisticsInputBuilder = new GetRspStatisticsInputBuilder();
        Future<RpcResult<GetRspStatisticsOutput>> result =
                sfcStatisticsRpc.getRspStatistics(getRspStatisticsInputBuilder.build());

        // An error should be returned when no RSPs have been created.
        assertRspApplicationError(result);
    }

    @Test
    public void getRspStatisticsNonExistantRspTest() throws Exception {
        // Create 1 RSP
        List<RenderedServicePath> rspList = sfcStatsTestUtils.createOperationalRsps(1, false);
        assertEquals(1, rspList.size());

        GetRspStatisticsInputBuilder getRspStatisticsInputBuilder = new GetRspStatisticsInputBuilder();
        getRspStatisticsInputBuilder.setName(RSP_NAME_NON_EXISTANT);
        Future<RpcResult<GetRspStatisticsOutput>> result =
                sfcStatisticsRpc.getRspStatistics(getRspStatisticsInputBuilder.build());

        // An error should be returned when a Non-Existent RSP name is provided
        assertRspApplicationError(result);
    }

    @Test
    public void getRspStatisticsSingleRspTest() throws Exception {
        // Create 1 RSP
        List<RenderedServicePath> rspList = sfcStatsTestUtils.createOperationalRsps(1, false);
        assertEquals(1, rspList.size());

        RenderedServicePath rsp = rspList.get(0);
        GetRspStatisticsInputBuilder getRspStatisticsInputBuilder = new GetRspStatisticsInputBuilder();
        getRspStatisticsInputBuilder.setName(rsp.getName().getValue());
        Future<RpcResult<GetRspStatisticsOutput>> result =
                sfcStatisticsRpc.getRspStatistics(getRspStatisticsInputBuilder.build());

        assertNotNull(result);
        assertNotNull(result.get());
        assertTrue(result.get().isSuccessful());
        assertTrue(result.get().getErrors().isEmpty());

        GetRspStatisticsOutput output = result.get().getResult();
        assertNotNull(output);
        assertEquals(1, output.getStatistics().size());

        sfcStatsTestUtils.checkStatistics(output.getStatistics().get(0), rsp.getName(),
                sfcStatsTestUtils.STATS_COUNTER_BYTES, sfcStatsTestUtils.STATS_COUNTER_BYTES,
                sfcStatsTestUtils.STATS_COUNTER_PACKETS, sfcStatsTestUtils.STATS_COUNTER_PACKETS);
    }

    @Test
    public void getRspStatisticsAllRspsTest() throws Exception {
        // Create 2 RSPs and give an empty RSP name, it should return stats for all RSPs
        List<RenderedServicePath> rspList = sfcStatsTestUtils.createOperationalRsps(2, true);
        assertEquals(4, rspList.size());

        Future<RpcResult<GetRspStatisticsOutput>> result =
                sfcStatisticsRpc.getRspStatistics(new GetRspStatisticsInputBuilder().build());

        assertNotNull(result);
        assertNotNull(result.get());
        assertTrue(result.get().isSuccessful());
        assertTrue(result.get().getErrors().isEmpty());

        GetRspStatisticsOutput output = result.get().getResult();
        assertNotNull(output);
        // We created 2 symmetric RSPs, which should return stats for 4 RSPs
        assertEquals(4, output.getStatistics().size());

        for (int i = 0; i < rspList.size(); i++) {
            // The stats list and rsp list arent in the same order
            sfcStatsTestUtils.checkStatistics(output.getStatistics().get(i), null,
                    sfcStatsTestUtils.STATS_COUNTER_BYTES, sfcStatsTestUtils.STATS_COUNTER_BYTES,
                    sfcStatsTestUtils.STATS_COUNTER_PACKETS, sfcStatsTestUtils.STATS_COUNTER_PACKETS);
        }
    }

    //
    // SFF Statistics tests
    //

    @Test
    public void getSffStatisticsTest() throws Exception {
        GetSffStatisticsInputBuilder getSffStatisticsInputBuilder = new GetSffStatisticsInputBuilder();
        Future<RpcResult<GetSffStatisticsOutput>> result =
                sfcStatisticsRpc.getSffStatistics(getSffStatisticsInputBuilder.build());

        // getSffStatistics is not implemented yet, so it should return an error

        assertNotNull(result);
        assertNotNull(result.get());
        assertNull(result.get().getResult());
        assertFalse(result.get().isSuccessful());

        Collection<RpcError> rpcErrors = result.get().getErrors();
        assertNotNull(rpcErrors);
        assertEquals(1, rpcErrors.size());
        assertEquals(RpcError.ErrorType.APPLICATION, rpcErrors.iterator().next().getErrorType());
    }

    //
    // SF Statistics tests
    //

    @Test
    public void getSfStatisticsTest() throws Exception {
        GetSfStatisticsInputBuilder getSfStatisticsInputBuilder = new GetSfStatisticsInputBuilder();
        Future<RpcResult<GetSfStatisticsOutput>> result =
                sfcStatisticsRpc.getSfStatistics(getSfStatisticsInputBuilder.build());

        // getSfStatistics is not implemented yet, so it should return an error

        assertNotNull(result);
        assertNotNull(result.get());
        assertNull(result.get().getResult());
        assertFalse(result.get().isSuccessful());

        Collection<RpcError> rpcErrors = result.get().getErrors();
        assertNotNull(rpcErrors);
        assertEquals(1, rpcErrors.size());
        assertEquals(RpcError.ErrorType.APPLICATION, rpcErrors.iterator().next().getErrorType());
    }
}

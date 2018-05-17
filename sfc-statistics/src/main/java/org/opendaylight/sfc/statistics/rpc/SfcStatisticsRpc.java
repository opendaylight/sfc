/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.rpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.statistics.SfcStatisticsFactory;
import org.opendaylight.sfc.statistics.handlers.SfcStatisticsHandlerBase;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetRspStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetRspStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetRspStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSfStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSfStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSffStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.GetSffStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.SfcStatisticsOperationsService;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.get.rsp.statistics.output.Statistics;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.stats.ops.rev171215.get.rsp.statistics.output.StatisticsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsRpc implements SfcStatisticsOperationsService {

    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsRpc.class);

    @Override
    public ListenableFuture<RpcResult<GetRspStatisticsOutput>> getRspStatistics(GetRspStatisticsInput input) {
        List<RenderedServicePath> rspList;

        if (input.getName() == null) {
            // If the name is not present, then return the stats for ALL RSPs
            rspList = getAllRenderedServicePaths();
        } else {
            RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName(input.getName()));
            if (rsp == null) {
                return RpcResultBuilder.<GetRspStatisticsOutput>failed().withError(
                                RpcError.ErrorType.APPLICATION, "RSP does not exist.").buildFuture();
            }

            rspList = Collections.singletonList(rsp);
        }

        List<Statistics> statsList = new ArrayList<>();
        for (RenderedServicePath rsp : rspList) {
            Optional<ServiceFunctionForwarder> sff = getFirstSffFromRsp(rsp);
            if (!sff.isPresent()) {
                LOG.warn("There are no SFFs on this RSP [{}]", rsp.getName().getValue());
                continue;
            }

            SfcStatisticsHandlerBase rspStatsHandler = SfcStatisticsFactory.getRspHandler(rsp, sff.get());
            if (rspStatsHandler == null) {
                LOG.warn("Unable to get RspHandler for SFF [{}]", sff.get().getName().getValue());
                continue;
            }

            List<StatisticByTimestamp> rspStatsList = rspStatsHandler.getStatistics(rsp);
            if (rspStatsList.isEmpty()) {
                LOG.warn("No statistics available for RSP [{}]", rsp.getName().getValue());
                continue;
            }

            StatisticsBuilder statsBuilder = new StatisticsBuilder();
            statsBuilder.setName(rsp.getName().getValue());
            statsBuilder.setStatisticByTimestamp(rspStatsList);
            statsList.add(statsBuilder.build());
        }

        // No stats were collected
        if (statsList.isEmpty()) {
            return Futures.immediateFuture(
                    RpcResultBuilder.<GetRspStatisticsOutput>failed().withError(
                            RpcError.ErrorType.APPLICATION, "SFF statistics is not implemented yet.").build());
        }

        GetRspStatisticsOutputBuilder output = new GetRspStatisticsOutputBuilder();
        output.setStatistics(statsList);

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetSffStatisticsOutput>> getSffStatistics(GetSffStatisticsInput input) {
        return RpcResultBuilder.<GetSffStatisticsOutput>failed().withError(
                        RpcError.ErrorType.APPLICATION, "SFF statistics is not implemented yet.").buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetSfStatisticsOutput>> getSfStatistics(GetSfStatisticsInput input) {
        return RpcResultBuilder.<GetSfStatisticsOutput>failed().withError(
                        RpcError.ErrorType.APPLICATION, "SF statistics is not implemented yet.").buildFuture();
    }

    private List<RenderedServicePath> getAllRenderedServicePaths() {
        RenderedServicePaths rsps = SfcDataStoreAPI.readTransactionAPI(InstanceIdentifier.create(
                RenderedServicePaths.class), LogicalDatastoreType.OPERATIONAL);

        if (rsps == null) {
            LOG.warn("No RSPs exist");
            return Collections.emptyList();
        }

        List<RenderedServicePath> rspList = rsps.getRenderedServicePath();
        return rspList == null
                ? Collections.emptyList()
                : rspList;
    }

    private Optional<ServiceFunctionForwarder> getFirstSffFromRsp(RenderedServicePath rsp) {
        List<RenderedServicePathHop> rspHopList = rsp.getRenderedServicePathHop();
        if (rspHopList == null) {
            return Optional.empty();
        }

        if (rspHopList.isEmpty()) {
            return Optional.empty();
        }

        RenderedServicePathHop firstHop = rspHopList.get(0);
        if (firstHop == null) {
            return Optional.empty();
        }

        SffName sffName = firstHop.getServiceFunctionForwarder();
        if (sffName == null) {
            return Optional.empty();
        }

        ServiceFunctionForwarder sff =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        return Optional.ofNullable(sff);
    }
}

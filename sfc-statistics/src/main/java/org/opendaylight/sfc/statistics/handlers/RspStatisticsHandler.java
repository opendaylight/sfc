/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.statistics.utils.SfcStatisticsOpenFlowUtils;
import org.opendaylight.sfc.statistics.utils.SfcStatisticsWriter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestampBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RspStatisticsHandler extends SfcStatisticsHandlerBase {

    private static final Logger LOG = LoggerFactory.getLogger(RspStatisticsHandler.class);
    private final SfcOfTableOffsets sfcOfTableOffsets;
    private RenderedServicePath rsp;

    // Constructor for OpenFlow based statistics. Should probably make a VPP-based constructor too.
    public RspStatisticsHandler(RenderedServicePath rsp, SfcOfTableOffsets sfcOfTableOffsets) {
        this.rsp = rsp;
        this.sfcOfTableOffsets = sfcOfTableOffsets;
    }

    @Override
    protected void doWriteStatistics() {

        LOG.debug("RspStatisticsHandler::writeStatistics firing");

        // The RSP may have changed between stats invocations
        if (!getUpdatedRsp()) {
            LOG.warn("RspStatisticsHandler cant get updated RSP");
            return;
        }

        // Need to get the "Next Hop" flows for the first-hop and last-hop of the RSP
        // These will be the RSP in bytes/packets and out bytes/packets respectively
        Optional<ServiceFunctionForwarder> firstHopSff = getFirstHopSff();
        if (!firstHopSff.isPresent()) {
            LOG.warn("RspStatisticsHandler cant get firstHopSff for RPS [{}]", rsp.getPathId());
            return;
        }

        Optional<ServiceFunctionForwarder> lastHopSff = getLastHopSff();
        if (!lastHopSff.isPresent()) {
            LOG.warn("RspStatisticsHandler cant get lastHopSff for RPS [{}]", rsp.getPathId());
            return;
        }

        StatisticByTimestampBuilder statsBuilder = new StatisticByTimestampBuilder();
        statsBuilder.setTimestamp(this.timestampKey.getTimestamp());
        statsBuilder.setKey(this.timestampKey);
        statsBuilder.setServiceStatistic(new ServiceStatisticBuilder().build());

        boolean status = false;
        SffOvsBridgeAugmentation sffOvsBridgeAugmentation =
                firstHopSff.get().getAugmentation(SffOvsBridgeAugmentation.class);
        if (sffOvsBridgeAugmentation != null) {
            // OVS-based SFF
            status = getOpenFlowStatistics(firstHopSff.get(), lastHopSff.get(), statsBuilder, this.sfcOfTableOffsets);
        } else {
            // VPP-based SFF
            status = getVppStatistics(firstHopSff.get(), lastHopSff.get(), statsBuilder);
        }
        // TODO need to implement others later, like ios-xe??

        if (!status) {
            LOG.warn("RspStatisticsHandler::writeStatistics not able to collect statistics");
            return;
        }

        List<StatisticByTimestamp> statsList = new ArrayList<>();
        statsList.add(statsBuilder.build());

        RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder(this.rsp);
        rspBuilder.setStatisticByTimestamp(statsList);

        InstanceIdentifier<RenderedServicePath> rspStatsIid = InstanceIdentifier
                .builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, new RenderedServicePathKey(rsp.getName()))
                .build();

        SfcStatisticsWriter.writeRspStatistics(rspStatsIid, rspBuilder.build());

        LOG.debug("RspStatisticsHandler::writeStatistics completed with statistics");
    }

    private boolean getUpdatedRsp() {
        this.rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(this.rsp.getName());

        return this.rsp != null;
    }

    private Optional<ServiceFunctionForwarder> getFirstHopSff() {
        List<RenderedServicePathHop> rspHopList = this.rsp.getRenderedServicePathHop();
        if (rspHopList == null) {
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

    private Optional<ServiceFunctionForwarder> getLastHopSff() {
        List<RenderedServicePathHop> rspHopList = this.rsp.getRenderedServicePathHop();
        if (rspHopList == null) {
            return Optional.empty();
        }

        RenderedServicePathHop lastHop = rspHopList.get(rspHopList.size() - 1);
        if (lastHop == null) {
            return Optional.empty();
        }

        SffName sffName = lastHop.getServiceFunctionForwarder();
        if (sffName == null) {
            return Optional.empty();
        }

        ServiceFunctionForwarder sff =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        return Optional.ofNullable(sff);
    }

    private boolean getOpenFlowStatistics(ServiceFunctionForwarder firstHopSff, ServiceFunctionForwarder lastHopSff,
            StatisticByTimestampBuilder statsBuilder, SfcOfTableOffsets sfcOfTableOffsets) {

        if (! SfcStatisticsOpenFlowUtils.getSffNextHopStatsIn(
                firstHopSff, rsp.getPathId(), rsp.getStartingIndex(), statsBuilder, sfcOfTableOffsets)) {
            return false;
        }

        if (! SfcStatisticsOpenFlowUtils.getSffNextHopStatsOut(
                lastHopSff, rsp.getPathId(), rsp.getStartingIndex(), statsBuilder, sfcOfTableOffsets)) {
            return false;
        }

        return true;
    }

    private boolean getVppStatistics(ServiceFunctionForwarder firstHopSff, ServiceFunctionForwarder lastHopSff,
            StatisticByTimestampBuilder statsBuilder) {
        // Noop
        LOG.warn("RspStatisticsHandler getVppStatistics is not implemented yet");

        return false;
    }
}

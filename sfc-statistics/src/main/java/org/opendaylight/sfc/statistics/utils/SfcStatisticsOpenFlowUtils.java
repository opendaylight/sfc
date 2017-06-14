/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.utils;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestampBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.sfc.of.table.offsets.SfcOfTablesByBaseTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsOpenFlowUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsOpenFlowUtils.class);

    public static boolean getSffNextHopStatsIn(
            ServiceFunctionForwarder sff, long nsp, short nsi, StatisticByTimestampBuilder statsBuilder,
            SfcOfTableOffsets sfcOfTableOffsets) {

        Optional<FlowStatisticsData> flowStatsData = getSffNextHopStats(sff, nsp, nsi, sfcOfTableOffsets);
        if (!flowStatsData.isPresent()) {
            LOG.warn("SfcStatisticsOpenFlowUtils::getSffNextHopStatsIn flowStatsData is null for nsp [{}] nsi [{}]",
                    nsp, nsi);
            return false;
        }

        ServiceStatisticBuilder srvStatsBuilder = new ServiceStatisticBuilder(statsBuilder.getServiceStatistic());
        srvStatsBuilder.setBytesIn(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getByteCount()));
        srvStatsBuilder.setPacketsIn(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getPacketCount()));

        statsBuilder.setServiceStatistic(srvStatsBuilder.build());

        return true;
    }

    public static boolean getSffNextHopStatsOut(ServiceFunctionForwarder sff, long nsp, short nsi,
            StatisticByTimestampBuilder statsBuilder, SfcOfTableOffsets sfcOfTableOffsets) {

        Optional<FlowStatisticsData> flowStatsData = getSffNextHopStats(sff, nsp, nsi, sfcOfTableOffsets);
        if (!flowStatsData.isPresent()) {
            LOG.warn("SfcStatisticsOpenFlowUtils::getSffNextHopStatsOut flowStatsData is null for nsp [{}] nsi [{}]",
                    nsp, nsi);
            return false;
        }

        ServiceStatisticBuilder srvStatsBuilder = new ServiceStatisticBuilder(statsBuilder.getServiceStatistic());
        srvStatsBuilder.setBytesOut(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getByteCount()));
        srvStatsBuilder.setPacketsOut(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getPacketCount()));

        statsBuilder.setServiceStatistic(srvStatsBuilder.build());

        return true;
    }

    private static Optional<FlowStatisticsData> getSffNextHopStats(ServiceFunctionForwarder sff, long nsp, short nsi,
                                                                   SfcOfTableOffsets sfcOfTableOffsets) {
        Optional<NodeId> nodeId = getSffNodeId(sff);
        if (! nodeId.isPresent()) {
            return Optional.empty();
        }

        NodeKey nodeKey = new NodeKey(nodeId.get());
        TableKey tableKey = new TableKey(getNextHopTableId(sff, sfcOfTableOffsets));

        StringJoiner flowName = new StringJoiner(OpenflowConstants.FLOW_NAME_DELIMITER);
        flowName.add(OpenflowConstants.FLOW_NAME_NEXT_HOP).add(String.valueOf(nsi)).add(String.valueOf(nsp));
        FlowKey flowKey = new FlowKey(new FlowId(flowName.toString()));

        InstanceIdentifier<Flow> iidFlow = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey)
                .child(Flow.class, flowKey).build();
        Flow flow = SfcDataStoreAPI.readTransactionAPI(iidFlow, LogicalDatastoreType.OPERATIONAL);
        if (flow == null) {
            LOG.warn("SfcStatisticsOpenFlowUtils getSffNextHopStats flow null for flowName [{}]", flowName.toString());
            return Optional.empty();
        }

        FlowStatisticsData flowStatsData = flow.getAugmentation(FlowStatisticsData.class);
        if (flowStatsData == null) {
            LOG.warn("SfcStatisticsOpenFlowUtils getSffNextHopStats flowStatsData null for nsp [{}] nsi [{}]",
                    nsp, nsi);
            return Optional.empty();
        }

        LOG.debug("SfcStatisticsOpenFlowUtils stats nsp [{}] nsi [{}] bytes [{}] packets [{}]", nsp, nsi,
                  flowStatsData.getFlowStatistics().getByteCount().getValue(),
                  flowStatsData.getFlowStatistics().getPacketCount().getValue());

        return Optional.of(flowStatsData);
    }

    private static Optional<NodeId> getSffNodeId(ServiceFunctionForwarder sff) {

        SffOvsBridgeAugmentation sffOvsBridgeAugmentation = sff.getAugmentation(SffOvsBridgeAugmentation.class);
        if (sffOvsBridgeAugmentation == null) {
            LOG.warn("SfcStatisticsOpenFlowUtils::getSffNodeId sff [{}] does not have OvsBridgeAugmentation",
                    sff.getName());
            return Optional.empty();
        }

        String sffNodeName = null;
        if (sffOvsBridgeAugmentation.getOvsBridge() != null) {
            sffNodeName = sffOvsBridgeAugmentation.getOvsBridge().getOpenflowNodeId();
        }

        if (sffNodeName != null) {
            return Optional.of(new NodeId(sffNodeName));
        }

        LOG.warn("SfcStatisticsOpenFlowUtils::getSffNodeId not able to get NodeId for sff [{}]", sff.getName());

        return Optional.empty();
    }

    private static short getNextHopTableId(ServiceFunctionForwarder sff, SfcOfTableOffsets sfcOfTableOffsets) {
        List<SfcOfTablesByBaseTable> tableOffsets = sfcOfTableOffsets.getSfcOfTablesByBaseTable();
        SfcOfTablesByBaseTable ofTables = tableOffsets.get(0);

        return ofTables.getNextHopTable().shortValue();
    }

}

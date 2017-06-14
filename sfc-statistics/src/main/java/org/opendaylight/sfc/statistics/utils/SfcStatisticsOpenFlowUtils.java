/**
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.utils;

import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestampBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsOpenFlowUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsOpenFlowUtils.class);

    public static void getSffNextHopStatsIn(
            ServiceFunctionForwarder sff, long nsp, short nsi, StatisticByTimestampBuilder statsBuilder) {

        Optional<FlowStatisticsData> flowStatsData = getSffNextHopStats(sff, nsp, nsi);
        if (!flowStatsData.isPresent()) {
            return;
        }

        ServiceStatisticBuilder srvStatsBuilder = new ServiceStatisticBuilder(statsBuilder.getServiceStatistic());
        srvStatsBuilder.setBytesIn(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getByteCount()));
        srvStatsBuilder.setPacketsIn(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getPacketCount()));

        statsBuilder.setServiceStatistic(srvStatsBuilder.build());
    }

    public static void getSffNextHopStatsOut(ServiceFunctionForwarder sff, long nsp, short nsi,
            StatisticByTimestampBuilder statsBuilder) {

        Optional<FlowStatisticsData> flowStatsData = getSffNextHopStats(sff, nsp, nsi);
        if (!flowStatsData.isPresent()) {
            return;
        }

        ServiceStatisticBuilder srvStatsBuilder = new ServiceStatisticBuilder(statsBuilder.getServiceStatistic());
        srvStatsBuilder.setBytesOut(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getByteCount()));
        srvStatsBuilder.setPacketsOut(new ZeroBasedCounter64(flowStatsData.get().getFlowStatistics().getPacketCount()));

        statsBuilder.setServiceStatistic(srvStatsBuilder.build());

    }

    private static Optional<FlowStatisticsData> getSffNextHopStats(ServiceFunctionForwarder sff, long nsp, short nsi) {
        Optional<NodeId> nodeId = getSffNodeId(sff);

        NodeKey nodeKey = new NodeKey(nodeId.get());
        TableKey tableKey = new TableKey(getNextHopTableId(sff));
        //FlowKey flowKey = new FlowKey();

        /* TODO since the flowIds are created as AtomicIncrementing IDs, its not easy to know which to use
         *      for now, just get all the flows in the table.
        InstanceIdentifier<Flow> iidFlow = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey)
                .child(Flow.class, flowKey).build();
                */
        InstanceIdentifier<Table> iidTable = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).build();
        Table table = SfcDataStoreAPI.readTransactionAPI(iidTable, LogicalDatastoreType.OPERATIONAL);

        List<Flow> flowList = table.getFlow();
        for (Flow flow : flowList) {
            Match match = flow.getMatch();
            Optional<Long> matchNsp = getMatchNsp(match);
            if (!matchNsp.isPresent()) {
                return Optional.empty();
            }

            Optional<Short> matchNsi = getMatchNsi(match);
            if (!matchNsi.isPresent()) {
                return Optional.empty();
            }

            if (matchNsp.get().longValue() == nsp && matchNsi.get().shortValue() == nsi) {
                FlowStatisticsData flowStatsData = flow.getAugmentation(FlowStatisticsData.class);
                LOG.info("SfcStatisticsOpenFlowUtils stats nsp [{}] nsi [{}] bytes [{}] packets [{}]", nsp, nsi,
                        flowStatsData.getFlowStatistics().getByteCount(),
                        flowStatsData.getFlowStatistics().getPacketCount());

                return Optional.of(flowStatsData);
            }
        }

        return Optional.empty();
    }

    private static Optional<NodeId> getSffNodeId(ServiceFunctionForwarder sff) {

        SffOvsBridgeAugmentation sffOvsBridgeAugmentation = sff.getAugmentation(SffOvsBridgeAugmentation.class);
        if (sffOvsBridgeAugmentation == null) {
            return Optional.empty();
        }

        String sffNodeName = null;
        if (sffOvsBridgeAugmentation != null) {
            if (sffOvsBridgeAugmentation.getOvsBridge() != null) {
                sffNodeName = sffOvsBridgeAugmentation.getOvsBridge().getOpenflowNodeId();
            }
        }

        if (sffNodeName != null) {
            return Optional.of(new NodeId(sffNodeName));
        }

        return Optional.empty();
    }

    private static short getNextHopTableId(ServiceFunctionForwarder sff) {
        // TODO need to check if AppCoexistence has been configured or if its a LogicalSff
        //return SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP;
        return 4;
    }

    private static Optional<Long> getMatchNsp(Match match) {
        GeneralAugMatchNodesNodeTableFlow genAug =
                match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);

        List<ExtensionList> extensions = genAug.getExtensionList();
        for (ExtensionList extensionList : extensions) {
            Extension extension = extensionList.getExtension();
            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class);

            if (nxAugMatch.getNxmNxNsp() != null) {
                return Optional.of(nxAugMatch.getNxmNxNsp().getValue());
            }
        }

        return Optional.empty();
    }

    private static Optional<Short> getMatchNsi(Match match) {
        GeneralAugMatchNodesNodeTableFlow genAug =
                match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);

        List<ExtensionList> extensions = genAug.getExtensionList();
        for (ExtensionList extensionList : extensions) {
            Extension extension = extensionList.getExtension();
            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class);

            if (nxAugMatch.getNxmNxNsi() != null) {
                return Optional.of(nxAugMatch.getNxmNxNsi().getNsi());
            }
        }

        return Optional.empty();
    }
}

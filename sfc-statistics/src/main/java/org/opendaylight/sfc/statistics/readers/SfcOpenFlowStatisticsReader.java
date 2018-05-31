/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.readers;

import java.util.Optional;
import java.util.StringJoiner;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatistic;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.sfc.of.table.offsets.SfcOfTablesByBaseTable;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.sfc.of.table.offsets.SfcOfTablesByBaseTableKey;
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

public class SfcOpenFlowStatisticsReader extends SfcStatisticsReaderBase {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOpenFlowStatisticsReader.class);
    private SfcOfTablesByBaseTable sfcOfTableOffsets;

    public SfcOpenFlowStatisticsReader(ServiceFunctionForwarder sff) {
        // Get the table offsets for the SFF
        InstanceIdentifier<SfcOfTablesByBaseTable> iid = InstanceIdentifier.create(SfcOfTableOffsets.class)
                .child(SfcOfTablesByBaseTable.class, new SfcOfTablesByBaseTableKey(sff.getName()));

        this.sfcOfTableOffsets = SfcDataStoreAPI.readTransactionAPI(iid, LogicalDatastoreType.OPERATIONAL);
        if (this.sfcOfTableOffsets == null) {
            throw new IllegalArgumentException("No OfTableOffsets exist for SFF: " + sff.getName().getValue());
        }
    }

    @Override
    public Optional<ServiceStatistic>
        getNextHopStatistics(boolean inputStats, ServiceFunctionForwarder sff, long nsp, short nsi) {
        Optional<NodeId> nodeId = getSffNodeId(sff, nsp, nsi);
        if (! nodeId.isPresent()) {
            return Optional.empty();
        }

        String flowName = getNextHopFlowName(nsp, nsi);
        FlowKey flowKey = new FlowKey(new FlowId(flowName));

        InstanceIdentifier<Flow> iidFlow = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId.get()))
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(this.sfcOfTableOffsets.getNextHopTable().shortValue()))
                .child(Flow.class, flowKey).build();
        Flow flow = SfcDataStoreAPI.readTransactionAPI(iidFlow, LogicalDatastoreType.OPERATIONAL);
        if (flow == null) {
            LOG.warn("getSffNextHopStats flow null for flowName [{}]", flowName);
            return Optional.empty();
        }

        FlowStatisticsData flowStatsData = flow.augmentation(FlowStatisticsData.class);
        if (flowStatsData == null) {
            LOG.warn("getSffNextHopStats flowStatsData null for nsp [{}] nsi [{}]",
                    nsp, nsi);
            return Optional.empty();
        }

        LOG.debug("Stats nsp [{}] nsi [{}] bytes [{}] packets [{}]", nsp, nsi,
                flowStatsData.getFlowStatistics().getByteCount().getValue(),
                flowStatsData.getFlowStatistics().getPacketCount().getValue());

        return Optional.of(getStats(inputStats, flowStatsData));
    }

    @Override
    public Optional<ServiceStatistic> getTransportIngressStatistics(ServiceFunctionForwarder sff) {
        // Not implemented yet
        return Optional.empty();
    }

    @Override
    public Optional<ServiceStatistic> getTransportEgressStatistics(ServiceFunctionForwarder sff) {
        // Not implemented yet
        return Optional.empty();
    }

    private ServiceStatistic getStats(boolean inputStats, FlowStatisticsData flowStatsData) {
        ServiceStatisticBuilder srvStatsBuilder = new ServiceStatisticBuilder();
        if (inputStats) {
            srvStatsBuilder.setBytesIn(new ZeroBasedCounter64(flowStatsData.getFlowStatistics().getByteCount()));
            srvStatsBuilder.setPacketsIn(new ZeroBasedCounter64(flowStatsData.getFlowStatistics().getPacketCount()));
        } else {
            srvStatsBuilder.setBytesOut(new ZeroBasedCounter64(flowStatsData.getFlowStatistics().getByteCount()));
            srvStatsBuilder.setPacketsOut(new ZeroBasedCounter64(flowStatsData.getFlowStatistics().getPacketCount()));
        }

        return srvStatsBuilder.build();
    }

    protected Optional<NodeId> getSffNodeId(ServiceFunctionForwarder sff, long nsp, short nsi) {

        SffOvsBridgeAugmentation sffOvsBridgeAugmentation = sff.augmentation(SffOvsBridgeAugmentation.class);
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

    private static String getNextHopFlowName(long nsp, short nsi) {
        StringJoiner flowName = new StringJoiner(OpenflowConstants.OF_NAME_DELIMITER);
        flowName.add(OpenflowConstants.OF_NAME_NEXT_HOP).add(String.valueOf(nsi)).add(String.valueOf(nsp));
        return flowName.toString();
    }
}

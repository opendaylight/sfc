/*
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer.openflow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.l2renderer.sfg.GroupBucketInfo;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class writes Openflow Flow Entries to the SFF once an SFF has been configured.
 *
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 *
 * @version 0.1
 *
 * @since 2014-08-07
 */
public class SfcL2FlowProgrammerOFimpl implements SfcL2FlowProgrammerInterface {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowProgrammerOFimpl.class);

    private static final int COOKIE_BIGINT_HEX_RADIX = 16;
    private static final long SHUTDOWN_TIME = 5;
    private static final BigInteger TRANSPORT_EGRESS_COOKIE =
            new BigInteger("BA5EBA11BA5EBA11", COOKIE_BIGINT_HEX_RADIX);

    // Which bits in the metadata field to set, Assuming 4095 PathId's
    private static final BigInteger METADATA_MASK_SFP_MATCH =
            new BigInteger("000000000000FFFF", COOKIE_BIGINT_HEX_RADIX);

    private static final short TABLE_INDEX_CLASSIFIER_TABLE = 0;
    private static final short TABLE_INDEX_INGRESS_TRANSPORT_TABLE = 1;
    private static final short TABLE_INDEX_PATH_MAPPER = 2;
    private static final short TABLE_INDEX_PATH_MAPPER_ACL = 3;
    private static final short TABLE_INDEX_NEXT_HOP = 4;
    private static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;
    private static final short TABLE_INDEX_MAX_OFFSET = TABLE_INDEX_TRANSPORT_EGRESS;

    private static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 250;
    private static final int FLOW_PRIORITY_ARP_TRANSPORT_INGRESS = 300;
    private static final int FLOW_PRIORITY_PATH_MAPPER = 350;
    private static final int FLOW_PRIORITY_PATH_MAPPER_ACL = 450;
    private static final int FLOW_PRIORITY_NEXT_HOP = 550;
    private static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 650;
    private static final int FLOW_PRIORITY_MATCH_ANY = 5;

    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 1000;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final int PKTIN_IDLE_TIMEOUT = 60;

    private static final String LOGSTR_THREAD_QUEUE_FULL = "Thread Queue is full, cant execute action: {}";

    // Internal class used to store the details of a flow for easy deletion later
    private class FlowDetails {

        public String sffNodeName;
        public FlowKey flowKey;
        public TableKey tableKey;

        public FlowDetails(final String sffNodeName, FlowKey flowKey, TableKey tableKey) {
            this.sffNodeName = sffNodeName;
            this.flowKey = flowKey;
            this.tableKey = tableKey;
        }
    }

    // Instance variables
    private short tableBase;
    // TODO tableEgress is not implemented yet
    // Used for app-coexistence
    private short tableEgress;
    private ExecutorService threadPoolExecutorService;
    private Map<Long, List<FlowDetails>> rspNameToFlowsMap;
    private Long flowRspId;

    public SfcL2FlowProgrammerOFimpl() {
        this.tableBase = (short) 0;
        this.tableEgress = (short) 0;
        this.rspNameToFlowsMap = new HashMap<Long, List<FlowDetails>>();
        this.flowRspId = new Long(0);

        // Not using an Executors.newSingleThreadExecutor() here, since it creates
        // an Executor that uses a single worker thread operating off an unbounded
        // queue, and we want to be able to limit the size of the queue
        this.threadPoolExecutorService = new ThreadPoolExecutor(SCHEDULED_THREAD_POOL_SIZE, SCHEDULED_THREAD_POOL_SIZE,
                ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));

    }

    // This method should only be called by SfcL2Renderer.close()
    @Override
    public void shutdown() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        threadPoolExecutorService.shutdown();
        if (!threadPoolExecutorService.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) {
            LOG.error("SfcL2FlowProgrammerOFimpl Executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = threadPoolExecutorService.shutdownNow();
            LOG.error("SfcL2FlowProgrammerOFimpl Executor was abruptly shut down. [{}] tasks will not be executed.",
                    droppedTasks.size());
        }
    }

    @Override
    public short getTableBase() {
        return tableBase;
    }

    @Override
    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    @Override
    public short getTableEgress() {
        return tableEgress;
    }

    @Override
    public void setTableEgress(short tableEgress) {
        this.tableEgress = tableEgress;
    }

    @Override
    public short getMaxTableOffset() {
        return TABLE_INDEX_MAX_OFFSET;
    }

    @Override
    public void setFlowRspId(Long rspId) {
        this.flowRspId = rspId;
    }

    public boolean compareClassificationTableCookie(FlowCookie cookie) {
        if (cookie == null) {
            return false;
        }

        if (cookie.getValue() == null) {
            return false;
        }

        return cookie.getValue().equals(TRANSPORT_EGRESS_COOKIE);
    }

    @Override
    public void deleteRspFlows(final Long rspId) {
        List<FlowDetails> flowDetailsList = rspNameToFlowsMap.get(rspId);
        if (flowDetailsList == null) {
            LOG.warn("deleteRspFlows() no flows exist for RSP [{}]", rspId);
            return;
        }

        rspNameToFlowsMap.remove(rspId);
        for (FlowDetails flowDetails : flowDetailsList) {
            removeFlowFromConfig(flowDetails.sffNodeName, flowDetails.flowKey, flowDetails.tableKey);
        }

        // If there is just one entry left, then all flows for RSPs have
        // been deleted, and the only flows remaining are those that are
        // common to all RSPs, which can now be deleted
        if (rspNameToFlowsMap.size() == 1) {
            Set<Entry<Long, List<FlowDetails>>> entries = rspNameToFlowsMap.entrySet();
            flowDetailsList = entries.iterator().next().getValue();
            for (FlowDetails flowDetails : flowDetailsList) {
                removeFlowFromConfig(flowDetails.sffNodeName, flowDetails.flowKey, flowDetails.tableKey);
            }
            rspNameToFlowsMap.clear();
        }
    }

    //
    // Configure the MatchAny entry specifying if it should drop or goto the next table
    // Classifier MatchAny will go to TransportIngress
    // TransportIngress MatchAny will drop
    // PathMapper MatchAny will go to PathMapperAcl
    // PathMapperAcl MatchAny will go to NextHop
    // NextHop MatchAny will go to TransportEgress
    // TransportEgress MatchAny will drop
    //

    @Override
    public void configureClassifierTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_CLASSIFIER_TABLE),
                        getTableId(TABLE_INDEX_INGRESS_TRANSPORT_TABLE));
        writeFlowToConfig(flowRspId, sffNodeName, flowBuilder);
    }

    @Override
    public void configureTransportIngressTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyDropFlow(
                        getTableId(TABLE_INDEX_INGRESS_TRANSPORT_TABLE));
        writeFlowToConfig(flowRspId, sffNodeName, flowBuilder);
    }

    @Override
    public void configurePathMapperTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_PATH_MAPPER),
                        getTableId(TABLE_INDEX_PATH_MAPPER_ACL));
        writeFlowToConfig(flowRspId, sffNodeName, flowBuilder);
    }

    @Override
    public void configurePathMapperAclTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                        getTableId(TABLE_INDEX_NEXT_HOP));
        writeFlowToConfig(flowRspId, sffNodeName, flowBuilder);
    }

    @Override
    public void configureNextHopTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder =
                configureTableMatchAnyFlow(
                        getTableId(TABLE_INDEX_NEXT_HOP),
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
        writeFlowToConfig(flowRspId, sffNodeName, flowBuilder);
    }

    @Override
    public void configureTransportEgressTableMatchAny(final String sffNodeName) {
        // This is the last table, cant set next table AND doDrop should be false
        FlowBuilder flowBuilder =
                configureTableMatchAnyDropFlow(
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
        writeFlowToConfig(flowRspId, sffNodeName, flowBuilder);
    }

    private FlowBuilder configureTableMatchAnyDropFlow(short tableId) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyDropFlow tableId [{}]",
                tableId);

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionDropPacket(0));

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        int order = 0;
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order++);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());

        // Match any
        MatchBuilder match = new MatchBuilder();

        // Finish up the instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);
    }

    private FlowBuilder configureTableMatchAnyFlow(short tableId, short nextTableId) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyFlow, tableId [{}] nextTableId [{}]",
                tableId, nextTableId);

        // Action, goto next table
        GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(nextTableId);

        InstructionBuilder ib = new InstructionBuilder();
        int order = 0;
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order++);
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());

        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());

        // Match any
        MatchBuilder match = new MatchBuilder();

        // Finish up the instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);
    }

    //
    // Configure Table 1, Transport Ingress
    //

    @Override
    public void configureIpv4TransportIngressFlow(final String sffNodeName) {
        FlowBuilder transportIngressFlowTcp =
                configureTransportIngressFlow(
                        SfcOpenflowUtils.ETHERTYPE_IPV4,
                        SfcOpenflowUtils.IP_PROTOCOL_TCP);
        writeFlowToConfig(flowRspId, sffNodeName, transportIngressFlowTcp);

        FlowBuilder transportIngressFlowUdp =
                configureTransportIngressFlow(
                        SfcOpenflowUtils.ETHERTYPE_IPV4,
                        SfcOpenflowUtils.IP_PROTOCOL_UDP);
        writeFlowToConfig(flowRspId, sffNodeName, transportIngressFlowUdp);
    }

    @Override
    public void configureVlanTransportIngressFlow(final String sffNodeName) {
        FlowBuilder transportIngressFlow =
                configureTransportIngressFlow(SfcOpenflowUtils.ETHERTYPE_VLAN);
        writeFlowToConfig(flowRspId, sffNodeName, transportIngressFlow);
    }

    @Override
    public void configureVxlanGpeTransportIngressFlow(final String sffNodeName) {
        FlowBuilder transportIngressFlow =
                configureTransportIngressFlow(
                        SfcOpenflowUtils.ETHERTYPE_IPV4,
                        (short) -1,
                        getTableId(TABLE_INDEX_NEXT_HOP));
        writeFlowToConfig(flowRspId, sffNodeName, transportIngressFlow);
    }

    @Override
    public void configureMplsTransportIngressFlow(final String sffNodeName) {

        FlowBuilder transportIngressFlow =
                configureTransportIngressFlow(SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        writeFlowToConfig(flowRspId, sffNodeName, transportIngressFlow);
    }

    // Simple pass through with default args for ipProtocol and nextTable
    private FlowBuilder configureTransportIngressFlow(long etherType) {
        return configureTransportIngressFlow(etherType, (short) -1, getTableId(TABLE_INDEX_PATH_MAPPER));
    }

    // Simple pass through with default args for nextTable
    private FlowBuilder configureTransportIngressFlow(long etherType, short ipProtocol) {
        return configureTransportIngressFlow(etherType, ipProtocol, getTableId(TABLE_INDEX_PATH_MAPPER));
    }

    private FlowBuilder configureTransportIngressFlow(long etherType, short ipProtocol, short nextTable) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow, etherType [{}] ipProtocol [{}]",
                etherType, ipProtocol);

        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();
        if (ipProtocol > 0) {
            SfcOpenflowUtils.addMatchIpProtocol(match, ipProtocol);
        }

        if (etherType == SfcOpenflowUtils.ETHERTYPE_VLAN) {
            // vlan match
            // For some reason it didnt match setting etherType=0x8100
            VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
            VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
            vlanIdBuilder.setVlanIdPresent(true);
            vlanBuilder.setVlanId(vlanIdBuilder.build());
            match.setVlanMatch(vlanBuilder.build());
        } else {
            SfcOpenflowUtils.addMatchEtherType(match, etherType);
        }

        // Action, goto the nextTable
        GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(nextTable);

        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());
        ib.setKey(new InstructionKey(1));
        ib.setOrder(0);
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(ib);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_INGRESS_TRANSPORT_TABLE),
                FLOW_PRIORITY_TRANSPORT_INGRESS,
                "ingress_Transport_Default_Flow", match, isb);
    }

    // Create the ARP flow
    @Override
    public void configureArpTransportIngressFlow(final String sffNodeName, final String mac) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportArpIngressThread, sff [{}] mac [{}]",
                sffNodeName, mac);

        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_ARP);
        SfcOpenflowUtils.addMatchArpRequest(match);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveEthSrcToEthDstAction(order++));
        actionList.add(SfcOpenflowUtils.createActionSetDlSrc(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpOpAction(SfcOpenflowUtils.ARP_REPLY, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpShaAction(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpShaToArpThaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpTpaToRegAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpSpaToArpTpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveRegToArpSpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++));

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        int ibOrder = 0;
        InstructionBuilder actionsIb = new InstructionBuilder();
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(ibOrder));
        actionsIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(actionsIb);

        // Create and configure the FlowBuilder
        FlowBuilder arpTransportIngressFlow =
                SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_INGRESS_TRANSPORT_TABLE),
                        FLOW_PRIORITY_ARP_TRANSPORT_INGRESS,
                        "ingress_Transport_Default_Flow",
                        match, isb);

        writeFlowToConfig(flowRspId, sffNodeName, arpTransportIngressFlow);
    }

    //
    // Configure Table 2, PathMapper
    //

    @Override
    public void configureMplsPathMapperFlow(final String sffNodeName, final long mplsLabel, long pathId, boolean isSf) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMplsLabel(match, mplsLabel);

        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPopMpls(0));

        FlowBuilder pathMapperFlow;
        if(isSf) {
            pathMapperFlow = configurePathMapperSfFlow(pathId, match, actionList);
        } else {
            pathMapperFlow = configurePathMapperFlow(pathId, match, actionList);
        }
        writeFlowToConfig(flowRspId, sffNodeName, pathMapperFlow);
    }

    @Override
    public void configureVlanPathMapperFlow(final String sffNodeName, final int vlan, long pathId, boolean isSf) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchVlan(match, vlan);

        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPopVlan(0));

        FlowBuilder pathMapperFlow;
        if(isSf) {
            pathMapperFlow = configurePathMapperSfFlow(pathId, match, actionList);
        } else {
            pathMapperFlow = configurePathMapperFlow(pathId, match, actionList);
        }
        writeFlowToConfig(flowRspId, sffNodeName, pathMapperFlow);
    }

    // Simple pass through for SF pathMapper flows
    private FlowBuilder configurePathMapperSfFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        SfcOpenflowUtils.addMatchDscp(match, (short) pathId);
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER+10);
    }

    // Simple pass through with default arg for flowPriority
    private FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER);
    }

    private FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList, int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.configurePathMapperFlow sff [{}] pathId [{}]",
                pathId);

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();

        int ibOrder = 0;
        int actionOrder = 0;
        InstructionBuilder metadataIb = new InstructionBuilder();
        metadataIb.setInstruction(
                SfcOpenflowUtils.createInstructionMetadata(
                        actionOrder++,
                        getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH));
        metadataIb.setKey(new InstructionKey(ibOrder));
        metadataIb.setOrder(ibOrder++);
        instructions.add(metadataIb.build());

        InstructionBuilder actionsIb = new InstructionBuilder();
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(ibOrder));
        actionsIb.setOrder(ibOrder++);

        GoToTableBuilder gotoNextHop = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_NEXT_HOP));
        InstructionBuilder gotoNextHopIb = new InstructionBuilder();
        gotoNextHopIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoNextHop.build()).build());
        gotoNextHopIb.setKey(new InstructionKey(ibOrder));
        gotoNextHopIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        instructions.add(actionsIb.build());
        instructions.add(gotoNextHopIb.build());
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_PATH_MAPPER),
                flowPriority,
                "nextHop", match, isb);
    }

    //
    // Table 3, PathMapper ACL
    // This table is populated as a result of PktIn for TCP Proxy SFs.
    // The Src/Dst IP will be used to map the path ID
    //

    public void configurePathMapperAclFlow(final String sffNodeName, final String pktSrcIpStr, final String pktDstIpStr,
            short pathId) {
        LOG.debug(
                "SfcProviderSffFlowWriter.configurePathMapperAclFlow sff [{}] srcIp [{}] dstIp [{}] pathId [{}]",
                sffNodeName, pktSrcIpStr, pktDstIpStr, pathId);

        //
        // Match on the Src and Dst IPs
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchSrcIpv4(match, pktSrcIpStr, 32);
        SfcOpenflowUtils.addMatchDstIpv4(match, pktDstIpStr, 32);

        // Set the PathId in the metadata and goto the TransportEgress table
        int ibOrder = 0;
        InstructionBuilder metadataIb = new InstructionBuilder();
        metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(ibOrder,
                getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH));
        metadataIb.setKey(new InstructionKey(ibOrder));
        metadataIb.setOrder(ibOrder++);

        GoToTableBuilder gotoNextHop = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_NEXT_HOP));
        InstructionBuilder gotoNextHopIb = new InstructionBuilder();
        gotoNextHopIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoNextHop.build()).build());
        gotoNextHopIb.setKey(new InstructionKey(ibOrder));
        gotoNextHopIb.setOrder(ibOrder++);

        // Put our Instruction in a list of Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(metadataIb.build());
        instructions.add(gotoNextHopIb.build());

        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        FlowBuilder ingressFlow = SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                FLOW_PRIORITY_PATH_MAPPER_ACL,
                "nextHop",
                match, isb);
        // Set an idle timeout on this flow
        ingressFlow.setIdleTimeout(PKTIN_IDLE_TIMEOUT);

        writeFlowToConfig(flowRspId, sffNodeName, ingressFlow);
    }

    //
    // Table 4, NextHop
    //

    @Override
    public void configureMacNextHopFlow(final String sffNodeName, final long pathId, final String srcMac,
            final String dstMac) {
        int flowPriority = FLOW_PRIORITY_NEXT_HOP;
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        if (srcMac != null) {
            SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
        } else {
            // If the srcMac is null, then the packet is entering SFC and we dont know
            // from where. Make it a lower priority, and only match on the pathId
            flowPriority -= 10;
        }

        List<Action> actionList = new ArrayList<Action>();
        if (dstMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            actionList.add(SfcOpenflowUtils.createActionSetDlDst(dstMac, 0));
        }

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, flowPriority);
        writeFlowToConfig(flowRspId, sffNodeName, nextHopFlow);
    }

    @Override
    public void configureVxlanGpeNextHopFlow(final String sffNodeName, final String dstIp, final long nshNsp,
            final short nshNsi) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        if (dstIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);
            actionList.add(actionSetNwDst);
        }

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList);
        writeFlowToConfig(flowRspId, sffNodeName, nextHopFlow);
    }

    // Simple pass through with default arg for flowPriority
    private FlowBuilder configureNextHopFlow(MatchBuilder match, List<Action> actionList) {
        return configureNextHopFlow(match, actionList, FLOW_PRIORITY_NEXT_HOP);
    }

    private FlowBuilder configureNextHopFlow(MatchBuilder match, List<Action> actionList, int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.configureNextHopFlow");

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        GoToTableBuilder gotoTb = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_TRANSPORT_EGRESS));

        InstructionBuilder gotoTbIb = new InstructionBuilder();
        gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
        gotoTbIb.setKey(new InstructionKey(1));
        gotoTbIb.setOrder(1);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setKey(new InstructionKey(0));
        ib.setOrder(0);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        instructions.add(gotoTbIb.build());
        isb.setInstruction(instructions);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_NEXT_HOP),
                flowPriority, "nextHop", match, isb);
    }


    //
    // Table 10, Transport Egress
    //

    @Override
    public void configureVlanTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId, boolean setDscp, final boolean doPktIn) {
        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPushVlan(order++));
        actionList.add(SfcOpenflowUtils.createActionSetVlanId(dstVlan, order++));

        FlowBuilder transportEgressFlow =
                configureTransportEgressFlow(match, actionList, port, order, pathId, srcMac, dstMac, setDscp, doPktIn);
        writeFlowToConfig(flowRspId, sffNodeName, transportEgressFlow);
    }

    @Override
    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId, boolean setDscp, final boolean doPktIn) {
        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(SfcOpenflowUtils.createActionPushMpls(order++));
        actionList.add(SfcOpenflowUtils.createActionSetMplsLabel(mplsLabel, order++));

        FlowBuilder transportEgressFlow =
                configureTransportEgressFlow(match, actionList, port, order, pathId, srcMac, dstMac, setDscp, doPktIn);
        writeFlowToConfig(flowRspId, sffNodeName, transportEgressFlow);
    }

    @Override
    public void configureVxlanGpeTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port, final boolean isLastHop) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);

        int order = 0;
        List<Action> actionList = new ArrayList<Action>();
        if (isLastHop) {
            // On the last hop Copy/Move Nsi, Nsp, Nsc1=>TunIpv4Dst, and Nsc2=>TunId
            // (Vnid)
            actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(order++));
            actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(order++));
            actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(order++));
            actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(order++));
        } else {
            // If its not the last hop, Copy/Move Nsc1/Nsc2 to the next hop
            actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1(order++));
            actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2(order++));
            actionList.add(SfcOpenflowUtils.createActionNxMoveTunIdRegister(order++));
        }

        FlowBuilder transportEgressFlow =
                configureTransportEgressFlow(match, actionList, port, order);
        writeFlowToConfig(flowRspId, sffNodeName, transportEgressFlow);
    }

    // Simple pass through with logic for src/dstMac
    private FlowBuilder configureTransportEgressFlow(MatchBuilder match, List<Action> actionList,
            String port, int order, final long pathId, final String srcMac, final String dstMac,
            boolean setDscp, final boolean doPktIn) {

        //Optionally match on the dstMac
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        if (dstMac != null) {
            SfcOpenflowUtils.addMatchDstMac(match, dstMac);
        } else {
            // If the dstMac is null, then the packet is leaving SFC and we dont know
            // to where. Make it a lower priority, and only match on the pathId
            flowPriority -= 10;
        }

        if (doPktIn) {
            // Notice TCP SYN matching is only supported in OpenFlow 1.5
            SfcOpenflowUtils.addMatchTcpSyn(match);
            actionList.add(SfcOpenflowUtils.createActionPktIn(SfcOpenflowUtils.PKT_LENGTH_IP_HEADER, order++));
        }

        // Optionally write the DSCP with the pathId
        if (setDscp) {
            // In order to set the IP DSCP, we need to match IPv4
            SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
            actionList.add(SfcOpenflowUtils.createActionWriteDscp((short) pathId, order++));
        }

        // Set the macSrc
        if (srcMac != null) {
            actionList.add(SfcOpenflowUtils.createActionSetDlSrc(srcMac, order++));
        }

        return configureTransportEgressFlow(match, actionList, port, order, flowPriority);
    }

    // Simple pass through with default arg for flowPriority
    private FlowBuilder configureTransportEgressFlow(MatchBuilder match, List<Action> actionList, String port, int order) {
        return configureTransportEgressFlow(match, actionList, port, order, FLOW_PRIORITY_TRANSPORT_EGRESS);
    }

    private FlowBuilder configureTransportEgressFlow(MatchBuilder match, List<Action> actionList, String port, int order, int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportEgressFlow");

        actionList.add(SfcOpenflowUtils.createActionOutPort(port, order++));

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(ib);

        return SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                flowPriority,
                TRANSPORT_EGRESS_COOKIE,
                "default_egress_flow", match, isb);
    }

    // For NSH, Return the packet to INPORT if the NSH Nsc1 Register is not present (==0)
    // If it is present, it will be handled in ConfigureTransportEgressFlowThread()
    // This flow will have a higher priority than the flow created in
    // ConfigureTransportEgressFlowThread()
    @Override
    public void configureNshNscTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi, String port) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureNshNscTransportEgressFlowThread, sff [{}]",
                sffNodeName);

        // Match any
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchNshNsp(match, nshNsp);
        SfcOpenflowUtils.addMatchNshNsi(match, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, 0l);

        // Create the actions
        int order = 0;
        Action outPortBuilder = SfcOpenflowUtils.createActionOutPort(port, order++);

        List<Action> actionList = new ArrayList<Action>();
        actionList.add(outPortBuilder);

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(ib);

        // Create and configure the FlowBuilder
        FlowBuilder transportIngressFlow =
                SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                        FLOW_PRIORITY_TRANSPORT_EGRESS + 10,
                        "nsh_nsc_egress_flow",
                        match, isb);

        writeFlowToConfig(flowRspId, sffNodeName, transportIngressFlow);
    }

    @Override
    public void configureGroup(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
            List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {

        LOG.debug("configuring group: sffName {}, groupName {}, ofNodeId {}, id {}, type {}",
                sffNodeName, sfgName, openflowNodeId, sfgId, groupType);
        GroupBuilder gb = new GroupBuilder();
        BucketsBuilder bbs = new BucketsBuilder();
        gb.setBarrier(true);
        gb.setGroupType(GroupTypes.forValue(groupType));
        gb.setGroupName(sfgName);
        gb.setGroupId(new GroupId(sfgId));

        List<Bucket> buckets = new ArrayList<Bucket>();
        BucketBuilder bb = new BucketBuilder();
        for (GroupBucketInfo bucketInfo : bucketInfos) {
            LOG.debug("building bucket {}", bucketInfo);
            buckets.add(buildBucket(bb, bucketInfo));
        }
        bbs.setBucket(buckets);
        gb.setBuckets(bbs.build());
        String nodeName = openflowNodeId != null ? openflowNodeId : sffNodeName;
        writeGroupToDataStore(nodeName, gb, isAddGroup);
    }

    private Bucket buildBucket(BucketBuilder bb, GroupBucketInfo bucketInfo) {
        int order = 0;
        BucketId bucketId = new BucketId((long) bucketInfo.getIndex());
        bb.setBucketId(bucketId);
        bb.setKey(new BucketKey(bucketId));
        String sfMac = bucketInfo.getSfMac();
        String sfIp = bucketInfo.getSfIp();
        List<Action> actionList = new ArrayList<Action>();
        if (sfMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            Action actionDstMac = SfcOpenflowUtils.createActionSetDlDst(sfMac, order);
            order++;
            actionList.add(actionDstMac);
        }

        if (sfIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionSetNwDst(sfIp, 32, order);
            order++;
            actionList.add(actionSetNwDst);
        }

        Action actionOutPort = SfcOpenflowUtils.createActionOutPort(bucketInfo.getOutPort(), order);
        actionList.add(actionOutPort);

        bb.setAction(actionList);
        return bb.build();
    }

    @Override
    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId,
            String groupName) {
        LOG.debug(
                "SfcProviderSffFlowWriter.ConfigureGroupNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] groupId[{}]",
                sffNodeName, sfpId, srcMac, groupId);

        int flowPriority = FLOW_PRIORITY_NEXT_HOP;

        //
        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();

        // Match on the either the metadata sfpId or the NSH NSP and NSI
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);

        // match on the src mac
        if (srcMac != null) {
            SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
        } else {
            // If the srcMac is null, then the packet is entering SFC and we dont know
            // from where. Make it a lower priority, and only match on the pathId
            flowPriority -= 10;
        }

        //
        // Create the Actions
        List<Action> actionList = new ArrayList<Action>();

        GroupActionBuilder groupActionBuilder = new GroupActionBuilder();
        groupActionBuilder.setGroupId(groupId);
        groupActionBuilder.setGroup(groupName);

        GroupActionCaseBuilder groupActionCaseBuilder = new GroupActionCaseBuilder();
        groupActionCaseBuilder.setGroupAction(groupActionBuilder.build());

        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setAction(groupActionCaseBuilder.build());
        actionBuilder.setOrder(0);
        actionBuilder.setKey(new ActionKey(0));
        Action groupAction = actionBuilder.build();

        actionList.add(groupAction);

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setKey(new InstructionKey(0));
        ib.setOrder(0);

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);

        //
        // Create and configure the FlowBuilder
        FlowBuilder nextHopFlow =
                SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_NEXT_HOP),
                        flowPriority, "nextHop", match, isb);
        writeFlowToConfig(flowRspId, sffNodeName, nextHopFlow);
    }

    private void writeGroupToDataStore(String sffNodeName, GroupBuilder gb, boolean isAdd) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        GroupKey gk = new GroupKey(gb.getGroupId());
        InstanceIdentifier<Group> groupIID;

        groupIID = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Group.class, gk)
            .build();
        Group group = gb.build();
        LOG.debug("about to write group to data store \nID: {}\nGroup: {}", groupIID, group);
        if (isAdd) {
            if (!SfcDataStoreAPI.writeMergeTransactionAPI(groupIID, group, LogicalDatastoreType.CONFIGURATION)) {
                LOG.warn("Failed to write group to data store");
            }
        } else {
            if (!SfcDataStoreAPI.deleteTransactionAPI(groupIID, LogicalDatastoreType.CONFIGURATION)) {
                LOG.warn("Failed to remove group from data store");
            }
        }
    }

    /**
     * Write the flows in a separate thread
     *
     * @author ebrjohn
     */
    class FlowWriterThread implements Runnable {
        String sffNodeName;
        InstanceIdentifier<Flow> flowInstanceId;
        FlowBuilder flowBuilder;

        public FlowWriterThread(String sffNodeName, InstanceIdentifier<Flow> flowInstanceId, FlowBuilder flowBuilder) {
            this.sffNodeName = sffNodeName;
            this.flowInstanceId = flowInstanceId;
            this.flowBuilder = flowBuilder;
        }

        public void run(){
            if (!SfcDataStoreAPI.writeMergeTransactionAPI(
                    this.flowInstanceId,
                    this.flowBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION)) {
                LOG.error("{}: Failed to create Flow on node: {}", Thread.currentThread().getStackTrace()[1], this.sffNodeName);
            }
        }
    }

    /**
     * Remove the flows in a separate thread
     *
     * @author ebrjohn
     */
    class FlowRemoverThread implements Runnable {
        String sffNodeName;
        InstanceIdentifier<Flow> flowInstanceId;

        public FlowRemoverThread(String sffNodeName, InstanceIdentifier<Flow> flowInstanceId) {
            this.flowInstanceId = flowInstanceId;
            this.sffNodeName = sffNodeName;
        }

        public void run(){
            if (!SfcDataStoreAPI.deleteTransactionAPI(flowInstanceId, LogicalDatastoreType.CONFIGURATION)) {
                LOG.error("{}: Failed to remove Flow on node: {}", Thread.currentThread().getStackTrace()[1], sffNodeName);
            }
        }
    }

    /**
     * Remove a Flow from the DataStore
     *
     * @param sffNodeName - which SFF the flow is in
     * @param flowKey - The flow key of the flow to be removed
     * @param tableKey - The table the flow was written to
     */
    private void removeFlowFromConfig(final String sffNodeName, FlowKey flowKey, TableKey tableKey) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Table.class, tableKey)
            .child(Flow.class, flowKey)
            .build();

        FlowRemoverThread removerThread = new FlowRemoverThread(sffNodeName, flowInstanceId);
        try {
            threadPoolExecutorService.execute(removerThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    /**
     * Write a flow to the DataStore
     *
     * @param sffNodeName - which SFF to write the flow to
     * @param flow - details of the flow to be written
     */
    private void writeFlowToConfig(final Long rspId, final String sffNodeName, FlowBuilder flow) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path, which will include the Node, Table, and Flow
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(flow.getTableId()))
            .child(Flow.class, flow.getKey())
            .build();

        LOG.debug("writeFlowToConfig writing flow to Node {}, table {}", sffNodeName, flow.getTableId());

        storeFlowDetails(rspId, sffNodeName, flow.getKey(), flow.getTableId());

        FlowWriterThread writerThread = new FlowWriterThread(sffNodeName, flowInstanceId, flow);
        try {
            threadPoolExecutorService.execute(writerThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private static BigInteger getMetadataSFP(long sfpId) {
        return (BigInteger.valueOf(sfpId).and(new BigInteger("FFFF", COOKIE_BIGINT_HEX_RADIX)));
    }

    /**
     * storeFlowDetails
     * Store the flow details so the flows are easy to delete later
     *
     * @param sffNodeName - the SFF the flow is written to
     * @param flowKey - the flow key of the new flow
     * @param tableId - the table the flow was written to
     */
    private void storeFlowDetails(final Long rspId, final String sffNodeName, FlowKey flowKey, short tableId) {
        List<FlowDetails> flowDetails = rspNameToFlowsMap.get(rspId);
        if (flowDetails == null) {
            flowDetails = new ArrayList<FlowDetails>();
            rspNameToFlowsMap.put(rspId, flowDetails);
        }
        flowDetails.add(new FlowDetails(sffNodeName, flowKey, new TableKey(tableId)));
    }

    /**
     * getTableId
     * Having a TableBase allows us to "offset" the SFF tables by this.tableBase
     * tables Doing so allows for OFS tables previous to the SFF tables.
     * tableIndex should be one of: TABLE_INDEX_SFF_ACL or
     * TABLE_INDEX_SFF_OUTPUT
     */
    private short getTableId(short tableIndex) {
        return (short) (tableBase + tableIndex);
    }
}

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
import java.util.HashSet;
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
import org.opendaylight.sfc.l2renderer.SfcL2FlowProgrammerInterface;
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
    }

    @Override
    public Set<NodeId> clearSffsIfNoRspExists() {
        // If there is just one entry left in the rsp-flows mapping, then all flows for RSPs
        // have been deleted, and the only flows remaining are those that are common to all
        // RSPs, which can be deleted.
        Set<NodeId> sffNodeIDs = new HashSet<>();
        if (rspNameToFlowsMap.size() == 1) {
            Set<Entry<Long, List<FlowDetails>>> entries = rspNameToFlowsMap.entrySet();
            List<FlowDetails> flowDetailsList = entries.iterator().next().getValue();
            for (FlowDetails flowDetails : flowDetailsList) {
                removeFlowFromConfig(flowDetails.sffNodeName, flowDetails.flowKey, flowDetails.tableKey);
                sffNodeIDs.add(new NodeId(flowDetails.sffNodeName));
            }
            rspNameToFlowsMap.clear();
        }
        return sffNodeIDs;
    }

    @Override
    public void configureClassifierTableMatchAny(final String sffNodeName, final boolean doDrop) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        TABLE_INDEX_CLASSIFIER_TABLE,
                        TABLE_INDEX_INGRESS_TRANSPORT_TABLE,
                        doDrop);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    //
    // Configure the MatchAny entry specifying if it should drop or goto the next table
    // If doDrop == False
    // TransportIngress MatchAny will go to Ingress
    // Ingress MatchAny will go to Acl
    // Acl MatchAny will go to NextHop
    // NextHop MatchAny will go to TransportEgress
    //
    @Override
    public void configureTransportIngressTableMatchAny(final String sffNodeName, final boolean doDrop) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                   new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        TABLE_INDEX_INGRESS_TRANSPORT_TABLE,
                        TABLE_INDEX_PATH_MAPPER,
                        doDrop);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configurePathMapperTableMatchAny(final String sffNodeName, final boolean doDrop) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        TABLE_INDEX_PATH_MAPPER,
                        TABLE_INDEX_PATH_MAPPER_ACL,
                        doDrop);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configurePathMapperAclTableMatchAny(final String sffNodeName, final boolean doDrop) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        TABLE_INDEX_PATH_MAPPER_ACL,
                        TABLE_INDEX_NEXT_HOP,
                        doDrop);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureNextHopTableMatchAny(final String sffNodeName, final boolean doDrop) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(sffNodeName,
                        TABLE_INDEX_NEXT_HOP,
                        TABLE_INDEX_TRANSPORT_EGRESS,
                        doDrop);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureTransportEgressTableMatchAny(final String sffNodeName, final boolean doDrop) {
        // This is the last table, cant set next table AND doDrop should be false
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        TABLE_INDEX_TRANSPORT_EGRESS,
                        (short) -1,
                        doDrop);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    /**
     * Configure MatchAny rules for the different tables used. When passing
     * a tableIndex, it will be converted to the correct table internally.
     *
     * @author ebrjohn
     *
     */
    private class ConfigureTableMatchAnyThread implements Runnable {

        private String sffNodeName;
        private boolean doDrop;
        private short tableIdIndex;
        private short nextTableIdIndex;
        private Long rspId;

        public ConfigureTableMatchAnyThread(final String sffNodeName, final short tableIdIndex, final short nextTableIdIndex,
                final boolean doDrop) {
            this.sffNodeName = sffNodeName;
            this.tableIdIndex = tableIdIndex;
            this.nextTableIdIndex = nextTableIdIndex;
            this.doDrop = doDrop;
            this.rspId = flowRspId;
        }

        @Override
        public void run() {
            try {
                LOG.debug(
                        "SfcProviderSffFlowWriter.ConfigureTableMatchAnyThread, sff [{}] tableIndex [{}] nextTableIndex [{}] doDrop {}",
                        this.sffNodeName, this.tableIdIndex, this.nextTableIdIndex, this.doDrop);

                //
                // Create the actions
                List<Instruction> instructions = new ArrayList<Instruction>();
                int order = 0;

                if (this.doDrop) {
                    List<Action> actionList = new ArrayList<Action>();
                    ApplyActionsBuilder aab = new ApplyActionsBuilder();

                    // Add our drop action to a list
                    actionList.add(SfcOpenflowUtils.createActionDropPacket(0));

                    // Create an Apply Action
                    aab.setAction(actionList);

                    // Wrap our Apply Action in an Instruction
                    InstructionBuilder ib = new InstructionBuilder();
                    ib.setKey(new InstructionKey(order));
                    ib.setOrder(order++);
                    ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                    instructions.add(ib.build());
                } else {
                    //
                    // Action, goto Ingress table
                    GoToTableBuilder gotoIngress =
                            SfcOpenflowUtils.createActionGotoTable(
                                    getTableId(this.nextTableIdIndex));

                    InstructionBuilder ib = new InstructionBuilder();
                    ib.setKey(new InstructionKey(order));
                    ib.setOrder(order++);
                    ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());
                    instructions.add(ib.build());
                }

                //
                // Match any
                MatchBuilder match = new MatchBuilder();

                //
                // Finish up the instructions
                InstructionsBuilder isb = new InstructionsBuilder();
                isb.setInstruction(instructions);

                //
                // Create and configure the FlowBuilder
                FlowBuilder transportIngressFlow = SfcOpenflowUtils.createFlowBuilder(
                        getTableId(this.tableIdIndex),
                        FLOW_PRIORITY_MATCH_ANY,
                        "MatchAny",
                        match,
                        isb);

                writeFlowToConfig(rspId, sffNodeName, transportIngressFlow);

            } catch (Exception e) {
                LOG.error("ConfigureTableMatchAnyThread writer caught an Exception: ", e);
            }
        }
    }

    //
    // Congfigure Table 0, Transport Ingress
    //
    @Override
    public void configureIpv4TransportIngressFlow(final String sffNodeName) {
        ConfigureTransportIngressThread configureIngressTransportTcpThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_IPV4);
        configureIngressTransportTcpThread.setIpProtocol(SfcOpenflowUtils.IP_PROTOCOL_TCP);

        ConfigureTransportIngressThread configureIngressTransportUdpThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_IPV4);
        configureIngressTransportUdpThread.setIpProtocol(SfcOpenflowUtils.IP_PROTOCOL_UDP);

        try {
            threadPoolExecutorService.execute(configureIngressTransportTcpThread);
            threadPoolExecutorService.execute(configureIngressTransportUdpThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureVlanTransportIngressFlow(final String sffNodeName) {
        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_VLAN);
        try {
            threadPoolExecutorService.execute(configureIngressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureVxlanGpeTransportIngressFlow(final String sffNodeName) {
        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_IPV4);
        configureIngressTransportThread.setNextTableIndex(TABLE_INDEX_NEXT_HOP);
        try {
            threadPoolExecutorService.execute(configureIngressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureMplsTransportIngressFlow(final String sffNodeName) {

        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        try {
            threadPoolExecutorService.execute(configureIngressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureTransportIngressThread implements Runnable {

        String sffNodeName;
        long etherType;
        short ipProtocol;
        short nextTableIndex;
        Long rspId;

        public ConfigureTransportIngressThread(final String sffNodeName, long etherType) {
            this.sffNodeName = sffNodeName;
            this.etherType = etherType;
            this.ipProtocol = (short) -1;
            this.nextTableIndex = TABLE_INDEX_PATH_MAPPER;
            this.rspId = flowRspId;
        }

        public void setIpProtocol(short ipProtocol) {
            this.ipProtocol = ipProtocol;
        }

        public void setNextTableIndex(short nextTableIndex) {
            this.nextTableIndex = nextTableIndex;
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow, sff [{}] etherType [{}]",
                        this.sffNodeName, this.etherType);

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();
                if (this.ipProtocol > 0) {
                    SfcOpenflowUtils.addMatchIpProtocol(match, this.ipProtocol);
                }

                if (this.etherType == SfcOpenflowUtils.ETHERTYPE_VLAN) {
                    // vlan match
                    // For some reason it didnt match setting etherType=0x8100
                    VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
                    VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
                    vlanIdBuilder.setVlanIdPresent(true);
                    vlanBuilder.setVlanId(vlanIdBuilder.build());
                    match.setVlanMatch(vlanBuilder.build());
                } else {
                    SfcOpenflowUtils.addMatchEtherType(match, this.etherType);
                }

                //
                // Action, goto the nextTable, defaults to Ingress table unless otherwise set
                GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(getTableId(this.nextTableIndex));

                InstructionBuilder ib = new InstructionBuilder();
                ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());
                ib.setKey(new InstructionKey(1));
                ib.setOrder(0);

                // Put our Instruction in a list of Instructions
                InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(ib);

                //
                // Create and configure the FlowBuilder
                FlowBuilder transportIngressFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                getTableId(TABLE_INDEX_INGRESS_TRANSPORT_TABLE),
                                FLOW_PRIORITY_TRANSPORT_INGRESS,
                                "ingress_Transport_Default_Flow",
                                match,
                                isb);

                writeFlowToConfig(rspId, sffNodeName, transportIngressFlow);

            } catch (Exception e) {
                LOG.error("ConfigureTransportIngress writer caught an Exception: ", e);
            }
        }
    }

    // Thread to create ARP flows
    @Override
    public void configureArpTransportIngressFlow(final String sffNodeName, final String mac) {

        ConfigureTransportArpIngressThread configureArpIngressTransportThread =
                new ConfigureTransportArpIngressThread(sffNodeName, mac);
        try {
            threadPoolExecutorService.execute(configureArpIngressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureTransportArpIngressThread implements Runnable {

        String sffNodeName;
        String mac;
        Long rspId;

        public ConfigureTransportArpIngressThread(final String sffNodeName, final String mac) {
            this.sffNodeName = sffNodeName;
            this.mac = mac;
            this.rspId = flowRspId;
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportArpIngressThread, sff [{}] mac [{}]",
                        this.sffNodeName, this.mac);

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
                FlowBuilder transportIngressFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                getTableId(TABLE_INDEX_INGRESS_TRANSPORT_TABLE),
                                FLOW_PRIORITY_ARP_TRANSPORT_INGRESS,
                                "ingress_Transport_Default_Flow",
                                match,
                                isb);

                writeFlowToConfig(rspId, sffNodeName, transportIngressFlow);

            } catch (Exception e) {
                LOG.error("ConfigureTransportArpIngress writer caught an Exception: ", e);
            }
        }
    }

    //
    // Configure Table 1, PathMapper
    //
    @Override
    public void configureMacPathMapperFlow(final String sffNodeName, final String mac, long pathId, boolean isSf) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, isSf, pathId);
        configurePathMapperFlowThread.setMacAddress(mac);
        try {
            threadPoolExecutorService.execute(configurePathMapperFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureMplsPathMapperFlow(final String sffNodeName, final long label, long pathId, boolean isSf) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, isSf, pathId);
        configurePathMapperFlowThread.setMplsLabel(label);
        try {
            threadPoolExecutorService.execute(configurePathMapperFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureVlanPathMapperFlow(final String sffNodeName, final int vlan, long pathId, boolean isSf) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, isSf, pathId);
        configurePathMapperFlowThread.setVlanId(vlan);
        try {
            threadPoolExecutorService.execute(configurePathMapperFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureVxlanGpePathMapperFlow(final String sffNodeName, long nsp, short nsi, long pathId) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, false, pathId);
        configurePathMapperFlowThread.setNsp(nsp);
        configurePathMapperFlowThread.setNsi(nsi);
        try {
            threadPoolExecutorService.execute(configurePathMapperFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigurePathMapperFlowThread implements Runnable {

        String sffNodeName;
        long pathId;
        int vlan;
        long nsp;
        short nsi;
        long mplsLabel;
        String macAddress;
        boolean isSf;
        Long rspId;

        public void setVlanId(final int vlan) {
            this.vlan = vlan;
        }

        public void setNsp(final long nsp) {
            this.nsp = nsp;
        }

        public void setNsi(final short nsi) {
            this.nsi = nsi;
        }

        public void setMplsLabel(final long label) {
            this.mplsLabel = label;
        }

        public void setMacAddress(final String macAddress) {
            this.macAddress = macAddress;
        }

        public ConfigurePathMapperFlowThread(final String sffNodeName, final boolean isSf, final long pathId) {
            this.sffNodeName = sffNodeName;
            this.pathId = pathId;
            this.vlan = -1; // not set
            this.nsp = -1; // not set
            this.nsi = -1; // not set
            this.mplsLabel = -1; // not set
            this.isSf = isSf;
            this.rspId = flowRspId;
        }

        @Override
        public void run() {
            try {
                LOG.debug(
                        "SfcProviderSffFlowWriter.configurePathMapperFlow sff [{}] pathId [{}] vlan [{}] mpls [{}] mac [{}]",
                        this.sffNodeName, this.pathId, this.vlan, this.mplsLabel, this.macAddress);

                MatchBuilder match = new MatchBuilder();
                List<Action> actionList = new ArrayList<Action>();
                int actionOrder = 0;
                int flowPriority = FLOW_PRIORITY_PATH_MAPPER;

                if (this.isSf) {
                    flowPriority += 10;
                    SfcOpenflowUtils.addMatchDscp(match, (short) this.pathId);
                }

                if (this.vlan >= 0) {
                    SfcOpenflowUtils.addMatchVlan(match, this.vlan);
                    actionList.add(SfcOpenflowUtils.createActionPopVlan(actionOrder++));
                } else if (this.mplsLabel >= 0) {
                    SfcOpenflowUtils.addMatchMplsLabel(match, this.mplsLabel);
                    actionList.add(SfcOpenflowUtils.createActionPopMpls(actionOrder++));
                } else if (this.macAddress.length() > 0) {
                    SfcOpenflowUtils.addMatchSrcMac(match, this.macAddress);
                } else if (this.nsp >= 0 && this.nsi >= 0) {
                    // VxLAN-gpe + NSH
                    // TODO if the nsi is 0, drop the packet
                    SfcOpenflowUtils.addMatchNshNsp(match, this.nsp);
                    SfcOpenflowUtils.addMatchNshNsi(match, this.nsi);
                }

                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();

                int ibOrder = 0;
                InstructionBuilder metadataIb = new InstructionBuilder();
                metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(actionOrder++,
                        getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH));
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

                //
                // Create and configure the FlowBuilder
                FlowBuilder ingressFlow = SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_PATH_MAPPER),
                        flowPriority,
                        "nextHop",
                        match,
                        isb);

                writeFlowToConfig(rspId, sffNodeName, ingressFlow);

            } catch (Exception e) {
                LOG.error("ConfigurePathMapperFlow writer caught an Exception: ", e);
            }
        }
    }

    //
    // Table 3, PathMapper ACL
    // This table is populated as a result of PktIn for TCP Proxy SFs.
    // The Src/Dst IP will be used to map the path ID
    //
    public void configurePathMapperAclFlow(final String sffNodeName, final String pktSrcIpStr, final String pktDstIpStr,
            short pathId) {
        ConfigurePathMapperAclFlowThread configurePathMapperAclFlowThread =
                new ConfigurePathMapperAclFlowThread(sffNodeName, pktSrcIpStr, pktDstIpStr, pathId);

        try {
            threadPoolExecutorService.execute(configurePathMapperAclFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigurePathMapperAclFlowThread implements Runnable {

        String sffNodeName;
        String srcIpStr;
        String dstIpStr;
        long pathId;
        Long rspId;

        public ConfigurePathMapperAclFlowThread(final String sffNodeName, final String srcIpStr, final String dstIpStr,
                short pathId) {
            this.sffNodeName = sffNodeName;
            this.pathId = pathId;
            this.srcIpStr = srcIpStr;
            this.dstIpStr = dstIpStr;
            this.rspId = flowRspId;
        }

        @Override
        public void run() {
            try {
                LOG.debug(
                        "SfcProviderSffFlowWriter.configurePathMapperAclFlow sff [{}] srcIp [{}] dstIp [{}] pathId [{}]",
                        this.sffNodeName, this.srcIpStr, this.dstIpStr, this.pathId);

                //
                // Match on the Src and Dst IPs
                MatchBuilder match = new MatchBuilder();
                SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
                SfcOpenflowUtils.addMatchSrcIpv4(match, this.srcIpStr, 32);
                SfcOpenflowUtils.addMatchDstIpv4(match, this.dstIpStr, 32);

                //
                // Set the PathId in the metadata and goto the TransportEgress table
                int ibOrder = 0;
                InstructionBuilder metadataIb = new InstructionBuilder();
                metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(ibOrder,
                        getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH));
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

                //
                // Create and configure the FlowBuilder
                FlowBuilder ingressFlow = SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                        FLOW_PRIORITY_PATH_MAPPER_ACL,
                        "nextHop",
                        match,
                        isb);
                // Set an idle timeout on this flow
                ingressFlow.setIdleTimeout(PKTIN_IDLE_TIMEOUT);

                writeFlowToConfig(rspId, sffNodeName, ingressFlow);

            } catch (Exception e) {
                LOG.error("configurePathMapperAclFlow writer caught an Exception: ", e);
            }
        }
    }

    //
    // Table 4, NextHop
    //
    @Override
    public void configureNextHopFlow(final String sffNodeName, final long sfpId, final String srcMac,
            final String dstMac) {

        ConfigureNextHopFlowThread configureNextHopFlowThread =
                new ConfigureNextHopFlowThread(sffNodeName, sfpId, srcMac, dstMac);
        try {
            threadPoolExecutorService.execute(configureNextHopFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureVxlanGpeNextHopFlow(final String sffNodeName, final String dstIp, final long nsp,
            final short nsi) {
        ConfigureNextHopFlowThread configureNextHopFlowThread =
                new ConfigureNextHopFlowThread(sffNodeName, nsp, null, null);
        configureNextHopFlowThread.setDstIp(dstIp);
        configureNextHopFlowThread.setNsp(nsp);
        configureNextHopFlowThread.setNsi(nsi);
        try {
            threadPoolExecutorService.execute(configureNextHopFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureNextHopFlowThread implements Runnable {

        Long rspId;
        String sffNodeName;
        long sfpId;
        String srcMac;
        String dstMac;
        String dstIp;
        long nshNsp;
        short nshNsi;

        public void setDstIp(final String dstIp) {
            this.dstIp = dstIp;
        }

        public void setNsp(final long nshNsp) {
            this.nshNsp = nshNsp;
        }

        public void setNsi(final short nshNsi) {
            this.nshNsi = nshNsi;
        }

        public ConfigureNextHopFlowThread(final String sffNodeName, final long sfpId, final String srcMac,
                final String dstMac) {
            super();
            this.rspId = flowRspId;
            this.sffNodeName = sffNodeName;
            this.sfpId = sfpId;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.nshNsi = -1; // unused
            this.nshNsp = -1; // unused
            this.dstIp = null; // unused
        }

        @Override
        public void run() {
            try {
                LOG.debug(
                        "SfcProviderSffFlowWriter.configureNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] dstMac [{}]",
                        this.sffNodeName, this.sfpId, this.srcMac, this.dstMac);

                int flowPriority = FLOW_PRIORITY_NEXT_HOP;

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();

                // Match on the either the metadata sfpId or the NSH NSP and NSI
                if (nshNsp >= 0 && nshNsi >= 0) {
                    SfcOpenflowUtils.addMatchNshNsp(match, this.nshNsp);
                    SfcOpenflowUtils.addMatchNshNsi(match, this.nshNsi);
                } else {
                    SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);
                }

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
                int order = 0;

                if (dstMac != null) {
                    // Set the DL (Data Link) Dest Mac Address
                    actionList.add(SfcOpenflowUtils.createActionSetDlDst(dstMac, order++));
                }

                if (dstIp != null) {
                    Action actionSetNwDst;
                    if (nshNsp >= 0 && nshNsi >= 0) {
                        // For NSH, we need to set the Tunnel Dst IP, not the inner Dst IP
                        actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);
                    } else {
                        // If we're going to set IP, then we first have
                        // to match IP or else the flow will be discarded
                        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
                        actionSetNwDst = SfcOpenflowUtils.createActionSetNwDst(dstIp, 32, order++);
                    }
                    actionList.add(actionSetNwDst);
                }

                // Create an Apply Action
                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                GoToTableBuilder gotoTb = SfcOpenflowUtils.createActionGotoTable(
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS));

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

                //
                // Create and configure the FlowBuilder
                FlowBuilder nextHopFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                getTableId(TABLE_INDEX_NEXT_HOP),
                                flowPriority,
                                "nextHop",
                                match,
                                isb);

                writeFlowToConfig(rspId, sffNodeName, nextHopFlow);

            } catch (Exception e) {
                LOG.error("ConfigureNextHopFlow writer caught an Exception: ", e);
            }
        }
    }

    //
    // Table 10, Transport Egress
    //
    @Override
    public void configureMacTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final String port, final long pathId, final boolean setDscp, final boolean isLastHop,
            final boolean doPktIn) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isLastHop);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureVlanTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId, boolean setDscp, final boolean isLastHop,
            final boolean doPktIn) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isLastHop);
        configureEgressTransportThread.setDstVlan(dstVlan);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureVxlanGpeTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port, final boolean isLastHop, final boolean doPktIn) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, null, null, port, nshNsp, false, isLastHop);
        configureEgressTransportThread.setNshNsp(nshNsp);
        configureEgressTransportThread.setNshNsi(nshNsi);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    @Override
    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId, boolean setDscp, final boolean isLastHop,
            final boolean doPktIn) {

        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isLastHop);
        configureEgressTransportThread.setMplsLabel(mplsLabel);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureTransportEgressThread implements Runnable {

        Long rspId;
        String sffNodeName;
        String srcMac;
        String dstMac;
        int dstVlan;
        long nshNsp;
        short nshNsi;
        long mplsLabel;
        String port;
        long pathId;
        boolean setDscp;
        boolean isLastHop;
        boolean doPktIn;

        public ConfigureTransportEgressThread(final String sffNodeName, String srcMac, String dstMac, String port,
                final long pathId, boolean setDscp, final boolean isLastHop) {
            super();
            this.rspId = flowRspId;
            this.sffNodeName = sffNodeName;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.dstVlan = -1; // unused
            this.nshNsp = -1; // unused
            this.nshNsi = -1; // unused
            this.mplsLabel = -1; // unused
            this.port = port;
            this.pathId = pathId;
            this.setDscp = setDscp;
            this.isLastHop = isLastHop;
            this.doPktIn = false;
        }

        public void setDstVlan(final int dstVlan) {
            this.dstVlan = dstVlan;
        }

        public void setNshNsp(final long nshNsp) {
            this.nshNsp = nshNsp;
        }

        public void setNshNsi(final short nshNsi) {
            this.nshNsi = nshNsi;
        }

        public void setMplsLabel(final long mplsLabel) {
            this.mplsLabel = mplsLabel;
        }

        public void setDoPktIn(final boolean doPktIn) {
            this.doPktIn = doPktIn;
        }

        @Override
        public void run() {
            try {
                LOG.debug(
                        "SfcProviderSffFlowWriter.ConfigureTransportEgressFlow sff [{}] macSrc [{}] macDst [{}] vlan [{}] mpls [{}] nsp [{}] nsi [{}]",
                        this.sffNodeName, this.srcMac, this.dstMac, this.dstVlan, this.mplsLabel, this.nshNsp,
                        this.nshNsi);

                int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;

                //
                // Matches
                MatchBuilder match = new MatchBuilder();

                if (this.nshNsp >= 0 && this.nshNsi >= 0) {
                    // If its NSH, then we dont need the metadata, match on Nsp/Nsi instead
                    SfcOpenflowUtils.addMatchNshNsp(match, this.nshNsp);
                    SfcOpenflowUtils.addMatchNshNsi(match, this.nshNsi);
                } else {
                    // Match on the metadata pathId
                    SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH);
                }

                if (this.dstMac != null) {
                    SfcOpenflowUtils.addMatchDstMac(match, dstMac);
                } else {
                    // If the dstMac is null, then the packet is leaving SFC and we dont know
                    // to where. Make it a lower priority, and only match on the pathId
                    flowPriority -= 10;
                }

                //
                // Actions
                int order = 0;
                List<Action> actionList = new ArrayList<Action>();

                // Set the macSrc, if present
                if (this.doPktIn) {
                    // Notice TCP SYN matching is only supported in OpenFlow 1.5
                    SfcOpenflowUtils.addMatchTcpSyn(match);
                    actionList.add(SfcOpenflowUtils.createActionPktIn(SfcOpenflowUtils.PKT_LENGTH_IP_HEADER, order++));
                }

                // Set the macSrc
                if (this.srcMac != null) {
                    actionList.add(SfcOpenflowUtils.createActionSetDlSrc(this.srcMac, order++));
                }

                // Nsh stuff, if present
                if (this.nshNsp >= 0 && this.nshNsi >= 0) {
                    if (this.isLastHop) {
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
                }

                // Optionally write the DSCP with the pathId
                if (this.setDscp) {
                    // In order to set the IP DSCP, we need to match IPv4
                    SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
                    actionList.add(SfcOpenflowUtils.createActionWriteDscp((short) this.pathId, order++));
                }

                // Optionally set either the VLAN or MPLS info
                if (dstVlan > 0) {
                    actionList.add(SfcOpenflowUtils.createActionPushVlan(order++));
                    actionList.add(SfcOpenflowUtils.createActionSetVlanId(this.dstVlan, order++));
                } else if (mplsLabel > 0) {
                    actionList.add(SfcOpenflowUtils.createActionPushMpls(order++));
                    actionList.add(SfcOpenflowUtils.createActionSetMplsLabel(this.mplsLabel, order++));
                }

                actionList.add(SfcOpenflowUtils.createActionOutPort(this.port, order++));

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

                FlowBuilder egressTransportFlow = SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                        flowPriority,
                        TRANSPORT_EGRESS_COOKIE,
                        "default_egress_flow",
                        match,
                        isb);

                //
                // Now write the Flow Entry
                writeFlowToConfig(rspId, sffNodeName, egressTransportFlow);

            } catch (Exception e) {
                LOG.error("Caught an exception in ConfigureTransportEgressThread.run() : {}", e);
            }
        }
    }

    // For NSH, Return the packet to INPORT if the NSH Nsc1 Register is not present (==0)
    // If it is present, it will be handled in ConfigureTransportEgressFlowThread()
    // This flow will have a higher priority than the flow created in
    // ConfigureTransportEgressFlowThread()
    @Override
    public void configureNshNscTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port) {
        // This is the last table, cant set next table AND doDrop should be false
        ConfigureNshNscTransportEgressFlowThread configureNshNscTransportEgressFlowThread =
                new ConfigureNshNscTransportEgressFlowThread(sffNodeName, nshNsp, nshNsi, port);
        try {
            threadPoolExecutorService.execute(configureNshNscTransportEgressFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureNshNscTransportEgressFlowThread implements Runnable {

        private Long rspId;
        private String sffNodeName;
        private final long nshNsp;
        private final short nshNsi;
        private String port;

        public ConfigureNshNscTransportEgressFlowThread(final String sffNodeName, final long nshNsp, final short nshNsi,
                String port) {
            this.rspId = flowRspId;
            this.sffNodeName = sffNodeName;
            this.nshNsp = nshNsp;
            this.nshNsi = nshNsi;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.ConfigureNshNscTransportEgressFlowThread, sff [{}]",
                        this.sffNodeName);

                //
                // Match any
                MatchBuilder match = new MatchBuilder();
                SfcOpenflowUtils.addMatchNshNsp(match, this.nshNsp);
                SfcOpenflowUtils.addMatchNshNsi(match, this.nshNsi);
                SfcOpenflowUtils.addMatchNshNsc1(match, 0l);

                //
                // Create the actions
                int order = 0;
                Action outPortBuilder = SfcOpenflowUtils.createActionOutPort(this.port, order++);

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

                //
                // Create and configure the FlowBuilder
                FlowBuilder transportIngressFlow = SfcOpenflowUtils.createFlowBuilder(
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                        FLOW_PRIORITY_TRANSPORT_EGRESS + 10,
                        "MatchAny",
                        match,
                        isb);

                writeFlowToConfig(rspId, sffNodeName, transportIngressFlow);

            } catch (Exception e) {
                LOG.error("ConfigureNshNscTransportEgressFlowThread writer caught an Exception: ", e);
            }
        }
    }

    @Override
    public void configureGroup(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
            List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {

        ConfigureGroupThread configureGroupThread = new ConfigureGroupThread(sffNodeName, openflowNodeId, sfgName,
                sfgId, groupType, bucketInfos, isAddGroup);
        try {
            threadPoolExecutorService.execute(configureGroupThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureGroupThread implements Runnable {

        String sffNodeName;
        String sfgName;
        long sfgId;
        int groupType;
        List<GroupBucketInfo> bucketInfos;
        boolean isAddGroup;
        String openflowNodeId;

        public ConfigureGroupThread(String sffNodeName, String openflowNodeId, String sfgName, long sfgId,
                int groupType, List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {
            super();
            this.sffNodeName = sffNodeName;
            this.openflowNodeId = openflowNodeId;
            this.sfgName = sfgName;
            this.sfgId = sfgId;
            this.groupType = groupType;
            this.bucketInfos = bucketInfos;
            this.isAddGroup = isAddGroup;
        }

        @Override
        public void run() {
            LOG.debug("configuring group: sffName {}, groupName {}, ofNodeId {}, id {}, type {}", sffNodeName, sfgName,
                    openflowNodeId, sfgId, groupType);
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
            LOG.debug("finish writing group to data store \nID: {}\nGroup: {}", sfgId, sfgName);

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

        if (!SfcDataStoreAPI.deleteTransactionAPI(flowInstanceId, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("{}: Failed to remove Flow on node: {}", Thread.currentThread().getStackTrace()[1], sffNodeName);
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

        if (!SfcDataStoreAPI.writeMergeTransactionAPI(flowInstanceId, flow.build(),
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("{}: Failed to create Flow on node: {}", Thread.currentThread().getStackTrace()[1], sffNodeName);
        }
        storeFlowDetails(rspId, sffNodeName, flow.getKey(), flow.getTableId());
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

    @Override
    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId,
            String groupName) {
        ConfigureGroupNextHopFlowThread configureNextHopFlowThread =
                new ConfigureGroupNextHopFlowThread(sffNodeName, sfpId, srcMac, groupId, groupName);
        try {
            threadPoolExecutorService.execute(configureNextHopFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }

    }

    private class ConfigureGroupNextHopFlowThread implements Runnable {

        String sffNodeName;
        long sfpId;
        String srcMac;
        long groupId;
        String groupName;
        Long rspId;

        public ConfigureGroupNextHopFlowThread(final String sffNodeName, final long sfpId, final String srcMac,
                final long groupId, final String groupName) {
            super();
            this.rspId = flowRspId;
            this.sffNodeName = sffNodeName;
            this.sfpId = sfpId;
            this.srcMac = srcMac;
            this.groupId = groupId;
            this.groupName = groupName;
        }

        @Override
        public void run() {
            try {
                LOG.debug(
                        "SfcProviderSffFlowWriter.ConfigureGroupNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] groupId[{}]",
                        this.sffNodeName, this.sfpId, this.srcMac, this.groupId);

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
                                flowPriority,
                                "nextHop",
                                match,
                                isb);
                LOG.debug("writing group next hop flow: \n{}", nextHopFlow);
                writeFlowToConfig(rspId, sffNodeName, nextHopFlow);

            } catch (Exception e) {
                LOG.error("ConfigureNextHopFlow writer caught an Exception: ", e);
            }
        }
    }
}

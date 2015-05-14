/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer.openflow;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.l2renderer.SfcL2FlowProgrammerInterface;
import org.opendaylight.sfc.l2renderer.sfg.GroupBucketInfo;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * This class writes Openflow Flow Entries to the SFF once an SFF has been configured.
 * <p>
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * @since 2014-08-07
 */
public class SfcL2FlowProgrammerOFimpl implements SfcL2FlowProgrammerInterface {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowProgrammerOFimpl.class);

    private static final int COOKIE_BIGINT_HEX_RADIX = 16;
    private static final long SHUTDOWN_TIME = 5;
    private static final BigInteger TRANSPORT_EGRESS_COOKIE =
            new BigInteger("BA5EBA11BA5EBA11", COOKIE_BIGINT_HEX_RADIX);

    // Which bits in the metadata field to set, Assuming 4095 PathId's
    private static final BigInteger METADATA_MASK_SFP_MATCH = new BigInteger("000000000000FFFF", COOKIE_BIGINT_HEX_RADIX);

    private static final short TABLE_INDEX_INGRESS_TRANSPORT_TABLE = 0;
    private static final short TABLE_INDEX_PATH_MAPPER = 1;
    private static final short TABLE_INDEX_PATH_MAPPER_ACL = 2;
    private static final short TABLE_INDEX_NEXT_HOP = 3;
    private static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;

    private static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 250;
    private static final int FLOW_PRIORITY_PATH_MAPPER = 350;
    private static final int FLOW_PRIORITY_PATH_MAPPER_ACL = 450;
    private static final int FLOW_PRIORITY_NEXT_HOP = 550;
    private static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 650;
    private static final int FLOW_PRIORITY_MATCH_ANY = 5;

    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 50;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final int PKTIN_IDLE_TIMEOUT = 60;

    private static final String LOGSTR_THREAD_QUEUE_FULL = "Thread Queue is full, cant execute action: {}";

    // Instance variables
    private short tableBase;
    private ExecutorService threadPoolExecutorService;

    public SfcL2FlowProgrammerOFimpl() {
        this.tableBase = (short) 0;
        this.threadPoolExecutorService =
                new ThreadPoolExecutor(
                        SCHEDULED_THREAD_POOL_SIZE,
                        SCHEDULED_THREAD_POOL_SIZE,
                        ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));
    }

    // This method should only be called by SfcL2Renderer.close()
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

    public short getTableBase() {
        return tableBase;
    }

    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    public boolean compareClassificationTableCookie(FlowCookie cookie) {
        return cookie.getValue().equals(TRANSPORT_EGRESS_COOKIE);
    }

    //
    // Configure the MatchAny entry specifying if it should drop or goto the next table
    // If doDrop == False
    //      TransportIngress MatchAny will go to Ingress
    //      Ingress          MatchAny will go to Acl
    //      Acl              MatchAny will go to NextHop
    //      NextHop          MatchAny will go to TransportEgress
    //
    public void configureTransportIngressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        getTableId(TABLE_INDEX_INGRESS_TRANSPORT_TABLE),
                        getTableId(TABLE_INDEX_PATH_MAPPER),
                        doDrop,
                        isAddFlow);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configurePathMapperTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        getTableId(TABLE_INDEX_PATH_MAPPER),
                        getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                        doDrop,
                        isAddFlow);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configurePathMapperAclTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                        getTableId(TABLE_INDEX_NEXT_HOP),
                        doDrop,
                        isAddFlow);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureNextHopTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        getTableId(TABLE_INDEX_NEXT_HOP),
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                        doDrop,
                        isAddFlow);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureTransportEgressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow) {
        // This is the last table, cant set next table AND doDrop should be false
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                        (short) -1,
                        doDrop,
                        isAddFlow);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureTableMatchAnyThread implements Runnable {
        private String sffNodeName;
        private boolean doDrop;
        private short tableId;
        private short nextTableId;
        private boolean isAddFlow;

        public ConfigureTableMatchAnyThread(final String sffNodeName, final short tableId, final short nextTableId,
                final boolean doDrop, final boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.tableId = tableId;
            this.nextTableId = nextTableId;
            this.doDrop = doDrop;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyThread, sff [{}] tableId [{}] nextTableId [{}] doDrop {}",
                        this.sffNodeName, this.tableId, this.nextTableId, this.doDrop);

                //
                // Create the actions
                List<Instruction> instructions = new ArrayList<Instruction>();
                int order = 0;

                if(this.doDrop) {
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
                    GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(this.nextTableId);

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
                FlowBuilder transportIngressFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                this.tableId,
                                FLOW_PRIORITY_MATCH_ANY,
                                "MatchAny",
                                match,
                                isb);

                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, transportIngressFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, transportIngressFlow);
                }
            } catch (Exception e) {
                LOG.error("ConfigureTableMatchAnyThread writer caught an Exception: ", e);
            }
        }
    }

    //
    // Congfigure Table 0, Transport Ingress
    //
    public void configureIpv4TransportIngressFlow(final String sffNodeName, final boolean isAddFlow) {
        ConfigureTransportIngressThread configureIngressTransportTcpThread =
                new ConfigureTransportIngressThread(
                        sffNodeName, SfcOpenflowUtils.ETHERTYPE_IPV4, isAddFlow);
        configureIngressTransportTcpThread.setIpProtocol(SfcOpenflowUtils.IP_PROTOCOL_TCP);

        ConfigureTransportIngressThread configureIngressTransportUdpThread =
                new ConfigureTransportIngressThread(
                        sffNodeName, SfcOpenflowUtils.ETHERTYPE_IPV4, isAddFlow);
        configureIngressTransportUdpThread.setIpProtocol(SfcOpenflowUtils.IP_PROTOCOL_UDP);

        try {
            threadPoolExecutorService.execute(configureIngressTransportTcpThread);
            threadPoolExecutorService.execute(configureIngressTransportUdpThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVlanTransportIngressFlow(final String sffNodeName, final boolean isAddFlow) {
        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_VLAN, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureIngressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVxlanGpeTransportIngressFlow(final String sffNodeName, final boolean isAddFlow) {
        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_IPV4, isAddFlow);
        configureIngressTransportThread.setNextTable(TABLE_INDEX_NEXT_HOP);
        try {
            threadPoolExecutorService.execute(configureIngressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureMplsTransportIngressFlow(final String sffNodeName, final boolean isAddFlow) {

        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureIngressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureTransportIngressThread implements Runnable {
        String sffNodeName;
        boolean isAddFlow;
        long etherType;
        short ipProtocol;
        short nextTable;

        public ConfigureTransportIngressThread(final String sffNodeName, long etherType, final boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.etherType = etherType;
            this.isAddFlow = isAddFlow;
            this.ipProtocol = (short) -1;
            this.nextTable = TABLE_INDEX_PATH_MAPPER;
        }

        public void setIpProtocol(short ipProtocol) { this.ipProtocol = ipProtocol; }
        public void setNextTable(short nextTable) { this.nextTable = nextTable; }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow, sff [{}] etherType [{}]",
                        this.sffNodeName, this.etherType);

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();
                if(this.ipProtocol > 0) {
                    SfcOpenflowUtils.addMatchIpProtocol(match, this.ipProtocol);
                }

                if(this.etherType == SfcOpenflowUtils.ETHERTYPE_VLAN) {
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
                GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(getTableId(this.nextTable));

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
                                TABLE_INDEX_INGRESS_TRANSPORT_TABLE,
                                FLOW_PRIORITY_TRANSPORT_INGRESS,
                                "ingress_Transport_Default_Flow",
                                match,
                                isb);

                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, transportIngressFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, transportIngressFlow);
                }
            } catch (Exception e) {
                LOG.error("ConfigureTransportIngress writer caught an Exception: ", e);
            }
        }
    }


    //
    // Configure Table 1, PathMapper
    //
    public void configureMacPathMapperFlow(final String sffNodeName, final String mac, long pathId, boolean isSf, final boolean isAddFlow) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, isSf, pathId, isAddFlow);
        configurePathMapperFlowThread.setMacAddress(mac);
        try {
            threadPoolExecutorService.execute(configurePathMapperFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureMplsPathMapperFlow(final String sffNodeName, final long label, long pathId, boolean isSf, final boolean isAddFlow) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, isSf, pathId, isAddFlow);
        configurePathMapperFlowThread.setMplsLabel(label);
        try {
            threadPoolExecutorService.execute(configurePathMapperFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVlanPathMapperFlow(final String sffNodeName, final int vlan, long pathId, boolean isSf, final boolean isAddFlow) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, isSf, pathId, isAddFlow);
        configurePathMapperFlowThread.setVlanId(vlan);
        try {
            threadPoolExecutorService.execute(configurePathMapperFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVxlanGpePathMapperFlow(final String sffNodeName, long nsp, short nsi, long pathId, final boolean isAddFlow) {
        ConfigurePathMapperFlowThread configurePathMapperFlowThread =
                new ConfigurePathMapperFlowThread(sffNodeName, false, pathId, isAddFlow);
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
        boolean isAddFlow;

        public void setVlanId(final int vlan) { this.vlan = vlan; }
        public void setNsp(final long nsp) { this.nsp = nsp; }
        public void setNsi(final short nsi) { this.nsi = nsi; }
        public void setMplsLabel(final long label) { this.mplsLabel = label; }
        public void setMacAddress(final String macAddress) { this.macAddress = macAddress; }

        public ConfigurePathMapperFlowThread(final String sffNodeName, final boolean isSf, final long pathId, final boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.pathId = pathId;
            this.isAddFlow = isAddFlow;
            this.vlan = -1; // not set
            this.nsp = -1; // not set
            this.nsi = -1; // not set
            this.mplsLabel = -1; // not set
            this.isSf = isSf;
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.configurePathMapperFlow sff [{}] pathId [{}] vlan [{}] mpls [{}] mac [{}]",
                        this.sffNodeName, this.pathId, this.vlan, this.mplsLabel, this.macAddress);

                MatchBuilder match = new MatchBuilder();
                List<Action> actionList = new ArrayList<Action>();
                int actionOrder = 0;

                if(this.isSf) {
                    SfcOpenflowUtils.addMatchDscp(match, (short) this.pathId);
                }

                if(this.vlan >= 0) {
                    SfcOpenflowUtils.addMatchVlan(match, this.vlan);
                    actionList.add(SfcOpenflowUtils.createActionPopVlan(actionOrder++));
                } else if(this.mplsLabel >= 0) {
                    SfcOpenflowUtils.addMatchMplsLabel(match, this.mplsLabel);
                    actionList.add(SfcOpenflowUtils.createActionPopMpls(actionOrder++));
                } else if(this.macAddress.length() > 0) {
                    SfcOpenflowUtils.addMatchSrcMac(match, this.macAddress);
                } else if (this.nsp >= 0 && this.nsi >= 0) {
                    //VxLAN-gpe + NSH
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
                metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(
                        actionOrder++, getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH));
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
                FlowBuilder ingressFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                TABLE_INDEX_PATH_MAPPER,
                                FLOW_PRIORITY_PATH_MAPPER,
                                "nextHop",
                                match,
                                isb);

                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, ingressFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, ingressFlow);
                }
            } catch (Exception e) {
                LOG.error("ConfigurePathMapperFlow writer caught an Exception: ", e);
            }
        }
    }


    //
    // Table 3, PathMapper ACL
    //          This table is populated as a result of PktIn for TCP Proxy SFs.
    //          The Src/Dst IP will be used to map the path ID
    //
    public void configurePathMapperAclFlow(
            final String sffNodeName, final String pktSrcIpStr, final String pktDstIpStr, short pathId, boolean isAddFlow) {
        ConfigurePathMapperAclFlowThread configurePathMapperAclFlowThread =
                new ConfigurePathMapperAclFlowThread(sffNodeName, pktSrcIpStr, pktDstIpStr, pathId, isAddFlow);

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
        boolean isAddFlow;

        public ConfigurePathMapperAclFlowThread(
                final String sffNodeName, final String srcIpStr, final String dstIpStr, short pathId, boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.pathId = pathId;
            this.srcIpStr = srcIpStr;
            this.dstIpStr = dstIpStr;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.configurePathMapperAclFlow sff [{}] srcIp [{}] dstIp [{}] pathId [{}]",
                        this.sffNodeName, this.srcIpStr, this.dstIpStr, this.pathId);

                //
                // Match on the Src and Dst IPs
                MatchBuilder match = new MatchBuilder();
                SfcOpenflowUtils.addMatchSrcIpv4(match, this.srcIpStr);
                SfcOpenflowUtils.addMatchDstIpv4(match, this.dstIpStr);

                //
                // Set the PathId in the metadata and goto the TransportEgress table
                int ibOrder = 0;
                InstructionBuilder metadataIb = new InstructionBuilder();
                metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(
                        ibOrder, getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH));
                metadataIb.setKey(new InstructionKey(ibOrder));
                metadataIb.setOrder(ibOrder++);

                GoToTableBuilder gotoNextHop = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
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
                FlowBuilder ingressFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                TABLE_INDEX_PATH_MAPPER_ACL,
                                FLOW_PRIORITY_PATH_MAPPER_ACL,
                                "nextHop",
                                match,
                                isb);
                // Set an idle timeout on this flow
                ingressFlow.setIdleTimeout(PKTIN_IDLE_TIMEOUT);

                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, ingressFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, ingressFlow);
                }

            } catch (Exception e) {
                LOG.error("configurePathMapperAclFlow writer caught an Exception: ", e);
            }
        }
    }

    //
    // Table 4, NextHop
    //
    public void configureNextHopFlow(final String sffNodeName, final long sfpId, final String srcMac, final String dstMac, final boolean isAddFlow) {

        ConfigureNextHopFlowThread configureNextHopFlowThread =
                new ConfigureNextHopFlowThread(sffNodeName, sfpId, srcMac, dstMac, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureNextHopFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVxlanGpeNextHopFlow(final String sffNodeName, final String dstIp, final long nsp, final short nsi, final boolean isAddFlow) {
        ConfigureNextHopFlowThread configureNextHopFlowThread =
                new ConfigureNextHopFlowThread(sffNodeName, nsp, null, null, isAddFlow);
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
        String sffNodeName;
        long sfpId;
        String srcMac;
        String dstMac;
        String dstIp;
        long nshNsp;
        short nshNsi;
        boolean isAddFlow;

        public void setDstIp(final String dstIp) { this.dstIp = dstIp; }
        public void setNsp(final long nshNsp) { this.nshNsp = nshNsp; }
        public void setNsi(final short nshNsi) { this.nshNsi = nshNsi; }

        public ConfigureNextHopFlowThread(
                final String sffNodeName, final long sfpId, final String srcMac, final String dstMac, final boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.sfpId = sfpId;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.isAddFlow = isAddFlow;
            this.nshNsi = -1;     // unused
            this.nshNsp = -1;     // unused
            this.dstIp = null; // unused
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.configureNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] dstMac [{}]",
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
                if(srcMac != null) {
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

                GoToTableBuilder gotoTb = SfcOpenflowUtils.createActionGotoTable(TABLE_INDEX_TRANSPORT_EGRESS);

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
                                TABLE_INDEX_NEXT_HOP,
                                flowPriority,
                                "nextHop",
                                match,
                                isb);

                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, nextHopFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, nextHopFlow);
                }
            } catch (Exception e) {
                LOG.error("ConfigureNextHopFlow writer caught an Exception: ", e);
            }
        }
    }


    //
    // Table 10, Transport Egress
    //
    public void configureMacTransportEgressFlow(
            final String sffNodeName, final String srcMac, final String dstMac,
            final String port, final long pathId, final boolean setDscp,
            final boolean doPktIn, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isAddFlow);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVlanTransportEgressFlow(
            final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId, boolean setDscp,
            final boolean doPktIn, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isAddFlow);
        configureEgressTransportThread.setDstVlan(dstVlan);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVxlanGpeTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi,
            String port, final boolean doPktIn, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, null, null, port, nshNsp, false, isAddFlow);
        configureEgressTransportThread.setNshNsp(nshNsp);
        configureEgressTransportThread.setNshNsi(nshNsi);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureMplsTransportEgressFlow(
            final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, final String port, final long pathId, final boolean setDscp,
            final boolean doPktIn, final boolean isAddFlow) {

        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isAddFlow);
        configureEgressTransportThread.setMplsLabel(mplsLabel);
        configureEgressTransportThread.setDoPktIn(doPktIn);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureTransportEgressThread implements Runnable {
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
        boolean doPktIn;
        boolean isAddFlow;

        public ConfigureTransportEgressThread(
                final String sffNodeName, String srcMac, String dstMac, String port,
                final long pathId, boolean setDscp, boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.dstVlan = -1;   // unused
            this.nshNsp = -1;   // unused
            this.nshNsi = -1;   // unused
            this.mplsLabel = -1; // unused
            this.port = port;
            this.pathId = pathId;
            this.setDscp = setDscp;
            this.doPktIn = false;
            this.isAddFlow = isAddFlow;
        }
        public void setDstVlan(final int dstVlan) { this.dstVlan = dstVlan; }
        public void setNshNsp(final long nshNsp) { this.nshNsp = nshNsp; }
        public void setNshNsi(final short nshNsi) { this.nshNsi = nshNsi; }
        public void setMplsLabel(final long mplsLabel) { this.mplsLabel = mplsLabel; }
        public void setDoPktIn(final boolean doPktIn) { this.doPktIn = doPktIn; }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportEgressFlow sff [{}] macSrc [{}] macDst [{}] vlan [{}] mpls [{}] nsp [{}] nsi [{}]",
                        this.sffNodeName, this.srcMac, this.dstMac, this.dstVlan, this.mplsLabel, this.nshNsp, this.nshNsi);

                int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;

                int order = 0;
                List<Action> actionList = new ArrayList<Action>();
                MatchBuilder match = new MatchBuilder();

                if (this.nshNsp >=0 && this.nshNsi >= 0) {
                    // If its NSH, then we dont need the metadata
                    SfcOpenflowUtils.addMatchNshNsp(match, this.nshNsp);
                    SfcOpenflowUtils.addMatchNshNsi(match, this.nshNsi);
                    actionList.add(SfcOpenflowUtils.createActionNxMoveTunIdRegister(order++));
                } else {
                    // Match on the metadata pathId
                    SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH);
                }

                if(this.dstMac != null) {
                    SfcOpenflowUtils.addMatchDstMac(match, dstMac);
                } else {
                    // If the dstMac is null, then the packet is leaving SFC and we dont know
                    // to where. Make it a lower priority, and only match on the pathId
                    flowPriority -= 10;
                }

                if(this.doPktIn) {
                    SfcOpenflowUtils.addMatchTcpSyn(match);
                    actionList.add(SfcOpenflowUtils.createActionPktIn(SfcOpenflowUtils.PKT_LENGTH_IP_HEADER, order++));
                }

                // Set the macSrc
                if(this.srcMac != null) {
                    actionList.add(SfcOpenflowUtils.createActionSetDlSrc(this.srcMac, order++));
                }

                // Optionally write the DSCP with the pathId
                if(this.setDscp) {
                    // In order to set the IP DSCP, we need to match IPv4
                    SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
                    actionList.add(SfcOpenflowUtils.createActionWriteDscp((short) this.pathId, order++));
                }

                // Optionally set either the VLAN or MPLS info
                if(dstVlan > 0) {
                    actionList.add(SfcOpenflowUtils.createActionPushVlan(order++));
                    actionList.add(SfcOpenflowUtils.createActionSetVlanId(this.dstVlan, order++));
                } else if(mplsLabel > 0) {
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

                FlowBuilder egressTransportFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                TABLE_INDEX_TRANSPORT_EGRESS,
                                flowPriority,
                                TRANSPORT_EGRESS_COOKIE,
                                "default_egress_flow",
                                match,
                                isb);

                //
                // Now write the Flow Entry
                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, egressTransportFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, egressTransportFlow);
                }

            } catch (Exception e) {
                LOG.error("Caught an exception in ConfigureTransportEgressThread.run() : {}", e);
            }
        }
    }

    @Override
    public void configureGroup(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
            List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {

        ConfigureGroupThread configureGroupThread = new ConfigureGroupThread(sffNodeName, openflowNodeId, sfgName, sfgId, groupType, bucketInfos, isAddGroup);
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

        public ConfigureGroupThread(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
                List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {
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
            LOG.debug("configuring group: sffName {}, groupName {}, ofNodeId {}, id {}, type {}", sffNodeName, sfgName, openflowNodeId, sfgId, groupType);
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

        private Bucket buildBucket(BucketBuilder bb, GroupBucketInfo bucketInfo){
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
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class, gk).build();
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
     * @param flow - details of the flow to be removed
     */
    private void removeFlowFromConfig(final String sffNodeName, FlowBuilder flow) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey())
                .build();

        if (! SfcDataStoreAPI.deleteTransactionAPI(flowInstanceId, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("{}: Failed to remove Flow on node: {}",
                    Thread.currentThread().getStackTrace()[1], sffNodeName);
        }
    }

    /**
     * Write a flow to the DataStore
     *
     * @param sffNodeName - which SFF to write the flow to
     * @param flow - details of the flow to be written
     */
    private void writeFlowToConfig(final String sffNodeName, FlowBuilder flow) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path, which will include the Node, Table, and Flow
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey())
                .build();

        LOG.debug("writeFlowToConfig writing flow to Node {}, table {}", sffNodeName, flow.getTableId());

        if (! SfcDataStoreAPI.writeMergeTransactionAPI(flowInstanceId, flow.build(), LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("{}: Failed to create Flow on node: {}",
                    Thread.currentThread().getStackTrace()[1], sffNodeName);
        }
    }

    private static BigInteger getMetadataSFP(long sfpId) {
        return (BigInteger.valueOf(sfpId).and(new BigInteger("FFFF", COOKIE_BIGINT_HEX_RADIX)));
    }

    /**
     * getTableId
     *
     * Having a TableBase allows us to "offset" the SFF tables by this.tableBase
     * tables Doing so allows for OFS tables previous to the SFF tables.
     * tableIndex should be one of: TABLE_INDEX_SFF_ACL or
     * TABLE_INDEX_SFF_OUTPUT
     */
    private short getTableId(short tableIndex) {
        return (short) (tableBase + tableIndex);
    }

    @Override
    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId, String groupName, boolean isAddFlow) {
        ConfigureGroupNextHopFlowThread configureNextHopFlowThread =
                new ConfigureGroupNextHopFlowThread(sffNodeName, sfpId, srcMac, groupId, groupName, isAddFlow);
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
        boolean isAddFlow;

        public ConfigureGroupNextHopFlowThread(
                final String sffNodeName, final long sfpId, final String srcMac, final long groupId, final String groupName, final boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.sfpId = sfpId;
            this.srcMac = srcMac;
            this.groupId = groupId;
            this.groupName = groupName;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {
                LOG.debug("SfcProviderSffFlowWriter.ConfigureGroupNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] groupId[{}]",
                        this.sffNodeName, this.sfpId, this.srcMac, this.groupId);

                int flowPriority = FLOW_PRIORITY_NEXT_HOP;

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();

                // Match on the either the metadata sfpId or the NSH NSP and NSI
                SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);

                // match on the src mac
                if(srcMac != null) {
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
                                TABLE_INDEX_NEXT_HOP,
                                flowPriority,
                                "nextHop",
                                match,
                                isb);
                LOG.debug("writing group next hop flow: \n{}", nextHopFlow);
                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, nextHopFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, nextHopFlow);
                }
            } catch (Exception e) {
                LOG.error("ConfigureNextHopFlow writer caught an Exception: ", e);
            }
        }
    }
}

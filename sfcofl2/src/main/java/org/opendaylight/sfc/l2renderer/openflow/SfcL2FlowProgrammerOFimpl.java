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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.l2renderer.SfcL2FlowProgrammerInterface;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;

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

    // Which bits in the metadata field to set, Assuming 4095 PathId's
    private static final BigInteger METADATA_MASK_SFP_MATCH     = new BigInteger("000000000000FFFF", COOKIE_BIGINT_HEX_RADIX);
    // Just looking for a unique cookie, hopefully nobody else uses baseballbaseball
    private static final BigInteger CLASSIFICATION_TABLE_COOKIE = new BigInteger("BA5EBA11BA5EBA11", COOKIE_BIGINT_HEX_RADIX);

    private static final short TABLE_INDEX_INGRESS_TRANSPORT_TABLE = 0;
    private static final short TABLE_INDEX_INGRESS = 1;
    private static final short TABLE_INDEX_CLASSIFICATION = 2;
    private static final short TABLE_INDEX_NEXT_HOP = 3;
    private static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;

    private static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 256;
    private static final int FLOW_PRIORITY_INGRESS = 256;
    private static final int FLOW_PRIORITY_CLASSIFICATION = 256;
    private static final int FLOW_PRIORITY_NEXT_HOP = 256;
    private static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 256;
    private static final int FLOW_PRIORITY_MATCH_ANY = 5;

    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 50;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;

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
    public void shutdown() {
        threadPoolExecutorService.shutdown();
    }

    public short getTableBase() {
        return tableBase;
    }

    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    public boolean compareClassificationTableCookie(FlowCookie cookie) {
        return cookie.getValue().equals(CLASSIFICATION_TABLE_COOKIE);
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
                        getTableId(TABLE_INDEX_INGRESS),
                        doDrop,
                        isAddFlow);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureIngressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        getTableId(TABLE_INDEX_INGRESS),
                        getTableId(TABLE_INDEX_CLASSIFICATION),
                        doDrop,
                        isAddFlow);
        try {
            threadPoolExecutorService.execute(configureTableMatchAnyThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureAclTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow) {
        ConfigureTableMatchAnyThread configureTableMatchAnyThread =
                new ConfigureTableMatchAnyThread(
                        sffNodeName,
                        getTableId(TABLE_INDEX_CLASSIFICATION),
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
                LOG.info("SfcProviderSffFlowWriter.ConfigureTableMatchAnyThread, sff [{}] tableId [{}] nextTableId [{}] doDrop {}",
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
                LOG.info("ConfigureTableMatchAnyThread writer caught an Exception: ");
                LOG.error(e.getMessage(), e);
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

    public void configureVxlanGpeTransportIngressFlow(final String sffNodeName, int dstPort, final boolean isAddFlow) {
        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_VLAN, isAddFlow);
        configureIngressTransportThread.setIpProtocol(SfcOpenflowUtils.IP_PROTOCOL_UDP);
        configureIngressTransportThread.setDstUdpPort(new PortNumber(dstPort));
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
        PortNumber dstUdpPort;

        public ConfigureTransportIngressThread(final String sffNodeName, long etherType, final boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.etherType = etherType;
            this.isAddFlow = isAddFlow;
            this.ipProtocol = (short) -1;
            this.dstUdpPort = null;
        }

        public void setIpProtocol(short ipProtocol) { this.ipProtocol = ipProtocol; }
        public void setDstUdpPort(PortNumber dstUdpPort) { this.dstUdpPort = dstUdpPort; }

        @Override
        public void run() {
            try {
                LOG.info("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow, sff [{}] etherType [{}]",
                        this.sffNodeName, this.etherType);

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();
                if(this.ipProtocol > 0) {
                    SfcOpenflowUtils.addMatchIpProtocol(match, this.ipProtocol);
                }

                if (this.dstUdpPort != null) {
                    SfcOpenflowUtils.addMatchDstUdpPort(match, this.dstUdpPort);
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
                // Action, goto Ingress table
                GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_INGRESS));

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
                LOG.info("ConfigureTransportIngress writer caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }


    //
    // Configure Table 1, Ingress
    //

    public void configureMacIngressFlow(final String sffNodeName, final String mac, long pathId, final boolean isAddFlow) {
        ConfigureIngressFlowThread configureIngressFlowThread = new ConfigureIngressFlowThread(sffNodeName, pathId, isAddFlow);
        configureIngressFlowThread.setMacAddress(mac);
        try {
            threadPoolExecutorService.execute(configureIngressFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureMplsIngressFlow(final String sffNodeName, final long label, long pathId, final boolean isAddFlow) {
        ConfigureIngressFlowThread configureIngressFlowThread = new ConfigureIngressFlowThread(sffNodeName, pathId, isAddFlow);
        configureIngressFlowThread.setMplsLabel(label);
        try {
            threadPoolExecutorService.execute(configureIngressFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVlanIngressFlow(final String sffNodeName, final int vlan, long pathId, final boolean isAddFlow) {
        ConfigureIngressFlowThread configureIngressFlowThread = new ConfigureIngressFlowThread(sffNodeName, pathId, isAddFlow);
        configureIngressFlowThread.setVlanId(vlan);
        try {
            threadPoolExecutorService.execute(configureIngressFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVxlanGpeIngressFlow(final String sffNodeName, long nsp, short nsi, long pathId, final boolean isAddFlow) {
        ConfigureIngressFlowThread configureIngressFlowThread = new ConfigureIngressFlowThread(sffNodeName, pathId, isAddFlow);
        configureIngressFlowThread.setNsp(nsp);
        configureIngressFlowThread.setNsi(nsi);
        try {
            threadPoolExecutorService.execute(configureIngressFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureIngressFlowThread implements Runnable {
        String sffNodeName;
        long pathId;
        int vlan;
        long nsp;
        short nsi;
        long mplsLabel;
        String macAddress;
        boolean isAddFlow;

        public void setVlanId(final int vlan) { this.vlan = vlan; }
        public void setNsp(final long nsp) { this.nsp = nsp; }
        public void setNsi(final short nsi) { this.nsi = nsi; }
        public void setMplsLabel(final long label) { this.mplsLabel = label; }
        public void setMacAddress(final String macAddress) { this.macAddress = macAddress; }

        public ConfigureIngressFlowThread(final String sffNodeName, final long pathId, final boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.pathId = pathId;
            this.isAddFlow = isAddFlow;
            this.vlan = -1; // not set
            this.nsp = -1; // not set
            this.nsi = -1; // not set
            this.mplsLabel = -1; // not set
        }

        @Override
        public void run() {
            try {
                LOG.info("SfcProviderSffFlowWriter.configureIngressFlow sff [{}] pathId [{}] vlan [{}] mpls [{}] mac [{}]",
                        this.sffNodeName, this.pathId, this.vlan, this.mplsLabel, this.macAddress);

                MatchBuilder match = new MatchBuilder();
                List<Action> actionList = new ArrayList<Action>();
                int actionOrder = 0;

                if(this.vlan >= 0) {
                    SfcOpenflowUtils.addMatchVlan(match, this.vlan);
                    actionList.add(SfcOpenflowUtils.createActionPopVlan(actionOrder++));
                } else if(this.mplsLabel >= 0) {
                    SfcOpenflowUtils.addMatchMplsLabel(match, this.mplsLabel);
                    actionList.add(SfcOpenflowUtils.createActionPopMpls(actionOrder++));
                } else if(this.macAddress.length() > 0) {
                    // TODO, should this be addMatchSrcMac() or addMatchDstMac() ???
                    SfcOpenflowUtils.addMatchSrcMac(match, this.macAddress);
                } else if (this.nsp >= 0 && this.nsi >= 0) {
                    //VxLAN-gpe + NSH
                    SfcOpenflowUtils.addMatchNxNsp(match, this.nsp);
                    SfcOpenflowUtils.addMatchNxNsi(match, this.nsi);
                    actionList.add(SfcOpenflowUtils.createActionNxSetNsp(this.nsp, actionOrder++));
                    actionList.add(SfcOpenflowUtils.createActionNxSetNsi(this.nsi, actionOrder++));
                }

                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();

                int ibOrder = 0;
                short nextTable = TABLE_INDEX_NEXT_HOP;

                if(pathId < 0) {
                    nextTable = TABLE_INDEX_CLASSIFICATION;
                } else {
                    InstructionBuilder metadataIb = new InstructionBuilder();
                    metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(
                            actionOrder++, getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH));
                    metadataIb.setKey(new InstructionKey(ibOrder));
                    metadataIb.setOrder(ibOrder++);
                    instructions.add(metadataIb.build());
                }

                InstructionBuilder actionsIb = new InstructionBuilder();
                actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                actionsIb.setKey(new InstructionKey(ibOrder));
                actionsIb.setOrder(ibOrder++);

                GoToTableBuilder gotoNextHop = SfcOpenflowUtils.createActionGotoTable(getTableId(nextTable));
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
                                TABLE_INDEX_INGRESS,
                                FLOW_PRIORITY_INGRESS,
                                "nextHop",
                                match,
                                isb);

                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, ingressFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, ingressFlow);
                }
            } catch (Exception e) {
                LOG.info("ConfigureIngressFlow writer caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void configureClassificationFlow(final String sffNodeName, final long pathId, final boolean isAddFlow) {

        ConfigureClassificationFlowThread configureClassificationFlowThread =
                new ConfigureClassificationFlowThread(sffNodeName, pathId, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureClassificationFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureClassificationFlowThread implements Runnable {
        String sffNodeName;
        long pathId;
        boolean isAddFlow;

        public ConfigureClassificationFlowThread(final String sffNodeName, final long pathId, final boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.pathId = pathId;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {

                LOG.info("SfcProviderSffFlowWriter.writeSffAcl() pathId {}", pathId);

                //
                // Create the 5-tuple matching criteria

                // Match on the IP DSCP, which was set on the packet when it was sent to the SF
                MatchBuilder match = new MatchBuilder();
                SfcOpenflowUtils.addMatchDscp(match, (short) this.pathId);

                //
                // Create the Actions

                // Create the Metadata action and wrap it in an InstructionBuilder
                // Set the bits specified by METADATA_BITS with the bucket value
                WriteMetadataBuilder wmb = new WriteMetadataBuilder();
                wmb.setMetadata(getMetadataSFP(this.pathId));
                wmb.setMetadataMask(METADATA_MASK_SFP_MATCH);

                InstructionBuilder wmbIb = new InstructionBuilder();
                wmbIb.setInstruction(new WriteMetadataCaseBuilder().setWriteMetadata(wmb.build()).build());
                wmbIb.setKey(new InstructionKey(0));
                wmbIb.setOrder(0);

                // Create the Goto Table (Twcl) Action and wrap it in an
                // InstructionBuilder
                GoToTableBuilder gotoTb = new GoToTableBuilder();
                gotoTb.setTableId(getTableId(getTableId(TABLE_INDEX_NEXT_HOP)));

                InstructionBuilder gotoTbIb = new InstructionBuilder();
                gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
                gotoTbIb.setKey(new InstructionKey(1));
                gotoTbIb.setOrder(1);

                //
                // Put the Instructions in a list of Instructions
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(gotoTbIb.build());
                instructions.add(wmbIb.build());

                InstructionsBuilder isb = new InstructionsBuilder();
                isb.setInstruction(instructions);

                //
                // Create and configure the FlowBuilder
                FlowBuilder aclFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                TABLE_INDEX_CLASSIFICATION,
                                FLOW_PRIORITY_CLASSIFICATION,
                                CLASSIFICATION_TABLE_COOKIE,
                                "acl",
                                match,
                                isb);

                if (isAddFlow) {
                    writeFlowToConfig(sffNodeName, aclFlow);
                } else {
                    removeFlowFromConfig(sffNodeName, aclFlow);
                }
            } catch (Exception e) {
                LOG.info("SfcProviderSffFlowWriter.writeSffAcl() caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }

    //
    // Table 3, NextHop
    //
    public void configureNextHopFlow(final String sffNodeName, final long sfpId, final String srcMac, final String dstMac, final boolean isAddFlow) {

        // TODO make sure the sfpId is set to indicate Uplink/Downlink
        //      for packets coming from SF instead of checking the pathId
        //      For packets coming from the SF, we dont need the pathId, just the direction

        ConfigureNextHopFlowThread configureNextHopFlowThread =
                new ConfigureNextHopFlowThread(sffNodeName, sfpId, srcMac, dstMac, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureNextHopFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVxlanGpeNextHopFlow(final String sffNodeName, final long sfpId, final String srcIp, final String dstIp, final long nsp, final short nsi, final boolean isAddFlow) {
        ConfigureNextHopFlowThread configureNextHopFlowThread =
                new ConfigureNextHopFlowThread(sffNodeName, sfpId, null, null, isAddFlow);
        configureNextHopFlowThread.setSrcIp(srcIp);
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
        String srcIp;
        String dstIp;
        long nsp;
        short nsi;
        boolean isAddFlow;

        public void setSrcIp(final String srcIp) { this.srcIp = srcIp; }
        public void setDstIp(final String dstIp) { this.dstIp = dstIp; }
        public void setNsp(final long nsp) { this.nsp = nsp; }
        public void setNsi(final short nsi) { this.nsi = nsi; }

        public ConfigureNextHopFlowThread(
                final String sffNodeName, final long sfpId, final String srcMac, final String dstMac, final boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.sfpId = sfpId;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {
                LOG.info("SfcProviderSffFlowWriter.configureNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] dstMac [{}]",
                        this.sffNodeName, this.sfpId, this.srcMac, this.dstMac);

                if(this.dstMac == null) {
                    LOG.error("SfcProviderSffFlowWriter.configureNextHopFlow dstMac is null, returning");
                    return;
                }

                int flowPriority = FLOW_PRIORITY_NEXT_HOP;

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();

                // Match on the metadata sfpId
                SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);

                // match on the src mac
                if(srcMac != null) {
                    SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
                } else {
                    // If the srcMac is null, then the packet is entering SFC and we dont know
                    // from where. Make it a lower priority, and only match on the pathId
                    flowPriority -= 10;
                }

                if (nsp >= 0 && nsi >= 0) {
                    SfcOpenflowUtils.addMatchNxNsp(match, this.nsp);
                    SfcOpenflowUtils.addMatchNxNsi(match, this.nsi);
                }

                if (srcIp != null) {
                    SfcOpenflowUtils.addMatchSrcIpv4(match, new Ipv4Prefix(srcIp + "/32"));
                }

                //
                // Create the Actions
                List<Action> actionList = new ArrayList<Action>();

                if (dstMac != null) {
                    // Set the DL (Data Link) Dest Mac Address
                    Action actionDstMac = SfcOpenflowUtils.createActionSetDlDst(dstMac, 0);
                    actionList.add(actionDstMac);
                }

                if (dstIp != null) {
                    Address ipaddr;
                    ipaddr = new Ipv4Builder().setIpv4Address(new Ipv4Prefix(dstIp + "/32")).build();
                    Action actionSetNwDst = SfcOpenflowUtils.createActionSetNwDst(ipaddr, 0);
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
                LOG.info("ConfigureNextHopFlow writer caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }


    //
    // Table 10, Transport Egress
    // TODO we need to parameterize the out port "1"
    //
    public void configureMacTransportEgressFlow(
            final String sffNodeName, final String srcMac, final String dstMac,
            int port, final long pathId, boolean setDscp, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVlanTransportEgressFlow(
            final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, int port, final long pathId, boolean setDscp, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isAddFlow);
        configureEgressTransportThread.setDstVlan(dstVlan);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVxlanGpeTransportEgressFlow(
            final String sffNodeName, final String srcIp, final String dstIp,
            final long dstNsp, final short dstNsi, int port,
            final long pathId, boolean setDscp, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, null, null, port, pathId, setDscp, isAddFlow);
        configureEgressTransportThread.setSrcIp(srcIp);
        configureEgressTransportThread.setDstIp(dstIp);
        configureEgressTransportThread.setDstNsp(dstNsp);
        configureEgressTransportThread.setDstNsi(dstNsi);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureMplsTransportEgressFlow(
            final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, int port, final long pathId, boolean setDscp, final boolean isAddFlow) {

        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, srcMac, dstMac, port, pathId, setDscp, isAddFlow);
        configureEgressTransportThread.setMplsLabel(mplsLabel);
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
        String srcIp;
        String dstIp;
        long dstNsp;
        short dstNsi;
        long mplsLabel;
        int port;
        long pathId;
        boolean setDscp;
        boolean isAddFlow;

        public ConfigureTransportEgressThread(
                final String sffNodeName, String srcMac, String dstMac, int port,
                final long pathId, boolean setDscp, boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.dstVlan = -1;   // unused
            this.srcIp = null;
            this.dstIp = null;
            this.dstNsp = -1;   // unused
            this.dstNsi = -1;   // unused
            this.mplsLabel = -1; // unused
            this.port = port;
            this.pathId = pathId;
            this.setDscp = setDscp;
            this.isAddFlow = isAddFlow;
        }
        public void setDstVlan(final int dstVlan) { this.dstVlan = dstVlan; }
        public void setSrcIp(final String srcIp) { this.srcIp = srcIp; }
        public void setDstIp(final String dstIp) { this.dstIp = dstIp; }
        public void setDstNsp(final long dstNsp) { this.dstNsp = dstNsp; }
        public void setDstNsi(final short dstNsi) { this.dstNsi = dstNsi; }
        public void setMplsLabel(final long mplsLabel) { this.mplsLabel = mplsLabel; }

        @Override
        public void run() {
            try {
                LOG.info("SfcProviderSffFlowWriter.ConfigureTransportEgressFlow sff [{}] macSrc [{}] macDst [{}] vlan [{}] mpls [{}]",
                        this.sffNodeName, this.srcMac, this.dstMac, this.dstVlan, this.mplsLabel);

                int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;

                MatchBuilder match = new MatchBuilder();

                // Match on the metadata pathId
                SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH);

                if(this.dstMac != null) {
                    SfcOpenflowUtils.addMatchDstMac(match, dstMac);
                    flowPriority -= 10;
                }

                int order = 0;
                List<Action> actionList = new ArrayList<Action>();

                // Set the macSrc
                if(this.srcMac != null) {
                    Action setMacSrc = SfcOpenflowUtils.createActionSetDlSrc(this.srcMac, order++);
                    actionList.add(setMacSrc);
                }

                // Optionally write the DSCP with the pathId
                if(this.setDscp) {
                    // TODO currently it is not working to set the IP DSCP, with and without setting IPv4
                    //Action setIp = SfcOpenflowUtils.createActionSetEtherType(SfcOpenflowUtils.ETHERTYPE_IPV4, order++);
                    Action writeDscp = SfcOpenflowUtils.createActionWriteDscp((short) this.pathId, order++);
                    //actionList.add(setIp);
                    actionList.add(writeDscp);
                }

                // Optionally set either the VLAN or MPLS info
                if(dstVlan > 0) {
                    Action vlanPush = SfcOpenflowUtils.createActionPushVlan(order++);
                    actionList.add(vlanPush);

                    Action vlanDst = SfcOpenflowUtils.createActionSetVlanId(this.dstVlan, order++);
                    actionList.add(vlanDst);
                } else if(mplsLabel > 0) {
                    Action mplsPush = SfcOpenflowUtils.createActionPushMpls(order++);
                    actionList.add(mplsPush);

                    Action setMpls = SfcOpenflowUtils.createActionSetMplsLabel(this.mplsLabel, order++);
                    actionList.add(setMpls);
                } else if (this.dstNsp >=0 && this.dstNsi >= 0) {
                    actionList.add(SfcOpenflowUtils.createActionNxSetNsp(this.dstNsp, order++));
                    actionList.add(SfcOpenflowUtils.createActionNxSetNsi(this.dstNsi, order++));
                }

                Action outPortBuilder = SfcOpenflowUtils.createActionOutPort(this.port, order);
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

                FlowBuilder egressTransportFlow =
                        SfcOpenflowUtils.createFlowBuilder(
                                TABLE_INDEX_TRANSPORT_EGRESS,
                                flowPriority,
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

            } catch (Exception ex) {
                LOG.error("Caught an exception in ConfigureTransportEgressThread.run() : {}", ex.toString());
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

        LOG.info("writeFlowToConfig writing flow to Node {}, table {}", sffNodeName, flow.getTableId());

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
}

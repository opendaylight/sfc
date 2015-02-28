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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
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

    // Which bits in the metadata field to set, used for the PathId and allows 4095 sfpid's
    // TODO check how many sfpid's there can be
    private static final BigInteger METADATA_MASK_SFP_MATCH = new BigInteger("000000000000FFFF", COOKIE_BIGINT_HEX_RADIX);

    private static final int L4_PORT_MASK = 0x0000FFFF;
    private static final int L4_PROTOCOL_TCP = 6;

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
    private static final int FLOW_PRIORITY_MATCH_ANY = 5000;

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

                InstructionBuilder ib = null;
                if(this.doDrop) {
                    // Add our drop action to a list
                    List<Action> actionList = new ArrayList<Action>();
                    actionList.add(SfcOpenflowUtils.createActionDropPacket(0));

                    // Create an Apply Action
                    ApplyActionsBuilder aab = new ApplyActionsBuilder();
                    aab.setAction(actionList);

                    // Wrap our Apply Action in an Instruction
                    ib = new InstructionBuilder();
                    ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                } else {
                    //
                    // Action, goto Ingress table
                    GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(this.nextTableId);

                    ib = new InstructionBuilder();
                    ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());
                }

                // Match any
                MatchBuilder match = new MatchBuilder();

                ib.setKey(new InstructionKey(1));
                ib.setOrder(0);
                InstructionsBuilder isb = SfcOpenflowUtils.createInstructionsBuilder(ib);

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
        ConfigureTransportIngressThread configureIngressTransportThread =
                new ConfigureTransportIngressThread(sffNodeName, SfcOpenflowUtils.ETHERTYPE_IPV4, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureIngressTransportThread);
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

        public ConfigureTransportIngressThread(final String sffNodeName, long etherType, final boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.etherType = etherType;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {
                LOG.info("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow, sff [{}] etherType [{}]",
                        this.sffNodeName, this.etherType);

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();
                SfcOpenflowUtils.addMatchEtherType(match, this.etherType);

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

    private class ConfigureIngressFlowThread implements Runnable {
        String sffNodeName;
        long pathId;
        int vlan;
        long mplsLabel;
        String macAddress;
        boolean isAddFlow;

        public void setVlanId(final int vlan) { this.vlan = vlan; }
        public void setMplsLabel(final long label) { this.mplsLabel = label; }
        public void setMacAddress(final String macAddress) { this.macAddress = macAddress; }

        public ConfigureIngressFlowThread(final String sffNodeName, final long pathId, final boolean isAddFlow) {
            this.sffNodeName = sffNodeName;
            this.pathId = pathId;
            this.isAddFlow = isAddFlow;
            this.vlan = -1; // not set
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
                }

                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                int ibOrder = 0;
                InstructionBuilder metadataIb = new InstructionBuilder();
                metadataIb.setInstruction(SfcOpenflowUtils.createInstructionMetadata(
                        actionOrder++, getMetadataSFP(this.pathId), METADATA_MASK_SFP_MATCH));
                metadataIb.setKey(new InstructionKey(ibOrder));
                metadataIb.setOrder(ibOrder++);

                InstructionBuilder actionsIb = new InstructionBuilder();
                actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                actionsIb.setKey(new InstructionKey(ibOrder));
                actionsIb.setOrder(ibOrder++);

                GoToTableBuilder gotoNextHop = SfcOpenflowUtils.createActionGotoTable(getTableId(TABLE_INDEX_CLASSIFICATION));
                InstructionBuilder gotoNextHopIb = new InstructionBuilder();
                gotoNextHopIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoNextHop.build()).build());
                gotoNextHopIb.setKey(new InstructionKey(ibOrder));
                gotoNextHopIb.setOrder(ibOrder++);

                // Put our Instruction in a list of Instructions
                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(metadataIb.build());
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

    // TODO some of the 5tuple entries may be optional, need to add logic to
    // writeSffAcl() to not write them if not specified
    public void configureClassificationFlow(final String sffNodeName, final String srcIp, final short srcMask, final String dstIp,
            final short dstMask, final short srcPort, final short dstPort, final byte protocol, final long sfpId,
            final boolean isAddFlow) {

        ConfigureClassificationFlowThread configureClassificationFlowThread =
                new ConfigureClassificationFlowThread(
                        sffNodeName, srcIp, srcMask, dstIp, dstMask, srcPort, dstPort, protocol, sfpId, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureClassificationFlowThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureClassificationFlowThread implements Runnable {
        String sffNodeName;
        String srcIp;
        short srcMask;
        String dstIp;
        short dstMask;
        short srcPort;
        short dstPort;
        byte protocol;
        long sfpId;
        boolean isAddFlow;

        public ConfigureClassificationFlowThread(final String sffNodeName, final String srcIp, final short srcMask, final String dstIp,
                final short dstMask, final short srcPort, final short dstPort, final byte protocol, final long sfpId,
                final boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.srcIp = srcIp;
            this.srcMask = srcMask;
            this.dstIp = dstIp;
            this.dstMask = dstMask;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
            this.protocol = protocol;
            this.sfpId = sfpId;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {

                LOG.info("SfcProviderSffFlowWriter.writeSffAcl() SFPid {}", sfpId);

                boolean isIpMatch = false;
                boolean isPortMatch = false;
                //
                // Create the 5-tuple matching criteria

                // To do an IP match, the EtherType needs to be set to
                // 0x0800 which indicates IP
                MatchBuilder match = new MatchBuilder();
                SfcOpenflowUtils.addMatchEtherType(match, Long.valueOf(SfcOpenflowUtils.ETHERTYPE_IPV4));

                Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
                if (srcIp != null) {
                    ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(SfcOpenflowUtils.longToIp(srcIp, srcMask)));
                    isIpMatch = true;
                }
                if (dstIp != null) {
                    ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(SfcOpenflowUtils.longToIp(dstIp, dstMask)));
                    isIpMatch = true;
                }
                if (isIpMatch) {
                    match.setLayer3Match(ipv4MatchBuilder.build());
                }

                IpMatchBuilder ipmatch = new IpMatchBuilder();
                if (protocol != 0) {
                    ipmatch.setIpProtocol((short) protocol);
                    match.setIpMatch(ipmatch.build());

                    if (protocol == L4_PROTOCOL_TCP) {
                        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
                        // There must be a bug in setTcpSource/DestinationPort(),
                        // because its looking at the upper 2 bytes of the port
                        // and thinks its out of range
                        if (srcPort != 0) {
                            tcpMatch.setTcpSourcePort(new PortNumber(Integer.valueOf(L4_PORT_MASK & srcPort)));
                            isPortMatch = true;
                        }
                        if (dstPort != 0) {
                            tcpMatch.setTcpDestinationPort(new PortNumber(Integer.valueOf(L4_PORT_MASK & dstPort)));
                            isPortMatch = true;
                        }
                        if (isPortMatch) {
                            match.setLayer4Match(tcpMatch.build());
                        }
                    } else {
                        UdpMatchBuilder udpMatch = new UdpMatchBuilder();
                        if (srcPort != 0) {
                            udpMatch.setUdpSourcePort(new PortNumber(Integer.valueOf(L4_PORT_MASK & srcPort)));
                            isPortMatch = true;
                        }
                        if (dstPort != 0) {
                            udpMatch.setUdpDestinationPort(new PortNumber(Integer.valueOf(L4_PORT_MASK & dstPort)));
                            isPortMatch = true;
                        }
                        if (isPortMatch) {
                            match.setLayer4Match(udpMatch.build());
                        }
                    }
                }

                //
                // Create the Actions

                // Create the Metadata action and wrap it in an
                // InstructionBuilder
                // Set the bits specified by METADATA_BITS with the bucket
                // value
                WriteMetadataBuilder wmb = new WriteMetadataBuilder();
                wmb.setMetadata(getMetadataSFP(sfpId));
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

        ConfigureNextHopFlowThread configureNextHopFlowThread =
                new ConfigureNextHopFlowThread(sffNodeName, sfpId, srcMac, dstMac, isAddFlow);
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
        boolean isAddFlow;

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
                LOG.info("SfcProviderSffFlowWriter.configureNextHopFlow sfpId [{}] srcMac [{}] dstMac [{}]",
                        this.sffNodeName, this.sfpId, this.srcMac, this.dstMac);

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();

                // Match on the metadata sfpId
                SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);

                // match on the src mac
                SfcOpenflowUtils.addMatchSrcMac(match, srcMac);

                //
                // Create the Actions

                // Set the DL (Data Link) Dest Mac Address
                Action actionDstMac = SfcOpenflowUtils.createActionSetDlDst(dstMac, 0);

                List<Action> actionList = new ArrayList<Action>();
                actionList.add(actionDstMac);

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
                                FLOW_PRIORITY_NEXT_HOP,
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
    public void configureMacTransportEgressFlow(final String sffNodeName, final String dstMac, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, dstMac, isAddFlow);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureVlanTransportEgressFlow(final String sffNodeName, final String dstMac, final int dstVlan, final boolean isAddFlow) {
        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, dstMac, isAddFlow);
        configureEgressTransportThread.setDstVlan(dstVlan);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    public void configureMplsTransportEgressFlow(final String sffNodeName, final String dstMac, final long mplsLabel, final boolean isAddFlow) {

        ConfigureTransportEgressThread configureEgressTransportThread =
                new ConfigureTransportEgressThread(sffNodeName, dstMac, isAddFlow);
        configureEgressTransportThread.setMplsLabel(mplsLabel);
        try {
            threadPoolExecutorService.execute(configureEgressTransportThread);
        } catch (Exception ex) {
            LOG.error(LOGSTR_THREAD_QUEUE_FULL, ex.toString());
        }
    }

    private class ConfigureTransportEgressThread implements Runnable {
        String sffNodeName;
        String dstMac;
        int dstVlan;
        long mplsLabel;
        boolean isAddFlow;

        public ConfigureTransportEgressThread(final String sffNodeName, String dstMac, boolean isAddFlow) {
            super();
            this.sffNodeName = sffNodeName;
            this.dstMac = dstMac;
            this.dstVlan = -1;   // unused
            this.mplsLabel = -1; // unused
            this.isAddFlow = isAddFlow;
        }
        public void setDstVlan(final int dstVlan) { this.dstVlan = dstVlan; }
        public void setMplsLabel(final long mplsLabel) { this.mplsLabel = mplsLabel; }

        @Override
        public void run() {
            try {
                LOG.info("SfcProviderSffFlowWriter.ConfigureTransportEgressFlow sff [{}] mac [{}] vlan [{}] mpls [{}]",
                        this.sffNodeName, this.dstMac, this.dstVlan, this.mplsLabel);

                MatchBuilder match = new MatchBuilder();
                SfcOpenflowUtils.addMatchDstMac(match, dstMac);

                int order = 0;
                List<Action> actionList = new ArrayList<Action>();

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
                }

                // TODO we need to parameterize the out port "1"
                Action outPortBuilder = SfcOpenflowUtils.createActionOutPort("1", order);
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
                                FLOW_PRIORITY_TRANSPORT_EGRESS,
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

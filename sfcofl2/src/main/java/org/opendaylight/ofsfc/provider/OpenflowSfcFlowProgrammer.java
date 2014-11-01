/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ofsfc.provider;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigInteger;

import com.google.common.base.Joiner;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.ofsfc.provider.utils.SfcOfL2Constants;
import org.opendaylight.ofsfc.provider.utils.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;

/**
 * This class writes Flow Entries to the SFF once an SFF has been configured.
 * <p>
 * 
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * @since 2014-08-07
 */
public class OpenflowSfcFlowProgrammer {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowSfcFlowProgrammer.class);
    private static OpenflowSfcFlowProgrammer instance = null;
    private final AtomicInteger m_atomicInteger = new AtomicInteger();

    private final AtomicLong flowIdInc = new AtomicLong();
    private short tableBase = (short) 0;
    private final DataBroker dataBroker;
    private boolean isReady;
    private String sffNodeName;

    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 50;
    private static final int ASYNC_THREAD_POOL_CORE_SIZE = 1;
    private static final int ASYNC_THREAD_POOL_MAX_SIZE = 1;
    private static final int ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;

    public static final String FLOWID_PREFIX = "SFC";
    public static final String FLOWID_SEPARATOR = ".";

    private ExecutorService s_ThreadPoolExecutorService = new ThreadPoolExecutor(SCHEDULED_THREAD_POOL_SIZE,
            SCHEDULED_THREAD_POOL_SIZE, ASYNC_THREAD_POOL_KEEP_ALIVE_TIME_SECS, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));

    public static void createFlowProgrammer(DataBroker dataBroker) {
        if (instance == null) {
            instance = new OpenflowSfcFlowProgrammer(dataBroker);
        }
    }

    public static OpenflowSfcFlowProgrammer getInstance() {
        // TODO make it threadsafe
        return instance;
    }

    public OpenflowSfcFlowProgrammer(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        isReady = false;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setNodeInfo(String sffNodeName) {
        this.sffNodeName = sffNodeName;
        isReady = true;
    }

    public short getTableBase() {
        return tableBase;
    }

    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    /**
     * getTableId
     * 
     * Having a TableBase allows us to "offset" the SFF tables by this.tableBase
     * tables Doing so allows for OFS tab6es previous to the SFF tables.
     * tableIndex should be one of: TABLE_INDEX_SFF_ACL or
     * TABLE_INDEX_SFF_OUTPUT
     */
    private short getTableId(short tableIndex) {
        return (short) (tableBase + tableIndex);
    }

    public void configureClassificationFlow(final String srcIp, final short srcMask, final String dstIp,
            final short dstMask, final short srcPort, final short dstPort, final byte protocol, final long sfpId,
            final boolean isAddFlow) {

        ConfigureClassificationFlowThread configureClassificationFlowThread = new ConfigureClassificationFlowThread(
                srcIp, srcMask, dstIp, dstMask, srcPort, dstPort, protocol, sfpId, isAddFlow);
        try {
            s_ThreadPoolExecutorService.execute(configureClassificationFlowThread);
        } catch (Exception ex) {
            LOG.error("Queue size is full");
        }
    }

    private class ConfigureClassificationFlowThread implements Runnable {
        String srcIp;
        short srcMask;
        String dstIp;
        short dstMask;
        short srcPort;
        short dstPort;
        byte protocol;
        long sfpId;
        boolean isAddFlow;

        public ConfigureClassificationFlowThread(final String srcIp, final short srcMask, final String dstIp,
                final short dstMask, final short srcPort, final short dstPort, final byte protocol, final long sfpId,
                final boolean isAddFlow) {
            super();
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
                if (!isReady) {
                    LOG.error("{} NOT ready to write yet", Thread.currentThread().getStackTrace()[0]);
                    return;
                }

                LOG.debug(" OpenflowSfcFlowProgrammer.configureClassificationFlow() SFPid {}", sfpId);

                boolean isIpMatch = false;
                boolean isPortMatch = false;
                // Create the 5-tuple matching criteria

                EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
                ethTypeBuilder.setType(new EtherType(0x0800L));
                EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
                ethMatchBuilder.setEthernetType(ethTypeBuilder.build());
                MatchBuilder match = new MatchBuilder();
                match.setEthernetMatch(ethMatchBuilder.build());

                Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
                if (srcIp != null) {
                    ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(longToIp(srcIp, srcMask)));
                    isIpMatch = true;
                }
                if (dstIp != null) {
                    ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(longToIp(dstIp, dstMask)));
                    isIpMatch = true;
                }
                if (isIpMatch == true) {
                    match.setLayer3Match(ipv4MatchBuilder.build());
                }

                IpMatchBuilder ipmatch = new IpMatchBuilder();
                if (protocol != 0) {
                    ipmatch.setIpProtocol((short) protocol);
                    match.setIpMatch(ipmatch.build());

                    if (protocol == 6) {
                        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
                        // There must be a bug in
                        // setTcpSource/DestinationPort(), because its
                        // looking at the upper 2 bytes of the port and thinks
                        // its out of range
                        if (srcPort != 0) {
                            tcpMatch.setTcpSourcePort(new PortNumber(new Integer(0x0000FFFF & srcPort)));
                            isPortMatch = true;
                        }
                        if (dstPort != 0) {
                            tcpMatch.setTcpDestinationPort(new PortNumber(new Integer(0x0000FFFF & dstPort)));
                            isPortMatch = true;
                        }
                        if (isPortMatch == true) {
                            match.setLayer4Match(tcpMatch.build());
                        }
                    } else {
                        UdpMatchBuilder udpMatch = new UdpMatchBuilder();
                        if (srcPort != 0) {
                            udpMatch.setUdpSourcePort(new PortNumber(new Integer(0x0000FFFF & srcPort)));
                            isPortMatch = true;
                        }
                        if (dstPort != 0) {
                            udpMatch.setUdpDestinationPort(new PortNumber(new Integer(0x0000FFFF & dstPort)));
                            isPortMatch = true;
                        }
                        if (isPortMatch == true) {
                            match.setLayer4Match(udpMatch.build());
                        }
                    }
                }

                // Create the Actions

                WriteMetadataBuilder wmb = new WriteMetadataBuilder();
                wmb.setMetadata(SfcOpenflowUtils.getMetadataSFP(sfpId));
                wmb.setMetadataMask(SfcOfL2Constants.METADATA_BASE_MASK);

                InstructionBuilder wmbIb = new InstructionBuilder();
                wmbIb.setInstruction(new WriteMetadataCaseBuilder().setWriteMetadata(wmb.build()).build());
                wmbIb.setKey(new InstructionKey(0));
                wmbIb.setOrder(0);

                // Create the Goto Table Action and wrap it in an
                // InstructionBuilder
                GoToTableBuilder gotoTb = new GoToTableBuilder();
                gotoTb.setTableId(getTableId(getTableId(SfcOfL2Constants.TABLE_INDEX_NEXT_HOP)));

                InstructionBuilder gotoTbIb = new InstructionBuilder();
                gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
                gotoTbIb.setKey(new InstructionKey(1));
                gotoTbIb.setOrder(1);

                // Put the Instructions in a list of Instructions
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(gotoTbIb.build());
                instructions.add(wmbIb.build());

                InstructionsBuilder isb = new InstructionsBuilder();
                isb.setInstruction(instructions);

                // Create and configure the FlowBuilder
                FlowBuilder aclFlow = SfcOpenflowUtils
                        .buildFlow(getFlowRef(srcIp, srcMask, dstIp, dstMask, srcPort, dstPort, protocol, sfpId),
                                getTableId(SfcOfL2Constants.TABLE_INDEX_CLASSIFICATION),
                                SfcOfL2Constants.FLOW_CLASSIFICATION, SfcOpenflowUtils.getCookieSFP(sfpId), match, isb,
                                SfcOfL2Constants.FLOW_PRIORITY_CLASSIFICATION);

                addOrRemoveFlow(isAddFlow, aclFlow);
            } catch (Exception e) {
                LOG.error("OpenflowSfcFlowProgrammer.configureClassificationFlow() caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void configureSffNextHopDefaultFlow(final boolean isAddFlow) {

        ConfigureSffNextHopDefaultFlowThread configureSffNextHopDefaultFlowThread = new ConfigureSffNextHopDefaultFlowThread(
                isAddFlow);
        try {
            s_ThreadPoolExecutorService.execute(configureSffNextHopDefaultFlowThread);

        } catch (Exception ex) {
            LOG.error("Queue size is full");
        }
    }

    private class ConfigureSffNextHopDefaultFlowThread implements Runnable {
        boolean isAddFlow;

        public ConfigureSffNextHopDefaultFlowThread(final boolean isAddFlow) {
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {

                if (!isReady) {
                    LOG.error("{} NOT ready to write yet", Thread.currentThread().getStackTrace()[0]);
                    return;
                }
                LOG.debug(" OpenflowSfcFlowProgrammer.configureSffNextHopDefaultFlow default entry ");

                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();

                GoToTableBuilder gotoTb = new GoToTableBuilder();
                gotoTb.setTableId(getTableId(getTableId(SfcOfL2Constants.TABLE_INDEX_FIRST_HOP)));

                InstructionBuilder gotoTbIb = new InstructionBuilder();
                gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
                gotoTbIb.setKey(new InstructionKey(1));
                gotoTbIb.setOrder(0);

                // Put our Instruction in a list of Instructions
                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(gotoTbIb.build());
                isb.setInstruction(instructions);

                // Create and configure the FlowBuilder
                FlowBuilder defNextHopFlow = SfcOpenflowUtils.buildFlow(
                        getFlowRef(SfcOfL2Constants.TABLE_INDEX_NEXT_HOP),
                        getTableId(SfcOfL2Constants.TABLE_INDEX_NEXT_HOP), SfcOfL2Constants.FLOW_NEXHOP_DEFAULT,
                        SfcOpenflowUtils.getCookieDefault(SfcOfL2Constants.TABLE_INDEX_NEXT_HOP), match, isb,
                        SfcOfL2Constants.FLOW_PRIORITY_DEFAULT_NEXT_HOP);

                addOrRemoveFlow(isAddFlow, defNextHopFlow);
            } catch (Exception e) {
                LOG.error("SfcProviderSffFlowWriter.writeSffNextHop() caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }

        }
    }

    public void configureNextHopFlow(final long sfpId, final String srcMac, final String dstMac, final int dstVlan,
            final boolean isAddFlow) {

        ConfigureNextHopFlowThread configureNextHopFlowThread = new ConfigureNextHopFlowThread(sfpId, srcMac, dstMac,
                dstVlan, isAddFlow);
        try {
            s_ThreadPoolExecutorService.execute(configureNextHopFlowThread);

        } catch (Exception ex) {
            LOG.error("Queue size is full");
        }
    }

    private class ConfigureNextHopFlowThread implements Runnable {
        long sfpId;
        String srcMac;
        String dstMac;
        int dstVlan;
        boolean isAddFlow;

        public ConfigureNextHopFlowThread(final long sfpId, final String srcMac, final String dstMac,
                final int dstVlan, final boolean isAddFlow) {
            super();
            this.sfpId = sfpId;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.dstVlan = dstVlan;
            this.isAddFlow = isAddFlow;

        }

        @Override
        public void run() {
            try {
                if (!isReady) {
                    LOG.error("{} NOT ready to write yet", Thread.currentThread().getStackTrace()[0]);
                    return;
                }
                LOG.debug(" OpenflowSfcFlowProgrammer.configureSffNextHopFlow");

                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();

                // Match on the metadata sfpId
                MetadataBuilder metadata = new MetadataBuilder();
                metadata.setMetadata(SfcOpenflowUtils.getMetadataSFP(sfpId));
                metadata.setMetadataMask(SfcOfL2Constants.METADATA_MASK_SFP_MATCH);
                match.setMetadata(metadata.build());

                EthernetTypeBuilder ethtype = new EthernetTypeBuilder();
                EtherType type = new EtherType(0x0800L);

                // match the src mac
                EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
                EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
                ethSourceBuilder.setAddress(new MacAddress(srcMac));
                // ethernetMatch.setEthernetType(ethtype.setType(type).build());
                ethernetMatch.setEthernetSource(ethSourceBuilder.build());
                match.setEthernetMatch(ethernetMatch.build());

                // Create the Actions
                Action actionDstMac = SfcOpenflowUtils.createSetDlDstAction(dstMac, 0);
                Action actionPushVlan = SfcOpenflowUtils.createPushVlanAction(1);
                Action actionDstVlan = SfcOpenflowUtils.createSetDstVlanAction(dstVlan, 2);

                List<Action> actionList = new ArrayList<Action>();

                actionList.add(actionDstMac);
                actionList.add(actionPushVlan);
                actionList.add(actionDstVlan);
                // Create an Apply Action
                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                GoToTableBuilder gotoTb = new GoToTableBuilder();
                gotoTb.setTableId(getTableId(getTableId(SfcOfL2Constants.TABLE_INDEX_TRANSPORT_EGRESS)));

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
                FlowBuilder nextHopFlow = SfcOpenflowUtils.buildFlow(getFlowRef(sfpId, srcMac, dstMac, dstVlan),
                        getTableId(SfcOfL2Constants.TABLE_INDEX_NEXT_HOP), SfcOfL2Constants.FLOW_NEXTHOP,
                        SfcOpenflowUtils.getCookieSFP(sfpId), match, isb, SfcOfL2Constants.FLOW_PRIORITY_NEXT_HOP);

                if (isAddFlow == true) {
                    writeFlowToConfig(nextHopFlow);
                } else {
                    removeFlowFromConfig(nextHopFlow);
                }
            } catch (Exception e) {
                LOG.error(" OpenflowSfcFlowProgrammer.configureSffNextHopFlow() caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void configureIngressFlow(final int vlan, final boolean isAddFlow) {

        ConfigureIngressFlowThread configureIngressFlowThread = new ConfigureIngressFlowThread(vlan, isAddFlow);
        try {
            s_ThreadPoolExecutorService.execute(configureIngressFlowThread);

        } catch (Exception ex) {
            LOG.error("Queue size is full");
        }
    }

    private class ConfigureIngressFlowThread implements Runnable {
        int vlan;
        boolean isAddFlow;

        public ConfigureIngressFlowThread(final int vlan, final boolean isAddFlow) {
            this.vlan = vlan;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {
                if (!isReady) {
                    LOG.error("{} NOT ready to write yet", Thread.currentThread().getStackTrace()[0]);
                    return;
                }

                LOG.debug(" OpenflowSfcFlowProgrammer.configureIngressFlow");
                MatchBuilder matchBuilder = new MatchBuilder();
                matchBuilder.setVlanMatch(SfcOpenflowUtils.createVlanMatch(vlan));

                Action actionPopVlan = SfcOpenflowUtils.createPopVlanAction(0);
                List<Action> actionList = new ArrayList<Action>();
                actionList.add(actionPopVlan);
                // Create an Apply Action
                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                InstructionBuilder instructionBuilder = new InstructionBuilder();
                instructionBuilder.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                instructionBuilder.setKey(new InstructionKey(0));
                instructionBuilder.setOrder(0);

                GoToTableBuilder gotoTb = new GoToTableBuilder();
                gotoTb.setTableId(getTableId(getTableId(SfcOfL2Constants.TABLE_INDEX_CLASSIFICATION)));

                InstructionBuilder gotoTbIb = new InstructionBuilder();
                gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
                gotoTbIb.setKey(new InstructionKey(1));
                gotoTbIb.setOrder(0);

                // Put our Instruction in a list of Instructions
                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(instructionBuilder.build());
                instructions.add(gotoTbIb.build());
                isb.setInstruction(instructions);

                //
                // Create and configure the FlowBuilder
                FlowBuilder ingressFlow = SfcOpenflowUtils.buildFlow(getFlowRef(vlan),
                        getTableId(SfcOfL2Constants.TABLE_INDEX_INGRESS), SfcOfL2Constants.FLOW_INGRESS,
                        SfcOpenflowUtils.getCookieIngress(vlan), matchBuilder, isb,
                        SfcOfL2Constants.FLOW_PRIORITY_INGRESS);

                addOrRemoveFlow(isAddFlow, ingressFlow);
            } catch (Exception e) {
                LOG.error("OpenflowSfcFlowProgrammer.configureIngressFlow() caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void configureDefaultNextHopFlow(final long sfpId, final String dstMac, final int dstVlan,
            final boolean isAddFlow) {

        ConfigureDefaultNextHopFlowThread configureDefaultNextHopFlowThread = new ConfigureDefaultNextHopFlowThread(
                sfpId, dstMac, dstVlan, isAddFlow);
        try {
            s_ThreadPoolExecutorService.execute(configureDefaultNextHopFlowThread);

        } catch (Exception ex) {
            LOG.error("Queue size is full");
        }
    }

    private class ConfigureDefaultNextHopFlowThread implements Runnable {
        long sfpId;
        String dstMac;
        int dstVlan;
        boolean isAddFlow;

        public ConfigureDefaultNextHopFlowThread(final long sfpId, final String dstMac, final int dstVlan,
                final boolean isAddFlow) {
            this.sfpId = sfpId;
            this.dstMac = dstMac;
            this.dstVlan = dstVlan;
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {
                if (!isReady) {
                    LOG.error("{} NOT ready to write yet", Thread.currentThread().getStackTrace()[0]);
                    return;
                }
                LOG.debug(" OpenflowSfcFlowProgrammer first nexthop");

                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();

                // Match on the metadata sfpId
                MetadataBuilder metadata = new MetadataBuilder();
                metadata.setMetadata(SfcOpenflowUtils.getMetadataSFP(sfpId));
                metadata.setMetadataMask(SfcOfL2Constants.METADATA_MASK_SFP_MATCH);
                match.setMetadata(metadata.build());

                EthernetMatchBuilder ethmatch = new EthernetMatchBuilder();
                EthernetTypeBuilder ethtype = new EthernetTypeBuilder();
                EtherType type = new EtherType(0x0800L);
                // ethmatch.setEthernetType(ethtype.setType(type).build());

                match.setEthernetMatch(ethmatch.build());
                // Create the Actions

                // Set the DL (Data Link) Dest Mac Address
                Action actionDstMac = SfcOpenflowUtils.createSetDlDstAction(dstMac, 0);
                Action actionPushVlan = SfcOpenflowUtils.createPushVlanAction(1);
                Action actionDstVlan = SfcOpenflowUtils.createSetDstVlanAction(dstVlan, 2);

                List<Action> actionList = new ArrayList<Action>();

                actionList.add(actionDstMac);
                actionList.add(actionPushVlan);
                actionList.add(actionDstVlan);

                // Create an Apply Action
                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                // Wrap our Apply Action in an Instruction
                InstructionBuilder ib = new InstructionBuilder();
                ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                ib.setKey(new InstructionKey(0));
                ib.setOrder(0);

                GoToTableBuilder gotoTb = new GoToTableBuilder();
                gotoTb.setTableId(getTableId(getTableId(SfcOfL2Constants.TABLE_INDEX_TRANSPORT_EGRESS)));

                InstructionBuilder gotoTbIb = new InstructionBuilder();
                gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
                gotoTbIb.setKey(new InstructionKey(1));
                gotoTbIb.setOrder(1);

                // Put our Instruction in a list of Instructions
                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(ib.build());
                instructions.add(gotoTbIb.build());
                isb.setInstruction(instructions);

                //
                // Create and configure the FlowBuilder
                FlowBuilder nextHopFlow = SfcOpenflowUtils.buildFlow(getFlowRef(sfpId, dstMac, dstVlan),
                        getTableId(SfcOfL2Constants.TABLE_INDEX_FIRST_HOP), SfcOfL2Constants.FLOW_FIRST_HOP,
                        SfcOpenflowUtils.getCookieSFP(sfpId), match, isb, SfcOfL2Constants.FLOW_PRIORITY_NEXT_HOP);

                addOrRemoveFlow(isAddFlow, nextHopFlow);
            } catch (Exception e) {
                LOG.error(" OpenflowSfcFlowProgrammer first nexthop caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void configureEgressTransportFlow(final String dstMac, final int dstVlan, int ofPort, final boolean isAddFlow) {

        ConfigureEgressTransportThread configureEgressTransportThread = new ConfigureEgressTransportThread(dstMac,
                dstVlan, ofPort, isAddFlow);
        try {
            s_ThreadPoolExecutorService.execute(configureEgressTransportThread);

        } catch (Exception ex) {
            LOG.error("Queue size is full");
        }

    }

    private class ConfigureEgressTransportThread implements Runnable {

        String dstMac;
        int dstVlan;
        int ofPort;
        boolean isAddFlow;

        public ConfigureEgressTransportThread(String dstMac, int dstVlan, int ofPort, boolean isAddFlow) {
            super();
            this.dstMac = dstMac;
            this.dstVlan = dstVlan;
            this.ofPort = ofPort;
            this.isAddFlow = isAddFlow;

        }

        @Override
        public void run() {
            try {

                if (!isReady) {
                    LOG.error("{} NOT ready to write yet", Thread.currentThread().getStackTrace()[0]);
                    return;
                }

                LOG.debug("OpenflowSfcFlowProgrammer configureEgressTransport");

                MatchBuilder matchBuilder = new MatchBuilder();
                matchBuilder.setVlanMatch(SfcOpenflowUtils.createVlanMatch(dstVlan));

                EthernetTypeBuilder ethtype = new EthernetTypeBuilder();
                EtherType type = new EtherType(0x8100L);

                // match the dst mac
                EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
                EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
                ethDestinationBuilder.setAddress(new MacAddress(dstMac));
                // ethernetMatch.setEthernetType(ethtype.setType(type).build());
                ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
                matchBuilder.setEthernetMatch(ethernetMatch.build());

                List<Action> actionList = new ArrayList<Action>();
                ActionBuilder ab = new ActionBuilder();
                OutputActionBuilder output = new OutputActionBuilder();

                Uri value = new Uri(new Integer(ofPort).toString());
                output.setOutputNodeConnector(value);
                ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
                ab.setOrder(0);
                ab.setKey(new ActionKey(0));
                actionList.add(ab.build());
                // Create an Apply Action
                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);

                // Wrap our Apply Action in an Instruction
                InstructionBuilder ib = new InstructionBuilder();
                ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                ib.setOrder(0);
                ib.setKey(new InstructionKey(0));

                // Put our Instruction in a list of Instructions
                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(ib.build());
                isb.setInstruction(instructions);

                FlowBuilder egressTransportFlow = SfcOpenflowUtils.buildFlow(getFlowRef(dstMac, dstVlan),
                        getTableId(SfcOfL2Constants.TABLE_INDEX_TRANSPORT_EGRESS),
                        SfcOfL2Constants.FLOW_TRANSPORT_EGRESS, new BigInteger("20", 10), matchBuilder, isb,
                        SfcOfL2Constants.FLOW_PRIORITY_TRANSPORT);

                addOrRemoveFlow(isAddFlow, egressTransportFlow);

            } catch (Exception ex) {
                LOG.error(" OpenflowSfcFlowProgrammer configureEgressTransport caught an Exception: ");
                LOG.error(ex.getMessage(), ex);
            }
        }
    }

    public void configureIngressTransportFlow(final boolean isAddFlow) {

        ConfigureIngressTransportThread configureIngressTransportThread = new ConfigureIngressTransportThread(isAddFlow);
        try {
            s_ThreadPoolExecutorService.execute(configureIngressTransportThread);

        } catch (Exception ex) {
            LOG.error("Queue size is full");
        }
    }

    private class ConfigureIngressTransportThread implements Runnable {
        boolean isAddFlow;

        public ConfigureIngressTransportThread(final boolean isAddFlow) {
            this.isAddFlow = isAddFlow;
        }

        @Override
        public void run() {
            try {

                if (!isReady) {
                    LOG.error("{} NOT ready to write yet", Thread.currentThread().getStackTrace()[0]);
                    return;
                }
                LOG.debug("OpenflowSfcFlowProgrammer configureIngressTransport");

                //
                // Create the matching criteria
                MatchBuilder match = new MatchBuilder();
                // Set the DL (Data Link) Dest Mac Address

                GoToTableBuilder gotoTb = new GoToTableBuilder();
                gotoTb.setTableId(SfcOfL2Constants.TABLE_INDEX_INGRESS);

                InstructionBuilder gotoTbIb = new InstructionBuilder();
                gotoTbIb.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoTb.build()).build());
                gotoTbIb.setKey(new InstructionKey(1));
                gotoTbIb.setOrder(0);

                // Put our Instruction in a list of Instructions
                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> instructions = new ArrayList<Instruction>();
                instructions.add(gotoTbIb.build());
                isb.setInstruction(instructions);

                //
                // Create and configure the FlowBuilder
                FlowBuilder ingressTransportFlow = SfcOpenflowUtils.buildFlow(
                        getFlowRef(SfcOfL2Constants.TABLE_INDEX_INGRESS_TRANSPORT_TABLE),
                        SfcOfL2Constants.TABLE_INDEX_INGRESS_TRANSPORT_TABLE, SfcOfL2Constants.FLOW_INGRESS,
                        new BigInteger("20", 10), match, isb, SfcOfL2Constants.FLOW_PRIORITY_TRANSPORT);

                addOrRemoveFlow(isAddFlow, ingressTransportFlow);
            } catch (Exception e) {
                LOG.error("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop() caught an Exception: ");
                LOG.error(e.getMessage(), e);
            }

        }
    }

    private void addOrRemoveFlow(boolean isAddFlow, FlowBuilder flow) {
        if (isAddFlow == true) {
            writeFlowToConfig(flow);
        } else {
            removeFlowFromConfig(flow);
        }
    }

    private void removeFlowFromConfig(FlowBuilder flow) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path
        InstanceIdentifier<Flow> flowPath = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flow.getKey()).build();
        InstanceIdentifier<Node> nodePath = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).toInstance();

        InstanceIdentifier<Table> tableInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).build();

        String sTransactionUri = generateTransactionUri();
        RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(flow.build());
        builder.setTransactionUri(new Uri(sTransactionUri));
        builder.setNode(new NodeRef(nodePath));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setFlowRef(new FlowRef(flowPath));
        builder.setStrict(true);
        OpenflowSfcRenderer.getOpendaylightSfcObj().getRpcProvider().getRpcService(SalFlowService.class)
                .removeFlow(builder.build());
    }

    private void writeFlowToConfig(FlowBuilder flow) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path
        InstanceIdentifier<Flow> flowPath = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flow.getKey()).build();
        InstanceIdentifier<Node> nodePath = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).toInstance();

        InstanceIdentifier<Table> tableInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).build();

        String sTransactionUri = generateTransactionUri();
        AddFlowInputBuilder builder = new AddFlowInputBuilder(flow.build());

        builder.setTransactionUri(new Uri(sTransactionUri));
        builder.setNode(new NodeRef(nodePath));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setFlowRef(new FlowRef(flowPath));

        OpenflowSfcRenderer.getOpendaylightSfcObj().getRpcProvider().getRpcService(SalFlowService.class)
                .addFlow(builder.build());
    }

    private String getFlowRef(final String srcIp, final short srcMask, final String dstIp, final short dstMask,
            final short srcPort, final short dstPort, final byte protocol, final long sfpId) {
        return new StringBuilder(256).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(sfpId)
                .append(FLOWID_SEPARATOR).append(srcIp).append(FLOWID_SEPARATOR).append(srcMask)
                .append(FLOWID_SEPARATOR).append(dstIp).append(FLOWID_SEPARATOR).append(dstMask)
                .append(FLOWID_SEPARATOR).append(srcPort).append(FLOWID_SEPARATOR).append(dstPort)
                .append(FLOWID_SEPARATOR).append(protocol).append(FLOWID_SEPARATOR).append(sfpId).toString();
    }

    private String getFlowRef(final long sfpId, final String dstMac, final int dstVlan) {
        return new StringBuilder(256).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(sfpId)
                .append(FLOWID_SEPARATOR).append(dstMac).append(FLOWID_SEPARATOR).append(dstVlan).toString();
    }

    private String getFlowRef(final String dstMac, final int dstVlan) {
        return new StringBuilder(256).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(dstMac)
                .append(FLOWID_SEPARATOR).append(dstVlan).toString();
    }

    private String getFlowRef(final int vlan) {
        return new StringBuilder(256).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(vlan).toString();
    }

    private String getFlowRef(final long sfpId, final String srcMac, final String dstMac, final int dstVlan) {
        return new StringBuilder(256).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append(sfpId)
                .append(FLOWID_SEPARATOR).append(srcMac).append(FLOWID_SEPARATOR).append(dstMac)
                .append(FLOWID_SEPARATOR).append(dstVlan).toString();
    }

    private String getFlowRef(final short tableId) {
        return new StringBuilder(256).append(FLOWID_PREFIX).append(FLOWID_SEPARATOR).append("default")
                .append(FLOWID_SEPARATOR).append(tableId).toString();
    }

    private String generateTransactionUri() {
        long lTransactionIdOut = m_atomicInteger.incrementAndGet();
        return new StringBuilder(16).append(lTransactionIdOut).toString();
    }

    public static String longToIp(String ip, short mask) {
        StringBuilder sb = new StringBuilder(16);
        sb.append(ip).append("/").append(mask);
        return sb.toString();
    }

}

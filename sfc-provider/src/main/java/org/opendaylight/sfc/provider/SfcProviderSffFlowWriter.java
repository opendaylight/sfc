/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigInteger;

import com.google.common.base.Joiner;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.sfc.provider.utils.MdSalUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
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
 * 
 * <p>
 * 
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * @since 2014-08-07
 */
public class SfcProviderSffFlowWriter {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcProviderSffFlowWriter.class);
    private static SfcProviderSffFlowWriter instance = null;

    // Which bits in the metadata field to set, used for the Bucket and allows
    // 4095 sfpid's
    // TODO check how many sfpid's there can be
    private static final Integer METADATA_BITS = new Integer(0x0fff);
    private static final short TABLE_INDEX_SFF_ACL = 0;
    private static final short TABLE_INDEX_SFF_NEXT_HOP = 1;
    private static final int FLOW_PRIORITY_ACL = 256;
    private static final int FLOW_PRIORITY_NEXT_HOP = 256;
    private static final String SFF_OUTPUT_PORT = "SFF_OUTPUT";// just assuming
    // it openflow
    // port

    private final AtomicLong flowIdInc = new AtomicLong();
    private short tableBase = (short) 0;
    private final DataBroker dataBroker;
    private boolean isReady;
    private String sffNodeName;

    public static void createInstance(DataBroker dataBroker) {
        if (instance == null) {
            instance = new SfcProviderSffFlowWriter(dataBroker);
        }
    }

    public static SfcProviderSffFlowWriter getInstance() {
        return instance;
    }

    public SfcProviderSffFlowWriter(DataBroker dataBroker) {
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
     * tables Doing so allows for OFS tables previous to the SFF tables.
     * tableIndex should be one of: TABLE_INDEX_SFF_ACL or
     * TABLE_INDEX_SFF_OUTPUT
     */
    private short getTableId(short tableIndex) {
        return (short) (tableBase + tableIndex);
    }

    private List<Action> createOutputAction(String outputSffPort) {

        List<Action> actions = new ArrayList<Action>();

        Action outputAction = new ActionBuilder()
                .setAction(
                        new OutputActionCaseBuilder()
                                .setOutputAction(
                                        new OutputActionBuilder()
                                                .setMaxLength(
                                                        Integer.valueOf(0))
                                                .setOutputNodeConnector(
                                                        new Uri(outputSffPort))
                                                .build()).build())
                .setKey(new ActionKey(12)).build();
        actions.add(outputAction);
        return actions;
    }

    public void writeOutGroup() {

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (!isReady) {
                        LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() NOT ready to write yet");
                        return;
                    }

                    LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() Group entry");

                    String groupName = "Default";
                    long groupNum = 1000;
                    GroupId groupId = new GroupId(groupNum);

                    BucketBuilder bucket = new BucketBuilder();
                    bucket.setBucketId(new BucketId((long) 12));// assumed
                    bucket.setKey(new BucketKey(new BucketId((long) 12)));
                    bucket.setAction(createOutputAction(SFF_OUTPUT_PORT));

                    BucketsBuilder bucketsBuilder = new BucketsBuilder();
                    List<Bucket> listBucket = new ArrayList<Bucket>();
                    listBucket.add(bucket.build());
                    bucketsBuilder.setBucket(listBucket);

                    GroupBuilder groupBuilder = new GroupBuilder()
                            .setKey(new GroupKey(groupId)).setGroupId(groupId)
                            .setGroupName(groupName)
                            .setGroupType(GroupTypes.GroupIndirect)
                            .setBuckets(bucketsBuilder.build());

                    // Now write the Flow Entry
                    getResult(writeGroupToConfig(groupBuilder));

                } catch (Exception e) {
                    LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() caught an Exception: ");
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        t.start();

    }

    // TODO some of the 5tuple entries may be optional, need to add logic to
    // writeSffAcl() to not write them if not specified
    public void writeSffAcl(final String srcIp, final short srcMask,
            final String dstIp, final short dstMask, final short srcPort,
            final short dstPort, final byte protocol, final long sfpId) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (!isReady) {
                        LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() NOT ready to write yet");
                        return;
                    }

                    LOG.trace(
                            "+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() SFPid {}",
                            sfpId);

                    //
                    // Create the 5-tuple matching criteria

                    // To do an IP match, the EtherType needs to be set to
                    // 0x0800 which indicates IP
                    EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
                    ethTypeBuilder.setType(new EtherType(0x0800L));
                    EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
                    ethMatchBuilder.setEthernetType(ethTypeBuilder.build());

                    Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
                    ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(longToIp(
                            srcIp, srcMask)));
                    ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(
                            longToIp(dstIp, dstMask)));

                    IpMatchBuilder ipmatch = new IpMatchBuilder();
                    ipmatch.setIpProtocol((short) protocol);

                    MatchBuilder match = new MatchBuilder();
                    match.setEthernetMatch(ethMatchBuilder.build());
                    match.setLayer3Match(ipv4MatchBuilder.build());
                    match.setIpMatch(ipmatch.build());

                    if (protocol == 6) {
                        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
                        // There must be a bug in
                        // setTcpSource/DestinationPort(), because its
                        // looking at the upper 2 bytes of the port and thinks
                        // its out of range
                        tcpMatch.setTcpSourcePort(new PortNumber(new Integer(
                                0x0000FFFF & srcPort)));
                        tcpMatch.setTcpDestinationPort(new PortNumber(
                                new Integer(0x0000FFFF & dstPort)));
                        match.setLayer4Match(tcpMatch.build());
                    } else {
                        UdpMatchBuilder udpMatch = new UdpMatchBuilder();
                        udpMatch.setUdpSourcePort(new PortNumber(new Integer(
                                0x0000FFFF & srcPort)));
                        udpMatch.setUdpDestinationPort(new PortNumber(
                                new Integer(0x0000FFFF & dstPort)));
                        match.setLayer4Match(udpMatch.build());
                    }

                    //
                    // Create the Actions

                    // Create the Metadata action and wrap it in an
                    // InstructionBuilder
                    // Set the bits specified by METADATA_BITS with the bucket
                    // value
                    WriteMetadataBuilder wmb = new WriteMetadataBuilder();
                    wmb.setMetadata(new BigInteger(Long.toString(sfpId), 10));
                    wmb.setMetadataMask(new BigInteger(
                            METADATA_BITS.toString(), 10));

                    InstructionBuilder wmbIb = new InstructionBuilder();
                    wmbIb.setInstruction(new WriteMetadataCaseBuilder()
                            .setWriteMetadata(wmb.build()).build());
                    wmbIb.setKey(new InstructionKey(0));
                    wmbIb.setOrder(0);

                    // Create the Goto Table (Twcl) Action and wrap it in an
                    // InstructionBuilder
                    GoToTableBuilder gotoTb = new GoToTableBuilder();
                    gotoTb.setTableId(getTableId(getTableId(TABLE_INDEX_SFF_NEXT_HOP)));

                    InstructionBuilder gotoTbIb = new InstructionBuilder();
                    gotoTbIb.setInstruction(new GoToTableCaseBuilder()
                            .setGoToTable(gotoTb.build()).build());
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
                    FlowBuilder aclFlow = new FlowBuilder();
                    aclFlow.setId(new FlowId(String.valueOf(flowIdInc
                            .getAndIncrement())));
                    aclFlow.setKey(new FlowKey(new FlowId(Long
                            .toString(flowIdInc.getAndIncrement()))));
                    aclFlow.setTableId(getTableId(TABLE_INDEX_SFF_ACL));
                    aclFlow.setFlowName("acl"); // should this name be unique??
                    BigInteger cookieValue = new BigInteger("10", 10);
                    aclFlow.setCookie(new FlowCookie(cookieValue));
                    aclFlow.setCookieMask(new FlowCookie(cookieValue));
                    aclFlow.setContainerName(null);
                    aclFlow.setStrict(false);
                    aclFlow.setMatch(match.build());
                    aclFlow.setInstructions(isb.build());
                    aclFlow.setPriority(FLOW_PRIORITY_ACL);
                    aclFlow.setHardTimeout(0);
                    aclFlow.setIdleTimeout(0);
                    aclFlow.setFlags(new FlowModFlags(false, false, false,
                            false, false));
                    if (null == aclFlow.isBarrier()) {
                        aclFlow.setBarrier(Boolean.FALSE);
                    }

                    //
                    // Now write the Flow Entry
                    getResult(writeFlowToConfig(aclFlow));

                } catch (Exception e) {
                    LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() caught an Exception: ");
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        t.start();
    }

    public void writeSffNextHopGroup(final String srcMac, final int srcVlan,
            final long sfpId, final long nextHopGroupId) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {

                    if (!isReady) {
                        LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() NOT ready to write yet");
                        return;
                    }
                    LOG.trace(
                            "+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop srcVlan {} , sfpId {}, srcMac {}, nextHopGroupId{}",
                            srcVlan, sfpId, srcMac, nextHopGroupId);

                    //
                    // Create the matching criteria
                    MatchBuilder match = new MatchBuilder();

                    // Match on the metadata sfpId
                    MetadataBuilder metadata = new MetadataBuilder();
                    metadata.setMetadata(BigInteger.valueOf(sfpId));
                    metadata.setMetadataMask(new BigInteger(METADATA_BITS
                            .toString(), 10));
                    match.setMetadata(metadata.build());

                    // match the src mac
                    EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
                    EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
                    ethSourceBuilder.setAddress(new MacAddress(srcMac));
                    ethernetMatch.setEthernetSource(ethSourceBuilder.build());
                    match.setEthernetMatch(ethernetMatch.build());

                    // match the src vlan
                    VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
                    VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
                    VlanId vlanId = new VlanId(srcVlan);
                    vlanIdBuilder.setVlanId(vlanId);
                    vlanIdBuilder.setVlanIdPresent(true);
                    vlanBuilder.setVlanId(vlanIdBuilder.build());

                    match.setVlanMatch(vlanBuilder.build());

                    // Set the DL (Data Link) Dest Mac Address
                    Action abGroup = MdSalUtils.createSetGroupAction(
                            nextHopGroupId, 0);
                    List<Action> actionList = new ArrayList<Action>();

                    actionList.add(abGroup);

                    // Create an Apply Action
                    ApplyActionsBuilder aab = new ApplyActionsBuilder();
                    aab.setAction(actionList);

                    // Wrap our Apply Action in an Instruction
                    InstructionBuilder ib = new InstructionBuilder();
                    ib.setInstruction(new ApplyActionsCaseBuilder()
                            .setApplyActions(aab.build()).build());
                    ib.setKey(new InstructionKey(0));
                    ib.setOrder(0);

                    // Put our Instruction in a list of Instructions
                    InstructionsBuilder isb = new InstructionsBuilder();
                    List<Instruction> instructions = new ArrayList<Instruction>();
                    instructions.add(ib.build());
                    isb.setInstruction(instructions);

                    //
                    // Create and configure the FlowBuilder
                    FlowBuilder nextHopFlow = new FlowBuilder();
                    nextHopFlow.setId(new FlowId(String.valueOf(flowIdInc
                            .getAndIncrement())));
                    nextHopFlow.setKey(new FlowKey(new FlowId(Long
                            .toString(flowIdInc.getAndIncrement()))));
                    nextHopFlow
                            .setTableId(getTableId(TABLE_INDEX_SFF_NEXT_HOP));
                    nextHopFlow.setFlowName("nextHop");
                    BigInteger cookieValue = new BigInteger("20", 10);
                    nextHopFlow.setCookie(new FlowCookie(cookieValue));
                    nextHopFlow.setCookieMask(new FlowCookie(cookieValue));
                    nextHopFlow.setContainerName(null);
                    nextHopFlow.setStrict(false);
                    nextHopFlow.setMatch(match.build());
                    nextHopFlow.setInstructions(isb.build());
                    nextHopFlow.setPriority(FLOW_PRIORITY_NEXT_HOP);
                    nextHopFlow.setHardTimeout(0);
                    nextHopFlow.setIdleTimeout(0);
                    nextHopFlow.setFlags(new FlowModFlags(false, false, false,
                            false, false));
                    if (null == nextHopFlow.isBarrier()) {
                        nextHopFlow.setBarrier(Boolean.FALSE);
                    }

                    //
                    // Now write the Flow Entry
                    getResult(writeFlowToConfig(nextHopFlow));

                } catch (Exception e) {
                    LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop() caught an Exception: ");
                    LOG.error(e.getMessage(), e);
                }

            }
        };
        t.start();

    }

    // Overriden it for our specific L2 case
    public void writeSffNextHop(final String srcMac, final int srcVlan,
            final String dstMac, final int dstVlan, final long sfpId) {

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (!isReady) {
                        LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() NOT ready to write yet");
                        return;
                    }

                    LOG.trace(
                            "+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop srcVlan{}, sfpId{}, srcMac{}, dstMac{}, dstVlan{}",
                            srcVlan, sfpId, srcMac, dstMac, dstVlan);

                    //
                    // Create the matching criteria
                    MatchBuilder match = new MatchBuilder();

                    // Match on the metadata sfpId
                    MetadataBuilder metadata = new MetadataBuilder();
                    metadata.setMetadata(BigInteger.valueOf(sfpId));
                    metadata.setMetadataMask(new BigInteger(METADATA_BITS
                            .toString(), 10));
                    match.setMetadata(metadata.build());

                    // match the src mac
                    EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
                    EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
                    ethSourceBuilder.setAddress(new MacAddress(srcMac));
                    ethernetMatch.setEthernetSource(ethSourceBuilder.build());
                    match.setEthernetMatch(ethernetMatch.build());

                    // match the src vlan
                    VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
                    VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
                    VlanId vlanId = new VlanId(srcVlan);
                    vlanIdBuilder.setVlanId(vlanId);
                    vlanIdBuilder.setVlanIdPresent(true);
                    vlanBuilder.setVlanId(vlanIdBuilder.build());

                    match.setVlanMatch(vlanBuilder.build());

                    // Create the Actions

                    // Set the DL (Data Link) Dest Mac Address
                    Action abDst = MdSalUtils.createSetDlDstAction(dstMac, 0);

                    // Set the DL (Data Link) Source Mac Address
                    Action abSrc = MdSalUtils.createSetDlSrcAction(srcMac, 1);

                    Action dstMac = MdSalUtils.createSetDstVlanAction(dstVlan,
                            2);

                    List<Action> actionList = new ArrayList<Action>();

                    actionList.add(abDst);
                    actionList.add(abSrc);
                    actionList.add(dstMac);
                    // Create an Apply Action
                    ApplyActionsBuilder aab = new ApplyActionsBuilder();
                    aab.setAction(actionList);

                    // Wrap our Apply Action in an Instruction
                    InstructionBuilder ib = new InstructionBuilder();
                    ib.setInstruction(new ApplyActionsCaseBuilder()
                            .setApplyActions(aab.build()).build());
                    ib.setKey(new InstructionKey(0));
                    ib.setOrder(0);

                    // Put our Instruction in a list of Instructions
                    InstructionsBuilder isb = new InstructionsBuilder();
                    List<Instruction> instructions = new ArrayList<Instruction>();
                    instructions.add(ib.build());
                    isb.setInstruction(instructions);

                    //
                    // Create and configure the FlowBuilder
                    FlowBuilder nextHopFlow = new FlowBuilder();
                    nextHopFlow.setId(new FlowId(String.valueOf(flowIdInc
                            .getAndIncrement())));
                    nextHopFlow.setKey(new FlowKey(new FlowId(Long
                            .toString(flowIdInc.getAndIncrement()))));
                    nextHopFlow
                            .setTableId(getTableId(TABLE_INDEX_SFF_NEXT_HOP));
                    nextHopFlow.setFlowName("nextHop");
                    BigInteger cookieValue = new BigInteger("20", 10);
                    nextHopFlow.setCookie(new FlowCookie(cookieValue));
                    nextHopFlow.setCookieMask(new FlowCookie(cookieValue));
                    nextHopFlow.setContainerName(null);
                    nextHopFlow.setStrict(false);
                    nextHopFlow.setMatch(match.build());
                    nextHopFlow.setInstructions(isb.build());
                    nextHopFlow.setPriority(FLOW_PRIORITY_NEXT_HOP);
                    nextHopFlow.setHardTimeout(0);
                    nextHopFlow.setIdleTimeout(0);
                    nextHopFlow.setFlags(new FlowModFlags(false, false, false,
                            false, false));
                    if (null == nextHopFlow.isBarrier()) {
                        nextHopFlow.setBarrier(Boolean.FALSE);
                    }

                    //
                    // Now write the Flow Entry
                    getResult(writeFlowToConfig(nextHopFlow));

                } catch (Exception e) {
                    LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop() caught an Exception: ");
                    LOG.error(e.getMessage(), e);
                }

            }
        };
        t.start();
    }

    // TODO need to check if these types are correct: sfpId, src/dstMac, etc
    public void writeSffNextHop(final int inPort, final long sfpId,
            final String srcMac, final String dstMac, final int outPort) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (!isReady) {
                        LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() NOT ready to write yet");
                        return;
                    }

                    LOG.trace(
                            "+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop inPort {} sfpId {}, MAC src/dest {}/{} outPort {}",
                            inPort, sfpId, srcMac, dstMac, outPort);

                    //
                    // Create the matching criteria
                    MatchBuilder match = new MatchBuilder();

                    // Match on the metadata sfpId
                    MetadataBuilder metadata = new MetadataBuilder();
                    metadata.setMetadata(BigInteger.valueOf(sfpId));
                    metadata.setMetadataMask(new BigInteger(METADATA_BITS
                            .toString(), 10));
                    match.setMetadata(metadata.build());

                    // Match on the inPort
                    EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
                    ethTypeBuilder.setType(new EtherType(0x0800L));
                    EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
                    ethMatchBuilder.setEthernetType(ethTypeBuilder.build());
                    match.setEthernetMatch(ethMatchBuilder.build());
                    match.setInPort(new NodeConnectorId(Integer
                            .toString(inPort)));

                    //
                    // Create the Actions

                    // Set the DL (Data Link) Dest Mac Address
                    Action abDst = MdSalUtils.createSetDlDstAction(dstMac, 0);

                    // Set the DL (Data Link) Source Mac Address
                    Action abSrc = MdSalUtils.createSetDlSrcAction(srcMac, 1);

                    // Set the output port
                    Action abPort = MdSalUtils.createOutputAction(new Uri(
                            Integer.toString(outPort)), 2);

                    List<Action> actionList = new ArrayList<Action>();

                    actionList.add(abDst);
                    actionList.add(abSrc);
                    actionList.add(abPort);

                    // Create an Apply Action
                    ApplyActionsBuilder aab = new ApplyActionsBuilder();
                    aab.setAction(actionList);

                    // Wrap our Apply Action in an Instruction
                    InstructionBuilder ib = new InstructionBuilder();
                    ib.setInstruction(new ApplyActionsCaseBuilder()
                            .setApplyActions(aab.build()).build());
                    ib.setKey(new InstructionKey(0));
                    ib.setOrder(0);

                    // Put our Instruction in a list of Instructions
                    InstructionsBuilder isb = new InstructionsBuilder();
                    List<Instruction> instructions = new ArrayList<Instruction>();
                    instructions.add(ib.build());
                    isb.setInstruction(instructions);

                    //
                    // Create and configure the FlowBuilder
                    FlowBuilder nextHopFlow = new FlowBuilder();
                    nextHopFlow.setId(new FlowId(String.valueOf(flowIdInc
                            .getAndIncrement())));
                    nextHopFlow.setKey(new FlowKey(new FlowId(Long
                            .toString(flowIdInc.getAndIncrement()))));
                    nextHopFlow
                            .setTableId(getTableId(TABLE_INDEX_SFF_NEXT_HOP));
                    nextHopFlow.setFlowName("nextHop"); // should this name be
                    // unique??
                    BigInteger cookieValue = new BigInteger("20", 10);
                    nextHopFlow.setCookie(new FlowCookie(cookieValue));
                    nextHopFlow.setCookieMask(new FlowCookie(cookieValue));
                    nextHopFlow.setContainerName(null);
                    nextHopFlow.setStrict(false);
                    nextHopFlow.setMatch(match.build());
                    nextHopFlow.setInstructions(isb.build());
                    nextHopFlow.setPriority(FLOW_PRIORITY_NEXT_HOP);
                    nextHopFlow.setHardTimeout(0);
                    nextHopFlow.setIdleTimeout(0);
                    nextHopFlow.setFlags(new FlowModFlags(false, false, false,
                            false, false));
                    if (null == nextHopFlow.isBarrier()) {
                        nextHopFlow.setBarrier(Boolean.FALSE);
                    }

                    //
                    // Now write the Flow Entry
                    getResult(writeFlowToConfig(nextHopFlow));

                } catch (Exception e) {
                    LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop() caught an Exception: ");
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        t.start();
    }

    //
    // Private internal methods
    //

    private boolean getResult(
            ListenableFuture<RpcResult<TransactionStatus>> result) {
        return getResult(result, false);
    }

    // Should only set wait to true for debugging
    private boolean getResult(
            ListenableFuture<RpcResult<TransactionStatus>> result, boolean wait) {
        try {
            if (wait) {
                while (!result.isDone()) {
                    // LOG.trace("writeFlowToConfig status is not done yet");
                }
            }

            RpcResult<TransactionStatus> rpct = result.get();
            TransactionStatus ts = rpct.getResult();
            if (ts != TransactionStatus.COMMITED) {
                LOG.trace("TransactionStatus result {}, result NOT SUCCESSFUL",
                        ts.toString());
                return false;
            }

            LOG.trace("TransactionStatus result {}, result SUCCESSFUL",
                    ts.toString());
            return true;

        } catch (Exception e) {
            LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter waitForResult() caught an Exception: ");
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    private ListenableFuture<RpcResult<TransactionStatus>> writeGroupToConfig(
            GroupBuilder group) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // create the group path

        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .builder(Nodes.class).child(Node.class, nodeBuilder.getKey())
                .toInstance();

        InstanceIdentifier<Group> groupPath = InstanceIdentifier
                .builder(Nodes.class).child(Node.class, nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Group.class, group.getKey()).build();

        WriteTransaction addGroupTransaction = dataBroker
                .newWriteOnlyTransaction();
        addGroupTransaction.put(LogicalDatastoreType.CONFIGURATION, nodePath,
                nodeBuilder.build(), true /*
                                           * create Missing parents if needed
                                           */);
        addGroupTransaction.put(LogicalDatastoreType.CONFIGURATION, groupPath,
                group.build(), true);

        return addGroupTransaction.commit();
    }

    private ListenableFuture<RpcResult<TransactionStatus>> writeFlowToConfig(
            FlowBuilder flow) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path
        InstanceIdentifier<Flow> flowPath = InstanceIdentifier
                .builder(Nodes.class).child(Node.class, nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey()).build();
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .builder(Nodes.class).child(Node.class, nodeBuilder.getKey())
                .toInstance();

        WriteTransaction addFlowTransaction = dataBroker
                .newWriteOnlyTransaction();
        addFlowTransaction.put(LogicalDatastoreType.CONFIGURATION, nodePath,
                nodeBuilder.build(), true /*
                                           * create Missing parents if needed
                                           */);
        addFlowTransaction.put(LogicalDatastoreType.CONFIGURATION, flowPath,
                flow.build(), true);

        return addFlowTransaction.commit();
    }

    public static String longToIp(String ip, short mask) {
        StringBuilder sb = new StringBuilder(15);
        sb.append("/").append(mask);
        return sb.toString();
    }

    public void writeSffNextHopDefaultFlow(final String dstMac,
            final int dstVlan) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {

                    if (!isReady) {
                        LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() NOT ready to write yet");
                        return;
                    }
                    LOG.trace(
                            "+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop default entry dstMac{}, dstVlan{}",
                            dstMac, dstVlan);

                    //
                    // Create the matching criteria
                    MatchBuilder match = new MatchBuilder();
                    // Set the DL (Data Link) Dest Mac Address
                    Action abDst = MdSalUtils.createSetDlDstAction(dstMac, 0);

                    Action dstMac = MdSalUtils.createSetDstVlanAction(dstVlan,
                            1);

                    List<Action> actionList = new ArrayList<Action>();

                    actionList.add(abDst);
                    actionList.add(dstMac);
                    // Create an Apply Action
                    ApplyActionsBuilder aab = new ApplyActionsBuilder();
                    aab.setAction(actionList);

                    // Wrap our Apply Action in an Instruction
                    InstructionBuilder ib = new InstructionBuilder();
                    ib.setInstruction(new ApplyActionsCaseBuilder()
                            .setApplyActions(aab.build()).build());
                    ib.setKey(new InstructionKey(0));
                    ib.setOrder(0);

                    // Put our Instruction in a list of Instructions
                    InstructionsBuilder isb = new InstructionsBuilder();
                    List<Instruction> instructions = new ArrayList<Instruction>();
                    instructions.add(ib.build());
                    isb.setInstruction(instructions);

                    //
                    // Create and configure the FlowBuilder
                    FlowBuilder nextHopFlow = new FlowBuilder();
                    nextHopFlow.setId(new FlowId(String.valueOf(flowIdInc
                            .getAndIncrement())));
                    nextHopFlow.setKey(new FlowKey(new FlowId(Long
                            .toString(flowIdInc.getAndIncrement()))));
                    nextHopFlow
                            .setTableId(getTableId(TABLE_INDEX_SFF_NEXT_HOP));
                    nextHopFlow.setFlowName("nextHopDefaultFlow");
                    BigInteger cookieValue = new BigInteger("20", 10);
                    nextHopFlow.setCookie(new FlowCookie(cookieValue));
                    nextHopFlow.setCookieMask(new FlowCookie(cookieValue));
                    nextHopFlow.setContainerName(null);
                    nextHopFlow.setStrict(false);
                    nextHopFlow.setMatch(match.build());
                    nextHopFlow.setInstructions(isb.build());
                    nextHopFlow.setPriority(FLOW_PRIORITY_NEXT_HOP);
                    nextHopFlow.setHardTimeout(0);
                    nextHopFlow.setIdleTimeout(0);
                    nextHopFlow.setFlags(new FlowModFlags(false, false, false,
                            false, false));
                    if (null == nextHopFlow.isBarrier()) {
                        nextHopFlow.setBarrier(Boolean.FALSE);
                    }

                    //
                    // Now write the Flow Entry
                    getResult(writeFlowToConfig(nextHopFlow));

                } catch (Exception e) {
                    LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffNextHop() caught an Exception: ");
                    LOG.error(e.getMessage(), e);
                }

            }
        };
        t.start();

    }

}

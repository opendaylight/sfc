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

import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//
//From maven repo:
//   <groupId>org.opendaylight.controller.model</groupId>
//   <artifactId>model-flow-service</artifactId>
//   <version>1.1-SNAPSHOT</version>
//Yang version: rev130819
//
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;


//
//From maven repo:
//   <groupId>org.opendaylight.controller.model</groupId>
//   <artifactId>model-flow-base</artifactId>
//   <version>1.1-SNAPSHOT</version>
//Yang version: rev131026
//
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;

//
//From maven repo:
//    <groupId>org.opendaylight.controller.model</groupId>
//    <artifactId>model-inventory</artifactId>
//    <version>1.1-SNAPSHOT</version>
//Yang version: rev130819
//
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

// 
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * This class writes Flow Entries to the SFF once an SFF has been configured.
 *
 * <p>
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * @since       2014-08-07
 */
public class SfcProviderSffFlowWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSffFlowWriter.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    // Which bits in the metadata field to set, used for the Bucket and allows 4095 sfpid's 
    // TODO check how many sfpid's there can be
    private static final Integer METADATA_BITS = new Integer(0x0fff);
    private static final short TABLE_INDEX_SFF_ACL = 0;
    private static final short TABLE_INDEX_SFF_NEXT_HOP = 1;
    private static final int ACL_FLOW_PRIORITY = 256;

    private AtomicLong flowIdInc = new AtomicLong();
    private short tableBase = (short) 0;
    private DataBroker dataBroker;
    private boolean isReady;
    private String sffNodeName;

    public SfcProviderSffFlowWriter(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.isReady = false;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setNodeInfo(String sffNodeName) {
        this.sffNodeName = sffNodeName;
        isReady = true;
    }

    public short getTableBase() {
        return this.tableBase;
    }
    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    private boolean getResult(ListenableFuture<RpcResult<TransactionStatus>> result) {
        return getResult(result, false);
    }

    private boolean getResult(ListenableFuture<RpcResult<TransactionStatus>> result, boolean wait) {
        try {
            if(wait) {
                while(!result.isDone()) {
                    //LOG.trace("writeFlowToConfig status is not done yet");
                }
            }

            RpcResult<TransactionStatus> rpct = result.get();
            TransactionStatus ts = rpct.getResult();
            if (ts != TransactionStatus.COMMITED) {
                LOG.trace("TransactionStatus result {}, result NOT SUCCESSFUL", ts.toString());
                return false;
            }

            LOG.trace("TransactionStatus result {}, result SUCCESSFUL", ts.toString());
            return true;

        } catch(Exception e) {
        	LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter waitForResult() caught an Exception: ");
        	LOG.error(e.getMessage(), e);
            return false;
        }
   }

   private ListenableFuture<RpcResult<TransactionStatus>> writeFlowToConfig(FlowBuilder flow) {
	   // Create the NodeBuilder
       NodeBuilder nodeBuilder = new NodeBuilder();
       nodeBuilder.setId(new NodeId(this.sffNodeName));
       nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

       // Create the flow path
       InstanceIdentifier<Flow> flowPath = 
    		   InstanceIdentifier.builder(Nodes.class)
    		   .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class)
    		   .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flow.getKey()).build();
       InstanceIdentifier<Node> nodePath = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeBuilder.getKey()).toInstance();

       //logger.trace("+++++++++++++++++  writeFlowToConfig: Node path {}", nodePath);
       //logger.trace("+++++++++++++++++  writeFlowToConfig: Flow path {}", flowPath);

       WriteTransaction addFlowTransaction = dataBroker.newWriteOnlyTransaction();
       addFlowTransaction.put(LogicalDatastoreType.CONFIGURATION, nodePath, nodeBuilder.build());
       addFlowTransaction.put(LogicalDatastoreType.CONFIGURATION, flowPath, flow.build());

       return addFlowTransaction.commit();
   }


    /**
     * getTableId
     * 
     * Having a TableBase allows us to "offset" the SFF tables by this.tableBase tables
     * Doing so allows for OFS tables previous to the SFF tables.
     * tableIndex should be one of: TABLE_INDEX_SFF_ACL or TABLE_INDEX_SFF_OUTPUT
     */
    public short getTableId(short tableIndex) {
        return (short) (this.tableBase + tableIndex);
    }

    public void writeSffAcl(String srcIp, String dstIp, short srcPort, short dstPort, byte protocol, int sfpId, short nextTable) {
        try {
        	if(!isReady) {
        		LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() NOT ready to write yet");
        		return;
        	}

        	LOG.trace("+++++++++++++++++  SfcProviderSffFlowWriter.writeSffAcl() SFPid {}", sfpId);

            //
            // Create the 5-tuple matching criteria

            // To do an IP match, the EtherType needs to be set to 0x0800 which indicates IP
            EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
            ethTypeBuilder.setType(new EtherType(0x0800L));
            EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
            ethMatchBuilder.setEthernetType(ethTypeBuilder.build());

            Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
            ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(srcIp));
            ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(dstIp));

            IpMatchBuilder ipmatch = new IpMatchBuilder();
            ipmatch.setIpProtocol((short) protocol);

            MatchBuilder match = new MatchBuilder();
            match.setEthernetMatch(ethMatchBuilder.build());
            match.setLayer3Match(ipv4MatchBuilder.build());
            match.setIpMatch(ipmatch.build());

            if(protocol == 6) {
                TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
                // There must be a bug in setTcpSource/DestinationPort(), because its 
                // looking at the upper 2 bytes of the port and thinks its out of range
                tcpMatch.setTcpSourcePort(new PortNumber(new Integer(0x0000FFFF & (int) srcPort)));
                tcpMatch.setTcpDestinationPort(new PortNumber(new Integer(0x0000FFFF & (int) dstPort)));
                match.setLayer4Match(tcpMatch.build());
            } else {
                UdpMatchBuilder udpMatch = new UdpMatchBuilder();
                udpMatch.setUdpSourcePort(new PortNumber(new Integer(0x0000FFFF & (int) srcPort)));
                udpMatch.setUdpDestinationPort(new PortNumber(new Integer(0x0000FFFF & (int) dstPort)));
                match.setLayer4Match(udpMatch.build());
            }

            //
            // Create the Actions

            // Create the Metadata action and wrap it in an InstructionBuilder
            // Set the bits specified by METADATA_BITS with the bucket value
            WriteMetadataBuilder wmb = new WriteMetadataBuilder();
            wmb.setMetadata(new BigInteger(Integer.toString(sfpId), 10));
            wmb.setMetadataMask(new BigInteger(METADATA_BITS.toString(), 10));

            InstructionBuilder wmbIb = new InstructionBuilder();
            wmbIb.setInstruction(new WriteMetadataCaseBuilder().setWriteMetadata(wmb.build()).build());
            wmbIb.setKey(new InstructionKey(0));
            wmbIb.setOrder(0);

            // Create the Goto Table (Twcl) Action and wrap it in an InstructionBuilder
            GoToTableBuilder gotoTb = new GoToTableBuilder();
            gotoTb.setTableId(getTableId(nextTable));

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
            FlowBuilder aclFlow = new FlowBuilder();
            aclFlow.setId(new FlowId(String.valueOf(flowIdInc.getAndIncrement())));
            aclFlow.setKey(new FlowKey(new FlowId(Long.toString(flowIdInc.getAndIncrement()))));
            aclFlow.setTableId(getTableId(TABLE_INDEX_SFF_ACL));
            aclFlow.setFlowName("acl"); // should this name be unique??
            BigInteger cookieValue = new BigInteger("10", 10);
            aclFlow.setCookie(new FlowCookie(cookieValue));
            aclFlow.setCookieMask(new FlowCookie(cookieValue));
            aclFlow.setContainerName(null);
            aclFlow.setStrict(false);
            aclFlow.setMatch(match.build());
            aclFlow.setInstructions(isb.build());
            aclFlow.setPriority(ACL_FLOW_PRIORITY);
            aclFlow.setHardTimeout(0);
            aclFlow.setIdleTimeout(0);
            aclFlow.setFlags(new FlowModFlags(false, false, false, false, false));
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

    public void writeSffNextHop(int inPort, int sfpId, String srcMac, String dstMac, int outPort) {
    }
}

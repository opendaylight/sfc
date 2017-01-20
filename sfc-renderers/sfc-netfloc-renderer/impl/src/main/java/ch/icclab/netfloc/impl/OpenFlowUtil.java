/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import ch.icclab.netfloc.iface.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.controller.action._case.ControllerActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.hw.path.action._case.HwPathActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Set;
import java.util.LinkedList;

public class OpenFlowUtil {

	static final Logger logger = LoggerFactory.getLogger(OpenFlowUtil.class);
	public static final long LLDP_LONG = (long) 0x88CC;
	public static Flow createLLDPFlow(IBridgeOperator bridge, int priority) {

		MatchBuilder matchBuilder = new MatchBuilder();

		matchBuilder.setEthernetMatch(ethernetMatch(
			null,
			null,
			LLDP_LONG));

		// Prepare Instuction
		InstructionsBuilder isb = new InstructionsBuilder();
		List<Instruction> instructions = new LinkedList<Instruction>();
		InstructionBuilder ib = new InstructionBuilder();
		ApplyActionsBuilder aab = new ApplyActionsBuilder();
		List<Action> actionList = new LinkedList<Action>();

  	actionList.add(createControllerAction());

		// Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
		ib.setOrder(0);
		ib.setKey(new InstructionKey(0));
		instructions.add(ib.build());

		// Create Flow
		FlowBuilder flowBuilder = new FlowBuilder();
		flowBuilder.setMatch(matchBuilder.build());

		String flowId = OpenFlowUtil.createLLDPFlowId(bridge);
		flowBuilder.setId(new FlowId(flowId));
		FlowKey key = new FlowKey(new FlowId(flowId));

		flowBuilder.setBarrier(true);
		flowBuilder.setTableId((short)0);
		flowBuilder.setKey(key);
		flowBuilder.setPriority(priority);
		flowBuilder.setFlowName(flowId);
		flowBuilder.setHardTimeout(0);
		flowBuilder.setIdleTimeout(0);

		return flowBuilder.setInstructions(isb.setInstruction(instructions).build()).build();
	}

	public static String createLLDPFlowId(IBridgeOperator bridge) {
		return "LLDP_" + bridge.getDatapathId();
	}

	public static Action createControllerAction() {
		ActionBuilder ab = new ActionBuilder();
		OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(0xffff);
        Uri value = new Uri("CONTROLLER");
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
		return ab.build();
	}

	public static Flow createNormalFlow(IBridgeOperator bridge, int priority) {

		// Empty match
		MatchBuilder matchBuilder = new MatchBuilder();

		// Prepare Instuction
		InstructionsBuilder isb = new InstructionsBuilder();
		List<Instruction> instructions = new LinkedList<Instruction>();
		InstructionBuilder ib = new InstructionBuilder();
		ApplyActionsBuilder aab = new ApplyActionsBuilder();
		List<Action> actionList = new LinkedList<Action>();

    actionList.add(createNormalAction());

		// Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
		ib.setOrder(0);
		ib.setKey(new InstructionKey(0));
		instructions.add(ib.build());

		// Create Flow
		FlowBuilder flowBuilder = new FlowBuilder();
		flowBuilder.setMatch(matchBuilder.build());

		String flowId = OpenFlowUtil.createNormalFlowId(bridge);
		flowBuilder.setId(new FlowId(flowId));
		FlowKey key = new FlowKey(new FlowId(flowId));

		flowBuilder.setBarrier(true);
		flowBuilder.setTableId((short)0);
		flowBuilder.setKey(key);
		flowBuilder.setPriority(priority);
		flowBuilder.setFlowName(flowId);
		flowBuilder.setHardTimeout(0);
		flowBuilder.setIdleTimeout(0);

		return flowBuilder.setInstructions(isb.setInstruction(instructions).build()).build();
	}

	public static String createNormalFlowId(IBridgeOperator bridge) {
		return "NORMAL_" + bridge.getDatapathId();
	}

	private static Action createNormalAction() {
		ActionBuilder ab = new ActionBuilder();
		OutputActionBuilder output = new OutputActionBuilder();
        Uri value = new Uri("NORMAL");
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        return ab.build();
	}

	public static Flow createBroadcastFlow(IBridgeOperator bridge, IPortOperator inPort, Set<IPortOperator> outPorts, String srcMac, int priority) {
		NodeConnectorId ncidIn = new NodeConnectorId("openflow:" + Long.parseLong(bridge.getDatapathId().replace(":", ""), 16) + ":" + inPort.getOfport());
		MatchBuilder matchBuilder = new MatchBuilder();

		EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethDestinationBuilder.setAddress(new MacAddress("ff:ff:ff:ff:ff:ff"));
        ethSourceBuilder.setAddress(new MacAddress(srcMac));
        ethernetMatch.setEthernetSource(ethSourceBuilder.build());
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());
		matchBuilder.setInPort(ncidIn);

		// Prepare Instuction
		InstructionsBuilder isb = new InstructionsBuilder();
		List<Instruction> instructions = new LinkedList<Instruction>();
		InstructionBuilder ib = new InstructionBuilder();
		ApplyActionsBuilder aab = new ApplyActionsBuilder();


		List<Action> actionList = new LinkedList<Action>();
		int keyCount = 0;
		for (IPortOperator outPort : outPorts) {
			ActionBuilder ab = new ActionBuilder();
			OutputActionBuilder output = new OutputActionBuilder();
			NodeConnectorId ncidOut = new NodeConnectorId("openflow:" + Long.parseLong(bridge.getDatapathId().replace(":", ""), 16) + ":" + outPort.getOfport());
			output.setOutputNodeConnector(ncidOut);
			output.setMaxLength(65535);
			ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
			ab.setOrder(keyCount);
			ab.setKey(new ActionKey(keyCount));
			actionList.add(ab.build());
			keyCount++;
		}

		// Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
		ib.setOrder(0);
		ib.setKey(new InstructionKey(0));
		instructions.add(ib.build());

		// Create Flow
		FlowBuilder flowBuilder = new FlowBuilder();
		flowBuilder.setMatch(matchBuilder.build());

		String flowId = OpenFlowUtil.createBroadcastFlowId(bridge, inPort, srcMac);
		flowBuilder.setId(new FlowId(flowId));
		FlowKey key = new FlowKey(new FlowId(flowId));

		flowBuilder.setBarrier(true);
		flowBuilder.setTableId((short)0);
		flowBuilder.setKey(key);
		flowBuilder.setPriority(priority);
		flowBuilder.setFlowName(flowId);
		flowBuilder.setHardTimeout(0);
		flowBuilder.setIdleTimeout(0);

		return flowBuilder.setInstructions(isb.setInstruction(instructions).build()).build();
	}

	public static String createBroadcastFlowId(IBridgeOperator bridge, IPortOperator inPort, String srcMac) {
		return "Broadcast_" + inPort + "_" + srcMac + "_" + bridge.getDatapathId();
	}

	public static Flow createForwardFlow(IBridgeOperator bridge, IPortOperator inPort, IPortOperator outPort, String srcMac, String dstMac, int priority) {

		logger.info("create forward flow: in {}, out {}, src {}, dst {}", inPort, outPort, srcMac, dstMac);

		// Match src & dst MAC
		NodeConnectorId ncidIn = new NodeConnectorId("openflow:" + Long.parseLong(bridge.getDatapathId().replace(":", ""), 16) + ":" + inPort.getOfport());
		MatchBuilder matchBuilder = new MatchBuilder();
		matchBuilder.setEthernetMatch(OpenFlowUtil.ethernetMatch(
			new MacAddress(srcMac),
			new MacAddress(dstMac),
			null));
		matchBuilder.setInPort(ncidIn);

		// Prepare Instuction
		InstructionsBuilder isb = new InstructionsBuilder();
		List<Instruction> instructions = new LinkedList<Instruction>();
		InstructionBuilder ib = new InstructionBuilder();
		ApplyActionsBuilder aab = new ApplyActionsBuilder();
		ActionBuilder ab = new ActionBuilder();
		List<Action> actionList = new LinkedList<Action>();

		// Output Action
		OutputActionBuilder output = new OutputActionBuilder();

		NodeConnectorId ncidOut = new NodeConnectorId("openflow:" + Long.parseLong(bridge.getDatapathId().replace(":", ""), 16) + ":" + outPort.getOfport());
		output.setOutputNodeConnector(ncidOut);

		output.setMaxLength(65535);
		ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
		ab.setOrder(0);
		ab.setKey(new ActionKey(0));
		actionList.add(ab.build());

		// Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
		ib.setOrder(0);
		ib.setKey(new InstructionKey(0));
		instructions.add(ib.build());

		// Create Flow
		FlowBuilder flowBuilder = new FlowBuilder();
		flowBuilder.setMatch(matchBuilder.build());

		String flowId = OpenFlowUtil.createForwardFlowId(bridge, srcMac, dstMac);
		flowBuilder.setId(new FlowId(flowId));
		FlowKey key = new FlowKey(new FlowId(flowId));

		flowBuilder.setBarrier(true);
		flowBuilder.setTableId((short)0);
		flowBuilder.setKey(key);
		flowBuilder.setPriority(priority);
		flowBuilder.setFlowName(flowId);
		flowBuilder.setHardTimeout(0);
		flowBuilder.setIdleTimeout(0);

		return flowBuilder.setInstructions(isb.setInstruction(instructions).build()).build();
	}

	public static String createForwardFlowId(IBridgeOperator bridge, String srcMac, String dstMac) {
		return "NetworkPath_" + srcMac + "_" + dstMac + "_" + bridge.getDatapathId();
	}

	public static EthernetMatch ethernetMatch(MacAddress srcMac,
                                            	MacAddress dstMac,
                                            	Long etherType) {
        EthernetMatchBuilder emb = new  EthernetMatchBuilder();
        if (srcMac != null)
            emb.setEthernetSource(new EthernetSourceBuilder()
                .setAddress(srcMac)
                .build());
        if (dstMac != null)
            emb.setEthernetDestination(new EthernetDestinationBuilder()
                .setAddress(dstMac)
                .build());
        if (etherType != null)
            emb.setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType(etherType))
                .build());
        return emb.build();
    }

    public static EthernetMatch ethernetMatchMasked(MacAddress srcMac,
												MacAddress srcMask,
                                            	MacAddress dstMac,
                                            	MacAddress dstMask,
                                            	Long etherType) {
        EthernetMatchBuilder emb = new  EthernetMatchBuilder();
		emb.setEthernetSource(new EthernetSourceBuilder()
		    .setAddress(srcMac)
		    .setMask(srcMask)
		    .build());

        emb.setEthernetDestination(new EthernetDestinationBuilder()
			.setAddress(dstMac)
			.setMask(dstMask)
			.build());

        if (etherType != null)
            emb.setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType(etherType))
                .build());
        return emb.build();
    }

    public static Action createRewriteAction(int chainId, int hop, int order) {
		ActionBuilder abRewrite = new ActionBuilder();
		SetDlDstActionBuilder rewrite = new SetDlDstActionBuilder();
		rewrite.setAddress(OpenFlowUtil.getVirtualMac(chainId, hop));
		abRewrite.setAction(new SetDlDstActionCaseBuilder().setSetDlDstAction(rewrite.build()).build());
		abRewrite.setOrder(order);
		abRewrite.setKey(new ActionKey(order));
		return abRewrite.build();
	}

	public static Action createRewriteActionSrc(int chainId, int hop, int order) {
		ActionBuilder abRewrite = new ActionBuilder();
		SetDlSrcActionBuilder rewrite = new SetDlSrcActionBuilder();
		rewrite.setAddress(OpenFlowUtil.getVirtualMac(chainId, hop));
		abRewrite.setAction(new SetDlSrcActionCaseBuilder().setSetDlSrcAction(rewrite.build()).build());
		abRewrite.setOrder(order);
		abRewrite.setKey(new ActionKey(order));
		return abRewrite.build();
	}

    public static Action createReactiveRewriteAction(int chainId, int hop, int connId, int order) {
		ActionBuilder abRewrite = new ActionBuilder();
		SetDlDstActionBuilder rewrite = new SetDlDstActionBuilder();
		rewrite.setAddress(OpenFlowUtil.getVirtualReactiveMac(chainId, hop, connId));
		abRewrite.setAction(new SetDlDstActionCaseBuilder().setSetDlDstAction(rewrite.build()).build());
		abRewrite.setOrder(order);
		abRewrite.setKey(new ActionKey(order));
		return abRewrite.build();
	}

	public static MacAddress getVirtualReactiveMac(int chainId, int hop, int connId) {
		String chainIdHex = Integer.toHexString(chainId);
		String hopHex = Integer.toHexString(hop);
		String connIdHex = Integer.toHexString(connId);
		return new MacAddress(((chainIdHex.length() == 2) ? chainIdHex : "0" + chainIdHex) +
			":" + ((hopHex.length() == 2) ? hopHex : "0" + hopHex) +
			":" + ((connIdHex.length() == 2) ? connIdHex : "0" + connIdHex) +
			":ff:ff:ff");
	}

	public static MacAddress getVirtualMac(int chainId, int hop) {
		String chainIdHex = Integer.toHexString(chainId);
		String hopHex = Integer.toHexString(hop);
		return new MacAddress(((chainIdHex.length() == 2) ? chainIdHex : "0" + chainIdHex) +
			":" + ((hopHex.length() == 2) ? hopHex : "0" + hopHex) +
			":ff:ff:ff:ff");
	}

	public static Action createOutputAction(IBridgeOperator bridge, IPortOperator outPort, int order) {
		ActionBuilder abOutput = new ActionBuilder();
		OutputActionBuilder output = new OutputActionBuilder();
		NodeConnectorId ncidOut = new NodeConnectorId("openflow:" +
			Long.parseLong(bridge.getDatapathId()
				.replace(":", ""), 16) +
			":" + outPort.getOfport());

		output.setOutputNodeConnector(ncidOut);
		output.setMaxLength(65535);

		abOutput.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
		abOutput.setOrder(order);
		abOutput.setKey(new ActionKey(order));
		return abOutput.build();
	}
}

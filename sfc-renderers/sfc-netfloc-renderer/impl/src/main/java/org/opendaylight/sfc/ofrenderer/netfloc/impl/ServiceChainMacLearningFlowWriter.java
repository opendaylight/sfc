/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IPortOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IFlowprogrammer;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IMacLearningListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
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
import com.google.common.util.concurrent.FutureCallback;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceChainMacLearningFlowWriter implements IMacLearningListener {

	private static final int CHAIN_PRIORITY = 20;
	private IFlowprogrammer flowprogrammer;
	private int chainId;
	private IBridgeOperator beginBridge;
	private IBridgeOperator endBridge;
	private IPortOperator beginBridgeBeginPort;
	private IPortOperator beginBridgeEndPort;
	private IPortOperator endBridgeBeginPort;
	private IPortOperator endBridgeEndPort;
	private int endBridgeHop;
	private int connId = 0;
	private final List<Flow> beginBridgeFlowCache = new LinkedList<Flow>();
	private final List<Flow> endBridgeFlowCache = new LinkedList<Flow>();
	private Set<String> learnedMacs = new HashSet<String>();
	static final Logger logger = LoggerFactory.getLogger(ServiceChainMacLearningFlowWriter.class);

	public ServiceChainMacLearningFlowWriter(int chainId,
		IBridgeOperator beginBridge, IBridgeOperator endBridge,
		IPortOperator beginBridgeBeginPort, IPortOperator beginBridgeEndPort,
		IPortOperator endBridgeBeginPort, IPortOperator endBridgeEndPort,
		int endBridgeHop, IFlowprogrammer flowprogrammer) {
		if (beginBridge == null ||
			endBridge == null ||
			beginBridgeEndPort == null ||
			endBridgeBeginPort == null ||
			endBridgeEndPort == null ||
			flowprogrammer == null) {
			throw new IllegalArgumentException("ServiceChainMacLearningFlowWriter Cannot instantiate chain with null arguments.");
		}

		this.chainId = chainId;
		this.beginBridge = beginBridge;
		this.endBridge = endBridge;
		this.beginBridgeBeginPort = beginBridgeBeginPort;
		this.beginBridgeEndPort = beginBridgeEndPort;
		this.endBridgeBeginPort = endBridgeBeginPort;
		this.endBridgeEndPort = endBridgeEndPort;
		this.endBridgeHop = endBridgeHop;
		this.flowprogrammer = flowprogrammer;
	}

	@Override
	public void shutDown() {
		for (Flow flow : this.beginBridgeFlowCache) {
			flowprogrammer.deleteFlow(flow, this.beginBridge, new FutureCallback<Void>() {
				public void onSuccess(Void result) {
					logger.info("ServiceChainMacLearningFlowWriter Reactive flow deleted");
				}

				public void onFailure(Throwable t) {
					logger.info("ServiceChainMacLearningFlowWriter New service chain reactive flow failed");
				}
			});
		}
		for (Flow flow : this.endBridgeFlowCache) {
			flowprogrammer.deleteFlow(flow, this.endBridge, new FutureCallback<Void>() {
				public void onSuccess(Void result) {
					logger.info("ServiceChainMacLearningFlowWriter Reactive flow deleted");
				}

				public void onFailure(Throwable t) {
					logger.info("ServiceChainMacLearningFlowWriter New service chain reactive flow failed");
				}
			});
		}
		this.beginBridgeFlowCache.removeAll(this.beginBridgeFlowCache);
		this.endBridgeFlowCache.removeAll(this.endBridgeFlowCache);
	}

	@Override
	public void macAddressesLearned(NodeConnectorId inPort, MacAddress srcMac, MacAddress dstMac) {
		logger.info("ServiceChainMacLearningFlowWriter Notified for new mac address pair, srcMac {} and dstMac {} ", srcMac, dstMac);
		NodeConnectorId beginPortNcId = new NodeConnectorId("openflow:" +
			Long.parseLong(beginBridge.getDatapathId()
				.replace(":", ""), 16) +
			":" + beginBridgeBeginPort.getOfport());
		if (!inPort.equals(beginPortNcId)) {
			return;
		}

		if (this.learnedMacs.contains(srcMac.toString() + dstMac.toString())) {
			logger.info("ServiceChainMacLearningFlowWriter Mac address pair srcMac {} and dstMac {} allready learned ", srcMac, dstMac);
			return;
		}

		this.learnedMacs.add(srcMac.toString() + dstMac.toString());
		connId++;

		NodeConnectorId endPortNcId = new NodeConnectorId("openflow:" +
			Long.parseLong(endBridge.getDatapathId()
				.replace(":", ""), 16) +
			":" + endBridgeBeginPort.getOfport());

		final Flow beginBridgeFlow = this.createBeginBridgeFlow(inPort, srcMac, dstMac);
		final Flow endBridgeFlow = this.createEndBridgeFlow(endPortNcId, srcMac, dstMac);
		flowprogrammer.programFlow(beginBridgeFlow, this.beginBridge, new FutureCallback<Void>() {
			public void onSuccess(Void result) {
				logger.info("ServiceChainMacLearningFlowWriter New service chain reactive flow {} programmed on beginBridge", beginBridgeFlow);
				beginBridgeFlowCache.add(beginBridgeFlow);
			}

			public void onFailure(Throwable t) {
				logger.info("ServiceChainMacLearningFlowWriter New service chain reactive flow failed");
			}
		});
		flowprogrammer.programFlow(endBridgeFlow, this.endBridge, new FutureCallback<Void>() {
			public void onSuccess(Void result) {
				logger.info("ServiceChainMacLearningFlowWriter New service chain reactive flow {} programmed on endBridge ", endBridgeFlow);
				endBridgeFlowCache.add(endBridgeFlow);
			}

			public void onFailure(Throwable t) {
				logger.info("ServiceChainMacLearningFlowWriter New service chain reactive flow failed");
			}
		});
	}

	private Flow createBeginBridgeFlow(NodeConnectorId inPort, MacAddress srcMac, MacAddress dstMac) {
		MatchBuilder matchBuilder = new MatchBuilder();
		matchBuilder.setEthernetMatch(OpenFlowUtil.ethernetMatch(srcMac, dstMac, null));
		matchBuilder.setInPort(inPort);

		// Prepare Instruction
		InstructionsBuilder isb = new InstructionsBuilder();
		List<Instruction> instructions = new LinkedList<Instruction>();
		InstructionBuilder ib = new InstructionBuilder();
		ApplyActionsBuilder aab = new ApplyActionsBuilder();

		List<Action> actionList = new LinkedList<Action>();
		// Rewrite Action Src
		actionList.add(OpenFlowUtil.createRewriteActionSrc(chainId, connId, 0));
		// Rewrite Action Dst
		actionList.add(OpenFlowUtil.createRewriteAction(chainId, 0, 1));
		// Output Action
		actionList.add(OpenFlowUtil.createOutputAction(beginBridge, beginBridgeEndPort, 2));

		// Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
		ib.setOrder(0);
		ib.setKey(new InstructionKey(0));
		instructions.add(ib.build());

		// Create Flow
		FlowBuilder flowBuilder = new FlowBuilder();
		flowBuilder.setMatch(matchBuilder.build());

		String flowId = "ServiceChainRewrite_" + chainId + "_" + connId + "_" + 0 + "_" + beginBridge.getDatapathId();
		logger.info("ServiceChainMacLearningFlowWriter createBeginBridgeFlow: flowId (chainId, connId, 0, endBridge) ", flowId);
		flowBuilder.setId(new FlowId(flowId));
		FlowKey key = new FlowKey(new FlowId(flowId));

		flowBuilder.setBarrier(true);
		flowBuilder.setTableId((short)0);
		flowBuilder.setKey(key);
		flowBuilder.setPriority(CHAIN_PRIORITY);
		flowBuilder.setFlowName(flowId);
		flowBuilder.setHardTimeout(0);
		flowBuilder.setIdleTimeout(0);

		return flowBuilder.setInstructions(isb.setInstruction(instructions).build()).build();
	}

	private Flow createEndBridgeFlow(NodeConnectorId inPort, MacAddress srcMac, MacAddress dstMac) {
		MatchBuilder matchBuilder = new MatchBuilder();
		EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
		EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
		ethDestinationBuilder.setAddress(OpenFlowUtil.getVirtualMac(chainId, endBridgeHop));
		ethDestinationBuilder.setMask(new MacAddress("ff:ff:00:00:00:00"));
		EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
		ethSourceBuilder.setAddress(OpenFlowUtil.getVirtualMac(chainId, connId));
		ethSourceBuilder.setMask(new MacAddress("ff:ff:00:00:00:00"));
		ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
		ethernetMatch.setEthernetSource(ethSourceBuilder.build());
		matchBuilder.setEthernetMatch(ethernetMatch.build());
		matchBuilder.setInPort(inPort);

		// Prepare Instruction
		InstructionsBuilder isb = new InstructionsBuilder();
		List<Instruction> instructions = new LinkedList<Instruction>();
		InstructionBuilder ib = new InstructionBuilder();
		ApplyActionsBuilder aab = new ApplyActionsBuilder();
		ActionBuilder abOutput = new ActionBuilder();
		ActionBuilder abRewrite = new ActionBuilder();
		List<Action> actionList = new LinkedList<Action>();

		// Rewrite Action Src
		SetDlSrcActionBuilder rewriteSrc = new SetDlSrcActionBuilder();
		rewriteSrc.setAddress(srcMac);
		abRewrite.setAction(new SetDlSrcActionCaseBuilder().setSetDlSrcAction(rewriteSrc.build()).build());
		abRewrite.setOrder(0);
		abRewrite.setKey(new ActionKey(0));
		actionList.add(abRewrite.build());

		// Rewrite Action Dst
		SetDlDstActionBuilder rewriteDst = new SetDlDstActionBuilder();
		rewriteDst.setAddress(dstMac);
		abRewrite.setAction(new SetDlDstActionCaseBuilder().setSetDlDstAction(rewriteDst.build()).build());
		abRewrite.setOrder(1);
		abRewrite.setKey(new ActionKey(1));
		actionList.add(abRewrite.build());

		// Output Action
		OutputActionBuilder output = new OutputActionBuilder();
		NodeConnectorId ncidOut = new NodeConnectorId("openflow:" +
			Long.parseLong(endBridge.getDatapathId()
				.replace(":", ""), 16) +
			":" + endBridgeEndPort.getOfport());
		output.setOutputNodeConnector(ncidOut);
		output.setMaxLength(65535);

		abOutput.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
		abOutput.setOrder(2);
		abOutput.setKey(new ActionKey(2));
		actionList.add(abOutput.build());

		// Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
		ib.setOrder(0);
		ib.setKey(new InstructionKey(0));
		instructions.add(ib.build());

		// Create Flow
		FlowBuilder flowBuilder = new FlowBuilder();
		flowBuilder.setMatch(matchBuilder.build());

		String flowId = "ServiceChainEndRewrite_" + chainId + "_" + connId + "_" + endBridgeHop + "_" + endBridge.getDatapathId();
		logger.info("ServiceChainMacLearningFlowWriter createEndBridgeFlow: flowId (chainId, connId, endBridgeHop, endBridge) ", flowId);
		flowBuilder.setId(new FlowId(flowId));
		FlowKey key = new FlowKey(new FlowId(flowId));

		flowBuilder.setBarrier(true);
		flowBuilder.setTableId((short)0);
		flowBuilder.setKey(key);
		flowBuilder.setPriority(CHAIN_PRIORITY);
		flowBuilder.setFlowName(flowId);
		flowBuilder.setHardTimeout(0);
		flowBuilder.setIdleTimeout(0);

		return flowBuilder.setInstructions(isb.setInstruction(instructions).build()).build();
	}
}

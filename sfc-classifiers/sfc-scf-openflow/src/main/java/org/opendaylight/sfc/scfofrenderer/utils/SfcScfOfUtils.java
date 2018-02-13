/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.utils;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.sfc.util.macchaining.VirtualMacAddress;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

public final class SfcScfOfUtils {
    // TODO this must be defined somewhere else; link to 'it' rather than have
    // it here
    public static final short TABLE_INDEX_CLASSIFIER = 0;
    public static final short TABLE_INDEX_INGRESS_TRANSPORT = 1;

    public static final int FLOW_PRIORITY_CLASSIFIER = 1000;
    public static final int FLOW_PRIORITY_MATCH_ANY = 5;
    public static final short NSH_MDTYPE_ONE = 0x1;
    public static final short NSH_NP_ETH = 0x3;

    private SfcScfOfUtils() {
    }

    /**
     * Get a FlowBuilder object that install the table-miss in the classifier
     * table.
     *
     * @return the FlowBuilder object, with a MatchAny match, and a single
     *         GotoTable (transport ingress) instruction
     */
    public static FlowBuilder initClassifierTable() {
        MatchBuilder match = new MatchBuilder();

        InstructionsBuilder isb = SfcOpenflowUtils.appendGotoTableInstruction(new InstructionsBuilder(),
                TABLE_INDEX_INGRESS_TRANSPORT);

        return SfcOpenflowUtils.createFlowBuilder(TABLE_INDEX_CLASSIFIER, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match,
                isb);
    }

    /**
     * create classifier DPDK output flow.
     *
     * @param outPort
     *            flow out port
     * @return the {@link FlowBuilder} object
     */
    public static FlowBuilder initClassifierDpdkOutputFlow(Long outPort) {
        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(OutputPortValues.LOCAL.toString()));

        // Action output
        List<Action> actionList = new ArrayList<>();
        String outPortStr = "output:" + outPort.toString();
        actionList.add(SfcOpenflowUtils.createActionOutPort(outPortStr, actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(TABLE_INDEX_CLASSIFIER, FLOW_PRIORITY_CLASSIFIER,
                "classifier_dpdk_output", match, isb);
    }

    /**
     * create classifier DPDK input flow.
     *
     * @param nodeName
     *            flow table node name
     * @param inPort
     *            flow in port
     * @return the {@link FlowBuilder} object
     */
    public static FlowBuilder initClassifierDpdkInputFlow(String nodeName, Long inPort) {
        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeId(nodeName), inPort);

        // Action NORMAL
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNormal(actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(TABLE_INDEX_CLASSIFIER, FLOW_PRIORITY_CLASSIFIER,
                "classifier_dpdk_input", match, isb);
    }

    /**
     * create classifier out flow. The function returns true if successful. The
     * function returns false if unsuccessful. Get a FlowBuilder object w/ the
     * classifier 'out' flow.
     *
     * @param flowKey
     *            flow key
     * @param match
     *            flow match
     * @param sfcRspInfo
     *            nsh header
     * @param outPort
     *            flow out port
     * @return create flow result
     */
    public static FlowBuilder createClassifierOutFlow(String flowKey, Match match, SfcRspInfo sfcRspInfo,
            Long outPort) {

        if (flowKey == null || sfcRspInfo == null || sfcRspInfo.getVxlanIpDst() == null) {
            return null;
        }

        String dstIp = sfcRspInfo.getVxlanIpDst().getValue();

        List<Action> theActions = buildNshActions(sfcRspInfo);
        theActions.add(SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, theActions.size()));
        theActions.add(outPort == null
                ? SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), theActions.size())
                : SfcOpenflowUtils.createActionOutPort(outPort.intValue(), theActions.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey)).setTableId(TABLE_INDEX_CLASSIFIER).setKey(new FlowKey(new FlowId(flowKey)))
                .setPriority(FLOW_PRIORITY_CLASSIFIER).setMatch(match).setInstructions(isb.build());
        return flowb;
    }

    /**
     * Build a list of actions which will be installed into the classifier.
     *
     * @param sfcRspInfo
     *            the {@link SfcRspInfo} object encapsulating all NSH related
     *            data
     * @return the List of {@link Action} related to NSH which will be pushed
     *         into the classifier
     */
    public static List<Action> buildNshActions(SfcRspInfo sfcRspInfo) {
        List<Action> theActions = new ArrayList<>();
        theActions.add(SfcOpenflowUtils.createActionNxPushNsh(theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxLoadNshMdtype(SfcScfOfUtils.NSH_MDTYPE_ONE, theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxLoadNshNp(SfcScfOfUtils.NSH_NP_ETH, theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxSetNsp(sfcRspInfo.getNshNsp(), theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxSetNsi(sfcRspInfo.getNshStartNsi(), theActions.size()));
        if (sfcRspInfo.getNshMetaC1() != null) {
            theActions.add(SfcOpenflowUtils.createActionNxSetNshc1(sfcRspInfo.getNshMetaC1(), theActions.size()));
        }
        if (sfcRspInfo.getNshMetaC2() != null) {
            theActions.add(SfcOpenflowUtils.createActionNxSetNshc1(sfcRspInfo.getNshMetaC2(), theActions.size()));
        }
        if (sfcRspInfo.getNshMetaC3() != null) {
            theActions.add(SfcOpenflowUtils.createActionNxSetNshc1(sfcRspInfo.getNshMetaC3(), theActions.size()));
        }
        if (sfcRspInfo.getNshMetaC4() != null) {
            theActions.add(SfcOpenflowUtils.createActionNxSetNshc1(sfcRspInfo.getNshMetaC4(), theActions.size()));
        }
        theActions.add(
                SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, theActions.size()));
        return theActions;
    }

    /**
     * Get a FlowBuilder object w/ the classifier 'in' flow.
     *
     * @param flowKey
     *            flow key
     * @param sfcRspInfo
     *            nsh header
     * @param outPort
     *            flow out port
     * @return create in result
     */
    public static FlowBuilder createClassifierInFlow(String flowKey, SfcRspInfo sfcRspInfo, Long outPort) {
        if (flowKey == null || sfcRspInfo == null || sfcRspInfo.getVxlanIpDst() == null) {
            return null;
        }

        MatchBuilder mb = SfcOpenflowUtils.getNshMatches(sfcRspInfo.getNshNsp(), sfcRspInfo.getNshEndNsi());

        List<Action> theActions = new ArrayList<>();
        theActions.add(SfcOpenflowUtils.createActionNxPopNsh(theActions.size()));
        theActions.add(outPort == null
                ? SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), theActions.size())
                : SfcOpenflowUtils.createActionOutPort(outPort.intValue(), theActions.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey)).setTableId(TABLE_INDEX_CLASSIFIER).setKey(new FlowKey(new FlowId(flowKey)))
                .setPriority(FLOW_PRIORITY_CLASSIFIER).setMatch(mb.build()).setInstructions(isb.build());

        return flowb;
    }

    /**
     * Get a FlowBuilder object w/ the classifier relay flow.
     *
     * @param flowKey
     *            flow key
     * @param sfcRspInfo
     *            nsh header
     * @return the FlowBuilder containing the classifier relay flow
     */
    public static FlowBuilder createClassifierRelayFlow(String flowKey, SfcRspInfo sfcRspInfo) {
        if (flowKey == null || sfcRspInfo == null || sfcRspInfo.getVxlanIpDst() == null) {
            return null;
        }
        String dstIp = sfcRspInfo.getVxlanIpDst().getValue();
        List<Action> theActions = new ArrayList<>();
        theActions
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxMoveNsp(theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxMoveNsi(theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxMoveNsc1(theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionNxMoveNsc2(theActions.size()));
        theActions.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), theActions.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);
        FlowBuilder flowb = new FlowBuilder();
        MatchBuilder mb = SfcOpenflowUtils.getNshMatches(sfcRspInfo.getNshNsp(), sfcRspInfo.getNshEndNsi());
        flowb.setId(new FlowId(flowKey)).setTableId(TABLE_INDEX_CLASSIFIER).setKey(new FlowKey(new FlowId(flowKey)))
                .setPriority(FLOW_PRIORITY_CLASSIFIER).setMatch(mb.build()).setInstructions(isb.build());
        return flowb;
    }

    /**
     * delete classifier flow. The function returns true if successful. The
     * function returns false if unsuccessful.
     *
     * @param nodeName
     *            flow table node name
     * @param flowKey
     *            flow key
     * @return delete result
     */
    public static boolean deleteClassifierFlow(String nodeName, String flowKey) {

        if (nodeName == null || flowKey == null) {
            return false;
        }

        return SfcOpenflowUtils.removeFlowFromDataStore(nodeName, new TableKey(TABLE_INDEX_CLASSIFIER),
                new FlowKey(new FlowId(flowKey)));
    }

    /**
     * create classifier out flow for MAC Chaining.
     * The function returns true if successful.
     * The function returns false if unsuccessful.
     *
     * @param  nodeName flow table node name
     * @param  flowKey  flow key
     * @param  match    flow match
     * @param  outPort  flow out port
     * @param  pathId   chain path ID
     * @param  startIndex  Firt hop in the chain
     * @return          create flow result
     */
    public static FlowBuilder createMacChainClassifierOutFlow(String nodeName, String flowKey, Match match,
                                                              String outPort, Long pathId, short startIndex) {
        int order = 0;

        VirtualMacAddress vmac = VirtualMacAddress.getForwardAddress(pathId, 0);

        if (nodeName == null || flowKey == null) {
            return null;
        }

        Action macDst = SfcOpenflowUtils.createActionSetDlDst(vmac.getHop(startIndex).getValue(), order++);

        Action out = SfcOpenflowUtils.createActionOutPort(Integer.parseInt(outPort), order);


        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
                .setTableId(TABLE_INDEX_CLASSIFIER)
                .setKey(new FlowKey(new FlowId(flowKey)))
                .setPriority(Integer.valueOf(FLOW_PRIORITY_CLASSIFIER))
                .setMatch(match)
                .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                        .createActionsInstructionBuilder(macDst, out))
                        .build());
        return flowb;
    }

    /**
     * create classifier relay flow for MAC Chaining.
     * The function returns true if successful.
     * The function returns false if unsuccessful.
     *
     * @param  nodeName flow table node name
     * @param  flowKey  flow key
     * @param  outPort  flow out port
     * @param  pathId   chain path ID
     * @param  startIndex  Firt hop in the chain
     * @param  lastIndex   Last hop in the chain
     * @return          create relay result
     */
    public static FlowBuilder createClassifierMacChainingRelayFlow(String nodeName, String flowKey, String outPort,
                                                                   Long pathId, short startIndex, short lastIndex) {
        int order = 0;

        VirtualMacAddress vmac = VirtualMacAddress.getForwardAddress(pathId, 0);

        if (nodeName == null || flowKey == null) {
            return null;
        }

        MatchBuilder mb = new MatchBuilder();

        SfcOpenflowUtils.addMatchDstMac(mb, vmac.getHop(lastIndex).getValue());

        Action macDst = SfcOpenflowUtils.createActionSetDlDst(vmac.getHop(startIndex).getValue(), order++);

        Action out = SfcOpenflowUtils.createActionOutPort(Integer.parseInt(outPort), order);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
                .setTableId(TABLE_INDEX_CLASSIFIER)
                .setKey(new FlowKey(new FlowId(flowKey)))
                .setPriority(Integer.valueOf(FLOW_PRIORITY_CLASSIFIER))
                .setMatch(mb.build())
                .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                        .createActionsInstructionBuilder(macDst, out))
                        .build());

        return flowb;
    }

    /**
     * create classifier out flow for MAC Chaining.
     * The function returns true if successful.
     * The function returns false if unsuccessful.
     *
     * @param  nodeName flow table node name
     * @param  flowKey  flow key
     * @param  outPort  flow out port
     * @param  gwMac      Gateway MAC to recovery original MAC addresses
     * @param  pathId     chain path ID
     * @param  startIndex   Firt hop in the chain
     * @return          create flow result
     */
    public static FlowBuilder createMacChainClassifierInFlow(String nodeName, String flowKey, String outPort,
                                                             String gwMac, Long pathId, short startIndex) {
        int order = 0;

        VirtualMacAddress vmac = VirtualMacAddress.getForwardAddress(pathId, 0);

        if (nodeName == null || flowKey == null) {
            return null;
        }

        MatchBuilder mb = new MatchBuilder();

        SfcOpenflowUtils.addMatchDstMac(mb, vmac.getHop(startIndex).getValue());

        //set here the gateway MAC to end the chain
        Action macDst = SfcOpenflowUtils.createActionSetDlDst(gwMac, order++);
        Action out = SfcOpenflowUtils.createActionOutPort(Integer.parseInt(outPort), order);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
                .setTableId(TABLE_INDEX_CLASSIFIER)
                .setKey(new FlowKey(new FlowId(flowKey)))
                .setPriority(Integer.valueOf(FLOW_PRIORITY_CLASSIFIER + 1))
                .setMatch(mb.build())
                .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                        .createActionsInstructionBuilder(macDst, out))
                        .build());

        return flowb;
    }
}

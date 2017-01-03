/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.utils;

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


import java.util.ArrayList;
import java.util.List;


public class SfcScfOfUtils {
    // TODO this must be defined somewhere else; link to 'it' rather than have it here
    private static final short TABLE_INDEX_CLASSIFIER = 0;
    private static final short TABLE_INDEX_INGRESS_TRANSPORT = 1;

    public static final int FLOW_PRIORITY_CLASSIFIER = 1000;
    public static final int FLOW_PRIORITY_MATCH_ANY = 5;
    public static final short NSH_MDTYPE_ONE = 0x1;
    public static final short NSH_NP_ETH = 0x3;
    public static final short TUN_GPE_NP_NSH = 0x4;

   /**
    * Get a FlowBuilder object that install the table-miss in the classifier table.
    *
    * @return          the FlowBuilder object, with a MatchAny match,
    *                   and a single GotoTable (transport ingress) instruction
    */
    public static FlowBuilder initClassifierTable() {
        MatchBuilder match = new MatchBuilder();

        InstructionsBuilder isb = SfcOpenflowUtils
                .appendGotoTableInstruction(new InstructionsBuilder(), TABLE_INDEX_INGRESS_TRANSPORT);

        return SfcOpenflowUtils.createFlowBuilder(
                TABLE_INDEX_CLASSIFIER, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);
    }

   /**
    * create classifier DPDK output flow.
    *
    * @param  outPort  flow out port
    * @return the {@link FlowBuilder} object
    */
    public static FlowBuilder initClassifierDpdkOutputFlow(Long outPort)
    {
        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(OutputPortValues.LOCAL.toString()));

        int order = 0;

        // Action output
        List<Action> actionList = new ArrayList<>();
        String outPortStr = "output:" + outPort.toString();
        actionList.add(SfcOpenflowUtils.createActionOutPort(outPortStr, order++));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                TABLE_INDEX_CLASSIFIER,
                FLOW_PRIORITY_CLASSIFIER,
                "classifier_dpdk_output",
                match,
                isb);
    }

   /**
    * create classifier DPDK input flow.
    *
    * @param  nodeName flow table node name
    * @param  inPort  flow in port
    * @return the {@link FlowBuilder} object
    */
    public static FlowBuilder initClassifierDpdkInputFlow(String nodeName, Long inPort)
    {
        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeId(nodeName), inPort);

        int order = 0;

        // Action NORMAL
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNormal(order++));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(
                TABLE_INDEX_CLASSIFIER,
                FLOW_PRIORITY_CLASSIFIER,
                "classifier_dpdk_input",
                match,
                isb);
    }

   /**
    * create classifier out flow.
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    * Get a FlowBuilder object w/ the classifier 'out' flow.
    *
    * @param  flowKey  flow key
    * @param  match    flow match
    * @param  sfcNshHeader nsh header
    * @param  outPort  flow out port
    * @return          create flow result
    */
    public static FlowBuilder createClassifierOutFlow(String flowKey, Match match, SfcNshHeader sfcNshHeader,
                                                  Long outPort) {

        if ((flowKey == null) || (sfcNshHeader == null) || (sfcNshHeader.getVxlanIpDst()==null)) {
            return null;
        }

        String dstIp = sfcNshHeader.getVxlanIpDst().getValue();

        List<Action> theActions = new ArrayList<Action>(){{
            int order = 0;
            add(SfcOpenflowUtils.createActionNxPushNsh(order++));
            add(SfcOpenflowUtils.createActionNxLoadNshMdtype(NSH_MDTYPE_ONE, order++));
            add(SfcOpenflowUtils.createActionNxLoadNshNp(NSH_NP_ETH, order++));
            add(SfcOpenflowUtils.createActionNxSetNsp(sfcNshHeader.getNshNsp(), order++));
            add(SfcOpenflowUtils.createActionNxSetNsi(sfcNshHeader.getNshStartNsi(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc1(sfcNshHeader.getNshMetaC1(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc2(sfcNshHeader.getNshMetaC2(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc3(sfcNshHeader.getNshMetaC3(), order++));
            add(SfcOpenflowUtils.createActionNxSetNshc4(sfcNshHeader.getNshMetaC4(), order++));
            add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++));
            add(SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++));
            add( outPort == null ?
                    SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++) :
                    SfcOpenflowUtils.createActionOutPort(outPort.intValue(), order++));
        }};

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
            .setTableId(TABLE_INDEX_CLASSIFIER)
            .setKey(new FlowKey(new FlowId(flowKey)))
            .setPriority(FLOW_PRIORITY_CLASSIFIER)
            .setMatch(match)
            .setInstructions(isb.build());
        return flowb;
    }

   /**
    * Get a FlowBuilder object w/ the classifier 'in' flow.
    *
    * @param  flowKey  flow key
    * @param  sfcNshHeader nsh header
    * @param  outPort  flow out port
    * @return          create in result
    */
    public static FlowBuilder createClassifierInFlow(String flowKey, SfcNshHeader sfcNshHeader, Long outPort) {
        if ((flowKey == null) || (sfcNshHeader == null) || (sfcNshHeader.getVxlanIpDst()==null)) {
            return null;
        }

        MatchBuilder mb = SfcOpenflowUtils.getNshMatches(sfcNshHeader.getNshNsp(), sfcNshHeader.getNshEndNsi());

        List<Action> theActions = new ArrayList<Action>() {{
            int order = 0;
            add(SfcOpenflowUtils.createActionNxPopNsh(order++));
            add(outPort == null ?
                    SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++) :
                    SfcOpenflowUtils.createActionOutPort(outPort.intValue(), order++));
        }};

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
            .setTableId(TABLE_INDEX_CLASSIFIER)
            .setKey(new FlowKey(new FlowId(flowKey)))
            .setPriority(FLOW_PRIORITY_CLASSIFIER)
            .setMatch(mb.build())
            .setInstructions(isb.build());

        return flowb;
    }

   /**
    * Get a FlowBuilder object w/ the classifier relay flow.
    *
    * @param  flowKey  flow key
    * @param  sfcNshHeader nsh header
    * @return          the FlowBuilder containing the classifier relay flow
    */
    public static FlowBuilder createClassifierRelayFlow(String flowKey, SfcNshHeader sfcNshHeader) {
        if ((flowKey == null) || (sfcNshHeader == null) || (sfcNshHeader.getVxlanIpDst()==null)) {
            return null;
        }

        MatchBuilder mb = SfcOpenflowUtils.getNshMatches(sfcNshHeader.getNshNsp(), sfcNshHeader.getNshEndNsi());

        String dstIp = sfcNshHeader.getVxlanIpDst().getValue();
        List<Action> theActions = new ArrayList<Action>() {{
            int order = 0;
            add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++));
            add(SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++));
            add(SfcOpenflowUtils.createActionNxMoveNsp(order++));
            add(SfcOpenflowUtils.createActionNxMoveNsi(order++));
            add(SfcOpenflowUtils.createActionNxMoveNsc1(order++));
            add(SfcOpenflowUtils.createActionNxMoveNsc2(order++));
            add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++));
        }};

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(theActions);
        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
            .setTableId(TABLE_INDEX_CLASSIFIER)
            .setKey(new FlowKey(new FlowId(flowKey)))
            .setPriority(FLOW_PRIORITY_CLASSIFIER)
            .setMatch(mb.build())
            .setInstructions(isb.build());
        return flowb;
    }

   /**
    * delete classifier flow.
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  nodeName flow table node name
    * @param  flowKey  flow key
    * @return          delete result
    */
    public static boolean deleteClassifierFlow(String nodeName, String flowKey) {

       if ((nodeName == null) || (flowKey == null)) {
           return false;
       }

       return SfcOpenflowUtils.removeFlowFromDataStore(nodeName,
               new TableKey(TABLE_INDEX_CLASSIFIER),
               new FlowKey(new FlowId(flowKey)));
   }
}

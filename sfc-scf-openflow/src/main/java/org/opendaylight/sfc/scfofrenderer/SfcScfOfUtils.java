/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import java.util.ArrayList;
import java.util.List;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;

public class SfcScfOfUtils {
    private static final short TABLE_INDEX_CLASSIFIER = 0;
    private static final short TABLE_INDEX_INGRESS_TRANSPORT = 1;

    private static final int FLOW_PRIORITY_CLASSIFIER = 1000;
    private static final int FLOW_PRIORITY_MATCH_ANY = 5;
    private static final short NSH_MDTYPE_ONE = 0x1;
    private static final short NSH_NP_ETH = 0x3;
    private static final short TUN_GPE_NP_NSH = 0x4;

   /**
    * Initialize classifier flow table.
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  nodeName flow table node name
    * @return          initialization result
    */
    public static boolean initClassifierTable(String nodeName) {
        int order = 0;

        if (nodeName == null) {
            return false;
        }

        List<Instruction> instructions = new ArrayList<Instruction>();
        GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(TABLE_INDEX_INGRESS_TRANSPORT);

        InstructionBuilder ib = new InstructionBuilder();
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order++);
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());
        instructions.add(ib.build());

        MatchBuilder match = new MatchBuilder();

        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(TABLE_INDEX_CLASSIFIER, FLOW_PRIORITY_MATCH_ANY, "MatchAny",
                match, isb);
        return SfcOpenflowUtils.writeFlowToDataStore(nodeName, fb);
    }

   /**
    * create classifier out flow.
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  nodeName flow table node name
    * @param  flowKey  flow key
    * @param  match    flow match
    * @param  sfcNshHeader nsh header
    * @param  outPort  flow out port
    * @return          create flow result
    */
    public static boolean createClassifierOutFlow(String nodeName, String flowKey, Match match, SfcNshHeader sfcNshHeader,
            Long outPort) {
        int order = 0;

        if ((nodeName == null) || (flowKey == null) || (sfcNshHeader == null) || (sfcNshHeader.getVxlanIpDst()==null)) {
            return false;
        }

        String dstIp = sfcNshHeader.getVxlanIpDst().getValue();
        Action pushNsh = SfcOpenflowUtils.createActionNxPushNsh(order++);
        Action loadNshMdtype = SfcOpenflowUtils.createActionNxLoadNshMdtype(NSH_MDTYPE_ONE, order++);
        Action loadNshNp = SfcOpenflowUtils.createActionNxLoadNshNp(NSH_NP_ETH, order++);
        Action setNsp = SfcOpenflowUtils.createActionNxSetNsp(sfcNshHeader.getNshNsp(), order++);
        Action setNsi = SfcOpenflowUtils.createActionNxSetNsi(sfcNshHeader.getNshStartNsi(), order++);
        Action setC1 = SfcOpenflowUtils.createActionNxSetNshc1(sfcNshHeader.getNshMetaC1(), order++);
        Action setC2 = SfcOpenflowUtils.createActionNxSetNshc2(sfcNshHeader.getNshMetaC2(), order++);
        Action setC3 = SfcOpenflowUtils.createActionNxSetNshc3(sfcNshHeader.getNshMetaC3(), order++);
        Action setC4 = SfcOpenflowUtils.createActionNxSetNshc4(sfcNshHeader.getNshMetaC4(), order++);
        Action loadTunGpeNp = SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++);
        Action setTunIpDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);

        Action out = null;
        if (outPort == null) {
            out = SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++);
        } else {
            out = SfcOpenflowUtils.createActionOutPort(outPort.intValue(), order++);
        }

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
            .setTableId(TABLE_INDEX_CLASSIFIER)
            .setKey(new FlowKey(new FlowId(flowKey)))
            .setPriority(Integer.valueOf(FLOW_PRIORITY_CLASSIFIER))
            .setMatch(match)
            .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                .createActionsInstructionBuilder(pushNsh, loadNshMdtype, loadNshNp, setNsp, setNsi, setC1, setC2, setC3, setC4, loadTunGpeNp, setTunIpDst, out))
                .build());
        return SfcOpenflowUtils.writeFlowToDataStore(nodeName, flowb);
    }

   /**
    * create classifier in flow.
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  nodeName flow table node name
    * @param  flowKey  flow key
    * @param  sfcNshHeader nsh header
    * @param  outPort  flow out port
    * @return          create in result
    */
    public static boolean createClassifierInFlow(String nodeName, String flowKey, SfcNshHeader sfcNshHeader, Long outPort) {
        int order = 0;

        if ((nodeName == null) || (flowKey == null) || (sfcNshHeader == null) || (sfcNshHeader.getVxlanIpDst()==null)) {
            return false;
        }

        MatchBuilder mb = new MatchBuilder();

        SfcOpenflowUtils.addMatchNshNsp(mb, sfcNshHeader.getNshNsp());
        SfcOpenflowUtils.addMatchNshNsi(mb, sfcNshHeader.getNshEndNsi());

        /* Pop NSH */
        Action popNsh = SfcOpenflowUtils.createActionNxPopNsh(order++);

        Action out = null;
        if (outPort == null) {
            out = SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++);
        } else {
            out = SfcOpenflowUtils.createActionOutPort(outPort.intValue(), order++);
        }

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
            .setTableId(TABLE_INDEX_CLASSIFIER)
            .setKey(new FlowKey(new FlowId(flowKey)))
            .setPriority(Integer.valueOf(FLOW_PRIORITY_CLASSIFIER))
            .setMatch(mb.build())
            .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                .createActionsInstructionBuilder(popNsh, out))
                .build());
        return SfcOpenflowUtils.writeFlowToDataStore(nodeName, flowb);
    }

   /**
    * create classifier relay flow.
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  nodeName flow table node name
    * @param  flowKey  flow key
    * @param  sfcNshHeader nsh header
    * @return          create relay result
    */
    public static boolean createClassifierRelayFlow(String nodeName, String flowKey, SfcNshHeader sfcNshHeader) {
        int order = 0;

        if ((nodeName == null) || (flowKey == null) || (sfcNshHeader == null) || (sfcNshHeader.getVxlanIpDst()==null)) {
            return false;
        }

        MatchBuilder mb = new MatchBuilder();

        SfcOpenflowUtils.addMatchNshNsp(mb, sfcNshHeader.getNshNsp());
        SfcOpenflowUtils.addMatchNshNsi(mb, sfcNshHeader.getNshEndNsi());

        String dstIp = sfcNshHeader.getVxlanIpDst().getValue();
        Action loadTunGpeNp = SfcOpenflowUtils.createActionNxLoadTunGpeNp(TUN_GPE_NP_NSH, order++);
        Action setTunIpDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);
        Action mvNsp = SfcOpenflowUtils.createActionNxMoveNsp(order++);
        Action mvNsi = SfcOpenflowUtils.createActionNxMoveNsi(order++);
        Action mvC1 = SfcOpenflowUtils.createActionNxMoveNsc1(order++);
        Action mvC2 = SfcOpenflowUtils.createActionNxMoveNsc2(order++);
        Action out = SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flowKey))
            .setTableId(TABLE_INDEX_CLASSIFIER)
            .setKey(new FlowKey(new FlowId(flowKey)))
            .setPriority(Integer.valueOf(FLOW_PRIORITY_CLASSIFIER))
            .setMatch(mb.build())
            .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                .createActionsInstructionBuilder(loadTunGpeNp, setTunIpDst, mvNsp, mvNsi, mvC1, mvC2, out))
                .build());
        return SfcOpenflowUtils.writeFlowToDataStore(nodeName, flowb);
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

       return SfcOpenflowUtils.removeFlowFromDataStore(nodeName, new TableKey(TABLE_INDEX_CLASSIFIER),
                                                            new FlowKey(new FlowId(flowKey.toString())));
   }
}

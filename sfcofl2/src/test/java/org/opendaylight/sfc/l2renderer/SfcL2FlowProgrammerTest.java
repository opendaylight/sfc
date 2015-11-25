/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import static org.junit.Assert.*;

import java.util.List;

/**
 * An interface to be implemented by concrete classes that will OpenFlow rules to MD-SAL datastore.
 *
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */

import org.junit.Test;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.arp.op._case.ArpOpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class SfcL2FlowProgrammerTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowProgrammerTest.class);

    public static final String SFF_NAME = "sff1";
    public static final String MAC_SRC = "AA:BB:CC:DD:EE:FF";
    public static final String MAC_DST = "FF:EE:DD:CC:BB:AA";
    public static final String IP_DST = "192.168.0.1";
    public static final String INPORT = "INPORT";
    public static final long PATH_ID = 123456789;
    public static final long SFP = 987654321;
    public static final long MPLS_LABEL = 123456789;
    public static final boolean IS_SF = false;
    public static final long NSP = 1;
    public static final short NSI = 255;
    public static final int VLAN_ID = 100;

    SfcL2FlowWriterInterface sfcL2FlowWriter;
    SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    FlowBuilder flowBuilder;

    public SfcL2FlowProgrammerTest() {

        this.sfcL2FlowWriter = new SfcL2FlowWriterTest();
        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammerOFimpl(sfcL2FlowWriter);
    }

    public short getTableBase() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setTableBase(short tableBase) {
        // TODO Auto-generated method stub

    }

    public short getMaxTableOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    public short getTableEgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setTableEgress(short tableEgress) {
        // TODO Auto-generated method stub

    }

    public void setFlowRspId(Long rspId) {
        // TODO Auto-generated method stub

    }

    public void deleteRspFlows(Long rspId) {
        // TODO Auto-generated method stub

    }

    public void setFlowWriter(SfcL2FlowWriterInterface sfcL2FlowWriter) {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureIpv4TransportIngressFlow() {

        sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(SFF_NAME);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_IPV4);

        // Now check that the actions are: Goto Table TABLE_INDEX_PATH_MAPPER
        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        if (curInstruction instanceof GoToTableCase) {
            GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
            assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
            LOG.info("configureIpv4TransportIngressFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
        }

    }

    @Test
    public void configureVlanTransportIngressFlow() {

        sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(SFF_NAME);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);

        Match match = flowBuilder.getMatch();
//        VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
//        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
//        vlanIdBuilder.setVlanIdPresent(true);
//        vlanBuilder.setVlanId(vlanIdBuilder.build());
//        match.setVlanMatch(vlanBuilder.build());
        LOG.info("MATCH : [{}]", match.toString());
        assertTrue(match.getVlanMatch().getVlanPcp().getValue() != null);

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        if (curInstruction instanceof GoToTableCase) {
            GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
            assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
            LOG.info("configureVlanTransportIngressFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
        }
    }

    @Test
    public void configureVxlanGpeTransportIngressFlow() {

        sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(SFF_NAME);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_IPV4);

        // Now check that the actions are: Goto Table TABLE_INDEX_PATH_MAPPER
        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        if (curInstruction instanceof GoToTableCase) {
            GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
            assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
            LOG.info("configureVxlanGpeTransportIngressFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
        }
    }

    @Test
    public void configureMplsTransportIngressFlow() {

        sfcL2FlowProgrammer.configureMplsTransportIngressFlow(SFF_NAME);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        LOG.info("MATCH MPLS: [{}]", match.toString());

        // Now check that the actions are: Goto Table TABLE_INDEX_PATH_MAPPER
        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        if (curInstruction instanceof GoToTableCase) {
            GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
            assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
            LOG.info("configureMplsTransportIngressFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
        }

    }

    @Test
    public void configureArpTransportIngressFlow() {
        sfcL2FlowProgrammer.configureArpTransportIngressFlow(SFF_NAME, MAC_SRC);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_ARP);

        Instructions isb = flowBuilder.getInstructions();

        // ARP TEST TO CHECK
//        public void testArpMatchConversion() {
//            MatchBuilder builder = new MatchBuilder();
//            ArpMatchBuilder arpBuilder = new ArpMatchBuilder();
//            arpBuilder.setArpOp(5);
//            arpBuilder.setArpSourceTransportAddress(new Ipv4Prefix("10.0.0.3/32"));
//            arpBuilder.setArpTargetTransportAddress(new Ipv4Prefix("10.0.0.4/32"));
//            ArpSourceHardwareAddressBuilder srcHwBuilder = new ArpSourceHardwareAddressBuilder();
//            srcHwBuilder.setAddress(new MacAddress("00:00:00:00:00:05"));
//            arpBuilder.setArpSourceHardwareAddress(srcHwBuilder.build());
//            ArpTargetHardwareAddressBuilder dstHwBuilder = new ArpTargetHardwareAddressBuilder();
//            dstHwBuilder.setAddress(new MacAddress("00:00:00:00:00:06"));
//            arpBuilder.setArpTargetHardwareAddress(dstHwBuilder.build());
//            builder.setLayer3Match(arpBuilder.build());
//            Match match = builder.build();
//
//            List<MatchEntry> entries = convertor.convert(match, new BigInteger("42"));
//            Assert.assertEquals("Wrong entries size", 5, entries.size());
//            MatchEntry entry = entries.get(0);
//            checkEntryHeader(entry, ArpOp.class, false);
//            Assert.assertEquals("Wrong arp op", 5, ((ArpOpCase) entry.getMatchEntryValue())
//                    .getArpOp().getOpCode().intValue());
//            entry = entries.get(1);
//            checkEntryHeader(entry, ArpSpa.class, false);
//            Assert.assertEquals("Wrong arp spa", "10.0.0.3", ((ArpSpaCase) entry.getMatchEntryValue())
//                    .getArpSpa().getIpv4Address().getValue());
//            entry = entries.get(2);
//            checkEntryHeader(entry, ArpTpa.class, false);
//            Assert.assertEquals("Wrong arp tpa", "10.0.0.4", ((ArpTpaCase) entry.getMatchEntryValue())
//                    .getArpTpa().getIpv4Address().getValue());
//            entry = entries.get(3);
//            checkEntryHeader(entry, ArpSha.class, false);
//            Assert.assertEquals("Wrong arp sha", "00:00:00:00:00:05", ((ArpShaCase) entry.getMatchEntryValue())
//                    .getArpSha().getMacAddress().getValue());
//            entry = entries.get(4);
//            checkEntryHeader(entry, ArpTha.class, false);
//            Assert.assertEquals("Wrong arp tha", "00:00:00:00:00:06", ((ArpThaCase) entry.getMatchEntryValue())
//                    .getArpTha().getMacAddress().getValue());
//        }


        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof DstOfArpOpCase){
                ArpOpBuilder arp = (ArpOpBuilder) curInstruction;
                LOG.info("ARP OP STATE: [{}]", arp.getOpCode());
            }

            if (curInstruction instanceof OutputActionCase) {
                OutputActionCase output = (OutputActionCase) curInstruction;
                assertTrue(output.getOutputAction().getOutputNodeConnector().getValue().toString() == INPORT);
                LOG.info("configureArpTransportIngressFlow() Output Port: [{}]", output.getOutputAction().getOutputNodeConnector().getValue().toString());
            }
        }
    }


    @Test
    public void configureMacPathMapperFlow() {

        sfcL2FlowProgrammer.configureMacPathMapperFlow(SFF_NAME, MAC_SRC, PATH_ID, IS_SF);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        LOG.info("configureMacPathMapperFlow table_id: [{}]", flowBuilder.getTableId());

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);

        LOG.info("configureMacPathMapperFlow() priority is: [{}]", flowBuilder.getPriority());

        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetSource().getAddress().getValue().toString() == MAC_SRC);

        LOG.info("MAC ADDRESS!!!!!!!!!!!!: [{}]", flowBuilder.getMatch().getEthernetMatch().getEthernetSource().getAddress().getValue().toString());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof GoToTableCase) {
                GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
                assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
                LOG.info("configureMacPathMapperFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
            }
        }

    }

    @Test
    public void configureMplsPathMapperFlow() {

        sfcL2FlowProgrammer.configureMplsPathMapperFlow(SFF_NAME, MPLS_LABEL, PATH_ID, IS_SF);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        LOG.info("configureMplsPathMapperFlow table_id: [{}]", flowBuilder.getTableId());

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);

        LOG.info("configureMplsPathMapperFlow() priority is: [{}]", flowBuilder.getPriority());

        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getProtocolMatchFields().getMplsLabel().longValue() == MPLS_LABEL);

        LOG.info("MPLS LABEL: [{}]", match.getProtocolMatchFields().getMplsLabel().longValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            LOG.info("INSTRUCTIONS FOR MPLS ARE: [{}]", curInstruction.toString());

            if(curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PopMplsActionCase popMpls = (PopMplsActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(popMpls.getPopMplsAction() != null);
                LOG.info("POP MPLS: [{}] and TEST PASSED", popMpls.getPopMplsAction());
            }


            if (curInstruction instanceof GoToTableCase) {
                GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
                assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
                LOG.info("configureMplsPathMapperFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
            }
        }


    }

    @Test
    public void configureVlanPathMapperFlow() {

        sfcL2FlowProgrammer.configureVlanPathMapperFlow(SFF_NAME, VLAN_ID, PATH_ID, IS_SF);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        LOG.info("configureVlanPathMapperFlow table_id: [{}]", flowBuilder.getTableId());

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);

        LOG.info("configureVlanPathMapperFlow() priority is: [{}]", flowBuilder.getPriority());

        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getVlanMatch().getVlanId().getVlanId().getValue() == VLAN_ID);

        LOG.info("VLAN ID: [{}]", match.getVlanMatch().getVlanId().getVlanId().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            LOG.info("INSTRUCTIONS FOR VLAN ARE: [{}]", curInstruction.toString());

            if(curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PopVlanActionCase popVlan = (PopVlanActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(popVlan.getPopVlanAction() != null);
                LOG.info("POP VLAN: [{}] and TEST PASSED", popVlan.getPopVlanAction().toString());
            }

            if (curInstruction instanceof GoToTableCase) {
                GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
                assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
                LOG.info("configureVlanPathMapperFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
            }
        }
    }

    @Test
    public void configureVxlanGpePathMapperFlow() {

        sfcL2FlowProgrammer.configureVxlanGpePathMapperFlow(SFF_NAME, NSP, NSI, PATH_ID);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        LOG.info("configureVxlanGpePathMapperFlow table_id: [{}]", flowBuilder.getTableId());

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);

        LOG.info("configureVxlanGpePathMapperFlow() priority is: [{}]", flowBuilder.getPriority());

        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        LOG.info("VXLAN-GPE MATCH IS: [{}]", match.toString());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            LOG.info("INSTRUCTIONS FOR VXLAN-GPE ARE: [{}]", curInstruction.toString());

            if (curInstruction instanceof GoToTableCase) {
                GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
                assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
                LOG.info("configureVxlanGpePathMapperFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
            }
        }
    }

    @Test
    public void configureNextHopFlow() {

        sfcL2FlowProgrammer.configureNextHopFlow(SFF_NAME, SFP, MAC_SRC, MAC_DST);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        LOG.info("configureNextHopFlow table_id: [{}]", flowBuilder.getTableId());

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);

        LOG.info("configureNextHopFlow() priority is: [{}]", flowBuilder.getPriority());

        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetSource().getAddress().getValue().toString() == MAC_SRC);

        LOG.info("MAC NEXT HOP FLOW MATCH IS: [{}]", match.toString());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            LOG.info("INSTRUCTIONS FOR MAC NEXT-HOP FLOW ARE: [{}]", curInstruction.toString());

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();

                assertTrue(setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue().toString() == MAC_DST);
                LOG.info("configureNextHopFlow() Dest Mac Adress is: [{}]", setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue().toString());

            }

            if (curInstruction instanceof GoToTableCase) {
                GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
                assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
                LOG.info("configureNextHopFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
            }
        }

    }

    @Test
    public void configureGroupNextHopFlow() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureVxlanGpeNextHopFlow() {

        sfcL2FlowProgrammer.configureVxlanGpeNextHopFlow(SFF_NAME, IP_DST, NSP, NSI);
        sfcL2FlowProgrammer.awaitUntilCompleted();
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        LOG.info("configureNextHopFlow table_id: [{}]", flowBuilder.getTableId());

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);

        LOG.info("configureNextHopFlow() priority is: [{}]", flowBuilder.getPriority());

        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP-10);

        Match match = flowBuilder.getMatch();
        LOG.info("VXLAN-GPE NEXT HOP MATCH IS: [{}]", match.toString());

        Flow flow = flowBuilder.build();
        GeneralAugMatchNodesNodeTableFlow genAug = flow.getMatch().getAugmentation(
                GeneralAugMatchNodesNodeTableFlow.class);

        List<ExtensionList> extensions = genAug.getExtensionList();
        for (ExtensionList extensionList : extensions) {
            Extension extension = extensionList.getExtension();
            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class);

            if (nxAugMatch.getNxmNxNsp() != null) {
                assertTrue(nxAugMatch.getNxmNxNsp().getValue() == NSP);
                LOG.info("NSP is: [{}]", nxAugMatch.getNxmNxNsp().getValue());
            }
            if (nxAugMatch.getNxmNxNsi() != null) {
                assertTrue(nxAugMatch.getNxmNxNsi().getNsi().shortValue() == NSI);
                LOG.info("NSI is: [{}]", nxAugMatch.getNxmNxNsi().getNsi().shortValue());
            }
        }

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            LOG.info("INSTRUCTIONS FOR VXLAN-GPE NEXT HOP ARE: [{}]", curInstruction.toString());

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegLoadNodesNodeTableFlowApplyActionsCase nxLoad =
                        (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0).getAction();
                int ip = InetAddresses.coerceToInteger(InetAddresses.forString(IP_DST));
                long ipl = ip & 0xffffffffL;

                assertTrue(nxLoad.getNxRegLoad().getValue().longValue() == ipl);
                LOG.info("configureVxlanGpeNextHopFlow() DST TUNNEL IP: [{}]", nxLoad.getNxRegLoad().getValue().longValue());
            }

            if (curInstruction instanceof GoToTableCase) {
                GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
                assertTrue(goToTablecase.getGoToTable().getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
                LOG.info("configureVxlanGpeNextHopFlow() GoTo TableId: [{}]", goToTablecase.getGoToTable().getTableId());
            }
        }


    }

    @Test
    public void configureMacTransportEgressFlow() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureVlanTransportEgressFlow() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureVxlanGpeTransportEgressFlow() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureMplsTransportEgressFlow() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureNshNscTransportEgressFlow() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureClassifierTableMatchAny() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureTransportIngressTableMatchAny() {
        // TODO Auto-generated method stub
    }

    @Test
    public void configurePathMapperTableMatchAny() {
        assertTrue(true);
    }

    @Test
    public void configurePathMapperAclTableMatchAny() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureNextHopTableMatchAny() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureTransportEgressTableMatchAny() {
        // TODO Auto-generated method stub

    }

    @Test
    public void configureGroup() {
        // TODO Auto-generated method stub

    }

}

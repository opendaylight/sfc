/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import static org.junit.Assert.assertTrue;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc1Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc2Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class SfcL2FlowProgrammerTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowProgrammerTest.class);

    private static final String SFF_NAME = "sff1";
    private static final String MAC_SRC = "AA:BB:CC:DD:EE:FF";
    private static final String MAC_DST = "FF:EE:DD:CC:BB:AA";
    private static final String IP_DST = "192.168.0.1";
    private static final String INPORT = "INPORT";
    private static final String PORT = "1";
    private static final long PATH_ID = 123456;
    private static final long SFP = 987654;
    private static final long MPLS_LABEL = 123456789;
    private static final boolean IS_SF = false;
    private static final long NSP = 1;
    private static final short NSI = 255;
    private static final int VLAN_ID = 100;
    private static final long GROUP_ID = 1;
    private static final String GROUP_NAME = "GROUP";
    private static final boolean DSCP = false;
    private static final boolean LAST_HOP = false;
    private static final boolean PKTIN = false;

    SfcL2FlowWriterInterface sfcL2FlowWriter;
    SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    FlowBuilder flowBuilder;

    public SfcL2FlowProgrammerTest() {

        this.sfcL2FlowWriter = new SfcL2FlowWriterTest();
        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammerOFimpl(sfcL2FlowWriter);
    }



    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureIpv4TransportIngressFlow(String)}
     */
    @Test
    public void configureIpv4TransportIngressFlow() {

        sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);
        //TODO Two test cases for TCP/UDP shall be done
        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_IPV4);
        LOG.info("configureIpv4TransportIngressFlow() Match EtherType: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue());

        // Now check that the actions are: Goto Table TABLE_INDEX_PATH_MAPPER
        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        LOG.info("configureIpv4TransportIngressFlow() Action NextTableId: [{}]",
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVlanTransportIngressFlow(String)}
     */
    @Test
    public void configureVlanTransportIngressFlow() {

        sfcL2FlowProgrammer.configureVlanTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getVlanMatch().getVlanId().isVlanIdPresent() == true);
        LOG.info("configureVlanTransportIngressFlow() VLAN Match : [{}]",
                match.getVlanMatch().getVlanId().isVlanIdPresent());

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        LOG.info("configureVlanTransportIngressFlow() Action NextTableId: [{}]",
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeTransportIngressFlow(String)}
     */
    @Test
    public void configureVxlanGpeTransportIngressFlow() {

        sfcL2FlowProgrammer.configureVxlanGpeTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_IPV4);
        LOG.info("configureVxlanGpeTransportIngressFlow() Match EtherType: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue());

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        LOG.info("configureVxlanGpeTransportIngressFlow() Action NextTableId: [{}]",
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureMplsTransportIngressFlow(String)}
     */
    @Test
    public void configureMplsTransportIngressFlow() {

        sfcL2FlowProgrammer.configureMplsTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        LOG.info("configureMplsTransportIngressFlow() Match EtherType: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue());
        //TODO Two test cases for MPLS UCAST/MCAST shall be done

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        LOG.info("configureMplsTransportIngressFlow() Action NextTableId: [{}]",
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureArpTransportIngressFlow(String, String)}
     */
    @Test
    public void configureArpTransportIngressFlow() {
        sfcL2FlowProgrammer.configureArpTransportIngressFlow(SFF_NAME, MAC_SRC);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_ARP_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_ARP);
        LOG.info("configureArpTransportIngressFlow() Match EtherType: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue());

        Instructions isb = flowBuilder.getInstructions();

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                NxActionRegLoadNodesNodeTableFlowApplyActionsCase regLoad = (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(2).getAction();
                NxActionRegLoadNodesNodeTableFlowApplyActionsCase regLoad2 = (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(3).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove2 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(4).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove3 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(5).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove4 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(6).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove5 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(7).getAction();
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(8).getAction();

                DstNxArpShaCase arpSha = (DstNxArpShaCase) regLoad2.getNxRegLoad().getDst().getDstChoice();
                DstNxArpThaCase arpTha = (DstNxArpThaCase) regMove2.getNxRegMove().getDst().getDstChoice();
                DstNxRegCase regCase = (DstNxRegCase) regMove3.getNxRegMove().getDst().getDstChoice();
                DstOfArpTpaCase arpTpa = (DstOfArpTpaCase) regMove4.getNxRegMove().getDst().getDstChoice();
                DstOfArpSpaCase arpSpa = (DstOfArpSpaCase) regMove5.getNxRegMove().getDst().getDstChoice();

                assertTrue(regMove.getNxRegMove().getDst().getDstChoice() != null);
                assertTrue(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue().toString() == MAC_SRC);
                assertTrue(regLoad.getNxRegLoad().getValue().intValue() == SfcOpenflowUtils.ARP_REPLY);
                assertTrue(arpSha.isNxArpSha() == true);
                assertTrue(arpTha.isNxArpTha() == true);
                assertTrue(regCase.getNxReg().getCanonicalName() == "org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0");
                assertTrue(arpTpa.isOfArpTpa() == true);
                assertTrue(arpSpa.isOfArpSpa() == true);
                assertTrue(output.getOutputAction().getOutputNodeConnector().getValue() == INPORT);

                LOG.info("configureArpTransportIngressFlow() Action ArpOp: [{}], SrcMac: [{}], Output port: [{}]",
                        regLoad.getNxRegLoad().getValue().intValue(),
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue().toString(),
                        output.getOutputAction().getOutputNodeConnector().getValue());

            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureMplsPathMapperFlow(String, long, long, boolean)}
     */
    @Test
    public void configureMplsPathMapperFlow() {

        sfcL2FlowProgrammer.configureMplsPathMapperFlow(SFF_NAME, MPLS_LABEL, PATH_ID, IS_SF);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetType().getType().getValue() == SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        assertTrue(match.getProtocolMatchFields().getMplsLabel().longValue() == MPLS_LABEL);
        LOG.info("configureMplsPathMapperFlow() Match EtherType: [{}], Mpls Label: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue(),
                match.getProtocolMatchFields().getMplsLabel().longValue());
        //TODO Two test cases for MPLS UCAST/MCAST shall be done

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            checkMetadata(curInstruction, PATH_ID);
            LOG.info("configureMplsPathMapperFlow() Action Metadata PathId: [{}]", PATH_ID);

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PopMplsActionCase popMpls = (PopMplsActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(popMpls.getPopMplsAction() != null);
                LOG.info("configureMplsPathMapperFlow() Action popMpls: [{}]",
                        popMpls.getPopMplsAction().toString());
            }

            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
            LOG.info("configureMplsPathMapperFlow() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVlanPathMapperFlow(String, int, long, boolean)}
     */
    @Test
    public void configureVlanPathMapperFlow() {

        sfcL2FlowProgrammer.configureVlanPathMapperFlow(SFF_NAME, VLAN_ID, PATH_ID, IS_SF);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getVlanMatch().getVlanId().getVlanId().getValue() == VLAN_ID);
        LOG.info("configureVlanPathMapperFlow() Match VlanId: [{}]",
                match.getVlanMatch().getVlanId().getVlanId().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            checkMetadata(curInstruction, PATH_ID);
            LOG.info("configureVlanPathMapperFlow() Action Metadata PathId: [{}]", PATH_ID);

            if(curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PopVlanActionCase popVlan = (PopVlanActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(popVlan.getPopVlanAction() != null);
                LOG.info("configureVlanPathMapperFlow() Action PopVlan: [{}]",
                        popVlan.getPopVlanAction().toString());
            }

            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
            LOG.info("configureVlanPathMapperFlow() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureMacNextHopFlow(String, long, String, String)}
     */
    @Test
    public void configureMacNextHopFlow() {

        sfcL2FlowProgrammer.configureMacNextHopFlow(SFF_NAME, SFP, MAC_SRC, MAC_DST);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetSource().getAddress().getValue().toString() == MAC_SRC);
        assertTrue(match.getMetadata().getMetadata().longValue() == SFP);
        LOG.info("configureNextHopFlow() Match SrcMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetSource().getAddress().getValue().toString(),
                match.getMetadata().getMetadata().longValue());


        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue().toString() == MAC_DST);
                LOG.info("configureNextHopFlow() Action Set DstMac: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue().toString());
            }

            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNextHopFlow() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureGroupNextHopFlow(String, long, String, long, String)}
     */
    @Test
    public void configureGroupNextHopFlow() {

        sfcL2FlowProgrammer.configureGroupNextHopFlow(SFF_NAME, SFP, MAC_SRC, GROUP_ID, GROUP_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetSource().getAddress().getValue().toString() == MAC_SRC);
        assertTrue(match.getMetadata().getMetadata().longValue() == SFP);
        LOG.info("configureGroupNextHopFlow() Match SrcMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetSource().getAddress().getValue().toString(),
                match.getMetadata().getMetadata().longValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                GroupActionCase group = (GroupActionCase) action.getApplyActions().getAction().get(0).getAction();

                assertTrue(group.getGroupAction().getGroup().equals(GROUP_NAME));
                assertTrue(group.getGroupAction().getGroupId() == GROUP_ID);
                LOG.info("configureGroupNextHopFlow() Action GroupName: [{}], GroupId: [{}]",
                        group.getGroupAction().getGroup(),
                        group.getGroupAction().getGroupId());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeNextHopFlow(String, String, long, short)}
     */
    @Test
    public void configureVxlanGpeNextHopFlow() {

        sfcL2FlowProgrammer.configureVxlanGpeNextHopFlow(SFF_NAME, IP_DST, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP);
        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegLoadNodesNodeTableFlowApplyActionsCase nxLoad =
                        (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0).getAction();
                int ip = InetAddresses.coerceToInteger(InetAddresses.forString(IP_DST));
                long ipl = ip & 0xffffffffL;

                assertTrue(nxLoad.getNxRegLoad().getValue().longValue() == ipl);
                LOG.info("configureVxlanGpeNextHopFlow() Action DstTunIP long: [{}]",
                        nxLoad.getNxRegLoad().getValue().longValue());
            }

            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureVxlanGpeNextHopFlow() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }


    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVlanTransportEgressFlow(String, String, String, int, String, long, boolean, boolean)}
     */
    @Test
    public void configureVlanTransportEgressFlow() {

        sfcL2FlowProgrammer.configureVlanTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID, DSCP, PKTIN);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetDestination().getAddress().getValue() == MAC_DST);
        assertTrue(match.getMetadata().getMetadata().longValue() == PATH_ID);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PushVlanActionCase pushVlan = (PushVlanActionCase) action.getApplyActions().getAction().get(0).getAction();
                SetFieldCase vlanId = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(3).getAction();

                assertTrue(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue().toString() == MAC_SRC);
                assertTrue(pushVlan.getPushVlanAction().getEthernetType() == SfcOpenflowUtils.ETHERTYPE_VLAN);
                assertTrue(vlanId.getSetField().getVlanMatch().getVlanId().getVlanId().getValue() == VLAN_ID);
                assertTrue(output.getOutputAction().getOutputNodeConnector().getValue().toString().equals(PORT));
                LOG.info("configureVlanTransportEgressFlow() Action SrcMac is: [{}], PushVlan Ethertype: [{}], VlanId: [{}], OuputPort: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue().toString(),
                        pushVlan.getPushVlanAction().getEthernetType().toString(),
                        vlanId.getSetField().getVlanMatch().getVlanId().getVlanId().getValue(),
                        output.getOutputAction().getOutputNodeConnector().getValue().toString());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeTransportEgressFlow(String, long, short, String, boolean)}
     */
    @Test
    public void configureVxlanGpeTransportEgressFlow() {

        sfcL2FlowProgrammer.configureVxlanGpeTransportEgressFlow(SFF_NAME, NSP, NSI, PORT, LAST_HOP);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS);
        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove2 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(1).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove3 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(2).getAction();
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(3).getAction();

                DstNxNshc1Case nshC1dst = (DstNxNshc1Case) regMove.getNxRegMove().getDst().getDstChoice();
                DstNxNshc2Case nshC2dst = (DstNxNshc2Case) regMove2.getNxRegMove().getDst().getDstChoice();
                DstNxTunIdCase tunIdDst = (DstNxTunIdCase) regMove3.getNxRegMove().getDst().getDstChoice();

                assertTrue(nshC1dst.isNxNshc1Dst() == true);
                assertTrue(nshC2dst.isNxNshc2Dst() == true);
                assertTrue(tunIdDst.isNxTunId() == true);
                assertTrue(output.getOutputAction().getOutputNodeConnector().getValue() == PORT);
                LOG.info("configureVxlanGpeTransportEgressFlow() Action OutputPort: [{}]",
                        output.getOutputAction().getOutputNodeConnector().getValue().toString());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureMplsTransportEgressFlow(String, String, String, long, String, long, boolean, boolean)}
     */
    @Test
    public void configureMplsTransportEgressFlow() {

        sfcL2FlowProgrammer.configureMplsTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, MPLS_LABEL, PORT, PATH_ID, DSCP, PKTIN);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getEthernetMatch().getEthernetDestination().getAddress().getValue() == MAC_DST);
        assertTrue(match.getMetadata().getMetadata().longValue() == PATH_ID);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PushMplsActionCase pushMpls = (PushMplsActionCase) action.getApplyActions().getAction().get(0).getAction();
                SetFieldCase mplsLabel = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(3).getAction();

                assertTrue(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue().toString() == MAC_SRC);
                assertTrue(pushMpls.getPushMplsAction().getEthernetType() == SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
                assertTrue(mplsLabel.getSetField().getProtocolMatchFields().getMplsLabel() == MPLS_LABEL);
                assertTrue(output.getOutputAction().getOutputNodeConnector().getValue().toString().equals(PORT));
                LOG.info("configureMplsTransportEgressFlow() Action SrcMac: [{}], PushMplsEthertype: [{}], LabelId: [{}], OutputPort: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue().toString(),
                        pushMpls.getPushMplsAction().getEthernetType().toString(),
                        mplsLabel.getSetField().getProtocolMatchFields().getMplsLabel(),
                        output.getOutputAction().getOutputNodeConnector().getValue().toString());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureNshNscTransportEgressFlow(String, long, short, String)}
     */
    @Test
    public void configureNshNscTransportEgressFlow() {

        sfcL2FlowProgrammer.configureNshNscTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS+10);

        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(output.getOutputAction().getOutputNodeConnector().getValue().toString().equals(PORT));
                LOG.info("configureNshNscTransportEgressFlow() Action OutputPort is: [{}]",
                        output.getOutputAction().getOutputNodeConnector().getValue().toString());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureTransportIngressTableMatchAny(String)}
     */
    @Test
    public void configureTransportIngressTableMatchAny() {

        sfcL2FlowProgrammer.configureTransportIngressTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_INGRESS_TRANSPORT_TABLE);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                DropActionCase drop = (DropActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(drop.getDropAction() != null);
                LOG.info("configureTransportIngressTableMatchAny() Action is: [{}]",
                        drop.getDropAction().toString());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configurePathMapperTableMatchAny(String)}
     */
    @Test
    public void configurePathMapperTableMatchAny() {

        sfcL2FlowProgrammer.configurePathMapperTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER_ACL);
            LOG.info("configurePathMapperTableMatchAny() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER_ACL);
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configurePathMapperAclTableMatchAny(String)}
     */
    @Test
    public void configurePathMapperAclTableMatchAny() {

        sfcL2FlowProgrammer.configurePathMapperAclTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER_ACL);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
            LOG.info("configurePathMapperAclTableMatchAny() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureNextHopTableMatchAny(String)}
     */
    @Test
    public void configureNextHopTableMatchAny() {

        sfcL2FlowProgrammer.configureNextHopTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNextHopTableMatchAny() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureTransportEgressTableMatchAny(String)}
     */
    @Test
    public void configureTransportEgressTableMatchAny() {

        sfcL2FlowProgrammer.configureTransportEgressTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertTrue(flowBuilder.getTableId() == SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertTrue(flowBuilder.getPriority() == SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                DropActionCase drop = (DropActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(drop.getDropAction() != null);
                LOG.info("configureTransportEgressTableMatchAny() Action is: [{}]",
                        drop.getDropAction().toString());
            }
        }
    }

    public void checkGoToTable(Instruction curInstruction, short nextTableId) {

        if (curInstruction instanceof GoToTableCase) {
            GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
            assertTrue(goToTablecase.getGoToTable().getTableId() == nextTableId);
        }
    }

    public void checkMetadata(Instruction curInstruction, long pathId) {

        if (curInstruction instanceof WriteMetadataCase) {
            WriteMetadataCase metadata = (WriteMetadataCase) curInstruction;
            assertTrue(metadata.getWriteMetadata().getMetadata().longValue() == pathId);
        }
    }

    public void checkMatchNsh(Flow flow, long nsp, short nsi) {

        GeneralAugMatchNodesNodeTableFlow genAug = flow.getMatch().getAugmentation(
                GeneralAugMatchNodesNodeTableFlow.class);

        List<ExtensionList> extensions = genAug.getExtensionList();
        for (ExtensionList extensionList : extensions) {
            Extension extension = extensionList.getExtension();
            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class);

            if (nxAugMatch.getNxmNxNsp() != null) {
                assertTrue(nxAugMatch.getNxmNxNsp().getValue() == NSP);
            }
            if (nxAugMatch.getNxmNxNsi() != null) {
                assertTrue(nxAugMatch.getNxmNxNsi().getNsi().shortValue() == NSI);
            }
        }

    }
}


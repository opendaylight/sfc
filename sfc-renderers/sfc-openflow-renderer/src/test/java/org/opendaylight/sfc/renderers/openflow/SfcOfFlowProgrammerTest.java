/*
 * Copyright (c) 2014, 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.openflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.google.common.net.InetAddresses;
import java.util.List;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.renderers.openflow.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.util.macchaining.VirtualMacAddress;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitNodesNodeTableFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SFC OpenFlow Programmer Test.
 *
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */

public class SfcOfFlowProgrammerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOfFlowProgrammerTest.class);

    private static final String SFF_NAME = "sff1";
    private static final String MAC_SRC = "AA:BB:CC:DD:EE:FF";
    private static final String MAC_DST = "FF:EE:DD:CC:BB:AA";
    private static final String IP_SRC = "192.168.0.5";
    private static final String IP_DST = "192.168.0.1";
    private static final String INPORT = "INPORT";
    private static final String PORT = "1";
    private static final long PATH_ID = 123456;
    private static final long PATH_ID_SMALL = 63;
    private static final long SFP = 987654;
    private static final long MPLS_LABEL = 1234567;
    private static final boolean IS_SF = false;
    private static final long NSP = 1;
    private static final short NSI = 255;
    private static final int VLAN_ID = 100;
    private static final long GROUP_ID = 1;
    private static final String GROUP_NAME = "GROUP";
    private static final boolean PKTIN = false;
    private static final short TABLE_BASE = 30;
    private static final short TABLE_EGRESS = 80;

    private SfcOfFlowWriterInterface sfcOfFlowWriter;
    private SfcOfFlowProgrammerImpl sfcOfFlowProgrammer;
    private FlowBuilder flowBuilder;

    public SfcOfFlowProgrammerTest() {
        this.flowBuilder = null;
        this.sfcOfFlowWriter = mock(SfcOfFlowWriterInterface.class);
        // Configure Mockito to store the FlowBuilder when writeFlowToConfig()
        // is called
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            flowBuilder = (FlowBuilder) args[2];
            return null;
        }).when(this.sfcOfFlowWriter).writeFlow(anyLong(), anyString(), (FlowBuilder) anyObject());

        // Configure Mockito to return the FlowBuilder stored by
        // writeFlowToConfig()
        // when getFlowBuilder() is called
        // The following didnt work, since it used the value of flowBuilder at
        // the time of invocation:
        // when(sfcOfFlowWriter.getFlowBuilder).thenReturn(flowBuilder)
        doAnswer(invocation -> flowBuilder).when(this.sfcOfFlowWriter).getFlowBuilder();

        this.sfcOfFlowProgrammer = new SfcOfFlowProgrammerImpl(sfcOfFlowWriter);
    }

    /**
     * Unit test to check match and action fields.
     *
     */
    @Test
    public void configureIpv4TransportIngressFlow() {
        sfcOfFlowProgrammer.configureIpv4TransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_INGRESS);
        // TODO Two test cases for TCP/UDP shall be done
        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(),
                SfcOpenflowUtils.ETHERTYPE_IPV4);
        LOG.info("configureIpv4TransportIngressFlow() Match EtherType: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue());

        // Now check that the actions are: Goto Table TABLE_INDEX_PATH_MAPPER
        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
        LOG.info("configureIpv4TransportIngressFlow() Action NextTableId: [{}]",
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureVlanTransportIngressFlow() {
        sfcOfFlowProgrammer.configureVlanTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getVlanMatch().getVlanId().isVlanIdPresent());
        LOG.info("configureVlanTransportIngressFlow() VLAN Match : [{}]",
                match.getVlanMatch().getVlanId().isVlanIdPresent());

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
        LOG.info("configureVlanTransportIngressFlow() Action NextTableId: [{}]",
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureEthNshTransportIngressFlow() {
        sfcOfFlowProgrammer.configureEthNshTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_INGRESS);

        assertEquals(OpenflowConstants.ETHERTYPE_NSH,
                flowBuilder.getMatch().getEthernetMatch().getEthernetType().getType().getValue().longValue());

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        LOG.info("configureNshVxgpeTransportIngressFlow() Action NextTableId: [{}]",
                SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNshTransportIngressFlow() {
        sfcOfFlowProgrammer.configureNshTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_INGRESS);

        assertEquals(OpenflowConstants.PACKET_TYPE_NSH,
                flowBuilder.getMatch().getPacketTypeMatch().getPacketType().longValue());

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        LOG.info("configureNshVxgpeTransportIngressFlow() Action NextTableId: [{}]",
                SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureMplsTransportIngressFlow() {
        sfcOfFlowProgrammer.configureMplsTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(),
                SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        LOG.info("configureMplsTransportIngressFlow() Match EtherType: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue());
        // TODO Two test cases for MPLS UCAST/MCAST shall be done

        Instructions isb = flowBuilder.getInstructions();
        Instruction curInstruction = isb.getInstruction().get(0).getInstruction();

        checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
        LOG.info("configureMplsTransportIngressFlow() Action NextTableId: [{}]",
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureArpTransportIngressFlow() {
        sfcOfFlowProgrammer.configureArpTransportIngressFlow(SFF_NAME, MAC_SRC);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_ARP_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(),
                SfcOpenflowUtils.ETHERTYPE_ARP);
        LOG.info("configureArpTransportIngressFlow() Match EtherType: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue());

        Instructions isb = flowBuilder.getInstructions();

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {
            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0)
                                .getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                NxActionRegLoadNodesNodeTableFlowApplyActionsCase regLoad =
                        (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(2)
                                .getAction();

                assertTrue(regMove.getNxRegMove().getDst().getDstChoice() != null);
                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        MAC_SRC);
                assertEquals(regLoad.getNxRegLoad().getValue().intValue(), SfcOpenflowUtils.ARP_REPLY);

                NxActionRegLoadNodesNodeTableFlowApplyActionsCase regLoad2 =
                        (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(3)
                                .getAction();
                DstNxArpShaCase arpSha = (DstNxArpShaCase) regLoad2.getNxRegLoad().getDst().getDstChoice();
                assertTrue(arpSha.isNxArpSha());

                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove2 =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(4)
                                .getAction();
                DstNxArpThaCase arpTha = (DstNxArpThaCase) regMove2.getNxRegMove().getDst().getDstChoice();
                assertTrue(arpTha.isNxArpTha());

                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove3 =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(5)
                                .getAction();
                DstNxRegCase regCase = (DstNxRegCase) regMove3.getNxRegMove().getDst().getDstChoice();
                assertEquals(regCase.getNxReg().getCanonicalName(),
                        "org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0");

                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove4 =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(6)
                                .getAction();
                DstOfArpTpaCase arpTpa = (DstOfArpTpaCase) regMove4.getNxRegMove().getDst().getDstChoice();
                assertTrue(arpTpa.isOfArpTpa());

                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove5 =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(7)
                                .getAction();
                DstOfArpSpaCase arpSpa = (DstOfArpSpaCase) regMove5.getNxRegMove().getDst().getDstChoice();
                assertTrue(arpSpa.isOfArpSpa());

                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(8).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), INPORT);

                LOG.info("configureArpTransportIngressFlow() Action ArpOp: [{}], SrcMac: [{}], Output port: [{}]",
                        regLoad.getNxRegLoad().getValue().intValue(),
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureMplsPathMapperFlow() {

        sfcOfFlowProgrammer.configureMplsPathMapperFlow(SFF_NAME, MPLS_LABEL, PATH_ID, IS_SF);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(),
                SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        assertEquals(match.getProtocolMatchFields().getMplsLabel().longValue(), MPLS_LABEL);
        LOG.info("configureMplsPathMapperFlow() Match EtherType: [{}], Mpls Label: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue(),
                match.getProtocolMatchFields().getMplsLabel());
        // TODO Two test cases for MPLS UCAST/MCAST shall be done

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            checkMetadata(curInstruction, PATH_ID);
            LOG.info("configureMplsPathMapperFlow() Action Metadata PathId: [{}]", PATH_ID);

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PopMplsActionCase popMpls = (PopMplsActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(popMpls.getPopMplsAction() != null);
                LOG.info("configureMplsPathMapperFlow() Action popMpls: [{}]", popMpls.getPopMplsAction().toString());
            }

            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
            LOG.info("configureMplsPathMapperFlow() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureVlanPathMapperFlow() {
        sfcOfFlowProgrammer.configureVlanPathMapperFlow(SFF_NAME, VLAN_ID, PATH_ID, IS_SF);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getVlanMatch().getVlanId().getVlanId().getValue().intValue(), VLAN_ID);
        LOG.info("configureVlanPathMapperFlow() Match VlanId: [{}]",
                match.getVlanMatch().getVlanId().getVlanId().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            checkMetadata(curInstruction, PATH_ID);
            LOG.info("configureVlanPathMapperFlow() Action Metadata PathId: [{}]", PATH_ID);

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PopVlanActionCase popVlan = (PopVlanActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(popVlan.getPopVlanAction() != null);
                LOG.info("configureVlanPathMapperFlow() Action PopVlan: [{}]", popVlan.getPopVlanAction().toString());
            }

            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
            LOG.info("configureVlanPathMapperFlow() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureMacNextHopFlow() {

        sfcOfFlowProgrammer.configureMacNextHopFlow(SFF_NAME, SFP, MAC_SRC, MAC_DST);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
        assertEquals(match.getMetadata().getMetadata().longValue(), SFP);
        LOG.info("configureNextHopFlow() Match SrcMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetSource().getAddress().getValue(),
                match.getMetadata().getMetadata().longValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue(),
                        MAC_DST);
                LOG.info("configureNextHopFlow() Action Set DstMac: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue());
            }

            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNextHopFlow() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }


    /**
     * Unit test to check match and action fields from mac chaining flows of no transparent SFs.
     *
     */
    @Test
    public void configureMacChainingNoTransparentSfsNextHopFlow() {


        VirtualMacAddress hopMac = VirtualMacAddress.getForwardAddress(PATH_ID, 0);
        String vmac = hopMac.getHop(NSI).getValue();
        String nextVmac = hopMac.getHop((short)(NSI - 1)).getValue();


        sfcOfFlowProgrammer.configureMacChainingNextHopFlow(SFF_NAME, vmac, MAC_DST, nextVmac, false);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        // transparent equals false -> VMAC should be match in the destination MAC address
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), vmac);

        LOG.info("configureMacChainingNextHopFlow() Match DstMac: [{}]",
                match.getEthernetMatch().getEthernetDestination().getAddress().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase setField1 = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(setField1.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        nextVmac);
                LOG.info("configureNextHopFlow() Action Set SrcMac: [{}]",
                        setField1.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue());

                SetFieldCase setField2 = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                assertEquals(setField2.getSetField().getEthernetMatch()
                        .getEthernetDestination().getAddress().getValue(), MAC_DST);
                LOG.info("configureNextHopFlow() Action Set DstMac: [{}]",
                        setField2.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue());
            }

            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNextHopFlow() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }

    /**
     * Unit test to check match and action fields from mac chaining flows of transparent SFs.
     *
     */
    @Test
    public void configureMacChainingTransparentSfsNextHopFlow() {


        VirtualMacAddress hopMac = VirtualMacAddress.getForwardAddress(PATH_ID, 0);
        String vmac = hopMac.getHop(NSI).getValue();
        String nextVmac = hopMac.getHop((short)(NSI - 1)).getValue();


        sfcOfFlowProgrammer.configureMacChainingNextHopFlow(SFF_NAME, vmac, MAC_DST, nextVmac, true);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        // transparent equals true -> VMAC should be match in the source MAC address
        assertEquals(match.getEthernetMatch().getEthernetSource().getAddress().getValue(), vmac);

        LOG.info("configureMacChainingNextHopFlow() Match DstMac: [{}]",
                match.getEthernetMatch().getEthernetSource().getAddress().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase setField1 = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(setField1.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        nextVmac);
                LOG.info("configureNextHopFlow() Action Set SrcMac: [{}]",
                        setField1.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue());

                SetFieldCase setField2 = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                assertEquals(setField2.getSetField().getEthernetMatch()
                        .getEthernetDestination().getAddress().getValue(), MAC_DST);
                LOG.info("configureNextHopFlow() Action Set DstMac: [{}]",
                        setField2.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue());
            }

            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNextHopFlow() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureGroupNextHopFlow() {

        sfcOfFlowProgrammer.configureGroupNextHopFlow(SFF_NAME, SFP, MAC_SRC, GROUP_ID, GROUP_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
        assertEquals(match.getMetadata().getMetadata().longValue(), SFP);
        LOG.info("configureGroupNextHopFlow() Match SrcMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetSource().getAddress().getValue(),
                match.getMetadata().getMetadata().longValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                GroupActionCase group = (GroupActionCase) action.getApplyActions().getAction().get(0).getAction();

                assertEquals(group.getGroupAction().getGroup(), GROUP_NAME);
                assertEquals(group.getGroupAction().getGroupId().longValue(), GROUP_ID);
                LOG.info("configureGroupNextHopFlow() Action GroupName: [{}], GroupId: [{}]",
                        group.getGroupAction().getGroup(), group.getGroupAction().getGroupId());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNshVxgpeNextHopFlow() {
        sfcOfFlowProgrammer.configureNshVxgpeNextHopFlow(SFF_NAME, IP_DST, null, NSP, NSI);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_NEXT_HOP);
        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegLoadNodesNodeTableFlowApplyActionsCase nxLoad =
                        (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0)
                                .getAction();
                int ip = InetAddresses.coerceToInteger(InetAddresses.forString(IP_DST));
                long ipl = ip & 0xffffffffL;

                assertEquals(nxLoad.getNxRegLoad().getValue().longValue(), ipl);
                LOG.info("configureNshVxgpeNextHopFlow() Action DstTunIP long: [{}]",
                        nxLoad.getNxRegLoad().getValue().longValue());
            }

            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNshVxgpeNextHopFlow() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNshEthNextHopFlow() {
        sfcOfFlowProgrammer.configureNshEthNextHopFlow(SFF_NAME, MAC_SRC, MAC_DST, NSP, NSI);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_NEXT_HOP);
        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase setFieldCase = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(MAC_SRC,
                        setFieldCase.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue());

                setFieldCase = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                assertEquals(MAC_DST,
                        setFieldCase.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue());

                LOG.info("configureNshVxgpeNextHopFlow() Action: [{}]", action);
            }

            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNshEthNextHopFlow() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureVlanSfTransportEgressFlow() {
        sfcOfFlowProgrammer.configureVlanSfTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID_SMALL,
                PKTIN);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS + 10);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);
        assertEquals(match.getMetadata().getMetadata().longValue(), PATH_ID_SMALL);
        LOG.info("configureMacTransportEgressFlow() Match DstMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetDestination().getAddress().getValue(),
                match.getMetadata().getMetadata().longValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase dscp = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                PushVlanActionCase pushVlan =
                        (PushVlanActionCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase vlanId = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();

                assertEquals(dscp.getSetField().getIpMatch().getIpDscp().getValue().shortValue(), PATH_ID_SMALL);
                assertEquals(pushVlan.getPushVlanAction().getEthernetType().intValue(),
                        SfcOpenflowUtils.ETHERTYPE_VLAN);
                assertEquals(vlanId.getSetField().getVlanMatch().getVlanId().getVlanId().getValue().intValue(),
                        VLAN_ID);

                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(3).getAction();
                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        MAC_SRC);
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(4).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);

                LOG.info("configureMacTransportEgressFlow() Action SrcMac: [{}], OutputPort: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }


    /**
     * Unit test to check match and action fields from flows to forward packet to other SFFs.
     *
     */
    @Test
    public void configureMacChainingSffTransportEgressFlow() {

        VirtualMacAddress hopMac = VirtualMacAddress.getForwardAddress(PATH_ID, 0);
        String vmac = hopMac.getHop(NSI).getValue();

        sfcOfFlowProgrammer.configureMacChainingSfTransportEgressFlow(SFF_NAME, MAC_DST, PORT, vmac);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);

        LOG.info("configureMacTransportEgressFlow() Match DstMac: [{}]",
                match.getEthernetMatch().getEthernetDestination().getAddress().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;

                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue(),
                        vmac);
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(1).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);

                LOG.info("configureMacTransportEgressFlow() Action DstMac: [{}], OutputPort: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue(),
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows to forward packet to SFs.
     *
     */
    @Test
    public void configureMacChainingSfTransportEgressFlow() {

        sfcOfFlowProgrammer.configureMacChainingSfTransportEgressFlow(SFF_NAME, MAC_DST, PORT, null);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);

        LOG.info("configureMacTransportEgressFlow() Match DstMac: [{}]",
                match.getEthernetMatch().getEthernetDestination().getAddress().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;

                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);

                LOG.info("configureMacTransportEgressFlow() Action  OutputPort: [{}]",
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureVlanTransportEgressFlow() {

        sfcOfFlowProgrammer.configureVlanTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS + 10);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);
        assertEquals(match.getMetadata().getMetadata().longValue(), PATH_ID);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PushVlanActionCase pushVlan =
                        (PushVlanActionCase) action.getApplyActions().getAction().get(0).getAction();
                SetFieldCase vlanId = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();

                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        MAC_SRC);
                assertEquals(pushVlan.getPushVlanAction().getEthernetType().intValue(),
                        SfcOpenflowUtils.ETHERTYPE_VLAN);
                assertEquals(vlanId.getSetField().getVlanMatch().getVlanId().getVlanId().getValue().intValue(),
                        VLAN_ID);

                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(3).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);
                LOG.info("Action SrcMac is: [{}], PushVlan Ethertype: [{}], VlanId: [{}], OuputPort: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        pushVlan.getPushVlanAction().getEthernetType().toString(),
                        vlanId.getSetField().getVlanMatch().getVlanId().getVlanId().getValue(),
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNshEthTransportEgressFlow() {
        sfcOfFlowProgrammer.configureNshEthTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS);
        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);
                LOG.info("configureNshEthTransportEgressFlow() Action OutputPort: [{}]",
                        output.getOutputAction().getOutputNodeConnector());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNshVxgpeTransportEgressFlow() {
        sfcOfFlowProgrammer.configureNshVxgpeTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        int priority = flowBuilder.getPriority();
        assertTrue(priority == SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS
                || priority == SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS + 5);
        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;

                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0)
                                .getAction();
                DstNxTunIdCase tunIdDst = (DstNxTunIdCase) regMove.getNxRegMove().getDst().getDstChoice();
                assertTrue(tunIdDst.isNxTunId());

                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(1).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), INPORT);
                LOG.info("configureNshVxgpeTransportEgressFlow() Action OutputPort: [{}]",
                        output.getOutputAction().getOutputNodeConnector());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNshVxgpeLastHopTransportEgressFlow() {
        sfcOfFlowProgrammer.configureNshVxgpeLastHopTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS + 5);
        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {
            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;

                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove0 =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0)
                                .getAction();
                DstNxTunIpv4DstCase tunIpv4Dst = (DstNxTunIpv4DstCase) regMove0.getNxRegMove().getDst().getDstChoice();
                assertTrue(tunIpv4Dst.isNxTunIpv4Dst());

                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove1 =
                        (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(1)
                                .getAction();
                DstNxTunIdCase tunIdDst = (DstNxTunIdCase) regMove1.getNxRegMove().getDst().getDstChoice();
                assertTrue(tunIdDst.isNxTunId());

                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(2).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), INPORT);
                LOG.info("configureNshVxgpeTransportEgressFlow() Action OutputPort: [{}]",
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureMplsTransportEgressFlow() {
        sfcOfFlowProgrammer.configureMplsTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, MPLS_LABEL, PORT, PATH_ID);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS + 10);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);
        assertEquals(match.getMetadata().getMetadata().longValue(), PATH_ID);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PushMplsActionCase pushMpls =
                        (PushMplsActionCase) action.getApplyActions().getAction().get(0).getAction();
                SetFieldCase mplsLabel = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();

                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        MAC_SRC);
                assertEquals(pushMpls.getPushMplsAction().getEthernetType().intValue(),
                        SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
                assertEquals(mplsLabel.getSetField().getProtocolMatchFields().getMplsLabel().longValue(), MPLS_LABEL);

                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(3).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);
                LOG.info("Action SrcMac: [{}], PushMplsEthertype: [{}], LabelId: [{}], OutputPort: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                        pushMpls.getPushMplsAction().getEthernetType().toString(),/*
    public static void addMatchTunGpeNp(MatchBuilder match, short tunGpeNp) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxTunGpeNp(new NxmNxTunGpeNpBuilder().setValue(Short.valueOf(tunGpeNp)).build()).build();
        addExtension(match, NxmNxTunGpeNpKey.class, am);
    }
*/

                        mplsLabel.getSetField().getProtocolMatchFields().getMplsLabel(),
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNshNscTransportEgressFlow() {
        sfcOfFlowProgrammer.configureNshNscTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_TRANSPORT_EGRESS + 10);

        checkMatchNsh(flowBuilder.build(), NSP, NSI);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                assertEquals(1, action.getApplyActions().getAction().size());
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);
                LOG.info("configureNshNscTransportEgressFlow() Action OutputPort is: [{}]",
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureTransportIngressTableMatchAny() {
        sfcOfFlowProgrammer.configureTransportIngressTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                DropActionCase drop = (DropActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(drop.getDropAction() != null);
                LOG.info("configureTransportIngressTableMatchAny() Action is: [{}]", drop.getDropAction().toString());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureTransportIngressTableMatchAnyResubmit() {
        sfcOfFlowProgrammer.configureTransportIngressTableMatchAnyResubmit(SFF_NAME,
             NwConstants.LPORT_DISPATCHER_TABLE);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            checkActionHasResubmit(curInstruction, NwConstants.LPORT_DISPATCHER_TABLE);
        }
    }


    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configurePathMapperTableMatchAny() {
        sfcOfFlowProgrammer.configurePathMapperTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {
            Instruction curInstruction = instruction.getInstruction();
            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER_ACL);
            LOG.info("configurePathMapperTableMatchAny() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER_ACL);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configurePathMapperAclTableMatchAny() {
        sfcOfFlowProgrammer.configurePathMapperAclTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER_ACL);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
            LOG.info("configurePathMapperAclTableMatchAny() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureNextHopTableMatchAny() {
        sfcOfFlowProgrammer.configureNextHopTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            checkGoToTable(curInstruction, SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNextHopTableMatchAny() Action NextTableId: [{}]",
                    SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }
    }

    /**
     * Unit test to check match and action fields from flows.
     *
     */
    @Test
    public void configureTransportEgressTableMatchAny() {
        sfcOfFlowProgrammer.configureTransportEgressTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcOfFlowProgrammerImpl.OF_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction
                 instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                DropActionCase drop = (DropActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(drop.getDropAction() != null);
                LOG.info("configureTransportEgressTableMatchAny() Action is: [{}]", drop.getDropAction().toString());
            }
        }
    }

    /**
     * Unit test to check application coexistence works for NSH flows.
     */
    @Test
    public void appCoexistenceNsh() {
        sfcOfFlowProgrammer.setTableBase(TABLE_BASE);
        sfcOfFlowProgrammer.setTableEgress(TABLE_EGRESS);

        // When checking the table offsets, we need to subtract 2 to compensate
        // for:
        // - TABLE_INDEX_CLASSIFIER=0 - which is not used for AppCoexistence
        // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for
        // AppCoexistence
        // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should return 20

        // Check that transport ingress flows are written to the
        // correct table
        // Notice: TransportIngress doesn't use the offset, as it will always be
        // table 0
        sfcOfFlowProgrammer.configureEthNshTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);

        sfcOfFlowProgrammer.configureNshTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);

        // Check that configureNshVxgpeNextHopFlow() is written to the correct
        // table
        sfcOfFlowProgrammer.configureNshVxgpeNextHopFlow(SFF_NAME, IP_DST, null, NSP, NSI);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(1).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2),
                true);

        // Check that configureNshVxgpeLastHopTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureNshVxgpeLastHopTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureNshVxgpeTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureNshVxgpeTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureNshNscTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureNshNscTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureNshVxgpeAppCoexistTransportEgressFlow() is
        // written
        // to the correct table and that it goes to TABLE_EGRESS
        sfcOfFlowProgrammer.configureNshVxgpeAppCoexistTransportEgressFlow(SFF_NAME, NSP, NSI, IP_SRC);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);
        checkActionHasResubmit(curInstruction, TABLE_EGRESS);
    }

    /**
     * Unit test to check application coexistence works for VLAN flows.
     */
    @Test
    public void appCoexistenceVlan() {
        sfcOfFlowProgrammer.setTableBase(TABLE_BASE);
        sfcOfFlowProgrammer.setTableEgress(TABLE_EGRESS);

        // When checking the table offsets, we need to subtract 2 to compensate
        // for:
        // - TABLE_INDEX_CLASSIFIER=0 - which is not used for AppCoexistence
        // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for
        // AppCoexistence
        // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should return 20

        // Check that configureVlanTransportIngressFlow() is written to the
        // correct table
        // Notice: TransportIngress doesn't use the offset, as it will always be
        // table 0
        sfcOfFlowProgrammer.configureVlanTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);

        // Check that configureVlanPathMapperFlow() is written to the correct
        // table
        sfcOfFlowProgrammer.configureVlanPathMapperFlow(SFF_NAME, VLAN_ID, PATH_ID_SMALL, true);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(2).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);

        // Check that configureVlanPathMapperFlow() is written to the correct
        // table
        sfcOfFlowProgrammer.configureVlanPathMapperFlow(SFF_NAME, VLAN_ID, PATH_ID, false);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(2).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);

        // Check that configureMacNextHopFlow() is written to the correct table
        sfcOfFlowProgrammer.configureMacNextHopFlow(SFF_NAME, SFP, MAC_SRC, MAC_DST);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(1).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2),
                true);

        // Check that configureVlanSfTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureVlanSfTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID_SMALL,
                true);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVlanSfTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureVlanSfTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID_SMALL,
                false);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVlanTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureVlanTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVlanLastHopTransportEgressFlow() is written
        // to the correct table and that it goes to TABLE_EGRESS
        sfcOfFlowProgrammer.configureVlanLastHopTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);
        // checkGoToTable(curInstruction, TABLE_EGRESS, true);
    }

    /**
     * Unit test to check application coexistence works for Mac Chaining flows.
     */
    @Test
    public void appCoexistenceMacChaining() {
        sfcOfFlowProgrammer.setTableBase(TABLE_BASE);
        sfcOfFlowProgrammer.setTableEgress(TABLE_EGRESS);

        // When checking the table offsets, we need to subtract 2 to compensate
        // for:
        // - TABLE_INDEX_CLASSIFIER=0 - which is not used for AppCoexistence
        // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for
        // AppCoexistence
        // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should return 20

        // Check that configureMacChainingTransportIngressFlow() is written to the
        // correct table
        // Notice: TransportIngress doesn't use the offset, as it will always be
        // table 0
        sfcOfFlowProgrammer.configureMacChainingTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);

        // Check that configureMacChainingNextHopFlow() is written to the correct table

        VirtualMacAddress hopMac = VirtualMacAddress.getForwardAddress(PATH_ID_SMALL, 0);
        String vmac = hopMac.getHop(NSI).getValue();
        String nextVmac = hopMac.getHop((short)(NSI - 1)).getValue();

        sfcOfFlowProgrammer.configureMacChainingNextHopFlow(SFF_NAME, vmac, MAC_DST, nextVmac, false);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(1).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2),
                true);

        // Check that configureMacChainingSfTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureMacChainingSfTransportEgressFlow(SFF_NAME, MAC_DST, PORT, null);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);
    }

    /**
     * Unit test to check application coexistence works for MPLS flows.
     */
    @Test
    public void appCoexistenceMpls() {
        sfcOfFlowProgrammer.setTableBase(TABLE_BASE);
        sfcOfFlowProgrammer.setTableEgress(TABLE_EGRESS);

        // Check that configureMplsTransportIngressFlow() is written to the
        // correct table
        // Notice: TransportIngress doesnt use the offset, as it will always be
        // table 0
        sfcOfFlowProgrammer.configureMplsTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2),
                true);

        // Check that configureMplsPathMapperFlow() is written to the correct
        // table
        sfcOfFlowProgrammer.configureMplsPathMapperFlow(SFF_NAME, MPLS_LABEL, PATH_ID_SMALL, true);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);

        // Check that configureMplsPathMapperFlow() is written to the correct
        // table
        sfcOfFlowProgrammer.configureMplsPathMapperFlow(SFF_NAME, MPLS_LABEL, PATH_ID, false);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);

        // Mpls NextHop uses MacNextHop which is tested in appCoexistenceVlan

        // Check that configureMplsTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcOfFlowProgrammer.configureMplsTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, MPLS_LABEL, PORT, PATH_ID);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureMplsLastHopTransportEgressFlow() is written
        // to the correct table and that it goes to TABLE_EGRESS
        sfcOfFlowProgrammer.configureMplsLastHopTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, MPLS_LABEL, PORT,
                PATH_ID);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);
        // checkGoToTable(curInstruction, TABLE_EGRESS, true);
    }

    /**
     * Unit test to check application coexistence works for the MatchAny flows.
     */
    @Test
    public void appCoexistenceMatchAny() {
        sfcOfFlowProgrammer.setTableBase(TABLE_BASE);
        sfcOfFlowProgrammer.setTableEgress(TABLE_EGRESS);

        // Test Classifier Match Any table offset.
        // No flows should be written
        sfcOfFlowProgrammer.configureClassifierTableMatchAny(SFF_NAME);
        assertEquals(sfcOfFlowWriter.getFlowBuilder(), null);

        // Test TransportIngress Match Any table offset.
        // No flows should be written
        sfcOfFlowProgrammer.configureTransportIngressTableMatchAny(SFF_NAME);
        assertEquals(sfcOfFlowWriter.getFlowBuilder(), null);

        // Test PathMapper Match Any table offset.
        // It should go to PathMapperAcl + offset
        sfcOfFlowProgrammer.configurePathMapperTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER_ACL + TABLE_BASE - 2),
                true);

        // Test PathMapperAcl Match Any table offset.
        // It should go to NextHop + offset
        sfcOfFlowProgrammer.configurePathMapperAclTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER_ACL + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);

        // Test NextHop Match Any table offset.
        // It should go to TransportEgress + offset
        sfcOfFlowProgrammer.configureNextHopTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2),
                true);

        // Test TransportEgress Match Any table offset.
        // It should do a drop
        sfcOfFlowProgrammer.configureTransportEgressTableMatchAny(SFF_NAME);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkDrop(curInstruction, true);
    }

    /**
     * Unit test to check app coexistence works for the PathMapperAcl flows.
     */
    @Test
    public void appCoexistencePathMapperAcl() {
        sfcOfFlowProgrammer.setTableBase(TABLE_BASE);
        sfcOfFlowProgrammer.setTableEgress(TABLE_EGRESS);
        sfcOfFlowProgrammer.configurePathMapperAclFlow(SFF_NAME, IP_SRC, IP_DST, (short) PATH_ID);
        flowBuilder = sfcOfFlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcOfFlowProgrammerImpl.TABLE_INDEX_PATH_MAPPER_ACL + TABLE_BASE - 2);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(1).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcOfFlowProgrammerImpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);
    }

    private void checkGoToTable(Instruction curInstruction, short nextTableId) {
        checkGoToTable(curInstruction, nextTableId, false);
    }

    private void checkGoToTable(Instruction curInstruction, short nextTableId, boolean mustExist) {

        if (curInstruction instanceof GoToTableCase) {
            GoToTableCase goToTablecase = (GoToTableCase) curInstruction;
            assertEquals(goToTablecase.getGoToTable().getTableId().shortValue(), nextTableId);
        } else {
            if (mustExist) {
                LOG.info("checkGoToTable expecting GoToTableCase, but received [{}]", curInstruction);
                fail();
            }
        }
    }

    private void checkActionHasResubmit(Instruction curInstruction, short nextTableId) {
        assertTrue(curInstruction instanceof ApplyActionsCase);
        boolean resubmitActionFound = false;
        for (Action action : ((ApplyActionsCase) curInstruction).getApplyActions().getAction()) {
            LOG.info("checkActionHasResubmit : action [{}]", action.getAction());
            if (action.getAction() instanceof NxActionResubmitNodesNodeTableFlowWriteActionsCase) {
                NxActionResubmitNodesNodeTableFlowWriteActionsCase resubmitAction =
                        (NxActionResubmitNodesNodeTableFlowWriteActionsCase) action.getAction();
                assertEquals(resubmitAction.getNxResubmit().getTable().shortValue(), nextTableId);
                resubmitActionFound = true;
            }
            // ApplyActionsCase aac = (ApplyActionsCase) curInstruction;
        }

        assertTrue(resubmitActionFound);
    }

    private void checkDrop(Instruction curInstruction, boolean mustExist) {
        if (curInstruction instanceof ApplyActionsCase) {
            ApplyActionsCase action = (ApplyActionsCase) curInstruction;
            DropActionCase drop = (DropActionCase) action.getApplyActions().getAction().get(0).getAction();
            assertTrue(drop.getDropAction() != null);
        } else {
            if (mustExist) {
                LOG.info("checkDrop expecting GoToTableCase, but received [{}]", curInstruction);
                fail();
            }
        }
    }

    private void checkMetadata(Instruction curInstruction, long pathId) {
        if (curInstruction instanceof WriteMetadataCase) {
            WriteMetadataCase metadata = (WriteMetadataCase) curInstruction;
            assertEquals(metadata.getWriteMetadata().getMetadata().longValue(), pathId);
        }
    }

    private void checkMatchNsh(Flow flow, long nsp, short nsi) {

        assertEquals(OpenflowConstants.ETHERTYPE_NSH,
                flow.getMatch().getEthernetMatch().getEthernetType().getType().getValue().longValue());

        GeneralAugMatchNodesNodeTableFlow genAug =
                flow.getMatch().augmentation(GeneralAugMatchNodesNodeTableFlow.class);

        List<ExtensionList> extensions = genAug.getExtensionList();
        for (ExtensionList extensionList : extensions) {
            Extension extension = extensionList.getExtension();
            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.augmentation(NxAugMatchNodesNodeTableFlow.class);

            if (nxAugMatch.getNxmNxNsp() != null) {
                assertEquals(nxAugMatch.getNxmNxNsp().getValue().longValue(), nsp);
            }
            if (nxAugMatch.getNxmNxNsi() != null) {
                assertEquals(nxAugMatch.getNxmNxNsi().getNsi().shortValue(), nsi);
            }
        }
    }
}

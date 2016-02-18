/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import com.google.common.net.InetAddresses;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNsiCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc1NodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitNodesNodeTableFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Ricardo Noriega (ricardo.noriega.de.soto@ericsson.com)
 * @since 2015-11-25
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcOvsUtil.class})
public class SfcL2FlowProgrammerTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2FlowProgrammerTest.class);

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

    private final SfcL2FlowWriterInterface sfcL2FlowWriter;
    private SfcL2FlowProgrammerOFimpl sfcL2FlowProgrammer;
    private FlowBuilder flowBuilder;

    public SfcL2FlowProgrammerTest() {
        // TODO use one Mockito or PowerMock
        this.flowBuilder = null;
        this.sfcL2FlowWriter = mock(SfcL2FlowWriterInterface.class);
        // Configure Mockito to store the FlowBuilder when writeFlowToConfig() is called
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                flowBuilder = (FlowBuilder) args[2];
                return null;
            }
        }).when(this.sfcL2FlowWriter).writeFlowToConfig(anyLong(), anyString(), (FlowBuilder) anyObject());

        // Configure Mockito to return the FlowBuilder stored by writeFlowToConfig()
        // when getFlowBuilder() is called
        // The following didnt work, since it used the value of flowBuilder at the time of invocation:
        //    when(sfcL2FlowWriter.getFlowBuilder).thenReturn(flowBuilder)
        doAnswer(new Answer<FlowBuilder>() {
            public FlowBuilder answer(InvocationOnMock invocation) {
                return flowBuilder;
            }
        }).when(this.sfcL2FlowWriter).getFlowBuilder();

        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammerOFimpl(sfcL2FlowWriter);
    }

    @Test
    public void compareClassificationCookieTest() {
        boolean result = sfcL2FlowProgrammer.compareClassificationTableCookie(null);
        assertFalse(result);
        FlowCookie cookie = new FlowCookie(new BigInteger("1"));
        result = sfcL2FlowProgrammer.compareClassificationTableCookie(cookie);
        assertFalse(result);
        cookie = new FlowCookie(SfcL2FlowProgrammerOFimpl.TRANSPORT_EGRESS_COOKIE);
        result = sfcL2FlowProgrammer.compareClassificationTableCookie(cookie);
        assertTrue(result);
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureIpv4TransportIngressFlow(String)}
     */
    @Test
    public void configureIpv4TransportIngressFlow() {

        sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);
        //TODO Two test cases for TCP/UDP shall be done
        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(), SfcOpenflowUtils.ETHERTYPE_IPV4);
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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertTrue(match.getVlanMatch().getVlanId().isVlanIdPresent());
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
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeTransportIngressFlow(String, long, short)}
     */
    @Test
    public void configureVxlanGpeTransportIngressFlow() {

        sfcL2FlowProgrammer.configureVxlanGpeTransportIngressFlow(SFF_NAME, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);

        checkNspNsiMatch(flowBuilder.build());

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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(), SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_ARP_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(), SfcOpenflowUtils.ETHERTYPE_ARP);
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
                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
                assertEquals(regLoad.getNxRegLoad().getValue().intValue(), SfcOpenflowUtils.ARP_REPLY);
                assertTrue(arpSha.isNxArpSha());
                assertTrue(arpTha.isNxArpTha());
                assertEquals(regCase.getNxReg().getCanonicalName(), "org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0");
                assertTrue(arpTpa.isOfArpTpa());
                assertTrue(arpSpa.isOfArpSpa());
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), INPORT);

                LOG.info("configureArpTransportIngressFlow() Action ArpOp: [{}], SrcMac: [{}], Output port: [{}]",
                        regLoad.getNxRegLoad().getValue().intValue(),
                        setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetType().getType().getValue().longValue(), SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
        assertEquals(match.getProtocolMatchFields().getMplsLabel().longValue(), MPLS_LABEL);
        LOG.info("configureMplsPathMapperFlow() Match EtherType: [{}], Mpls Label: [{}]",
                match.getEthernetMatch().getEthernetType().getType().getValue(),
                match.getProtocolMatchFields().getMplsLabel());
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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_PATH_MAPPER);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getVlanMatch().getVlanId().getVlanId().getValue().intValue(), VLAN_ID);
        LOG.info("configureVlanPathMapperFlow() Match VlanId: [{}]",
                match.getVlanMatch().getVlanId().getVlanId().getValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            checkMetadata(curInstruction, PATH_ID);
            LOG.info("configureVlanPathMapperFlow() Action Metadata PathId: [{}]", PATH_ID);

            if (curInstruction instanceof ApplyActionsCase) {

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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
        assertEquals(match.getMetadata().getMetadata().longValue(), SFP);
        LOG.info("configureNextHopFlow() Match SrcMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetSource().getAddress().getValue(),
                match.getMetadata().getMetadata().longValue());


        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                assertEquals(setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);
                LOG.info("configureNextHopFlow() Action Set DstMac: [{}]",
                        setField.getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue());
            }

            checkGoToTable(curInstruction, SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
            LOG.info("configureNextHopFlow() Action NextTableId: [{}]",
                    SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        }

        // Src mac == null
        sfcL2FlowProgrammer.configureMacNextHopFlow(SFF_NAME, SFP, null, MAC_DST);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP - 10);
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureGroupNextHopFlow(String, long, String, long, String)}
     */
    @Test
    public void configureGroupNextHopFlow() {

        sfcL2FlowProgrammer.configureGroupNextHopFlow(SFF_NAME, SFP, MAC_SRC, GROUP_ID, GROUP_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
        assertEquals(match.getMetadata().getMetadata().longValue(), SFP);
        LOG.info("configureGroupNextHopFlow() Match SrcMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetSource().getAddress().getValue(),
                match.getMetadata().getMetadata().longValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                GroupActionCase group = (GroupActionCase) action.getApplyActions().getAction().get(0).getAction();

                assertEquals(group.getGroupAction().getGroup(), GROUP_NAME);
                assertEquals(group.getGroupAction().getGroupId().longValue(), GROUP_ID);
                LOG.info("configureGroupNextHopFlow() Action GroupName: [{}], GroupId: [{}]",
                        group.getGroupAction().getGroup(),
                        group.getGroupAction().getGroupId());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureNshNscTransportEgressFlow(String, long, short, String)}
     */
    @Test
    public void configureNshNscTransportEgressFlowTest() {

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort")).toReturn(0L);

        sfcL2FlowProgrammer.configureNshNscTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS + 1);
        checkNspNsiMatch(flowBuilder.build());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase actionsCase = (ApplyActionsCase) curInstruction;
                List<Action> actions = actionsCase.getApplyActions().getAction();
                assertTrue(actions.size() == 1);
                Action action = actions.get(0);
                assertTrue(action.getAction() instanceof OutputActionCase);
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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_NEXT_HOP);
        checkNspNsiMatch(flowBuilder.build());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegLoadNodesNodeTableFlowApplyActionsCase nxLoad =
                        (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0).getAction();
                int ip = InetAddresses.coerceToInteger(InetAddresses.forString(IP_DST));
                long ipl = ip & 0xffffffffL;

                assertEquals(nxLoad.getNxRegLoad().getValue().longValue(), ipl);
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
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVlanSfTransportEgressFlow(String, String, String, int, String, long, boolean)}
     */
    @Test
    public void configureVlanSfTransportEgressFlow() {

        sfcL2FlowProgrammer.configureVlanSfTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID_SMALL, PKTIN);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS + 10);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);
        assertEquals(match.getMetadata().getMetadata().longValue(), PATH_ID_SMALL);
        LOG.info("configureMacTransportEgressFlow() Match DstMac: [{}], Sfp: [{}]",
                match.getEthernetMatch().getEthernetDestination().getAddress().getValue(),
                match.getMetadata().getMetadata().longValue());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                SetFieldCase dscp = (SetFieldCase) action.getApplyActions().getAction().get(0).getAction();
                PushVlanActionCase pushVlan = (PushVlanActionCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase vlanId = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(3).getAction();

                assertEquals(dscp.getSetField().getIpMatch().getIpDscp().getValue().shortValue(), PATH_ID_SMALL);
                assertEquals(pushVlan.getPushVlanAction().getEthernetType().intValue(), SfcOpenflowUtils.ETHERTYPE_VLAN);
                assertEquals(vlanId.getSetField().getVlanMatch().getVlanId().getVlanId().getValue().intValue(), VLAN_ID);
                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVlanTransportEgressFlow(String, String, String, int, String, long)}
     */
    @Test
    public void configureVlanTransportEgressFlow() {

        sfcL2FlowProgrammer.configureVlanTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS + 10);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);
        assertEquals(match.getMetadata().getMetadata().longValue(), PATH_ID);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PushVlanActionCase pushVlan = (PushVlanActionCase) action.getApplyActions().getAction().get(0).getAction();
                SetFieldCase vlanId = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();

                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
                assertEquals(pushVlan.getPushVlanAction().getEthernetType().intValue(), SfcOpenflowUtils.ETHERTYPE_VLAN);
                assertEquals(vlanId.getSetField().getVlanMatch().getVlanId().getVlanId().getValue().intValue(), VLAN_ID);
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeTransportEgressFlow(String, long, short)}
     */
    @Test
    public void configureVxlanGpeTransportEgressFlow() {

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort")).toReturn(0L);

        sfcL2FlowProgrammer.configureVxlanGpeTransportEgressFlow(SFF_NAME, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS + 1);
        checkNspNsiMatch(flowBuilder.build());

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

                assertTrue(nshC1dst.isNxNshc1Dst());
                assertTrue(nshC2dst.isNxNshc2Dst());
                assertTrue(tunIdDst.isNxTunId());
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), INPORT);
                LOG.info("configureVxlanGpeTransportEgressFlow() Action OutputPort: [{}]",
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeLastHopTransportEgressFlow(String, long, short, String)}
     */
    @Test
    public void configureVxlanGpeLastHopTransportEgressFlow() {

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort")).toReturn(0L);

        sfcL2FlowProgrammer.configureVxlanGpeLastHopTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS + 1);
        checkNspNsiMatch(flowBuilder.build());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();
            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove1 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(0).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove2 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(1).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove3 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(2).getAction();
                NxActionRegMoveNodesNodeTableFlowApplyActionsCase regMove4 = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getApplyActions().getAction().get(3).getAction();
                OutputActionCase output = (OutputActionCase) action.getApplyActions().getAction().get(4).getAction();

                DstNxNsiCase nshNsi = (DstNxNsiCase) regMove1.getNxRegMove().getDst().getDstChoice();
                DstNxNspCase nshNsp = (DstNxNspCase) regMove2.getNxRegMove().getDst().getDstChoice();
                DstNxTunIpv4DstCase tunIpv4Dst = (DstNxTunIpv4DstCase) regMove3.getNxRegMove().getDst().getDstChoice();
                DstNxTunIdCase tunIdDst = (DstNxTunIdCase) regMove4.getNxRegMove().getDst().getDstChoice();

                assertTrue(nshNsi.isNxNsiDst());
                assertTrue(nshNsp.isNxNspDst());
                assertTrue(tunIpv4Dst.isNxTunIpv4Dst());
                assertTrue(tunIdDst.isNxTunId());
                assertEquals(output.getOutputAction().getOutputNodeConnector().getValue(), PORT);
                LOG.info("configureVxlanGpeTransportEgressFlow() Action OutputPort: [{}]",
                        output.getOutputAction().getOutputNodeConnector().getValue());
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureMplsTransportEgressFlow(String, String, String, long, String, long)}
     */
    @Test
    public void configureMplsTransportEgressFlow() {

        sfcL2FlowProgrammer.configureMplsTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, MPLS_LABEL, PORT, PATH_ID);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_TRANSPORT_EGRESS + 10);

        Match match = flowBuilder.getMatch();
        assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress().getValue(), MAC_DST);
        assertEquals(match.getMetadata().getMetadata().longValue(), PATH_ID);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {

                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                PushMplsActionCase pushMpls = (PushMplsActionCase) action.getApplyActions().getAction().get(0).getAction();
                SetFieldCase mplsLabel = (SetFieldCase) action.getApplyActions().getAction().get(1).getAction();
                SetFieldCase setField = (SetFieldCase) action.getApplyActions().getAction().get(2).getAction();

                assertEquals(setField.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(), MAC_SRC);
                assertEquals(pushMpls.getPushMplsAction().getEthernetType().intValue(), SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST);
                assertEquals(mplsLabel.getSetField().getProtocolMatchFields().getMplsLabel().longValue(), MPLS_LABEL);
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureClassifierTableMatchAny(String)}
     */
    @Test
    public void configureClassifierTableMatchAnyTest() {

        sfcL2FlowProgrammer.configureClassifierTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_CLASSIFIER);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase action = (ApplyActionsCase) curInstruction;
                DropActionCase drop = (DropActionCase) action.getApplyActions().getAction().get(0).getAction();
                assertTrue(drop.getDropAction() != null);
                LOG.info("configureClassifierTableMatchAny() Action is: [{}]",
                        drop.getDropAction().toString());
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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER_ACL);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

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

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_MATCH_ANY);

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

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeSfLoopbackEncapsulatedEgressFlow(String, String, short, long)}
     */
    @Test
    public void configureVxlanGpeSfLoopbackEncapsulatedEgressFlowTest() {
        sfcL2FlowProgrammer.configureVxlanGpeSfLoopbackEncapsulatedEgressFlow(SFF_NAME, IP_SRC, Short.valueOf(PORT), 2L);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_ARP_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertNotNull(match.getEthernetMatch());
        assertNotNull(match.getIpMatch());
        assertNotNull(match.getLayer3Match());
        assertNotNull(match.getLayer4Match());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase actionsCase = (ApplyActionsCase) curInstruction;
                List<Action> actions = actionsCase.getApplyActions().getAction();
                assertTrue(actions.size() == 1);
                Action action = actions.get(0);
                assertTrue(action.getAction() instanceof OutputActionCase);
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeSfReturnLoopbackIngressFlow(String, short, long)} (String, String, short, long)}
     */
    @Test
    public void configureVxlanGpeSfReturnLoopbackIngressFlowTest() {
        sfcL2FlowProgrammer.configureVxlanGpeSfReturnLoopbackIngressFlow(SFF_NAME, Short.valueOf(PORT), 2L);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        assertEquals(flowBuilder.getTableId().shortValue(), SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_INGRESS);
        assertEquals(flowBuilder.getPriority().intValue(), SfcL2FlowProgrammerOFimpl.FLOW_PRIORITY_ARP_TRANSPORT_INGRESS);

        Match match = flowBuilder.getMatch();
        assertNotNull(match.getEthernetMatch());
        assertNotNull(match.getInPort());
        assertNotNull(match.getIpMatch());
        assertNotNull(match.getLayer4Match());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase actionsCase = (ApplyActionsCase) curInstruction;
                List<Action> actions = actionsCase.getApplyActions().getAction();
                assertTrue(actions.size() == 1);
                Action action = actions.get(0);
                assertTrue(action.getAction() instanceof OutputActionCase);
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeAppCoexistTransportEgressFlow(String, long, short)}
     */
    @Test
    public void configureVxlanGpeAppCoexistTransportEgressFlowTest() {

        // Coexistence not set
        sfcL2FlowProgrammer.configureVxlanGpeAppCoexistTransportEgressFlow(SFF_NAME, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertNull(flowBuilder);

        // Coexistence set
        sfcL2FlowProgrammer.setTableEgress(SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort")).toReturn(0L);

        sfcL2FlowProgrammer.configureVxlanGpeAppCoexistTransportEgressFlow(SFF_NAME, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        checkNspNsiMatch(flowBuilder.build());

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase actionsCase = (ApplyActionsCase) curInstruction;
                List<Action> actions = actionsCase.getApplyActions().getAction();
                assertTrue(actions.size() == 4);
                assertTrue(actions.get(0).getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(1).getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(2).getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(3).getAction() instanceof OutputActionCase);
            }
        }
    }

    /**
     * Unit test to check match and action fields from flows generated by:
     * {@link org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl#configureVxlanGpeLastHopAppCoexistTransportEgressFlow(String, long, short, String)}
     */
    @Test
    public void configureVxlanGpeLastHopAppCoexistTransportEgressFlowTest() {

        // Coexistence not set
        sfcL2FlowProgrammer.configureVxlanGpeLastHopAppCoexistTransportEgressFlow(SFF_NAME, NSP, NSI, IP_SRC);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertNull(flowBuilder);

        // Coexistence set
        sfcL2FlowProgrammer.setTableEgress(SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS);

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort")).toReturn(0L);

        sfcL2FlowProgrammer.configureVxlanGpeLastHopAppCoexistTransportEgressFlow(SFF_NAME, NSP, NSI, IP_SRC);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();

        checkNspNsiMatch(flowBuilder.build());
        checkNshc1Match(flowBuilder.build(), InetAddresses.coerceToInteger(InetAddresses.forString(IP_SRC)) & 0xffffffffL);

        Instructions isb = flowBuilder.getInstructions();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction instruction : isb.getInstruction()) {

            Instruction curInstruction = instruction.getInstruction();

            if (curInstruction instanceof ApplyActionsCase) {
                ApplyActionsCase actionsCase = (ApplyActionsCase) curInstruction;
                List<Action> actions = actionsCase.getApplyActions().getAction();
                assertTrue(actions.size() == 6);
                assertTrue(actions.get(0).getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(1).getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(2).getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(3).getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(4).getAction() instanceof NxActionSetNshc1NodesNodeTableFlowApplyActionsCase);
                assertTrue(actions.get(5).getAction() instanceof NxActionResubmitNodesNodeTableFlowWriteActionsCase);

            }
        }
    }

    /**
     * Unit test to check app coexistence works for NSH flows
     */
    @Test
    public void appCoexistenceNsh() {

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort")).toReturn(0L);

        sfcL2FlowProgrammer.setTableBase(TABLE_BASE);
        sfcL2FlowProgrammer.setTableEgress(TABLE_EGRESS);

        // When checking the table offsets, we need to subtract 2 to compensate for:
        // - TABLE_INDEX_CLASSIFIER=0 - which is not used for AppCoexistence
        // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for AppCoexistence
        // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should return 20

        // Check that configureVxlanGpeTransportIngressFlow() is written to the correct table
        // Notice: TransportIngress doesnt use the offset, as it will always be table 0
        sfcL2FlowProgrammer.configureVxlanGpeTransportIngressFlow(SFF_NAME, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);

        // Check that configureVxlanGpeNextHopFlow() is written to the correct table
        sfcL2FlowProgrammer.configureVxlanGpeNextHopFlow(SFF_NAME, IP_DST, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(1).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2), true);

        // Check that configureVxlanGpeLastHopTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcL2FlowProgrammer.configureVxlanGpeLastHopTransportEgressFlow(SFF_NAME, NSP, NSI, PORT);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVxlanGpeTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcL2FlowProgrammer.configureVxlanGpeTransportEgressFlow(SFF_NAME, NSP, NSI);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVxlanGpeAppCoexistTransportEgressFlow() is written
        // to the correct table and that it goes to TABLE_EGRESS
        sfcL2FlowProgrammer.configureVxlanGpeLastHopAppCoexistTransportEgressFlow(SFF_NAME, NSP, NSI, IP_SRC);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);
        checkActionHasResubmit(curInstruction, TABLE_EGRESS);
    }

    /**
     * Unit test to check app coexistence works for VLAN flows
     */
    @Test
    public void appCoexistenceVlan() {
        sfcL2FlowProgrammer.setTableBase(TABLE_BASE);
        sfcL2FlowProgrammer.setTableEgress(TABLE_EGRESS);

        // When checking the table offsets, we need to subtract 2 to compensate for:
        // - TABLE_INDEX_CLASSIFIER=0 - which is not used for AppCoexistence
        // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for AppCoexistence
        // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should return 20

        // Check that configureVlanTransportIngressFlow() is written to the correct table
        // Notice: TransportIngress doesnt use the offset, as it will always be table 0
        sfcL2FlowProgrammer.configureVlanTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);

        // Check that configureVlanPathMapperFlow() is written to the correct table
        sfcL2FlowProgrammer.configureVlanPathMapperFlow(SFF_NAME, VLAN_ID, PATH_ID_SMALL, true);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(2).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);

        // Check that configureVlanPathMapperFlow() is written to the correct table
        sfcL2FlowProgrammer.configureVlanPathMapperFlow(SFF_NAME, VLAN_ID, PATH_ID, false);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(2).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);

        // Check that configureMacNextHopFlow() is written to the correct table
        sfcL2FlowProgrammer.configureMacNextHopFlow(SFF_NAME, SFP, MAC_SRC, MAC_DST);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(1).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2), true);

        // Check that configureVlanSfTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcL2FlowProgrammer.configureVlanSfTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID_SMALL, true);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVlanSfTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcL2FlowProgrammer.configureVlanSfTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID_SMALL, false);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVlanTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcL2FlowProgrammer.configureVlanTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureVlanLastHopTransportEgressFlow() is written
        // to the correct table and that it goes to TABLE_EGRESS
        sfcL2FlowProgrammer.configureVlanLastHopTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, VLAN_ID, PORT, PATH_ID);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);
        //checkGoToTable(curInstruction, TABLE_EGRESS, true);
    }

    /**
     * Unit test to check app coexistence works for MPLS flows
     */
    @Test
    public void appCoexistenceMpls() {
        sfcL2FlowProgrammer.setTableBase(TABLE_BASE);
        sfcL2FlowProgrammer.setTableEgress(TABLE_EGRESS);

        // Check that configureMplsTransportIngressFlow() is written to the correct table
        // Notice: TransportIngress doesnt use the offset, as it will always be table 0
        sfcL2FlowProgrammer.configureMplsTransportIngressFlow(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(), 0);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2), true);

        // Check that configureMplsPathMapperFlow() is written to the correct table
        sfcL2FlowProgrammer.configureMplsPathMapperFlow(SFF_NAME, MPLS_LABEL, PATH_ID_SMALL, true);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);

        // Check that configureMplsPathMapperFlow() is written to the correct table
        sfcL2FlowProgrammer.configureMplsPathMapperFlow(SFF_NAME, MPLS_LABEL, PATH_ID, false);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);

        // Mpls NextHop uses MacNextHop which is tested in appCoexistenceVlan

        // Check that configureMplsTransportEgressFlow() is written
        // to the correct table and that it does NOT go to TABLE_EGRESS
        sfcL2FlowProgrammer.configureMplsTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, MPLS_LABEL, PORT, PATH_ID);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);

        // Check that configureMplsLastHopTransportEgressFlow() is written
        // to the correct table and that it goes to TABLE_EGRESS
        sfcL2FlowProgrammer.configureMplsLastHopTransportEgressFlow(SFF_NAME, MAC_SRC, MAC_DST, MPLS_LABEL, PORT, PATH_ID);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        assertTrue(curInstruction instanceof ApplyActionsCase);
        //checkGoToTable(curInstruction, TABLE_EGRESS, true);
    }

    /**
     * Unit test to check app coexistence works for the MatchAny flows
     */
    @Test
    public void appCoexistenceMatchAny() {
        sfcL2FlowProgrammer.setTableBase(TABLE_BASE);
        sfcL2FlowProgrammer.setTableEgress(TABLE_EGRESS);

        // Test Classifier Match Any table offset.
        // No flows should be written
        sfcL2FlowProgrammer.configureClassifierTableMatchAny(SFF_NAME);
        assertEquals(sfcL2FlowWriter.getFlowBuilder(), null);

        // Test TransportIngress Match Any table offset.
        // No flows should be written
        sfcL2FlowProgrammer.configureTransportIngressTableMatchAny(SFF_NAME);
        assertEquals(sfcL2FlowWriter.getFlowBuilder(), null);

        // Test PathMapper Match Any table offset.
        // It should go to PathMapperAcl + offset
        sfcL2FlowProgrammer.configurePathMapperTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER_ACL + TABLE_BASE - 2), true);

        // Test PathMapperAcl Match Any table offset.
        // It should go to NextHop + offset
        sfcL2FlowProgrammer.configurePathMapperAclTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER_ACL + TABLE_BASE - 2);
        assertEquals(flowBuilder.getInstructions().getInstruction().size(), 1);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);

        // Test NextHop Match Any table offset.
        // It should go to TransportEgress + offset
        sfcL2FlowProgrammer.configureNextHopTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2), true);

        // Test TransportEgress Match Any table offset.
        // It should do a drop
        sfcL2FlowProgrammer.configureTransportEgressTableMatchAny(SFF_NAME);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_TRANSPORT_EGRESS + TABLE_BASE - 2);
        curInstruction = flowBuilder.getInstructions().getInstruction().get(0).getInstruction();
        checkDrop(curInstruction, true);
    }

    /**
     * Unit test to check app coexistence works for the PathMapperAcl flows
     */
    @Test
    public void appCoexistencePathMapperAcl() {
        sfcL2FlowProgrammer.setTableBase(TABLE_BASE);
        sfcL2FlowProgrammer.setTableEgress(TABLE_EGRESS);
        sfcL2FlowProgrammer.configurePathMapperAclFlow(SFF_NAME, IP_SRC, IP_DST, (short) PATH_ID);
        flowBuilder = sfcL2FlowWriter.getFlowBuilder();
        assertEquals(flowBuilder.getTableId().shortValue(),
                SfcL2FlowProgrammerOFimpl.TABLE_INDEX_PATH_MAPPER_ACL + TABLE_BASE - 2);
        Instruction curInstruction = flowBuilder.getInstructions().getInstruction().get(1).getInstruction();
        checkGoToTable(curInstruction, (short) (SfcL2FlowProgrammerOFimpl.TABLE_INDEX_NEXT_HOP + TABLE_BASE - 2), true);
    }

    public void checkGoToTable(Instruction curInstruction, short nextTableId) {
        checkGoToTable(curInstruction, nextTableId, false);
    }

    public void checkGoToTable(Instruction curInstruction, short nextTableId, boolean mustExist) {

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

    public void checkActionHasResubmit(Instruction curInstruction, short nextTableId) {
        assertTrue(curInstruction instanceof ApplyActionsCase);
        boolean resubmitActionFound = false;
        for (Action action : ((ApplyActionsCase) curInstruction).getApplyActions().getAction()) {
            LOG.info("checkActionHasResubmit : action [{}]", action.getAction());
            if (action.getAction() instanceof NxActionResubmitNodesNodeTableFlowWriteActionsCase) {
                NxActionResubmitNodesNodeTableFlowWriteActionsCase a =
                        (NxActionResubmitNodesNodeTableFlowWriteActionsCase) action.getAction();
                assertEquals(a.getNxResubmit().getTable().shortValue(), nextTableId);
                resubmitActionFound = true;
            }
            //ApplyActionsCase aac = (ApplyActionsCase) curInstruction;
        }

        assertTrue(resubmitActionFound);
    }

    public void checkDrop(Instruction curInstruction) {
        checkDrop(curInstruction, false);
    }

    public void checkDrop(Instruction curInstruction, boolean mustExist) {

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

    public void checkMetadata(Instruction curInstruction, long pathId) {

        if (curInstruction instanceof WriteMetadataCase) {
            WriteMetadataCase metadata = (WriteMetadataCase) curInstruction;
            assertEquals(metadata.getWriteMetadata().getMetadata().longValue(), pathId);
        }
    }

    public void checkNspNsiMatch(Flow flow) {

        boolean checkNsp = true;
        boolean checkNsi = true;

        GeneralAugMatchNodesNodeTableFlow genAug = flow.getMatch().getAugmentation(
                GeneralAugMatchNodesNodeTableFlow.class);

        List<ExtensionList> extensions = genAug.getExtensionList();
        for (ExtensionList extensionList : extensions) {
            Extension extension = extensionList.getExtension();
            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class);

            if (checkNsp) {
                assertEquals(nxAugMatch.getNxmNxNsp().getValue().longValue(), NSP);
                checkNsp = false;
                continue;
            }
            if (checkNsi) {
                assertEquals(nxAugMatch.getNxmNxNsi().getNsi().shortValue(), NSI);
                checkNsi = false;
            }
        }

    }

    public void checkNshc1Match(Flow flow, long nshc1) {

        GeneralAugMatchNodesNodeTableFlow genAug = flow.getMatch().getAugmentation(
                GeneralAugMatchNodesNodeTableFlow.class);

        List<ExtensionList> extensions = genAug.getExtensionList();
        for (ExtensionList extensionList : extensions) {
            Extension extension = extensionList.getExtension();
            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class);
            if (nxAugMatch.getNxmNxNshc1() != null) {
                assertEquals(nxAugMatch.getNxmNxNshc1().getValue(), Long.valueOf(nshc1));
            }
        }

    }
}


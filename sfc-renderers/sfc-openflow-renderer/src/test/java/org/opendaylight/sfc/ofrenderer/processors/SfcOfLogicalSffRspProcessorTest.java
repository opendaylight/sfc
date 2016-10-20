/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.ofrenderer.RspBuilder;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowWriterImpl;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfProviderUtilsTestMock;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionPopNshNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitNodesNodeTableFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.pop.nsh.grouping.NxPopNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsi.grouping.NxmNxNsi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsp.grouping.NxmNxNsp;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.math.BigInteger;

import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TABLE_INDEX_CLASSIFIER;
import org.opendaylight.genius.mdsalutil.NwConstants;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

/**
 * Component tests to test the Logical Sff feature
 *
 * @author Miguel Duarte (miguel.duarte.de.mora.barroso@ericsson.com)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcGeniusRpcClient.class, SfcGeniusDataUtils.class})
public class SfcOfLogicalSffRspProcessorTest {
    @InjectMocks
    private SfcGeniusRpcClient geniusClient;

    @Mock
    private OdlInterfaceRpcService interfaceManagerRpcService;

    @Mock
    private ItmRpcService itmRpcService;

    @Spy
    private SfcOfFlowProgrammerImpl flowProgrammer;

    @Spy
    private SfcOfFlowWriterImpl ofFlowWriter;

    private SfcOfRspProcessor sfcOfRspProcessor;
    private SfcOfProviderUtilsTestMock sfcUtils;
    private RspBuilder rspBuilder;
    private List<SftTypeName> sfTypes;

    private static final String theLogicalIfName = "tap40c552e0-36";
    private static final MacAddress theMacAddress = new MacAddress("11:22:33:44:55:66");

    public SfcOfLogicalSffRspProcessorTest() {
        initMocks(this);
        flowProgrammer.setFlowWriter(ofFlowWriter);
        sfcUtils = new SfcOfProviderUtilsTestMock();
        sfcOfRspProcessor = PowerMockito.spy(new SfcOfRspProcessor(
                flowProgrammer,
                sfcUtils,
                new SfcSynchronizer(),null));
        rspBuilder = new RspBuilder(sfcUtils);
        sfTypes = new ArrayList<SftTypeName>() {{
            add(new SftTypeName("firewall"));
            add(new SftTypeName("http-header-enrichment"));
        }};
        Mockito.doNothing().when(ofFlowWriter).flushFlows();
        Mockito.doNothing().when(ofFlowWriter).deleteFlowSet();
        Mockito.doNothing().when(ofFlowWriter).purgeFlows();
    }

    @Before
    public void setUp() {
        sfcUtils.resetCache();

        PowerMockito.spy(SfcGeniusRpcClient.class);
        PowerMockito.when(SfcGeniusRpcClient.getInstance()).thenReturn(geniusClient);

        PowerMockito.mockStatic(SfcGeniusDataUtils.class);
        PowerMockito.when(SfcGeniusDataUtils.getServiceFunctionMacAddress(anyString()))
                .thenReturn(Optional.of(theMacAddress));

        PowerMockito.when(SfcGeniusDataUtils.getSfLogicalInterface(any(ServiceFunction.class)))
                .thenReturn(theLogicalIfName);

        PowerMockito.when(SfcGeniusDataUtils.isSfUsingALogicalInterface(any(ServiceFunction.class)))
                .thenReturn(true);

        when(interfaceManagerRpcService.getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class)))
                .thenReturn(
                         Futures.immediateFuture(
                                RpcResultBuilder.success(
                                        new GetEgressActionsForInterfaceOutputBuilder()
                                                .setAction(new ArrayList<>()))
                                        .build()));

        when(itmRpcService.getTunnelInterfaceName(any(GetTunnelInterfaceNameInput.class)))
                .thenReturn(Futures.immediateFuture(
                        RpcResultBuilder.success(
                                new GetTunnelInterfaceNameOutputBuilder()
                                    .setInterfaceName(theLogicalIfName))
                                    .build()));

        when(interfaceManagerRpcService.getDpidFromInterface(any(GetDpidFromInterfaceInput.class)))
                .thenReturn(Futures.immediateFuture(
                        RpcResultBuilder.success(
                                new GetDpidFromInterfaceOutputBuilder()
                                    .setDpid(new BigInteger("1234567890")))
                                    .build()));
    }

    @Test
    public void testEthNshFlowCreation() throws Exception {
        when(interfaceManagerRpcService.getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class)))
                .thenReturn(
                        Futures.immediateFuture(
                                RpcResultBuilder.success(
                                        new GetEgressActionsForInterfaceOutputBuilder()
                                                .setAction(new ArrayList<Action>() {{
                                                    add( new ActionBuilder().build());
                                                }}))
                                        .build()));

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, true);
        sfcOfRspProcessor.processRenderedServicePath(vlanRsp);

        int nHops = sfTypes.size() + 1;

        verify(interfaceManagerRpcService, times(nHops))
                .getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class));

        // 2 SFFs, meaning 1 hop between SFFs, must get the logical interface between them just once
        verify(itmRpcService).getTunnelInterfaceName(any(GetTunnelInterfaceNameInput.class));

        // 2 SFs, must get their respective DpnId twice in total
        verify(interfaceManagerRpcService, times(sfTypes.size()))
                .getDpidFromInterface(any(GetDpidFromInterfaceInput.class));

        // fetch the set of added flows from the openflow writer
        Set<SfcOfFlowWriterImpl.FlowDetails> addedFlows =
                Whitebox.getInternalState(ofFlowWriter, "setOfFlowsToAdd");

        // Make sure we have the right amount of flows in each relevant table
        Assert.assertEquals(1, addedFlows.stream().filter(
                flow -> flow.tableKey.getId().equals(TABLE_INDEX_CLASSIFIER)).count());
        Assert.assertEquals(nHops, addedFlows.stream().filter(
                flow -> flow.tableKey.getId().equals(NwConstants.SFC_TRANSPORT_INGRESS_TABLE)).count());
        Assert.assertEquals(1, addedFlows.stream().filter(
                flow -> flow.tableKey.getId().equals(NwConstants.SFC_TRANSPORT_PATH_MAPPER_TABLE)).count());
        Assert.assertEquals(1, addedFlows.stream().filter(
                flow -> flow.tableKey.getId().equals(NwConstants.SFC_TRANSPORT_PATH_MAPPER_ACL_TABLE)).count());
        Assert.assertEquals(4, addedFlows.stream().filter(
                flow -> flow.tableKey.getId().equals(NwConstants.SFC_TRANSPORT_NEXT_HOP_TABLE)).count());

        Assert.assertEquals(2 * nHops, addedFlows.stream().map(flowDetail -> flowDetail.flow).filter(
                flow -> flow.getFlowName().equals("MatchAny")).count());
        Assert.assertEquals(sfTypes.size(), addedFlows.stream().map(flowDetail -> flowDetail.flow).filter(
                flow -> flow.getFlowName().equals("ingress_Transport_Flow")).count());
        Assert.assertEquals(nHops, addedFlows.stream().map(flowDetail -> flowDetail.flow).filter(
                flow -> flow.getFlowName().equals("nextHop")).count());
        Assert.assertEquals(nHops, addedFlows.stream().map(flowDetail -> flowDetail.flow).filter(
                flow -> flow.getFlowName().equals("default egress flow")).count());

        // we'll save in this set all the flows that are checked,
        // so that we can assure that all flows were accounted for
        Set<Flow> checkedFlows = new HashSet<>();

        // nextHop
        Assert.assertTrue(addedFlows.stream()
                .map(flowDetail -> flowDetail.flow)
                .filter(flow -> flow.getFlowName().equals("nextHop"))
                .peek(checkedFlows::add)
                .allMatch(nextHopFlow -> matchNextHop(nextHopFlow, vlanRsp.getPathId())));

        // transport ingress
        Assert.assertTrue(addedFlows.stream()
                .map(flowDetail -> flowDetail.flow)
                .filter(flow -> flow.getFlowName().equals("ingress_Transport_Flow"))
                .peek(checkedFlows::add)
                .allMatch(flow -> matchTransportIngress(flow, vlanRsp.getPathId())));

        // transport egress between SFFs
        Assert.assertTrue(addedFlows.stream()
                .map(flowDetail -> flowDetail.flow)
                .filter(flow -> flow.getFlowName().equals("default egress flow"))
                .peek(checkedFlows::add)
                .allMatch(transportEgressFlow -> matchTransportEgress(transportEgressFlow, false, vlanRsp.getPathId())));

        // transport egress last hop
        Assert.assertTrue(addedFlows.stream()
                .map(flowDetail -> flowDetail.flow)
                .filter(flow -> flow.getFlowName().equals("last hop egress flow"))
                .peek(checkedFlows::add)
                .allMatch(transportEgressFlow -> matchTransportEgress(transportEgressFlow, true, vlanRsp.getPathId())));

        // assure that the only flows we didn't check are the MatchAny flows
        Assert.assertEquals(
                addedFlows.size() - checkedFlows.size(),
                addedFlows.stream().filter(flowd -> !checkedFlows.contains(flowd.flow))
                        .filter(flowd -> flowd.flow.getFlowName().equals("MatchAny")).count());
    }

    private boolean matchTransportEgress(Flow transportEgressFlow, boolean lastHop, long rspId) {
        // check matches - check NSP + NSI
        List<NxAugMatchNodesNodeTableFlow> theNciraExtensions = getNciraExtensions(transportEgressFlow);

        // check the NSH headers - only NSP for the last hop
        if (!checkNshMatches(theNciraExtensions, true, rspId)) {
            return false;
        }

        List<Action> actionList =
                getInstructionsFromFlow(transportEgressFlow).stream()
                        .map(inst -> filterInstructionType(inst, ApplyActionsCase.class))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst()
                        .map(ApplyActionsCase::getApplyActions)
                        .map(ApplyActions::getAction)
                        .orElse(Collections.emptyList());

        // SFF-SFF
        if (!lastHop) {
            // 1 empty action (just like the genius RPC mock is defined)
            if (actionList.size() != 1 || Optional.ofNullable(actionList.get(0).getAction()).isPresent()) {
                return false;
            }
        }
        // LAST HOP
        else {
            Optional<NxPopNsh> popNsh = actionList.stream()
                    .map(action -> filterActionType(action, NxActionPopNshNodesNodeTableFlowApplyActionsCase.class))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .map(NxActionPopNshNodesNodeTableFlowApplyActionsCase::getNxPopNsh);

            // check actions - popNSH
            if(!popNsh.isPresent()) {
                return false;
            }
            Optional<Short> resubmit = actionList.stream()
                    .map(Action::getAction)
                    .filter(theAction -> theAction.getImplementedInterface().equals(NxActionResubmitNodesNodeTableFlowWriteActionsCase.class))
                    .map(theAction -> (NxActionResubmitNodesNodeTableFlowWriteActionsCase) theAction)
                    .findFirst()
                    .map(NxActionResubmitNodesNodeTableFlowWriteActionsCase::getNxResubmit)
                    .map(NxResubmit::getTable);

            // check actions - resubmit to dispatcher table
            if(!resubmit.filter(table_nr -> table_nr.equals(NwConstants.LPORT_DISPATCHER_TABLE)).isPresent()) {
                return false;
            }
        }

        return true;
    }

    private boolean matchTransportIngress(Flow transportIngressFlow, long rspId) {
        // check matches - check NSP
        List<NxAugMatchNodesNodeTableFlow> theNciraExtensions = getNciraExtensions(transportIngressFlow);
        // check the NSH headers
        if (!checkNshMatches(theNciraExtensions, false, rspId)) {
            return false;
        }

        // check actions - gotoTable 4
        Optional<Short> goToTableId = getGotoTableIdFromIntructions(transportIngressFlow);

        return goToTableId.filter(tableId -> tableId.equals(NwConstants.SFC_TRANSPORT_NEXT_HOP_TABLE)).isPresent();
    }

    private boolean matchNextHop(Flow nextHopFlow, long rspId) {
        // handle the Match
        List<NxAugMatchNodesNodeTableFlow> theNciraExtensions = getNciraExtensions(nextHopFlow);

        // check the NSH headers
        if (!checkNshMatches(theNciraExtensions, true, rspId)) {
            return false;
        }

        // handle the Actions part
        // assure 1 goto table instruction; goto table transport egress
        Optional<Short> goToTableId = getGotoTableIdFromIntructions(nextHopFlow);
        if (!goToTableId.filter(tableId -> tableId.equals(NwConstants.SFC_TRANSPORT_EGRESS_TABLE)).isPresent()) {
            return false;
        }

        // assure the destination mac address is the expected
        List<Action> actionList = getInstructionsFromFlow(nextHopFlow).stream()
                .map(inst -> filterInstructionType(inst, ApplyActionsCase.class))
                .filter(Optional::isPresent)
                .map(applyActionsInst -> applyActionsInst.get().getApplyActions())
                .map(ApplyActions::getAction)
                .findFirst()
                .orElse(Collections.emptyList());

        Optional<MacAddress> macAddress = actionList
                .stream()
                .map(action -> filterActionType(action, SetFieldCase.class))
                .filter(Optional::isPresent)
                .map(SetFieldCaseAction -> SetFieldCaseAction.get().getSetField())
                .map(SetField::getEthernetMatch)
                .map(EthernetMatch::getEthernetDestination)
                .map(EthernetDestination::getAddress)
                .findFirst();

        return macAddress.filter(theMac -> theMac.equals(theMacAddress)).isPresent();
    }

    private List<Instruction> getInstructionsFromFlow(Flow theFlow) {
        return Optional.ofNullable(theFlow.getInstructions())
                .map(Instructions::getInstruction)
                .orElse(Collections.emptyList());
    }

    private List<NxAugMatchNodesNodeTableFlow> getNciraExtensions(Flow theFlow) {
        if (theFlow.getMatch() == null)
            return Collections.emptyList();

        return Optional.ofNullable(theFlow.getMatch().getAugmentation(GeneralAugMatchNodesNodeTableFlow.class))
                        .map(GeneralAugMatchNodesNodeTableFlow::getExtensionList)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(ExtensionList::getExtension)
                        .map(extension -> extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class))
                        .collect(Collectors.toList());
    }

    private Optional<Short> getGotoTableIdFromIntructions(Flow theFlow) {
        return getInstructionsFromFlow(theFlow)
                .stream()
                .map(inst -> filterInstructionType(inst, GoToTableCase.class))
                .filter(Optional::isPresent)
                .map(gotoTableInst -> gotoTableInst.get().getGoToTable())
                .map(GoToTable::getTableId).findFirst();
    }

    private boolean checkNshMatches(List<NxAugMatchNodesNodeTableFlow> theNciraExtensions, boolean checkNsi, long rspId) {
        Optional<NxmNxNsp> theNsp = theNciraExtensions.stream().filter(node -> node.getNxmNxNsp() != null)
                .map(NxAugMatchNodesNodeTableFlow::getNxmNxNsp).findFirst();
        Optional<NxmNxNsi> theNsi = theNciraExtensions.stream().filter(node -> node.getNxmNxNsi() != null)
                .map(NxAugMatchNodesNodeTableFlow::getNxmNxNsi).findFirst();

        // must check if the NSP header matches the RSP ID
        if (!theNsp.map(NxmNxNsp::getValue)
                .filter(nsp -> nsp.equals(rspId)).isPresent())
            return false;

        // must check if the NSI is within range
        if (checkNsi) {
            if(!theNsi.map(NxmNxNsi::getNsi)
                    .filter(nsi -> nsi <= 255)
                    .filter(nsi -> nsi >= 255 - sfTypes.size()).isPresent()) {
                return false;
            }
        }
        return true;
    }

    private <T extends org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction>
    Optional<T> filterInstructionType(Instruction theInstruction, Class<T> instructionType) {
        return Optional.ofNullable(theInstruction.getInstruction())
                .filter(instruction -> instruction.getImplementedInterface().equals(instructionType))
                .map(instructionType::cast);
    }

    private <T extends org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action>
    Optional<T> filterActionType(Action theAction, Class<T> actionType) {
        return Optional.ofNullable(theAction.getAction())
                .filter(action -> action.getImplementedInterface().equals(actionType))
                .map(actionType::cast);
    }
}

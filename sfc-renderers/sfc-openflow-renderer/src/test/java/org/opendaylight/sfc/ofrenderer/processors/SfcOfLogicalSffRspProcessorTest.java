/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.COOKIE_BIGINT_HEX_RADIX;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TABLE_INDEX_CLASSIFIER;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TRANSPORT_EGRESS_COOKIE_STR_BASE;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_LOCAL_COOKIE;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_REMOTE_COOKIE;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TRANSPORT_EGRESS_NSH_ETH_LASTHOP_PIPELINE_COOKIE;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_LOCAL_COOKIE;
import static org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl.TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_REMOTE_COOKIE;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.ofrenderer.RspBuilder;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfProviderUtilsTestMock;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.sfc.ofrenderer.utils.operdsupdate.OperDsUpdateHandlerLSFFImpl;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterImpl;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.EthernetMatchFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.DstChoiceGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.SrcChoiceGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionPopNshNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitNodesNodeTableFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.pop.nsh.grouping.NxPopNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc1Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc2Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc1Grouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc2Grouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNsiGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNspGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._1.grouping.NxmNxNshc1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._2.grouping.NxmNxNshc2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsi.grouping.NxmNxNsi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsp.grouping.NxmNxNsp;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Component tests to test the Logical Sff feature.
 *
 * @author Miguel Duarte (miguel.duarte.de.mora.barroso@ericsson.com)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SfcGeniusRpcClient.class, SfcGeniusDataUtils.class, OperDsUpdateHandlerLSFFImpl.class,
        SfcOfRspProcessor.class, SfcRspProcessorLogicalSff.class, SfcOvsUtil.class})
public class SfcOfLogicalSffRspProcessorTest {

    private final SfcGeniusRpcClient geniusClient;

    @Mock
    private OdlInterfaceRpcService interfaceManagerRpcService;

    @Mock
    private ItmRpcService itmRpcService;

    @Spy
    private SfcOfFlowProgrammerImpl flowProgrammer;

    @Spy
    private SfcOfFlowWriterImpl ofFlowWriter;

    @Mock
    private DataBroker dataBroker;

    private final OperDsUpdateHandlerLSFFImpl operDsUpdateHandler;

    @Mock
    private RpcProviderRegistry rpcProviderRegistry;

    private final SfcOfRspProcessor sfcOfRspProcessor;
    private final SfcOfProviderUtilsTestMock sfcUtils;
    private final RspBuilder rspBuilder;
    private final List<SftTypeName> sfTypes;

    private final SfcRspTransportProcessorBase logicalSffProcessor;

    private static final String LOGICAL_INTERFACE_NAME = "tap40c552e0-36";
    private static final MacAddress[] MAC_ADDRESS_SF_SIDE = { new MacAddress("00:00:00:00:00:11"),
                                                              new MacAddress("00:00:00:00:00:22") };
    private static final MacAddress[] MAC_ADDRESS_OVS_SIDE = { new MacAddress("00:00:00:00:00:aa"),
                                                               new MacAddress("00:00:00:00:00:ff") };

    private static long VXLAN_GPE_OF_PORT = 10L;
    private static final IpAddress VTEP_IP = new IpAddress(new Ipv4Address("192.168.0.1"));
    private static final IpAddress LOCALHOST_IP = new IpAddress(new Ipv4Address("127.0.0.1"));

    /**
     * Test constructor.
     *
     * @throws Exception
     *             when something fails during private method suppression
     */
    public SfcOfLogicalSffRspProcessorTest() throws Exception {
        initMocks(this);

        operDsUpdateHandler = PowerMockito.spy(new OperDsUpdateHandlerLSFFImpl(dataBroker));
        flowProgrammer.setFlowWriter(ofFlowWriter);
        sfcUtils = new SfcOfProviderUtilsTestMock();
        geniusClient = PowerMockito.spy(new SfcGeniusRpcClient(rpcProviderRegistry));
        logicalSffProcessor = new SfcRspProcessorLogicalSff(geniusClient, operDsUpdateHandler);
        logicalSffProcessor.setSfcProviderUtils(sfcUtils);
        logicalSffProcessor.setFlowProgrammer(flowProgrammer);

        sfcOfRspProcessor = PowerMockito.spy(new SfcOfRspProcessor(flowProgrammer, sfcUtils, new SfcSynchronizer(),
                rpcProviderRegistry, dataBroker));
        PowerMockito.when(sfcOfRspProcessor, "getOperDsHandler").thenReturn(operDsUpdateHandler);

        rspBuilder = new RspBuilder(sfcUtils);
        sfTypes = new ArrayList<SftTypeName>() {
            {
                add(new SftTypeName("firewall"));
                add(new SftTypeName("http-header-enrichment"));
            }
        };
        Mockito.doNothing().when(ofFlowWriter).flushFlows();
        Mockito.doNothing().when(ofFlowWriter).deleteFlowSet();
        Mockito.doNothing().when(ofFlowWriter).purgeFlows();

        // Disable the execution of private methods interacting with the
        // datastore
        suppress(method(OperDsUpdateHandlerLSFFImpl.class, "updateRenderedServicePathOperationalStateWithDpnIds",
                SffGraph.class, RenderedServicePath.class, WriteTransaction.class));
        suppress(method(OperDsUpdateHandlerLSFFImpl.class, "updateSffStateWithDpnIds", SffGraph.class,
                RenderedServicePath.class, WriteTransaction.class));
        suppress(method(OperDsUpdateHandlerLSFFImpl.class, "deleteRspFromSffState", RenderedServicePath.class,
                WriteTransaction.class));
        suppress(method(OperDsUpdateHandlerLSFFImpl.class, "commitChangesAsync", WriteTransaction.class));
    }

    @Before
    public void setUp() throws Exception {
        sfcUtils.resetCache();

        PowerMockito.mockStatic(SfcGeniusDataUtils.class);
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(Mockito.mock(WriteTransaction.class));
        PowerMockito.when(sfcOfRspProcessor, "getGeniusRpcClient").thenReturn(geniusClient);
        PowerMockito.doReturn(logicalSffProcessor).when(sfcOfRspProcessor, "getReusableTransportProcessor", any(),
                any());
        PowerMockito.when(geniusClient, "getInterfaceManagerRpcService").thenReturn(interfaceManagerRpcService);
        PowerMockito.when(geniusClient, "getItmRpcService").thenReturn(itmRpcService);

        String ifName0 = rspBuilder.getLogicalInterfaceName(0);
        PowerMockito.mockStatic(SfcGeniusDataUtils.class);
        PowerMockito.when(SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName0))
                .thenReturn(Optional.of(MAC_ADDRESS_SF_SIDE[0]));
        PowerMockito.when(SfcGeniusDataUtils.getServiceFunctionForwarderPortMacAddress(ifName0))
                .thenReturn(Optional.of(MAC_ADDRESS_OVS_SIDE[0]));

        String ifName1 = rspBuilder.getLogicalInterfaceName(1);
        PowerMockito.when(SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName1))
                .thenReturn(Optional.of(MAC_ADDRESS_SF_SIDE[1]));
        PowerMockito.when(SfcGeniusDataUtils.getServiceFunctionForwarderPortMacAddress(ifName1))
                .thenReturn(Optional.of(MAC_ADDRESS_OVS_SIDE[1]));

        PowerMockito.when(SfcGeniusDataUtils.getLogicalInterfaceNameFromLocator(any(SfDataPlaneLocator.class)))
                .thenReturn(LOGICAL_INTERFACE_NAME);

        PowerMockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(null);

        PowerMockito.when(SfcGeniusDataUtils.isSfUsingALogicalInterface(any(ServiceFunction.class))).thenReturn(true);

        when(interfaceManagerRpcService.getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class)))
                .thenReturn(Futures.immediateFuture(RpcResultBuilder
                        .success(new GetEgressActionsForInterfaceOutputBuilder().setAction(new ArrayList<>()))
                        .build()));

        when(itmRpcService.getTunnelInterfaceName(any(GetTunnelInterfaceNameInput.class)))
                .thenReturn(Futures.immediateFuture(RpcResultBuilder
                        .success(new GetTunnelInterfaceNameOutputBuilder().setInterfaceName(LOGICAL_INTERFACE_NAME))
                        .build()));
    }

    @Test
    public void testEthNshFlowCreationSameComputeNode() throws Exception {
        when(interfaceManagerRpcService.getDpidFromInterface(any(GetDpidFromInterfaceInput.class)))
                // return the same dpid for every call, i.e. both SFs are hosted
                // in the same compute node
                .thenReturn(Futures.immediateFuture(RpcResultBuilder
                        .success(new GetDpidFromInterfaceOutputBuilder().setDpid(new BigInteger("1234567890")))
                        .build()));
        when(interfaceManagerRpcService.getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class)))
                .thenReturn(Futures.immediateFuture(RpcResultBuilder
                        .success(new GetEgressActionsForInterfaceOutputBuilder().setAction(new ArrayList<Action>() {
                            {
                                add(new ActionBuilder().build());
                            }
                        })).build()));
        when(interfaceManagerRpcService.getEndpointIpForDpn(any(GetEndpointIpForDpnInput.class)))
                .thenReturn(Futures.immediateFuture(
                        RpcResultBuilder.success(
                                new GetEndpointIpForDpnOutputBuilder()
                                        .setLocalIps(Collections.singletonList(LOCALHOST_IP))
                                        .build())
                                .build()));

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, true);
        sfcOfRspProcessor.processRenderedServicePath(vlanRsp);



        checkOperationalDatastoreUpdateOnRSPCreation();

        // two times when SFs are in the same compute node (only one invocation
        // per SF). When SFs
        // are in different compute nodes, there is a third invocation (for the
        // interface used
        // to go from SF1 to SF2)
        verify(interfaceManagerRpcService, times(sfTypes.size()))
                .getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class));

        // just one compute node: there must be no tunnel interface requests to
        // ITM
        verify(itmRpcService, times(0)).getTunnelInterfaceName(any(GetTunnelInterfaceNameInput.class));

        // 2 SFs, must get their respective DpnId twice in total
        verify(interfaceManagerRpcService, times(sfTypes.size()))
                .getDpidFromInterface(any(GetDpidFromInterfaceInput.class));

        // fetch the set of added flows from the openflow writer
        Set<FlowDetails> addedFlows = Whitebox.getInternalState(ofFlowWriter, "setOfFlowsToAdd");

        // Make sure we have the right amount of flows in each relevant table

        // Please note that there is only one switch being programmed in this
        // test. Even though the chain includes 2 SFs, the mocking returns the
        // same dpnid for both logical interfaces, i.e. simulating that
        // both SFs are hosted in the same compute node. It is important to
        // keep that in mind when accounting flows in the following tests

        // Logical SFF processor never uses table 0 as classifier (it uses
        // genius,
        // which uses that table for service binding)
        Assert.assertEquals(0,
                addedFlows.stream().filter(flow -> flow.getTableKey().getId().equals(TABLE_INDEX_CLASSIFIER)).count());

        // transport ingress: one (initialization in the only switch) + one per
        // (hops -1, this is the
        // number of "SF ingresses" in the chain)
        int numberOfHops = sfTypes.size() + 1;
        Assert.assertEquals(1 + numberOfHops - 1, addedFlows.stream()
                .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_INGRESS_TABLE)).count());

        // transport egress: one (initialization in the only switch) +
        // one (ChainEgress NSH C1) + two per (number of SFs) -1
        // (in this case both SFs are in the same compute node:
        // there is no a "set tunnel id = x ; then output to port y"
        // egress flow from first SF to the second one
        //Assert.assertEquals("SFC_TRANSPORT_EGRESS_TABLE", 2 + 2 * sfTypes.size() - 1, addedFlows.stream()
        //        .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_EGRESS_TABLE)).count());

        // path mapper: only the initialization flow in the only switch that it
        // is used in this test
        Assert.assertEquals(1,
                addedFlows.stream()
                        .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_PATH_MAPPER_TABLE))
                        .count());

        // path mapper acl: again, initialization only
        Assert.assertEquals(1,
                addedFlows.stream().filter(
                    flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_PATH_MAPPER_ACL_TABLE))
                        .count());

        // next hop: 1 (initialization in the only switch) + sfTypes.size()
        // (next hop to each SF)
        Assert.assertEquals("SFC_TRANSPORT_NEXT_HOP_TABLE", 1 + sfTypes.size(), addedFlows.stream()
                .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_NEXT_HOP_TABLE)).count());

        // match any: these are the 5 initialization flows for the 5 SFC tables
        // in the switch
        Assert.assertEquals(5, addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_MATCH_ANY)).count());

        Assert.assertEquals(sfTypes.size(), addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_TRANSPORT_INGRESS))
                .count());
        Assert.assertEquals(sfTypes.size(), addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_NEXT_HOP)).count());
        // only one "default egress" for each SF (the flow from SFF -> SF. There
        // would be a third one in
        // case both SFs were on different compute nodes (tunnel egress from the
        // first SF to the next one)
        Assert.assertEquals(sfTypes.size(), addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(
                    OpenflowConstants.OF_NAME_TRANSPORT_EGRESS + OpenflowConstants.OF_NAME_DELIMITER))
                .count());

        // we'll save in this set all the flows that are checked,
        // so that we can assure that all flows were accounted for
        Set<Flow> checkedFlows = new HashSet<>();

        // nextHop
        Assert.assertTrue(addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_NEXT_HOP))
                .peek(checkedFlows::add)
                .allMatch(nextHopFlow -> matchNextHop(nextHopFlow, vlanRsp.getPathId())));

        // transport ingress
        Assert.assertTrue(addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_TRANSPORT_INGRESS))
                .peek(checkedFlows::add)
                .allMatch(flow -> matchTransportIngress(flow, vlanRsp.getPathId())));

        // transport egress between SFFs
        Assert.assertTrue(addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_TRANSPORT_EGRESS
                        + OpenflowConstants.OF_NAME_DELIMITER))
                .peek(checkedFlows::add).allMatch(
                    transportEgressFlow -> matchTransportEgress(transportEgressFlow, vlanRsp.getPathId())));

        // transport egress last hop
        addedFlows.stream()
                .map(FlowDetails::getFlow)
                .filter(flow -> flow.getFlowName()
                        .startsWith(OpenflowConstants.OF_NAME_LASTHOP_TRANSPORT_EGRESS))
                .peek(checkedFlows::add)
                .forEach(flow -> assertTransportEgressLastHop(flow, vlanRsp.getPathId(), (short) (255 - sfTypes.size()),
                        MAC_ADDRESS_SF_SIDE[1], LOCALHOST_IP, -1));

        // assure that the only flows we didn't check are the MatchAny flows
        Assert.assertEquals(addedFlows.size() - checkedFlows.size(),
                addedFlows.stream().filter(flowd -> !checkedFlows.contains(flowd.getFlow()))
                        .filter(flowd -> flowd.getFlow().getFlowName()
                            .startsWith(OpenflowConstants.OF_NAME_MATCH_ANY)).count());
    }

    @Test
    public void testEthNshFlowCreationDifferentComputeNode() throws Exception {
        GetDpidFromInterfaceInput if1 = new GetDpidFromInterfaceInputBuilder().setIntfName("tap0000-00").build();
        GetDpidFromInterfaceInput if2 = new GetDpidFromInterfaceInputBuilder().setIntfName("tap0000-01").build();
        when(interfaceManagerRpcService.getDpidFromInterface(if1)).thenReturn(Futures.immediateFuture(RpcResultBuilder
                .success(new GetDpidFromInterfaceOutputBuilder().setDpid(new BigInteger("1234567890"))).build()));
        when(interfaceManagerRpcService.getDpidFromInterface(if2)).thenReturn(Futures.immediateFuture(RpcResultBuilder
                .success(new GetDpidFromInterfaceOutputBuilder().setDpid(new BigInteger("9876543210"))).build()));
        when(interfaceManagerRpcService.getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class)))
                .thenReturn(Futures.immediateFuture(RpcResultBuilder
                        .success(new GetEgressActionsForInterfaceOutputBuilder().setAction(new ArrayList<Action>() {
                            {
                                add(new ActionBuilder().build());
                            }
                        })).build()));
        when(interfaceManagerRpcService.getEndpointIpForDpn(any(GetEndpointIpForDpnInput.class)))
                .thenReturn(Futures.immediateFuture(
                        RpcResultBuilder.success(
                                new GetEndpointIpForDpnOutputBuilder()
                                        .setLocalIps(Collections.singletonList(VTEP_IP))
                                        .build())
                                .build()));

        PowerMockito.mockStatic(SfcOvsUtil.class);
        PowerMockito.when(SfcOvsUtil.getVxlanGpeOfPort(any())).thenReturn(VXLAN_GPE_OF_PORT);
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(any(), any())).thenReturn(null);

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, true);
        sfcOfRspProcessor.processRenderedServicePath(vlanRsp);

        int numberOfHops = sfTypes.size() + 1;
        verify(interfaceManagerRpcService, times(numberOfHops))
                .getEgressActionsForInterface(any(GetEgressActionsForInterfaceInput.class));

        // 2 SFFs, meaning 1 hop between SFFs, must get the logical interface
        // between them just once
        verify(itmRpcService).getTunnelInterfaceName(any(GetTunnelInterfaceNameInput.class));

        // 2 SFs, must get their respective DpnId twice in total
        verify(interfaceManagerRpcService, times(sfTypes.size()))
                .getDpidFromInterface(any(GetDpidFromInterfaceInput.class));

        // fetch the set of added flows from the openflow writer
        Set<FlowDetails> addedFlows = Whitebox.getInternalState(ofFlowWriter, "setOfFlowsToAdd");

        // Make sure we have the right amount of flows in each relevant table

        // Logical SFF processor never uses table 0 as classifier (it uses
        // genius,
        // which uses that table for service binding)
        //Assert.assertEquals(0,
        //        addedFlows.stream().filter(flow -> flow.getTableKey().getId().equals(TABLE_INDEX_CLASSIFIER)).count
        // ());

        // transport ingress: one (initialization in the only switch) + one per
        // (hops -1, this is the
        // number of "SF ingresses" in the chain)
        Assert.assertEquals("SFC_TRANSPORT_INGRESS_TABLE", 2 + numberOfHops - 1, addedFlows.stream()
                .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_INGRESS_TABLE)).count());

        // transport egress: one (initialization in the only switch) +
        // 5 (ChainEgress) + one per (hops -1, this is the
        // number of "SF egresses" in the chain)
        Assert.assertEquals("SFC_TRANSPORT_EGRESS_TABLE", 6 + 2 * (numberOfHops - 1), addedFlows.stream()
                .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_EGRESS_TABLE)).count());

        // path mapper: only the initialization flow in the two switches that
        // are used in this test
        Assert.assertEquals(2,
                addedFlows.stream()
                        .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_PATH_MAPPER_TABLE))
                        .count());

        // path mapper acl: again, initialization only
        Assert.assertEquals(2,
                addedFlows.stream().filter(
                    flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_PATH_MAPPER_ACL_TABLE))
                        .count());

        // next hop: 2 (initialization in the two switches) + sfTypes.size()
        // (next hop to each SF)
        Assert.assertEquals("SFC_TRANSPORT_NEXT_HOP_TABLE", 2 + sfTypes.size(), addedFlows.stream()
                .filter(flow -> flow.getTableKey().getId().equals(NwConstants.SFC_TRANSPORT_NEXT_HOP_TABLE)).count());

        // match any: these are the 10 initialization flows for the 5 SFC tables
        // in the switch
        Assert.assertEquals(OpenflowConstants.OF_NAME_MATCH_ANY, 10,
                addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_MATCH_ANY)).count());

        Assert.assertEquals(OpenflowConstants.OF_NAME_TRANSPORT_INGRESS, sfTypes.size(),
                addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                        .filter(flow -> flow.getFlowName()
                                .startsWith(OpenflowConstants.OF_NAME_TRANSPORT_INGRESS)).count());
        Assert.assertEquals(OpenflowConstants.OF_NAME_NEXT_HOP, sfTypes.size(),
                addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_NEXT_HOP)).count());
        Assert.assertEquals(OpenflowConstants.OF_NAME_TRANSPORT_EGRESS, numberOfHops,
                addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(
                    OpenflowConstants.OF_NAME_TRANSPORT_EGRESS + OpenflowConstants.OF_NAME_DELIMITER))
                .count());

        // we'll save in this set all the flows that are checked,
        // so that we can assure that all flows were accounted for
        Set<Flow> checkedFlows = new HashSet<>();

        // nextHop
        Assert.assertTrue(addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_NEXT_HOP))
                .peek(checkedFlows::add)
                .allMatch(nextHopFlow -> matchNextHop(nextHopFlow, vlanRsp.getPathId())));

        // transport ingress
        Assert.assertTrue(addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName().startsWith(OpenflowConstants.OF_NAME_TRANSPORT_INGRESS))
                .peek(checkedFlows::add)
                .allMatch(flow -> matchTransportIngress(flow, vlanRsp.getPathId())));

        // transport egress between SFFs
        Assert.assertTrue(addedFlows.stream().map(flowDetail -> flowDetail.getFlow())
                .filter(flow -> flow.getFlowName()
                        .startsWith(OpenflowConstants.OF_NAME_TRANSPORT_EGRESS
                                + OpenflowConstants.OF_NAME_DELIMITER))
                .peek(checkedFlows::add).allMatch(
                    transportEgressFlow -> matchTransportEgress(transportEgressFlow, vlanRsp.getPathId())));

        // transport egress last hop
        addedFlows.stream()
                .map(FlowDetails::getFlow)
                .filter(flow -> flow.getFlowName()
                        .startsWith(OpenflowConstants.OF_NAME_LASTHOP_TRANSPORT_EGRESS))
                .peek(checkedFlows::add)
                .forEach(flow -> assertTransportEgressLastHop(flow, vlanRsp.getPathId(), (short) (255 - sfTypes.size()),
                        MAC_ADDRESS_SF_SIDE[1], VTEP_IP, VXLAN_GPE_OF_PORT));

        // assure that the only flows we didn't check are the MatchAny flows
        Assert.assertEquals(addedFlows.size() - checkedFlows.size(),
                addedFlows.stream().filter(flowd -> !checkedFlows.contains(flowd.getFlow()))
                        .filter(flowd -> flowd.getFlow().getFlowName().startsWith(
                                OpenflowConstants.OF_NAME_MATCH_ANY)).count());
    }

    /*
     * Delete a RSP - Check that Operational Datastore is updated accordingly
     */
    @Test
    public void testOperationalDatastoreUpdateOnRSPDeletion() throws Exception {
        RenderedServicePath rsp = rspBuilder.createRspFromSfTypes(this.sfTypes, true);
        sfcOfRspProcessor.deleteRenderedServicePath(rsp);
        checkOperationalDatastoreUpdateOnRSPDeletion();
    }

    /**
     * Verifications regarding operational datastore updates for logical sff (at
     * RSP creation time).
     *
     * @throws Exception
     *             when some problem occur during verification
     */
    private void checkOperationalDatastoreUpdateOnRSPCreation() throws Exception {
        Mockito.verify(operDsUpdateHandler).onRspCreation(any(), any());
        PowerMockito.verifyPrivate(operDsUpdateHandler).invoke("updateRenderedServicePathOperationalStateWithDpnIds",
                any(), any(), any());
        PowerMockito.verifyPrivate(operDsUpdateHandler).invoke("updateSffStateWithDpnIds", any(), any(), any());
        PowerMockito.verifyPrivate(operDsUpdateHandler).invoke("commitChangesAsync", any());
        PowerMockito.verifyNoMoreInteractions(operDsUpdateHandler);
    }

    /**
     * Verifications regarding operational datastore updates for logical sff (at
     * RSP deletion time).
     *
     * @throws Exception
     *             when some problem occur during verification
     */
    private void checkOperationalDatastoreUpdateOnRSPDeletion() throws Exception {
        Mockito.verify(operDsUpdateHandler).onRspDeletion(any());
        PowerMockito.verifyPrivate(operDsUpdateHandler).invoke("deleteRspFromSffState", any(), any());
        PowerMockito.verifyPrivate(operDsUpdateHandler).invoke("commitChangesAsync", any());
        PowerMockito.verifyNoMoreInteractions(operDsUpdateHandler);
    }

    private boolean matchTransportEgress(Flow transportEgressFlow, long rspId) {
        // check matches - check NSP + NSI
        List<NxAugMatchNodesNodeTableFlow> theNciraExtensions = getNciraExtensions(transportEgressFlow);

        // check the NSH headers - only NSP for the last hop
        if (!checkNshMatches(theNciraExtensions, true, rspId)) {
            return false;
        }

        List<Action> actionList = getActionsFromFlow(transportEgressFlow);

        // If there is only one action, it must be empty (that is the genius
        // RPC mock for
        // hops in the same compute node
        if (actionList.size() == 1 && Optional.ofNullable(actionList.get(0).getAction()).isPresent()) {
            return false;
        } else if (actionList.size() == 2) {
            // if there are two actions: 1 empty action (just like the
            // genius RPC mock
            // is defined) + "set NP=4" (added to transport egress when the
            // SFs are in different
            // compute nodes in the hop
            if (Optional.ofNullable(actionList.get(0).getAction()).isPresent()) {
                return false;
            }
            Optional<NxRegLoad> nxRegLoadAction = actionList.stream()
                    .map(action -> filterActionType(action,
                            NxActionRegLoadNodesNodeTableFlowApplyActionsCase.class))
                    .filter(Optional::isPresent).map(Optional::get).findFirst()
                    .map(NxActionRegLoadNodesNodeTableFlowApplyActionsCase::getNxRegLoad);
            // check actions - nx action reg load (NP=4)
            if (!nxRegLoadAction.isPresent()) {
                return false;
            }
            // there are no other valid possibilities
        } else if (actionList.size() < 1 || actionList.size() > 2) {
            return false;
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

        Short theNsi = theNciraExtensions.stream().filter(node -> node.getNxmNxNsi() != null)
                .map(NxAugMatchNodesNodeTableFlow::getNxmNxNsi).findFirst().map(NxmNxNsi::getNsi).get();

        // handle the Actions part
        // assure 1 goto table instruction; goto table transport egress
        Optional<Short> goToTableId = getGotoTableIdFromIntructions(nextHopFlow);
        if (!goToTableId.filter(tableId -> tableId.equals(NwConstants.SFC_TRANSPORT_EGRESS_TABLE)).isPresent()) {
            return false;
        }

        // assure the destination mac address is the expected
        List<Action> actionList = getInstructionsFromFlow(nextHopFlow).stream()
                .map(inst -> filterInstructionType(inst, ApplyActionsCase.class))
                .map(applyActionsInst -> applyActionsInst.get().getApplyActions()).map(ApplyActions::getAction)
                .findFirst().orElse(Collections.emptyList());

        List<MacAddress> macAddresses = actionList.stream()
                .map(action -> filterActionType(action, NxActionRegLoadNodesNodeTableFlowApplyActionsCase.class))
                .filter(Optional::isPresent)
                .map(NxActionRegLoadNodesNodeTableFlowApplyActions -> NxActionRegLoadNodesNodeTableFlowApplyActions
                        .get().getNxRegLoad())
                .map(NxRegLoad::getValue).map(SfcOpenflowUtils::macStringFromBigInteger)
                .map(value -> new MacAddress(value)).collect(Collectors.toList());

        // there must be exactly two actions: one for replacing the destination
        // MAC address (the target SF) and other for setting
        // the source MAC address (so the packet can be returned after SF
        // processing)
        if (macAddresses.size() != 2) {
            return false;
        }
        Short startingIndex = rspBuilder.getStartingIndex();
        // we assign mac addresses in order, so first hop corresponds to first
        // addr set, and so on:
        Integer macAddrIndex = startingIndex - theNsi;
        List<MacAddress> expectedMacs = new ArrayList<>();
        expectedMacs.add(MAC_ADDRESS_SF_SIDE[macAddrIndex]);
        expectedMacs.add(MAC_ADDRESS_OVS_SIDE[macAddrIndex]);
        if (!macAddresses.containsAll(expectedMacs)) {
            return false;
        }

        return true;
    }

    private List<Instruction> getInstructionsFromFlow(Flow theFlow) {
        return Optional.ofNullable(theFlow.getInstructions()).map(Instructions::getInstruction)
                .orElse(Collections.emptyList());
    }

    List<Action> getActionsFromFlow(Flow theFlow) {
        List<Action> actionList = getInstructionsFromFlow(theFlow).stream()
                .map(inst -> filterInstructionType(inst, ApplyActionsCase.class)).filter(Optional::isPresent)
                .map(Optional::get).findFirst().map(ApplyActionsCase::getApplyActions).map(ApplyActions::getAction)
                .orElse(Collections.emptyList());
        return actionList;
    }

    private List<NxAugMatchNodesNodeTableFlow> getNciraExtensions(Flow theFlow) {
        if (theFlow.getMatch() == null) {
            return Collections.emptyList();
        }

        return Optional.ofNullable(theFlow.getMatch().getAugmentation(GeneralAugMatchNodesNodeTableFlow.class))
                .map(GeneralAugMatchNodesNodeTableFlow::getExtensionList).orElse(Collections.emptyList()).stream()
                .map(ExtensionList::getExtension)
                .map(extension -> extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class))
                .collect(Collectors.toList());
    }

    private Optional<Short> getGotoTableIdFromIntructions(Flow theFlow) {
        return getInstructionsFromFlow(theFlow).stream().map(inst -> filterInstructionType(inst, GoToTableCase.class))
                .filter(Optional::isPresent).map(gotoTableInst -> gotoTableInst.get().getGoToTable())
                .map(GoToTable::getTableId).findFirst();
    }

    private boolean checkNshMatches(List<NxAugMatchNodesNodeTableFlow> theNciraExtensions, boolean checkNsi,
            long rspId) {
        Optional<NxmNxNsp> theNsp = theNciraExtensions.stream().filter(node -> node.getNxmNxNsp() != null)
                .map(NxAugMatchNodesNodeTableFlow::getNxmNxNsp).findFirst();
        Optional<NxmNxNsi> theNsi = theNciraExtensions.stream().filter(node -> node.getNxmNxNsi() != null)
                .map(NxAugMatchNodesNodeTableFlow::getNxmNxNsi).findFirst();

        // must check if the NSP header matches the RSP ID
        if (!theNsp.map(NxmNxNsp::getValue).filter(nsp -> nsp.equals(rspId)).isPresent()) {
            return false;
        }

        // must check if the NSI is within range
        if (checkNsi) {
            if (!theNsi.map(NxmNxNsi::getNsi).filter(nsi -> nsi <= 255).filter(nsi -> nsi >= 255 - sfTypes.size())
                    .isPresent()) {
                return false;
            }
        }
        return true;
    }

    private <T extends org.opendaylight.yang.gen.v1.urn.opendaylight.flow
        .types.rev131026.instruction.Instruction> Optional<T> filterInstructionType(
            Instruction theInstruction, Class<T> instructionType) {
        return Optional.ofNullable(theInstruction.getInstruction())
                .filter(instruction -> instruction.getImplementedInterface().equals(instructionType))
                .map(instructionType::cast);
    }

    private <T extends org.opendaylight.yang.gen.v1.urn.opendaylight.action
        .types.rev131112.action.Action> Optional<T> filterActionType(
            Action theAction, Class<T> actionType) {
        return Optional.ofNullable(theAction.getAction())
                .filter(action -> action.getImplementedInterface().equals(actionType)).map(actionType::cast);
    }


    private void assertNshFields(Flow theFlow, Long theNsp, Short theNsi, Long theNshc1, Long theNshc2) {
        List<NxAugMatchNodesNodeTableFlow> nciraExtensions = getNciraExtensions(theFlow);
        Optional<Long> nsp = nciraExtensions.stream()
                .map(NxmNxNspGrouping::getNxmNxNsp)
                .filter(Objects::nonNull)
                .findFirst()
                .map(NxmNxNsp::getValue);
        assertThat(nsp, is(Optional.ofNullable(theNsp)));
        Optional<Short> nsi = nciraExtensions.stream()
                .map(NxmNxNsiGrouping::getNxmNxNsi)
                .filter(Objects::nonNull)
                .findFirst()
                .map(NxmNxNsi::getNsi);
        assertThat(nsi, is(Optional.ofNullable(theNsi)));
        Optional<Long> nshc1 = nciraExtensions.stream()
                .map(NxmNxNshc1Grouping::getNxmNxNshc1)
                .filter(Objects::nonNull)
                .findFirst()
                .map(NxmNxNshc1::getValue);
        assertThat(nshc1, is(Optional.ofNullable(theNshc1)));
        Optional<Long> nshc2 = nciraExtensions.stream()
                .map(NxmNxNshc2Grouping::getNxmNxNshc2)
                .filter(Objects::nonNull)
                .findFirst()
                .map(NxmNxNshc2::getValue);
        assertThat(nshc2, is(Optional.ofNullable(theNshc2)));
    }

    private void assertPopNsh(Flow theFlow) {
        Optional<NxPopNsh> popNsh = getActionsFromFlow(theFlow).stream()
                .map(action -> filterActionType(action, NxActionPopNshNodesNodeTableFlowApplyActionsCase.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(NxActionPopNshNodesNodeTableFlowApplyActionsCase::getNxPopNsh);
        assertThat(popNsh.isPresent(), is(true));
    }

    private void assertSetEthSrc(Flow theFlow, MacAddress theMacAddress) {
        Optional<MacAddress> setEthSrc = getActionsFromFlow(theFlow).stream()
                .map(action -> filterActionType(action, SetFieldCase.class))
                .map(o -> o.map(SetFieldCase::getSetField))
                .map(o -> o.map(Match::getEthernetMatch))
                .map(o -> o.map(EthernetMatchFields::getEthernetSource))
                .map(o -> o.map(EthernetSource::getAddress))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        assertThat(setEthSrc.isPresent(), is(true));
        assertThat(setEthSrc.get(), is(theMacAddress));
    }

    private void assertResubmit(Flow theFlow, short theTable) {
        Optional<Short> resubmit = getActionsFromFlow(theFlow).stream()
                .map(action -> filterActionType(action, NxActionResubmitNodesNodeTableFlowWriteActionsCase.class))
                .map(o -> o.map(NxActionResubmitNodesNodeTableFlowWriteActionsCase::getNxResubmit))
                .map(o -> o.map(NxResubmit::getTable))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        assertThat(resubmit.isPresent(), is(true));
        assertThat(resubmit.get(), is(theTable));
    }

    private void assertMoveC1ToTunDst(Flow theFlow) {
        Optional<NxRegMove> nxRegMoveC1ToTunDstAction = getActionsFromFlow(theFlow).stream()
                .map(action -> filterActionType(action, NxActionRegMoveNodesNodeTableFlowApplyActionsCase.class))
                .map(o -> o.map(NxActionRegMoveNodesNodeTableFlowApplyActionsCase::getNxRegMove))
                .filter(o -> o
                        .map(NxRegMove::getSrc)
                        .map(SrcChoiceGrouping::getSrcChoice)
                        .map(src -> src instanceof SrcNxNshc1Case)
                        .orElse(false))
                .filter(o -> o
                        .map(NxRegMove::getDst)
                        .map(DstChoiceGrouping::getDstChoice)
                        .map(src -> src instanceof DstNxTunIpv4DstCase)
                        .orElse(false))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        assertThat(nxRegMoveC1ToTunDstAction.isPresent(), is(true));
    }

    private void assertMoveC2ToTunId(Flow theFlow) {
        Optional<NxRegMove> nxRegMoveC2ToTunIdAction = getActionsFromFlow(theFlow).stream()
                .map(action -> filterActionType(action, NxActionRegMoveNodesNodeTableFlowApplyActionsCase.class))
                .map(o -> o.map(NxActionRegMoveNodesNodeTableFlowApplyActionsCase::getNxRegMove))
                .filter(o -> o
                        .map(NxRegMove::getSrc)
                        .map(SrcChoiceGrouping::getSrcChoice)
                        .map(src -> src instanceof SrcNxNshc2Case)
                        .orElse(false))
                .filter(o -> o
                        .map(NxRegMove::getDst)
                        .map(DstChoiceGrouping::getDstChoice)
                        .map(src -> src instanceof DstNxTunIdCase)
                        .orElse(false))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        assertThat(nxRegMoveC2ToTunIdAction.isPresent(), is(true));
    }

    private void assertOutput(Flow theFlow, long theOutputPort) {
        Optional<String> output = getActionsFromFlow(theFlow).stream()
                .map(action -> filterActionType(action, OutputActionCase.class))
                .map(o -> o.map(OutputActionCase::getOutputAction))
                .map(o -> o.map(OutputAction::getOutputNodeConnector))
                .map(o -> o.map(Uri::getValue))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        assertThat(output.isPresent(), is(true));
        assertThat(output.get(), is("output:" + theOutputPort));
    }

    private void assertTransportEgressLastHopPipelineFlow(Flow theFlow, long theNsp, short theNsi,
                                                          MacAddress theMacAddress) {
        assertThat(theFlow.getPriority(), is(680));
        assertNshFields(theFlow, theNsp, theNsi, 0L, null);
        assertPopNsh(theFlow);
        assertSetEthSrc(theFlow, theMacAddress);
        assertResubmit(theFlow, NwConstants.LPORT_DISPATCHER_TABLE);
    }

    private void assertTransportEgressLastHopRemoteTunnel(Flow theFlow, long theNsp, short theNsi, long theOutputPort) {
        assertThat(theFlow.getPriority(), is(650));
        assertNshFields(theFlow, theNsp, theNsi, null, null);
        assertPopNsh(theFlow);
        assertMoveC1ToTunDst(theFlow);
        assertMoveC2ToTunId(theFlow);
        assertOutput(theFlow, theOutputPort);
    }

    private void assertTransportEgressLastHopLocalTunnel(Flow theFlow, long theNsp, short theNsi,
                                                         IpAddress theSffIpAddress) {
        assertThat(theFlow.getPriority(), is(660));
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(theSffIpAddress.getIpv4Address().getValue()));
        long ipl = ip & 0xffffffffL;
        assertNshFields(theFlow, theNsp, theNsi, ipl, null);
        assertPopNsh(theFlow);
        assertMoveC1ToTunDst(theFlow);
        assertMoveC2ToTunId(theFlow);
        assertResubmit(theFlow, NwConstants.INTERNAL_TUNNEL_TABLE);
    }

    private void assertTransportEgressLastHopRemoteNsh(Flow theFlow, long theNsp, short theNsi, long theOutputPort) {
        assertThat(theFlow.getPriority(), is(670));
        assertNshFields(theFlow, theNsp, theNsi, null, 0L);
        assertMoveC1ToTunDst(theFlow);
        assertOutput(theFlow, theOutputPort);
    }

    private void assertTransportEgressLastHopLocalNsh(Flow theFlow, long theNsp, short theNsi,
                                                      IpAddress theSffIpAddress) {
        assertThat(theFlow.getPriority(), is(680));
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(theSffIpAddress.getIpv4Address().getValue()));
        long ipl = ip & 0xffffffffL;
        assertNshFields(theFlow, theNsp, theNsi, ipl, 0L);
        assertResubmit(theFlow, NwConstants.LPORT_DISPATCHER_TABLE);
    }

    private void assertTransportEgressLastHop(Flow theFlow, long theNsp, short theNsi,
                                              MacAddress theSfMacAddress, IpAddress theSffIpAddress,
                                              long theOutputPort) {
        assertThat(theFlow.getCookie(), notNullValue());
        assertThat(theFlow.getCookie().getValue(), notNullValue());
        BigInteger cookie = theFlow.getCookie().getValue();

        BigInteger egresspipelineCookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_PIPELINE_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);
        if (cookie.equals(egresspipelineCookie)) {
            assertTransportEgressLastHopPipelineFlow(theFlow, theNsp, theNsi, theSfMacAddress);
            return;
        }

        BigInteger remoteTunnelCookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_REMOTE_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);
        if (cookie.equals(remoteTunnelCookie)) {
            assertTransportEgressLastHopRemoteTunnel(theFlow, theNsp, theNsi, theOutputPort);
            return;
        }

        BigInteger localTunnelCookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_LOCAL_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);
        if (cookie.equals(localTunnelCookie)) {
            assertTransportEgressLastHopLocalTunnel(theFlow, theNsp, theNsi, theSffIpAddress);
            return;
        }

        BigInteger remoteNshCookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_REMOTE_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);
        if (cookie.equals(remoteNshCookie)) {
            assertTransportEgressLastHopRemoteNsh(theFlow, theNsp, theNsi, theOutputPort);
            return;
        }

        BigInteger localNshCookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_LOCAL_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);
        if (cookie.equals(localNshCookie)) {
            assertTransportEgressLastHopLocalNsh(theFlow, theNsp, theNsi, theSffIpAddress);
            return;
        }

        assert false : "Unrecognized cookie on last hop egress flow, cookie = " + cookie;
    }
}

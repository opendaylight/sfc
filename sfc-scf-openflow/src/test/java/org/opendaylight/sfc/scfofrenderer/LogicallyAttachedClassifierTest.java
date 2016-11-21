/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcGeniusDataUtils.class,
        SfcGeniusRpcClient.class,
        SfcNshHeader.class,
        LogicalClassifierDataGetter.class,
        SfcProviderServiceFunctionAPI.class})
public class LogicallyAttachedClassifierTest {
    @InjectMocks
    private SfcGeniusRpcClient geniusClient;

    @Mock
    private DataBroker dataBroker;

    @Mock
    private OdlInterfaceRpcService interfaceManagerRpcService;

    @Mock
    private ItmRpcService itmRpcService;

    @Mock
    private ServiceFunctionForwarder sff;

    @Mock
    private ServiceFunction sf;

    @Mock
    ReadWriteTransaction readWriteTransaction;

    @Mock
    Match aclMatch;

    @Mock
    LogicalClassifierDataGetter dataGetter;

    private String clientVmInterfaceNeutronPort;

    private String firstSfNeutronPort;

    private String classifierNeutronPort;

    private LogicallyAttachedClassifier logicalScf;

    private static final String FIRST_SF_NODE_NAME = "openflow:1234567890";

    private static final String CLASSIFIER_NODE_NAME = "openflow:9876543210";

    public LogicallyAttachedClassifierTest() {
        initMocks(this);

        clientVmInterfaceNeutronPort = "177bef73-514e-4922-990f-d7aba0f3b0f4";
        firstSfNeutronPort = "eba7a40e-a8f7-4b25-b1a2-4128f0da988e";
        classifierNeutronPort = "3c54af16-db5b-4f03-8ed8-9d008c164014";
    }

    @Before
    public void setUp() {

        // mock the first SFF in the RSP
        sff = mock(ServiceFunctionForwarder.class);
        when(sff.getSffDataPlaneLocator()).thenReturn(null);

        // mock the first SF in the RSP
        String sfName = "SF#1";
        sf = mock(ServiceFunction.class);
        List<SfDataPlaneLocator> theSfDpls = new ArrayList<SfDataPlaneLocator>() {{
            LocatorType theLocatorType = new LogicalInterfaceBuilder().setInterfaceName(firstSfNeutronPort).build();
            add(new SfDataPlaneLocatorBuilder().setLocatorType(theLocatorType).build());
        }};
        when(sf.getSfDataPlaneLocator()).thenReturn(theSfDpls);
        when(sf.getName()).thenReturn(new SfName(sfName));

        when(dataGetter.getNodeName(anyString())).thenReturn(Optional.of(CLASSIFIER_NODE_NAME));
        when(dataGetter.getEgressActionsForTunnelInterface(anyString(), any(Integer.class)))
                .thenReturn(Collections.emptyList());
        when(dataGetter.getFirstHopNodeName(any(SfcNshHeader.class))).thenReturn(Optional.of(FIRST_SF_NODE_NAME));
        when(dataGetter.getInterfaceBetweenDpnIds(any(DpnIdType.class), any(DpnIdType.class)))
                .thenReturn(Optional.of("tun-1234"));

        logicalScf = new LogicallyAttachedClassifier(sff, dataGetter);

        PowerMockito.mockStatic(LogicalClassifierDataGetter.class);
        PowerMockito.when(LogicalClassifierDataGetter.getDpnIdFromNodeName(anyString()))
                .thenReturn(new DpnIdType(new BigInteger("1234567890")));
        PowerMockito.when(LogicalClassifierDataGetter.getOpenflowPort(anyString())).thenReturn(Optional.of(2L));

        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        PowerMockito.when(SfcProviderServiceFunctionAPI.readServiceFunction(any(SfName.class)))
                .thenReturn(sf);

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOvsPort"))
                .toReturn(2L);

        PowerMockito.stub(PowerMockito.method(SfcNshHeader.class, "getSfcNshHeader"))
                .toReturn(new SfcNshHeader()
                        .setFirstSfName(new SfName(sfName))
                        .setNshEndNsi((short) 254)
                        .setNshMetaC1(123L)
                        .setNshMetaC2(321L)
                        .setNshMetaC3(2323L)
                        .setNshMetaC4(3232L)
                        .setNshNsp(666L)
                        .setNshStartNsi((short) 255)
                        .setSffName(new SffName("sff#1"))
                        .setVxlanIpDst(new Ipv4Address("192.168.1.1"))
                        .setVxlanUdpPort(new PortNumber(8080)));

        PowerMockito.stub(PowerMockito.method(SfcGeniusDataUtils.class, "getInterfaceLowerLayerIf"))
                .toReturn(new ArrayList<String>(){{add("openflow:1234567890:2");}});
    }

    @After
    public void cleanStuff() {

    }

    @Test
    public void initClassifierTest() {
        Assert.assertFalse(logicalScf.initClassifierTable().getInstructions().getInstruction().isEmpty());
    }

    @Test
    public void outFlowPositiveCoLocated() {
        Assert.assertTrue(logicalScf.usesLogicalInterfaces());

        FlowBuilder flow = logicalScf.createClassifierOutFlow("the-key",
                aclMatch,
                SfcNshHeader.getSfcNshHeader(new RspName("RSP_1")),
                "openflow:1234567890");

        Assert.assertEquals(2, flow.getInstructions().getInstruction().size());

        // push NSH...
        // TODO: check this better...
        Assert.assertEquals(10,
                ((ApplyActionsCase) flow.getInstructions().getInstruction().get(0).getInstruction())
                        .getApplyActions().getAction().size());

        Assert.assertEquals(10,
                ((ApplyActionsCase) flow.getInstructions().getInstruction().get(0).getInstruction())
                        .getApplyActions().getAction().size());

        Assert.assertEquals(new Short("83"),
                ((GoToTableCase) flow.getInstructions().getInstruction().get(1).getInstruction())
                        .getGoToTable().getTableId());
    }

    @Test
    public void outFlowPositiveUseTunnel() {
        Assert.assertTrue(logicalScf.usesLogicalInterfaces());

        FlowBuilder flow = logicalScf.createClassifierOutFlow("the-key",
                aclMatch,
                SfcNshHeader.getSfcNshHeader(new RspName("RSP_1")),
                "openflow:9876543210");

        Assert.assertEquals(1, flow.getInstructions().getInstruction().size());

        // push NSH...
        // TODO: check this better...
        // it is 10 because we're mocking the genius RPC, making it return an empty Action List
        Assert.assertEquals(10,
                ((ApplyActionsCase) flow.getInstructions().getInstruction().get(0).getInstruction())
                        .getApplyActions().getAction().size());
    }

    @Test
    public void outFlowEmptyFlowKey() {
        String classifierNodeName = "openflow:1234567890";
        Assert.assertNull(logicalScf.createClassifierOutFlow(
                null,
                aclMatch,
                SfcNshHeader.getSfcNshHeader(new RspName("RSP_1")),
                classifierNodeName));
    }

    @Test
    public void outFlowEmptyNshHeader() {
        String classifierNodeName = "openflow:1234567890";
        Assert.assertNull(logicalScf.createClassifierOutFlow(
                "the-key",
                aclMatch,
                null,
                classifierNodeName));
    }
}

/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerInterface;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Testing for the SfcL2RspProcessor class. The SfcL2RspProcessor receives
 * a Rendered Service Path (RSP) as input, and as output creates OpenFlow flows.
 * These Unit Tests isolate the SfcL2RspProcessor class by mocking the
 * SfcL2FlowProgrammerOFimpl class with Mockito. Given a particular RSP, the
 * SfcL2RspProcessor should create several OpenFlow flows by calling into the
 * SfcL2FlowProgrammerInterface, whose implementation is SfcL2FlowProgrammerOFimpl.
 * The SfcL2FlowProgrammerOFimpl is mocked with Mockito, which allows us to
 * verify that exactly the correct Flow creation methods are called with the
 * expected arguments.
 *
 * @author ebrjohn
 */

public class SfcL2RspProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RspProcessorTest.class);
    SfcL2RspProcessor sfcL2RspProcessor;
    RspBuilder rspBuilder;
    SfcL2FlowProgrammerInterface flowProgrammerTestMoc;
    SfcL2ProviderUtilsTestMock sfcUtilsTestMock;
    List<SftType> sfTypes;

    public SfcL2RspProcessorTest() {
        LOG.info("SfcL2RspProcessorTest constructor");

        this.flowProgrammerTestMoc = mock(SfcL2FlowProgrammerOFimpl.class);
        this.flowProgrammerTestMoc.setFlowWriter(mock(SfcL2FlowWriterInterface.class));
        this.sfcUtilsTestMock = new SfcL2ProviderUtilsTestMock();
        this.sfcL2RspProcessor =
                new SfcL2RspProcessor(this.flowProgrammerTestMoc, this.sfcUtilsTestMock, new SfcSynchronizer());
        this.rspBuilder = new RspBuilder(this.sfcUtilsTestMock);

        this.sfTypes = new ArrayList<>();
        this.sfTypes.add(new SftType("firewall"));
        this.sfTypes.add(new SftType("http-header-enrichment"));
    }

    @Before
    public void before() throws ExecutionException, InterruptedException {
        LOG.info("SfcL2RspProcessorTest before()");

        // this.flowProgrammerTestMoc = mock(SfcL2FlowProgrammerOFimpl.class);
        sfcUtilsTestMock.resetCache();
    }

    private void assertMatchAnyMethodsCalled(String sffName) {
        verify(this.flowProgrammerTestMoc).configureTransportIngressTableMatchAny(eq(sffName));
        verify(this.flowProgrammerTestMoc).configurePathMapperTableMatchAny(eq(sffName));
        verify(this.flowProgrammerTestMoc).configurePathMapperAclTableMatchAny(eq(sffName));
        verify(this.flowProgrammerTestMoc).configureNextHopTableMatchAny(eq(sffName));
        verify(this.flowProgrammerTestMoc).configureTransportEgressTableMatchAny(eq(sffName));
        verify(this.flowProgrammerTestMoc).configureClassifierTableMatchAny(eq(sffName));
    }

    // TODO tests to add:
    // - An SFF with > 1 SF

    @Test
    public void testVlanFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testVlanFlowCreation");

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mac.class);
        this.sfcL2RspProcessor.processRenderedServicePath(vlanRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcL2FlowWriterInterface) anyObject());

        // Verify calls to configureVlanTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureVlanTransportIngressFlow("SFF_0");
        verify(this.flowProgrammerTestMoc, times(2)).configureVlanTransportIngressFlow("SFF_1");

        // TODO need to parameterize all these parameter values and use them in
        // the RSP creation. As-is now, its pretty ugly to have all these
        // explicit values

        // Notice: the SFF vlanIds are calculated in SfcRspProcessorVlan and are
        // static, meaning the previous result will be remembered from previous
        // runs and vlanIds are not recycled. So, the vlanIds seen in this test
        // depend on if other Vlan Unit Tests are run before of after this one.

        // Verify calls to configureVlanPathMapperFlow
        // the first 2 calls are SFF vlans
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow(eq("SFF_0"), anyInt(), eq((long) 0),
                eq(false));
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow(eq("SFF_1"), anyInt(), eq((long) 0),
                eq(false));
        // the next 2 are SF vlans, these calls should instead be configureVlanSfPathMapperFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow("SFF_0", 2, (long) 0, true);
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow("SFF_1", 3, (long) 0, true);

        // Verify calls to configureNextHopFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureMacNextHopFlow("SFF_0", 0, null, "00:00:00:00:00:00");
        verify(this.flowProgrammerTestMoc, times(1)).configureMacNextHopFlow("SFF_0", 0, "00:00:00:00:00:00",
                "00:00:00:00:00:07");
        verify(this.flowProgrammerTestMoc, times(1)).configureMacNextHopFlow("SFF_1", 0, "00:00:00:00:00:04",
                "00:00:00:00:00:05");
        verify(this.flowProgrammerTestMoc, times(2)).configureMacNextHopFlow("SFF_1", 0, "00:00:00:00:00:05", null);

        // Verify calls to configureVlanTransportEgressFlow
        // the first 2 calls are SFF vlans
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanTransportEgressFlow(eq("SFF_0"),
                eq("00:00:00:00:00:04"), eq("00:00:00:00:00:07"), anyInt(), eq("1"), eq((long) 0));
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanLastHopTransportEgressFlow(eq("SFF_1"),
                eq("00:00:00:00:00:09"), eq((String) null), anyInt(), eq("1"), eq((long) 0));
        // the next 2 are SF vlans
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow("SFF_0", "00:00:00:00:00:02",
                "00:00:00:00:00:00", 2, "1", 0, false);
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow("SFF_1", "00:00:00:00:00:07",
                "00:00:00:00:00:05", 3, "1", 0, false);
        verifyNoMoreInteractions(this.flowProgrammerTestMoc);
    }

    @Test
    public void testMplsFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testMplsFlowCreation");

        RenderedServicePath mplsRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mpls.class);
        this.sfcL2RspProcessor.processRenderedServicePath(mplsRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcL2FlowWriterInterface) anyObject());

        // The calls to the VLAN methods are for the packets sent between SFF-SF

        // Verify calls to configureVlanTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanTransportIngressFlow("SFF_0");
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanTransportIngressFlow("SFF_1");

        // Verify calls to configureMplsTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureMplsTransportIngressFlow("SFF_0");
        verify(this.flowProgrammerTestMoc, times(1)).configureMplsTransportIngressFlow("SFF_1");

        // Verify calls to configureVlanPathMapperFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow(eq("SFF_0"), anyInt(), anyLong(),
                anyBoolean());
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow(eq("SFF_1"), anyInt(), anyLong(),
                anyBoolean());

        // Verify calls to configureMplsPathMapperFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureMplsPathMapperFlow(eq("SFF_0"), anyLong(), anyLong(),
                anyBoolean());
        verify(this.flowProgrammerTestMoc, times(1)).configureMplsPathMapperFlow(eq("SFF_1"), anyLong(), anyLong(),
                anyBoolean());

        // Verify calls to configureNextHopFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureMacNextHopFlow(eq("SFF_0"), anyLong(), anyString(),
                anyString());
        verify(this.flowProgrammerTestMoc, times(3)).configureMacNextHopFlow(eq("SFF_1"), anyLong(), anyString(),
                anyString());

        // Verify calls to configureVlanTransportEgressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(eq("SFF_0"), anyString(),
                anyString(), anyInt(), anyString(), anyLong(), anyBoolean());
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(eq("SFF_1"), anyString(),
                anyString(), anyInt(), anyString(), anyLong(), anyBoolean());

        // Verify calls to configureMplsTransportEgressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureMplsTransportEgressFlow(eq("SFF_0"), anyString(),
                anyString(), anyLong(), anyString(), anyLong());
        verify(this.flowProgrammerTestMoc, times(1)).configureMplsLastHopTransportEgressFlow(eq("SFF_1"), anyString(),
                anyString(), anyLong(), anyString(), anyLong());

        verifyNoMoreInteractions(this.flowProgrammerTestMoc);

    }

    @Test
    public void testNshFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testNshFlowCreation");

        RenderedServicePath nshRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, VxlanGpe.class);
        this.sfcL2RspProcessor.processRenderedServicePath(nshRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcL2FlowWriterInterface) anyObject());

        // Verify calls to configureVxlanGpeTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportIngressFlow(eq("SFF_0"), anyLong(),
                anyShort());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportIngressFlow(eq("SFF_1"), anyLong(),
                anyShort());

        // Verify calls to configureVxlanGpeNextHopFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureVxlanGpeNextHopFlow(eq("SFF_0"), anyString(), anyLong(),
                anyShort());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeNextHopFlow(eq("SFF_1"), anyString(), anyLong(),
                anyShort());

        // Verify calls to configureVxlanGpeTransportEgressFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureVxlanGpeTransportEgressFlow(eq("SFF_0"), anyLong(),
                anyShort());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeLastHopTransportEgressFlow(eq("SFF_1"), anyLong(),
                anyShort(), anyString());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportEgressFlow(eq("SFF_1"), anyLong(),
                anyShort());
        //verify(this.flowProgrammerTestMoc).configureNshNscTransportEgressFlow("SFF_1", 0, (short) 253);

        // Verify calls to configureNshNscTransportEgressFlow
        //verify(this.flowProgrammerTestMoc).configureNshNscTransportEgressFlow(eq("SFF_1"), anyLong(), anyShort());
        verify(this.flowProgrammerTestMoc).configureVxlanGpeAppCoexistTransportEgressFlow(eq("SFF_0"), eq((long) 0), anyShort());
    }

    @Test
    public void testNshOneHopFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testNshOneHopFlowCreation");

        List<SftType> sfOneHopTypes;
        sfOneHopTypes = new ArrayList<>();
        sfOneHopTypes.add(new SftType("firewall"));

        RenderedServicePath nshRsp = rspBuilder.createRspFromSfTypes(sfOneHopTypes, VxlanGpe.class);
        this.sfcL2RspProcessor.processRenderedServicePath(nshRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcL2FlowWriterInterface) anyObject());

        // Verify calls to configureVxlanGpeTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportIngressFlow(eq("SFF_0"), anyLong(),
                anyShort());

        // Verify calls to configureVxlanGpeNextHopFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeNextHopFlow("SFF_0", "192.168.0.1", 0,
                (short) 255);

        // Verify calls to configureVxlanGpeTransportEgressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeLastHopTransportEgressFlow("SFF_0", 0,
                (short) 254, "INPORT");
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportEgressFlow("SFF_0", 0, (short) 255);

        // Verify calls to configureNshNscTransportEgressFlow
        //verify(this.flowProgrammerTestMoc).configureNshNscTransportEgressFlow("SFF_0", 0, (short) 254);

        verify(this.flowProgrammerTestMoc).configureVxlanGpeAppCoexistTransportEgressFlow("SFF_0", 0, (short) 255);
    }

    @Test
    public void testVlanTcpProxyFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testVlanTcpProxyFlowCreation");

        List<SftType> sfTcpProxyTypes;
        sfTcpProxyTypes = new ArrayList<>();
        sfTcpProxyTypes.add(new SftType("tcp-proxy"));
        sfTcpProxyTypes.add(new SftType("tcp-proxy"));

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(sfTcpProxyTypes, Mac.class);
        this.sfcL2RspProcessor.processRenderedServicePath(vlanRsp);

        // TODO
        // ArgumentCaptor's can be used to get method argument values
        // ArgumentCaptor<Integer> intArg = ArgumentCaptor.forClass(Integer.class);
        // verify(mock).doSomething(intArg.capture());
        // assertEquals(100, argument.getValue());

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcL2FlowWriterInterface) anyObject());

        // Verify calls to configureVlanTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureVlanTransportIngressFlow(eq("SFF_0"));
        verify(this.flowProgrammerTestMoc, times(2)).configureVlanTransportIngressFlow(eq("SFF_1"));

        // Verify calls to configureArpTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureArpTransportIngressFlow(eq("SFF_0"), anyString());
        verify(this.flowProgrammerTestMoc, times(1)).configureArpTransportIngressFlow(eq("SFF_1"), anyString());

        // Verify calls to configureVlanPathMapperFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureVlanPathMapperFlow(eq("SFF_0"), anyInt(), anyLong(),
                anyBoolean());
        verify(this.flowProgrammerTestMoc, times(2)).configureVlanPathMapperFlow(eq("SFF_1"), anyInt(), anyLong(),
                anyBoolean());

        // Verify calls to configureNextHopFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureMacNextHopFlow(eq("SFF_0"), anyLong(), anyString(),
                anyString());
        verify(this.flowProgrammerTestMoc, times(3)).configureMacNextHopFlow(eq("SFF_1"), anyLong(), anyString(),
                anyString());

        // Verify calls to configureVlanTransportEgressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanTransportEgressFlow(eq("SFF_0"), anyString(),
                anyString(), anyInt(), anyString(), anyLong());
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanLastHopTransportEgressFlow(eq("SFF_1"), anyString(),
                anyString(), anyInt(), anyString(), anyLong());
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(eq("SFF_0"), anyString(),
                anyString(), anyInt(), anyString(), anyLong(), eq(true));
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(eq("SFF_1"), anyString(),
                anyString(), anyInt(), anyString(), anyLong(), eq(true));

        verifyNoMoreInteractions(this.flowProgrammerTestMoc);

    }

}

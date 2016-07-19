/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
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
import org.opendaylight.sfc.ofrenderer.RspBuilder;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowWriterInterface;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfProviderUtilsTestMock;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Testing for the SfcOfRspProcessor class. The SfcOfRspProcessor receives
 * a Rendered Service Path (RSP) as input, and as output creates OpenFlow flows.
 * These Unit Tests isolate the SfcOfRspProcessor class by mocking the
 * SfcOfFlowProgrammerImpl class with Mockito. Given a particular RSP, the
 * SfcOfRspProcessor should create several OpenFlow flows by calling into the
 * SfcOfFlowProgrammerInterface, whose implementation is SfcOfFlowProgrammerImpl.
 * The SfcOfFlowProgrammerImpl is mocked with Mockito, which allows us to
 * verify that exactly the correct Flow creation methods are called with the
 * expected arguments.
 *
 * @author ebrjohn
 */

public class SfcOfRspProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRspProcessorTest.class);
    SfcOfRspProcessor sfcOfRspProcessor;
    RspBuilder rspBuilder;
    SfcOfFlowProgrammerInterface flowProgrammerTestMoc;
    SfcOfProviderUtilsTestMock sfcUtilsTestMock;
    List<SftTypeName> sfTypes;

    public SfcOfRspProcessorTest() {
        LOG.info("SfcOfRspProcessorTest constructor");

        this.flowProgrammerTestMoc = mock(SfcOfFlowProgrammerImpl.class);
        this.flowProgrammerTestMoc.setFlowWriter(mock(SfcOfFlowWriterInterface.class));
        this.sfcUtilsTestMock = new SfcOfProviderUtilsTestMock();
        this.sfcOfRspProcessor = new SfcOfRspProcessor(
                this.flowProgrammerTestMoc,
                this.sfcUtilsTestMock,
                new SfcSynchronizer());
        this.rspBuilder = new RspBuilder(this.sfcUtilsTestMock);

        this.sfTypes = new ArrayList<>();
        this.sfTypes.add(new SftTypeName("firewall"));
        this.sfTypes.add(new SftTypeName("http-header-enrichment"));
    }

    @Before
    public void before() throws ExecutionException, InterruptedException {
        LOG.info("SfcOfRspProcessorTest before()");

        // this.flowProgrammerTestMoc = mock(SfcOfFlowProgrammerImpl.class);
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
        LOG.info("SfcOfRspProcessorTest testVlanFlowCreation");

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mac.class);
        this.sfcOfRspProcessor.processRenderedServicePath(vlanRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcOfFlowWriterInterface) anyObject());

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
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow(eq("SFF_0"), anyInt(), eq((long) 0), eq(false));
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanPathMapperFlow(eq("SFF_1"), anyInt(), eq((long) 0), eq(false));
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
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanTransportEgressFlow(
                eq("SFF_0"), eq("00:00:00:00:00:04"), eq("00:00:00:00:00:07"), anyInt(), eq("1"), eq((long) 0));
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanLastHopTransportEgressFlow(
                eq("SFF_1"), eq("00:00:00:00:00:09"), eq((String) null), anyInt(), eq("1"), eq((long) 0));
        // the next 2 are SF vlans
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(
                "SFF_0", "00:00:00:00:00:02", "00:00:00:00:00:00", 2, "1", 0, false);
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(
                "SFF_1", "00:00:00:00:00:07", "00:00:00:00:00:05", 3, "1", 0, false);

        // verify flow flushing
        verify(this.flowProgrammerTestMoc).flushFlows();
        verify(this.flowProgrammerTestMoc).purgeFlows();

        verifyNoMoreInteractions(this.flowProgrammerTestMoc);
    }

    @Test
    public void testMplsFlowCreation() {
        LOG.info("SfcOfRspProcessorTest testMplsFlowCreation");

        RenderedServicePath mplsRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mpls.class);
        this.sfcOfRspProcessor.processRenderedServicePath(mplsRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcOfFlowWriterInterface) anyObject());

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

        // verify flow flushing
        verify(this.flowProgrammerTestMoc).flushFlows();
        verify(this.flowProgrammerTestMoc).purgeFlows();

        verifyNoMoreInteractions(this.flowProgrammerTestMoc);

    }

    @Test
    public void testNshFlowCreation() {
        LOG.info("SfcOfRspProcessorTest testNshFlowCreation");

        RenderedServicePath nshRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, VxlanGpe.class);
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcOfFlowWriterInterface) anyObject());

        // Verify calls to configureVxlanGpeTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportIngressFlow(eq("SFF_0"), anyLong(), anyShort());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportIngressFlow(eq("SFF_1"), anyLong(), anyShort());

        // Verify calls to configureVxlanGpeNextHopFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureVxlanGpeNextHopFlow(
                eq("SFF_0"), anyString(), anyLong(), anyShort());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeNextHopFlow(
                eq("SFF_1"), anyString(), anyLong(), anyShort());

        // Verify calls to configureVxlanGpeTransportEgressFlow
        verify(this.flowProgrammerTestMoc, times(2)).configureVxlanGpeTransportEgressFlow(
                eq("SFF_0"), anyLong(), anyShort(), anyString());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeLastHopTransportEgressFlow(
                eq("SFF_1"), anyLong(), anyShort(), anyString());
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportEgressFlow(
                eq("SFF_1"), anyLong(), anyShort(), anyString());
        verify(this.flowProgrammerTestMoc).configureNshNscTransportEgressFlow(
                "SFF_1", 0, (short) 253, "INPORT");

        // Verify calls to configureNshNscTransportEgressFlow
        verify(this.flowProgrammerTestMoc).configureNshNscTransportEgressFlow(
                eq("SFF_1"), anyLong(), anyShort(), anyString());
        verify(this.flowProgrammerTestMoc).configureVxlanGpeAppCoexistTransportEgressFlow(
                eq("SFF_1"), anyLong(), anyShort(), anyString());

        // verify flow flushing
        verify(this.flowProgrammerTestMoc).flushFlows();
        verify(this.flowProgrammerTestMoc).purgeFlows();

        verifyNoMoreInteractions(this.flowProgrammerTestMoc);
    }

    @Test
    public void testNshOneHopFlowCreation() {
        LOG.info("SfcOfRspProcessorTest testNshOneHopFlowCreation");

        List<SftTypeName> sfOneHopTypes;
        sfOneHopTypes = new ArrayList<>();
        sfOneHopTypes.add(new SftTypeName("firewall"));

        RenderedServicePath nshRsp = rspBuilder.createRspFromSfTypes(sfOneHopTypes, VxlanGpe.class);
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);

        assertMatchAnyMethodsCalled("SFF_0");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcOfFlowWriterInterface) anyObject());

        // Verify calls to configureVxlanGpeTransportIngressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportIngressFlow(eq("SFF_0"), anyLong(), anyShort());

        // Verify calls to configureVxlanGpeNextHopFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeNextHopFlow(
                "SFF_0", "192.168.0.1", 0, (short) 255);

        // Verify calls to configureVxlanGpeTransportEgressFlow
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeLastHopTransportEgressFlow(
                "SFF_0", 0, (short) 254, "INPORT");
        verify(this.flowProgrammerTestMoc, times(1)).configureVxlanGpeTransportEgressFlow(
                "SFF_0", 0, (short) 255, "INPORT");

        // Verify calls to configureNshNscTransportEgressFlow
        verify(this.flowProgrammerTestMoc).configureNshNscTransportEgressFlow(
                "SFF_0", 0, (short) 254, "INPORT");

        verify(this.flowProgrammerTestMoc).configureVxlanGpeAppCoexistTransportEgressFlow(
                "SFF_0", 0, (short) 254, "192.168.0.2");

        // verify flow flushing
        verify(this.flowProgrammerTestMoc).flushFlows();
        verify(this.flowProgrammerTestMoc).purgeFlows();

        verifyNoMoreInteractions(this.flowProgrammerTestMoc);
    }

    @Test
    public void testVlanTcpProxyFlowCreation() {
        LOG.info("SfcOfRspProcessorTest testVlanTcpProxyFlowCreation");

        List<SftTypeName> sfTcpProxyTypes;
        sfTcpProxyTypes = new ArrayList<>();
        sfTcpProxyTypes.add(new SftTypeName("tcp-proxy"));
        sfTcpProxyTypes.add(new SftTypeName("tcp-proxy"));

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(sfTcpProxyTypes, Mac.class);
        this.sfcOfRspProcessor.processRenderedServicePath(vlanRsp);

        // TODO
        // ArgumentCaptor's can be used to get method argument values
        // ArgumentCaptor<Integer> intArg = ArgumentCaptor.forClass(Integer.class);
        // verify(mock).doSomething(intArg.capture());
        // assertEquals(100, argument.getValue());

        assertMatchAnyMethodsCalled("SFF_0");
        assertMatchAnyMethodsCalled("SFF_1");
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowRspId(anyLong());
        verify(this.flowProgrammerTestMoc, atLeastOnce()).setFlowWriter((SfcOfFlowWriterInterface) anyObject());

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
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanTransportEgressFlow(
                eq("SFF_0"), anyString(), anyString(), anyInt(), anyString(), anyLong());
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanLastHopTransportEgressFlow(
                eq("SFF_1"), anyString(), anyString(), anyInt(), anyString(), anyLong());
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(
                eq("SFF_0"), anyString(), anyString(), anyInt(), anyString(), anyLong(), eq(true));
        verify(this.flowProgrammerTestMoc, times(1)).configureVlanSfTransportEgressFlow(
                eq("SFF_1"), anyString(), anyString(), anyInt(), anyString(), anyLong(), eq(true));

        // verify flow flushing
        verify(this.flowProgrammerTestMoc).flushFlows();
        verify(this.flowProgrammerTestMoc).purgeFlows();

        verifyNoMoreInteractions(this.flowProgrammerTestMoc);

    }

}

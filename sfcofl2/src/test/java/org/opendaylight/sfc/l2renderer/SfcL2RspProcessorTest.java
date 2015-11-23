/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// public class SfcL2RspProcessorTest extends AbstractDataBrokerTest {
public class SfcL2RspProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RspProcessorTest.class);
    SfcL2RspProcessor sfcL2RspProcessor;
    RspBuilder rspBuilder;
    SfcL2FlowProgrammerTestMoc flowProgrammerTestMoc;
    SfcL2ProviderUtilsTestMock sfcUtilsTestMock;
    List<SftType> sfTypes;

    public SfcL2RspProcessorTest() {
        LOG.info("SfcL2RspProcessorTest constructor");

        this.flowProgrammerTestMoc = new SfcL2FlowProgrammerTestMoc();
        this.sfcUtilsTestMock = new SfcL2ProviderUtilsTestMock();
        this.sfcL2RspProcessor = new SfcL2RspProcessor(this.flowProgrammerTestMoc, this.sfcUtilsTestMock);
        this.rspBuilder = new RspBuilder(this.sfcUtilsTestMock);

        this.sfTypes = new ArrayList<SftType>();
        this.sfTypes.add(new SftType("firewall"));
        this.sfTypes.add(new SftType("http-header-enrichment"));
    }

    private void assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces methodIndex, int count) {
        assertEquals(count, flowProgrammerTestMoc.getMethodCalledCount(methodIndex));
    }

    private void assertMatchAnyMethodsCalled() {
        // Default values
        assertMatchAnyMethodsCalled(2, 2, 2, 2, 2);
    }

    private void assertMatchAnyMethodsCalled(int transportIngressCount, int pathMapperCount, int pathMapperAclCount,
            int nextHopCount, int transportEgressCount) {
        // Each of these is called once per SFF, and there are 2 SFFs
        assertMethodCallCount(
                SfcL2FlowProgrammerTestMoc.MethodIndeces.configureTransportIngressTableMatchAnyMethodIndex,
                transportIngressCount);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configurePathMapperTableMatchAnyMethodIndex,
                pathMapperCount);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configurePathMapperAclTableMatchAnyMethodIndex,
                pathMapperAclCount);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureNextHopTableMatchAnyMethodIndex,
                nextHopCount);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureTransportEgressTableMatchAnyMethodIndex,
                transportEgressCount);
    }

    @Before
    public void before() throws ExecutionException, InterruptedException {
        LOG.info("SfcL2RspProcessorTest before()");

        // Reset the methods called before each test
        flowProgrammerTestMoc.resetCalledMethods();
        sfcUtilsTestMock.resetCache();
    }

    // TODO tests to add:
    // - An SFF with > 1 SF
    // - An SF of type TCP Proxy and PktIn

    @Test
    public void testVlanFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testVlanFlowCreation");

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mac.class);
        this.sfcL2RspProcessor.processRenderedServicePath(vlanRsp);

        assertMatchAnyMethodsCalled();
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanTransportIngressFlowMethodIndex, 3);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanPathMapperFlowMethodIndex, 5);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureNextHopFlowMethodIndex, 4);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanTransportEgressFlowMethodIndex, 4);
        // TODO need to check that all the rest are 0
    }

    @Test
    public void testMplsFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testMplsFlowCreation");

        RenderedServicePath mplsRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mpls.class);
        this.sfcL2RspProcessor.processRenderedServicePath(mplsRsp);

        assertMatchAnyMethodsCalled();
        // The calls to teh VLAN methods are for the packets sent between SFF-SF
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanTransportIngressFlowMethodIndex, 2);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureMplsTransportIngressFlowMethodIndex, 1);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanPathMapperFlowMethodIndex, 2);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureMplsPathMapperFlowMethodIndex, 3);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureNextHopFlowMethodIndex, 4);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanTransportEgressFlowMethodIndex, 2);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureMplsTransportEgressFlowMethodIndex, 2);
    }

    @Test
    public void testNshFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testNshFlowCreation");

        RenderedServicePath nshRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, VxlanGpe.class);
        this.sfcL2RspProcessor.processRenderedServicePath(nshRsp);

        assertMatchAnyMethodsCalled();
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpeTransportIngressFlowMethodIndex,
                3);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpePathMapperFlowMethodIndex, 0);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpeNextHopFlowMethodIndex, 3);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpeTransportEgressFlowMethodIndex,
                4);
    }

    @Test
    public void testNshOneHopFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testNshOneHopFlowCreation");

        List<SftType> sfOneHopTypes;
        sfOneHopTypes = new ArrayList<SftType>();
        sfOneHopTypes.add(new SftType("firewall"));

        RenderedServicePath nshRsp = rspBuilder.createRspFromSfTypes(sfOneHopTypes, VxlanGpe.class);
        this.sfcL2RspProcessor.processRenderedServicePath(nshRsp);

        assertMatchAnyMethodsCalled(1, 1, 1, 1, 1);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpeTransportIngressFlowMethodIndex,
                2);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpePathMapperFlowMethodIndex, 0);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpeNextHopFlowMethodIndex, 1);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVxlanGpeTransportEgressFlowMethodIndex,
                2);
    }

    @Test
    public void testVlanTcpProxyFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testVlanTcpProxyFlowCreation");

        List<SftType> sfTcpProxyTypes;
        sfTcpProxyTypes = new ArrayList<SftType>();
        sfTcpProxyTypes.add(new SftType("tcp-proxy"));
        sfTcpProxyTypes.add(new SftType("tcp-proxy"));

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(sfTcpProxyTypes, Mac.class);
        this.sfcL2RspProcessor.processRenderedServicePath(vlanRsp);

        assertMatchAnyMethodsCalled();
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanTransportIngressFlowMethodIndex, 3);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureArpTransportIngressFlowMethodIndex, 2);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanPathMapperFlowMethodIndex, 5);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureNextHopFlowMethodIndex, 4);
        assertMethodCallCount(SfcL2FlowProgrammerTestMoc.MethodIndeces.configureVlanTransportEgressFlowMethodIndex, 4);
        // TODO need to check that all the rest are 0
    }

}

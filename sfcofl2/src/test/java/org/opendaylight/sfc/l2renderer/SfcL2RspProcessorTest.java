package org.opendaylight.sfc.l2renderer;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.HttpHeaderEnrichment;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;

public class SfcL2RspProcessorTest extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RspProcessorTest.class);
    DataBroker dataBroker;
    ExecutorService executor;
    OpendaylightSfc opendaylightSfc;

    SfcL2RspProcessor sfcL2RspProcessor;
    RspBuilder rspBuilder;
    SfcL2FlowProgrammerTestMoc flowProgrammerTestMoc;
    List<Class<? extends ServiceFunctionTypeIdentity>> sfTypes;

    public SfcL2RspProcessorTest() {
        LOG.info("SfcL2RspProcessorTest constructor");

        opendaylightSfc = new OpendaylightSfc();
        opendaylightSfc.setDataProvider(getDataBroker());
        executor = opendaylightSfc.getExecutor();

        this.flowProgrammerTestMoc = new SfcL2FlowProgrammerTestMoc();
        this.sfcL2RspProcessor = new SfcL2RspProcessor(this.flowProgrammerTestMoc);
        this.rspBuilder = new RspBuilder();

        this.sfTypes = new ArrayList<Class<? extends ServiceFunctionTypeIdentity>>();
        this.sfTypes.add(Firewall.class);
        this.sfTypes.add(HttpHeaderEnrichment.class);
    }

    private void assertMatchAnyMethodsCalled() {
        assertTrue(this.flowProgrammerTestMoc.configureTransportIngressTableMatchAnyCalled);
        assertTrue(this.flowProgrammerTestMoc.configureIngressTableMatchAnyCalled);
        assertTrue(this.flowProgrammerTestMoc.configureNextHopTableMatchAnyCalled);
        assertTrue(this.flowProgrammerTestMoc.configureTransportEgressTableMatchAnyCalled);
    }

    @Before
    public void before() throws ExecutionException, InterruptedException {
        LOG.info("SfcL2RspProcessorTest flowProgrammerTestMoc.resetCalledMethods");

        // Reset the methods called before each test
        flowProgrammerTestMoc.resetCalledMethods();
    }

    @Test
    public void testMplsFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testMplsFlowCreation");

        RenderedServicePath mplsRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mpls.class);
        this.sfcL2RspProcessor.processRenderedServicePath(mplsRsp, true);

        assertMatchAnyMethodsCalled();
        assertTrue(this.flowProgrammerTestMoc.configureMplsTransportIngressFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureMplsIngressFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureNextHopFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureMplsTransportEgressFlowCalled);
    }

    @Test
    public void testVlanFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testMplsFlowCreation");

        RenderedServicePath vlanRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, Mac.class);
        this.sfcL2RspProcessor.processRenderedServicePath(vlanRsp, true);

        assertMatchAnyMethodsCalled();
        assertTrue(this.flowProgrammerTestMoc.configureVlanTransportIngressFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureVlanIngressFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureNextHopFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureVlanTransportEgressFlowCalled);
    }

    @Test
    public void testNshFlowCreation() {
        LOG.info("SfcL2RspProcessorTest testMplsFlowCreation");

        RenderedServicePath nshRsp = rspBuilder.createRspFromSfTypes(this.sfTypes, VxlanGpe.class);
        this.sfcL2RspProcessor.processRenderedServicePath(nshRsp, true);

        assertMatchAnyMethodsCalled();
        assertTrue(this.flowProgrammerTestMoc.configureVxlanGpeTransportIngressFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureVxlanGpeIngressFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureVxlanGpeNextHopFlowCalled);
        assertTrue(this.flowProgrammerTestMoc.configureVxlanGpeTransportEgressFlowCalled);
    }
}

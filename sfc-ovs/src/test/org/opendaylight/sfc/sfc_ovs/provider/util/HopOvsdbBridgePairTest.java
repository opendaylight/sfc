package org.opendaylight.sfc.sfc_ovs.provider.util;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see HopOvsdbBridgePair
 * <p/>
 * @since 2015-04-30
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOvsUtil.class)
public class HopOvsdbBridgePairTest {
    private static final String testString = "test string";
    private RenderedServicePathHopBuilder renderedServicePathHopBuilder;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;

    @Test
    public void constructorTest() throws Exception {
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(testString));
        HopOvsdbBridgePair hopOvsdbBridgePair = new HopOvsdbBridgePair(renderedServicePathHopBuilder.build(), ovsdbBridgeAugmentationBuilder.build());

        // constructor test
        Assert.assertEquals(hopOvsdbBridgePair.ovsdbBridgeAugmentation.getBridgeName().getValue(), "test string");
    }

    @Test
    public void buildHopOvsdbBridgePairReturnsEmptyList0() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();

        renderedServicePathHopBuilder.setHopNumber(Short.valueOf("5"));
        renderedServicePathHopBuilder.setServiceFunctionForwarder("Sff");
        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        PowerMockito.mockStatic(SfcOvsUtil.class);
        Mockito.when(SfcOvsUtil.submitCallable(Mockito.any(SfcOvsDataStoreAPI.class), Mockito.any(ExecutorService.class))).thenReturn(null);

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList = Whitebox.invokeMethod(HopOvsdbBridgePair.class, "buildHopOvsdbBridgePairList", renderedServicePathBuilder.build(), executorService);

        //buildHopOvsdbBridgePair test
        Assert.assertEquals(hopOvsdbBridgePairList.size(), 0);
    }

    @Test
    public void buildHopOvsdbBridgePairReturnsEmptyList1() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();

        renderedServicePathHopBuilder.setHopNumber(Short.valueOf("5"));
        renderedServicePathHopBuilder.setServiceFunctionForwarder("Sff");
        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        PowerMockito.mockStatic(SfcOvsUtil.class);
        Mockito.when(SfcOvsUtil.submitCallable(Mockito.any(SfcOvsDataStoreAPI.class), Mockito.any(ExecutorService.class))).thenReturn(ovsdbBridgeAugmentationBuilder.build());

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList = Whitebox.invokeMethod(HopOvsdbBridgePair.class, "buildHopOvsdbBridgePairList", renderedServicePathBuilder.build(), executorService);

        //buildHopOvsdbBridgePair test
        Assert.assertEquals(hopOvsdbBridgePairList.size(), 0);
    }

    @Test
    public void buildHopOvsdbBridgePairTest() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();

        renderedServicePathHopBuilder.setHopNumber(Short.valueOf("0"));
        renderedServicePathHopBuilder.setServiceFunctionForwarder("Sff");
        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        PowerMockito.mockStatic(SfcOvsUtil.class);
        Mockito.when(SfcOvsUtil.submitCallable(Mockito.any(SfcOvsDataStoreAPI.class), Mockito.any(ExecutorService.class))).thenReturn(ovsdbBridgeAugmentationBuilder.build());

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList = Whitebox.invokeMethod(HopOvsdbBridgePair.class, "buildHopOvsdbBridgePairList", renderedServicePathBuilder.build(), executorService);

        //buildHopOvsdbBridgePair test
        Assert.assertEquals(hopOvsdbBridgePairList.size(), 1);
    }
}




















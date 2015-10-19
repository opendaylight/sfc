/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.util;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see HopOvsdbBridgePair
 * @since 2015-04-30
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOvsUtil.class)
public class HopOvsdbBridgePairTest {

    private static final SffName sff = new SffName("sff");
    private RenderedServicePathHopBuilder renderedServicePathHopBuilder;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;

    @Test
    public void constructorTest() throws Exception {
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        HopOvsdbBridgePair hopOvsdbBridgePair =
                new HopOvsdbBridgePair(renderedServicePathHopBuilder.build(), ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must be not null", hopOvsdbBridgePair.ovsdbBridgeAugmentation);
        assertNotNull("Must be not null", hopOvsdbBridgePair.renderedServicePathHop);
    }

    @Test
    public void buildHopOvsdbBridgePair_EmptyList0() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();

        renderedServicePathHopBuilder.setHopNumber(Short.valueOf("5")).setServiceFunctionForwarder(sff);
        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable")).toReturn(null);

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList =
                HopOvsdbBridgePair.buildHopOvsdbBridgePairList(renderedServicePathBuilder.build(), executorService);

        assertEquals("Must be Equal", hopOvsdbBridgePairList, Collections.emptyList());
    }

    @Test
    public void buildHopOvsdbBridgePair_EmptyList1() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();

        renderedServicePathHopBuilder.setHopNumber(Short.valueOf("5")).setServiceFunctionForwarder(sff);
        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable"))
            .toReturn(ovsdbBridgeAugmentationBuilder.build());

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList =
                HopOvsdbBridgePair.buildHopOvsdbBridgePairList(renderedServicePathBuilder.build(), executorService);

        assertEquals("Must be equal", hopOvsdbBridgePairList, Collections.emptyList());
    }

    @Test
    public void buildHopOvsdbBridgePairTest() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();

        renderedServicePathHopBuilder.setHopNumber(Short.valueOf("0")).setServiceFunctionForwarder(sff);
        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable"))
            .toReturn(ovsdbBridgeAugmentationBuilder.build());

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList =
                HopOvsdbBridgePair.buildHopOvsdbBridgePairList(renderedServicePathBuilder.build(), executorService);

        assertEquals(hopOvsdbBridgePairList.size(), 1);
    }
}

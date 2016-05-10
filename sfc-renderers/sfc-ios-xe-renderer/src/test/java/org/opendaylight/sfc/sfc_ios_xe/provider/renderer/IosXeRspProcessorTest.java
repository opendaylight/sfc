/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.renderer;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ios_xe.provider.api.IosXeDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.ConfigServiceChainPathMode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.sfc.sfc_ios_xe.provider.api.IosXeDataStoreAPI.Transaction.READ_PATH;
import static org.opendaylight.sfc.sfc_ios_xe.provider.api.IosXeDataStoreAPI.Transaction.WRITE_FUNCTION;

public class IosXeRspProcessorTest extends AbstractDataBrokerTest {

    private final OpendaylightSfc odl = new OpendaylightSfc();
    private final String forwarderName = "forwarder";
    private final String firstFunctionName = "firstFunction";
    private final String secondFunctionName = "secondFunction";
    private final String thirdFunctionName = "thirdFunction";
    private final String mgmtIp = "10.0.0.1";
    private DataBroker dataBroker;
    private NodeManager nodeManager;

    @Before
    public void init() {
        dataBroker = getDataBroker();
        odl.setDataProvider(dataBroker);
        nodeManager = mock(NodeManager.class);
        prepareSfcEntities();
    }

    @Test
    public void updateRsp() {
        when(nodeManager.getMountpointFromIpAddress(new IpAddress(new Ipv4Address(mgmtIp)))).thenReturn(dataBroker);

        IosXeRspProcessor processor = new IosXeRspProcessor(dataBroker, nodeManager);
        processor.updateRsp(createTestRenderedServicePath());

        verify(nodeManager, times(1)).getMountpointFromIpAddress(new IpAddress(new Ipv4Address(mgmtIp)));

        // Read and test created service path
        ServicePath servicePath = (ServicePath) new IosXeDataStoreAPI(dataBroker, new ServicePathKey(10L), READ_PATH,
                LogicalDatastoreType.CONFIGURATION).call();
        assertNotNull(servicePath);
        ConfigServiceChainPathMode chainPathMode = servicePath.getConfigServiceChainPathMode();
        assertTrue(chainPathMode.getServiceIndex().getServices().size() == 4);
    }

    private RenderedServicePath createTestRenderedServicePath() {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        // Prepare hops
        List<RenderedServicePathHop> hops = new ArrayList<>();
        RenderedServicePathHopBuilder firstHop = new RenderedServicePathHopBuilder();
        firstHop.setServiceFunctionForwarder(new SffName(forwarderName))
                .setServiceFunctionName(new SfName(firstFunctionName));
        RenderedServicePathHopBuilder secondHop = new RenderedServicePathHopBuilder();
        secondHop.setServiceFunctionForwarder(new SffName(forwarderName))
                .setServiceFunctionName(new SfName(secondFunctionName));
        RenderedServicePathHopBuilder thirdHop = new RenderedServicePathHopBuilder();
        thirdHop.setServiceFunctionForwarder(new SffName(forwarderName))
                .setServiceFunctionName(new SfName(thirdFunctionName));
        hops.add(firstHop.build());
        hops.add(secondHop.build());
        hops.add(thirdHop.build());
        renderedServicePathBuilder.setName(new RspName("testRsp"))
                .setKey(new RenderedServicePathKey(new RspName("testRsp")))
                .setPathId(10L)
                .setStartingIndex((short) 255)
                .setRenderedServicePathHop(hops);
        return renderedServicePathBuilder.build();
    }

    private void prepareSfcEntities() {
        // First SFF
        ServiceFunctionForwarderBuilder serviceForwarderBuilder = new ServiceFunctionForwarderBuilder();
        List<SffDataPlaneLocator> sffDataPlaneLocators = new ArrayList<>();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        String dplIp = "100.0.0.1";
        dataPlaneLocatorBuilder.setLocatorType(new IpBuilder().setIp(new IpAddress(new Ipv4Address(dplIp))).build());
        String sffDpl = "sffDpl";
        sffDataPlaneLocatorBuilder.setName(new SffDataPlaneLocatorName(sffDpl))
                .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName(sffDpl)))
                .setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        sffDataPlaneLocators.add(sffDataPlaneLocatorBuilder.build());
        serviceForwarderBuilder.setName(new SffName(forwarderName))
                .setKey(new ServiceFunctionForwarderKey(new SffName(forwarderName)))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(mgmtIp)))
                .setSffDataPlaneLocator(sffDataPlaneLocators);

        // First SF
        ServiceFunctionBuilder firstServiceFunctionBuilder = new ServiceFunctionBuilder();
        firstServiceFunctionBuilder.setName(firstFunctionName)
                .setKey(new ServiceFunctionKey(firstFunctionName));
        // Second SF
        ServiceFunctionBuilder secondServiceFunctionBuilder = new ServiceFunctionBuilder();
        secondServiceFunctionBuilder.setName(secondFunctionName)
                .setKey(new ServiceFunctionKey(secondFunctionName));
        // Third SF
        ServiceFunctionBuilder thirdServiceFunctionBuilder = new ServiceFunctionBuilder();
        thirdServiceFunctionBuilder.setName(thirdFunctionName)
                .setKey(new ServiceFunctionKey(thirdFunctionName));

        SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(serviceForwarderBuilder.build());

        new IosXeDataStoreAPI(dataBroker, firstServiceFunctionBuilder.build(), WRITE_FUNCTION, OPERATIONAL).call();
        new IosXeDataStoreAPI(dataBroker, secondServiceFunctionBuilder.build(), WRITE_FUNCTION, OPERATIONAL).call();
        new IosXeDataStoreAPI(dataBroker, thirdServiceFunctionBuilder.build(), WRITE_FUNCTION, OPERATIONAL).call();
    }
}
/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class SfcServiceFuntionSchedulerAPITest extends AbstractDataBrokerTest {

    DataBroker dataBroker;
    ExecutorService executor;
    OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    List<SfDataPlaneLocator> sfDPLList = new ArrayList<>();
    List<ServiceFunction> sfList = new ArrayList<>();
    ServiceFunctionChain sfChain;

    @Before
    public void before() throws ExecutionException, InterruptedException {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

        String sfcName = "unittest-sched-chain-1";
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder()
                                           .setName("firewall")
                                           .setKey(new SfcServiceFunctionKey("firewall"))
                                           .setType(Firewall.class)
                                           .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder()
                                           .setName("dpi")
                                           .setKey(new SfcServiceFunctionKey("dpi"))
                                           .setType(Dpi.class)
                                           .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder()
                                           .setName("nat")
                                           .setKey(new SfcServiceFunctionKey("nat"))
                                           .setType(Napt44.class)
                                           .build());

        sfChain = new ServiceFunctionChainBuilder()
                .setName(sfcName)
                .setKey(new ServiceFunctionChainKey(sfcName))
                .setSfcServiceFunction(sfcServiceFunctionList)
                .build();

        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("moscow-5.5.5.5:555-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.5")), 555),
                "sff-moscow", VxlanGpe.class));
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("newyork-6.6.6.6:666-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("6.6.6.6")), 666),
                "sff-newyork", VxlanGpe.class));
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("paris-7.7.7.7:777-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("7.7.7.7")), 777),
                "sff-newyork", VxlanGpe.class));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_100", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.101")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_110", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.110.101")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_120", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.120.101")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_100", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.100.102")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_110", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.110.102")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_120", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.120.102")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_100", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.100.103")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_110", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.110.103")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_120", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.120.103")), sfDPLList.get(2), Boolean.FALSE));

    }

    @After
    public void after() {
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
    }

    @Test
    public void testBasicEnvSetup() throws ExecutionException, InterruptedException {

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();
        for (ServiceFunction serviceFuntion : sfList) {
            Object[] parameters2 = {serviceFuntion.getName()};
            Class[] parameterTypes2 = {String.class};
            Object result = executor.submit(SfcProviderServiceFunctionAPI
                    .getRead(parameters2, parameterTypes2)).get();
            ServiceFunction sf2 = (ServiceFunction) result;

            assertNotNull("Must be not null", sf2);
            assertEquals("Must be equal", sf2.getName(), serviceFuntion.getName());
            assertEquals("Must be equal", sf2.getType(), serviceFuntion.getType());
        }

        Object[] sfcParameters = {sfChain};
        Class[] sfcParameterTypes = {ServiceFunctionChain.class};
        executor.submit(SfcProviderServiceChainAPI
                .getPut(sfcParameters, sfcParameterTypes)).get();

        Object[] parameters2 = {sfChain.getName()};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfChain.getSfcServiceFunction());

        for (ServiceFunction serviceFunction: sfList) {
            SfcProviderServiceTypeAPI.createServiceFunctionTypeEntryExecutor(serviceFunction);
        }

        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Dpi.class);
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Napt44.class);
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);
    }

    @Test
    public void testServiceFunctionRandomScheduler() throws ExecutionException, InterruptedException {

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();

        Object[] sfcParameters = {sfChain};
        Class[] sfcParameterTypes = {ServiceFunctionChain.class};
        executor.submit(SfcProviderServiceChainAPI
                .getPut(sfcParameters, sfcParameterTypes)).get();

        for (ServiceFunction serviceFunction: sfList) {
            SfcProviderServiceTypeAPI.createServiceFunctionTypeEntryExecutor(serviceFunction);
        }

        int serviceIndex = 255;
        SfcServiceFunctionSchedulerAPI scheduler = new SfcServiceFunctionRandomSchedulerAPI();
        List<RenderedServicePathHop> renderedServicePathHopArrayList =
                scheduler.scheduleServiceFuntions(sfChain, serviceIndex);

        assertNotNull("Must be not null", renderedServicePathHopArrayList);

        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI
                                    .readServiceFunctionExecutor(renderedServicePathHopArrayList.get(0).getServiceFunctionName());
        assertEquals("Must be equal", serviceFunction.getType(), Firewall.class);
        assertNotEquals("Must be not equal", serviceFunction.getType(), Dpi.class);

        serviceFunction = SfcProviderServiceFunctionAPI
                                .readServiceFunctionExecutor(renderedServicePathHopArrayList.get(1).getServiceFunctionName());
        assertEquals("Must be equal", serviceFunction.getType(), Dpi.class);
        assertNotEquals("Must be not equal", serviceFunction.getType(), Napt44.class);

        serviceFunction = SfcProviderServiceFunctionAPI
                                .readServiceFunctionExecutor(renderedServicePathHopArrayList.get(2).getServiceFunctionName());
        assertEquals("Must be equal", serviceFunction.getType(), Napt44.class);
        assertNotEquals("Must be not equal", serviceFunction.getType(), Firewall.class);
    }

    @Test
    public void testServiceFunctionRoundRobinScheduler() throws ExecutionException, InterruptedException {

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();

        Object[] sfcParameters = {sfChain};
        Class[] sfcParameterTypes = {ServiceFunctionChain.class};
        executor.submit(SfcProviderServiceChainAPI
                .getPut(sfcParameters, sfcParameterTypes)).get();

        for (ServiceFunction serviceFunction: sfList) {
            SfcProviderServiceTypeAPI.createServiceFunctionTypeEntryExecutor(serviceFunction);
        }

        int serviceIndex = 255;
        SfcServiceFunctionSchedulerAPI scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();
        List<RenderedServicePathHop> renderedServicePathHopArrayList;

        ServiceFunctionType serviceFunctionType;
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
        List<SftServiceFunctionName> sftFirewallList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Dpi.class);
        List<SftServiceFunctionName> sftDpiList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Napt44.class);
        List<SftServiceFunctionName> sftNapt44List = serviceFunctionType.getSftServiceFunctionName();

        /* First round */
        renderedServicePathHopArrayList = scheduler.scheduleServiceFuntions(sfChain, serviceIndex);
        assertNotNull("Must be not null", renderedServicePathHopArrayList);
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(0).getServiceFunctionName(), sftFirewallList.get(0).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(1).getServiceFunctionName(), sftDpiList.get(0).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(2).getServiceFunctionName(), sftNapt44List.get(0).getName());

        renderedServicePathHopArrayList = scheduler.scheduleServiceFuntions(sfChain, serviceIndex);
        assertNotNull("Must be not null", renderedServicePathHopArrayList);
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(0).getServiceFunctionName(), sftFirewallList.get(1).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(1).getServiceFunctionName(), sftDpiList.get(1).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(2).getServiceFunctionName(), sftNapt44List.get(1).getName());

        renderedServicePathHopArrayList = scheduler.scheduleServiceFuntions(sfChain, serviceIndex);
        assertNotNull("Must be not null", renderedServicePathHopArrayList);
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(0).getServiceFunctionName(), sftFirewallList.get(2).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(1).getServiceFunctionName(), sftDpiList.get(2).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(2).getServiceFunctionName(), sftNapt44List.get(2).getName());

        /* Second round */
        renderedServicePathHopArrayList = scheduler.scheduleServiceFuntions(sfChain, serviceIndex);
        assertNotNull("Must be not null", renderedServicePathHopArrayList);
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(0).getServiceFunctionName(), sftFirewallList.get(0).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(1).getServiceFunctionName(), sftDpiList.get(0).getName());
        assertEquals("Must be equal", renderedServicePathHopArrayList.get(2).getServiceFunctionName(), sftNapt44List.get(0).getName());
    }
}

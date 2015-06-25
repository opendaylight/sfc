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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

public class SfcServiceFunctionSchedulerAPITest extends AbstractDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionSchedulerAPITest.class);
    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private final List<SfDataPlaneLocator> sfDPLList = new ArrayList<>();
    private final List<ServiceFunction> sfList = new ArrayList<>();
    private ExecutorService executor;
    private ServiceFunctionChain sfChain;
    private ServiceFunctionPath sfPath;

    @Before
    public void before() throws ExecutionException, InterruptedException {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

        /* Delete all the content in SFC data store before unit test */
        int maxTries = 10;
        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;
        boolean emptyFlag = true;
        while (maxTries > 0) {
            emptyFlag = true;
            executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            Thread.sleep(1000); //Wait for real delete

            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
            if (serviceFunctionType != null) {
                sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (sftServiceFunctionNameList.size() != 0) {
                    emptyFlag = false;
                }
            }

            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Dpi.class);
            if (serviceFunctionType != null) {
                sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (sftServiceFunctionNameList.size() != 0) {
                    emptyFlag = false;
                }
            }

            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Napt44.class);
            if (serviceFunctionType != null) {
                sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (sftServiceFunctionNameList.size() != 0) {
                    emptyFlag = false;
                }
            }

            maxTries--;
            if (emptyFlag) {
                break;
            }
        }
        LOG.debug("Empty SFC data store {} times: {}", 10 - maxTries, emptyFlag ? "Successful" : "Failed");
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

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey("key"));
        serviceFunctionPathBuilder.setPathId(1l);
        serviceFunctionPathBuilder.setServiceChainName(sfcName);
        List<ServicePathHop> sphs = new ArrayList<>();
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        sfPath = serviceFunctionPathBuilder.build();

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

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();
        Thread.sleep(1000); //Wait they are really created

        for (ServiceFunction serviceFunction : sfList) {
            SfcProviderServiceTypeAPI.createServiceFunctionTypeEntryExecutor(serviceFunction);
        }

        Object[] sfcParameters = {sfChain};
        Class[] sfcParameterTypes = {ServiceFunctionChain.class};
        executor.submit(SfcProviderServiceChainAPI
                .getPut(sfcParameters, sfcParameterTypes)).get();
        Thread.sleep(1000); //Wait it is really created
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
        int maxTries;
        for (ServiceFunction serviceFunction : sfList) {
            maxTries = 10;
            ServiceFunction sf2 = null;
            while (maxTries > 0) {
                Object[] parameters2 = {serviceFunction.getName()};
                Class[] parameterTypes2 = {String.class};
                Object result = executor.submit(SfcProviderServiceFunctionAPI
                        .getRead(parameters2, parameterTypes2)).get();
                sf2 = (ServiceFunction) result;
                maxTries--;
                if (sf2 != null) {
                    break;
                }
                Thread.sleep(1000);
            }
            LOG.debug("SfcServiceFunctionSchedulerAPITest: getRead ServiceFunction {} {} times: {}", serviceFunction.getName(), 10 - maxTries, (sf2 != null) ? "Successful" : "Failed");
            assertNotNull("Must be not null", sf2);
            assertEquals("Must be equal", sf2.getName(), serviceFunction.getName());
            assertEquals("Must be equal", sf2.getType(), serviceFunction.getType());
        }

        Object[] parameters2 = {sfChain.getName()};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfChain.getSfcServiceFunction());

        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
            LOG.debug("sftServiceFunctionName: {}", sftServiceFunctionName.getName());
        }
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
        int serviceIndex = 255;
        SfcServiceFunctionSchedulerAPI scheduler = new SfcServiceFunctionRandomSchedulerAPI();
        List<String> serviceFunctionNameArrayList =
                scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);

        assertNotNull("Must be not null", serviceFunctionNameArrayList);

        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(serviceFunctionNameArrayList.get(0));
        assertNotNull("Must be not null", serviceFunction);
        assertEquals("Must be equal", serviceFunction.getType(), Firewall.class);
        assertNotEquals("Must be not equal", serviceFunction.getType(), Dpi.class);

        serviceFunction = SfcProviderServiceFunctionAPI
                .readServiceFunctionExecutor(serviceFunctionNameArrayList.get(1));
        assertEquals("Must be equal", serviceFunction.getType(), Dpi.class);
        assertNotEquals("Must be not equal", serviceFunction.getType(), Napt44.class);

        serviceFunction = SfcProviderServiceFunctionAPI
                .readServiceFunctionExecutor(serviceFunctionNameArrayList.get(2));
        assertEquals("Must be equal", serviceFunction.getType(), Napt44.class);
        assertNotEquals("Must be not equal", serviceFunction.getType(), Firewall.class);
    }

    @Test
    public void testServiceFunctionRoundRobinScheduler() throws ExecutionException, InterruptedException {
        int serviceIndex = 255;
        SfcServiceFunctionSchedulerAPI scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();
        List<String> serviceFunctionNameArrayList;

        ServiceFunctionType serviceFunctionType;
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
        List<SftServiceFunctionName> sftFirewallList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Dpi.class);
        List<SftServiceFunctionName> sftDpiList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Napt44.class);
        List<SftServiceFunctionName> sftNapt44List = serviceFunctionType.getSftServiceFunctionName();

        /* First round */
        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0), sftFirewallList.get(0).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1), sftDpiList.get(0).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2), sftNapt44List.get(0).getName());

        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0), sftFirewallList.get(1).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1), sftDpiList.get(1).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2), sftNapt44List.get(1).getName());

        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0), sftFirewallList.get(2).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1), sftDpiList.get(2).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2), sftNapt44List.get(2).getName());

        /* Second round */
        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0), sftFirewallList.get(0).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1), sftDpiList.get(0).getName());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2), sftNapt44List.get(0).getName());

        Class scheduleType = scheduler.getSfcServiceFunctionSchedulerType();

        assertNotNull("Must be not null", scheduleType);
        assertEquals("Must be equal", scheduleType.getSimpleName(), RoundRobin.class.getSimpleName());
    }
}

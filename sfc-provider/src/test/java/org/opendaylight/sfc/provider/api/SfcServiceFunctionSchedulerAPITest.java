/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcServiceFunctionSchedulerAPITest extends AbstractDataStoreManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionSchedulerAPITest.class);
    private final List<SfDataPlaneLocator> sfDPLList = new ArrayList<>();
    private final List<ServiceFunction> sfList = new ArrayList<>();
    private ServiceFunctionChain sfChain;
    private ServiceFunctionPath sfPath;

    @Before
    public void before() throws Exception {
        setOdlSfc();

        int maxTries = 10;
        boolean emptyFlag = true;

        // before test, private static variable mapCountRoundRobin has to be restored to original
        // state
        Whitebox.getField(SfcServiceFunctionRoundRobinSchedulerAPI.class, "mapCountRoundRobin").set(HashMap.class,
                new HashMap<>());

        LOG.debug("Empty SFC data store {} times: {}", 10 - maxTries, emptyFlag ? "Successful" : "Failed");
        SfcName sfcName = new SfcName("unittest-sched-chain-1");
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder().setName("firewall")
            .setKey(new SfcServiceFunctionKey("firewall"))
            .setType(new SftTypeName("firewall"))
            .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder().setName("dpi")
            .setKey(new SfcServiceFunctionKey("dpi"))
            .setType(new SftTypeName("dpi"))
            .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder().setName("nat")
            .setKey(new SfcServiceFunctionKey("nat"))
            .setType(new SftTypeName("napt44"))
            .build());

        sfChain = new ServiceFunctionChainBuilder().setName(sfcName)
            .setKey(new ServiceFunctionChainKey(sfcName))
            .setSfcServiceFunction(sfcServiceFunctionList)
            .build();

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey(new SfpName("key")));
        serviceFunctionPathBuilder.setPathId(1L);
        serviceFunctionPathBuilder.setServiceChainName(sfcName);
        List<ServicePathHop> sphs = new ArrayList<>();
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        sfPath = serviceFunctionPathBuilder.build();

        SfDataPlaneLocatorName sfDplName = new SfDataPlaneLocatorName("moscow-5.5.5.5:555-vxlan");
        SffName sffName = new SffName("sff-moscow");
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator(sfDplName,
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.5")), 555), sffName,
                VxlanGpe.class));

        sfDplName = new SfDataPlaneLocatorName("newyork-6.6.6.6:666-vxlan");
        sffName = new SffName("sff-newyork");
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator(sfDplName,
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("6.6.6.6")), 666), sffName,
                VxlanGpe.class));

        sfDplName = new SfDataPlaneLocatorName("paris-7.7.7.7:777-vxlan");
        sffName = new SffName("sff-paris");
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator(sfDplName,
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("7.7.7.7")), 777), sffName,
                VxlanGpe.class));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_100"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.100.101")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_110"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.110.101")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_120"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.120.101")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_dpi_100"), new SftTypeName("dpi"),
                new IpAddress(new Ipv4Address("192.168.100.102")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_dpi_110"), new SftTypeName("dpi"),
                new IpAddress(new Ipv4Address("192.168.110.102")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_dpi_120"), new SftTypeName("dpi"),
                new IpAddress(new Ipv4Address("192.168.120.102")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_nat_100"), new SftTypeName("napt44"),
                new IpAddress(new Ipv4Address("192.168.100.103")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_nat_110"), new SftTypeName("napt44"),
                new IpAddress(new Ipv4Address("192.168.110.103")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_nat_120"), new SftTypeName("napt44"),
                new IpAddress(new Ipv4Address("192.168.120.103")), sfDPLList.get(2), Boolean.FALSE));

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SF_IID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        for (ServiceFunction serviceFunction : sfList) {
            SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction);
        }

        SfcProviderServiceChainAPI.putServiceFunctionChain(sfChain);
    }

    @Test
    public void testBasicEnvSetup() {
        int maxTries;
        for (ServiceFunction serviceFunction : sfList) {
            maxTries = 10;
            ServiceFunction sf2 = null;
            while (maxTries > 0) {
                sf2 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunction.getName());
                maxTries--;
                if (sf2 != null) {
                    break;
                }
            }
            LOG.debug("SfcServiceFunctionSchedulerAPITest: getRead ServiceFunction {} {} times: {}",
                    serviceFunction.getName(), 10 - maxTries, (sf2 != null) ? "Successful" : "Failed");
            assertNotNull("Must be not null", sf2);
            assertEquals("Must be equal", sf2.getName(), serviceFunction.getName());
            assertEquals("Must be equal", sf2.getType(), serviceFunction.getType());
        }

        SfcProviderServiceFunctionAPI.readServiceFunction(new SfName(sfChain.getName().getValue()));
        ServiceFunctionChain sfc2 = SfcProviderServiceChainAPI.readServiceFunctionChain(sfChain.getName());

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfChain.getSfcServiceFunction());

        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("firewall"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
            LOG.debug("sftServiceFunctionName: {}", sftServiceFunctionName.getName());
        }
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("dpi"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("napt44"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);
    }

    @Test
    public void testServiceFunctionRandomScheduler() {
        int serviceIndex = 255;
        SfcServiceFunctionSchedulerAPI scheduler = new SfcServiceFunctionRandomSchedulerAPI();
        List<SfName> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);

        assertNotNull("Must be not null", serviceFunctionNameArrayList);

        ServiceFunction serviceFunction =
                SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(0));
        assertNotNull("Must be not null", serviceFunction);
        assertEquals("Must be equal", serviceFunction.getType(), new SftTypeName("firewall"));
        assertNotEquals("Must be not equal", serviceFunction.getType(), new SftTypeName("dpi"));

        serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(1));
        assertEquals("Must be equal", serviceFunction.getType(), new SftTypeName("dpi"));
        assertNotEquals("Must be not equal", serviceFunction.getType(), new SftTypeName("napt44"));

        serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(2));
        assertEquals("Must be equal", serviceFunction.getType(), new SftTypeName("napt44"));
        assertNotEquals("Must be not equal", serviceFunction.getType(), new SftTypeName("firewall"));
    }

    @Test
    public void testServiceFunctionRoundRobinScheduler() {
        int serviceIndex = 255;
        SfcServiceFunctionSchedulerAPI scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();
        List<SfName> serviceFunctionNameArrayList;

        ServiceFunctionType serviceFunctionType;
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("firewall"));
        List<SftServiceFunctionName> sftFirewallList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("dpi"));
        List<SftServiceFunctionName> sftDpiList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("napt44"));
        List<SftServiceFunctionName> sftNapt44List = serviceFunctionType.getSftServiceFunctionName();

        /* First round */
        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0).getValue(), sftFirewallList.get(0).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1).getValue(), sftDpiList.get(0).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2).getValue(), sftNapt44List.get(0).getName().getValue());

        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0).getValue(), sftFirewallList.get(1).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1).getValue(), sftDpiList.get(1).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2).getValue(), sftNapt44List.get(1).getName().getValue());

        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0).getValue(), sftFirewallList.get(2).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1).getValue(), sftDpiList.get(2).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2).getValue(), sftNapt44List.get(2).getName().getValue());

        /* Second round */
        serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0).getValue(), sftFirewallList.get(0).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1).getValue(), sftDpiList.get(0).getName().getValue());
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2).getValue(), sftNapt44List.get(0).getName().getValue());

        Class<?> scheduleType = scheduler.getSfcServiceFunctionSchedulerType();

        assertNotNull("Must be not null", scheduleType);
        assertEquals("Must be equal", scheduleType.getSimpleName(), RoundRobin.class.getSimpleName());

    }

}

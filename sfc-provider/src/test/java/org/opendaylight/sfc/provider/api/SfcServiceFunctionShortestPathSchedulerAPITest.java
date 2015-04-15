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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionaryBuilder;
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
import static org.junit.Assert.assertNotNull;

public class SfcServiceFunctionShortestPathSchedulerAPITest extends AbstractDataBrokerTest {

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

        String sfcName = "ShortestPath-unittest-chain-1";
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

        //build SFs
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("moscow-5.5.5.5:555-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.5")), 555),
                "SFF1", VxlanGpe.class));
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("newyork-6.6.6.6:666-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("6.6.6.6")), 666),
                "SFF2", VxlanGpe.class));
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("paris-7.7.7.7:777-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("7.7.7.7")), 777),
                "SFF3", VxlanGpe.class));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_100", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.101")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_110", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.110.101")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_120", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.110.101")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_100", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.100.103")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_110", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.110.103")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_120", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.110.103")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_100", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.110.102")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_110", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.110.102")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_120", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.110.102")), sfDPLList.get(2), Boolean.FALSE));

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();

        // build SFFs
        String sffName1 = "SFF1";
        String sffName2 = "SFF2";
        String sffName3 = "SFF3";

        // SFF1
        List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
        ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();

        ConnectedSffDictionary sffDictEntry = sffDictionaryEntryBuilder.setName(sffName3).build();
        sffDictionaryList.add(sffDictEntry);

        List<ServiceFunctionDictionary> sfDictionaryList = new ArrayList<>();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();

        ServiceFunctionDictionary sfDictEntry = dictionaryEntryBuilder.setName("simple_fw_100").build();
        sfDictionaryList.add(sfDictEntry);
        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_nat_100").build();
        sfDictionaryList.add(sfDictEntry);
        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_dpi_100").build();
        sfDictionaryList.add(sfDictEntry);

        ServiceFunctionForwarderBuilder sffBuilder1 = new ServiceFunctionForwarderBuilder();
        ServiceFunctionForwarder sff1 = sffBuilder1.setName(sffName1)
                                                   .setKey(new ServiceFunctionForwarderKey(sffName1))
                                                   .setServiceFunctionDictionary(sfDictionaryList)
                                                   .setConnectedSffDictionary(sffDictionaryList)
                                                   .setServiceNode(null)
                                                   .build();
        executor.submit(SfcProviderServiceForwarderAPI.getPut
                (new Object[]{sff1}, new Class[]{ServiceFunctionForwarder.class})).get();

        // SFF2
        sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
        sffDictEntry = sffDictionaryEntryBuilder.setName(sffName3).build();
        sffDictionaryList.clear();
        sffDictionaryList.add(sffDictEntry);

        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_fw_110").build();
        sfDictionaryList.clear();
        sfDictionaryList.add(sfDictEntry);
        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_nat_110").build();
        sfDictionaryList.add(sfDictEntry);
        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_dpi_110").build();
        sfDictionaryList.add(sfDictEntry);

        ServiceFunctionForwarderBuilder sffBuilder2 = new ServiceFunctionForwarderBuilder();
        ServiceFunctionForwarder sff2 = sffBuilder2.setName(sffName2)
                                                   .setKey(new ServiceFunctionForwarderKey(sffName2))
                                                   .setServiceFunctionDictionary(sfDictionaryList)
                                                   .setConnectedSffDictionary(sffDictionaryList)
                                                   .setServiceNode(null)
                                                   .build();
        executor.submit(SfcProviderServiceForwarderAPI.getPut
                (new Object[]{sff2}, new Class[]{ServiceFunctionForwarder.class})).get();

        // SFF3
        sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
        sffDictEntry = sffDictionaryEntryBuilder.setName(sffName1).build();
        sffDictionaryList.clear();
        sffDictionaryList.add(sffDictEntry);
        sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
        sffDictEntry = sffDictionaryEntryBuilder.setName(sffName2).build();
        sffDictionaryList.add(sffDictEntry);

        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_fw_120").build();
        sfDictionaryList.clear();
        sfDictionaryList.add(sfDictEntry);
        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_nat_120").build();
        sfDictionaryList.add(sfDictEntry);
        dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictEntry = dictionaryEntryBuilder.setName("simple_dpi_120").build();
        sfDictionaryList.add(sfDictEntry);

        ServiceFunctionForwarderBuilder sffBuilder3 = new ServiceFunctionForwarderBuilder();
        ServiceFunctionForwarder sff3 = sffBuilder3.setName(sffName3)
                                                   .setKey(new ServiceFunctionForwarderKey(sffName3))
                                                   .setServiceFunctionDictionary(sfDictionaryList)
                                                   .setConnectedSffDictionary(sffDictionaryList)
                                                   .setServiceNode(null)
                                                   .build();
        executor.submit(SfcProviderServiceForwarderAPI.getPut
                (new Object[]{sff3}, new Class[]{ServiceFunctionForwarder.class})).get();

    }

    @After
    public void after() {
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
    }

    @Test
    public void testSfcServiceFunctionShortestPathScheduler() throws ExecutionException, InterruptedException {

        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

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

        int serviceIndex = 255;

        SfcServiceFunctionShortestPathSchedulerAPI scheduler = new SfcServiceFunctionShortestPathSchedulerAPI();
        List<String> serviceFunctionNameArrayList = scheduler.scheduleServiceFuntions(sfChain, serviceIndex);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);

        Object[] parametersHop = {serviceFunctionNameArrayList.get(0)};
        Class[] parameterTypesHop = {String.class};
        Object resultHop = executor.submit(SfcProviderServiceFunctionAPI
                                .getRead(parametersHop, parameterTypesHop)).get();
        ServiceFunction sfHop0 = (ServiceFunction) resultHop;
        String sffHop0 = sfHop0.getSfDataPlaneLocator().get(0).getName();

        Object[] parametersHop1 = {serviceFunctionNameArrayList.get(1)};
        Class[] parameterTypesHop1 = {String.class};
        Object resultHop1 = executor.submit(SfcProviderServiceFunctionAPI
                                .getRead(parametersHop1, parameterTypesHop1)).get();
        ServiceFunction sfHop1 = (ServiceFunction) resultHop1;
        String sffHop1 = sfHop1.getSfDataPlaneLocator().get(0).getName();

        Object[] parametersHop2 = {serviceFunctionNameArrayList.get(2)};
        Class[] parameterTypesHop2 = {String.class};
        Object resultHop2 = executor.submit(SfcProviderServiceFunctionAPI
                                .getRead(parametersHop2, parameterTypesHop2)).get();
        ServiceFunction sfHop2 = (ServiceFunction) resultHop2;
        String sffHop2 = sfHop2.getSfDataPlaneLocator().get(0).getName();

        assertEquals("Must be equal", sffHop0, sffHop1);
        assertEquals("Must be equal", sffHop1, sffHop2);
    }
}

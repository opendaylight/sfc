/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

/**
 * Tests for dictionary operations on SFFs
 */
public class SfcProviderServiceForwarderAPIDictionaryTest extends AbstractDataBrokerTest {

    DataBroker dataBroker;
    ExecutorService executor;
    OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    String[] sffName = {"unittest-forwarder-1", "unittest-forwarder-2", "unittest-forwarder-3"};
    List<ServiceFunction> sfList = new ArrayList<>();

    @Before
    public void before() {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

        Ip dummyIp = SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.6")), 555);
        SfDataPlaneLocator dummyLocator = SimpleTestEntityBuilder.buildSfDataPlaneLocator("kyiv-5.5.5.6:555-vxlan", dummyIp, "sff-kyiv", VxlanGpe.class);

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("dict_fw_101", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.111")), dummyLocator, Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("dict_fw_102", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.112")), dummyLocator, Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("dict_fw_103", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.113")), dummyLocator, Boolean.FALSE));
    }

    @After
    public void after() {
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
    }

    @Test
    public void testUpdateDictionary() throws ExecutionException, InterruptedException {

        String name = sffName[0];

        List<SffDataPlaneLocator> locatorList = new ArrayList<>();


        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address("10.1.1.101")))
                .setPort(new PortNumber(555));

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build())
                .setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        locatorBuilder.setName("locator-1").setKey(new SffDataPlaneLocatorKey("locator-1"))
                .setDataPlaneLocator(sffLocatorBuilder.build());

        locatorList.add(locatorBuilder.build());


        List<ServiceFunctionDictionary> dictionary = new ArrayList<>();

        ServiceFunction sf0 = sfList.get(0);
        SfDataPlaneLocator sfDPLocator0 = sf0.getSfDataPlaneLocator().get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder0 = new SffSfDataPlaneLocatorBuilder(sfDPLocator0);
        SffSfDataPlaneLocator sffSfDataPlaneLocator0 = sffSfDataPlaneLocatorBuilder0.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder0 = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder0
                .setName(sf0.getName()).setKey(new ServiceFunctionDictionaryKey(sf0.getName()))
                .setType(sf0.getType())
                .setSffSfDataPlaneLocator(sffSfDataPlaneLocator0)
                .setFailmode(Open.class)
                .setSffInterfaces(null);

        ServiceFunctionDictionary firstDictEntry = dictionaryEntryBuilder0.build();
        dictionary.add(firstDictEntry);

        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();

        ServiceFunctionForwarder sff =
                sffBuilder.setName(name).setKey(new ServiceFunctionForwarderKey(name))
                        .setSffDataPlaneLocator(locatorList)
                        .setServiceFunctionDictionary(dictionary)
                        .setServiceNode(null) // for consistency only; we are going to get rid of ServiceNodes in the future
                        .build();

        Object[] parameters = {sff};
        Class[] parameterTypes = {ServiceFunctionForwarder.class};

        executor.submit(SfcProviderServiceForwarderAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {name};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceForwarderAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionForwarder sff2 = (ServiceFunctionForwarder) result;

        assertNotNull("Must be not null", sff2);
        assertEquals("Must be equal", sff2.getSffDataPlaneLocator(), locatorList);
        assertThat("Must contain first dictionary entry", sff2.getServiceFunctionDictionary(), hasItem(firstDictEntry));

        ServiceFunction sf1 = sfList.get(1);
        SfDataPlaneLocator sfDPLocator1 = sf1.getSfDataPlaneLocator().get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder1 = new SffSfDataPlaneLocatorBuilder(sfDPLocator1);
        SffSfDataPlaneLocator sffSfDataPlaneLocator1 = sffSfDataPlaneLocatorBuilder1.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder1 = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder1
                .setName(sf1.getName()).setKey(new ServiceFunctionDictionaryKey(sf1.getName()))
                .setType(sf1.getType())
                .setSffSfDataPlaneLocator(sffSfDataPlaneLocator1)
                .setFailmode(Open.class)
                .setSffInterfaces(null);

        ServiceFunctionDictionary newDictEntry = dictionaryEntryBuilder1.build();

        sff2.getServiceFunctionDictionary().add(newDictEntry);
        dictionary.add(newDictEntry);

        Object[] parameters3 = {sff};
        Class[] parameterTypes3 = {ServiceFunctionForwarder.class};

        executor.submit(SfcProviderServiceForwarderAPI
                .getPut(parameters3, parameterTypes3)).get();

        Object[] parameters4 = {name};
        Class[] parameterTypes4 = {String.class};
        Object result4 = executor.submit(SfcProviderServiceForwarderAPI
                .getRead(parameters4, parameterTypes4)).get();
        ServiceFunctionForwarder sff4 = (ServiceFunctionForwarder) result4;

        assertNotNull("Must be not null", sff4);
        assertThat("Must contain first dictionary entry", sff4.getServiceFunctionDictionary(), hasItem(firstDictEntry));
        assertThat("Must contain new dictionary entry", sff4.getServiceFunctionDictionary(), hasItem(newDictEntry));
    }

}

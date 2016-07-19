/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

/**
 * Tests for dictionary operations on SFFs
 */
public class SfcProviderServiceForwarderAPIDictionaryTest extends AbstractDataStoreManager {

    private List<SffName> sffName = new ArrayList<SffName>() {

        {
            add(new SffName("unittest-forwarder-1"));
            add(new SffName("unittest-forwarder-2"));
            add(new SffName("unittest-forwarder-3"));
        }
    };

    private List<ServiceFunction> sfList = new ArrayList<>();

    @Before
    public void before() {
        setOdlSfc();

        Ip dummyIp = SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.6")), 555);
        SffName sffName = new SffName("sff-kyiv");
        SfDataPlaneLocatorName sfDplName = new SfDataPlaneLocatorName("kyiv-5.5.5.6:555-vxlan");
        SfDataPlaneLocator dummyLocator =
                SimpleTestEntityBuilder.buildSfDataPlaneLocator(sfDplName, dummyIp, sffName, VxlanGpe.class);

        SfName sfName = new SfName("dict_fw_101");
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(sfName, new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.100.111")), dummyLocator, Boolean.FALSE));
        sfName = new SfName("dict_fw_102");
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(sfName, new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.100.112")), dummyLocator, Boolean.FALSE));
        sfName = new SfName("dict_fw_103");
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(sfName, new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.100.113")), dummyLocator, Boolean.FALSE));
    }

    @Test
    public void testUpdateDictionary() {

        SffName name = new SffName(sffName.get(0));
        SffDataPlaneLocatorName sffDplName = new SffDataPlaneLocatorName("locator-1");

        List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address("10.1.1.101"))).setPort(new PortNumber(555));

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        locatorBuilder.setName(sffDplName)
            .setKey(new SffDataPlaneLocatorKey(sffDplName))
            .setDataPlaneLocator(sffLocatorBuilder.build());

        locatorList.add(locatorBuilder.build());

        List<ServiceFunctionDictionary> dictionary = new ArrayList<>();

        ServiceFunction sf0 = sfList.get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder0 = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder0.setSfDplName(sf0.getSfDataPlaneLocator().get(0).getName());
        SffSfDataPlaneLocator sffSfDataPlaneLocator0 = sffSfDataPlaneLocatorBuilder0.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder0 = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder0.setName(sf0.getName())
            .setKey(new ServiceFunctionDictionaryKey(sf0.getName()))
            .setSffSfDataPlaneLocator(sffSfDataPlaneLocator0)
            .setFailmode(Open.class)
            .setSffInterfaces(null);

        ServiceFunctionDictionary firstDictEntry = dictionaryEntryBuilder0.build();
        dictionary.add(firstDictEntry);

        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();

        ServiceFunctionForwarder sff = sffBuilder.setName(name)
            .setKey(new ServiceFunctionForwarderKey(name))
            .setSffDataPlaneLocator(locatorList)
            .setServiceFunctionDictionary(dictionary)
            .setServiceNode(null) // for consistency only; we are going to get rid of ServiceNodes
                                  // in the future
            .build();

        SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);
        ServiceFunctionForwarder sff2 = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(name);
        assertNotNull("Must be not null", sff2);
        assertEquals("Must be equal", sff2.getSffDataPlaneLocator(), locatorList);
        assertThat("Must contain first dictionary entry", sff2.getServiceFunctionDictionary(), hasItem(firstDictEntry));

        ServiceFunction sf1 = sfList.get(1);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder1 = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder1.setSfDplName(sf1.getSfDataPlaneLocator().get(0).getName());
        SffSfDataPlaneLocator sffSfDataPlaneLocator1 = sffSfDataPlaneLocatorBuilder1.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder1 = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder1.setName(sf1.getName())
            .setKey(new ServiceFunctionDictionaryKey(sf1.getName()))
            .setSffSfDataPlaneLocator(sffSfDataPlaneLocator1)
            .setFailmode(Open.class)
            .setSffInterfaces(null);

        ServiceFunctionDictionary newDictEntry = dictionaryEntryBuilder1.build();

        sff2.getServiceFunctionDictionary().add(newDictEntry);
        dictionary.add(newDictEntry);

        SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);
        ServiceFunctionForwarder sff4 = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(name);
        assertNotNull("Must be not null", sff4);
        assertThat("Must contain first dictionary entry", sff4.getServiceFunctionDictionary(), hasItem(firstDictEntry));
        assertThat("Must contain new dictionary entry", sff4.getServiceFunctionDictionary(), hasItem(newDictEntry));
    }

}

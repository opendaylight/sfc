/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry
        .SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry
        .SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions
        .ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.choice.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

public class SfcProviderServiceFunctionAPITest extends AbstractDataBrokerTest {

    public static final String[] LOCATOR_IP_ADDRESS =
            {"196.168.55.1",
                    "196.168.55.2",
                    "196.168.55.3"};
    public static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101",
                    "196.168.55.102",
                    "196.168.55.103"};
    public static final int PORT = 555;
    DataBroker dataBroker;
    ExecutorService executor;
    OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionAPITest.class);

    @Before
    public void before() {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();
    }

    @Test
    public void testDummy() {
        assertEquals("Something wrong with JUnit", 42, 42);
    }

    @Test
    public void testCreateReadServiceFunction() throws ExecutionException, InterruptedException {

        String name = "unittest-fw-1";
        Class<? extends ServiceFunctionTypeIdentity> type = Firewall.class;
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(name);

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(LOCATOR_IP_ADDRESS[0]).setLocatorType(ipBuilder.build());
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(name).setKey(key)
                .setType(type)
                .setIpMgmtAddress(ipMgmtAddress)
                .setSfDataPlaneLocator(dataPlaneLocatorList);

        Object[] parameters = {sfBuilder.build()};
        Class[] parameterTypes = {ServiceFunction.class};

        executor.submit(SfcProviderServiceFunctionAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {name};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunction sf2 = (ServiceFunction) result;

        assertNotNull("Must be not null", sf2);
        assertEquals("Must be equal", sf2.getIpMgmtAddress(), ipMgmtAddress);
        assertEquals("Must be equal", sf2.getType(), type);
        assertEquals("Must be equal", sf2.getSfDataPlaneLocator(), dataPlaneLocatorList);
    }

    @Test
    public void testDeleteServiceFunction() throws ExecutionException, InterruptedException {

        String name = "unittest-fw-1";
        Class<? extends ServiceFunctionTypeIdentity> type = Firewall.class;
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(name);

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(LOCATOR_IP_ADDRESS[0]).setLocatorType(ipBuilder.build());
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(name).setKey(key)
                .setType(type)
                .setIpMgmtAddress(ipMgmtAddress)
                .setSfDataPlaneLocator(dataPlaneLocatorList);

        Object[] parameters = {sfBuilder.build()};
        Class[] parameterTypes = {ServiceFunction.class};

        executor.submit(SfcProviderServiceFunctionAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {name};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();

        assertNotNull("Must be not null", result);
        assertTrue("Must be ServiceFunction", result instanceof ServiceFunction);

        executor.submit(SfcProviderServiceFunctionAPI
                .getDelete(parameters2, parameterTypes2)).get();
        result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();

        assertNull("Must be null", result);
    }

    @Test
    public void testCreateReadServiceFunctions() throws ExecutionException, InterruptedException {

        final String[] sfName = {"unittest-fw-1", "unittest-fw-2", "unittest-fw-3"};
        final Class<? extends ServiceFunctionTypeIdentity> sfType = Firewall.class;
        final IpAddress[] ipMgmtAddress =
                {new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0])),
                        new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[1])),
                        new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[2]))};
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[3];
        ServiceFunctionKey[] key = new ServiceFunctionKey[3];
        for (int i = 0; i < 3; i++) {
            key[i] = new ServiceFunctionKey(sfName[i]);
        }

        final IpAddress[] locatorIpAddress = {new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0])),
                new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[1])),
                new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[2]))};
        PortNumber portNumber = new PortNumber(PORT);

        List<ServiceFunction> list = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(LOCATOR_IP_ADDRESS[i]).setLocatorType(ipBuilder.build());
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(sfName[i]).setKey(key[i])
                    .setType(sfType)
                    .setIpMgmtAddress(ipMgmtAddress[i])
                    .setSfDataPlaneLocator(dataPlaneLocatorList);
            list.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(list);

        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();


        Object[] parameters2 = {sfName[1]};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunction sf2 = (ServiceFunction) result;

        assertNotNull("Must be not null", sf2);
        assertEquals("Must be equal", sf2.getIpMgmtAddress(), ipMgmtAddress[1]);
        assertEquals("Must be equal", sf2.getType(), sfType);
        List<SfDataPlaneLocator> dataPlaneLocatorList2 = new ArrayList<>();
        dataPlaneLocatorList2.add(sfDataPlaneLocator[1]);
        assertEquals("Must be equal", sf2.getSfDataPlaneLocator(), dataPlaneLocatorList2);
    }

}

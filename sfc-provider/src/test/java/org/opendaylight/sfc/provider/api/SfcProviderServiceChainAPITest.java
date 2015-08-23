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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping
        .ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping
        .ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping
        .ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping
        .service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping
        .service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping
        .service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type
        .IpBuilder;
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

public class SfcProviderServiceChainAPITest extends AbstractDataBrokerTest {
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
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);

    List<ServiceFunction> sfList = new ArrayList<>();

    @Before
    public void before() throws ExecutionException, InterruptedException {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

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
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        executor.submit(SfcProviderServiceFunctionAPI
                .getPutAll(
                        new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();

    }

    @After
    public void after() {
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
    }

    @Test
    public void testCreateReadServiceFunctionChain() throws ExecutionException, InterruptedException {

        String name = "unittest-chain-1";
        ServiceFunctionChainKey key = new ServiceFunctionChainKey(name);

        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (ServiceFunction serviceFunction : sfList) {

            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                    sfcServiceFunctionBuilder.setName(serviceFunction.getName())
                            .setKey(new SfcServiceFunctionKey(serviceFunction.getName()))
                            .setType(serviceFunction.getType())
                            .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(name).setKey(key)
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(false);

        Object[] parameters = {sfcBuilder.build()};
        Class[] parameterTypes = {ServiceFunctionChain.class};

        executor.submit(SfcProviderServiceChainAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {name};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfcServiceFunctionList);
    }

    @Test
    public void testDeleteServiceFunctionChain() throws ExecutionException, InterruptedException {

        String name = "unittest-chain-2";
        ServiceFunctionChainKey key = new ServiceFunctionChainKey(name);

        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (ServiceFunction serviceFunction : sfList) {

            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                    sfcServiceFunctionBuilder.setName(serviceFunction.getName())
                            .setKey(new SfcServiceFunctionKey(serviceFunction.getName()))
                            .setType(serviceFunction.getType())
                            .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(name).setKey(key)
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(false);

        Object[] parameters = {sfcBuilder.build()};
        Class[] parameterTypes = {ServiceFunctionChain.class};

        executor.submit(SfcProviderServiceChainAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {name};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();

        assertNotNull("Must be not null", result);
        assertTrue("Must be ServiceFunctionChain", result instanceof ServiceFunctionChain);

       executor.submit(SfcProviderServiceChainAPI
                .getDelete(parameters2, parameterTypes2)).get();

        result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();

        assertNull("Must be null", result);
    }

    @Test
    public void testCreateReadServiceFunctionChains() throws ExecutionException, InterruptedException {

        List<SfcServiceFunction> sfcAllServiceFunctionList = new ArrayList<>();

        for (ServiceFunction serviceFunction : sfList) {

            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                    sfcServiceFunctionBuilder.setName(serviceFunction.getName())
                            .setKey(new SfcServiceFunctionKey(serviceFunction.getName()))
                            .setType(serviceFunction.getType())
                            .build();
            sfcAllServiceFunctionList.add(sfcServiceFunction);
        }

        List<String> sfcName = new ArrayList<>();
        sfcName.add("unittest-sfc-1");
        sfcName.add("unittest-sfc-2");
        sfcName.add("unittest-sfc-3");

        ServiceFunctionChain[] sfcArray = new ServiceFunctionChain[3];

        List<ServiceFunctionChain> sfcList = new ArrayList<>();
        int i = 0;
        for (String name : sfcName) {
            List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

            int j = 0;
            for (SfcServiceFunction sf : sfcAllServiceFunctionList) {
                if (i != j) {
                    sfcServiceFunctionList.add(sf);
                }
                j++;
            }
            ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
            sfcArray[i] = sfcBuilder.setName(name).setKey(new ServiceFunctionChainKey(name))
                    .setSfcServiceFunction(sfcServiceFunctionList)
                    .build();
            sfcList.add(sfcArray[i]);
            i++;
        }

        ServiceFunctionChainsBuilder sfcsBuilder = new ServiceFunctionChainsBuilder();
        sfcsBuilder.setServiceFunctionChain(sfcList);

        executor.submit(SfcProviderServiceChainAPI
                .getPutAll(
                        new Object[]{sfcsBuilder.build()}, new Class[]{ServiceFunctionChains.class})).get();

        final int INDEX_TO_READ = 1;
        Object[] parameters2 = {sfcName.get(INDEX_TO_READ)};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2, sfcArray[INDEX_TO_READ]);
    }

}

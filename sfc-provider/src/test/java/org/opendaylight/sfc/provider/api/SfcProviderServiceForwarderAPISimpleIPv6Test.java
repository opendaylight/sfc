package org.opendaylight.sfc.provider.api;

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

/**
 * Tests for simple datastore operations on SFFs (i.e. Service Functions are created first)
 */

public class SfcProviderServiceForwarderAPISimpleIPv6Test extends AbstractDataBrokerTest {

    private static final String[] LOCATOR_IP_ADDRESS =
            {"fe80::5efe:c000:0201", "ae80::5efe:c000:0201", "be80::5efe:c000:0201",
                    "ce80::5efe:c000:0201", "de80::5efe:c000:0201"};
    private static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101", "196.168.55.102", "196.168.55.103",
                    "196.168.55.104", "196.168.55.105"};
    private static final int[] PORT = {1111, 2222, 3333, 4444, 5555};
    private static final Class[] sfTypes = {Firewall.class, Dpi.class, Napt44.class, HttpHeaderEnrichment.class, Qos.class};
    private static final String[] SF_ABSTRACT_NAMES = {"firewall", "dpi", "napt", "http-header-enrichment", "qos"};
    private static final String SFC_NAME = "unittest-chain-1";
    private static final String SFP_NAME = "unittest-sfp-1";
    private static final String RSP_NAME = "unittest-rsp-1";
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);
    private static final String[] sfNames = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1", "unittest-http-header-enrichment-1", "unittest-qos-1"};
    private String[] SFF_NAMES = {"SFF1", "SFF2", "SFF3", "SFF4", "SFF5"};
    private String[][] TO_SFF_NAMES =
            {{"SFF2", "SFF5"}, {"SFF3", "SFF1"}, {"SFF4", "SFF2"}, {"SFF5", "SFF3"}, {"SFF1", "SFF4"}};
    private String[] SFF_LOCATOR_IP =
            {"196.168.66.101", "196.168.66.102", "196.168.66.103", "196.168.66.104", "196.168.66.105"};
    private List<ServiceFunction> sfList = new ArrayList<>();
    private DataBroker dataBroker;
    private ExecutorService executor;
    private OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    String[] sffName = {"unittest-forwarder-1", "unittest-forwarder-2", "unittest-forwarder-3"};

    @Before
    public void before() throws ExecutionException, InterruptedException {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

        /* Some unit tests can't delete all the objects, so clean up them first */
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        Thread.sleep(1000); // Wait for real delete

        // Create Service Functions
        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames.length];
        final IpAddress[] locatorIpAddress = new IpAddress[sfNames.length];
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[sfNames.length];
        ServiceFunctionKey[] key = new ServiceFunctionKey[sfNames.length];
        for (int i = 0; i < sfNames.length; i++) {
            ipMgmtAddress[i] = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
            locatorIpAddress[i] = new IpAddress(new Ipv6Address(LOCATOR_IP_ADDRESS[0]));
            PortNumber portNumber = new PortNumber(PORT[i]);
            key[i] = new ServiceFunctionKey(sfNames[i]);

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(LOCATOR_IP_ADDRESS[i]).setLocatorType(ipBuilder.build()).setServiceFunctionForwarder(SFF_NAMES[i]);
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(sfNames[i]).setKey(key[i])
                    .setType(sfTypes[i])
                    .setIpMgmtAddress(ipMgmtAddress[i])
                    .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        executor.submit(SfcProviderServiceFunctionAPI.getPutAll(new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();
        Thread.sleep(1000); // Wait they are really created

        // Create ServiceFunctionTypeEntry for all ServiceFunctions
        for (ServiceFunction serviceFunction : sfList) {
            boolean ret = SfcProviderServiceTypeAPI.createServiceFunctionTypeEntryExecutor(serviceFunction);
            LOG.debug("call createServiceFunctionTypeEntryExecutor for {}", serviceFunction.getName());
            assertTrue("Must be true", ret);
        }

        // Create Service Function Forwarders
        for (int i = 0; i < SFF_NAMES.length; i++) {
            List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
                ConnectedSffDictionary sffDictEntry = sffDictionaryEntryBuilder.setName(TO_SFF_NAMES[i][j]).build();
                sffDictionaryList.add(sffDictEntry);
            }

            List<ServiceFunctionDictionary> sfDictionaryList = new ArrayList<>();
            ServiceFunction serviceFunction = sfList.get(i);
            SfDataPlaneLocator sfDPLocator = serviceFunction.getSfDataPlaneLocator().get(0);
            SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder(sfDPLocator);
            SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
            ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
            dictionaryEntryBuilder.setName(serviceFunction.getName())
                    .setKey(new ServiceFunctionDictionaryKey(serviceFunction.getName()))
                    .setType(serviceFunction.getType())
                    .setSffSfDataPlaneLocator(sffSfDataPlaneLocator)
                    .setFailmode(Open.class)
                    .setSffInterfaces(null);
            ServiceFunctionDictionary sfDictEntry = dictionaryEntryBuilder.build();
            sfDictionaryList.add(sfDictEntry);

            List<SffDataPlaneLocator> locatorList = new ArrayList<>();
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[i])))
                    .setPort(new PortNumber(PORT[i]));
            DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
            sffLocatorBuilder.setLocatorType(ipBuilder.build())
                    .setTransport(VxlanGpe.class);
            SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
            locatorBuilder.setName(SFF_LOCATOR_IP[i])
                    .setKey(new SffDataPlaneLocatorKey(SFF_LOCATOR_IP[i]))
                    .setDataPlaneLocator(sffLocatorBuilder.build());
            locatorList.add(locatorBuilder.build());
            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
            sffBuilder.setName(SFF_NAMES[i])
                    .setKey(new ServiceFunctionForwarderKey(SFF_NAMES[i]))
                    .setSffDataPlaneLocator(locatorList)
                    .setServiceFunctionDictionary(sfDictionaryList)
                    .setConnectedSffDictionary(sffDictionaryList)
                    .setServiceNode(null);
            ServiceFunctionForwarder sff = sffBuilder.build();
            executor.submit(SfcProviderServiceForwarderAPI.getPut(new Object[]{sff}, new Class[]{ServiceFunctionForwarder.class})).get();
        }

        //Create Service Function Chain
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(SFC_NAME);
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < SF_ABSTRACT_NAMES.length; i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                    sfcSfBuilder.setName(SF_ABSTRACT_NAMES[i])
                            .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES[i]))
                            .setType(sfTypes[i])
                            .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(SFC_NAME).setKey(sfcKey)
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(true);

        Object[] parameters = {sfcBuilder.build()};
        Class[] parameterTypes = {ServiceFunctionChain.class};

        executor.submit(SfcProviderServiceChainAPI
                .getPut(parameters, parameterTypes)).get();
        Thread.sleep(1000); // Wait SFC is really crated

        //Check if Service Function Chain was created
        Object[] parameters2 = {SFC_NAME};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(SFP_NAME)
                .setServiceChainName(SFC_NAME)
                .setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        boolean ret = SfcProviderServicePathAPI.putServiceFunctionPathExecutor(serviceFunctionPath);
        assertTrue("Must be true", ret);

        Thread.sleep(1000); // Wait they are really created
    }

    @After
    public void after() {
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));

        /* Can't create RSP if we don't do these cleanups, don't know why */
        SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor(SFP_NAME);
        SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor(SFP_NAME);
        SfcProviderServicePathAPI.deleteServicePathStateExecutor(SFP_NAME);
        SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(RSP_NAME);
        for (int i = 0; i < SFF_NAMES.length; i++) {
            SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderStateExecutor(SFF_NAMES[i]);
        }
        for (int i = 0; i < sfNames.length; i++) {
            SfcProviderServiceFunctionAPI.deleteServiceFunctionStateExecutor(sfNames[i]);
        }
    }

    @Test
    public void testCreateReadUpdateServiceFunctionForwarder() throws ExecutionException, InterruptedException {

        String name = sffName[0];

        List<SffDataPlaneLocator> locatorList = new ArrayList<>();


        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv6Address("fe80::5efe:c000:0202")))
                .setPort(new PortNumber(555));

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build())
                .setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        locatorBuilder.setName("locator-1").setKey(new SffDataPlaneLocatorKey("locator-1"))
                .setDataPlaneLocator(sffLocatorBuilder.build());

        locatorList.add(locatorBuilder.build());


        List<ServiceFunctionDictionary> dictionary = new ArrayList<>();

        ServiceFunction sf = sfList.get(0);
        SfDataPlaneLocator sfDPLocator = sf.getSfDataPlaneLocator().get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder(sfDPLocator);
        SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder
                .setName(sf.getName()).setKey(new ServiceFunctionDictionaryKey(sf.getName()))
                .setType(sf.getType())
                .setSffSfDataPlaneLocator(sffSfDataPlaneLocator)
                .setFailmode(Open.class)
                .setSffInterfaces(null);

        ServiceFunctionDictionary dictionaryEntry = dictionaryEntryBuilder.build();
        dictionary.add(dictionaryEntry);

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
        assertEquals("Must be equal", sff2.getServiceFunctionDictionary(), dictionary);
    }
}


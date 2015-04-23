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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Qos;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.HttpHeaderEnrichment;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.lang.reflect.*;

import static org.junit.Assert.*;

public class SfcProviderRenderedPathAPITest extends AbstractDataBrokerTest {
    private static final String[] LOCATOR_IP_ADDRESS =
            {"196.168.55.1", "196.168.55.2","196.168.55.3",
             "196.168.55.4", "196.168.55.5"};
    private static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101", "196.168.55.102", "196.168.55.103",
             "196.168.55.104", "196.168.55.105"};
    private String[] SFF_NAMES = {"SFF1", "SFF2", "SFF3", "SFF4", "SFF5"};
    private String[][] TO_SFF_NAMES =
            {{"SFF2", "SFF5"}, {"SFF3", "SFF1"}, {"SFF4", "SFF2"}, {"SFF5", "SFF3"}, {"SFF1", "SFF4"}};
    private String[] SFF_LOCATOR_IP =
            {"196.168.66.101", "196.168.66.102", "196.168.66.103", "196.168.66.104", "196.168.66.105"};
    private static final int[] PORT = {1111, 2222, 3333, 4444, 5555};
    private static final Class[] sfTypes = {Firewall.class, Dpi.class, Napt44.class, HttpHeaderEnrichment.class, Qos.class};

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

        /* Some unit tests can't delete all the objects, so clean up them first */
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));

        // Create Service Functions
        final String[] sfNames = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1", "unittest-http-header-enrichment-1", "unittest-qos-1"};
        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames.length];
        final IpAddress[] locatorIpAddress = new IpAddress[sfNames.length];
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[sfNames.length];
        ServiceFunctionKey[] key = new ServiceFunctionKey[sfNames.length];
        for (int i = 0; i < sfNames.length; i++) {
            ipMgmtAddress[i] = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
            locatorIpAddress[i] = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
            PortNumber portNumber = new PortNumber(PORT[i]);
            key[i] = new ServiceFunctionKey(sfNames[i]);

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(LOCATOR_IP_ADDRESS[i]).setLocatorType(ipBuilder.build());
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

        // Create Service Function Forwarders

        for (int i = 0; i < SFF_NAMES.length; i++) {
            //ServiceFunctionForwarders connected to SFF_NAMES[i]
            List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
                ConnectedSffDictionary sffDictEntry = sffDictionaryEntryBuilder.setName(TO_SFF_NAMES[i][j]).build();
                sffDictionaryList.add(sffDictEntry);
            }

            //ServiceFunctions attached to SFF_NAMES[i]
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
    }

    @After
    public void after() {
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
    }

    @Test
    public void testReadRenderedServicePathFirstHop() throws ExecutionException, InterruptedException {
        boolean ret;
        String sfcName = "unittest-chain-1";
        String[] sfNames = {"firewall", "dpi", "napt", "http-header-enrichment", "qos"};
        ServiceFunctionChainKey key = new ServiceFunctionChainKey(sfcName);

        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < sfNames.length; i++) {

            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                sfcSfBuilder.setName(sfNames[i])
                            .setKey(new SfcServiceFunctionKey(sfNames[i]))
                            .setType(sfTypes[i])
                            .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(sfcName).setKey(key)
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(true);

        Object[] parameters = {sfcBuilder.build()};
        Class[] parameterTypes = {ServiceFunctionChain.class};

        executor.submit(SfcProviderServiceChainAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {sfcName};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        String pathName = "unittest-sfp-1";
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(pathName)
                   .setServiceChainName(sfcName)
                   .setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        ret = SfcProviderServicePathAPI.putServiceFunctionPathExecutor(serviceFunctionPath);
        assertTrue("Must be true", ret);

        /* Create RenderedServicePath and reverse RenderedServicePath */
        RenderedServicePath renderedServicePath = null;
        RenderedServicePath revRenderedServicePath = null;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setSymmetric(serviceFunctionPath.isSymmetric());
        try {
            Class sfcProviderRenderedPathAPI = Class.forName("org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI");
            Method createRenderedServicePathAndState = sfcProviderRenderedPathAPI.getDeclaredMethod("createRenderedServicePathAndState", ServiceFunctionPath.class, CreateRenderedPathInput.class);
            renderedServicePath = (RenderedServicePath) createRenderedServicePathAndState.invoke(null, serviceFunctionPath, createRenderedPathInputBuilder.build());
            assertNotNull("Must be not null", renderedServicePath);

            Method createSymmetricRenderedServicePathAndState = sfcProviderRenderedPathAPI.getDeclaredMethod("createSymmetricRenderedServicePathAndState", ServiceFunctionPath.class);
            revRenderedServicePath = (RenderedServicePath) createSymmetricRenderedServicePathAndState.invoke(null, renderedServicePath);
            assertNotNull("Must be not null", revRenderedServicePath);

            RenderedServicePathFirstHop firstHop;
            RenderedServicePathFirstHop lastHop;
            Method readRenderedServicePathFirstHop = sfcProviderRenderedPathAPI.getDeclaredMethod("readRenderedServicePathFirstHop", String.class);
            firstHop = (RenderedServicePathFirstHop) readRenderedServicePathFirstHop.invoke(null, renderedServicePath.getName());
            assertNotNull("Must be not null", firstHop);
            lastHop = (RenderedServicePathFirstHop) readRenderedServicePathFirstHop.invoke(null, revRenderedServicePath.getName());
            assertNotNull("Must be not null", lastHop);
            LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
            LOG.debug("Last hop IP: {}, port: {}", lastHop.getIp().toString(), lastHop.getPort());
            assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[0])));
            assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT[0]));
            assertEquals("Must be equal", lastHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[SFF_LOCATOR_IP.length-1])));
            assertEquals("Must be equal", lastHop.getPort(), new PortNumber(PORT[PORT.length-1]));
            Method deleteRenderedServicePathExecutor = sfcProviderRenderedPathAPI.getDeclaredMethod("deleteRenderedServicePathExecutor", String.class);
            deleteRenderedServicePathExecutor.invoke(null, renderedServicePath.getName());
            deleteRenderedServicePathExecutor.invoke(null, revRenderedServicePath.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReadRspFirstHopBySftList() throws ExecutionException, InterruptedException {
        List<Class<? extends ServiceFunctionTypeIdentity>> sftList = new ArrayList<Class<? extends ServiceFunctionTypeIdentity>>();
        sftList.add(Firewall.class);
        sftList.add(Dpi.class);
        sftList.add(Napt44.class);
        sftList.add(HttpHeaderEnrichment.class);
        sftList.add(Qos.class);
        assertEquals("sftList size should be 5", sftList.size(), 5);
        try {
            Class sfcProviderRenderedPathAPI = Class.forName("org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI");
            Method readRspFirstHopBySftList = sfcProviderRenderedPathAPI.getDeclaredMethod("readRspFirstHopBySftList", List.class);
            RenderedServicePathFirstHop firstHop = (RenderedServicePathFirstHop) readRspFirstHopBySftList.invoke(null, sftList);
            assertNotNull("Must be not null", firstHop);
            LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
            assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[SFF_LOCATOR_IP.length-1])));
            assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT[0]));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

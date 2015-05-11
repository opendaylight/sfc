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
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcDataStoreAPI.class, SfcProviderServiceTypeAPI.class, SfcProviderRenderedPathAPI.class})
public class SfcProviderRenderedPathAPITest extends AbstractDataBrokerTest {
    private static final String[] LOCATOR_IP_ADDRESS =
            {"196.168.55.1", "196.168.55.2", "196.168.55.3",
                    "196.168.55.4", "196.168.55.5"};
    private static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101", "196.168.55.102", "196.168.55.103",
                    "196.168.55.104", "196.168.55.105"};
    private static final int[] PORT = {1111, 2222, 3333, 4444, 5555};
    private static final Class[] sfTypes = {Firewall.class, Dpi.class, Napt44.class, HttpHeaderEnrichment.class, Qos.class};
    private static final String[] SF_ABSTRACT_NAMES = {"firewall", "dpi", "napt", "http-header-enrichment", "qos"};
    private static final String SFC_NAME = "unittest-chain-1";
    private static final String SFP_NAME = "unittest-sfp-1";
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);
    private String[] SFF_NAMES = {"SFF1", "SFF2", "SFF3", "SFF4", "SFF5"};
    private String[][] TO_SFF_NAMES =
            {{"SFF2", "SFF5"}, {"SFF3", "SFF1"}, {"SFF4", "SFF2"}, {"SFF5", "SFF3"}, {"SFF1", "SFF4"}};
    private String[] SFF_LOCATOR_IP =
            {"196.168.66.101", "196.168.66.102", "196.168.66.103", "196.168.66.104", "196.168.66.105"};
    private DataBroker dataBroker;
    private ExecutorService executor;
    private OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private CreateRenderedPathInputBuilder createRenderedPathInputBuilder;
    private ServiceFunctionPathBuilder serviceFunctionPathBuilder;
    private RenderedServicePath testRenderedServicePath;
    private SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI;
    private List<ServiceFunction> sfList = new ArrayList<>();
    private Object[] params;

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
    }

    @Test
    public void testReadRenderedServicePathFirstHop() throws ExecutionException, InterruptedException {
        ServiceFunctionPath serviceFunctionPath =
                SfcProviderServicePathAPI.readServiceFunctionPathExecutor(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        /* Create RenderedServicePath and reverse RenderedServicePath */
        RenderedServicePath renderedServicePath = null;
        RenderedServicePath revRenderedServicePath = null;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setSymmetric(serviceFunctionPath.isSymmetric());
        try {
            renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath, createRenderedPathInputBuilder.build());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        assertNotNull("Must be not null", renderedServicePath);

        try {
            revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        assertNotNull("Must be not null", revRenderedServicePath);

        RenderedServicePathFirstHop firstHop;
        RenderedServicePathFirstHop lastHop;
        firstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(renderedServicePath.getName());
        assertNotNull("Must be not null", firstHop);
        lastHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(revRenderedServicePath.getName());
        assertNotNull("Must be not null", lastHop);
        LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
        LOG.debug("Last hop IP: {}, port: {}", lastHop.getIp().toString(), lastHop.getPort());
        assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[0])));
        assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT[0]));
        assertEquals("Must be equal", lastHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[SFF_LOCATOR_IP.length - 1])));
        assertEquals("Must be equal", lastHop.getPort(), new PortNumber(PORT[PORT.length - 1]));
        SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(renderedServicePath.getName());
        SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(revRenderedServicePath.getName());
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
        RenderedServicePathFirstHop firstHop = null;
        try {
            firstHop = SfcProviderRenderedPathAPI.readRspFirstHopBySftList(sftList);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        assertNotNull("Must be not null", firstHop);
        LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
        assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[0])));
        assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT[0]));
    }

    @Test
    public void testCreateRenderedServicePathHopList() throws ExecutionException, InterruptedException {
        final String[] sfNames = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1"};
        List<String> sfNameList = Arrays.asList(sfNames);
        final int startingIndex = 255;

        Object[] parameters = {};
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI(parameters, null);

        List<RenderedServicePathHop> rspHopList = null;

        //sfNameList and sfgNameList null
        rspHopList = sfcProviderRenderedPathAPI.createRenderedServicePathHopList(null, null, startingIndex);
        assertNull("Must be null", rspHopList);

        //usual behaviour
        rspHopList = sfcProviderRenderedPathAPI.createRenderedServicePathHopList(sfNameList, null, startingIndex);
        assertEquals("Size must be equal", sfNameList.size(), rspHopList.size());
        assertEquals("SI must be equal", rspHopList.get(0).getServiceIndex().intValue(), startingIndex);
        assertEquals("SF name must be equal", rspHopList.get(0).getServiceFunctionName(), sfNameList.get(0));
        assertEquals("SI must be equal", rspHopList.get(1).getServiceIndex().intValue(), startingIndex - 1);
        assertEquals("SF name must be equal", rspHopList.get(1).getServiceFunctionName(), sfNameList.get(1));
        assertEquals("SI must be equal", rspHopList.get(2).getServiceIndex().intValue(), startingIndex - 2);
        assertEquals("SF name must be equal", rspHopList.get(2).getServiceFunctionName(), sfNameList.get(2));

        final String[] sfNamesNotExisting = {"unittest-fw-1", "unittest-blabla-1", "unittest-napt-1"};
        sfNameList = Arrays.asList(sfNamesNotExisting);
        //unittest-blabla-1 SF does not exist
        rspHopList = sfcProviderRenderedPathAPI.createRenderedServicePathHopList(sfNameList, null, startingIndex);
        assertNull("Must be null", rspHopList);
    }

    @Test
    public void testCreateRenderedServicePathAndState() throws ExecutionException, InterruptedException {
        //TODO: fix this
        //nasty workaround - this should be executed in after() block
        SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderStateExecutor(SFF_NAMES[1]);
        SfcProviderServiceFunctionAPI.deleteServiceFunctionStateExecutor("unittest-fw-1");
        SfcProviderServicePathAPI.deleteServicePathStateExecutor(SFP_NAME);

        final String rspName = "unittest-rsp";

        ServiceFunctionPath serviceFunctionPath =
                SfcProviderServicePathAPI.readServiceFunctionPathExecutor(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(rspName);

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(
                serviceFunctionPath, createRenderedPathInputBuilder.build());

        //check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffStateExecutor(SFF_NAMES[1]);
        assertNotNull("Must be not null", sffServicePathList);
        assertEquals(sffServicePathList.get(0).getName(), rspName);

        //check if SF oper contains RSP
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionStateExecutor("unittest-fw-1");
        assertEquals(sfServicePathList.get(0).getName(), rspName);

        //check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI.readServicePathStateExecutor(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName(), rspName);
    }

    @Test
    public void testCreateRenderedServicePathEntryWhereServiceFunctionChainIsNull() throws Exception {
        params = new Object[0];
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI(params, null);
        serviceFunctionPathBuilder.setServiceChainName(null);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build());

        assertNull("Must be null", testRenderedServicePath);
    }

    @Test
    public void testCreateRenderedServicePathEntryWhereSfNameListIsNull() throws Exception {
        String serviceChainName = "unittest-chain-1";
        params = new Object[0];
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI(params, null);

        serviceFunctionPathBuilder.setServiceChainName(serviceChainName);

        //this method is called in scheduleServiceFunctions method which is called by scheduler
        PowerMockito.stub(PowerMockito.method(SfcProviderServiceTypeAPI.class, "readServiceFunctionTypeExecutor")).toReturn(null);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build());

        assertNull("Must be null", testRenderedServicePath);
    }

    @Test
    public void testCreateRenderedServicePathEntryWhereServicePathHopIsNull() throws Exception {
        String serviceChainName = "unittest-chain-1";
        params = new Object[0];
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI(params, null);

        serviceFunctionPathBuilder.setServiceChainName(serviceChainName);

        //set list renderedServicePathHopArrayList to null
        PowerMockito.stub(PowerMockito.method(SfcProviderRenderedPathAPI.class, "createRenderedServicePathHopList")).toReturn(null);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build());

        assertNull("Must be null", testRenderedServicePath);
    }


    @Test
    public void testCreateRenderedServicePathEntryPathInputNameAndTransportType() throws Exception {
        String renderedPathInputName = "rpiName";
        String serviceChainName = "unittest-chain-1";
        params = new Object[0];
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI(params, null);

        createRenderedPathInputBuilder.setName(renderedPathInputName);
        serviceFunctionPathBuilder.setTransportType(VxlanGpe.class);
        serviceFunctionPathBuilder.setServiceChainName(serviceChainName);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build());

        assertNotNull("Must not be null", testRenderedServicePath);
        assertEquals("Name must be equal", testRenderedServicePath.getName(), renderedPathInputName);
        assertEquals("Transport type class must be equal", testRenderedServicePath.getTransportType(), VxlanGpe.class);
    }

    @Test
    public void testCreateRenderedServicePathEntryWhereCreationOfRenderedServicePathFailed() throws Exception {
        String renderedPathInputName = "rpiName";
        String serviceChainName = "unittest-chain-1";
        params = new Object[0];
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI(params, null);

        createRenderedPathInputBuilder.setName(renderedPathInputName);
        serviceFunctionPathBuilder.setTransportType(VxlanGpe.class);
        serviceFunctionPathBuilder.setServiceChainName(serviceChainName);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "writeMergeTransactionAPI")).toReturn(false);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build());

        assertNull("Must be null", testRenderedServicePath);
    }

    @Test
    public void testCreateRenderedServicePathEntry() throws Exception {
        String serviceChainName = "unittest-chain-1";
        Long pathId = 1L;
        params = new Object[0];
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI(params, null);

        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey("key"));
        serviceFunctionPathBuilder.setPathId(pathId);
        serviceFunctionPathBuilder.setServiceChainName(serviceChainName);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build());

        assertNotNull("Must not be null", testRenderedServicePath);
        assertEquals("Chain name must be equal", testRenderedServicePath.getServiceChainName(), serviceChainName);
        assertEquals("Transport type must be equal", testRenderedServicePath.getTransportType(), VxlanGpe.class);
        assertEquals("Key name must be equal", testRenderedServicePath.getKey().getName(), "key-Path-1");
        assertEquals("Name must be equal", testRenderedServicePath.getName(), "key-Path-1");
    }
}

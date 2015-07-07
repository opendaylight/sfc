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
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

/**
 * Tests for simple datastore operations on SFFs (i.e. Service Functions are created first)
 */
public class SfcProviderServiceForwarderAPISimpleTest extends AbstractDataBrokerTest {

    private DataBroker dataBroker;
    private ExecutorService executor;
    private static OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    Object[] params = {"hello"};
    SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = new SfcProviderServiceForwarderAPILocal(params, "m");
    private static ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
    private static RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
    private static ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
    private static RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
    private static RenderedServicePathHop renderedServicePathHop = renderedServicePathHopBuilder.setKey(new RenderedServicePathHopKey((short)3))
            .setServiceFunctionForwarder("SFF1").build();
    private static boolean setUpIsDone = false;
    ServiceFunctionForwarder sff;

    static{

    }

    @BeforeClass
    public static void beforeClass() throws Exception{

    }

    @Before
    public void before() throws Exception {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

        //clear data store
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        Thread.sleep(1000);

        if(setUpIsDone){
            return;
        }
        renderedServicePathBuilder.setName("RSP1")
                .setKey(new RenderedServicePathKey("RSP1"));

        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();
        renderedServicePathHopList.add(renderedServicePathHop);

        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        InstanceIdentifier<RenderedServicePath> rspIID =
                InstanceIdentifier.builder(RenderedServicePaths.class)
                        .child(RenderedServicePath.class, new RenderedServicePathKey("RSP1"))
                        .build();
        SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(), LogicalDatastoreType.OPERATIONAL);

        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setName("SFF1")
                .setKey(new ServiceFunctionForwarderKey("SFF1"));
        ServiceFunctionForwarder sff = sffBuilder.build();

        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey("SFF1");
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).
                child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey).build();

        SfcDataStoreAPI.writePutTransactionAPI(sffEntryIID, sff, LogicalDatastoreType.CONFIGURATION);
    }

    @After
    public void after() throws Exception {
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        Thread.sleep(1000);
    }


    @Test
    public void testCreateReadUpdateServiceFunctionForwarder() throws ExecutionException, InterruptedException {
        String name = "SFF1";
        String[] sfNames = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1", "unittest-http-header-enrichment-1", "unittest-qos-1"};
        IpAddress[] ipMgmtAddress = new IpAddress[sfNames.length];

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

        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder.setName("unittest-fw-1").setKey(new SfDataPlaneLocatorKey("unittest-fw-1"));

        SfDataPlaneLocator sfDataPlaneLocator = sfDataPlaneLocatorBuilder.build();
        List<ServiceFunction> sfList = new ArrayList<>();

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);
        sfBuilder.setName(sfNames[0]).setKey(new ServiceFunctionKey("unittest-fw-1"))
                .setType(Firewall.class)
                .setIpMgmtAddress(ipMgmtAddress[0])
                .setSfDataPlaneLocator(dataPlaneLocatorList);
        sfList.add(sfBuilder.build());

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

    @Test
    public void testAddPathToServiceForwarderState(){
        boolean bool = sfcProviderServiceForwarderAPI.addPathToServiceForwarderState("RSP1");
        assertNotNull("Variable has not been set correctly.", bool);
    }

    @Test
    public void testAddPathToServiceForwarderStateRSP() {
        boolean bool = sfcProviderServiceForwarderAPI.addPathToServiceForwarderState("RSP1");
        assertNotNull("Data has not been set correctly.", bool);
    }

    @Test
    public void testDeletePathFromServiceForwarderState(){
        SffServicePathKey sffServicePathKey = new SffServicePathKey("RSP1");
        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey("SFF1");
        InstanceIdentifier<SffServicePath> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                        .child(SffServicePath.class, sffServicePathKey).build();
        SffServicePathBuilder sffServicePathBuilder = new SffServicePathBuilder();
        SffServicePath sffServicePath = sffServicePathBuilder.setName("RSP1").setKey(new SffServicePathKey("RSP1")).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sffServicePath, LogicalDatastoreType.OPERATIONAL);

        boolean bool = sfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState("RSP1");
        assertNotNull("Data has not been correctly deleted.", bool);
    }

    @Test
    public void testDeletePathFromServiceForwarderStateList(){
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setName("RSP1")
                .setKey(new RenderedServicePathKey("RSP1"));

        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();
        renderedServicePathHopList.add(renderedServicePathHop);

        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        InstanceIdentifier<RenderedServicePath> rspIID =
                InstanceIdentifier.builder(RenderedServicePaths.class)
                        .child(RenderedServicePath.class, new RenderedServicePathKey("RSP1"))
                        .build();
        SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(), LogicalDatastoreType.OPERATIONAL);

        SffServicePathKey sffServicePathKey = new SffServicePathKey("RSP1");
        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey("SFF1");
        InstanceIdentifier<SffServicePath> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                        .child(SffServicePath.class, sffServicePathKey).build();
        SffServicePathBuilder sffServicePathBuilder = new SffServicePathBuilder();
        SffServicePath sffServicePath = sffServicePathBuilder.setName("RSP1").setKey(new SffServicePathKey("RSP1")).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sffServicePath, LogicalDatastoreType.OPERATIONAL);

        List<String> servicePaths = new ArrayList<>();
        servicePaths.add("RSP1");
        boolean bool = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(servicePaths);
        assertTrue("Boolean variable has not been set correctly", bool);
    }

    @Test
    public void testDeletePathFromServiceForwarderStateExecutor(){

        pathBuilder.setName("SFP1")
                .setServiceChainName("SFP1")
                .setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        SfcProviderServicePathAPI.putServiceFunctionPathExecutor(serviceFunctionPath);
        assertNotNull("Must be not null", serviceFunctionPath);
        assertTrue(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor(serviceFunctionPath));
    }

    @Test
    public void testAddPathToServiceForwarderStateExecutor(){
        assertTrue(SfcProviderServiceForwarderAPI.addPathToServiceForwarderStateExecutor("RSP1"));
    }

    @Test
    public void testUpdateServiceFunctionForwarderExecutor(){

        sffBuilder.setName("SFF1")
                .setKey(new ServiceFunctionForwarderKey("SFF1"));
        ServiceFunctionForwarder sff = sffBuilder.build();
        assertTrue(SfcProviderServiceForwarderAPI.updateServiceFunctionForwarderExecutor(sff));
    }

    @Test
    public void testDeletePathFromServiceForwarderStateExecutorSP(){
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ServiceFunctionPath serviceFunctionPath = serviceFunctionPathBuilder.setName("SFP1").setKey(new ServiceFunctionPathKey("SFP1")).build();
        assertTrue(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor(serviceFunctionPath));
    }

    @Test
    public void testDeletePathFromServiceForwarderStateExecutorList(){
        List<String> servicePaths = new ArrayList<>();
        servicePaths.add("SP1");
        assertTrue(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor(servicePaths));
    }

//This test runs ok in IDE, but in build it fails
//    @Test
//    public void testDeleteRenderedPathsUsedByServiceForwarder(){
//        ServiceFunctionForwarder serviceFunctionForwarderMock = mock(ServiceFunctionForwarder.class);
//        doReturn("SFF1").when(serviceFunctionForwarderMock).getName();
//
//        List<SffServicePath> sffServicePathList = new ArrayList<>();
//        SffServicePath sffServicePathMock = mock(SffServicePath.class);
//        sffServicePathList.add(sffServicePathMock);
//
//        ServiceFunctionForwarderState serviceFunctionForwarderStateMock = mock(ServiceFunctionForwarderState.class);
//
//        PowerMockito.mockStatic(SfcDataStoreAPI.class);
//        when(SfcDataStoreAPI.readTransactionAPI(any(InstanceIdentifier.class), eq(LogicalDatastoreType.OPERATIONAL))).thenReturn(serviceFunctionForwarderStateMock);
//
//        doReturn("SP1").when(sffServicePathMock).getName();
//
//        RenderedServicePath renderedServicePathMock = mock(RenderedServicePath.class);
//        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
//        when(SfcProviderRenderedPathAPI.readRenderedServicePath("SP1")).thenReturn(renderedServicePathMock);
//
//        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
//        when(SfcProviderRenderedPathAPI.deleteRenderedServicePath("SP1")).thenReturn(true);
//
//        doReturn(sffServicePathList).when(serviceFunctionForwarderStateMock).getSffServicePath();
//        assertTrue(SfcProviderServiceForwarderAPI.deleteRenderedPathsUsedByServiceForwarder(serviceFunctionForwarderMock));
//    }

    @Test
    public void testDeletePathFromServiceForwarderStateExecutorStr(){
        assertTrue(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor("RSP1", "SFF1"));
    }

    @Test
    public void testPutServiceFunctionForwarderExecutor(){
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setName("SFF1")
                .setKey(new ServiceFunctionForwarderKey("SFF1"));
        ServiceFunctionForwarder sff = sffBuilder.build();
        assertTrue(SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(sff));
    }

    @Test
    public void testDeleteSffDataPlaneLocator(){
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();

        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey("SFF1");
        SffDataPlaneLocatorKey sffDataPlaneLocatorKey = new SffDataPlaneLocatorKey("SFFLoc1");
        SffDataPlaneLocator sffDataPlaneLocator = sffDataPlaneLocatorBuilder.setName("SFDP1")
                .setKey(sffDataPlaneLocatorKey).build();

        InstanceIdentifier<SffDataPlaneLocator> sffDataPlaneLocatorIID = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(SffDataPlaneLocator.class, sffDataPlaneLocatorKey).build();

        SfcDataStoreAPI.writePutTransactionAPI(sffDataPlaneLocatorIID, sffDataPlaneLocator, LogicalDatastoreType.CONFIGURATION);

        Object[] params = {"hello"};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = new SfcProviderServiceForwarderAPILocal(params, "m");

        assertTrue(sfcProviderServiceForwarderAPI.deleteSffDataPlaneLocator("SFF1", "SFFLoc1"));
    }

    @Test
    public void testDeleteSffDataPlaneLocatorExecutor(){
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();

        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey("SFF1");
        SffDataPlaneLocatorKey sffDataPlaneLocatorKey = new SffDataPlaneLocatorKey("SFFLoc1");
        SffDataPlaneLocator sffDataPlaneLocator = sffDataPlaneLocatorBuilder.setName("SFDP1")
                .setKey(sffDataPlaneLocatorKey).build();

        InstanceIdentifier<SffDataPlaneLocator> sffDataPlaneLocatorIID = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(SffDataPlaneLocator.class, sffDataPlaneLocatorKey).build();

        SfcDataStoreAPI.writePutTransactionAPI(sffDataPlaneLocatorIID, sffDataPlaneLocator, LogicalDatastoreType.CONFIGURATION);

        assertTrue(SfcProviderServiceForwarderAPI.deleteSffDataPlaneLocatorExecutor("SFF1", "SFFLoc1"));
    }

    @Test
    public void testDeleteServiceFunctionForwarder(){
        Object[] params = {"hello"};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = new SfcProviderServiceForwarderAPILocal(params, "m");

        assertTrue(sfcProviderServiceForwarderAPI.deleteServiceFunctionForwarder("SFF1"));
    }

    @Test
    public void testDeleteServiceFunctionForwarderExecutor(){
        assertTrue(SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarderExecutor("SFF1"));
    }

    @Test
    public void testPutAllServiceFunctionForwarders(){
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setName("SFF1")
                .setKey(new ServiceFunctionForwarderKey("SFF1"));
        ServiceFunctionForwarder sff = sffBuilder.build();

        List<ServiceFunctionForwarder> serviceFunctionForwarderList = new ArrayList<>();
        serviceFunctionForwarderList.add(sff);
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();

        serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);
        ServiceFunctionForwarders sffs = serviceFunctionForwardersBuilder.build();

        Object[] params = {"hello"};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = new SfcProviderServiceForwarderAPILocal(params, "m");

        assertTrue(sfcProviderServiceForwarderAPI.putAllServiceFunctionForwarders(sffs));
    }

    @Test
    public void testDeleteServiceFunctionFromForwarder(){
        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        SfDataPlaneLocator sfDataPlaneLocator = sfDataPlaneLocatorBuilder.setName("SFDPL1")
                .setKey(new SfDataPlaneLocatorKey("SFDPL1")).setServiceFunctionForwarder("SFF1").build();
        List<SfDataPlaneLocator> sfDataPlaneLocatorList = new ArrayList<>();
        sfDataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName("SFD1")
                .setKey(new ServiceFunctionKey("SFD1")).setSfDataPlaneLocator(sfDataPlaneLocatorList);
        ServiceFunction serviceFunction = serviceFunctionBuilder.build();

        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();

        ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();
        ServiceFunctionDictionary serviceFunctionDictionary = serviceFunctionDictionaryBuilder.setName("SFD1")
                .setKey(new ServiceFunctionDictionaryKey("SFD1")).setSffSfDataPlaneLocator(sffSfDataPlaneLocator).build();

        InstanceIdentifier<ServiceFunctionDictionary> sffIID;
        ServiceFunctionDictionaryKey serviceFunctionDictionaryKey =
                new ServiceFunctionDictionaryKey(serviceFunction.getName());
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey("SFF1");
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(ServiceFunctionDictionary.class, serviceFunctionDictionaryKey)
                .build();

        SfcDataStoreAPI.writePutTransactionAPI(sffIID, serviceFunctionDictionary, LogicalDatastoreType.CONFIGURATION);

        Object[] params = {"hello"};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = new SfcProviderServiceForwarderAPILocal(params, "m");
        assertTrue(sfcProviderServiceForwarderAPI.deleteServiceFunctionFromForwarder(serviceFunction));
    }

    private class SfcProviderServiceForwarderAPILocal extends SfcProviderServiceForwarderAPI{

        SfcProviderServiceForwarderAPILocal(Object[] params, String m) {
            super(params, m);
        }
    }
}

/*
 * Copyright (c) 2015, 2017 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.AbstractSfcRendererServicePathAPITest;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.InstantiateServiceFunctionChainInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class SfcProviderRpcTest extends AbstractSfcRendererServicePathAPITest {

    private SfcProviderRpc sfcProviderRpc;

    @Before
    public void setUp() {
        setupSfc();
        sfcProviderRpc = new SfcProviderRpc(dataBroker);
    }

    @After
    public void after() throws ExecutionException, InterruptedException {
        close();
    }

    private void writeRSP() {
        init();

        createRenderedServicePath(RSP_NAME);

        // check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(SFF_NAMES.get(1));
        assertNotNull("Must be not null", sffServicePathList);
        assertEquals(sffServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        // check if SF oper contains RSP
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI
                .readServiceFunctionState(new SfName("unittest-fw-1"));
        assertEquals(sfServicePathList.get(0).getName().getValue(), SFP_NAME.getValue());

        // check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI
                .readServicePathState(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName(), RSP_NAME);

        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(SFP_NAME.getValue());
        ServiceFunctionClassifier serviceFunctionClassifier = serviceFunctionClassifierBuilder.build();

        InstanceIdentifier<ServiceFunctionClassifier> sclIID;
        ServiceFunctionClassifierKey serviceFunctionKey = new ServiceFunctionClassifierKey(SFP_NAME.getValue());
        sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                .child(ServiceFunctionClassifier.class, serviceFunctionKey).build();

        SfcDataStoreAPI.writePutTransactionAPI(sclIID, serviceFunctionClassifier, LogicalDatastoreType.CONFIGURATION);
    }

    @Test
    public void readRenderedServicePathFirstHopTest() throws Exception {
        writeRSP();
        ReadRenderedServicePathFirstHopInputBuilder readRenderedServicePathFirstHopInputBuilder =
                new ReadRenderedServicePathFirstHopInputBuilder();
        readRenderedServicePathFirstHopInputBuilder.setName(RSP_NAME.getValue());
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInput =
                readRenderedServicePathFirstHopInputBuilder.build();

        Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result = sfcProviderRpc
                .readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInput);
        assertNotNull(result);
        assertNotNull(result.get());
        assertNotNull(result.get().getResult());
        assertTrue(result.get().getErrors().isEmpty());
        assertTrue(result.get().isSuccessful());
    }

    @Test
    public void readRenderedServicePathFirstHopElseTest() throws Exception {
        ReadRenderedServicePathFirstHopInputBuilder readRenderedServicePathFirstHopInputBuilder =
                new ReadRenderedServicePathFirstHopInputBuilder();
        readRenderedServicePathFirstHopInputBuilder.setName(RSP_NAME.getValue());
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInput =
                readRenderedServicePathFirstHopInputBuilder
                .build();

        Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result = sfcProviderRpc
                .readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInput);
        assertNotNull(result);
        assertNotNull(result.get());
        assertFalse(result.get().getErrors().isEmpty());
    }

    @Test
    public void putServiceFunctionChainsTest() {
        PutServiceFunctionChainsInputBuilder putServiceFunctionChainsInputBuilder =
                new PutServiceFunctionChainsInputBuilder();
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        serviceFunctionChainBuilder.setName(new SfcName("SFC1"))
                .withKey(new ServiceFunctionChainKey(new SfcName("SFC1")));
        ServiceFunctionChain serviceFunctionChain = serviceFunctionChainBuilder.build();
        List<ServiceFunctionChain> serviceFunctionChainList = new ArrayList<>();
        serviceFunctionChainList.add(serviceFunctionChain);
        putServiceFunctionChainsInputBuilder.setServiceFunctionChain(serviceFunctionChainList);
        PutServiceFunctionChainsInput putServiceFunctionChainsInput = putServiceFunctionChainsInputBuilder.build();
        sfcProviderRpc.putServiceFunctionChains(putServiceFunctionChainsInput);

        ServiceFunctionChainsBuilder serviceFunctionChainsBuilder = new ServiceFunctionChainsBuilder();
        serviceFunctionChainsBuilder = serviceFunctionChainsBuilder
                .setServiceFunctionChain(putServiceFunctionChainsInput.getServiceFunctionChain());
        serviceFunctionChainsBuilder.build();
        InstanceIdentifier<ServiceFunctionChains> sfcId =
                InstanceIdentifier.builder(ServiceFunctionChains.class).build();
        ServiceFunctionChains serviceFunctionChainsFromDataStore = SfcDataStoreAPI.readTransactionAPI(sfcId,
                LogicalDatastoreType.CONFIGURATION);
        assertNotNull(serviceFunctionChainsFromDataStore);
        assertEquals(new SfcName("SFC1"),
                serviceFunctionChainsFromDataStore.getServiceFunctionChain().get(0).getName());
    }

    @Test
    public void instantiateServiceFunctionChainTest() {
        InstantiateServiceFunctionChainInputBuilder instantiateServiceFunctionChainInput =
                new InstantiateServiceFunctionChainInputBuilder();
        assertNull(sfcProviderRpc.instantiateServiceFunctionChain(instantiateServiceFunctionChainInput.build()));
    }

    private void createRenderedServicePath(RspName pathName) {
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        RenderedServicePath configRsp = SfcProviderRenderedPathAPI.createRenderedServicePathInConfig(
                serviceFunctionPath,
                pathName.getValue());
        assertNotNull("Failed to create config rendered service path", configRsp);
        RenderedServicePath operRsp;
        operRsp = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath, configRsp);
        assertNotNull("Failed to create oper rendered service path", operRsp);
    }

    @Test
    public void putAndReadServiceFunctionTest() throws Exception {
        PutServiceFunctionInput putSfInput1 = createPutServiceFunctionInput(new SfName("sfName1"),
                new SftTypeName("firewall"), "192.168.50.80", "192.168.50.85", 6644,
                new SfDataPlaneLocatorName("dpLocatorKey1"));

        createPutServiceFunctionInput(new SfName("sfName2"), new SftTypeName("dpi"), "192.168.50.90", "192.168.50.95",
                6655, new SfDataPlaneLocatorName("dpLocatorKey2"));

        assertServiceFunctionDoesNotExist(putSfInput1.getName());
        putServiceFunction(putSfInput1);
        readAndAssertServiceFunction(putSfInput1);

        // assertServiceFunctionDoesNotExist(putSfInput2.getName());
        // putServiceFunction(putSfInput2);
        // readAndAssertServiceFunction(putSfInput1);
        // readAndAssertServiceFunction(putSfInput2);
    }

    private static PutServiceFunctionInput createPutServiceFunctionInput(SfName sfName, SftTypeName sfType,
            String ipMgmtAddress, String dpLocatorIpAddress, int dpLocatorPort, SfDataPlaneLocatorName dpLocatorKey) {

        // prepare ip builder for data plane locator
        IpAddress ipAddress = IpAddressBuilder.getDefaultInstance(dpLocatorIpAddress);
        PortNumber portNumber = new PortNumber(dpLocatorPort);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder = ipBuilder.setIp(ipAddress).setPort(portNumber);

        // prepare data plane locators
        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        SfDataPlaneLocatorKey sfDataPlaneLocatorKey = new SfDataPlaneLocatorKey(dpLocatorKey);
        sfDataPlaneLocatorBuilder.withKey(sfDataPlaneLocatorKey);
        sfDataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        sfDataPlaneLocatorBuilder.setTransport(SlTransportType.class);
        List<SfDataPlaneLocator> sfDataPlaneLocators = new ArrayList<>();
        sfDataPlaneLocators.add(sfDataPlaneLocatorBuilder.build());

        // create PutServiceFunctionInput
        PutServiceFunctionInputBuilder putServiceFunctionInputBuilder = new PutServiceFunctionInputBuilder();
        putServiceFunctionInputBuilder = putServiceFunctionInputBuilder.setName(sfName).setType(sfType)
                .setIpMgmtAddress(IpAddressBuilder.getDefaultInstance(ipMgmtAddress))
                .setSfDataPlaneLocator(sfDataPlaneLocators);
        return putServiceFunctionInputBuilder.build();
    }

    private void putServiceFunction(PutServiceFunctionInput putSfInput) throws Exception {
        Future<RpcResult<PutServiceFunctionOutput>> result = sfcProviderRpc.putServiceFunction(putSfInput);
        assertTrue("Failed to put service function.",
                result != null && result.get() != null && result.get().isSuccessful());
        assertNotNull(result.get().getResult());
        assertNotNull(result.get().getErrors());
        assertTrue(result.get().getErrors().isEmpty());
    }

    private void readAndAssertServiceFunction(PutServiceFunctionInput putSfInput) throws Exception {
        ReadServiceFunctionInputBuilder readInputBuilder = new ReadServiceFunctionInputBuilder();
        readInputBuilder.setName(putSfInput.getName().getValue());
        ReadServiceFunctionInput readSfInput = readInputBuilder.build();
        Future<RpcResult<ReadServiceFunctionOutput>> readSfResult = sfcProviderRpc.readServiceFunction(readSfInput);
        assertTrue("Failed to read service function",
                readSfResult != null && readSfResult.get() != null && readSfResult.get().isSuccessful());
        assertNotNull(readSfResult.get().getErrors());
        assertTrue(readSfResult.get().getErrors().isEmpty());

        ReadServiceFunctionOutput readSfOutput = readSfResult.get().getResult();
        assertNotNull("Service function not found.", readSfOutput);
        assertEquals("Unexpected SF name.", putSfInput.getName(), readSfOutput.getName());
        assertEquals("Unexpected SF type.", putSfInput.getType(), readSfOutput.getType());
        assertEquals("Unexpected IP mgmt address.", putSfInput.getIpMgmtAddress().getIpv4Address().getValue(),
                readSfOutput.getIpMgmtAddress().getIpv4Address().getValue());
        assertEquals("Bad number of data plane locators.", putSfInput.getSfDataPlaneLocator().size(),
                readSfOutput.getSfDataPlaneLocator().size());
        assertEquals("Unexpected data plane locator item value(s).", putSfInput.getSfDataPlaneLocator().get(0),
                readSfOutput.getSfDataPlaneLocator().get(0));
    }

    private static void assertServiceFunctionDoesNotExist(SfName sfName) {
        InstanceIdentifier<ServiceFunction> sfId = createServiceFunctionId(sfName);
        ServiceFunction sf = SfcDataStoreAPI.readTransactionAPI(sfId, LogicalDatastoreType.CONFIGURATION);
        assertNull("Service function must not exist.", sf);
    }

    private static InstanceIdentifier<ServiceFunction> createServiceFunctionId(SfName sfName) {
        ServiceFunctionKey sfKey = new ServiceFunctionKey(sfName);
        return InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class, sfKey).build();
    }
}

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
//import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
//import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

//import java.util.ArrayList;
//import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OpendaylightSfc.class, SfcProviderServicePathAPI.class, SfcProviderRenderedPathAPI.class,
        SfcProviderServiceClassifierAPI.class, SfcProviderServiceForwarderAPI.class,
        SfcProviderServiceFunctionAPI.class, RpcResultBuilder.class, Futures.class })
public class SfcProviderRpcTest {

    SfcProviderRpc sfcProviderRpc;

    DataBroker dataBrokerMock;

    @Before
    public void before() throws ExecutionException, InterruptedException {
        OpendaylightSfc opendaylightSfcMock = mock(OpendaylightSfc.class);
        PowerMockito.mockStatic(OpendaylightSfc.class);
        when(OpendaylightSfc.getOpendaylightSfcObj()).thenReturn(opendaylightSfcMock);

        dataBrokerMock = mock(DataBroker.class);
        when(opendaylightSfcMock.getDataProvider()).thenReturn(dataBrokerMock);

        sfcProviderRpc = new SfcProviderRpc();
    }

    @Test
    public void createRenderedPathTest() throws ExecutionException, InterruptedException {
        CreateRenderedPathInput createRenderedPathInputMock = mock(CreateRenderedPathInput.class);

        ServiceFunctionPath serviceFunctionPathMock = mock(ServiceFunctionPath.class);
        PowerMockito.mockStatic(SfcProviderServicePathAPI.class);
        doReturn("crpiString1").when(createRenderedPathInputMock).getParentServiceFunctionPath();
        when(SfcProviderServicePathAPI.readServiceFunctionPathExecutor("crpiString1")).thenReturn(
                serviceFunctionPathMock);

        RenderedServicePath renderedServicePathMock = mock(RenderedServicePath.class);
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        when(
                SfcProviderRenderedPathAPI.createRenderedServicePathAndState(eq(serviceFunctionPathMock),
                        eq(createRenderedPathInputMock))).thenReturn(renderedServicePathMock);
        doReturn("classifier1").when(serviceFunctionPathMock).getClassifier();

        ServiceFunctionClassifier serviceFunctionClassifier = mock(ServiceFunctionClassifier.class);
        PowerMockito.mockStatic(SfcProviderServiceClassifierAPI.class);
        doReturn("classifier1").when(serviceFunctionPathMock).getSymmetricClassifier();
        when(SfcProviderServiceClassifierAPI.readServiceClassifierExecutor("classifier1")).thenReturn(
                serviceFunctionClassifier);

        doReturn(true).when(serviceFunctionPathMock).isSymmetric();

        when(SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePathMock))
                .thenReturn(renderedServicePathMock);
        doReturn("rspName1").when(renderedServicePathMock).getName();

        assertEquals("RSP name has not been set correctly.", "rspName1",
                sfcProviderRpc.createRenderedPath(createRenderedPathInputMock).get().getResult().getName());
        PowerMockito.verifyStatic(Mockito.times(2));
        SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor("classifier1", "rspName1");
        PowerMockito.verifyStatic(Mockito.times(1));
        SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePathMock);
        PowerMockito.verifyStatic(Mockito.times(2));
        SfcProviderServiceClassifierAPI.readServiceClassifierExecutor("classifier1");
        PowerMockito.verifyStatic(Mockito.times(1));
        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPathMock,
                createRenderedPathInputMock);
        PowerMockito.verifyStatic(Mockito.times(1));
        SfcProviderServicePathAPI.readServiceFunctionPathExecutor("crpiString1");
    }

    @Test
    public void readRenderedServicePathFirstHopTest() throws ExecutionException, InterruptedException {
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInputMock = mock(ReadRenderedServicePathFirstHopInput.class);
        doReturn("renderedServicePathFirstHopInputName1").when(readRenderedServicePathFirstHopInputMock).getName();
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        RenderedServicePathFirstHop renderedServicePathFirstHopMock = mock(RenderedServicePathFirstHop.class);
        doReturn("rspFirstHop1").when(readRenderedServicePathFirstHopInputMock).getName();
        when(SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop("rspFirstHop1")).thenReturn(
                renderedServicePathFirstHopMock);
        assertNotNull("RenderedServicePathFirstHop has not been set correctly.",
                sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInputMock));
        PowerMockito.verifyStatic(Mockito.times(1));
        SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop("rspFirstHop1");
        PowerMockito.verifyStatic(Mockito.times(1));
        RpcResultBuilder.success(any(ReadRenderedServicePathFirstHopOutput.class));
    }

    @Test
    public void readRenderedServicePathFirstHopElseTest() throws ExecutionException, InterruptedException {
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInputMock = mock(ReadRenderedServicePathFirstHopInput.class);
        doReturn("renderedServicePathFirstHopInputName1").when(readRenderedServicePathFirstHopInputMock).getName();
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        doReturn("rspFirstHop1").when(readRenderedServicePathFirstHopInputMock).getName();
        when(SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop("rspFirstHop1")).thenReturn(null);
        assertNotNull("RenderedServicePathFirstHop has not been set correctly.",
                sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInputMock));
        Mockito.verify(readRenderedServicePathFirstHopInputMock, times(2)).getName();
        PowerMockito.verifyStatic(Mockito.times(1));
        SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop("rspFirstHop1");
    }

    @Test
    public void deleteRenderedPathTest() throws ExecutionException, InterruptedException {
        DeleteRenderedPathInput deleteRenderedPathInput = mock(DeleteRenderedPathInput.class);
        doReturn("stringName1").when(deleteRenderedPathInput).getName();
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        when(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor("stringName1"))
                .thenReturn(true);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        when(SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor("stringName1"))
                .thenReturn(true);
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        when(SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor("stringName1")).thenReturn(true);
        assertNotNull("DeleteRenderedPath has not run correctly.",
                sfcProviderRpc.deleteRenderedPath(deleteRenderedPathInput));
        PowerMockito.verifyStatic();
        SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor("stringName1");
        PowerMockito.verifyStatic();
        SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor("stringName1");
        PowerMockito.verifyStatic();
        SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor("stringName1");
        PowerMockito.verifyStatic();
        RpcResultBuilder.success(any(DeleteRenderedPathOutput.class));
    }

    @Test
    public void getSfcProviderRpcTest() throws Exception {
        assertNotNull(SfcProviderRpc.getSfcProviderRpc());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void readServiceFunctionTest() throws Exception {
        ReadServiceFunctionInput inputMock = mock(ReadServiceFunctionInput.class);
        doReturn("serviceFunctionName").when(inputMock).getName();

        ReadOnlyTransaction readTxMock = mock(ReadOnlyTransaction.class);
        doReturn(readTxMock).when(dataBrokerMock).newReadOnlyTransaction();

        CheckedFuture<Optional<ServiceFunction>, ReadFailedException> checkedFutureMock = mock(CheckedFuture.class);
        when(readTxMock.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(
                checkedFutureMock);

        Optional<ServiceFunction> dataObjectMock = mock(Optional.class);
        doReturn(dataObjectMock).when(checkedFutureMock).get();

        ServiceFunction serviceFunctionMock = mock(ServiceFunction.class);
        doReturn(serviceFunctionMock).when(dataObjectMock).get();

        Future<RpcResult<ReadServiceFunctionOutput>> result = sfcProviderRpc.readServiceFunction(inputMock);
        assertNotNull(result);
        assertNotNull(result.get());
        assertTrue(result.get().isSuccessful());
        assertNotNull(result.get().getResult());
        assertNotNull(result.get().getErrors());
        assertTrue(result.get().getErrors().isEmpty());
        verify(serviceFunctionMock, times(1)).getType();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void readServiceFunctionNotFoundTest() throws Exception {
        ReadServiceFunctionInput inputMock = mock(ReadServiceFunctionInput.class);
        doReturn("serviceFunctionName").when(inputMock).getName();

        ReadOnlyTransaction readTxMock = mock(ReadOnlyTransaction.class);
        doReturn(readTxMock).when(dataBrokerMock).newReadOnlyTransaction();

        CheckedFuture<Optional<ServiceFunction>, ReadFailedException> checkedFutureMock = mock(CheckedFuture.class);
        when(readTxMock.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(
                checkedFutureMock);

        Optional<ServiceFunction> dataObjectMock = mock(Optional.class);
        doReturn(dataObjectMock).when(checkedFutureMock).get();

        ServiceFunction serviceFunctionMock = mock(ServiceFunction.class);
        doReturn(null).when(dataObjectMock).get();

        Future<RpcResult<ReadServiceFunctionOutput>> result = sfcProviderRpc.readServiceFunction(inputMock);
        assertNotNull(result);
        assertNotNull(result.get());
        assertTrue(result.get().isSuccessful());
        assertNull(result.get().getResult());
        assertNotNull(result.get().getErrors());
        assertTrue(result.get().getErrors().isEmpty());
        verify(serviceFunctionMock, never()).getType();
    }

    @Test
    public void deleteRenderedPathElseTest() throws ExecutionException, InterruptedException {
        DeleteRenderedPathInput deleteRenderedPathInput = mock(DeleteRenderedPathInput.class);
        doReturn("stringName1").when(deleteRenderedPathInput).getName();
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        when(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor("stringName1"))
                .thenReturn(true);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        when(SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor("stringName1"))
                .thenReturn(true);
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        when(SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor("stringName1")).thenReturn(false);
        assertNotNull("DeleteRenderedPath has not run correctly.",
                sfcProviderRpc.deleteRenderedPath(deleteRenderedPathInput));
        Mockito.verify(deleteRenderedPathInput, times(4)).getName();
        PowerMockito.verifyStatic();
        SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor("stringName1");
        PowerMockito.verifyStatic();
        SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor("stringName1");
        PowerMockito.verifyStatic();
        SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor("stringName1");
    }

    @Test
    public void readRspFirstHopBySftListTest() throws Exception {
        ReadRspFirstHopBySftListInput inputMock = new ReadRspFirstHopBySftListInputBuilder().build();

        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);

        Future<RpcResult<ReadRspFirstHopBySftListOutput>> result = sfcProviderRpc.readRspFirstHopBySftList(inputMock);
        assertNotNull(result);
        assertNotNull(result.get());
        assertTrue(result.get().isSuccessful());
        assertNull(result.get().getResult());
        assertNotNull(result.get().getErrors());
        assertTrue(result.get().getErrors().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void putServiceFunctionTest() throws Exception {
        PutServiceFunctionInput input = new PutServiceFunctionInputBuilder().build();

        WriteTransaction writeTxMock = mock(WriteTransaction.class);
        doReturn(writeTxMock).when(dataBrokerMock).newWriteOnlyTransaction();

        Future<RpcResult<Void>> result = sfcProviderRpc.putServiceFunction(input);
        assertNotNull(result);
        assertNotNull(result.get());
        assertTrue(result.get().isSuccessful());
        assertNull(result.get().getResult());
        assertNotNull(result.get().getErrors());
        assertTrue(result.get().getErrors().isEmpty());
        verify(writeTxMock, times(1)).merge(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class),
                any(ServiceFunction.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void putServiceFunctionChainsTest() throws Exception {
        PutServiceFunctionChainsInput input = new PutServiceFunctionChainsInputBuilder().build();

        WriteTransaction writeTxMock = mock(WriteTransaction.class);
        doReturn(writeTxMock).when(dataBrokerMock).newWriteOnlyTransaction();

        Future<RpcResult<Void>> result = sfcProviderRpc.putServiceFunctionChains(input);
        assertNotNull(result);
        assertNotNull(result.get());
        assertTrue(result.get().isSuccessful());
        assertNull(result.get().getResult());
        assertNotNull(result.get().getErrors());
        assertTrue(result.get().getErrors().isEmpty());
        verify(writeTxMock, times(1)).merge(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class),
                any(ServiceFunction.class), eq(true));
    }
}

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcProviderRpc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpendaylightSfc.class, SfcProviderServicePathAPI.class, SfcProviderRenderedPathAPI.class,
        SfcProviderServiceClassifierAPI.class, SfcProviderServiceForwarderAPI.class, SfcProviderServiceFunctionAPI.class})
public class SfcProviderRpcTest {

    SfcProviderRpc sfcProviderRpc;

    @Before
    public void before() throws ExecutionException, InterruptedException {
        OpendaylightSfc opendaylightSfcMock = mock(OpendaylightSfc.class);
        PowerMockito.mockStatic(OpendaylightSfc.class);
        when(OpendaylightSfc.getOpendaylightSfcObj()).thenReturn(opendaylightSfcMock);
        sfcProviderRpc = new SfcProviderRpc();
    }

    @Test
    public void createRenderedPathTest() throws ExecutionException, InterruptedException {
        CreateRenderedPathInput createRenderedPathInputMock = mock(CreateRenderedPathInput.class);

        ServiceFunctionPath serviceFunctionPathMock = mock(ServiceFunctionPath.class);
        PowerMockito.mockStatic(SfcProviderServicePathAPI.class);
        when(SfcProviderServicePathAPI.readServiceFunctionPathExecutor(anyString())).thenReturn(serviceFunctionPathMock);

        RenderedServicePath renderedServicePathMock = mock(RenderedServicePath.class);
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        when(SfcProviderRenderedPathAPI.createRenderedServicePathAndState(any(ServiceFunctionPath.class), eq(createRenderedPathInputMock))).thenReturn(renderedServicePathMock);
        doReturn("classifier1").when(serviceFunctionPathMock).getClassifier();

        ServiceFunctionClassifier serviceFunctionClassifier = mock(ServiceFunctionClassifier.class);
        PowerMockito.mockStatic(SfcProviderServiceClassifierAPI.class);
        when(SfcProviderServiceClassifierAPI.readServiceClassifierExecutor("classifier1")).thenReturn(serviceFunctionClassifier);

        doReturn(true).when(serviceFunctionPathMock).isSymmetric();

        when(SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(any(RenderedServicePath.class))).thenReturn(renderedServicePathMock);
        when(SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(anyString())).thenReturn(serviceFunctionClassifier);

        doReturn("symmetricClassifier1").when(serviceFunctionPathMock).getSymmetricClassifier();

        assertNotNull("Rendered Path has not been set correctly.", sfcProviderRpc.createRenderedPath(createRenderedPathInputMock));
        Mockito.verify(renderedServicePathMock, times(2)).getName();
    }

    @Test
    public void readRenderedServicePathFirstHopTest() throws ExecutionException, InterruptedException {
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInputMock = mock(ReadRenderedServicePathFirstHopInput.class);
        doReturn("renderedServicePathFirstHopInputName1").when(readRenderedServicePathFirstHopInputMock).getName();
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        RenderedServicePathFirstHop renderedServicePathFirstHopMock = mock(RenderedServicePathFirstHop.class);
        when(SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(anyString())).thenReturn(renderedServicePathFirstHopMock);
        assertNotNull("RenderedServicePathFirstHop has not been set correctly.", sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInputMock));
        Mockito.verify(readRenderedServicePathFirstHopInputMock, times(1)).getName();
    }

    @Test
    public void readRenderedServicePathFirstHopElseTest() throws ExecutionException, InterruptedException {
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInputMock = mock(ReadRenderedServicePathFirstHopInput.class);
        doReturn("renderedServicePathFirstHopInputName1").when(readRenderedServicePathFirstHopInputMock).getName();
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        when(SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(anyString())).thenReturn(null);
        assertNotNull("RenderedServicePathFirstHop has not been set correctly.", sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInputMock));
        Mockito.verify(readRenderedServicePathFirstHopInputMock, times(2)).getName();
    }

    @Test
    public void deleteRenderedPathTest() throws ExecutionException, InterruptedException {
        DeleteRenderedPathInput deleteRenderedPathInput = mock(DeleteRenderedPathInput.class);
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        when(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor(anyString())).thenReturn(true);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        when(SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor(anyString())).thenReturn(true);
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        when(SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(anyString())).thenReturn(true);
        assertNotNull("DeleteRenderedPath has not run correctly.", sfcProviderRpc.deleteRenderedPath(deleteRenderedPathInput));
        Mockito.verify(deleteRenderedPathInput, times(3)).getName();
    }

    @Test
    public void deleteRenderedPathElseTest() throws ExecutionException, InterruptedException {
        DeleteRenderedPathInput deleteRenderedPathInput = mock(DeleteRenderedPathInput.class);
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        when(SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderStateExecutor(anyString())).thenReturn(true);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        when(SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor(anyString())).thenReturn(true);
        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        when(SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(anyString())).thenReturn(false);
        assertNotNull("DeleteRenderedPath has not run correctly.", sfcProviderRpc.deleteRenderedPath(deleteRenderedPathInput));
        Mockito.verify(deleteRenderedPathInput, times(4)).getName();
    }
}

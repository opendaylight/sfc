/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfReader;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusInterfaceServiceManagerTest {

    @Mock
    ReadWriteTransaction readWriteTransaction;

    @Mock
    DataBroker dataBroker;

    @Mock
    Executor executor;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Mock
    ItmRpcService itmRpcService;

    @Mock
    SfcGeniusSfReader sfcGeniusSfReader;

    @Mock
    SfcGeniusServiceHandler sfcGeniusServiceHandler;

    @Mock
    RpcProviderRegistry rpcProviderRegistry;

    SfcGeniusInterfaceServiceManager sfcGeniusInterfaceServiceManager;

    @Before
    public void setup() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);
        when(rpcProviderRegistry.getRpcService(ItmRpcService.class)).thenReturn(itmRpcService);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgumentAt(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any());
        when(readWriteTransaction.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        when(sfcGeniusServiceHandler.unbindFromInterface(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        sfcGeniusInterfaceServiceManager = spy(
                new SfcGeniusInterfaceServiceManager(dataBroker, rpcProviderRegistry, executor));

        doReturn(sfcGeniusServiceHandler).when(sfcGeniusInterfaceServiceManager)
                .getSfcGeniusServiceHandler(readWriteTransaction);
        doReturn(sfcGeniusSfReader).when(sfcGeniusInterfaceServiceManager)
                .getSfcGeniusSfReader(readWriteTransaction);
    }

    @Test
    public void unbindInterfacesOfServiceFunction() throws Exception {
        when(sfcGeniusSfReader.readInterfacesOfSf(any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList("IF1", "IF2")));

        sfcGeniusInterfaceServiceManager.unbindInterfacesOfServiceFunction("SF1");

        verify(sfcGeniusSfReader).readInterfacesOfSf(new SfName("SF1"));
        verify(sfcGeniusServiceHandler).unbindFromInterface("IF1");
        verify(sfcGeniusServiceHandler).unbindFromInterface("IF2");
        verify(readWriteTransaction).submit();
    }

    @Test
    public void unbindInterfacesOfServiceFunctionNoInterfaces() throws Exception {
        when(sfcGeniusSfReader.readInterfacesOfSf(any()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        sfcGeniusInterfaceServiceManager.unbindInterfacesOfServiceFunction("SF1");

        verify(sfcGeniusSfReader).readInterfacesOfSf(new SfName("SF1"));
        verifyZeroInteractions(sfcGeniusServiceHandler);
    }

    @Test
    public void unbindInterfacesOfServiceFunctionKnownException() throws Exception {
        when(sfcGeniusSfReader.readInterfacesOfSf(any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList("IF1", "IF2")));
        when(readWriteTransaction.submit()).thenThrow(new SfcGeniusRuntimeException(new Throwable()));

        sfcGeniusInterfaceServiceManager.unbindInterfacesOfServiceFunction("SF1");
    }

    @Test(expected = CompletionException.class)
    public void unbindInterfacesOfServiceFunctionUnknownException() throws Exception {
        when(sfcGeniusSfReader.readInterfacesOfSf(any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList("IF1", "IF2")));
        when(readWriteTransaction.submit()).thenThrow(new RuntimeException(""));

        sfcGeniusInterfaceServiceManager.unbindInterfacesOfServiceFunction("SF1");
    }

}

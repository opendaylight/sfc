/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusTsaWriterTest {

    @Mock
    ItmRpcService itmRpcService;

    @Mock
    Executor executor;

    @Mock
    RpcResult rpcResult;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Captor
    ArgumentCaptor<CreateTerminatingServiceActionsInput> createInputCaptor;

    @Captor
    ArgumentCaptor<RemoveTerminatingServiceActionsInput> removeInputCaptor;

    @Test
    public void removeTerminatingServiceAction() throws Exception {
        BigInteger dpnid = BigInteger.valueOf(37);

        when(itmRpcService.removeTerminatingServiceActions(any()))
                .thenReturn(CompletableFuture.completedFuture(rpcResult));
        when(rpcResult.isSuccessful()).thenReturn(true);

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture completableFuture = writer.removeTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        verify(itmRpcService).removeTerminatingServiceActions(removeInputCaptor.capture());
        RemoveTerminatingServiceActionsInput input = removeInputCaptor.getValue();

        assertThat(completableFuture.isDone(), is(true));
        assertThat(dpnid, is(input.getDpnId()));
        assertThat(SfcGeniusConstants.SFC_VNID, is(input.getServiceId()));
        assertThat(completableFuture.isCompletedExceptionally(), is(false));
        assertThat(completableFuture.isCancelled(), is(false));
    }

    @Test
    public void removeTerminatingServiceActionUnsuccessful() throws Exception {
        BigInteger dpnid = BigInteger.ZERO;

        when(itmRpcService.removeTerminatingServiceActions(any()))
                .thenReturn(CompletableFuture.completedFuture(rpcResult));
        when(rpcResult.isSuccessful()).thenReturn(false);

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture completableFuture = writer.removeTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertTrue(completableFuture.isCompletedExceptionally());
        try {
            completableFuture.join();
        } catch (Exception e) {
            assertThat(e.getCause(), is(instanceOf(SfcGeniusRuntimeException.class)));
        };
    }

    @Test
    public void removeTerminatingServiceActionException() throws Exception {
        BigInteger dpnid = BigInteger.ZERO;
        CompletableFuture<RpcResult<Void>> future = new CompletableFuture<>();
        Throwable throwable = new Throwable();
        future.completeExceptionally(throwable);

        when(itmRpcService.removeTerminatingServiceActions(any())).thenReturn(future);

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture completableFuture = writer.removeTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (Exception e) {
            assertThat(e.getCause(), is(instanceOf(SfcGeniusRuntimeException.class)));
        }
    }

}

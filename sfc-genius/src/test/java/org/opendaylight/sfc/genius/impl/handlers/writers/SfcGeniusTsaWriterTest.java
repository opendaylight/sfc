/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.util.SfcGeniusConstants;
import org.opendaylight.sfc.genius.util.SfcGeniusRuntimeException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsOutput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusTsaWriterTest {

    @Mock
    ItmRpcService itmRpcService;

    @Mock
    Executor executor;

    @Mock
    RpcResult<CreateTerminatingServiceActionsOutput> createActionsRpcResult;

    @Mock
    RpcResult<RemoveTerminatingServiceActionsOutput> removeActionsRpcResult;

    @Mock
    RpcError rpcError;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Captor
    ArgumentCaptor<CreateTerminatingServiceActionsInput> createInputCaptor;

    @Captor
    ArgumentCaptor<RemoveTerminatingServiceActionsInput> removeInputCaptor;

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    public void createTerminatingServiceAction() throws Exception {
        Uint64 dpnid = Uint64.valueOf(8);

        when(itmRpcService.createTerminatingServiceActions(any()))
                .thenReturn(Futures.immediateFuture(createActionsRpcResult));
        when(createActionsRpcResult.isSuccessful()).thenReturn(true);

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture<Void> completableFuture = writer.createTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(itmRpcService).createTerminatingServiceActions(createInputCaptor.capture());

        assertThat(completableFuture.isDone(), is(true));

        CreateTerminatingServiceActionsInput input = createInputCaptor.getValue();
        assertThat(input, notNullValue());
        assertThat(input.getDpnId(), is(dpnid));
        assertThat(input.getServiceId(), is(Uint16.valueOf(SfcGeniusConstants.SFC_VNID)));
        assertThat(input.getInstruction().size(), is(1));

        Instruction instruction = input.getInstruction().get(0);
        assertThat(instruction.getInstruction(), is(instanceOf(GoToTableCase.class)));

        GoToTableCase goToTable = (GoToTableCase) instruction.getInstruction();
        assertThat(goToTable.getGoToTable().getTableId(), is(Uint8.valueOf(NwConstants.SFC_TRANSPORT_INGRESS_TABLE)));
    }

    @Test
    public void createTerminatingServiceActionUnsuccessful() throws Exception {
        Uint64 dpnid = Uint64.ZERO;
        Throwable throwable = new Throwable();

        when(itmRpcService.createTerminatingServiceActions(any()))
                .thenReturn(Futures.immediateFuture(createActionsRpcResult));
        when(createActionsRpcResult.isSuccessful()).thenReturn(false);
        when(createActionsRpcResult.getErrors()).thenReturn(Collections.singletonList(rpcError));
        when(rpcError.getCause()).thenReturn(throwable);

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture<Void> completableFuture = writer.createTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (CompletionException e) {
            assertThat(e.getCause(), is(instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause().getCause(), is(instanceOf(RuntimeException.class)));
            assertThat(e.getCause().getCause().getCause(), sameInstance(throwable));
        }
    }

    @Test
    public void createTerminatingServiceActionException() throws Exception {
        Uint64 dpnid = Uint64.ZERO;
        Throwable throwable = new Throwable();

        when(itmRpcService.createTerminatingServiceActions(any())).thenReturn(
                Futures.immediateFailedFuture(throwable));

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture<Void> completableFuture = writer.createTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (CompletionException e) {
            assertThat(e.getCause(), is(instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause().getCause(), is(instanceOf(RuntimeException.class)));
            assertThat(e.getCause().getCause().getCause(), is(instanceOf(ExecutionException.class)));
            assertThat(e.getCause().getCause().getCause().getCause(), sameInstance(throwable));
        }
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void removeTerminatingServiceAction() throws Exception {
        Uint64 dpnid = Uint64.valueOf(37);

        when(itmRpcService.removeTerminatingServiceActions(any()))
                .thenReturn(Futures.immediateFuture(removeActionsRpcResult));
        when(removeActionsRpcResult.isSuccessful()).thenReturn(true);

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture<Void> completableFuture = writer.removeTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        verify(itmRpcService).removeTerminatingServiceActions(removeInputCaptor.capture());
        RemoveTerminatingServiceActionsInput input = removeInputCaptor.getValue();

        assertThat(completableFuture.isDone(), is(true));
        assertThat(dpnid, is(input.getDpnId()));
        assertThat(input.getServiceId(), is(Uint16.valueOf(SfcGeniusConstants.SFC_VNID)));
        assertThat(completableFuture.isCompletedExceptionally(), is(false));
        assertThat(completableFuture.isCancelled(), is(false));
    }

    @Test
    //@Test(expected = org.opendaylight.sfc.genius.util.SfcGeniusRuntimeException.class)
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void removeTerminatingServiceActionUnsuccessful() throws Exception {
        Uint64 dpnid = Uint64.ZERO;
        Throwable throwable = new Throwable();

        when(itmRpcService.removeTerminatingServiceActions(any()))
                .thenReturn(Futures.immediateFuture(removeActionsRpcResult));
        when(removeActionsRpcResult.isSuccessful()).thenReturn(false);
        when(removeActionsRpcResult.getErrors()).thenReturn(Collections.singletonList(rpcError));
        when(rpcError.getCause()).thenReturn(throwable);

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture<Void> completableFuture = writer.removeTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (Exception e) {
            assertThat(e.getCause(), is(instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause().getCause(), is(instanceOf(RuntimeException.class)));
            assertThat(e.getCause().getCause().getCause(), sameInstance(throwable));
        }
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void removeTerminatingServiceActionException() throws Exception {
        Uint64 dpnid = Uint64.ZERO;
        Throwable throwable = new Throwable();

        when(itmRpcService.removeTerminatingServiceActions(any())).thenReturn(Futures.immediateFailedFuture(throwable));

        SfcGeniusTsaWriter writer = new SfcGeniusTsaWriter(itmRpcService, executor);
        CompletableFuture<Void> completableFuture = writer.removeTerminatingServiceAction(dpnid);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (Exception e) {
            assertThat(e.getCause(), is(instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause().getCause(), is(instanceOf(RuntimeException.class)));
            assertThat(e.getCause().getCause().getCause(), is(instanceOf(ExecutionException.class)));
            assertThat(e.getCause().getCause().getCause().getCause(), sameInstance(throwable));
        }
    }

}

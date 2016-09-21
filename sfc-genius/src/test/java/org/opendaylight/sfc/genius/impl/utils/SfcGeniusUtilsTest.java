/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.binding.DataObject;


@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusUtilsTest {

    @Mock
    Executor executor;

    @Mock
    DataObject dataObject;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Test
    public void toCompletableFutureDone() throws Exception {
        ListenableFuture<DataObject> listenableFuture = Futures.immediateFuture(dataObject);

        CompletableFuture<DataObject> completableFuture;
        completableFuture = SfcGeniusUtils.toCompletableFuture(listenableFuture, executor);

        assertThat(completableFuture.isDone(), is(false));
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.isDone(), is(true));
        assertThat(completableFuture.get(), is(dataObject));
    }

    @Test
    public void toCompletableFutureExceptionallyDone() throws Exception {
        Throwable t = new Throwable();
        ListenableFuture<DataObject> listenableFuture = Futures.immediateFailedFuture(t);

        CompletableFuture<DataObject> completableFuture;
        completableFuture = SfcGeniusUtils.toCompletableFuture(listenableFuture, executor);

        assertThat(completableFuture.isDone(), is(false));
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (CompletionException e) {
            assertThat(e.getCause(), is(t));
        }
    }

    @Test
    public void toCompletableFutureCancelled() throws Exception {
        ListenableFuture<DataObject> listenableFuture = Futures.immediateCancelledFuture();

        CompletableFuture<DataObject> completableFuture;
        completableFuture = SfcGeniusUtils.toCompletableFuture(listenableFuture, executor);

        assertThat(completableFuture.isDone(), is(false));
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.isCancelled(), is(true));
    }

    @Test
    public void getDpnIdFromLowerLayerIfListTooManyItems() throws Exception {
        try {
            SfcGeniusUtils.getDpnIdFromLowerLayerIfList(Arrays.asList("Item1", "Item2"));
        } catch (Exception e) {
            assertThat(e, is(instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause(), is(instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    public void getDpnIdFromLowerLayerIfListBadItem() throws Exception {
        try {
            SfcGeniusUtils.getDpnIdFromLowerLayerIfList(Collections.singletonList(""));
        } catch (Exception e) {
            assertThat(e, is(instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause(), is(instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    public void getDpnIdFromNullLowerLayerIfList() throws Exception {
        try {
            SfcGeniusUtils.getDpnIdFromLowerLayerIfList(null);
        } catch (Exception e) {
            assertThat(e, is(instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause(), is(instanceOf(IllegalArgumentException.class)));
        }
    }

}

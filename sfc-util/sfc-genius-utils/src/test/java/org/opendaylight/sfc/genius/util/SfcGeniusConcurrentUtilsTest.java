/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.binding.DataObject;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusConcurrentUtilsTest {

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
        completableFuture = SfcGeniusConcurrentUtils.toCompletableFuture(listenableFuture, executor);

        assertThat(completableFuture.isDone(), is(false));
        Mockito.verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.isDone(), is(true));
        assertThat(completableFuture.get(), is(dataObject));
    }

    @Test
    public void toCompletableFutureExceptionallyDone() throws Exception {
        Throwable throwable = new Throwable();
        ListenableFuture<DataObject> listenableFuture = Futures.immediateFailedFuture(throwable);

        CompletableFuture<DataObject> completableFuture;
        completableFuture = SfcGeniusConcurrentUtils.toCompletableFuture(listenableFuture, executor);

        assertThat(completableFuture.isDone(), is(false));
        Mockito.verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (CompletionException e) {
            assertThat(e.getCause(), is(throwable));
        }
    }

    @Test
    public void toCompletableFutureCancelled() throws Exception {
        ListenableFuture<DataObject> listenableFuture = Futures.immediateCancelledFuture();

        CompletableFuture<DataObject> completableFuture;
        completableFuture = SfcGeniusConcurrentUtils.toCompletableFuture(listenableFuture, executor);

        assertThat(completableFuture.isDone(), is(false));
        Mockito.verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.isCancelled(), is(true));
    }
}

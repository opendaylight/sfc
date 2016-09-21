/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.yangtools.yang.binding.DataObject;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusReaderAbstractTest {

    @Mock
    Executor executor;

    @Mock
    ReadTransaction readTransaction;

    @Mock
    DataObject dataObject;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @InjectMocks
    SfcGeniusReaderAbstract reader;

    @Test
    public void read() throws Exception {
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, null))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(dataObject)));

        CompletableFuture completableFuture = reader.doRead(LogicalDatastoreType.CONFIGURATION, null);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.get(), is(dataObject));
    }

    @Test
    public void readNotFound() throws Exception {
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, null))
                .thenReturn(Futures.immediateCheckedFuture(Optional.absent()));

        CompletableFuture completableFuture = reader.doRead(LogicalDatastoreType.CONFIGURATION, null);

        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertThat(completableFuture.isCompletedExceptionally(), is(true));
        try {
            completableFuture.join();
        } catch (Exception e) {
            assertThat(e.getCause(), is(instanceOf(SfcGeniusRuntimeException.class)));
        }
    }

    @Test
    public void readOptional() throws Exception {
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, null))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(dataObject)));
        CompletableFuture<java.util.Optional<DataObject>> completableFuture = reader.doReadOptional(
                LogicalDatastoreType.CONFIGURATION, null);
        assertFalse(completableFuture.isDone());
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertTrue(completableFuture.isDone());
        assertEquals(dataObject, completableFuture.get().get());
    }

    @Test
    public void readOptionalNotFound() throws Exception {
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, null))
                .thenReturn(Futures.immediateCheckedFuture(Optional.absent()));
        CompletableFuture<java.util.Optional<DataObject>> completableFuture = reader.doReadOptional(
                LogicalDatastoreType.CONFIGURATION, null);
        assertFalse(completableFuture.isDone());
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        assertTrue(completableFuture.isDone());
        assertFalse(completableFuture.get().isPresent());
    }
}

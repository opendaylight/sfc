/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * Utility functions for sfc-genius module.
 */
public final class SfcGeniusConcurrentUtils {

    /**
     * Private constructor to avoid instantiation.
     */
    private SfcGeniusConcurrentUtils() {
    }

    /**
     * Adapts a {@link ListenableFuture} to a {@link CompletableFuture}. The
     * provided executor is used to execute the callback as per
     * {@link Futures#addCallback(ListenableFuture, FutureCallback, Executor)}
     * On such callback, completion will be performed.
     *
     * @param listenableFuture
     *            the listenable future to adapt.
     * @param executor
     *            the executor where the callback execution is submitted.
     * @param <T>
     *            the type of the listenable future.
     * @return the completable future.
     */
    public static <T> CompletableFuture<T> toCompletableFuture(final ListenableFuture<T> listenableFuture,
            Executor executor) {

        CompletableFuture<T> completable = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(mayInterruptIfRunning);
                return result;
            }
        };

        Futures.addCallback(listenableFuture, new FutureCallback<T>() {
            @Override
            // Invalid violation where FB thinks that the param to CompletableFuture#complete must be non-null.
            @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
            public void onSuccess(@Nullable T listenable) {
                completable.complete(listenable);
            }

            @Override
            public void onFailure(Throwable throwable) {
                completable.completeExceptionally(throwable);
            }
        }, executor);
        return completable;
    }

}

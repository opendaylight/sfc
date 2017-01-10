/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

/**
 * Utility functions for sfc-genius module.
 */
public class SfcGeniusUtils {

    /**
     * Private constructor to avoid instantiation.
     */
    private SfcGeniusUtils() {
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
            public void onSuccess(@Nullable T t) {
                completable.complete(t);
            }

            @Override
            public void onFailure(Throwable throwable) {
                completable.completeExceptionally(throwable);
            }
        }, executor);
        return completable;
    }

    /**
     * Extracts the data plane node Id from a lower layer interface list.
     *
     * @param lowerLayerIfList
     *            to extract the data plane node Id from.
     * @return the data plane node Id.
     * @throws SfcGeniusRuntimeException
     *             wrapping an {@link IllegalArgumentException} if the input
     *             list does not contain one item only, or if the format of the
     *             item is invalid.
     */
    public static BigInteger getDpnIdFromLowerLayerIfList(List<String> lowerLayerIfList) {
        if (lowerLayerIfList == null || lowerLayerIfList.size() != 1) {
            throw new SfcGeniusRuntimeException(
                    new IllegalArgumentException("Expected 1 and only 1 item in lower layer interface list"));
        }
        long nodeId = MDSALUtil.getDpnIdFromPortName(new NodeConnectorId(lowerLayerIfList.get(0)));
        if (nodeId < 0L) {
            throw new SfcGeniusRuntimeException(
                    new IllegalArgumentException("Unexpected format of lower layer interface list"));
        }
        return BigInteger.valueOf(nodeId);
    }
}

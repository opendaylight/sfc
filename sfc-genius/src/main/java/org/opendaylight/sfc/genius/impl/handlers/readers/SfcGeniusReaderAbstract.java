/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract helper class that provides utility methods to read from data store
 * asynchronously. These methods will return a {@link CompletableFuture} that
 * will hold the read {@link DataObject} at completion if successful or will be
 * completed exceptionally on error. The data store callback that will complete
 * the future will execute through the provided {@link Executor}.
 */
class SfcGeniusReaderAbstract {

    private final ReadTransaction readTransaction;
    private final Executor executor;

    protected SfcGeniusReaderAbstract(ReadTransaction readTransaction, Executor executor) {
        this.readTransaction = readTransaction;
        this.executor = executor;
    }

    /**
     * Utility method to read a {@link DataObject} from the data store. If it
     * does not exist, the future will be completed exceptionally with a
     * {@link SfcGeniusRuntimeException} wrapping a
     * {@link NoSuchElementException}.
     *
     * @param logicalDatastoreType the data store type to read from..
     * @param instanceIdentifier the instance identifier of the object to read.
     * @param <T> the type of the object to read.
     * @return completable future to the data object.
     */
    protected <T extends DataObject> CompletableFuture<T> doRead(LogicalDatastoreType logicalDatastoreType,
                                                                 InstanceIdentifier<T> instanceIdentifier) {
        return SfcGeniusUtils.toCompletableFuture(
                readTransaction.read(logicalDatastoreType, instanceIdentifier),
                executor
        ).thenApply(optional -> {
            if (optional.isPresent()) {
                return optional.get();
            } else {
                throw new SfcGeniusRuntimeException(
                        new NoSuchElementException("Data store object not found: " + instanceIdentifier));
            }
        });
    }

    /**
     * Utility method to read a {#DataObject} from the data store.
     *
     * @param logicalDatastoreType to read from.
     * @param instanceIdentifier of the data object to read.
     * @param <T> the type of the data object to read.
     * @return future to an optional data object.
     */
    protected <T extends DataObject> CompletableFuture<Optional<T>> doReadOptional(
            LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<T> instanceIdentifier) {
        return SfcGeniusUtils.toCompletableFuture(
                readTransaction.read(logicalDatastoreType, instanceIdentifier), executor).thenApply(
                optional -> {
                    if (optional.isPresent()) {
                        return Optional.of(optional.get());
                    } else {
                        return Optional.empty();
                    }
                });
    }
}

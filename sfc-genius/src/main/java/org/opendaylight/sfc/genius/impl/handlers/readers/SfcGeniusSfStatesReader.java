/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;

/**
 * Helper class to the read information regarding the several service
 * functions from the SFC operational data store asynchronous API.
 */
public class SfcGeniusSfStatesReader {

    private final ReadTransaction transaction;
    private final Executor executor;

    /**
     * Constructs a {@code SfcGeniusSfStatesReader} using the provided
     * {@link ReadTransaction} and {@link Executor}.
     *
     * @param readTransaction the read readTransaction.
     * @param executor the callback executor.
     */
    public SfcGeniusSfStatesReader(ReadTransaction readTransaction, Executor executor) {
        this.transaction = readTransaction;
        this.executor = executor;
    }

    /**
     * Read the service function path names associated to several service
     * functions.
     *
     * @param sfNames the name of the service functions.
     * @return completable future that will hold the names of the service
     *         function paths upon completion.
     */
    public CompletableFuture<List<SfpName>> readSfpNames(List<SfName> sfNames) {
        SfcGeniusSfStateReader sfStateReader = getSfStateReader();
        return sfNames.stream()
                .map(sfStateReader::readSfpNames)
                .map(futureList -> futureList.thenApply(List::stream))
                .reduce(CompletableFuture.completedFuture(Stream.empty()),
                        (f1, f2) -> f1.thenCombine(f2, Stream::concat))
                .thenApply(s -> s.distinct().collect(Collectors.toList()));
    }

    protected SfcGeniusSfStateReader getSfStateReader() {
        return new SfcGeniusSfStateReader(transaction, executor);
    }
}

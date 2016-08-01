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

public class SfcGeniusSfStatesReader {

    private final ReadTransaction transaction;
    private final Executor executor;

    public SfcGeniusSfStatesReader(ReadTransaction transaction, Executor executor) {
        this.transaction = transaction;
        this.executor = executor;
    }

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

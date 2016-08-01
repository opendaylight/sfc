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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;

public class SfcGeniusSfpStatesReader {

    private final ReadTransaction transaction;
    private final Executor executor;

    public SfcGeniusSfpStatesReader(ReadTransaction transaction, Executor executor) {
        this.transaction = transaction;
        this.executor = executor;
    }

    public CompletableFuture<List<RspName>> readRspNames(List<SfpName> sfpNames) {
        SfcGeniusSfpStateReader sfpStateReader = getSfpStateReader();
        return sfpNames.stream()
                .map(sfpStateReader::readRspNames)
                .map(futureList -> futureList.thenApply(List::stream))
                .reduce(CompletableFuture.completedFuture(Stream.empty()),
                        (f1, f2) -> f1.thenCombine(f2, Stream::concat))
                .thenApply(stream -> stream.distinct().collect(Collectors.toList()));
    }

    protected SfcGeniusSfpStateReader getSfpStateReader() {
        return new SfcGeniusSfpStateReader(transaction, executor);
    }
}

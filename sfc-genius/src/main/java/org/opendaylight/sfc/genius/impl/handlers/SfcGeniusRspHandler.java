/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusRspReader;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfReader;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfStatesReader;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfpStatesReader;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusRspWriter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;

/**
 * Handles required actions towards rendered service paths resulting from
 * interface events.
 */
class SfcGeniusRspHandler {

    private Executor executor;
    private final ReadWriteTransaction transaction;


    SfcGeniusRspHandler(ReadWriteTransaction transaction, Executor executor) {
        this.executor = executor;
        this.transaction = transaction;
    }

    CompletableFuture<Void> interfaceStateUp(String interfaceName) {
        SfcGeniusSfReader sfReader = getSfReader();
        SfcGeniusSfStatesReader sfStatesReader = getSfStatesReader();
        SfcGeniusSfpStatesReader sfpStatesReader = getSfpStatesReader();

        return sfReader.readSfOnInterface(interfaceName)
                .thenCompose(sfStatesReader::readSfpNames)
                .thenCompose(sfpStatesReader::readRspNames)
                .thenCompose(rspList -> CompletableFuture.allOf(
                        rspList.stream()
                                .map(this::reRenderRsp)
                                .toArray(size -> new CompletableFuture<?>[size])
                ));
    }

    /**
     * Re-render a rendered service path
     *
     * @param rspName the rendered service path name
     */
    private CompletableFuture<Void> reRenderRsp(RspName rspName) {
        SfcGeniusRspReader rspReader = getRspReader();
        SfcGeniusRspWriter rspWriter = getRspWriter();
        return rspReader.readRsp(rspName)
                .thenCompose(rspWriter::deleteRsp)
                .thenCompose(rspWriter::createRsp);
    }

    protected SfcGeniusRspWriter getRspWriter() {
        return new SfcGeniusRspWriter(transaction, executor);
    }

    protected SfcGeniusRspReader getRspReader() {
        return new SfcGeniusRspReader(transaction, executor);
    }

    protected SfcGeniusSfReader getSfReader() {
        return new SfcGeniusSfReader(transaction, executor);
    }

    protected  SfcGeniusSfStatesReader getSfStatesReader() {
        return new SfcGeniusSfStatesReader(transaction, executor);
    }

    protected  SfcGeniusSfpStatesReader getSfpStatesReader() {
        return new SfcGeniusSfpStatesReader(transaction, executor);
    }
}

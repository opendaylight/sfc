/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;


import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusRspReader;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfStatesReader;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusRspWriter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles required actions towards rendered service paths resulting from
 * interface events.
 */
class SfcGeniusRspHandler {

    private Executor executor;
    private final ReadWriteTransaction transaction;
    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusRspHandler.class);


    SfcGeniusRspHandler(ReadWriteTransaction transaction, Executor executor) {
        this.executor = executor;
        this.transaction = transaction;
    }

    /**
     * Handle the rendered service paths for an interface that has become
     * available: re-render the rendered service paths associated to service
     * functions that have such an interface as data plane locator.
     *
     * @param interfaceName the name of the interface.
     * @param sfNameList the service function names with such interface as
     *                   locator.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> interfaceStateUp(String interfaceName, List<SfName> sfNameList) {
        SfcGeniusSfStatesReader sfStatesReader = getSfStatesReader();

        LOG.debug("Re-render for interface {} of service functions {}", interfaceName, sfNameList);

        return sfStatesReader.readSfpNames(sfNameList)
                .thenCompose(rspList -> CompletableFuture.allOf(
                        rspList.stream()
                                .map(SfpName::getValue)
                                .map(RspName::new)
                                .map(this::reRenderRsp)
                                .toArray(size -> new CompletableFuture<?>[size])
                ));
    }

    /**
     * Re-render a rendered service path.
     *
     * @param rspName the rendered service path name.
     * @return future signaling completion of the operation.
     */
    private CompletableFuture<Void> reRenderRsp(RspName rspName) {
        SfcGeniusRspReader rspReader = getRspReader();
        SfcGeniusRspWriter rspWriter = getRspWriter();

        LOG.debug("Re-render RSP {}", rspName);

        return rspReader.readRsp(rspName)
                .thenCompose(rspWriter::deleteRsp)
                .thenCompose(rspWriter::createRsp);
    }

    protected SfcGeniusRspWriter getRspWriter() {
        return new SfcGeniusRspWriter(transaction);
    }

    protected SfcGeniusRspReader getRspReader() {
        return new SfcGeniusRspReader(transaction, executor);
    }

    protected SfcGeniusSfStatesReader getSfStatesReader() {
        return new SfcGeniusSfStatesReader(transaction, executor);
    }
}

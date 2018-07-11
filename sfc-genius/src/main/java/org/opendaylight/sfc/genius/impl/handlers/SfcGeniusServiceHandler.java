/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusBoundServiceWriter;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusTsaWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that handles the SFC service binding of interfaces through
 * Genius APIs.
 * - All the data store reads & writes are done through a single
 *   {@link ReadWriteTransaction}
 * - {@link ItmRpcService} is used for Genius ITM RPC APIs.
 * - An {@link Executor} has to be provided to execute the synchronous RPC
 *   blocking calls and the asynchronous data store callbacks.
 */
class SfcGeniusServiceHandler {

    private final ItmRpcService itmRpcService;
    private final Executor executor;
    private final ReadWriteTransaction transaction;
    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusServiceHandler.class);

    /**
     * Constructs a {@code SfcGeniusServiceHandler}.
     *
     * @param transaction
     *            the transaction used for read & write operations to the data
     *            store.
     * @param itmRpcService
     *            the Genius ITM RPC service
     * @param executor
     *            the executor used for callbacks & blocking calls.
     */
    SfcGeniusServiceHandler(ReadWriteTransaction transaction, ItmRpcService itmRpcService,
                            Executor executor) {
        this.transaction = transaction;
        this.itmRpcService = itmRpcService;
        this.executor = executor;
    }

    /**
     * Bind SFC service to interface. Will also add the terminating service
     * action if this is the first interface bound to SFC service on the node.
     *
     * @param interfaceName
     *            the interface name.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> bindToInterface(String interfaceName) {
        LOG.debug("Bind SFC service to interface {}", interfaceName);
        SfcGeniusBoundServiceWriter boundServiceWriter = getBoundServiceWriter();
        return boundServiceWriter.bindService(interfaceName);
    }

    /**
     * Unbind SFC service from interface. Will also remove the terminating
     * service action if this is the last interface bound to SFC service on the
     * node.
     *
     * @param interfaceName
     *            the interface name.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> unbindFromInterface(String interfaceName) {
        LOG.debug("Unbind SFC service from interface {}", interfaceName);
        SfcGeniusBoundServiceWriter boundServiceWriter = getBoundServiceWriter();
        return boundServiceWriter.unbindService(interfaceName);
    }

    /**
     * Bind SFC service to node: add the terminating service action for SFC
     * service.
     *
     * @param dpnId
     *            the node dataplane id.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> bindToNode(BigInteger dpnId) {
        LOG.debug("Bind SFC service on node {}", dpnId);
        SfcGeniusTsaWriter tsaWriter = getTsaWriter();
        return tsaWriter.createTerminatingServiceAction(dpnId);
    }

    /**
     * Unbind SFC service to node: remove the terminating service action for
     * SFC service.
     *
     * @param dpnId
     *            the node dataplane id.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> unbindFromNode(BigInteger dpnId) {
        LOG.debug("Unbind SFC service on node {}", dpnId);
        SfcGeniusTsaWriter tsaWriter = getTsaWriter();
        return tsaWriter.removeTerminatingServiceAction(dpnId);
    }

    protected SfcGeniusTsaWriter getTsaWriter() {
        return new SfcGeniusTsaWriter(itmRpcService, executor);
    }

    protected SfcGeniusBoundServiceWriter getBoundServiceWriter() {
        return new SfcGeniusBoundServiceWriter(transaction);
    }
}

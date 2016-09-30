/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;


import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusIfStateReader;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusBoundServiceWriter;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusDpnIfWriter;
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
 * - Data plane node to interface information is to be stored in a {@link Map}.
 *   {@see SfcGeniusDpnIfWriter} for more information.
 */
class SfcGeniusServiceHandler {

    private final Map<BigInteger, Set<String>> dpnInterfaces;
    private final ItmRpcService itmRpcService;
    private final Executor executor;
    private final ReadWriteTransaction transaction;
    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusServiceHandler.class);

    /**
     * Constructs a {@code SfcGeniusServiceHandler}.
     *
     * @param dpnInterfaces the Map that stores current data plane node to
     *                      to interfaces mapping information.
     * @param transaction the transaction used for read & write operations
     *                    to the data store.
     * @param rpcProviderRegistry the provider registry to obtain the
     *                            {@link ItmRpcService}.
     * @param executor the executor used for callbacks & blocking calls.
     */
    SfcGeniusServiceHandler(Map<BigInteger, Set<String>> dpnInterfaces,
                            ReadWriteTransaction transaction,
                            RpcProviderRegistry rpcProviderRegistry,
                            Executor executor) {
        this.transaction = transaction;
        this.itmRpcService = rpcProviderRegistry.getRpcService(ItmRpcService.class);
        this.dpnInterfaces = dpnInterfaces;
        this.executor = executor;
    }

    /**
     * Bind SFC service to interface. Will also add the terminating
     * service action if this is the first interface bound to SFC service on
     * the node.
     *
     * @param interfaceName the interface name.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> bindToInterface(String interfaceName) {
        SfcGeniusIfStateReader ifStateReader = getIfStateReader();
        SfcGeniusDpnIfWriter dpnIfWriter = getDpnIfWriter();
        SfcGeniusTsaWriter tsaWriter = getTsaWriter();
        SfcGeniusBoundServiceWriter boundServiceWriter = getBoundServiceWriter();

        LOG.debug("Bind SFC service to interface {}", interfaceName);

        return CompletableFuture.allOf(
                ifStateReader.readDpnId(interfaceName)
                        .thenCompose(dpnId -> dpnIfWriter.addInterface(dpnId, interfaceName))
                        .thenCompose(optionalOldDpn -> optionalOldDpn
                                .map(tsaWriter::createTerminatingServiceAction)
                                .orElse(CompletableFuture.completedFuture(null))),
                boundServiceWriter.bindService(interfaceName)
        );
    }

    /**
     * Unbind SFC service from interface. Will also remove the terminating
     * service action if this is the last interface bound to SFC service on
     * the node.
     *
     * @param interfaceName the interface name.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> unbindFromInterface(String interfaceName) {
        SfcGeniusIfStateReader ifStateReader = getIfStateReader();
        SfcGeniusDpnIfWriter dpnIfWriter = getDpnIfWriter();
        SfcGeniusTsaWriter tsaWriter = getTsaWriter();
        SfcGeniusBoundServiceWriter boundServiceWriter = getBoundServiceWriter();

        LOG.debug("Unbind SFC service from interface {}", interfaceName);

        return CompletableFuture.allOf(
                ifStateReader.readDpnId(interfaceName)
                        .thenCompose(dpnId -> dpnIfWriter.removeInterfaceFromDpn(dpnId, interfaceName))
                        .thenCompose(optionalOldDpn -> optionalOldDpn
                                .map(tsaWriter::removeTerminatingServiceAction)
                                .orElse(CompletableFuture.completedFuture(null))),
                boundServiceWriter.unbindService(interfaceName)
        );
    }

    /**
     * Handle SFC service for an interface that has become available: add the
     * terminating service action if this is the first interface bound to SFC
     * service on the node.
     *
     * @param interfaceName the name of the interface.
     * @param nodeId the data plane node Id where the interface is located.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> interfaceStateUp(String interfaceName, BigInteger nodeId) {
        SfcGeniusDpnIfWriter dpnIfWriter = getDpnIfWriter();
        SfcGeniusTsaWriter tsaWriter = getTsaWriter();

        return dpnIfWriter.addInterface(nodeId, interfaceName)
                .thenCompose(optionalNewDpn -> optionalNewDpn
                        .map(tsaWriter::createTerminatingServiceAction)
                        .orElse(CompletableFuture.completedFuture(null))
                );
    }

    /**
     * Handle SFC service for an interface that has become unavailable: remove
     * the terminating service action if this is the last interface bound to
     * SFC service on the node.
     *
     * @param interfaceName the name of the interface.
     * @param nodeId the data plane node Id where the interface was located.
     * @return future signaling completion of the operation.
     */
    CompletableFuture<Void> interfaceStateDown(String interfaceName, BigInteger nodeId) {
        SfcGeniusDpnIfWriter dpnIfWriter = getDpnIfWriter();
        SfcGeniusTsaWriter tsaWriter = getTsaWriter();

        return dpnIfWriter.removeInterfaceFromDpn(nodeId, interfaceName).thenCompose(
                optionalOldDpn -> optionalOldDpn
                        .map(tsaWriter::removeTerminatingServiceAction)
                        .orElse(CompletableFuture.completedFuture(null))
        );
    }

    protected SfcGeniusIfStateReader getIfStateReader() {
        return new SfcGeniusIfStateReader(transaction, executor);
    }

    protected SfcGeniusDpnIfWriter getDpnIfWriter() {
        return new SfcGeniusDpnIfWriter(dpnInterfaces);
    }

    protected SfcGeniusTsaWriter getTsaWriter() {
        return new SfcGeniusTsaWriter(itmRpcService, executor);
    }

    protected SfcGeniusBoundServiceWriter getBoundServiceWriter() {
        return new SfcGeniusBoundServiceWriter(transaction);
    }
}

/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfReader;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 *
 * <p>
 * SFC service binding to logical interface is done through Genius Interface
 * Manager via operation of the same name.
 *
 * <p>
 * SFC service binding to a node is done through Genius ITM via "create/remove
 * service terminating action".
 *
 * <p>
 * When an interface becomes available after being unavailable due to a
 * node/port transition, any RSPs on which associated service functions
 * participate will be re-rendered.
 *
 * @see "org.opendaylight.genius.itm"
 * @see "org.opendaylight.genius.interfacemanager"
 */
public class SfcGeniusServiceManagerImpl implements org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager {

    private final DataBroker dataBroker;
    private final RpcConsumerRegistry rpcRegistry;
    private final Executor executor;
    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusServiceManagerImpl.class);

    /**
     * Constructs a {@code SfcGeniusInterfaceServiceManager} using the provided
     * {@link DataBroker} for data store operations, the
     * {@link RpcConsumerRegistry} to access RPC services and the
     * {@link Executor} for asynchronous tasks.
     *
     *
     * @param dataBroker
     *            the data broker for data store operations.
     * @param rpcRegistry
     *            the RPC service registry.
     * @param executor
     *            the executor where asynchronous tasks are executed.
     */
    public SfcGeniusServiceManagerImpl(DataBroker dataBroker, RpcConsumerRegistry rpcRegistry,
            Executor executor) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        this.executor = executor;
    }

    @Override
    public void bindInterfacesOfServiceFunction(String sfName) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusSfReader sfReader = getSfcGeniusSfReader(readWriteTransaction);
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);

        LOG.debug("Bind interfaces of service function {}", sfName);

        sfReader.readInterfacesOfSf(new SfName(sfName))
                .thenCompose(interfaceList -> CompletableFuture.allOf(interfaceList.stream()
                        .map(serviceHandler::bindToInterface).toArray(size -> new CompletableFuture<?>[size])))
                .thenCompose(aVoid -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .exceptionally(exception -> {
                    if (exception.getCause() instanceof SfcGeniusRuntimeException) {
                        LOG.error("Error binding to interfaces of service function {}", sfName, exception.getCause());
                        return null;
                    }
                    throw new CompletionException(exception.getCause());
                }).join();
    }

    @Override
    public void unbindInterfacesOfServiceFunction(String sfName) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusSfReader sfReader = getSfcGeniusSfReader(readWriteTransaction);
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);

        LOG.debug("Unbind interfaces of service function {}", sfName);

        sfReader.readInterfacesOfSf(new SfName(sfName))
                .thenCompose(interfaceList -> CompletableFuture.allOf(interfaceList.stream()
                        .map(serviceHandler::unbindFromInterface).toArray(size -> new CompletableFuture<?>[size])))
                .thenCompose(aVoid -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .exceptionally(exception -> {
                    if (exception.getCause() instanceof SfcGeniusRuntimeException) {
                        LOG.error("Error unbinding from interfaces of service function {}", sfName,
                                exception.getCause());
                        return null;
                    }
                    throw new CompletionException(exception.getCause());
                }).join();
    }

    @Override
    public void unbindInterfaces(List<String> interfaceNames) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);

        LOG.debug("Unbind interfaces of service function {}", interfaceNames);

        CompletableFuture.allOf(interfaceNames.stream()
                        .map(serviceHandler::unbindFromInterface)
                        .toArray(size -> new CompletableFuture<?>[size]))
                .thenCompose(aVoid -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .exceptionally(exception -> {
                    if (exception.getCause() instanceof SfcGeniusRuntimeException) {
                        LOG.error("Error unbinding from interfaces {}", interfaceNames,
                                exception.getCause());
                        return null;
                    }
                    throw new CompletionException(exception.getCause());
                }).join();
    }

    @Override
    public void bindNode(BigInteger dpnId) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);
        serviceHandler.bindToNode(dpnId)
                .thenCompose(aVoid -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .join();
    }

    @Override
    public void unbindNode(BigInteger dpnId) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);
        serviceHandler.unbindFromNode(dpnId)
                .thenCompose(aVoid -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .join();
    }

    @Override
    public void interfaceStateUp(String interfaceName, BigInteger dpnId) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusRspHandler rspHandler = getSfcGeniusRspHandler(readWriteTransaction);
        SfcGeniusSfReader sfReader = getSfcGeniusSfReader(readWriteTransaction);

        sfReader.readSfOnInterface(interfaceName)
                .thenCompose(serviceFunctions -> serviceFunctions.isEmpty()
                        ? CompletableFuture.completedFuture(null)
                        : rspHandler.interfaceStateUp(interfaceName, serviceFunctions))
                .thenCompose(aVoid -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .handle((nop, exception) -> {
                    if (exception != null) {
                        LOG.error("Error handling interface {} state up on {}", interfaceName, dpnId, exception);
                    }
                    return null;
                }).join();
    }

    protected SfcGeniusRspHandler getSfcGeniusRspHandler(ReadWriteTransaction readWriteTransaction) {
        return new SfcGeniusRspHandler(readWriteTransaction, executor);
    }

    protected SfcGeniusSfReader getSfcGeniusSfReader(ReadWriteTransaction readWriteTransaction) {
        return new SfcGeniusSfReader(readWriteTransaction, executor);
    }

    protected SfcGeniusServiceHandler getSfcGeniusServiceHandler(ReadWriteTransaction readWriteTransaction) {
        return new SfcGeniusServiceHandler(readWriteTransaction, rpcRegistry, executor);
    }
}

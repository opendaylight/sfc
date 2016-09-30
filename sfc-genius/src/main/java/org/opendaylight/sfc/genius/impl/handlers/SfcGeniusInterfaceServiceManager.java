/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;


import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfReader;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 *
 * SFC service binding to logical interface is done through Genius Interface
 * Manager.
 *
 * On any given node, when one or more interfaces bound to SFC service are
 * present, a SFC service terminating action is configured through Genius ITM.
 * Otherwise, the service terminating action is removed.
 *
 * When an interface becomes available after being unavailable due to a
 * node/port transition, any RSPs on which associated service functions participate
 * will be re-rendered.
 *
 * @see "org.opendaylight.genius.itm"
 * @see "org.opendaylight.genius.interfacemanager"
 */
public class SfcGeniusInterfaceServiceManager implements ISfcGeniusInterfaceServiceHandler {

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final Executor executor;
    private final Map<BigInteger, Set<String>> dpnInterfaces;
    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusInterfaceServiceManager.class);

    /**
     * Constructs a {@code SfcGeniusInterfaceServiceManager} using the provided
     * {@link DataBroker} for data store operations, the
     * {@link RpcProviderRegistry} to access RPC services and the
     * {@link Executor} for asynchronous tasks.
     *
     *
     * @param dataBroker the data broker for data store operations.
     * @param rpcProviderRegistry the RPC provider registry of services.
     * @param executor the executor where asynchronous tasks are executed.
     */
    public SfcGeniusInterfaceServiceManager(DataBroker dataBroker,
                                            RpcProviderRegistry rpcProviderRegistry,
                                            Executor executor) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.executor = executor;
        this.dpnInterfaces = new HashMap<>();
    }

    @Override
    public void bindInterfacesOfServiceFunction(String sfName) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusSfReader sfReader = getSfcGeniusSfReader(readWriteTransaction);
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);

        LOG.debug("Bind interfaces of service function {}", sfName);

        sfReader.readInterfacesOfSf(new SfName(sfName))
                .thenCompose(interfaceList -> CompletableFuture.allOf(
                        interfaceList.stream()
                                .map(serviceHandler::bindToInterface)
                                .toArray(size -> new CompletableFuture<?>[size])))
                .thenCompose((aVoid) -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .exceptionally(exception -> {
                    if (exception.getCause() instanceof SfcGeniusRuntimeException) {
                        LOG.error("Error binding to interfaces of service function {}", sfName, exception.getCause());
                        return null;
                    }
                    throw new CompletionException(exception.getCause());
                })
                .join();
    }

    @Override
    public void unbindInterfacesOfServiceFunction(String sfName) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusSfReader sfReader = getSfcGeniusSfReader(readWriteTransaction);
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);

        LOG.debug("Unbind interfaces of service function {}", sfName);

        sfReader.readInterfacesOfSf(new SfName(sfName))
                .thenCompose(interfaceList ->
                        CompletableFuture.allOf(
                                interfaceList.stream()
                                        .map(serviceHandler::unbindFromInterface)
                                        .toArray(size -> new CompletableFuture<?>[size])))
                .thenCompose((aVoid) -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .exceptionally(exception -> {
                    if (exception.getCause() instanceof SfcGeniusRuntimeException) {
                        LOG.error("Error unbinding from interfaces of service function {}",
                                sfName, exception.getCause());
                        return null;
                    }
                    throw new CompletionException(exception.getCause());
                })
                .join();
    }

    @Override
    public void interfaceStateUp(String interfaceName, BigInteger dpnId) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusRspHandler rspHandler = getSfcGeniusRspHandler(readWriteTransaction);
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);
        SfcGeniusSfReader sfReader = getSfcGeniusSfReader(readWriteTransaction);

        sfReader.readSfOnInterface(interfaceName).thenCompose(serviceFunctions -> serviceFunctions.isEmpty()
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.allOf(
                        rspHandler.interfaceStateUp(interfaceName, serviceFunctions),
                        serviceHandler.interfaceStateUp(interfaceName, dpnId))
                    .thenCompose((aVoid) -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor)
                    ).handle((aVoid, exception) -> {
                        if (exception != null) {
                            LOG.error("Error handling interface {} state up on {}", interfaceName, dpnId, exception);
                        }
                        return null;
                    }))
                .join();
    }

    @Override
    public void interfaceStateDown(String interfaceName, BigInteger nodeId) {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        SfcGeniusServiceHandler serviceHandler = getSfcGeniusServiceHandler(readWriteTransaction);

        serviceHandler.interfaceStateDown(interfaceName, nodeId)
                .thenCompose((aVoid) -> SfcGeniusUtils.toCompletableFuture(readWriteTransaction.submit(), executor))
                .handle((aVoid, exception) -> {
                    if (exception != null) {
                        LOG.error("Error handling interface {} state up on {}", interfaceName, nodeId, exception);
                    }
                    return null;
                })
                .join();
    }

    protected SfcGeniusRspHandler getSfcGeniusRspHandler(ReadWriteTransaction readWriteTransaction) {
        return new SfcGeniusRspHandler(readWriteTransaction, executor);
    }

    protected SfcGeniusSfReader getSfcGeniusSfReader(ReadWriteTransaction readWriteTransaction) {
        return new SfcGeniusSfReader(readWriteTransaction, executor);
    }

    protected SfcGeniusServiceHandler getSfcGeniusServiceHandler(ReadWriteTransaction readWriteTransaction) {
        return new SfcGeniusServiceHandler(dpnInterfaces, readWriteTransaction, rpcProviderRegistry, executor);
    }

}

/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.sfc.genius.impl.handlers.SfcGeniusServiceManagerImpl;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusInterfaceStateListener;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusSfListener;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusSfStateListener;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusSffDpnStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SfcGeniusImpl {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusImpl.class);
    private final DataBroker dataBroker;
    private final RpcConsumerRegistry rpcRegistry;
    private AutoCloseable onDestroy;

    @Inject
    public SfcGeniusImpl(DataBroker dataBroker, RpcConsumerRegistry rpcRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcRegistry = Preconditions.checkNotNull(rpcRegistry);
    }

    @PostConstruct
    public void init() {
        LOG.info("Initializing SFC Genius module {}", this);

        // Listeners will submit jobs to this executor, data store events will be
        // handled synchronously, one at a time and in order.
        ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();

        // Handlers will submit asynchronous callback jobs to this executor
        ExecutorService handlerExecutor = Executors.newSingleThreadExecutor();

        // Main handler of data store events
        SfcGeniusServiceManager interfaceManager;
        interfaceManager = new SfcGeniusServiceManagerImpl(dataBroker, rpcRegistry, handlerExecutor);

        // Listeners to data store events
        SfcGeniusSfStateListener sfStateListener;
        sfStateListener = new SfcGeniusSfStateListener(dataBroker, interfaceManager, listenerExecutor);
        sfStateListener.register();

        SfcGeniusInterfaceStateListener interfaceStateListener;
        interfaceStateListener = new SfcGeniusInterfaceStateListener(dataBroker, interfaceManager, listenerExecutor);
        interfaceStateListener.register();

        SfcGeniusSffDpnStateListener sfcGeniusSffDpnStateListener;
        sfcGeniusSffDpnStateListener = new SfcGeniusSffDpnStateListener(dataBroker, interfaceManager, listenerExecutor);
        sfcGeniusSffDpnStateListener.register();

        SfcGeniusSfListener sfcGeniusSfListener;
        sfcGeniusSfListener = new SfcGeniusSfListener(dataBroker, interfaceManager, listenerExecutor);
        sfcGeniusSfListener.register();

        onDestroy = () -> {
            sfStateListener.close();
            interfaceStateListener.close();
            sfcGeniusSffDpnStateListener.close();
            sfcGeniusSfListener.close();
        };

        LOG.info("SFC Genius module {} initialized", this);
    }

    @PreDestroy
    public void destroy() throws Exception {
        LOG.info("Closing SFC Genius module {}", this);
        onDestroy.close();
        LOG.info("SFC Genius module instance {} closed", this);
    }
}

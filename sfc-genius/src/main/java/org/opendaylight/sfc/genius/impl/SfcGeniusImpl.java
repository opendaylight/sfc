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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.impl.handlers.ISfcGeniusInterfaceServiceHandler;
import org.opendaylight.sfc.genius.impl.handlers.SfcGeniusInterfaceServiceManager;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusInterfaceStateListener;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusSfStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcGeniusImpl {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusImpl.class);
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private AutoCloseable onDestroy;

    public SfcGeniusImpl(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
    }

    public void init() {
        LOG.info("Initializing SFC Genius module {}", this);

        // Listeners will submit jobs to this executor, data store events will be
        // handled synchronously, one at a time and in order.
        ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();

        // Handlers will submit asynchronous callback jobs to this executor
        ExecutorService handlerExecutor = Executors.newSingleThreadExecutor();

        // Main handler of data store events
        ISfcGeniusInterfaceServiceHandler interfaceManager;
        interfaceManager = new SfcGeniusInterfaceServiceManager(dataBroker, rpcProviderRegistry, handlerExecutor);

        // Listeners to data store events
        SfcGeniusSfStateListener sfStateListener;
        sfStateListener = new SfcGeniusSfStateListener(interfaceManager, listenerExecutor);
        SfcGeniusInterfaceStateListener interfaceStateListener;
        interfaceStateListener = new SfcGeniusInterfaceStateListener(interfaceManager, listenerExecutor);

        sfStateListener.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
        interfaceStateListener.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);

        onDestroy = () -> {
            sfStateListener.close();
            interfaceStateListener.close();
        };

        LOG.info("SFC Genius module {} initialized", this);
    }

    public void destroy() throws Exception {
        LOG.info("Closing SFC Genius module {}", this);
        onDestroy.close();
        LOG.info("SFC Genius module instance {} closed", this);
    }
}

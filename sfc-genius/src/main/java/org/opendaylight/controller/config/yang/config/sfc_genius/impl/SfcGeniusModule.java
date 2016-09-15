/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_genius.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.impl.handlers.ISfcGeniusInterfaceServiceHandler;
import org.opendaylight.sfc.genius.impl.handlers.SfcGeniusInterfaceServiceManager;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusInterfaceStateListener;
import org.opendaylight.sfc.genius.impl.listeners.SfcGeniusSfStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcGeniusModule
        extends AbstractSfcGeniusModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusModule.class);

    public SfcGeniusModule(ModuleIdentifier identifier,
                           DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcGeniusModule(ModuleIdentifier identifier,
                           DependencyResolver dependencyResolver,
                           SfcGeniusModule oldModule,
                           AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {

        LOG.info("Initializing SFC Genius module {}", this);

        DataBroker dataBroker = getDataBrokerDependency();
        RpcProviderRegistry rpcProviderRegistry = getRpcRegistryDependency();

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

        LOG.info("SFC Genius module {} initialized", this);

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                LOG.info("Closing SFC Genius module {}", this);

                sfStateListener.close();
                interfaceStateListener.close();

                LOG.info("SFC Genius module instance {} closed", this);
            }
        };
    }

}

/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_genius.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.sfc.genius.provider.SfcGeniusSfInterfaceManager;
import org.opendaylight.sfc.genius.provider.listeners.SfcGeniusInterfaceStateListener;
import org.opendaylight.sfc.genius.provider.listeners.SfcGeniusSfStateListener;

public class SfcGeniusModule
        extends org.opendaylight.controller.config.yang.config.sfc_genius.impl.AbstractSfcGeniusModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusModule.class);

    public SfcGeniusModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcGeniusModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                           org.opendaylight.controller.config.yang.config.sfc_genius.impl.SfcGeniusModule oldModule,
                           java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        LOG.info("Initializing SFC Genius module {}", this);

        DataBroker dataBroker = getDataBrokerDependency();
        SfcGeniusSfInterfaceManager interfaceManager = new SfcGeniusSfInterfaceManager(dataBroker);
        SfcGeniusSfStateListener sfListener = new SfcGeniusSfStateListener(interfaceManager);
        SfcGeniusInterfaceStateListener interfaceListener = new SfcGeniusInterfaceStateListener(interfaceManager);
        sfListener.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
        interfaceListener.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);

        LOG.info("SFC Genius module {} initialized", this);

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                LOG.info("Closing SFC Genius module {}", this);

                sfListener.close();
                interfaceListener.close();

                LOG.info("SFC Genius module instance {} closed", this);
            }
        };
    }

}

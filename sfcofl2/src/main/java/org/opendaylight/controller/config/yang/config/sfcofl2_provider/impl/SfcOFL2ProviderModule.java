/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl;

import org.opendaylight.ofsfc.provider.SfcL2Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOFL2ProviderModule extends org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.AbstractSfcOFL2ProviderModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOFL2ProviderModule.class);

    public SfcOFL2ProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcOFL2ProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                 org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.SfcOFL2ProviderModule oldModule,
                                 java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final SfcL2Renderer openflowSfcRenderer = new SfcL2Renderer(getDataBrokerDependency(), getRpcRegistryDependency());

        java.lang.AutoCloseable ret = new java.lang.AutoCloseable() {
            @Override
            public void close() throws Exception {
                openflowSfcRenderer.close();
            }
        };

        LOG.info("SFC OFL2 provider (instance {}) initialized.", ret);

        return ret;
    }
}

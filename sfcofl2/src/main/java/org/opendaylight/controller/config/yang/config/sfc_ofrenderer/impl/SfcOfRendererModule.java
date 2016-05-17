/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_ofrenderer.impl;

import org.opendaylight.sfc.ofrenderer.SfcOfRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOfRendererModule extends org.opendaylight.controller.config.yang.config.sfc_ofrenderer.impl.AbstractSfcOfRendererModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRendererModule.class);

    public SfcOfRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcOfRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                 org.opendaylight.controller.config.yang.config.sfc_ofrenderer.impl.SfcOfRendererModule oldModule,
                                 java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("SFC OF Renderer initializing");

        final SfcOfRenderer openflowSfcRenderer = new SfcOfRenderer(getDataBrokerDependency(), getNotificationServiceDependency());

        java.lang.AutoCloseable ret = new java.lang.AutoCloseable() {
            @Override
            public void close() throws Exception {
                openflowSfcRenderer.close();
            }
        };

        LOG.info("SFC OF Renderer initialized: (instance {})", ret);

        return ret;
    }
}

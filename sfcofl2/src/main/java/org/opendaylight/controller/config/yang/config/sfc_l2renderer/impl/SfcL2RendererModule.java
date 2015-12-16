/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_l2renderer.impl;

import org.opendaylight.sfc.l2renderer.SfcL2Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2RendererModule extends org.opendaylight.controller.config.yang.config.sfc_l2renderer.impl.AbstractSfcL2RendererModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RendererModule.class);

    public SfcL2RendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcL2RendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                 org.opendaylight.controller.config.yang.config.sfc_l2renderer.impl.SfcL2RendererModule oldModule,
                                 java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("SFC OFL2 Renderer initializing");

        final SfcL2Renderer openflowSfcRenderer = new SfcL2Renderer(getDataBrokerDependency(), getNotificationServiceDependency());

        java.lang.AutoCloseable ret = new java.lang.AutoCloseable() {
            @Override
            public void close() throws Exception {
                openflowSfcRenderer.close();
            }
        };

        LOG.info("SFC OFL2 Renderer initialized: (instance {})", ret);

        return ret;
    }
}

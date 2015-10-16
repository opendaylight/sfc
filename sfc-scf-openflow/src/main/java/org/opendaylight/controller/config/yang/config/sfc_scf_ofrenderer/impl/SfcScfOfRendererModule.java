/**
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.controller.config.yang.config.sfc_scf_ofrenderer.impl;

import org.opendaylight.sfc.scfofrenderer.SfcScfOfRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcScfOfRendererModule extends org.opendaylight.controller.config.yang.config.sfc_scf_ofrenderer.impl.AbstractSfcScfOfRendererModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfRendererModule.class);

    public SfcScfOfRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcScfOfRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                 org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                 org.opendaylight.controller.config.yang.config.sfc_scf_ofrenderer.impl.SfcScfOfRendererModule oldModule,
                                 java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("SFC SCF OF Renderer initializing");

        final SfcScfOfRenderer openflowSfcRenderer = new SfcScfOfRenderer(getDataBrokerDependency(), getNotificationServiceDependency());

        java.lang.AutoCloseable ret = new java.lang.AutoCloseable() {
            @Override
            public void close() throws Exception {
                openflowSfcRenderer.close();
            }
        };

        LOG.info("SFC SCF OF Renderer initialized: (instance {})", ret);

        return ret;
    }
}

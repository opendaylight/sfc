/**
 * Copyright (c) 2016 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_vpp_renderer.impl;

import org.opendaylight.sfc.sfc_vpp_renderer.SfcVppRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcVppRendererModule extends org.opendaylight.controller.config.yang.config.sfc_vpp_renderer.impl.AbstractSfcVppRendererModule {
    private static final Logger LOG = LoggerFactory.getLogger(SfcVppRendererModule.class);

    public SfcVppRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcVppRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_vpp_renderer.impl.SfcVppRendererModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Initializing SFC VPP renderer module ... ");
        SfcVppRenderer renderer = new SfcVppRenderer(getDataBrokerDependency(), getBindingRegistryDependency());

        LOG.info("SFC VPP renderer module initialized");

        java.lang.AutoCloseable autoClosable = new java.lang.AutoCloseable() {
            @Override
            public void close() {
                renderer.unregisterListeners();
                LOG.info("SFC VPP renderer listeners closed");
            }
        };

        return autoClosable;
    }

}

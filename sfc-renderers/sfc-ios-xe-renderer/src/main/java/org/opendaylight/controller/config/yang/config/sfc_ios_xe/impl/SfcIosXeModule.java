/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_ios_xe.impl;

import org.opendaylight.sfc.sfc_ios_xe.provider.SfcIosXeRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcIosXeModule extends org.opendaylight.controller.config.yang.config.sfc_ios_xe.impl.AbstractSfcIosXeModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcIosXeModule.class);

    public SfcIosXeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcIosXeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_ios_xe.impl.SfcIosXeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Initializing SFC IOS-XE renderer module ... ");

        SfcIosXeRenderer renderer = new SfcIosXeRenderer(getDataBrokerDependency(), getBindingRegistryDependency());

        LOG.info("SFC IOS-XE renderer module initialized");

        final class AutoCloseableSfcOvs implements AutoCloseable {

            @Override
            public void close() {
                renderer.unregisterListeners();
                LOG.info("IOS-XE renderer listeners closed");
            }
        }

        return new AutoCloseableSfcOvs();
    }

}

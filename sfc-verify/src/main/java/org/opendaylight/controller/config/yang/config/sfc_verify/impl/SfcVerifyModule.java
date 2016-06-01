/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_verify.impl;

import org.opendaylight.sfc.sfc_verify.SfcVerify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcVerifyModule extends org.opendaylight.controller.config.yang.config.sfc_verify.impl.AbstractSfcVerifyModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVerifyModule.class);

    public SfcVerifyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                       org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcVerifyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                           org.opendaylight.controller.config.yang.config.sfc_verify.impl.SfcVerifyModule oldModule,
                           java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        LOG.warn("SRI:Initializing SFC Verify module ... ");

        SfcVerify sfcverify = new SfcVerify(getDataBrokerDependency(), getBindingRegistryDependency());

        LOG.warn("SRI:Initialized SFC Verify module ... ");

        final class AutoCloseableSfcVerify implements AutoCloseable {

            @Override
            public void close() {
                sfcverify.unregisterListeners();
                sfcverify.close();
                LOG.info("SFC Verify listeners closed");
            }
        }

        return new AutoCloseableSfcVerify();
    }
}

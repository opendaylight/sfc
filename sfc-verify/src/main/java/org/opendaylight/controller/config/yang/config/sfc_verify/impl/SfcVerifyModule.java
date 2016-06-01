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

    private static final class AutoCloseableSfcVerify implements AutoCloseable {
        SfcVerify sfcverify;

        AutoCloseableSfcVerify (SfcVerify sfcverify) {
            this.sfcverify = sfcverify;
        }

        @Override
        public void close() {
            sfcverify.unregisterListeners();
            sfcverify.close();
            LOG.info("SFC verification listeners closed");
        }
    }


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

        LOG.info("SFC Verification Module initializing");

        final SfcVerify sfcverify = new SfcVerify(getDataBrokerDependency(), getBindingRegistryDependency());

        java.lang.AutoCloseable ret = new AutoCloseableSfcVerify(sfcverify);

        LOG.info("SFC Verification Module initialized: (instance {})", ret);

        return ret;
    }
}

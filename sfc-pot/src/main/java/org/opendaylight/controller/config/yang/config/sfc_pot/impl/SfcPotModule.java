/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_pot.impl;

import org.opendaylight.sfc.pot.provider.SfcPot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcPotModule extends org.opendaylight.controller.config.yang.config.sfc_pot.impl.AbstractSfcPotModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotModule.class);

    private static final class AutoCloseableSfcPot implements AutoCloseable {
        SfcPot sfcpot;

        AutoCloseableSfcPot (SfcPot sfcpot) {
            this.sfcpot = sfcpot;
        }

        @Override
        public void close() {
            sfcpot.unregisterListeners();
            sfcpot.close();
            LOG.info("SFC Proof of Transit listeners closed");
        }
    }


    public SfcPotModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                       org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcPotModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                           org.opendaylight.controller.config.yang.config.sfc_pot.impl.SfcPotModule oldModule,
                           java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        LOG.info("SFC Proof of Transit Module initializing");

        final SfcPot sfcpot = new SfcPot(getDataBrokerDependency(), getBindingRegistryDependency());

        java.lang.AutoCloseable ret = new AutoCloseableSfcPot(sfcpot);

        LOG.info("SFC Proof of Transit Module initialized: (instance {})", ret);

        return ret;
    }
}

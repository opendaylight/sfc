/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.config.yang.config.sfc_vnfm.impl;

import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class SfcVnfmModule extends org.opendaylight.controller.config.yang.config.sfc_vnfm.impl.AbstractSfcVnfmModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVnfmModule.class);


    public SfcVnfmModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcVnfmModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_vnfm.impl.SfcVnfmModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();
        LOG.info("SFC Vnf module initialized");

        // close()
        final class AutoCloseableSfcVnfm implements AutoCloseable {

            @Override
            public void close() {

                try {
                    opendaylightSfc.close();
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("\nFailed to close SfcVnfModule instance {} cleanly", this);
                }
                LOG.info("SfcVnfModule (instance {}) torn down", this);
            }
        }

        return new AutoCloseableSfcVnfm();
    }

}

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.config.yang.config.sfc_netconf.impl;

import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_netconf.provider.listener.SfcNetconfNodeDataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.concurrent.ExecutionException;

public class SfcNetconfModule extends org.opendaylight.controller.config.yang.config.sfc_netconf.impl.AbstractSfcNetconfModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfModule.class);

    public SfcNetconfModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcNetconfModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_netconf.impl.SfcNetconfModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();

        final SfcNetconfNodeDataListener sfcNetconfNodeDataListener = new SfcNetconfNodeDataListener(opendaylightSfc);

        LOG.info("SFC Netconf module initialized");

        final class AutoCloseableSfcOvs implements AutoCloseable {

            @Override
            public void close() {
                sfcNetconfNodeDataListener.getDataChangeListenerRegistration().close();

/*                try {
                    opendaylightSfc.close();
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("\nFailed to close SfcNetconfModule instance {} cleanly", this);
                }*/
                LOG.info("SfcNetconfModule (instance {}) torn down", this);
            }
        }

        return new AutoCloseableSfcOvs();
    }

}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.sfc_sb_rest_provider.impl;

import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sbrest.provider.listener.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class SfcSbRestProviderModule extends AbstractSfcSbRestProviderModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcSbRestProviderModule.class);

    public SfcSbRestProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcSbRestProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_sb_rest_provider.impl.SfcSbRestProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();

        final SbRestSffDataListener sbRestSffDataListener = new SbRestSffDataListener(opendaylightSfc);
        final SbRestSfpDataListener sbRestSfpDataListener = new SbRestSfpDataListener(opendaylightSfc);
        final SbRestSfpEntryDataListener sbRestSfpEntryDataListener = new SbRestSfpEntryDataListener(opendaylightSfc);
        final SbRestSfEntryDataListener sbRestSfEntryDataListener = new SbRestSfEntryDataListener(opendaylightSfc);
        final SbRestAclDataListener sbRestAclDataListener = new SbRestAclDataListener(opendaylightSfc);

        // close()
        final class AutoCloseableSfcSbRest implements AutoCloseable {

            @Override
            public void close() {
                sbRestSffDataListener.getDataChangeListenerRegistration().close();
                sbRestSfpDataListener.getDataChangeListenerRegistration().close();
                sbRestSfpEntryDataListener.getDataChangeListenerRegistration().close();
                sbRestSfEntryDataListener.getDataChangeListenerRegistration().close();
                sbRestAclDataListener.getDataChangeListenerRegistration().close();

                try {
                    opendaylightSfc.close();
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("\nFailed to close SfcSbRestProvider instance {} cleanly", this);
                }
                LOG.info("SfcSbRestProvider (instance {}) torn down", this);
            }
        }

        return new AutoCloseableSfcSbRest();
    }

}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.sfc_sb_rest_provider.impl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sbrest.provider.listener.SbRestAclEntryDataListener;
import org.opendaylight.sfc.sbrest.provider.listener.SbRestRspEntryDataListener;
import org.opendaylight.sfc.sbrest.provider.listener.SbRestScfEntryDataListener;
import org.opendaylight.sfc.sbrest.provider.listener.SbRestSfEntryDataListener;
import org.opendaylight.sfc.sbrest.provider.listener.SbRestSffEntryDataListener;
import org.opendaylight.sfc.sbrest.provider.listener.SbRestSfgEntryDataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        final SbRestSfEntryDataListener sbRestSfEntryDataListener = new SbRestSfEntryDataListener(opendaylightSfc);
        final SbRestSfgEntryDataListener sbRestSfgEntryDataListener = new SbRestSfgEntryDataListener(opendaylightSfc);
        final SbRestSffEntryDataListener sbRestSffEntryDataListener = new SbRestSffEntryDataListener(opendaylightSfc);
        final SbRestRspEntryDataListener sbRestRspEntryDataListener = new SbRestRspEntryDataListener(opendaylightSfc);
        final SbRestAclEntryDataListener sbRestAclEntryDataListener = new SbRestAclEntryDataListener(opendaylightSfc);
        final SbRestScfEntryDataListener sbRestScfEntryDataListener = new SbRestScfEntryDataListener(opendaylightSfc);

        // close()
        final class AutoCloseableSfcSbRest implements AutoCloseable {

            @Override
            public void close() {
                sbRestSfEntryDataListener.getDataChangeListenerRegistration().close();
                sbRestSffEntryDataListener.getDataChangeListenerRegistration().close();
                sbRestRspEntryDataListener.getDataChangeListenerRegistration().close();
                sbRestAclEntryDataListener.getDataChangeListenerRegistration().close();
                sbRestScfEntryDataListener.getDataChangeListenerRegistration().close();

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

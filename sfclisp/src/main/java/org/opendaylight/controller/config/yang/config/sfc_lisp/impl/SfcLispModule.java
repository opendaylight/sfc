/*
 * Copyright (c) 2015 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_lisp.impl;

import java.util.concurrent.ExecutionException;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_lisp.provider.LispUpdater;
import org.opendaylight.sfc.sfc_lisp.provider.listener.SfcLispRspEntryDataListener;
import org.opendaylight.sfc.sfc_lisp.provider.listener.SfcLispSfEntryDataListener;
import org.opendaylight.sfc.sfc_lisp.provider.listener.SfcLispSffDataListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.OdlMappingserviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcLispModule extends org.opendaylight.controller.config.yang.config.sfc_lisp.impl.AbstractSfcLispModule {
    private static final Logger LOG = LoggerFactory.getLogger(SfcLispModule.class);

    public SfcLispModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcLispModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_lisp.impl.SfcLispModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
        OdlMappingserviceService lfmService = getRpcRegistryDependency().getRpcService(OdlMappingserviceService.class);
        if (lfmService == null) {
            LOG.warn("lfmService is NULL!!");
        }
        final LispUpdater lispUpdater = new LispUpdater(lfmService);

        // listeners
        final SfcLispSffDataListener sfcProviderSffDataListener = new SfcLispSffDataListener(odlSfc, lispUpdater);
        final SfcLispSfEntryDataListener sfcProviderSfEntryDataListener = new SfcLispSfEntryDataListener(odlSfc, lispUpdater);
        final SfcLispRspEntryDataListener sfcLispRspEntryDataListener = new SfcLispRspEntryDataListener(odlSfc, lispUpdater);

        LOG.info("SFC LISP module initialized");

        final class AutoCloseableSfcLisp implements AutoCloseable {

            @Override
            public void close() throws Exception {
                try {
                    odlSfc.close();
                    sfcProviderSfEntryDataListener.closeDataChangeListener();
                    sfcProviderSffDataListener.closeDataChangeListener();
                    sfcLispRspEntryDataListener.closeDataChangeListener();
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("\nFailed to close SfcLispModule instance {}", this);
                }
                LOG.info("SfcLispModule (instance {}) closed", this);
            }

        }

        return new AutoCloseableSfcLisp();
    }

}

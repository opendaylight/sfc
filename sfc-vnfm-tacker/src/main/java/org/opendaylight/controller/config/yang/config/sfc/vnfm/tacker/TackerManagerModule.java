/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.config.yang.config.sfc.vnfm.tacker;

import org.opendaylight.sfc.tacker.api.TackerManager;
import org.opendaylight.sfc.tacker.dto.Auth;
import org.opendaylight.sfc.tacker.dto.PasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TackerManagerModule
        extends org.opendaylight.controller.config.yang.config.sfc.vnfm.tacker.AbstractTackerManagerModule {

    private static final Logger LOG = LoggerFactory.getLogger(TackerManagerModule.class);

    public TackerManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TackerManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.sfc.vnfm.tacker.TackerManagerModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        PasswordCredentials credentials = new PasswordCredentials(getSfcVnfmTackerName(), getSfcVnfmTackerPassword());
        Auth auth = Auth.builder().setTenantName(getSfcVnfmTackerTenant()).setPasswordCredentials(credentials).build();

        TackerManager sfcVnfManagerProvider = TackerManager.builder()
            .setAuth(auth)
            .setBaseUri(getSfcVnfmUri())
            .setKeystonePort(getSfcVnfmKeystonePort())
            .setTackerPort(getSfcVnfmTackerPort())
            .build();

        LOG.info("{} successfully started.", TackerManager.class.getCanonicalName());
        return sfcVnfManagerProvider;
    }

}

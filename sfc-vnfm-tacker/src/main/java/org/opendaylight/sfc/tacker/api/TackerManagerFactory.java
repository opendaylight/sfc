/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.tacker.api;

import com.google.common.base.Preconditions;
import org.opendaylight.sfc.tacker.dto.Auth;
import org.opendaylight.sfc.tacker.dto.PasswordCredentials;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sfc.vnfm.tacker.config.rev170724.VnfmTackerConfig;

/**
 * Factory for creating TackerManager instances.
 *
 * @author Thomas Pantelis
 */
public class TackerManagerFactory {
    public TackerManager newInstance(final VnfmTackerConfig config) {
        Preconditions.checkNotNull(config);
        PasswordCredentials credentials = new PasswordCredentials(config.getSfcVnfmTackerName(),
                config.getSfcVnfmTackerPassword());
        Auth auth = Auth.builder().setTenantName(config.getSfcVnfmTackerTenant())
                .setPasswordCredentials(credentials).build();

        return TackerManager.builder().setAuth(auth).setBaseUri(config.getSfcVnfmUri())
                .setKeystonePort(config.getSfcVnfmKeystonePort()).setTackerPort(config.getSfcVnfmTackerPort()).build();
    }
}

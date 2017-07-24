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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sfc.vnfm.tacker.config.rev170724.VnfmTackerConfig;

/**
 * Factory for creating TackerManager instances.
 *
 * @author Thomas Pantelis
 */
public class TackerManagerFactory {

    private static final String ILLEGAL_CONFIG_MSG = "The SFC Vnfm Tacker application must be properly configured";

    public AutoCloseableSfcVnfManager newInstance(final VnfmTackerConfig config) {
        Preconditions.checkNotNull(config);

        if (config.getSfcVnfmUri() == null || config.getSfcVnfmKeystonePort() == null
                || config.getSfcVnfmTackerPort() == null) {
            return new AutoCloseableSfcVnfManager() {
                @Override
                public StatisticByTimestamp getSfStatistics(ServiceFunction sf) {
                    throw new IllegalStateException(ILLEGAL_CONFIG_MSG);
                }

                @Override
                public boolean deleteSf(ServiceFunction sf) {
                    throw new IllegalStateException(ILLEGAL_CONFIG_MSG);
                }

                @Override
                public boolean createSf(ServiceFunctionType sfType) {
                    throw new IllegalStateException(ILLEGAL_CONFIG_MSG);
                }

                @Override
                public void close() {
                    // NOOP
                }
            };
        }

        PasswordCredentials credentials = new PasswordCredentials(config.getSfcVnfmTackerName(),
                config.getSfcVnfmTackerPassword());
        Auth auth = Auth.builder().setTenantName(config.getSfcVnfmTackerTenant())
                .setPasswordCredentials(credentials).build();

        return TackerManager.builder().setAuth(auth).setBaseUri(config.getSfcVnfmUri())
                .setKeystonePort(config.getSfcVnfmKeystonePort()).setTackerPort(config.getSfcVnfmTackerPort()).build();
    }
}

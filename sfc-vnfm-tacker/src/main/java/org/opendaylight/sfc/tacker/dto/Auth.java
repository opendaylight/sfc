/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class Auth {

    public String tenantName;
    public PasswordCredentials passwordCredentials;

    private Auth(AuthBuilder builder) {
        this.tenantName = builder.tenantName;
        this.passwordCredentials = builder.passwordCredentials;
    }

    public static class AuthBuilder {

        private String tenantName;
        private PasswordCredentials passwordCredentials;

        public AuthBuilder setTenantName(String tenantName) {
            this.tenantName = tenantName;
            return this;
        }

        public AuthBuilder setPasswordCredentials(PasswordCredentials passwordCredentials) {
            this.passwordCredentials = passwordCredentials;
            return this;
        }

        public Auth build() {
            return new Auth(this);
        }
    }
}

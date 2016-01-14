/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class Tenant {

    private String desription;
    private boolean enabled;
    private String id;
    private String name;

    // used by GSON
    private Tenant() {}

    private Tenant(TenantBuilder builder) {
        this.desription = builder.getDesription();
        this.enabled = builder.isEnabled();
        this.id = builder.getId();
        this.name = builder.getName();
    }

    public static TenantBuilder builder() {
        return new TenantBuilder();
    }

    public String getDesription() {
        return desription;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static class TenantBuilder {

        private String desription;
        private boolean enabled;
        private String id;
        private String name;

        public String getDesription() {
            return desription;
        }

        public TenantBuilder setDesription(String desription) {
            this.desription = desription;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public TenantBuilder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getId() {
            return id;
        }

        public TenantBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public TenantBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public Tenant build() {
            return new Tenant(this);
        }
    }
}

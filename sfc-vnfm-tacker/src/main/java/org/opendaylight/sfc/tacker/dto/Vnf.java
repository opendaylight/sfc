/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class Vnf {

    private String status;
    private String name;
    private String tenant_id;
    private String description;
    private String instance_id;
    private String mgmt_url;
    private Attributes attributes;
    private String id;
    private String vnfd_id;

    // used by GSON
    private Vnf() {}

    private Vnf(VnfBuilder builder) {
        status = builder.getStatus();
        name = builder.getName();
        tenant_id = builder.getTenantId();
        description = builder.getDescription();
        instance_id = builder.getInstanceId();
        mgmt_url = builder.getMgmtUrl();
        attributes = builder.getAttributes();
        id = builder.getId();
        vnfd_id = builder.getVnfdId();
    }

    public static VnfBuilder builder() {
        return new VnfBuilder();
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getTenantId() {
        return tenant_id;
    }

    public String getDescription() {
        return description;
    }

    public String getInstanceId() {
        return instance_id;
    }

    public String getMgmtUrl() {
        return mgmt_url;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public String getId() {
        return id;
    }

    public String getVnfdId() {
        return vnfd_id;
    }

    public static class VnfBuilder {

        private String status;
        private String name;
        private String tenant_id;
        private String description;
        private String instance_id;
        private String mgmt_url;
        private Attributes attributes;
        private String id;
        private String vnfd_id;

        public String getStatus() {
            return status;
        }

        public VnfBuilder setStatus(String status) {
            this.status = status;
            return this;
        }

        public String getName() {
            return name;
        }

        public VnfBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public String getTenantId() {
            return tenant_id;
        }

        public VnfBuilder setTenantId(String tenant_id) {
            this.tenant_id = tenant_id;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public VnfBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getInstanceId() {
            return instance_id;
        }

        public VnfBuilder setInstanceId(String instance_id) {
            this.instance_id = instance_id;
            return this;
        }

        public String getMgmtUrl() {
            return mgmt_url;
        }

        public VnfBuilder setMgmtUrl(String mgmt_url) {
            this.mgmt_url = mgmt_url;
            return this;
        }

        public Attributes getAttributes() {
            return attributes;
        }

        public VnfBuilder setAttributes(Attributes attributes) {
            this.attributes = attributes;
            return this;
        }

        public String getId() {
            return id;
        }

        public VnfBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public String getVnfdId() {
            return vnfd_id;
        }

        public VnfBuilder setVnfdId(String vnfd_id) {
            this.vnfd_id = vnfd_id;
            return this;
        }

        public Vnf build() {
            return new Vnf(this);
        }
    }
}

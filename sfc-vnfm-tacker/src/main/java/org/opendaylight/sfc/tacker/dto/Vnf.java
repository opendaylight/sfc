/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.tacker.dto;

import com.google.gson.annotations.SerializedName;

public final class Vnf {

    private String status;
    private String name;

    @SerializedName("tenant_id")
    private String tenantId;
    private String description;

    @SerializedName("instance_id")
    private String instanceId;

    @SerializedName("mgmt_url")
    private String mgmtUrl;
    private Attributes attributes;
    private String id;

    @SerializedName("vnfd_id")
    private String vnfdId;

    // used by GSON
    private Vnf() {

    }

    private Vnf(VnfBuilder builder) {
        status = builder.getStatus();
        name = builder.getName();
        tenantId = builder.getTenantId();
        description = builder.getDescription();
        instanceId = builder.getInstanceId();
        mgmtUrl = builder.getMgmtUrl();
        attributes = builder.getAttributes();
        id = builder.getId();
        vnfdId = builder.getVnfdId();
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
        return tenantId;
    }

    public String getDescription() {
        return description;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getMgmtUrl() {
        return mgmtUrl;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public String getId() {
        return id;
    }

    public String getVnfdId() {
        return vnfdId;
    }

    public static class VnfBuilder {

        private String status;
        private String name;
        private String tenantId;
        private String description;
        private String instanceId;
        private String mgmtUrl;
        private Attributes attributes;
        private String id;
        private String vnfdId;

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
            return tenantId;
        }

        public VnfBuilder setTenantId(String tenantId) {
            this.tenantId = tenantId;
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
            return instanceId;
        }

        public VnfBuilder setInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public String getMgmtUrl() {
            return mgmtUrl;
        }

        public VnfBuilder setMgmtUrl(String mgmtUrl) {
            this.mgmtUrl = mgmtUrl;
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
            return vnfdId;
        }

        public VnfBuilder setVnfdId(String vnfdId) {
            this.vnfdId = vnfdId;
            return this;
        }

        public Vnf build() {
            return new Vnf(this);
        }
    }
}

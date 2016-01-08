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
        tenant_id = builder.getTenant_id();
        description = builder.getDescription();
        instance_id = builder.getInstance_id();
        mgmt_url = builder.getMgmt_url();
        attributes = builder.getAttributes();
        id = builder.getId();
        vnfd_id = builder.getVnfd_id();
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

    public String getTenant_id() {
        return tenant_id;
    }

    public String getDescription() {
        return description;
    }

    public String getInstance_id() {
        return instance_id;
    }

    public String getMgmt_url() {
        return mgmt_url;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public String getId() {
        return id;
    }

    public String getVnfd_id() {
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

        public String getTenant_id() {
            return tenant_id;
        }

        public VnfBuilder setTenant_id(String tenant_id) {
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

        public String getInstance_id() {
            return instance_id;
        }

        public VnfBuilder setInstance_id(String instance_id) {
            this.instance_id = instance_id;
            return this;
        }

        public String getMgmt_url() {
            return mgmt_url;
        }

        public VnfBuilder setMgmt_url(String mgmt_url) {
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

        public String getVnfd_id() {
            return vnfd_id;
        }

        public VnfBuilder setVnfd_id(String vnfd_id) {
            this.vnfd_id = vnfd_id;
            return this;
        }

        public Vnf build() {
            return new Vnf(this);
        }
    }
}

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class Vnf {

    public String status;
    public String name;
    public String tenant_id;
    public String description;
    public String instance_id;
    public String mgmt_url;
    public Attributes attributes;
    public String id;
    public String vnfd_id;

    private Vnf(VnfBuilder builder) {
        status = builder.status;
        name = builder.name;
        tenant_id = builder.tenant_id;
        description = builder.description;
        instance_id = builder.instance_id;
        mgmt_url = builder.mgmt_url;
        attributes = builder.attributes;
        id = builder.id;
        vnfd_id = builder.vnfd_id;
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

        public VnfBuilder setStatus(String status) {
            this.status = status;
            return this;
        }

        public VnfBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public VnfBuilder setTenant_id(String tenant_id) {
            this.tenant_id = tenant_id;
            return this;
        }

        public VnfBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public VnfBuilder setInstance_id(String instance_id) {
            this.instance_id = instance_id;
            return this;
        }

        public VnfBuilder setMgmt_url(String mgmt_url) {
            this.mgmt_url = mgmt_url;
            return this;
        }

        public VnfBuilder setAttributes(Attributes attributes) {
            this.attributes = attributes;
            return this;
        }

        public VnfBuilder setId(String id) {
            this.id = id;
            return this;
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

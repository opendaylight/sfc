/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class Attributes {

    private String service_type;
    private String heat_template;
    private String monitoring_policy;
    private String failure_policy;

    // used by GSON
    private Attributes() {}

    private Attributes(AtributesBuilder atributesBuilder) {
        this.service_type = atributesBuilder.getService_type();
        this.heat_template = atributesBuilder.getHeat_template();
        this.monitoring_policy = atributesBuilder.getMonitoring_policy();
        this.failure_policy = atributesBuilder.getFailure_policy();
    }

    public static AtributesBuilder builder() {
        return new AtributesBuilder();
    }

    public String getService_type() {
        return service_type;
    }

    public String getHeat_template() {
        return heat_template;
    }

    public String getMonitoring_policy() {
        return monitoring_policy;
    }

    public String getFailure_policy() {
        return failure_policy;
    }

    public static class AtributesBuilder {

        private String service_type;
        private String heat_template;
        private String monitoring_policy;
        private String failure_policy;

        public String getService_type() {
            return service_type;
        }

        public AtributesBuilder setService_type(String service_type) {
            this.service_type = service_type;
            return this;
        }

        public String getHeat_template() {
            return heat_template;
        }

        public AtributesBuilder setHeat_template(String heat_template) {
            this.heat_template = heat_template;
            return this;
        }

        public String getMonitoring_policy() {
            return monitoring_policy;
        }

        public AtributesBuilder setMonitoring_policy(String monitoring_policy) {
            this.monitoring_policy = monitoring_policy;
            return this;
        }

        public String getFailure_policy() {
            return failure_policy;
        }

        public AtributesBuilder setFailure_policy(String failure_policy) {
            this.failure_policy = failure_policy;
            return this;
        }

        public Attributes build() {
            return new Attributes(this);
        }
    }
}

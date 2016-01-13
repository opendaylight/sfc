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
        this.service_type = atributesBuilder.getServiceType();
        this.heat_template = atributesBuilder.getHeatTemplate();
        this.monitoring_policy = atributesBuilder.getMonitoringPolicy();
        this.failure_policy = atributesBuilder.getFailurePolicy();
    }

    public static AtributesBuilder builder() {
        return new AtributesBuilder();
    }

    public String getServiceType() {
        return service_type;
    }

    public String getHeatTemplate() {
        return heat_template;
    }

    public String getMonitoringPolicy() {
        return monitoring_policy;
    }

    public String getFailurePolicy() {
        return failure_policy;
    }

    public static class AtributesBuilder {

        private String service_type;
        private String heat_template;
        private String monitoring_policy;
        private String failure_policy;

        public String getServiceType() {
            return service_type;
        }

        public AtributesBuilder setServiceType(String service_type) {
            this.service_type = service_type;
            return this;
        }

        public String getHeatTemplate() {
            return heat_template;
        }

        public AtributesBuilder setHeatTemplate(String heat_template) {
            this.heat_template = heat_template;
            return this;
        }

        public String getMonitoringPolicy() {
            return monitoring_policy;
        }

        public AtributesBuilder setMonitoringPolicy(String monitoring_policy) {
            this.monitoring_policy = monitoring_policy;
            return this;
        }

        public String getFailurePolicy() {
            return failure_policy;
        }

        public AtributesBuilder setFailurePolicy(String failure_policy) {
            this.failure_policy = failure_policy;
            return this;
        }

        public Attributes build() {
            return new Attributes(this);
        }
    }
}

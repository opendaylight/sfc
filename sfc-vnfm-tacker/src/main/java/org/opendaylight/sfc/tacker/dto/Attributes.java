/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class Attributes {

    public String service_type;
    public String heat_template;
    public String monitoring_policy;
    public String failure_policy;

    private Attributes(AtributesBuilder atributesBuilder) {
        this.service_type = atributesBuilder.service_type;
        this.heat_template = atributesBuilder.heat_template;
        this.monitoring_policy = atributesBuilder.monitoring_policy;
        this.failure_policy = atributesBuilder.failure_policy;
    }

    public static class AtributesBuilder {

        private String service_type;
        private String heat_template;
        private String monitoring_policy;
        private String failure_policy;

        public AtributesBuilder setService_type(String service_type) {
            this.service_type = service_type;
            return this;
        }

        public AtributesBuilder setHeat_template(String heat_template) {
            this.heat_template = heat_template;
            return this;
        }

        public AtributesBuilder setMonitoring_policy(String monitoring_policy) {
            this.monitoring_policy = monitoring_policy;
            return this;
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

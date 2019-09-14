/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.tacker.dto;

import com.google.gson.annotations.SerializedName;

public final class Attributes {

    @SerializedName("service_type")
    private String serviceType;

    @SerializedName("heat_template")
    private String heatTemplate;

    @SerializedName("monitoring_policy")
    private String monitoringPolicy;

    @SerializedName("failure_policy")
    private String failurePolicy;

    // used by GSON
    private Attributes() {

    }

    private Attributes(AttributesBuilder attributesBuilder) {
        this.serviceType = attributesBuilder.getServiceType();
        this.heatTemplate = attributesBuilder.getHeatTemplate();
        this.monitoringPolicy = attributesBuilder.getMonitoringPolicy();
        this.failurePolicy = attributesBuilder.getFailurePolicy();
    }

    public static AttributesBuilder builder() {
        return new AttributesBuilder();
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getHeatTemplate() {
        return heatTemplate;
    }

    public String getMonitoringPolicy() {
        return monitoringPolicy;
    }

    public String getFailurePolicy() {
        return failurePolicy;
    }

    public static class AttributesBuilder {

        private String serviceType;
        private String heatTemplate;
        private String monitoringPolicy;
        private String failurePolicy;

        public String getServiceType() {
            return serviceType;
        }

        public AttributesBuilder setServiceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public String getHeatTemplate() {
            return heatTemplate;
        }

        public AttributesBuilder setHeatTemplate(String heatTemplate) {
            this.heatTemplate = heatTemplate;
            return this;
        }

        public String getMonitoringPolicy() {
            return monitoringPolicy;
        }

        public AttributesBuilder setMonitoringPolicy(String monitoringPolicy) {
            this.monitoringPolicy = monitoringPolicy;
            return this;
        }

        public String getFailurePolicy() {
            return failurePolicy;
        }

        public AttributesBuilder setFailurePolicy(String failurePolicy) {
            this.failurePolicy = failurePolicy;
            return this;
        }

        public Attributes build() {
            return new Attributes(this);
        }
    }
}

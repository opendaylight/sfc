/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TackerRequest {

    public static final Gson GSON = new GsonBuilder().create();
    public Auth auth;
    public Vnf vnf;

    private TackerRequest(TackerRequestBuilder builder) {
        auth = builder.auth;
        vnf = builder.vnf;
    }

    public TackerRequest(String jsonRequest) {
        try {
            TackerRequest tackerRequest = GSON.fromJson(jsonRequest, this.getClass());
            this.auth = tackerRequest.auth;
            this.vnf = tackerRequest.vnf;
        } catch (Exception e) {
            this.auth = null;
            this.vnf = null;
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static class TackerRequestBuilder {

        private Auth auth;
        private Vnf vnf;

        public TackerRequestBuilder setAuth(Auth auth) {
            this.auth = auth;
            return this;
        }

        public TackerRequestBuilder setVnf(Vnf vnf) {
            this.vnf = vnf;
            return this;
        }

        public TackerRequest build() {
            return new TackerRequest(this);
        }
    }
}

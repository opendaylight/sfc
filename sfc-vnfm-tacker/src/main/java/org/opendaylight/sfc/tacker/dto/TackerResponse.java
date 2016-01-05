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

public class TackerResponse {

    public static final Gson GSON = new GsonBuilder().create();
    public Vnf vnf;

    private TackerResponse(TackerResponseBuilder builder) {
        vnf = builder.vnf;
    }

    public TackerResponse(String jsonResponse) {
        try {
            this.vnf = GSON.fromJson(jsonResponse, this.getClass()).vnf;
        } catch (Exception e) {
            this.vnf = null;
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static class TackerResponseBuilder {

        private Vnf vnf;

        public TackerResponseBuilder setVnf(Vnf vnf) {
            this.vnf = vnf;
            return this;
        }

        public TackerResponse build() {
            return new TackerResponse(this);
        }
    }
}

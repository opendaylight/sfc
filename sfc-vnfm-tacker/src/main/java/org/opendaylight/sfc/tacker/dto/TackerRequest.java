/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class TackerRequest {

    private Auth auth;
    private Vnf vnf;

    // used by GSON
    private TackerRequest() {}

    private TackerRequest(TackerRequestBuilder builder) {
        auth = builder.getAuth();
        vnf = builder.getVnf();
    }

    public static TackerRequestBuilder builder() {
        return new TackerRequestBuilder();
    }

    public Auth getAuth() {
        return auth;
    }

    public Vnf getVnf() {
        return vnf;
    }

    public static class TackerRequestBuilder {

        private Auth auth;
        private Vnf vnf;

        public Auth getAuth() {
            return auth;
        }

        public TackerRequestBuilder setAuth(Auth auth) {
            this.auth = auth;
            return this;
        }

        public Vnf getVnf() {
            return vnf;
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

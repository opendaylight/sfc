/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class TackerResponse {

    private Vnf vnf;

    // used by GSON
    private TackerResponse() {}

    private TackerResponse(TackerResponseBuilder builder) {
        vnf = builder.getVnf();
    }

    public static TackerResponseBuilder builder() {
        return new TackerResponseBuilder();
    }

    public Vnf getVnf() {
        return vnf;
    }

    public static class TackerResponseBuilder {

        private Vnf vnf;

        public Vnf getVnf() {
            return vnf;
        }

        public TackerResponseBuilder setVnf(Vnf vnf) {
            this.vnf = vnf;
            return this;
        }

        public TackerResponse build() {
            return new TackerResponse(this);
        }
    }
}

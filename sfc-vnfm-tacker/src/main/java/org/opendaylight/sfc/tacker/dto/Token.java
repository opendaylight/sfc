/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

import java.util.Date;

public class Token {

    private Date issued_at;
    private Date expires;
    private String id;
    private Tenant tenant;
    private String[] audit_ids;

    // used by GSON
    private Token() {}

    private Token(TokenBuilder builder) {
        this.issued_at = builder.getIssued_at();
        this.expires = builder.getExpires();
        this.id = builder.getId();
        this.tenant = builder.getTenant();
        this.audit_ids = builder.getAudit_ids();
    }

    public static TokenBuilder builder() {
        return new TokenBuilder();
    }

    public Date getIssued_at() {
        return issued_at;
    }

    public Date getExpires() {
        return expires;
    }

    public String getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String[] getAudit_ids() {
        return audit_ids;
    }

    public static class TokenBuilder {

        private Date issued_at;
        private Date expires;
        private String id;
        private Tenant tenant;
        private String[] audit_ids;

        public Date getIssued_at() {
            return issued_at;
        }

        public TokenBuilder setIssued_at(Date issued_at) {
            this.issued_at = issued_at;
            return this;
        }

        public Date getExpires() {
            return expires;
        }

        public TokenBuilder setExpires(Date expires) {
            this.expires = expires;
            return this;
        }

        public String getId() {
            return id;
        }

        public TokenBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public Tenant getTenant() {
            return tenant;
        }

        public TokenBuilder setTenant(Tenant tenant) {
            this.tenant = tenant;
            return this;
        }

        public String[] getAudit_ids() {
            return audit_ids;
        }

        public TokenBuilder setAudit_ids(String[] audit_ids) {
            this.audit_ids = audit_ids;
            return this;
        }

        public Token build() {
            return new Token(this);
        }
    }
}

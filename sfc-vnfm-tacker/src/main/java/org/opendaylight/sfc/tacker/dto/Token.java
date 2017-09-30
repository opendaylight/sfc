/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Date;

// Suppresses the "May expose internal representation by returning reference to mutable object" findbugs violation for
// the Date and String[] field getters. The alternative is to return a copy from the method but that introduces
// inefficiency.
@SuppressFBWarnings("EI_EXPOSE_REP")
public class Token {

    @SerializedName("issued_at")
    private Date issuedAt;
    private Date expires;
    private String id;
    private Tenant tenant;

    @SerializedName("audit_ids")
    private String[] auditIds;

    // used by GSON
    private Token() {}

    private Token(TokenBuilder builder) {
        this.issuedAt = builder.issuedAt;
        this.expires = builder.expires;
        this.id = builder.id;
        this.tenant = builder.tenant;
        this.auditIds = builder.auditIds;
    }

    public static TokenBuilder builder() {
        return new TokenBuilder();
    }

    public Date getIssuedAt() {
        return issuedAt;
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

    public String[] getAuditIds() {
        return auditIds;
    }

    // Suppresses the  "May expose internal representation by incorporating reference to mutable object" findbugs
    // violation for the Date and String[] field setters. The alternative is to copy it but that introduces
    // inefficiency.
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public static class TokenBuilder {

        private Date issuedAt;
        private Date expires;
        private String id;
        private Tenant tenant;
        private String[] auditIds;

        public TokenBuilder setIssuedAt(Date issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public TokenBuilder setExpires(Date expires) {
            this.expires = expires;
            return this;
        }

        public TokenBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public TokenBuilder setTenant(Tenant tenant) {
            this.tenant = tenant;
            return this;
        }

        public TokenBuilder setAuditIds(String[] auditIds) {
            this.auditIds = auditIds;
            return this;
        }

        public Token build() {
            return new Token(this);
        }
    }
}

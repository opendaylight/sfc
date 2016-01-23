/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class Error {

    private String message;
    private String type;
    private String detail;

    // used by GSON
    private Error() {}

    public Error(ErrorBuilder builder) {
        this.message = builder.getMessage();
        this.type = builder.getType();
        this.detail = builder.getDetail();
    }

    public static ErrorBuilder builder() {
        return new ErrorBuilder();
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }

    public static class ErrorBuilder {

        private String message;
        private String type;
        private String detail;

        public String getMessage() {
            return message;
        }

        public ErrorBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public String getType() {
            return type;
        }

        public ErrorBuilder setType(String type) {
            this.type = type;
            return this;
        }

        public String getDetail() {
            return detail;
        }

        public ErrorBuilder setDetail(String detail) {
            this.detail = detail;
            return this;
        }

        public Error build() {
            return new Error(this);
        }
    }
}

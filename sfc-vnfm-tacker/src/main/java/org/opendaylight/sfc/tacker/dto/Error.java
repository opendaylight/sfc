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
    private int code;
    private String title;

    // used by GSON
    private Error() {}

    public Error(ErrorBuilder builder) {
        this.message = builder.getMessage();
        this.code = builder.getCode();
        this.title = builder.getTitle();
    }

    public static ErrorBuilder builder() {
        return new ErrorBuilder();
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public static class ErrorBuilder {

        private String message;
        private int code;
        private String title;

        public String getMessage() {
            return message;
        }

        public ErrorBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public int getCode() {
            return code;
        }

        public ErrorBuilder setCode(int code) {
            this.code = code;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public ErrorBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Error build() {
            return new Error(this);
        }
    }
}

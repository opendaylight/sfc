/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class PasswordCredentials {

    private String username;
    private String password;

    // used by GSON
    private PasswordCredentials() {}

    /**
     * Constructor for PasswordCredential class with two parameters
     *
     * @param username is used to set the username for authentication
     * @param password is used to set the password for authentication
     */
    public PasswordCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

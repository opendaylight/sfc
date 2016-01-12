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

public class TackerError {

    private static final Gson GSON = new GsonBuilder().create();
    private Error error;

    // used by GSON
    private TackerError() {}

    public TackerError(Error err) {
        this.error = err;
    }

    public String getMessage() {
        return error.getMessage();
    }

    public int getCode() {
        return error.getCode();
    }

    public String getTitle() {
        return error.getTitle();
    }

    public Error getError() {
        return error;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    @Override
    public String toString() {
        if (this.error != null)
            return String.format("%s - %s: %s", getError().getCode(), getError().getTitle(), getError().getMessage());
        else
            return super.toString();
    }
}

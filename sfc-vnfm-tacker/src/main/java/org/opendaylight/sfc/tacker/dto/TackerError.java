/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.tacker.dto;

import com.google.gson.annotations.SerializedName;

public class TackerError {

    @SerializedName("TackerError")
    private Error tackerError;

    // used by GSON
    private TackerError() {

    }

    public TackerError(Error err) {
        this.tackerError = err;
    }

    public String getMessage() {
        return tackerError.getMessage();
    }

    public String getType() {
        return tackerError.getType();
    }

    public String getDetail() {
        return tackerError.getDetail();
    }

    public Error getError() {
        return tackerError;
    }

    @Override
    public String toString() {
        if (this.tackerError != null) {
            return String.format("%s - %s: %s", getError().getType(), getError().getMessage(), getError().getDetail());
        } else {
            return super.toString();
        }
    }
}

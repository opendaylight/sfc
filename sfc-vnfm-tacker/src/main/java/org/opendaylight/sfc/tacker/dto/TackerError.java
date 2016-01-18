/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

public class TackerError {

    private Error TackerError;

    // used by GSON
    private TackerError() {}

    public TackerError(Error err) {
        this.TackerError = err;
    }

    public String getMessage() {
        return TackerError.getMessage();
    }

    public String getType() {
        return TackerError.getType();
    }

    public String getDetail() {
        return TackerError.getDetail();
    }

    public Error getError() {
        return TackerError;
    }

    @Override
    public String toString() {
        if (this.TackerError != null)
            return String.format("%s - %s: %s", getError().getType(), getError().getMessage(), getError().getDetail());
        else
            return super.toString();
    }
}

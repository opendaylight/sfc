/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Coeffs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Indices;

import java.util.ArrayList;
import java.util.List;

/**
 * This template class is used to store south-bound configuration for SFC verification.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationPolyClass
 * @since 2016-05-01
 */
public class SfcVerificationPolyClass {
    private static final Logger LOG = LoggerFactory.getLogger(SfcVerificationPolyClass.class);

    private long  prime;
    private long  secret;
    private long  sfcsize;
    private final ArrayList<Indices> indices;
    private final List<Coeffs> coeffs;
    private final List<Long> shares;

    SfcVerificationPolyClass(long prime, long secret, ArrayList<Indices> indices,
                             List<Coeffs> coeffs, List<Long> shares, long sfcsize) {
        this.prime = prime;
        this.secret = secret;
        this.indices = indices;
        this.coeffs = coeffs;
        this.shares = shares;
        this.sfcsize = sfcsize;
    }

    public List<Coeffs> getCoeffs() {
        return this.coeffs;
    }

    public ArrayList<Indices> getIndices() {
        return this.indices;
    }

    public long getPrime() {
        return this.prime;
    }

    public long getSecret() {
        return this.secret;
    }

    public List<Long> getShares() {
        return this.shares;
    }

    public long getSfcSize() {
        return this.sfcsize;
    }
}

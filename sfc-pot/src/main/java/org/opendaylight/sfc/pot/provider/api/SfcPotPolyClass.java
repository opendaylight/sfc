/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Coeffs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Lpcs;

import java.util.List;

/**
 * This template class is used to store south-bound configuration for SFC verification.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.pot.provider.api.SfcPotPolyClass
 * @since 2016-05-01
 */
public class SfcPotPolyClass {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotPolyClass.class);

    private long  prime;
    private long  secret;
    private long  sfcsize;
    private final List<Coeffs> coeffs;
    private final List<Long> shares;
    private final List<Lpcs> lpcs;

    SfcPotPolyClass(long prime, long secret, List<Coeffs> coeffs,
                    List<Long> shares, List<Lpcs> lpcs, long sfcsize) {
        this.prime = prime;
        this.secret = secret;
        this.coeffs = coeffs;
        this.shares = shares;
        this.lpcs   = lpcs;
        this.sfcsize = sfcsize;
    }

    public List<Coeffs> getCoeffs() {
        return this.coeffs;
    }

    public List<Lpcs> getLpcs() {
        return this.lpcs;
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

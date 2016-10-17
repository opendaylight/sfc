/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import static org.opendaylight.genius.mdsalutil.NwConstants.SCF_SERVICE_INDEX;

import java.math.BigInteger;

/**
 * Holds constants that are used throughout the sfc-genius module.
 */
// TODO move appropriate constants to genius NwConstants
public class SfcGeniusConstants {

    /**
     * SFC service index used on genius binding.
     */
    public static final short SFC_SERVICE_INDEX = SCF_SERVICE_INDEX;

    /**
     * SFC service name used on genius binding.
     */
    public static final String SFC_SERVICE_NAME = "SFC_SERVICE";

    /**
     * Cookie used in the genius dispatcher flow for SFC
     */
    public static final BigInteger COOKIE_SFC_INGRESS_TABLE = new BigInteger("8030000", 16);

    /**
     * Priority used in the genius dispatcher flow for SFC
     */
    public static final int SFC_SERVICE_PRIORITY = 6;

    /**
     * Virtual network identifier used for SFC service.
     */
    public static final int SFC_VNID = 0;

    /**
     * SFC transport ingress table, first table of the SFC pipeline.
     */
    public static final short SFC_TRANSPORT_INGRESS_TABLE = 76;
}

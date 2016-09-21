/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import java.math.BigInteger;

/**
 * Holds constants that are used throughout the sfc-genius module.
 */
public class SfcGeniusConstants {

    public static final BigInteger COOKIE_SFC_INGRESS_TABLE = new BigInteger("8030000", 16);
    public static final int SFC_SERVICE_PRIORITY = 6;
    public static final String SFC_SERVICE_NAME = "SFC_SERVICE";

    /**
     * Virtual network identifier used for SFC service.
     */
    public static final int SFC_VNID = 0;

    /**
     * SFC transport ingress table, first table of the SFC pipeline.
     */
    // TODO: add this constant and other tables constants to genius NwConstants
    public static final short SFC_TRANSPORT_INGRESS_TABLE = 76;
}

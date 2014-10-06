/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.slf4j.Logger;

/**
 * This class is used to for debugging
 *
 * Example:
 *
 * if (SfcProviderDebug.ON) {
 *   ...
 * }
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public final class SfcProviderDebug {

    private SfcProviderDebug()
    {
    }
    public static final boolean ON = false;
    public static String traceStart = "####### Start: {}";
    public static String traceStop  = "####### Stop: {}";
    public static void printTraceStart(Logger LOG)
    {
        LOG.debug(traceStart, Thread.currentThread().getStackTrace()[1]);
    }
    public static void printTraceStop(Logger LOG)
    {
        LOG.debug(traceStop, Thread.currentThread().getStackTrace()[1]);
    }

}
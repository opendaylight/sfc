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

    private static boolean on = false;
    private static String traceStart = "####### Start: {}";
    private static String traceStop  = "####### Stop: {}";
    private static String trace = "####### Trace: {}";

    public static void setON(boolean on)
    {
        SfcProviderDebug.on = on;
    }

    public static boolean isOn()
    {
        return on;
    }

    public static String getTraceStart()
    {
        return traceStart;
    }

    public static void setTraceStart(String traceStart)
    {
        SfcProviderDebug.traceStart = traceStart;
    }

    public static String getTraceStop()
    {
        return traceStop;
    }

    public static void setTraceStop(String traceStop)
    {
        SfcProviderDebug.traceStop = traceStop;
    }

    public static void printTraceStart(Logger log)
    {
        log.debug(traceStart, Thread.currentThread().getStackTrace()[1]);
    }
    public static void printTraceStop(Logger log)
    {
        log.debug(traceStop, Thread.currentThread().getStackTrace()[1]);
    }
}

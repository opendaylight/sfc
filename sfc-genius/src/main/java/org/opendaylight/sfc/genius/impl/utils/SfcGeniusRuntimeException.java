/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

/**
 * Exception thrown when an error or other exception is encountered
 * in the course of completing a result or task within sfc-genius.
 * This exception only serves as a wrapper to hold the real cause
 * of the problem.
 */
public class SfcGeniusRuntimeException extends RuntimeException {

    /**
     * Constructs a {@code SfcGeniusException} with the specified
     * cause.
     *
     * @param cause the cause.
     */
    public SfcGeniusRuntimeException(Throwable cause) {
        super(cause);
    }
}

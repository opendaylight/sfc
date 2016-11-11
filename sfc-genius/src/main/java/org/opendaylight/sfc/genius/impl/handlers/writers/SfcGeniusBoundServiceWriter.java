/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import java.util.concurrent.CompletableFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.sfc.genius.util.servicebinding.GeniusServiceBinder;

/**
 * Helper class to handle SFC service binding through the Genius interface
 * manager data store API.
 */
public class SfcGeniusBoundServiceWriter {

    private final WriteTransaction transaction;

    /**
     * Constructs a {@code SfcGeniusBoundServiceWriter} using the
     * provided {@link WriteTransaction} to perform the required data
     * store modifications.
     *
     * @param transaction the write transaction
     */
    public SfcGeniusBoundServiceWriter(WriteTransaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Bind SFC service from an interface.
     *
     * @param interfaceName the interface name.
     *
     * @return future signaling completion of the operation.
     */
    public CompletableFuture<Void> bindService(String interfaceName) {
        return new GeniusServiceBinder()
                .bindService(transaction,
                        interfaceName,
                        NwConstants.SFC_SERVICE_INDEX,
                        NwConstants.SFC_TRANSPORT_INGRESS_TABLE,
                        SfcGeniusConstants.COOKIE_SFC_INGRESS_TABLE,
                        SfcGeniusConstants.SFC_SERVICE_PRIORITY,
                        NwConstants.SFC_SERVICE_NAME);
    }

    /**
     * Unbind SFC service from an interface.
     *
     * @param interfaceName the interface name.
     * @return future signaling completion of the operation.
     */
    public CompletableFuture<Void> unbindService(String interfaceName) {
        return new GeniusServiceBinder()
                .unbindService(
                        transaction,
                        interfaceName,
                        NwConstants.SFC_SERVICE_INDEX);
    }
}

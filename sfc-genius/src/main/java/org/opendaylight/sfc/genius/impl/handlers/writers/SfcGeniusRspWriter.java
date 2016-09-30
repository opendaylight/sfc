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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper class to handle rendered service path updates through the SFC
 * configuration data store asynchronous API.
 */
public class SfcGeniusRspWriter {

    private final WriteTransaction transaction;

    /**
     * Constructs a {@code SfcGeniusRspWriter} using the
     * provided {@link WriteTransaction} to perform the required data
     * store modifications.
     *
     * @param transaction the write transaction
     */
    public SfcGeniusRspWriter(WriteTransaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Deletes the given rendered service path.
     *
     * @param renderedServicePath the rendered service path.
     * @return the given rendered service path that was deleted.
     */
    public CompletableFuture<RenderedServicePath> deleteRsp(RenderedServicePath renderedServicePath) {
        InstanceIdentifier<RenderedServicePath> rspId = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePath.getKey())
                .build();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, rspId);
        return CompletableFuture.completedFuture(renderedServicePath);
    }

    /**
     * Creates the given rendered service path.
     *
     * @param renderedServicePath the rendered service path.
     * @return future signaling completion of the operation.
     */
    public CompletableFuture<Void> createRsp(RenderedServicePath renderedServicePath) {
        InstanceIdentifier<RenderedServicePath> rspId = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePath.getKey())
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, rspId, renderedServicePath);
        return CompletableFuture.completedFuture(null);
    }
}

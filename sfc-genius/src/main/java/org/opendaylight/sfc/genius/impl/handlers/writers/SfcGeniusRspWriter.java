/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcGeniusRspWriter {

    private final WriteTransaction transaction;
    private final Executor executor;

    public SfcGeniusRspWriter(WriteTransaction transaction, Executor executor) {
        this.transaction = transaction;
        this.executor = executor;
    }

    public CompletableFuture<RenderedServicePath> deleteRsp(RenderedServicePath renderedServicePath) {
        InstanceIdentifier<RenderedServicePath> rspId = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePath.getKey())
                .build();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, rspId);
        return CompletableFuture.completedFuture(renderedServicePath);
    }

    public CompletableFuture<Void> createRsp(RenderedServicePath renderedServicePath) {
        InstanceIdentifier<RenderedServicePath> rspId = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePath.getKey())
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, rspId, renderedServicePath);
        return CompletableFuture.completedFuture(null);
    }
}

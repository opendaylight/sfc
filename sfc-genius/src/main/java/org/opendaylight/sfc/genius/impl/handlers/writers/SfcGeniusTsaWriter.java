/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Helper class to handle terminating service actions through the Genius ITM
 * RPC API. An executor has to be provided to submit the job where the blocking
 * call to the RPC API is made.
 */
public class SfcGeniusTsaWriter {

    private final ItmRpcService itmRpcService;
    private final Executor executor;

    /**
     * Constructs a {@code SfcGeniusSfReader} using the provided
     * {@link ItmRpcService} to invoke the RPC and {@link Executor}
     * where RPC blocking call is performed.
     *
     * @param itmRpcService the read transaction.
     * @param executor the callback executor.
     */
    public SfcGeniusTsaWriter(ItmRpcService itmRpcService, Executor executor) {
        this.itmRpcService = itmRpcService;
        this.executor = executor;
    }

    /**
     * Adds the SFC terminating service action from a specific data plane
     * node.
     *
     * @param dpnId the data plane node identifier.
     * @return future signaling completion.
     */
    public CompletableFuture<Void> createTerminatingServiceAction(BigInteger dpnId) {
        short offset = NwConstants.SFC_TRANSPORT_INGRESS_TABLE;
        CreateTerminatingServiceActionsInput input = new CreateTerminatingServiceActionsInputBuilder()
                .setDpnId(dpnId)
                .setInstruction(Collections.singletonList(MDSALUtil.buildAndGetGotoTableInstruction(offset, 0)))
                .setServiceId(SfcGeniusConstants.SFC_VNID)
                .build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                RpcResult<Void> result = itmRpcService.createTerminatingServiceActions(input).get();
                if (!result.isSuccessful()) {
                    throw new SfcGeniusRuntimeException(
                            new RuntimeException(
                                    "Could not add terminating service action on dara plane node " + dpnId,
                                    result.getErrors().stream().findFirst().map(RpcError::getCause).orElse(null)));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new SfcGeniusRuntimeException(
                        new RuntimeException(
                                "Could not add terminating service action on dara plane node " + dpnId,
                                e));
            }
            return null;
        }, executor);
    }

    /**
     * Removes the SFC terminating service action from a specific data plane
     * node.
     *
     * @param dpnId the data plane node identifier.
     * @return future signaling completion.
     */
    public CompletableFuture<Void> removeTerminatingServiceAction(BigInteger dpnId) {
        RemoveTerminatingServiceActionsInput input = new RemoveTerminatingServiceActionsInputBuilder()
                .setDpnId(dpnId)
                .setServiceId(SfcGeniusConstants.SFC_VNID)
                .build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                RpcResult<Void> result = itmRpcService.removeTerminatingServiceActions(input).get();
                if (!result.isSuccessful()) {
                    throw new SfcGeniusRuntimeException(
                            new RuntimeException(
                                    "Could not remove terminating service action from data plane node" + dpnId,
                                    result.getErrors().stream().findFirst().map(RpcError::getCause).orElse(null)));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new SfcGeniusRuntimeException(
                        new RuntimeException(
                                "Could not remove terminating service action from data plane node" + dpnId,
                                e));
            }
            return null;
        }, executor);
    }
}

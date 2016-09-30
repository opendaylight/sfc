/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper class to the read information regarding a service function path state
 * from the SFC operational data store asynchronous API.
 *
 * @see SfcGeniusReaderAbstract#doRead(LogicalDatastoreType, InstanceIdentifier)
 */
public class SfcGeniusSfpStateReader extends SfcGeniusReaderAbstract {

    /**
     * Constructs a {@code SfcGeniusSfpStateReader} using the provided
     * {@link ReadTransaction} and {@link Executor}.
     *
     * @param readTransaction the read transaction.
     * @param executor the callback executor.
     */
    public SfcGeniusSfpStateReader(ReadTransaction readTransaction, Executor executor) {
        super(readTransaction, executor);
    }

    /**
     * Read the rendered service path names associated to a service function
     * path.
     *
     * @param sfpName the name of the service function path.
     * @return completable future that will hold the names of the rendered
     *         service paths upon completion.
     */
    public CompletableFuture<List<RspName>> readRspNames(SfpName sfpName) {
        InstanceIdentifier<ServiceFunctionPathState> sfpState;
        sfpState = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(sfpName))
                .build();
        return doReadOptional(LogicalDatastoreType.OPERATIONAL, sfpState)
                .thenApply(optionalServiceFunctionPathState -> optionalServiceFunctionPathState
                        .map(ServiceFunctionPathState::getSfpRenderedServicePath)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(SfpRenderedServicePath::getName)
                        .collect(Collectors.toList())
                );
    }
}

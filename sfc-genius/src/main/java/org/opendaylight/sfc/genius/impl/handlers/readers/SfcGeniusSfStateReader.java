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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper class to the read information regarding a service function state
 * from the SFC operational data store asynchronous API.
 *
 * @see SfcGeniusReaderAbstract#doRead(LogicalDatastoreType, InstanceIdentifier)
 */
public class SfcGeniusSfStateReader extends SfcGeniusReaderAbstract {

    /**
     * Constructs a {@code SfcGeniusSfStateReader} using the provided
     * {@link ReadTransaction} and {@link Executor}.
     *
     * @param readTransaction the read transaction.
     * @param executor the callback executor.
     */
    public SfcGeniusSfStateReader(ReadTransaction readTransaction, Executor executor) {
        super(readTransaction, executor);
    }

    /**
     * Read the service function path names associated to a service function.
     *
     * @param sfName the name of the service function.
     * @return completable future that will hold the names of the service
     *         function path names upon completion.
     */
    public CompletableFuture<List<SfpName>> readSfpNames(SfName sfName) {
        InstanceIdentifier<ServiceFunctionState> sfStateId = InstanceIdentifier.builder(ServiceFunctionsState.class)
                .child(ServiceFunctionState.class, new ServiceFunctionStateKey(sfName)).build();
        return doReadOptional(LogicalDatastoreType.OPERATIONAL, sfStateId)
                .thenApply(optionalServiceFunctionState -> optionalServiceFunctionState
                        .map(ServiceFunctionState::getSfServicePath)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(SfServicePath::getName)
                        .collect(Collectors.toList())
                );
    }
}

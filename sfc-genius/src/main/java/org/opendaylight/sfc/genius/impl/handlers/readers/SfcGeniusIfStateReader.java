/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper class to the read the state of interfaces from the
 * IETF-Interface data store asynchronous API.
 *
 * @see SfcGeniusReaderAbstract#doRead(LogicalDatastoreType, InstanceIdentifier)
 */
public class SfcGeniusIfStateReader extends SfcGeniusReaderAbstract {

    /**
     * Constructs a {@code SfcGeniusIfStateReader} using the provided
     * {@link ReadTransaction} and {@link Executor}.
     *
     * @param readTransaction the read transaction.
     * @param executor the callback executor.
     */
    public SfcGeniusIfStateReader(ReadTransaction readTransaction, Executor executor) {
        super(readTransaction, executor);
    }

    /**
     * Read the data plane node identifier where an interface is located.
     *
     * @param interfaceName the name of the interface.
     * @return completable future that will hold the data plane node identifier
     * on completion.
     */
    public CompletableFuture<BigInteger> readDpnId(String interfaceName) {
        InstanceIdentifier<Interface> interfaceIID = InstanceIdentifier.builder(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(interfaceName))
                .build();
        return doRead(LogicalDatastoreType.OPERATIONAL, interfaceIID)
                .thenApply(Interface::getLowerLayerIf)
                .thenApply(SfcGeniusUtils::getDpnIdFromLowerLayerIfList);
    }

}

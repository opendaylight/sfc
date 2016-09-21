/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class to handle mapping information of nodes to interfaces. This
 * mapping information is represented as a {@link Map} where each key is a
 * data plane node identifier and each value is a {@link Set} of the interface
 * names located on the such data plane node.
 */
public class SfcGeniusDpnIfWriter {

    private final Map<BigInteger, Set<String>> dpnInterfaces;

    /**
     * Constructs a {@code SfcGeniusDpnIfWriter} using the provided
     * {@link Map}.
     * @param dpnInterfaces the {@link Map} of data planes to interfaces
     *                      to be handled by this {@code SfcGeniusDpnIfWriter}.
     */
    public SfcGeniusDpnIfWriter(Map<BigInteger, Set<String>> dpnInterfaces) {
        this.dpnInterfaces = dpnInterfaces;
    }

    /**
     * Add an interface from the provided data plane node.
     *
     * @param dpnId the data plane node identifier to add the interface to.
     * @param interfaceName the name of the interface to add.
     *
     * @return Optionally, the data plane node identifier that was also
     * added because it was associated for the first time with an interface.
     */
    public CompletableFuture<Optional<BigInteger>> addInterface(BigInteger dpnId, String interfaceName) {
        Set<String> interfaces = dpnInterfaces.computeIfAbsent(dpnId, k -> new HashSet<>());
        return interfaces.add(interfaceName) && interfaces.size() == 1 ?
                CompletableFuture.completedFuture(Optional.of(dpnId)) :
                CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Remove an interface from the provided data plane node.
     *
     * @param dpnId the data plane node identifier to remove the interface
     *              from.
     * @param interfaceName the name of the interface to remove.
     *
     * @return Optionally, the data plane node identifier that was also
     * removed because it was no longer associated to any interface.
     */
    public CompletableFuture<Optional<BigInteger>> removeInterfaceFromDpn(BigInteger dpnId, String interfaceName) {
        Set<String> interfaces = dpnInterfaces.computeIfPresent(dpnId, (key, value) -> {
            value.remove(interfaceName);
            return value.isEmpty() ? null : value;
        });
        return interfaces == null ?
                CompletableFuture.completedFuture(Optional.of(dpnId)) :
                CompletableFuture.completedFuture(Optional.empty());
    }
}

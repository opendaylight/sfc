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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper class to read data relative to a Service Function configuration from
 * SFC data store API.
 *
 * @see SfcGeniusReaderAbstract#doRead(LogicalDatastoreType, InstanceIdentifier)
 */
public class SfcGeniusSfReader extends SfcGeniusReaderAbstract {

    /**
     * Constructs a {@code SfcGeniusSfReader} using the provided
     * {@link ReadTransaction} and {@link Executor}.
     *
     * @param readTransaction the read transaction.
     * @param executor the callback executor.
     */
    public SfcGeniusSfReader(ReadTransaction readTransaction, Executor executor) {
        super(readTransaction, executor);
    }

    /**
     * Read the service function names that have the given interface configured
     * as a data plane locator.
     *
     * @param interfaceName the interface name.
     * @return a list of service function names, empty list if none.
     */
    public CompletableFuture<List<SfName>> readSfOnInterface(String interfaceName) {
        InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).build();
        return doReadOptional(LogicalDatastoreType.CONFIGURATION, sfsIID)
                .thenApply(optionalServiceFunctions -> optionalServiceFunctions
                        .map(ServiceFunctions::getServiceFunction)
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(serviceFunction -> isServiceFunctionOnInterface(serviceFunction, interfaceName))
                        .map(ServiceFunction::getName)
                        .collect(Collectors.toList()));
    }

    private static boolean isServiceFunctionOnInterface(ServiceFunction serviceFunction, String interfaceName) {
        return serviceFunction.getSfDataPlaneLocator().stream()
                .map(DataPlaneLocator::getLocatorType)
                .filter(locatorType -> locatorType instanceof LogicalInterface)
                .map(locatorType -> (LogicalInterface) locatorType)
                .filter(logicalInterface -> interfaceName.equals(logicalInterface.getInterfaceName()))
                .findFirst()
                .isPresent();
    }

    /**
     * Read the interface names configured as data plane locators of a
     * given service function.
     *
     * @param sfName the service function name.
     * @return a list of interface names, empty list if none.
     */
    public CompletableFuture<List<String>> readInterfacesOfSf(SfName sfName) {
        InstanceIdentifier<ServiceFunction> sfID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, new ServiceFunctionKey(sfName)).build();
        return doRead(LogicalDatastoreType.CONFIGURATION, sfID)
                .thenApply(SfcGeniusSfReader::getInterfacesOfServiceFunction);
    }

    private static List<String> getInterfacesOfServiceFunction(ServiceFunction serviceFunction) {
        List<? extends DataPlaneLocator> locators = serviceFunction.getSfDataPlaneLocator();
        return Optional.ofNullable(locators)
                .orElse(Collections.emptyList())
                .stream()
                .map(DataPlaneLocator::getLocatorType)
                .filter(locatorType -> locatorType instanceof LogicalInterface)
                .map(locatorType -> (LogicalInterface) locatorType)
                .map(LogicalInterface::getInterfaceName)
                .filter(name -> name != null && ! name.isEmpty())
                .collect(Collectors.toList());
    }
}

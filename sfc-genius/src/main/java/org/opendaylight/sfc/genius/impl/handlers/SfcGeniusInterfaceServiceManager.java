/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;


import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 *
 * SFC service binding to logical interface is done through Genius Interface
 * Manager.
 *
 * On any given node, when one or more interfaces bound to SFC service are
 * present, a SFC service terminating action is configured through Genius ITM.
 * Otherwise, the service terminating action is removed.
 *
 * When an interface becomes available after being unavailable due to a
 * node/port transition, any RSPs on which associated service functions participate
 * will be re-rendered.
 *
 * @see "org.opendaylight.genius.itm"
 * @see "org.opendaylight.genius.interfacemanager"
 */
public class SfcGeniusInterfaceServiceManager implements ISfcGeniusInterfaceServiceHandler {

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final Executor executor;
    private final Map<BigInteger, Set<String>> dpnInterfaces;
    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusInterfaceServiceManager.class);

    public SfcGeniusInterfaceServiceManager(DataBroker dataBroker,
                                            RpcProviderRegistry rpcProviderRegistry,
                                            Executor executor) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.executor = executor;
        this.dpnInterfaces = new HashMap<>();
    }


    @Override
    public void bindInterfacesOfServiceFunction(String sfName) {
        // TODO implementation
    }


    @Override
    public void unbindInterfacesOfServiceFunction(String sfName) {
        // TODO implementation
    }

    @Override
    public void interfaceStateUp(String interfaceName, BigInteger dpnId) {
        // TODO implementation
    }

    @Override
    public void interfaceStateDown(String interfaceName, BigInteger nodeId) {
        // TODO implementation
    }


}

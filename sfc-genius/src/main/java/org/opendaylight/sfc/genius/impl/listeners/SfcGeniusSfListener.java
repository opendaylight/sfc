/*
 * Copyright (c) 2017 SUSE LINUX GmbH.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import java.util.Collections;
import java.util.concurrent.Executor;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcGeniusSfListener extends AsyncDataTreeChangeListenerBase<ServiceFunction, SfcGeniusSfListener> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSfListener.class);
    private final SfcGeniusServiceManager interfaceManager;
    private final Executor executor;

    public SfcGeniusSfListener(SfcGeniusServiceManager interfaceManager, Executor executor) {
        super(ServiceFunction.class, SfcGeniusSfListener.class);
        this.interfaceManager = interfaceManager;
        this.executor = executor;
    }

    @Override
    protected InstanceIdentifier<ServiceFunction> getWildCardPath() {
        return InstanceIdentifier.create(ServiceFunctions.class).child(ServiceFunction.class);
    }

    @Override
    protected void remove(InstanceIdentifier<ServiceFunction> key, ServiceFunction removedServiceFunction) {
        LOG.debug("Received service function remove event {}", removedServiceFunction);
        String interfaceName = SfcGeniusDataUtils.getSfLogicalInterface(removedServiceFunction);
        executor.execute(() -> interfaceManager.unbindInterfaces(Collections.singletonList(interfaceName)));
    }

    @Override
    protected void update(InstanceIdentifier<ServiceFunction> key,
                          ServiceFunction serviceFunctionBefore,
                          ServiceFunction serviceFunctionAfter) {
        // noop
    }

    @Override
    protected void add(InstanceIdentifier<ServiceFunction> key, ServiceFunction addedServiceFunction) {
        // noop
    }

    @Override
    protected SfcGeniusSfListener getDataTreeChangeListener() {
        return SfcGeniusSfListener.this;
    }
}

/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import java.util.concurrent.Executor;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.sfc.genius.impl.handlers.ISfcGeniusInterfaceServiceHandler;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for data store changes on service function state data tree.
 * sfc-genius needs to be aware of service functions participation on
 * RSPs.
 */
public class SfcGeniusSfStateListener extends AsyncDataTreeChangeListenerBase<ServiceFunctionState,
        SfcGeniusSfStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSfStateListener.class);
    private final ISfcGeniusInterfaceServiceHandler interfaceManager;
    private final Executor executor;

    public SfcGeniusSfStateListener(ISfcGeniusInterfaceServiceHandler interfaceManager, Executor executor) {
        super(ServiceFunctionState.class, SfcGeniusSfStateListener.class);
        this.interfaceManager = interfaceManager;
        this.executor = executor;
    }

    @Override
    protected InstanceIdentifier<ServiceFunctionState> getWildCardPath() {
        return InstanceIdentifier.create(ServiceFunctionsState.class).child(ServiceFunctionState.class);
    }

    @Override
    protected void remove(InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                          ServiceFunctionState oldSf) {
        // If this SF stops participating in RSPs, we need to unbind from its interfaces
        LOG.debug("Received service function state remove event {} {}", instanceIdentifier, oldSf);
        int numberOfOldPaths = oldSf.getSfServicePath() == null ? 0 : oldSf.getSfServicePath().size();
        if (numberOfOldPaths > 0) {
            String sfName = oldSf.getName().getValue();
            executor.execute(() -> interfaceManager.unbindInterfacesOfServiceFunction(sfName));
        }
    }

    @Override
    protected void update(InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                          ServiceFunctionState oldSf, ServiceFunctionState newSf) {
        // If this SF stops participating in RSPs, we need to unbind from its interfaces
        LOG.debug("Received service function state update event {} {} {}", instanceIdentifier, oldSf, newSf);
        int numberOfNewPaths = newSf.getSfServicePath() == null ? 0 : newSf.getSfServicePath().size();
        int numberOfOldPaths = oldSf.getSfServicePath() == null ? 0 : oldSf.getSfServicePath().size();
        String sfName = newSf.getName().getValue();
        if (numberOfNewPaths > numberOfOldPaths) {
            executor.execute(() -> interfaceManager.bindInterfacesOfServiceFunction(sfName));
        } else if (numberOfNewPaths < numberOfOldPaths && numberOfNewPaths <= 0) {
            executor.execute(() -> interfaceManager.unbindInterfacesOfServiceFunction(sfName));
        }
    }

    @Override
    protected void add(InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                       ServiceFunctionState newSf) {
        // If this SF starts participating in RSPs, we need to bind to its interfaces
        LOG.debug("Received service function state add event {} {}", instanceIdentifier, newSf);
        int numberOfNewPaths = newSf.getSfServicePath() == null ? 0 : newSf.getSfServicePath().size();
        String sfName = newSf.getName().getValue();
        if (numberOfNewPaths > 0) {
            executor.execute(() -> interfaceManager.bindInterfacesOfServiceFunction(sfName));
        }
    }

    @Override
    protected SfcGeniusSfStateListener getDataTreeChangeListener() {
        return SfcGeniusSfStateListener.this;
    }
}

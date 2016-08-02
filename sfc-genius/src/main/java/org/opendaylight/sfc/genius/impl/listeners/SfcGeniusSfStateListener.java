/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.sfc.genius.impl.SfcGeniusSfInterfaceManager;


public class SfcGeniusSfStateListener extends AsyncDataTreeChangeListenerBase<ServiceFunctionState,
        SfcGeniusSfStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSfStateListener.class);
    private final SfcGeniusSfInterfaceManager interfaceManager;

    public SfcGeniusSfStateListener(SfcGeniusSfInterfaceManager interfaceManager) {
        super(ServiceFunctionState.class, SfcGeniusSfStateListener.class);
        this.interfaceManager = interfaceManager;
    }

    @Override
    protected InstanceIdentifier<ServiceFunctionState> getWildCardPath() {
        return InstanceIdentifier.create(ServiceFunctionsState.class).child(ServiceFunctionState.class);
    }

    @Override
    protected void remove(InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                          ServiceFunctionState serviceFunction) {
        // TODO implement
        // If this SF stops participating in RSPs, we need to unbind from its interfaces
    }

    @Override
    protected void update(InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                          ServiceFunctionState serviceFunction, ServiceFunctionState t1) {
        // TODO implement
        // If this SF stops participating in RSPs, we need to unbind from its interfaces
    }

    @Override
    protected void add(InstanceIdentifier<ServiceFunctionState> instanceIdentifier,
                       ServiceFunctionState serviceFunction) {
        // TODO implement
        // If this SF starts participating in RSPs, we need to bind to its interfaces
    }

    @Override
    protected SfcGeniusSfStateListener getDataTreeChangeListener() {
        return SfcGeniusSfStateListener.this;
    }
}

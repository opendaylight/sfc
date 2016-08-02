/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.sfc.genius.impl.SfcGeniusSfInterfaceManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcGeniusInterfaceStateListener extends AsyncDataTreeChangeListenerBase<Interface,
        SfcGeniusInterfaceStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSfStateListener.class);
    private final SfcGeniusSfInterfaceManager interfaceManager;

    public SfcGeniusInterfaceStateListener(SfcGeniusSfInterfaceManager interfaceManager) {
        super(Interface.class, SfcGeniusInterfaceStateListener.class);
        this.interfaceManager = interfaceManager;
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> instanceIdentifier, Interface interfaceState) {
        // TODO implement
        // See {@link org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#remove}
        // VM migration: interface state is removed before being added again
        // RSPs will be re-rendered when the interface state is added again
        // As of now, we cannot delete RSPs at this time because we would not know how to recreate them at a later time
        // Terminating service action might have to be removed from the associated node
    }

    @Override
    protected void update(InstanceIdentifier<Interface> instanceIdentifier, Interface interfaceState, Interface t1) {
        // TODO implement
        // See {@link org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#update}
        // NOT VM migration: VM unavailable for any other reason
        // Do nothing, should be handled by failover
    }

    @Override
    protected void add(InstanceIdentifier<Interface> instanceIdentifier, Interface interfaceState) {
        // TODO implement
        // See {@link org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#remove}
        // VM migration: interface state is added once the VM has migrated
        // RSPs associated with this interface should be re-rendered
        // Terminating service action might have to be added to the associated node
    }

    @Override
    protected SfcGeniusInterfaceStateListener getDataTreeChangeListener() {
        return SfcGeniusInterfaceStateListener.this;
    }
}

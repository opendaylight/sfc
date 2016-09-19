/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import java.math.BigInteger;
import java.util.concurrent.Executor;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.sfc.genius.impl.handlers.ISfcGeniusInterfaceServiceHandler;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusRuntimeException;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Listener for data store changes of interface state data tree. sfc-genius
 * needs to be aware interface state changes that signal a migration of a
 * logical interface from one node/port to another.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7223">
 *     [RFC7223] A YANG Data Model for Interface Management</a>
 * @see "org.opendaylight.genius.interfacemanager"
 */
public class SfcGeniusInterfaceStateListener extends AsyncDataTreeChangeListenerBase<Interface,
        SfcGeniusInterfaceStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSfStateListener.class);
    private final ISfcGeniusInterfaceServiceHandler handler;
    private Executor executor;

    public SfcGeniusInterfaceStateListener(ISfcGeniusInterfaceServiceHandler handler, Executor executor) {
        super(Interface.class, SfcGeniusInterfaceStateListener.class);
        this.handler = handler;
        this.executor = executor;
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> instanceIdentifier, Interface interfaceState) {
        // VM migration: logical interface state is removed while VM migrates to different node/port
        // See org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#remove
        LOG.debug("Received interface state remove event {} {}", instanceIdentifier, interfaceState);
        String interfaceName = interfaceState.getName();
        BigInteger dpnId;
        try {
            dpnId = SfcGeniusUtils.getDpnIdFromLowerLayerIfList(interfaceState.getLowerLayerIf());
        } catch (SfcGeniusRuntimeException e) {
            LOG.debug("Event ignored, could not get underlying dpn id", e);
            return;
        }
        executor.execute(() -> handler.interfaceStateDown(interfaceName, dpnId));
    }

    @Override
    protected void update(InstanceIdentifier<Interface> instanceIdentifier, Interface interfaceState, Interface t1) {
        // NOT VM migration: VM unavailable for any other reason
        // See org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#update
        // Do nothing, should be handled by a failover mechanism
    }

    @Override
    protected void add(InstanceIdentifier<Interface> instanceIdentifier, Interface interfaceState) {
        // VM migration: logical interface state is added once the VM has migrated
        // See org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#remove
        LOG.debug("Received interface state add event {} {}", instanceIdentifier, interfaceState);
        String interfaceName = interfaceState.getName();
        BigInteger dpnId;
        try {
            dpnId = SfcGeniusUtils.getDpnIdFromLowerLayerIfList(interfaceState.getLowerLayerIf());
        } catch (SfcGeniusRuntimeException e) {
            LOG.debug("Event ignored, could not get underlying dpn id", e);
            return;
        }
        executor.execute(() -> handler.interfaceStateUp(interfaceName, dpnId));
    }

    @Override
    protected SfcGeniusInterfaceStateListener getDataTreeChangeListener() {
        return SfcGeniusInterfaceStateListener.this;
    }
}

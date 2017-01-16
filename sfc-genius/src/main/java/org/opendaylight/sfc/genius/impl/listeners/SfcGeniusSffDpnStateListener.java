/*
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderState;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.SffLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.DpnRsps;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.Dpn;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.RspsForDpnid;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.rsps._for.dpnid.Rsps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for data store changes on SFF state DPN data tree.
 * sfc-genius needs to be aware of data plane nodes participation on
 * RSPs.
 */
public class SfcGeniusSffDpnStateListener extends AsyncDataTreeChangeListenerBase<Dpn,
        SfcGeniusSffDpnStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSffDpnStateListener.class);
    private final SfcGeniusServiceManager interfaceManager;
    private final Executor executor;

    public SfcGeniusSffDpnStateListener(SfcGeniusServiceManager interfaceManager, Executor executor) {
        super(Dpn.class, SfcGeniusSffDpnStateListener.class);
        this.interfaceManager = interfaceManager;
        this.executor = executor;
    }

    @Override
    protected InstanceIdentifier<Dpn> getWildCardPath() {
        return InstanceIdentifier.create(ServiceFunctionForwardersState.class)
                .child(ServiceFunctionForwarderState.class)
                .augmentation(SffLogicalSffAugmentation.class)
                .child(DpnRsps.class)
                .child(Dpn.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Dpn> instanceIdentifier, Dpn dpn) {
        LOG.debug("Receive SFF state DPN remove event {} {}", instanceIdentifier, dpn);
        boolean pathsOnDpn = getPathsOnDpn(dpn).isEmpty();
        if (!pathsOnDpn) {
            BigInteger dpnId = dpn.getDpnId().getValue();
            executor.execute(() -> interfaceManager.unbindNode(dpnId));
        }
    }

    @Override
    protected void update(InstanceIdentifier<Dpn> instanceIdentifier, Dpn oldDpn, Dpn updatedDpn) {
        LOG.debug("Receive SFF state DPN update event {} {} {}", instanceIdentifier, oldDpn, updatedDpn);
        BigInteger dpnId = updatedDpn.getDpnId().getValue();
        boolean pathsOnUpdatedDpn = this.getPathsOnDpn(updatedDpn).isEmpty();
        boolean pathsOnOldDpn = this.getPathsOnDpn(oldDpn).isEmpty();
        if (!pathsOnUpdatedDpn && pathsOnOldDpn) {
            executor.execute(() -> interfaceManager.bindNode(dpnId));
        } else if (!pathsOnOldDpn && pathsOnUpdatedDpn) {
            executor.execute(() -> interfaceManager.unbindNode(dpnId));
        }
    }

    @Override
    protected void add(InstanceIdentifier<Dpn> instanceIdentifier, Dpn dpn) {
        LOG.debug("Receive SFF state DPN add event {} {}", instanceIdentifier, dpn);
        boolean pathsOnDpn = getPathsOnDpn(dpn).isEmpty();
        if (!pathsOnDpn) {
            BigInteger dpnId = dpn.getDpnId().getValue();
            executor.execute(() -> interfaceManager.bindNode(dpnId));
        }
    }

    private List<Rsps> getPathsOnDpn(Dpn dpn) {
        return Optional.ofNullable(dpn.getRspsForDpnid())
                .map(RspsForDpnid::getRsps)
                .orElse(Collections.emptyList());
    }

    @Override
    protected SfcGeniusSffDpnStateListener getDataTreeChangeListener() {
        return SfcGeniusSffDpnStateListener.this;
    }
}

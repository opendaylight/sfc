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
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractAsyncDataTreeChangeListener;
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
public class SfcGeniusSffDpnStateListener extends AbstractAsyncDataTreeChangeListener<Dpn> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusSffDpnStateListener.class);
    private final SfcGeniusServiceManager interfaceManager;

    public SfcGeniusSffDpnStateListener(DataBroker dataBroker,
                                        SfcGeniusServiceManager interfaceManager,
                                        ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, getWildCardPath(), executorService);
        this.interfaceManager = interfaceManager;
    }

    private static InstanceIdentifier<Dpn> getWildCardPath() {
        return InstanceIdentifier.create(ServiceFunctionForwardersState.class)
                .child(ServiceFunctionForwarderState.class)
                .augmentation(SffLogicalSffAugmentation.class)
                .child(DpnRsps.class)
                .child(Dpn.class);
    }

    private List<Rsps> getPathsOnDpn(Dpn dpn) {
        return Optional.ofNullable(dpn.getRspsForDpnid())
                .map(RspsForDpnid::getRsps)
                .orElse(Collections.emptyList());
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Dpn> instanceIdentifier, @Nonnull Dpn newDpn) {
        LOG.debug("Receive SFF state DPN add event {}", newDpn);
        boolean pathsOnDpn = getPathsOnDpn(newDpn).isEmpty();
        if (!pathsOnDpn) {
            BigInteger dpnId = newDpn.getDpnId().getValue();
            interfaceManager.bindNode(dpnId);
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Dpn> instanceIdentifier, @Nonnull Dpn removedDpn) {
        LOG.debug("Receive SFF state DPN remove event {}", removedDpn);
        boolean pathsOnDpn = getPathsOnDpn(removedDpn).isEmpty();
        if (!pathsOnDpn) {
            BigInteger dpnId = removedDpn.getDpnId().getValue();
            interfaceManager.unbindNode(dpnId);
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Dpn> instanceIdentifier, @Nonnull Dpn originalDpn,
                       @Nonnull Dpn updatedDpn) {
        LOG.debug("Receive SFF state DPN update event {}", originalDpn, updatedDpn);
        BigInteger dpnId = updatedDpn.getDpnId().getValue();
        boolean pathsOnUpdatedDpn = this.getPathsOnDpn(updatedDpn).isEmpty();
        boolean pathsOnOldDpn = this.getPathsOnDpn(originalDpn).isEmpty();
        if (!pathsOnUpdatedDpn && pathsOnOldDpn) {
            interfaceManager.bindNode(dpnId);
        } else if (!pathsOnOldDpn && pathsOnUpdatedDpn) {
            interfaceManager.unbindNode(dpnId);
        }
    }
}

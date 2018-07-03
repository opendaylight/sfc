/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the Network Topology
 */
package org.opendaylight.sfc.renderers.vpp.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.renderers.vpp.VppNodeManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class VppNodeListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(VppNodeListener.class);

    private final VppNodeManager vppNodeManager;

    @Inject
    public VppNodeListener(DataBroker dataBroker, VppNodeManager nodeManager) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Networks.class)
                .child(Network.class)
                .child(Node.class).build());
        this.vppNodeManager = nodeManager;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Node> instanceIdentifier, @Nonnull Node node) {
        if (vppNodeManager.isCapableNetconfDevice(node)) {
            LOG.debug("Adding node");
            vppNodeManager.updateNode(node);
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Node> instanceIdentifier,
                       @Nonnull Node node) {
        if (vppNodeManager.isCapableNetconfDevice(node)) {
            LOG.debug("Removing node");
            vppNodeManager.removeNode(node);
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Node> instanceIdentifier,
                       @Nonnull Node originalNode, @Nonnull Node updatedNode) {
        if (vppNodeManager.isCapableNetconfDevice(updatedNode)) {
            LOG.info("Updating node");
            vppNodeManager.updateNode(updatedNode);
        }
    }
}

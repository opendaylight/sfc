/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the Network Topology
 */
package org.opendaylight.sfc.iosxe.provider.listener;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.iosxe.provider.renderer.NodeManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NodeListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeListener.class);

    private final NodeManager nodeManager;

    @Inject
    public NodeListener(DataBroker dataBroker, NodeManager nodeManager) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class));
        this.nodeManager = nodeManager;
    }

    @Override
    public void add(@Nonnull Node newNode) {
        update(newNode, newNode);
    }

    @Override
    public void remove(@Nonnull Node removedNode) {
        if (nodeManager.isCapableNetconfDevice(removedNode)) {
            nodeManager.removeNode(removedNode);
        }
    }

    @Override
    public void update(@Nonnull Node originalNode, Node updatedDataObject) {
        if (nodeManager.isCapableNetconfDevice(originalNode)) {
            nodeManager.updateNode(originalNode);
        }
    }
}

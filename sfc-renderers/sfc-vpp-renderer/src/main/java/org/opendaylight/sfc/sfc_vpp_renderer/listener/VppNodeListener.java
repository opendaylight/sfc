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
package org.opendaylight.sfc.sfc_vpp_renderer.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppNodeManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppNodeListener extends AbstractDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(VppNodeListener.class);

    private final ListenerRegistration<VppNodeListener> listenerRegistration;
    private final VppNodeManager vppNodeManager;

    public VppNodeListener(DataBroker dataBroker, VppNodeManager nodeManager) {
        this.vppNodeManager = nodeManager;
        // Register listener
        final DataTreeIdentifier<Node> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class)
                    .child(Node.class));
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    @Override
    protected void add(Node newDataObject) {
        if (vppNodeManager.isCapableNetconfDevice(newDataObject)) {
            LOG.info("Adding node");
            vppNodeManager.updateNode(newDataObject);
        }
    }

    @Override
    protected void remove(Node removedDataObject) {
        if (vppNodeManager.isCapableNetconfDevice(removedDataObject)) {
            LOG.info("Removing node");
            vppNodeManager.removeNode(removedDataObject);
        }
    }

    @Override
    protected void update(Node originalDataObject, Node updatedDataObject) {
        if (vppNodeManager.isCapableNetconfDevice(updatedDataObject)) {
            LOG.info("Updating node");
            vppNodeManager.updateNode(updatedDataObject);
        }
    }
}

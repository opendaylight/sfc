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
package org.opendaylight.sfc.sfc_netconf.provider.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.NodeManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;

public class NodeListener implements DataTreeChangeListener<NetworkTopology> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeListener.class);

    private final ListenerRegistration listenerRegistration;
    private final NodeManager nodeManager;

    public NodeListener(DataBroker dataBroker, NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        // Register listener
        listenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(NetworkTopology.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<NetworkTopology>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<NetworkTopology> modification : changes) {
            DataObjectModification<NetworkTopology> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataAfter() != null) {
                        for (Topology topology : rootNode.getDataAfter().getTopology()) {
                            if (topology == null || topology.getNode() == null) {
                                continue;
                            }
                            topology.getNode().stream().filter(this::isNetconfDevice).forEach(node ->
                                    nodeManager.updateNode(topology, node));
                        }
                        break;
                    }
                case DELETE:
                    if (rootNode.getDataBefore() != null) {
                        for (Topology topology : rootNode.getDataBefore().getTopology()) {
                            if (topology == null || topology.getNode() == null) {
                                continue;
                            }
                            topology.getNode().stream().filter(this::isNetconfDevice).forEach(nodeManager::removeNode);
                        }
                    }
                    break;
            }
        }
    }

    private boolean isNetconfDevice(Node node) {
        NetconfNode netconfAugmentation = node.getAugmentation(NetconfNode.class);
        if (netconfAugmentation == null) {
            LOG.debug("Node {} is not a netconf device", node.getNodeId().getValue());
            return false;
        }
        return true;
    }

    public ListenerRegistration getRegistrationObject() {
        return listenerRegistration;
    }
}

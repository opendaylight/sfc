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
package org.opendaylight.sfc.sfc_ios_xe.provider.listener;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.NodeManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;

public class NodeListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeListener.class);

    private final ListenerRegistration listenerRegistration;
    private final NodeManager nodeManager;

    public NodeListener(DataBroker dataBroker, NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        // Register listener
        listenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(NetworkTopology.class)
                                .child(Topology.class)
                                .child(Node.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<Node> modification : changes) {
            DataObjectModification<Node> rootNode = modification.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataAfter() != null) {
                        Node node = rootNode.getDataAfter();
                        if (nodeManager.isCapableNetconfDevice(node)) {
                            nodeManager.updateNode(node);
                        }
                        break;
                    }
                case DELETE:
                    if (rootNode.getDataBefore() != null) {
                        Node node = rootNode.getDataBefore();
                        if (nodeManager.isCapableNetconfDevice(node)) {
                            nodeManager.removeNode(node);
                        }
                    }
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return listenerRegistration;
    }
}

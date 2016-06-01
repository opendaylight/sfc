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
package org.opendaylight.sfc.sfc_verify.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_verify.provider.SfcVerifyNodeManager;
import org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.ScvProfiles;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;

public class SfcVerifyNodeListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVerifyNodeListener.class);

    private final ListenerRegistration listenerRegistration;
    private final SfcVerifyNodeManager nodeManager;

    public SfcVerifyNodeListener(DataBroker dataBroker, SfcVerifyNodeManager nodeManager) {
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
                        if (isCapableNetconfDevice(node)) {
                            /* Check whether the node advertised
                             * ioam-scv capability and if so, add
                             * subscriptions
                             */
                            if (isIoamSfcvCapableNetconfDevice(node)) {
                                nodeManager.updateNode(node, true);
                            } else {
                                nodeManager.updateNode(node, false);
                            }
                        }
                        break;
                    }
                case DELETE:
                    if (rootNode.getDataBefore() != null) {
                        Node node = rootNode.getDataBefore();
                        if (isCapableNetconfDevice(node)) {
                            nodeManager.removeNode(node);
                        }
                    }
            }
        }
    }

    private boolean isCapableNetconfDevice(Node node) {
        NetconfNode netconfAugmentation = node.getAugmentation(NetconfNode.class);
        if (netconfAugmentation == null) {
            LOG.debug("Node {} is not a netconf device", node.getNodeId().getValue());
            return false;
        }
        AvailableCapabilities capabilities = netconfAugmentation.getAvailableCapabilities();
        // TODO maybe add more specific capability test
        return capabilities.getAvailableCapability()
                .contains("urn:ietf:params:netconf:capability:writable-running:1.0");
    }

    private boolean isIoamSfcvCapableNetconfDevice(Node node) {
        NetconfNode netconfAugmentation = node.getAugmentation(NetconfNode.class);
        if (netconfAugmentation == null) {
            LOG.debug("Node {} is not a netconf device", node.getNodeId().getValue());
            return false;
        }

        List<String> capabilities = netconfAugmentation.
                                    getAvailableCapabilities().
                                    getAvailableCapability();

        if (capabilities.contains(QName.create(ScvProfiles.QNAME, SfcVerificationListener.IOAM_SCV).toString())) {
            LOG.debug("Node {} supports iOAM based SFCV...", node.getNodeId().getValue());
            return true;
        }

        return false;
    }

    public ListenerRegistration getRegistrationObject() {
        return listenerRegistration;
    }
}

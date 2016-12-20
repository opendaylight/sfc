/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.pot.netconf.renderer.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfNodeManager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is used to listen to Netconf Node arrival and exit to help
 * build Netconf Node datastore to enable controller to send these nodes
 * iOAM and other configurations.
 * <p>
 *
 * @version 0.1
 */
public class SfcPotNetconfNodeListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfNodeListener.class);

    private final ListenerRegistration listenerRegistration;
    private final SfcPotNetconfNodeManager nodeManager;
    private List<String> ioamNetconfCapabilities = new ArrayList<>();

    public SfcPotNetconfNodeListener(DataBroker dataBroker, SfcPotNetconfNodeManager nodeManager) {
        this.nodeManager = nodeManager;

        /* Register listener */
        listenerRegistration = dataBroker.registerDataTreeChangeListener(new
            DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).
            child(Node.class).build()), this);

        /* Initialize iOAM Capabilities to check from Node info */
        ioamNetconfCapabilities = initializeIoamNetconfCapabilities();
    }

    private List<String> initializeIoamNetconfCapabilities() {
        final String netconf = "urn:ietf:params:netconf:base:1.0";
        final String ioam_pot = "(urn:cisco:params:xml:ns:yang:sfc-ioam-sb-pot?revision=2017-01-12)sfc-ioam-sb-pot";
        String capabilityEntries[] = {netconf, ioam_pot};
        return Arrays.asList(capabilityEntries);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> modification : changes) {
            DataObjectModification<Node> rootNode = modification.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataAfter() != null) {
                        Node node = rootNode.getDataAfter();
                        if (isIoamCapableNetconfDevice(node)) {
                            nodeManager.updateNode(node);
                        }
                        break;
                    }
                case DELETE:
                    if (rootNode.getDataBefore() != null) {
                        Node node = rootNode.getDataBefore();
                        if (isIoamCapableNetconfDevice(node)) {
                            nodeManager.removeNode(node);
                        }
                    }
            }
        }
    }

    private boolean isIoamCapableNetconfDevice(Node node) {
        NetconfNode netconfAugmentation = node.getAugmentation(NetconfNode.class);
        if (netconfAugmentation == null) {
            LOG.debug("iOAM:PoT:SB:Node {} is not a netconf device", node.getNodeId().getValue());
            return false;
        }

        AvailableCapabilities capabilities = netconfAugmentation.getAvailableCapabilities();

        return capabilities != null && capabilities.getAvailableCapability().stream()
                .map(AvailableCapability::getCapability).collect(Collectors.toList())
                .containsAll(ioamNetconfCapabilities);
    }

    public void closeListenerRegistration() {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }
}

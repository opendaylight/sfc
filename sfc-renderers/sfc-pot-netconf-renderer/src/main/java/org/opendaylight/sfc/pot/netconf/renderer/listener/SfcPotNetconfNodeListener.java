/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.pot.netconf.renderer.listener;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfNodeManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to listen to Netconf Node arrival and exit to help build
 * Netconf Node datastore to enable controller to send these nodes iOAM and
 * other configurations.
 *
 * <p>
 *
 * @version 0.1
 */
@Singleton
public class SfcPotNetconfNodeListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfNodeListener.class);

    private final SfcPotNetconfNodeManager nodeManager;
    private List<String> ioamNetconfCapabilities;

    @Inject
    public SfcPotNetconfNodeListener(DataBroker dataBroker, SfcPotNetconfNodeManager nodeManager) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class).build());
        this.nodeManager = nodeManager;

        /* Initialize iOAM Capabilities to check from Node info */
        this.ioamNetconfCapabilities = initializeIoamNetconfCapabilities();
    }

    private List<String> initializeIoamNetconfCapabilities() {
        final String netconf = "urn:ietf:params:netconf:base:1.0";
        final String ioamPot = "(urn:cisco:params:xml:ns:yang:sfc-ioam-sb-pot?revision=2017-01-12)sfc-ioam-sb-pot";
        String[] capabilityEntries = {netconf, ioamPot};
        return Arrays.asList(capabilityEntries);
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node node) {
        update(instanceIdentifier, node, node);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node node) {
        if (isIoamCapableNetconfDevice(node)) {
            nodeManager.removeNode(node);
        }
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node originalNode,
                       @NonNull Node updatedNode) {
        if (isIoamCapableNetconfDevice(updatedNode)) {
            nodeManager.updateNode(updatedNode);
        }
    }

    private boolean isIoamCapableNetconfDevice(Node node) {
        NetconfNode netconfAugmentation = node.augmentation(NetconfNode.class);
        if (netconfAugmentation == null) {
            LOG.debug("iOAM:PoT:SB:Node {} is not a netconf device", node.getNodeId().getValue());
            return false;
        }

        AvailableCapabilities capabilities = netconfAugmentation.getAvailableCapabilities();

        return capabilities != null && capabilities.getAvailableCapability().stream()
                .map(AvailableCapability::getCapability).collect(Collectors.toList())
                .containsAll(ioamNetconfCapabilities);
    }
}

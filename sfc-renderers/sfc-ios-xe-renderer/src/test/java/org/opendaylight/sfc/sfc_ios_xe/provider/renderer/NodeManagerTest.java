/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.renderer;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeManagerTest {

    private final String nodeId = "nodeId";
    private final String topologyId = "topologyId";
    private NodeManager manager;
    private MountPoint mountPoint;
    private DataBroker dataBroker;
    private BindingAwareBroker bindingAwareBroker;
    private BindingAwareBroker.ProviderContext providerContext;
    private MountPointService mountPointService;
    // Optionals
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<MountPoint> optionalMountPointObject;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<DataBroker> optionalDataBrokerObject;

    @Before
    @SuppressWarnings("unchecked")
    public void init() {
        mountPoint = mock(MountPoint.class);
        dataBroker = mock(DataBroker.class);
        bindingAwareBroker = mock(BindingAwareBroker.class);
        providerContext = mock(BindingAwareBroker.ProviderContext.class);
        mountPointService = mock(MountPointService.class);
        // Optionals
        optionalMountPointObject = mock(Optional.class);
        optionalDataBrokerObject = mock(Optional.class);
    }

    @Test
    public void updateNode_unsuccessful() {
        // Prepare topology
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setTopologyId(new TopologyId(topologyId));
        // Prepare node
        NodeBuilder nodeBuilder = new NodeBuilder();
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connected);
        nodeBuilder.setNodeId(new NodeId(nodeId));
        nodeBuilder.addAugmentation(NetconfNode.class, netconfNodeBuilder.build());

        when(bindingAwareBroker.registerProvider(any(BindingAwareProvider.class))).thenReturn(providerContext);
        when(providerContext.getSALService(any())).thenReturn(mountPointService);
        when(mountPointService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(optionalMountPointObject);

        manager = new NodeManager(dataBroker, bindingAwareBroker);
        manager.updateNode(nodeBuilder.build());

        assertTrue(manager.getActiveMountPoints().isEmpty());
        assertTrue(manager.getConnectedNodes().isEmpty());
    }

    @Test
    public void updateNode_successful() {
        // Prepare topology
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setTopologyId(new TopologyId(topologyId));
        // Prepare node
        NodeBuilder nodeBuilder = new NodeBuilder();
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connected);
        nodeBuilder.setNodeId(new NodeId(nodeId));
        nodeBuilder.addAugmentation(NetconfNode.class, netconfNodeBuilder.build());

        when(bindingAwareBroker.registerProvider(any(BindingAwareProvider.class))).thenReturn(providerContext);
        when(providerContext.getSALService(any())).thenReturn(mountPointService);
        when(mountPointService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(optionalMountPointObject);
        when(mountPoint.getService(eq(DataBroker.class))).thenReturn(optionalDataBrokerObject);

        // Mock getting mountpoint
        when(optionalMountPointObject.isPresent()).thenReturn(true);
        //noinspection OptionalGetWithoutIsPresent
        when(optionalMountPointObject.get()).thenReturn(mountPoint);
        when(optionalDataBrokerObject.isPresent()).thenReturn(true);
        //noinspection OptionalGetWithoutIsPresent
        when(optionalDataBrokerObject.get()).thenReturn(dataBroker);

        manager = new NodeManager(dataBroker, bindingAwareBroker);
        manager.updateNode(nodeBuilder.build());

        assertFalse(manager.getActiveMountPoints().isEmpty());
        assertFalse(manager.getConnectedNodes().isEmpty());
    }

    @Test
    public void updateAndRemoveNode() {
        // Prepare topology
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setTopologyId(new TopologyId(topologyId));
        // Prepare node
        NodeBuilder nodeBuilder = new NodeBuilder();
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connected);
        nodeBuilder.setNodeId(new NodeId(nodeId));
        nodeBuilder.addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        Node testNode = nodeBuilder.build();

        when(bindingAwareBroker.registerProvider(any(BindingAwareProvider.class))).thenReturn(providerContext);
        when(providerContext.getSALService(any())).thenReturn(mountPointService);
        when(mountPointService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(optionalMountPointObject);
        when(mountPoint.getService(eq(DataBroker.class))).thenReturn(optionalDataBrokerObject);

        // Mock getting mountpoint
        when(optionalMountPointObject.isPresent()).thenReturn(true);
        //noinspection OptionalGetWithoutIsPresent
        when(optionalMountPointObject.get()).thenReturn(mountPoint);
        when(optionalDataBrokerObject.isPresent()).thenReturn(true);
        //noinspection OptionalGetWithoutIsPresent
        when(optionalDataBrokerObject.get()).thenReturn(dataBroker);

        manager = new NodeManager(dataBroker, bindingAwareBroker);
        manager.updateNode(testNode);

        assertTrue(manager.getActiveMountPoints().size() == 1);
        assertTrue(manager.getConnectedNodes().size() == 1);

        manager.removeNode(testNode);

        assertTrue(manager.getActiveMountPoints().isEmpty());
        assertTrue(manager.getConnectedNodes().isEmpty());
    }
}

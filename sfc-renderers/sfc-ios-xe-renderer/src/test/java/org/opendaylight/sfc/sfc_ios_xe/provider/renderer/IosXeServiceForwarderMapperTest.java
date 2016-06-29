/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IosXeServiceForwarderMapperTest {

    private DataBroker dataBroker;
    private NodeManager nodeManager;
    private WriteTransaction writeTransaction;
    private IosXeServiceForwarderMapper sffMapper;
    private final String nodeId = "nodeId";
    private final String sffName2 = "forwarder2";
    private final String ipAddress = "10.0.0.1";

    @Before
    public void init() {
        dataBroker = mock(DataBroker.class);
        nodeManager = mock(NodeManager.class);
        writeTransaction = mock(WriteTransaction.class);
    }

    @Test
    public void syncForwarder_create() {
        // Forwarders
        List<ServiceFunctionForwarder> forwarders = new ArrayList<>();
        // SFF without management ip
        ServiceFunctionForwarderBuilder noMgmtIpForwarder = new ServiceFunctionForwarderBuilder();
        String sffName1 = "forwarder1";
        noMgmtIpForwarder.setName(new SffName(sffName1))
                .setKey(new ServiceFunctionForwarderKey(new SffName(sffName1)));
        // Test SFF
        List<SffDataPlaneLocator> dataPlaneLocators = new ArrayList<>();
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder.setLocatorType(new IpBuilder().setIp(new IpAddress(new Ipv4Address(ipAddress)))
                .build());
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        dataPlaneLocators.add(sffDataPlaneLocatorBuilder.build());

        ServiceFunctionForwarderBuilder testSff = new ServiceFunctionForwarderBuilder();
        testSff.setName(new SffName(sffName2))
                .setKey(new ServiceFunctionForwarderKey(new SffName(sffName2)))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)))
                .setSffDataPlaneLocator(dataPlaneLocators);
        forwarders.add(noMgmtIpForwarder.build());
        forwarders.add(testSff.build());
        // Node
        Map<NodeId, Node> nodeMap = new HashMap<>();
        Map<NodeId, DataBroker> nodeDbMap = new HashMap<>();
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(nodeId))
                .setKey(new NodeKey(new NodeId(nodeId)));
        Node node = nodeBuilder.build();
        nodeMap.put(node.getNodeId(), node);
        nodeDbMap.put(node.getNodeId(), dataBroker);

        when(nodeManager.getConnectedNodes()).thenReturn(nodeMap);
        when(nodeManager.getNetconfNodeIp(node)).thenReturn(new IpAddress(new Ipv4Address(ipAddress)));
        when(nodeManager.getActiveMountPoints()).thenReturn(nodeDbMap);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        sffMapper = new IosXeServiceForwarderMapper(dataBroker, nodeManager);
        sffMapper.syncForwarders(forwarders, false);

        verify(nodeManager, times(1)).getConnectedNodes();
        verify(nodeManager,times(1)).getNetconfNodeIp(node);
        verify(nodeManager, times(1)).getActiveMountPoints();
        verify(dataBroker, times(1)).newWriteOnlyTransaction();
    }

    @Test
    public void syncForwarder_delete() {
        // Forwarders
        List<ServiceFunctionForwarder> forwarders = new ArrayList<>();
        // Test SFF
        List<SffDataPlaneLocator> dataPlaneLocators = new ArrayList<>();
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder.setLocatorType(new IpBuilder().setIp(new IpAddress(new Ipv4Address(ipAddress)))
                .build());
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        dataPlaneLocators.add(sffDataPlaneLocatorBuilder.build());

        ServiceFunctionForwarderBuilder testSff = new ServiceFunctionForwarderBuilder();
        testSff.setName(new SffName(sffName2))
                .setKey(new ServiceFunctionForwarderKey(new SffName(sffName2)))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)))
                .setSffDataPlaneLocator(dataPlaneLocators);
        forwarders.add(testSff.build());
        // Node
        Map<NodeId, Node> nodeMap = new HashMap<>();
        Map<NodeId, DataBroker> nodeDbMap = new HashMap<>();
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(nodeId))
                .setKey(new NodeKey(new NodeId(nodeId)));
        Node node = nodeBuilder.build();
        nodeMap.put(node.getNodeId(), node);
        nodeDbMap.put(node.getNodeId(), dataBroker);

        when(nodeManager.getConnectedNodes()).thenReturn(nodeMap);
        when(nodeManager.getNetconfNodeIp(node)).thenReturn(new IpAddress(new Ipv4Address(ipAddress)));
        when(nodeManager.getActiveMountPoints()).thenReturn(nodeDbMap);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        sffMapper = new IosXeServiceForwarderMapper(dataBroker, nodeManager);
        sffMapper.syncForwarders(forwarders, true);

        verify(nodeManager, times(1)).getConnectedNodes();
        verify(nodeManager,times(1)).getNetconfNodeIp(node);
        verify(nodeManager, times(1)).getActiveMountPoints();
        verify(dataBroker, times(1)).newWriteOnlyTransaction();
    }
}

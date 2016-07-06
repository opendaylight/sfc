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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Gre;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IosXeServiceFunctionMapperTest {

    private final String nodeIdString = "nodeId";
    private final String ipAddress = "10.0.0.1";
    private DataBroker dataBroker;
    private NodeManager nodeManager;
    private WriteTransaction writeTransaction;
    private IosXeServiceFunctionMapper sfMapper;

    @Before
    public void init() {
        dataBroker = mock(DataBroker.class);
        nodeManager = mock(NodeManager.class);
        writeTransaction = mock(WriteTransaction.class);
    }

    @Test
    public void syncFunctions_update() {
        String sfName1 = "function1";
        String sfName2 = "function2";
        // Prepare DPL
        SfDataPlaneLocatorBuilder macLocatorType = new SfDataPlaneLocatorBuilder();
        macLocatorType.setLocatorType(new MacBuilder().build());
        SfDataPlaneLocatorBuilder ipLocatorType = new SfDataPlaneLocatorBuilder();
        ipLocatorType.setLocatorType(new IpBuilder().setIp(new IpAddress(new Ipv4Address(ipAddress))).build())
                .setTransport(Gre.class);
        SfDataPlaneLocator macDpl = macLocatorType.build();
        SfDataPlaneLocator ipDpl = ipLocatorType.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList;
        // Service functions
        List<ServiceFunction> serviceFunctions = new ArrayList<>();
        // SF without management IP
        ServiceFunctionBuilder emptySfBuilder = new ServiceFunctionBuilder();
        emptySfBuilder.setName(new SfName(sfName1))
                .setKey(new ServiceFunctionKey(new SfName(sfName1)));
        // SF without data plane locator
        ServiceFunctionBuilder noDplSfBuilder = new ServiceFunctionBuilder();
        noDplSfBuilder.setName(new SfName(sfName2))
                .setKey(new ServiceFunctionKey(new SfName(sfName2)))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)));
        // SF without ip data plane locator
        ServiceFunctionBuilder noIpDplSfBuilder = new ServiceFunctionBuilder();
        dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(macDpl);
        noIpDplSfBuilder.setName(new SfName(sfName2))
                .setKey(new ServiceFunctionKey(new SfName(sfName2)))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)))
                .setSfDataPlaneLocator(dataPlaneLocatorList);
        // Test SF
        ServiceFunctionBuilder testSfBuilder = new ServiceFunctionBuilder();
        dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(ipDpl);
        testSfBuilder.setName(new SfName(sfName2))
                .setKey(new ServiceFunctionKey(new SfName(sfName2)))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)))
                .setSfDataPlaneLocator(dataPlaneLocatorList);
        serviceFunctions.add(emptySfBuilder.build());
        serviceFunctions.add(noDplSfBuilder.build());
        serviceFunctions.add(noIpDplSfBuilder.build());
        serviceFunctions.add(testSfBuilder.build());
        // Node list
        Map<NodeId, Node> nodeMap = new HashMap<>();
        NodeId nodeId = new NodeId(nodeIdString);
        NodeBuilder nodeBuilder = new NodeBuilder();
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setHost(new Host(new IpAddress(new Ipv4Address(ipAddress))));
        nodeBuilder.setNodeId(nodeId)
                .addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        Node node = nodeBuilder.build();
        nodeMap.put(nodeId, node);

        Map<NodeId, DataBroker> nodeWithDataBrokerMap = new HashMap<>();
        nodeWithDataBrokerMap.put(nodeId, dataBroker);

        when(nodeManager.getConnectedNodes()).thenReturn(nodeMap);
        when(nodeManager.getNetconfNodeIp(node)).thenReturn(new IpAddress(new Ipv4Address(ipAddress)));
        when(nodeManager.getActiveMountPoints()).thenReturn(nodeWithDataBrokerMap);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        sfMapper = new IosXeServiceFunctionMapper(dataBroker, nodeManager);
        sfMapper.syncFunctions(serviceFunctions, false);

        verify(nodeManager, times(3)).getConnectedNodes();
        verify(nodeManager, times(3)).getNetconfNodeIp(any(Node.class));
        verify(nodeManager, times(3)).getActiveMountPoints();
        verify(dataBroker, times(1)).newWriteOnlyTransaction();
    }

    @Test
    public void syncFunctions_delete() {
        String sfName2 = "function2";
        sfMapper = new IosXeServiceFunctionMapper(dataBroker, nodeManager);
        // Prepare DPL
        SfDataPlaneLocatorBuilder macLocatorType = new SfDataPlaneLocatorBuilder();
        macLocatorType.setLocatorType(new MacBuilder().build());
        SfDataPlaneLocatorBuilder ipLocatorType = new SfDataPlaneLocatorBuilder();
        ipLocatorType.setLocatorType(new IpBuilder().setIp(new IpAddress(new Ipv4Address(ipAddress))).build())
                .setTransport(Gre.class);
        SfDataPlaneLocator ipDpl = ipLocatorType.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList;
        // Service function
        List<ServiceFunction> serviceFunctions = new ArrayList<>();
        ServiceFunctionBuilder testSfBuilder = new ServiceFunctionBuilder();
        dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(ipDpl);
        testSfBuilder.setName(new SfName(sfName2))
                .setKey(new ServiceFunctionKey(new SfName(sfName2)))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)))
                .setSfDataPlaneLocator(dataPlaneLocatorList);
        serviceFunctions.add(testSfBuilder.build());
        // Node list
        Map<NodeId, Node> nodeMap = new HashMap<>();
        NodeId nodeId = new NodeId(nodeIdString);
        NodeBuilder nodeBuilder = new NodeBuilder();
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setHost(new Host(new IpAddress(new Ipv4Address(ipAddress))));
        nodeBuilder.setNodeId(nodeId)
                .addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        Node node = nodeBuilder.build();
        nodeMap.put(nodeId, node);

        Map<NodeId, DataBroker> nodeWithDataBrokerMap = new HashMap<>();
        nodeWithDataBrokerMap.put(nodeId, dataBroker);

        when(nodeManager.getConnectedNodes()).thenReturn(nodeMap);
        when(nodeManager.getNetconfNodeIp(node)).thenReturn(new IpAddress(new Ipv4Address(ipAddress)));
        when(nodeManager.getActiveMountPoints()).thenReturn(nodeWithDataBrokerMap);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        sfMapper.syncFunctions(serviceFunctions, true);

        verify(nodeManager, times(1)).getConnectedNodes();
        verify(nodeManager, times(1)).getNetconfNodeIp(any(Node.class));
        verify(nodeManager, times(1)).getActiveMountPoints();
        verify(dataBroker, times(1)).newWriteOnlyTransaction();
    }
}

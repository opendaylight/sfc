/*
 * Copyright (c) 2015, 2017 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPITest;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains junit tests for SfcOvsUtil class.
 *
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsUtil
 * @since 2015-04-23
 */

public class SfcOvsUtilTest extends AbstractDataBrokerTest {

    private static final String OVSDB_BRIDGE_PREFIX = "/bridge/"; // copy of private String from
                                                                  // SfcOvsUtil.class
    private static final String IP_ADDRESS = "170.0.0.1";
    private static final String IPV6_ADDRESS = "0F:ED:CB:A9:87:65:43:21";
    private static final String TEST_BRIDGE_NAME = "Bridge Name";
    private static final String TEST_STRING = "Test string";
    private static final SffDataPlaneLocatorName SFF_DATA_PLANE_LOCATOR =
            new SffDataPlaneLocatorName("SFF_DATA_PLANE_LOCATOR test");
    private static final String TEST_DATA_PATH = "12:34:56:78:9A:BC:DE:F0";
    private static final Long  TEST_PORT = 1L;
    private static final SffDataPlaneLocatorName DPL_NAME = new SffDataPlaneLocatorName("sffdpl");
    private static final String TEST_IP_ADDRESS = "170.0.0.1";
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsUtil.class);
    private InstanceIdentifier<Node> nodeIID;
    private InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID;
    private ExecutorService executorService;

    @Before
    public void init() {
        DataBroker dataBroker = getDataBroker();
        SfcDataStoreAPI.setDataProviderAux(dataBroker);
        executorService = Executors.newFixedThreadPool(5);

        // before starting test, node is created
        nodeIID = createNodeIID();
        createOvsdbNode();
    }

    @After
    public void finalized() {

        // delete node after test
        deleteOvsdbNode(LogicalDatastoreType.CONFIGURATION);
    }

    @Test
    public void testSubmitCallable() throws Exception {

        // create simple call() method for testing purposes
        class CallableTestSuccess implements Callable {

            @Override
            public String call() throws Exception {
                return TEST_STRING;
            }
        }

        // call() method throws exception
        class CallableTestException implements Callable {

            @Override
            public Object call() throws Exception {
                throw new InterruptedException();
            }
        }

        Object object = SfcOvsUtil.submitCallable(new CallableTestSuccess(), executorService);

        assertNotNull("Must not be null", object);
        assertEquals("Must be equal", object, TEST_STRING);

        object = SfcOvsUtil.submitCallable(new CallableTestException(), executorService);

        assertNull("Must be null", object);
    }

    @Test
    // whether the ovsdb topology IID is successfully created
    public void testBuildOvsdbTopologyIID() {
        InstanceIdentifier<Topology> instanceIdentifierList = SfcOvsUtil.buildOvsdbTopologyIID();

        assertEquals("Must be equal", instanceIdentifierList.getTargetType().getName(), Topology.class.getName());
    }

    @Test
    // set bridge name into ovsdb bridge augmentation, then recover ovs node + bridge name
    public void testGetManagedByNodeId() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(TEST_BRIDGE_NAME))
            .setManagedBy(new OvsdbNodeRef(nodeIID));

        // TODO remove reflection for "getManagedByNodeId"
        NodeId nodeId =
                Whitebox.invokeMethod(SfcOvsUtil.class, "getManagedByNodeId", ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must not be null", nodeId);
        assertFalse("Must be false", nodeId.getValue().isEmpty());
        assertEquals("Must be equal", nodeId.getValue(), TEST_IP_ADDRESS + OVSDB_BRIDGE_PREFIX + TEST_BRIDGE_NAME);
    }

    @Test
    // whether the ovsdb node IID is successfully created when ovsdb bridge augmentation is
    // available
    public void testBuildOvsdbNodeIIDFFromOvsdbBridgeAugmentation() {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        OvsNodeBuilder ovsNodeBuilder = new OvsNodeBuilder();

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(nodeIID));
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(TEST_BRIDGE_NAME))
            .setManagedBy(new OvsdbNodeRef(nodeIID));

        InstanceIdentifier<Node> testNodeIID = SfcOvsUtil.buildOvsdbNodeIID(ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must not be null", testNodeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(testNodeIID).getNodeId().getValue(),
                TEST_IP_ADDRESS + OVSDB_BRIDGE_PREFIX + TEST_BRIDGE_NAME);
    }

    @Test
    // whether the ovsdb node IID is successfully created when sff name is available
    public void testBuildOvsdbNodeIIDFromString() {
        InstanceIdentifier<Node> testNodeIID = SfcOvsUtil.buildOvsdbNodeIID(TEST_STRING);

        assertNotNull("Must not be null", testNodeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(testNodeIID).getNodeId().getValue(), TEST_STRING);
    }

    @Test
    // whether the ovsdb node IID is successfully created when node id is available
    public void testBuildOvsdbNodeIIDFromNodeId() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(TEST_STRING));
        InstanceIdentifier<Node> testNodeIID = SfcOvsUtil.buildOvsdbNodeIID(nodeBuilder.build().getNodeId());

        assertNotNull("Must not be null", testNodeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(testNodeIID).getNodeId().getValue(), TEST_STRING);
    }

    @Test
    // whether the ovsdb bridge IID is successfully created when bridge ovsdb bridge augmentation is
    // available
    public void testBuildOvsdbBridgeIIDFromOvsdbBridgeAugmentation() {
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(createOvsdbBridgeAugmentation());

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal", bridgeIID.getTargetType(), OvsdbBridgeAugmentation.class);
        assertEquals("Must be equal",
                InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(),
                TEST_IP_ADDRESS + OVSDB_BRIDGE_PREFIX + TEST_BRIDGE_NAME);
    }

    @Test
    // whether the ovsdb bridge IID is successfully created when node id is available
    public void testBuildOvsdbBridgeIIDFromNodeId() {
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(new NodeId(TEST_STRING));

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal",
                InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(), TEST_STRING);
    }

    @Test
    // whether the ovsdb bridge IID is successfully created when sff name is available
    public void testBuildOvsdbBridgeIIDFromString() {
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(TEST_STRING);

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal",
                InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(), TEST_STRING);
    }

    @Test
    // whether the termination point IID is successfully created when ovs bridge augmentation &
    // ovsdb termination point are available
    public void testBuildOvsdbTerminationPointAugmentationIID() {

        // create ovsdb termination point augmentation with name
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(TEST_STRING);

        InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointAugmentationIID =
                SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(createOvsdbBridgeAugmentation(),
                        ovsdbTerminationPointAugmentationBuilder.build());

        assertNotNull("Must not be null", terminationPointAugmentationIID);
        assertEquals("Must be equal", terminationPointAugmentationIID.getTargetType(),
                OvsdbTerminationPointAugmentation.class);
        assertEquals("Must be equal", InstanceIdentifier
            .keyOf(terminationPointAugmentationIID.firstIdentifierOf(Node.class)).getNodeId().getValue(),
                TEST_IP_ADDRESS + OVSDB_BRIDGE_PREFIX + TEST_BRIDGE_NAME);
        assertEquals("Must be equal", InstanceIdentifier
            .keyOf(terminationPointAugmentationIID.firstIdentifierOf(TerminationPoint.class)).getTpId().getValue(),
                TEST_STRING);
    }

    @Test
    // whether the termination point IID is successfully created when sff name & sff data plane
    // locator name are available
    public void testBuildOvsdbTerminationPointIIDFromStrings() {
        InstanceIdentifier<TerminationPoint> terminationPointIID =
                SfcOvsUtil.buildOvsdbTerminationPointIID(new NodeId(TEST_STRING), SFF_DATA_PLANE_LOCATOR.getValue());

        assertEquals(InstanceIdentifier.keyOf(terminationPointIID).getTpId().getValue(),
                SFF_DATA_PLANE_LOCATOR.getValue());
    }

    @Test
    public void testConvertStringToIpAddress() throws Exception {
        // set incorrect ip address format
        IpAddress ipAddress = SfcOvsUtil.convertStringToIpAddress("fake ip");
        assertNull("Must be null", ipAddress);

        // set ipv4 address
        ipAddress = SfcOvsUtil.convertStringToIpAddress(IP_ADDRESS);
        assertNotNull("Must not be null", ipAddress);
        assertEquals("Must be equal", ipAddress.getIpv4Address().getValue(), IP_ADDRESS);

        // ser ipv6 address
        ipAddress = SfcOvsUtil.convertStringToIpAddress(IPV6_ADDRESS);
        assertNotNull("Must not be null", ipAddress);
        assertEquals("Must be equal", ipAddress.getIpv6Address().getValue(), IPV6_ADDRESS);
    }

    @Test
    public void testConvertIpAddressToString() {
        // set ipv4 string address
        String convertedIpAddress = SfcOvsUtil.convertIpAddressToString(new IpAddress(new Ipv4Address(IP_ADDRESS)));
        assertNotNull("Must not be null", convertedIpAddress);
        assertEquals("Must be equal", convertedIpAddress, IP_ADDRESS);

        // set ipv6 string address
        convertedIpAddress = SfcOvsUtil.convertIpAddressToString(new IpAddress(new Ipv6Address(IPV6_ADDRESS)));
        assertNotNull("Must not be null", convertedIpAddress);
        assertEquals("Must be equal", convertedIpAddress, IPV6_ADDRESS);
    }

    @Test
    // create ovsdb termination point and then delete it
    public void testPutAndDeleteOvsdbTerminationPoint() {

        // put ovsdb termination point
        boolean result = SfcOvsUtil.putOvsdbTerminationPoints(createOvsdbBridgeAugmentation(),
                createSffDataPlaneLocatorList(VxlanGpe.class, null));

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result);

        SffName sffName = new SffName(TEST_IP_ADDRESS + OVSDB_BRIDGE_PREFIX + TEST_BRIDGE_NAME);

        // delete created ovsdb termination point
        result = SfcOvsUtil.deleteOvsdbTerminationPoint(
                SfcOvsUtil.buildOvsdbTerminationPointIID(new NodeId(sffName.getValue()), "Dpl"));

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result);
    }

    @Test
    // put ovsdb bridge into ovs node (created in @Before block) and then delete whole node
    public void testPutAndDeleteOvsdbNode() throws Exception {

        boolean result = SfcOvsUtil.putOvsdbBridge(createOvsdbBridgeAugmentation());

        assertNotNull("Must be not null", result);
        assertTrue("Must be true", result);

        result = SfcOvsUtil.deleteOvsdbNode(nodeIID);

        assertNotNull("Must be not null", result);
        assertTrue("Must be true", result);
    }

    @Test
    /*
     * method tries to find ip-based data plane locator
     * both cases are tested, with ip & non-ip based dpl
     */
    public void testLookupTopologyNode() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(null);

        Node node = SfcOvsUtil.lookupTopologyNode(serviceFunctionForwarderBuilder.build());

        // no sff exists, should return null
        assertNull("Must be null", node);

        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(createSffDataPlaneLocatorList(null, null));

        node = SfcOvsUtil.lookupTopologyNode(serviceFunctionForwarderBuilder.build());

        // sff exists, but there is no ip address assigned, should return null
        assertNull("Must be null", node);

        IpBuilder ipBuilder = new IpBuilder();
        NodeBuilder nodeBuilder = new NodeBuilder();

        nodeBuilder.setNodeId(new NodeId(IP_ADDRESS));
        ipBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS)));

        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(createSffDataPlaneLocatorList(null, ipBuilder.build()));

        node = SfcOvsUtil.lookupTopologyNode(serviceFunctionForwarderBuilder.build());

        // ip address assigned to data plane locator, we can recover it from node
        assertNotNull("Must not be null", node);
        assertEquals("Must be equal", node.getNodeId().getValue(), IP_ADDRESS);
    }

    @Test
    // existing sff should be augmented with openflow node id
    public void testAugmentSffWithOpenFlowNodeId() throws Exception {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(new SffName(TEST_STRING));
        IpBuilder ipBuilder = new IpBuilder();

        // sff has got no node id, so it's returned without changes
        ServiceFunctionForwarder serviceFunctionForwarder =
                SfcOvsUtil.augmentSffWithOpenFlowNodeId(serviceFunctionForwarderBuilder.build());

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getName().getValue(), TEST_STRING);

        // create sff data plane locator
        ipBuilder.setIp(new IpAddress(new Ipv4Address(TEST_IP_ADDRESS)));

        // create sffOvsBridgeAugmentation
        SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(TEST_BRIDGE_NAME);
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());

        // create sff with all parameters
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(createSffDataPlaneLocatorList(null, ipBuilder.build()))
            .addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build())
            .setKey(new ServiceFunctionForwarderKey(new SffName(TEST_IP_ADDRESS)));

        serviceFunctionForwarder = SfcOvsUtil.augmentSffWithOpenFlowNodeId(serviceFunctionForwarderBuilder.build());

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getKey().getName().getValue(), TEST_IP_ADDRESS);
        assertEquals("Must be equal", serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getKey().getName(),
                DPL_NAME);
        assertEquals("Must be equal", serviceFunctionForwarder.getSffDataPlaneLocator()
            .get(0)
            .getDataPlaneLocator()
            .getLocatorType()
            .getImplementedInterface(), Ip.class);
    }

    @Test
    // rest of the cases of this method are tested here
    public void testAugmentSffWithOpenFlowNodeId1() throws Exception {
        final String ofNodeId = "openflow:95075992133360";

        // create and write node
        OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress ipAddress =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
                        .yang.ietf.inet.types.rev130715.Ipv4Address(TEST_IP_ADDRESS));

        ovsdbNodeAugmentationBuilder.setConnectionInfo(
                new ConnectionInfoBuilder().setRemoteIp(ipAddress).build());

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(new NodeKey(new NodeId("nodeId"))).addAugmentation(OvsdbNodeAugmentation.class,
                ovsdbNodeAugmentationBuilder.build());

        InstanceIdentifier<Node> nodeIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId("nodeId")));

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(nodeIID, nodeBuilder.build(),
                LogicalDatastoreType.CONFIGURATION);
        assertTrue("Must be true", transactionSuccessful);

        // create service function forwarder
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder =
                new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setKey(new ServiceFunctionForwarderKey(new SffName(TEST_STRING)))
            .setIpMgmtAddress(new IpAddress(new Ipv4Address(TEST_IP_ADDRESS)));

        ServiceFunctionForwarder serviceFunctionForwarder = serviceFunctionForwarderBuilder.build();

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getKey().getName().getValue(), TEST_STRING);
        assertEquals("Must be equal", serviceFunctionForwarder.getIpMgmtAddress().getIpv4Address().getValue(),
                TEST_IP_ADDRESS);

        // create and write ovs db bridge augmentation
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setDatapathId(new DatapathId(TEST_DATA_PATH));

        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId(TEST_IP_ADDRESS + "/bridge/" + TEST_BRIDGE_NAME)))
            .augmentation(OvsdbBridgeAugmentation.class);

        transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(bridgeEntryIID,
                ovsdbBridgeAugmentationBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        // create sff data plane locator
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS))).setPort(new PortNumber(5000));

        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder.setTransport(VxlanGpe.class).setLocatorType(ipBuilder.build());

        List<SffDataPlaneLocator> sffDataPlaneLocators = new ArrayList<>();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        SffDataPlaneLocatorName sffDplName = new SffDataPlaneLocatorName("sffLocator");
        sffDataPlaneLocatorBuilder.setName(sffDplName)
            .setKey(new SffDataPlaneLocatorKey(sffDplName))
            .setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        sffDataPlaneLocators.add(sffDataPlaneLocatorBuilder.build());

        // set sff bridge augmentation
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(TEST_BRIDGE_NAME);

        SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder
            .addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build())
            .setKey(new ServiceFunctionForwarderKey(new SffName(TEST_STRING)))
            .setSffDataPlaneLocator(sffDataPlaneLocators);

        serviceFunctionForwarder = SfcOvsUtil.augmentSffWithOpenFlowNodeId(serviceFunctionForwarderBuilder.build());

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getAugmentation(SffOvsBridgeAugmentation.class)
            .getOvsBridge()
            .getOpenflowNodeId(), ofNodeId);
        assertEquals("Must be equal", serviceFunctionForwarder.getKey().getName().getValue(), TEST_STRING);
    }

    @Test
    // get data path id from node id
    public void testGetOvsDataPathId() throws Exception {

        // id does not exist, should return null
        // TODO remove reflection for "getOvsDataPathId"
        DatapathId datapathId = Whitebox.invokeMethod(SfcOvsUtil.class, "getOvsDataPathId", new NodeId("fake id"));

        assertNull("Must be null", datapathId);

        // TODO remove reflection for "getOvsDataPathId"
        datapathId = Whitebox.invokeMethod(SfcOvsUtil.class, "getOvsDataPathId",
                InstanceIdentifier.keyOf(nodeIID).getNodeId());

        assertNotNull("Must not be null", datapathId);
        assertEquals(datapathId.getValue(), TEST_DATA_PATH);
    }

    @Test
    public void testGetLongFromDpid() throws Exception {
        Long result;
        // expected result of decoding based on TEST_DATA_PATH string, when you change that string,
        // this test will not pass!
        Long expectedResult = 95075992133360L;

        // TODO remove reflection for "getLongFromDpid"
        result = Whitebox.invokeMethod(SfcOvsUtil.class, "getLongFromDpid", TEST_DATA_PATH);

        assertNotNull("Must not be null", result);
        assertEquals("Must be equal", result, expectedResult);
    }

    @Test
    // there are only null returns
    public void testGetManagerNodeByIp() {

        // null ip address
        Node node = SfcOvsUtil.getManagerNodeByIp(null);

        assertNull("Must be null", node);

        node = SfcOvsUtil.getManagerNodeByIp(new IpAddress(new Ipv6Address(IPV6_ADDRESS)));

        assertNull("Must be null", node);
    }

    @Test
    // there is a test which uses node written in the data store
    public void testGetManagerNodeByIpV4() {

        // ipv4 address
        Node node = SfcOvsUtil.getManagerNodeByIp(new IpAddress(new Ipv4Address(TEST_IP_ADDRESS)));

        assertNotNull("Must be not null", node);
        assertEquals("Must be equal", node.getKey().getNodeId().getValue(), TEST_IP_ADDRESS);
    }

    @Test
    // ipv6 test
    public void testGetManagerNodeByIpV6() {

        // build ipv6 node
        OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress ipAddress =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params
                        .xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address(IPV6_ADDRESS));

        ovsdbNodeAugmentationBuilder.setConnectionInfo(
                new ConnectionInfoBuilder().setRemoteIp(ipAddress).build());

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(IPV6_ADDRESS)).addAugmentation(OvsdbNodeAugmentation.class,
                ovsdbNodeAugmentationBuilder.build());

        InstanceIdentifier<Node> nodeIID = SfcOvsUtil.buildOvsdbNodeIID(IPV6_ADDRESS);

        boolean transactionSuccessful =
                SfcDataStoreAPI.writePutTransactionAPI(nodeIID, nodeBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        // ipv6 address
        Node node = SfcOvsUtil.getManagerNodeByIp(new IpAddress(new Ipv6Address(IPV6_ADDRESS)));

        assertNotNull("Must be not null", node);
        assertEquals("Must be equal", node.getKey().getNodeId().getValue(), IPV6_ADDRESS);
    }

    @Test
    // get node from data store & create node augmentation
    public void testGetOvsdbNodeAugmentation() {
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(nodeIID);

        OvsdbNodeAugmentation ovsdbNodeAugmentation =
                SfcOvsUtil.getOvsdbNodeAugmentation(ovsdbNodeRef);

        assertNotNull("Must not be null", ovsdbNodeAugmentation);
        assertEquals("Must be equal", ovsdbNodeAugmentation.getDbVersion(), "DbVersion_");
        assertEquals("Must be equal", ovsdbNodeAugmentation.getOvsVersion(), "OvsVersion_");
    }

    @Test
    // null test
    public void testGetOvsdbNodeAugmentationNull() {
        InstanceIdentifier<Node> dummyIID = SfcOvsUtil.buildOvsdbNodeIID(new NodeId("dummmyID"));
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(dummyIID);

        OvsdbNodeAugmentation ovsdbNodeAugmentation =
                SfcOvsUtil.getOvsdbNodeAugmentation(ovsdbNodeRef);

        assertNull("Must be null", ovsdbNodeAugmentation);
    }

    @Test
    // null test
    public void testGetOvsdbNodeAugmentationNull1() {
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(createOvsdbBridgeIID(new NodeId(TEST_STRING)));

        OvsdbNodeAugmentation ovsdbNodeAugmentation =
                SfcOvsUtil.getOvsdbNodeAugmentation(ovsdbNodeRef);

        assertNull("Must be null", ovsdbNodeAugmentation);
    }

    @Test
    /*
     * core of this method is tested through "testAugmentSffWithOpenFlowNodeId"
     * there is first part of unsuccessful cases
     */
    public void getOpenFlowNodeIdForSffNullTests() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();

        // serviceForwarderOvsBridgeAugmentation == null
        String result = SfcOvsUtil.getOpenFlowNodeIdForSff(serviceFunctionForwarderBuilder.build());

        assertNull("Must be null", result);

        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.addAugmentation(SffOvsBridgeAugmentation.class,
                sffOvsBridgeAugmentationBuilder.build());

        // serviceForwarderOvsBridge == null
        result = SfcOvsUtil.getOpenFlowNodeIdForSff(serviceFunctionForwarderBuilder.build());

        assertNull("Must be null", result);
    }

    /*
     * auxiliary methods below
     */

    @Test
    /*
     * core of this method is tested through "testAugmentSffWithOpenFlowNodeId"
     * there is second part of unsuccessful cases
     */
    public void getOpenFlowNodeIdForSffNullTests1() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();

        ovsBridgeBuilder.setBridgeName(TEST_BRIDGE_NAME);
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());

        serviceFunctionForwarderBuilder.addAugmentation(SffOvsBridgeAugmentation.class,
                sffOvsBridgeAugmentationBuilder.build());

        // dataPathId == null
        String result = SfcOvsUtil.getOpenFlowNodeIdForSff(serviceFunctionForwarderBuilder.build());

        assertNull("Must be null", result);
    }

    @Test
    /*
     * Test case for getOfPortByName
     *
     */
    public void getOfPortByNameTest1() {
        final String ofNodeId = "openflow:95075992133360";
        assertEquals("Must be equal", SfcOvsUtil.getOfPortByName(ofNodeId, TEST_STRING), TEST_PORT);
    }

    @Test
    /*
     * Test case for getVxlanOfPort
     *
     */
    public void getVxlanOfPortTest1() {
        final String ofNodeId = "openflow:95075992133360";
        assertEquals("Must be equal", SfcOvsUtil.getVxlanOfPort(ofNodeId), TEST_PORT);
    }

    /*
     * create node IID
     */
    private static InstanceIdentifier<Node> createNodeIID() {
        return InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId(TEST_IP_ADDRESS)));

    }

    private void createOvsdbNode() {
        boolean isCreated;

        /*
         * a node is created and written into data store
         * this method is called in @Before block
         */

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(TEST_IP_ADDRESS)).setTerminationPoint(createTerminationPointList())
                .addAugmentation(OvsdbNodeAugmentation.class,
                        SfcOvsDataStoreAPITest.createOvsdbNodeAugmentation(TEST_IP_ADDRESS))
                .addAugmentation(OvsdbBridgeAugmentation.class, createOvsdbBridgeAugmentation())
                .setKey(new NodeKey(new NodeId(TEST_IP_ADDRESS)));
        isCreated =
                SfcDataStoreAPI.writePutTransactionAPI(nodeIID, nodeBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        if (isCreated) {
            LOG.debug("Node has been successfully created");
        } else {
            LOG.debug("Node has not been created. Test can fail");
        }
    }

    /*
     * after every test,this method is called to remove node from data store
     */
    private void deleteOvsdbNode(@SuppressWarnings("SameParameterValue") LogicalDatastoreType type) {

        boolean isDeleted;
        isDeleted = SfcDataStoreAPI.deleteTransactionAPI(nodeIID, type);

        if (isDeleted) {
            LOG.debug("Node has been deleted");
        } else {
            LOG.debug("Node has not been deleted");
        }
    }

    /*
     * create termination point list and add termination point
     * needed to create a node
     */
    private List<TerminationPoint> createTerminationPointList() {
        List<TerminationPoint> terminationPointList = new ArrayList<>();
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        terminationPointBuilder.setTpId(new TpId("tp_id"));
        terminationPointBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class,
                createOvsdbTerminationPointAugmentation());
        terminationPointList.add(terminationPointBuilder.build());
        return terminationPointList;
    }

    /*
     * build ovsdb termination point augmentation
     *
     */
    private OvsdbTerminationPointAugmentation createOvsdbTerminationPointAugmentation() {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(TEST_STRING);
        ovsdbTerminationPointAugmentationBuilder.setInterfaceType(InterfaceTypeVxlan.class);
        ovsdbTerminationPointAugmentationBuilder.setOfport(TEST_PORT);

        return ovsdbTerminationPointAugmentationBuilder.build();
    }

    /*
     * build ovsdb augmentation with name and parent node
     * needed to create a node
     */
    private OvsdbBridgeAugmentation createOvsdbBridgeAugmentation() {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(TEST_BRIDGE_NAME))
            .setManagedBy(new OvsdbNodeRef(nodeIID))
            .setDatapathId(new DatapathId(TEST_DATA_PATH));
        return ovsdbBridgeAugmentationBuilder.build();
    }

    /*
     * create ovsdb bride IID
     */
    private InstanceIdentifier<OvsdbBridgeAugmentation> createOvsdbBridgeIID(NodeId nodeId) {

        return InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(nodeId))
            .augmentation(OvsdbBridgeAugmentation.class);
    }

    /*
     * create sff data plane locator list
     */
    private List<SffDataPlaneLocator> createSffDataPlaneLocatorList(Class transportType, LocatorType locatorType) {
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();
        SffOvsLocatorOptionsAugmentationBuilder sffOvsLocatorOptionsAugmentationBuilder =
                new SffOvsLocatorOptionsAugmentationBuilder();

        // create and set ovs options
        ovsOptionsBuilder.setLocalIp(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
            .setLocalIp(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
            .setDstPort(SfcOvsUtil.OVSDB_OPTION_DST_PORT)
            .setNsp(SfcOvsUtil.OVSDB_OPTION_NSP)
            .setNsi(SfcOvsUtil.OVSDB_OPTION_NSI)
            .setKey(SfcOvsUtil.OVSDB_OPTION_KEY);
        sffOvsLocatorOptionsAugmentationBuilder.setOvsOptions(ovsOptionsBuilder.build());

        // set data plane locator
        // noinspection unchecked
        dataPlaneLocatorBuilder.setTransport(transportType);
        dataPlaneLocatorBuilder.setLocatorType(locatorType);

        // set sff data plane locator
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        sffDataPlaneLocatorBuilder.setName(DPL_NAME)
            .setDataPlaneLocator(dataPlaneLocatorBuilder.build())
            .addAugmentation(SffOvsLocatorOptionsAugmentation.class, sffOvsLocatorOptionsAugmentationBuilder.build());

        // add entry into data plane locator list
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        return sffDataPlaneLocatorList;
    }
}

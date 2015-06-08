package org.opendaylight.sfc.sfc_ovs.provider.api;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertNotNull;

/**
 * this class contains junit tests for SfcOvsDataStoreAPI class
 *
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsDataStoreAPI
 * <p/>
 * @since 2015-05-26
 */


/*
 * All methods in SfcDataStoreAPI are mocked here. The main reason is, that tests does not work reliable, when
 * there was something written into data store in more than one class. Most of SfcDataStoreAPI.class is tested through
 * SfcOvsUtil.class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcDataStoreAPI.class)
public class SfcOvsDataStoreAPITest extends AbstractDataBrokerTest {

    private static final String bridgeName = "bridge_name";
    private final Object[] methodParams = new Object[2];
    private final String dplName = "dpl name";
    private final String testIpv4 = "10.0.0.1";
    private final String testIpv6 = "01:23:45:67:89:AB:CD:EF";
    private final InstanceIdentifier<Node> nodeIID = createNodeIID();
    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private SfcOvsDataStoreAPI sfcOvsDataStoreAPIObject;
    private SfcOvsDataStoreAPI.Method methodToCall;
    private Object testResult;

    @Before
    public void init() {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "writePutTransactionAPI")).toReturn(true);
        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "deleteTransactionAPI")).toReturn(true);
    }

    @Test
    public void testReadOvsdbBridge() throws Exception {
        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_BRIDGE;
        methodParams[0] = createBridgeIID();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "readTransactionAPI")).toReturn(createOvsdbBridgeAugmentation());

        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
    }

    @Test
    public void testPutOvsdbBridge() throws Exception {
        methodToCall = SfcOvsDataStoreAPI.Method.PUT_OVSDB_BRIDGE;
        methodParams[0] = createOvsdbBridgeAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);

        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
    }

    @Test
    public void testDeleteOvsdbNode() throws Exception {
        methodToCall = SfcOvsDataStoreAPI.Method.DELETE_OVSDB_NODE;
        methodParams[0] = nodeIID;

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
    }

    @Test
    public void testPutAndDeleteOvsdbTerminationPoint() throws Exception {
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder.setName(dplName);
        SffDataPlaneLocator sffDataPlaneLocator = sffDataPlaneLocatorBuilder.build();

        //put tp
        methodToCall = SfcOvsDataStoreAPI.Method.PUT_OVSDB_TERMINATION_POINT;
        methodParams[0] = createOvsdbBridgeAugmentation();
        methodParams[1] = createOvsdbTerminationPointAugmentation();
        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);

        //delete tp
        methodToCall = SfcOvsDataStoreAPI.Method.DELETE_OVSDB_TERMINATION_POINT;
        methodParams[0] = createOvsdbTerminationPointIID(testIpv4, sffDataPlaneLocator.getName());

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
    }

    @Test
    public void testReadOvsdbNodeByIpFailed() throws Exception {

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_IP;
        methodParams[0] = testIpv4;

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "readTransactionAPI")).toReturn(createFaultyTopology());

        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);
    }

    @Test
    public void testReadOvsdbNodeByIpFailedAgain() throws Exception {

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_IP;
        methodParams[0] = testIpv4;

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "readTransactionAPI")).toReturn(createFaultyTopologyAgain());

        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);
    }

    @Test
    public void testReadOvsdbNodeByIpv4() throws Exception {

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_IP;
        methodParams[0] = testIpv4;

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "readTransactionAPI")).toReturn(createIpv4Topology());

        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
    }

    @Test
    public void testReadOvsdbNodeByIpv6() throws Exception {

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_IP;
        methodParams[0] = testIpv6;

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "readTransactionAPI")).toReturn(createIpv6Topology());

        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
    }

    @Test
    public void testReadOvsdbNodeByRef() throws Exception {
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(nodeIID);

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_REF;
        methodParams[0] = ovsdbNodeRef;

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "readTransactionAPI")).toReturn(createNode());

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
    }

    @Test
    public void testAllCallsWithIncorrectParameters() throws Exception {
        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_BRIDGE;
        methodParams[0] = createOvsdbTerminationPointAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);

        methodToCall = SfcOvsDataStoreAPI.Method.PUT_OVSDB_BRIDGE;
        methodParams[0] = createOvsdbTerminationPointAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);

        methodToCall = SfcOvsDataStoreAPI.Method.DELETE_OVSDB_NODE;
        methodParams[0] = createOvsdbTerminationPointAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);

        methodToCall = SfcOvsDataStoreAPI.Method.PUT_OVSDB_TERMINATION_POINT;
        methodParams[0] = createOvsdbTerminationPointAugmentation();
        methodParams[1] = createOvsdbTerminationPointAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);

        methodToCall = SfcOvsDataStoreAPI.Method.DELETE_OVSDB_TERMINATION_POINT;
        methodParams[0] = createOvsdbTerminationPointAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_IP;
        methodParams[0] = createOvsdbTerminationPointAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_REF;
        methodParams[0] = createOvsdbTerminationPointAugmentation();

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNull("Must be null", testResult);
    }

    //build ovsdb node augmentation
    private OvsdbNodeAugmentation createOvsdbNodeAugmentation() {
        OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(new IpAddress(new Ipv4Address(testIpv4)));
        ovsdbNodeAugmentationBuilder.setDbVersion("DbVersion_")
                .setOvsVersion("OvsVersion_")
                .setConnectionInfo(connectionInfoBuilder.build());
        return ovsdbNodeAugmentationBuilder.build();
    }

    //create node using augmentation
    private Node createNode() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbNodeAugmentation());
        return nodeBuilder.build();
    }

    //create ovsdb bridge augmentation
    private OvsdbBridgeAugmentation createOvsdbBridgeAugmentation() {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName))
                .setManagedBy(new OvsdbNodeRef(nodeIID))
                .setDatapathId(new DatapathId("12:34:56:78:9A:BC:DE:F0"));
        return ovsdbBridgeAugmentationBuilder.build();
    }

    //create ovsdb termination point augmentation
    private OvsdbTerminationPointAugmentation createOvsdbTerminationPointAugmentation() {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(dplName);
        return ovsdbTerminationPointAugmentationBuilder.build();
    }

    //create node IID
    private InstanceIdentifier<Node> createNodeIID() {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testIpv4)));
    }

    //create bridge IID
    private InstanceIdentifier<OvsdbBridgeAugmentation> createBridgeIID() {
        return nodeIID
                .augmentation(OvsdbBridgeAugmentation.class);
    }

    //create ovsdb termination point IID
    private InstanceIdentifier<OvsdbTerminationPointAugmentation> createOvsdbTerminationPointIID(String sffName, String sffDataPlaneLocatorName) {
        String BRIDGE_PREFIX = "/bridge/";
        String TERMINATION_POINT_PREFIX = "/terminationpoint/";
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(sffName + BRIDGE_PREFIX + bridgeName)))
                .child(TerminationPoint.class, new TerminationPointKey(
                        new TpId(sffName + BRIDGE_PREFIX + bridgeName + TERMINATION_POINT_PREFIX + sffDataPlaneLocatorName)))
                .augmentation(OvsdbTerminationPointAugmentation.class);

    }

    //create ipv4 topology for successful "readOvsdbNodeByIp" test
    private Topology createIpv4Topology() {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        List<Node> nodeList = new ArrayList<>();
        NodeBuilder nodeBuilder = new NodeBuilder();
        OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        TopologyBuilder topologyBuilder = new TopologyBuilder();

        //set ipv4
        connectionInfoBuilder.setRemoteIp(new IpAddress(new Ipv4Address(testIpv4)));

        //create ovsdbNodeAugmentation
        ovsdbNodeAugmentationBuilder.setDbVersion("DbVersion_")
                .setOvsVersion("OvsVersion_")
                .setConnectionInfo(connectionInfoBuilder.build());

        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugmentationBuilder.build());
        nodeList.add(nodeBuilder.build());
        topologyBuilder.setNode(nodeList);

        return topologyBuilder.build();
    }

    //create ipv6 topology for successful "readOvsdbNodeByIp" test
    private Topology createIpv6Topology() {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        List<Node> nodeList = new ArrayList<>();
        NodeBuilder nodeBuilder = new NodeBuilder();
        OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        TopologyBuilder topologyBuilder = new TopologyBuilder();

        //set ipv4
        connectionInfoBuilder.setRemoteIp(new IpAddress(new Ipv6Address(testIpv6)));

        //create ovsdbNodeAugmentation
        ovsdbNodeAugmentationBuilder.setDbVersion("DbVersion_")
                .setOvsVersion("OvsVersion_")
                .setConnectionInfo(connectionInfoBuilder.build());

        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugmentationBuilder.build());
        nodeList.add(nodeBuilder.build());
        topologyBuilder.setNode(nodeList);

        return topologyBuilder.build();
    }

    //create topology for unsuccessful "readOvsdbNodeByIp" test (return null)
    private Topology createFaultyTopology() {
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setNode(null);
        return topologyBuilder.build();
    }

    //create topology for unsuccessful "readOvsdbNodeByIp" test (return null, second option)
    private Topology createFaultyTopologyAgain() {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        List<Node> nodeList = new ArrayList<>();
        NodeBuilder nodeBuilder = new NodeBuilder();
        OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        TopologyBuilder topologyBuilder = new TopologyBuilder();

        //set ip to null
        connectionInfoBuilder.setRemoteIp(null);

        //create ovsdbNodeAugmentation
        ovsdbNodeAugmentationBuilder.setDbVersion("DbVersion_")
                .setOvsVersion("OvsVersion_")
                .setConnectionInfo(connectionInfoBuilder.build());

        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugmentationBuilder.build());
        nodeList.add(nodeBuilder.build());
        topologyBuilder.setNode(nodeList);

        return topologyBuilder.build();
    }


}
package org.opendaylight.sfc.sfc_ovs.provider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.*;


/**
 * this class contains junit tests for SfcOvsUtil class
 *
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsUtil
 * <p/>
 * @since 2015-04-23
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOvsUtil.class)
public class SfcOvsUtilTest extends AbstractDataBrokerTest {
    private static final String OVSDB_BRIDGE_PREFIX = "/bridge/";   //copy of private String from SfcOvsUtil.class
    private static final String OVSDB_TERMINATION_POINT_PREFIX = "/terminationpoint/";  //copy of private String from SfcOvsUtil.class
    private static final String ipv4Address = "170.0.0.1";
    private static final String ipv6Address = "::";
    private static final String testBridgeName = "Bridge Name";
    private static final String testString = "Test string";
    private static final String sffDataPlaneLocator = "sffDataPlaneLocator test";
    private static final String testDataPath = "12:34:56:78:9A:BC:DE:F0";
    private static final int numberOfNodes = 11;
    private static final String dplName = "sffdpl";
    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private final Logger LOG = LoggerFactory.getLogger(SfcOvsUtil.class);
    private String testIpAddress = "192.168.0.";
    private InstanceIdentifier<Node> nodeIID;
    private InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;
    private boolean result;
    private ExecutorService executorService;
    private ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder;
    private IpBuilder ipBuilder;
    private NodeBuilder nodeBuilder;
    private Node node;
    private NodeId nodeId;
    private OvsdbNodeAugmentation ovsdbNodeAugmentation;
    private OvsdbNodeRef ovsdbNodeRef;
    private boolean allOk;
    private InstanceIdentifier<OvsdbBridgeAugmentation> testBridgeIID;
    private ConnectionInfoBuilder connectionInfoBuilder;

    @Before
    public void init() {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executorService = opendaylightSfc.getExecutor();

        //before starting tests, all required node are created
        createOvsdbNodes();
    }

    @After
    //after tests, all nodes should be deleted
    public void deleteAllNodes() {
        allOk = true;
        boolean isDeleted;

        for (int i = 1; i <= numberOfNodes; i++) {
            isDeleted = SfcDataStoreAPI.deleteTransactionAPI(createNodeIID(i), LogicalDatastoreType.OPERATIONAL);
            if (!isDeleted)
                allOk = false;
        }

        if (allOk)
            LOG.debug("All created nodes has been deleted");
        else
            LOG.debug("Some of the created nodes has not been deleted");
    }

    @Test
    public void testCreateObject() {

        //just create an object of class SfcOvsUtil
        SfcOvsUtil sfcOvsUtil = new SfcOvsUtil();
        sfcOvsUtil.getClass();
    }

    @Test
    public void testSuccessfulSubmitCallable() throws Exception {

        //create simple call() method for testing purposes
        class CallableTestSuccess implements Callable {

            @Override
            public String call() throws Exception {
                return testString;
            }
        }

        //call() method throws exception
        class CallableTestException implements Callable {

            @Override
            public Object call() throws Exception {
                throw new InterruptedException();
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        Object object = SfcOvsUtil.submitCallable(new CallableTestSuccess(), executorService);

        assertNotNull("Must not be null", object);
        assertEquals("Must be equal", object, testString);

        object = SfcOvsUtil.submitCallable(new CallableTestException(), executorService);

        assertNull("Must be null", object);
    }

    @Test
    //whether the ovsdb topology IID is successfully created
    public void testBuildOvsdbTopologyIID() {
        InstanceIdentifier<Topology> instanceIdentifierList = SfcOvsUtil.buildOvsdbTopologyIID();

        assertEquals("Must be equal", instanceIdentifierList.getTargetType().getName(), Topology.class.getName());
    }

    @Test
    //set bridge name into ovsdb bridge augmentation, then recover ovs node + bridge name
    public void testGetManagedByNodeId() throws Exception {
        int index = 1;
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(createNodeIID(index)));

        NodeId nodeId = Whitebox.invokeMethod(SfcOvsUtil.class, "getManagedByNodeId", ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must not be null", nodeId);
        assertFalse("Must be false", nodeId.getValue().isEmpty());
        assertEquals("Must be equal", nodeId.getValue(), testIpAddress + index + OVSDB_BRIDGE_PREFIX + testBridgeName);
    }

    @Test
    //whether the ovsdb node IID is successfully created when ovsdb bridge augmentation is available
    public void testBuildOvsdbNodeIIDFFromOvsdbBridgeAugmentation() {
        int index = 2;
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        OvsNodeBuilder ovsNodeBuilder = new OvsNodeBuilder();

        nodeIID = createNodeIID(index);

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(nodeIID));
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(nodeIID));

        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must not be null", nodeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testIpAddress + index + OVSDB_BRIDGE_PREFIX + testBridgeName);
    }

    @Test
    //whether the ovsdb node IID is successfully created when sff name is available
    public void testBuildOvsdbNodeIIDFromString() {
        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(testString);

        assertNotNull("Must not be null", nodeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testString);
    }

    @Test
    //whether the ovsdb node IID is successfully created when node id is available
    public void testBuildOvsdbNodeIIDFromNodeID() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(testString));
        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(nodeBuilder.build().getNodeId());

        assertNotNull("Must not be null", nodeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testString);
    }

    @Test
    //whether the ovsdb bridge IID is successfully created when bridge ovsdb bridge augmentation is available
    public void testBuildOvsdbBridgeIIDFromOvsdbBridgeAugmentation() {
        int index = 3;
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(createOvsdbBridgeAugmentation(3));

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal", bridgeIID.getTargetType(), OvsdbBridgeAugmentation.class);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(),
                testIpAddress + index + OVSDB_BRIDGE_PREFIX + testBridgeName);
    }

    @Test
    //whether the ovsdb bridge IID is successfully created when node id is available
    public void testBuildOvsdbBridgeIIDFromNodeID() {
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(new NodeId(testString));

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(), testString);
    }

    @Test
    //whether the ovsdb bridge IID is successfully created when sff name is available
    public void testBuildOvsdbBridgeIID_String() {
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(testString);

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(), testString);
    }

    @Test
    //whether the termination point IID is successfully created when ovs bridge augmentation & ovsdb termination point are available
    public void testBuildOvsdbTerminationPointAugmentationIID() {
        int index = 4;

        //create ovsdb termination point augmentation with name
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(testString);

        InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointAugmentationIID =
                SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(createOvsdbBridgeAugmentation(4), ovsdbTerminationPointAugmentationBuilder.build());

        assertNotNull("Must not be null", terminationPointAugmentationIID);
        assertEquals("Must be equal", terminationPointAugmentationIID.getTargetType(), OvsdbTerminationPointAugmentation.class);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(terminationPointAugmentationIID.firstIdentifierOf(Node.class)).getNodeId().getValue(),
                testIpAddress + index + OVSDB_BRIDGE_PREFIX + testBridgeName);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(terminationPointAugmentationIID.firstIdentifierOf(TerminationPoint.class)).getTpId().getValue(),
                testIpAddress + index + OVSDB_BRIDGE_PREFIX + testBridgeName + OVSDB_TERMINATION_POINT_PREFIX + testString);
    }

    @Test
    //whether the termination point IID is successfully created when sff name & sff data plane locator name are available
    public void testBuildOvsdbTerminationPointIIDFromStrings() {
        InstanceIdentifier<TerminationPoint> terminationPointIID = SfcOvsUtil.buildOvsdbTerminationPointIID(testString, sffDataPlaneLocator);

        assertEquals(InstanceIdentifier.keyOf(terminationPointIID).getTpId().getValue(), testString + OVSDB_TERMINATION_POINT_PREFIX + sffDataPlaneLocator);
    }

    @Test
    public void testConvertStringToIpAddress() throws Exception {
        //set incorrect ip address format
        IpAddress ipAddress = SfcOvsUtil.convertStringToIpAddress(testIpAddress);
        assertNull("Must be null", ipAddress);

        //set ipv4 address
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv4Address);
        assertNotNull("Must not be null", ipAddress);
        assertEquals("Must be equal", ipAddress.getIpv4Address().getValue(), ipv4Address);

        //ser ipv6 address
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv6Address);
        assertNotNull("Must not be null", ipAddress);
        assertEquals("Must be equal", ipAddress.getIpv6Address().getValue(), ipv6Address);
    }

    @Test
    public void testConvertIpAddressToString() {
        //set ipv4 string address
        testIpAddress = SfcOvsUtil.convertIpAddressToString(new IpAddress(new Ipv4Address(ipv4Address)));
        assertNotNull("Must not be null", testIpAddress);
        assertEquals("Must be equal", testIpAddress, ipv4Address);

        //set ipv6 string address
        testIpAddress = SfcOvsUtil.convertIpAddressToString(new IpAddress(new Ipv6Address(ipv6Address)));
        assertNotNull("Must not be null", testIpAddress);
        assertEquals("Must be equal", testIpAddress, ipv6Address);
    }

    @Test
    //create ovsdb termination point and then delete it
    public void putAndDeleteOvsdbTerminationPoint() {
        final int index = 5;

        //put ovsdb termination point
        result = SfcOvsUtil.putOvsdbTerminationPoints(createOvsdbBridgeAugmentation(index), createSffDataPlaneLocatorList(VxlanGpe.class, null), executorService);

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result);

        String sffName = testIpAddress + index + OVSDB_BRIDGE_PREFIX + testBridgeName;

        //delete created ovsdb termination point
        result = SfcOvsUtil.deleteOvsdbTerminationPoint(SfcOvsUtil.buildOvsdbTerminationPointIID(sffName, "Dpl"), executorService);

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result);
    }


    @Test
    //put ovsdb bridge into ovs node (created in @Before block) and then delete whole node
    public void putAndDeleteOvsdbNode() {
        final int index = 6;
        result = SfcOvsUtil.putOvsdbBridge(createOvsdbBridgeAugmentation(index), executorService);

        assertNotNull("Must be not null", result);
        assertTrue("Must be true", result);

        nodeIID = createNodeIID(index);

        result = SfcOvsUtil.deleteOvsdbNode(nodeIID, executorService);

        assertNotNull("Must be not null", result);
        assertTrue("Must be true", result);
    }

    @Test
    public void testLookupTopologyNode() {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(null);

        node = SfcOvsUtil.lookupTopologyNode(serviceFunctionForwarderBuilder.build(), executorService);

        //no sff exists, should return null
        assertNull("Must be null", node);

        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(createSffDataPlaneLocatorList(null, null));

        node = SfcOvsUtil.lookupTopologyNode(serviceFunctionForwarderBuilder.build(), executorService);

        //sff exists, but there is no ip address assigned, should return null
        assertNull("Must be null", node);

        ipBuilder = new IpBuilder();
        nodeBuilder = new NodeBuilder();

        nodeBuilder.setNodeId(new NodeId(ipv4Address));
        ipBuilder.setIp(new IpAddress(new Ipv4Address(ipv4Address)));

        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(createSffDataPlaneLocatorList(null, ipBuilder.build()));

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable")).toReturn(nodeBuilder.build());

        node = SfcOvsUtil.lookupTopologyNode(serviceFunctionForwarderBuilder.build(), executorService);

        assertNotNull("Must not be null", node);
        assertEquals("Must be equal", node.getNodeId().getValue(), ipv4Address);
    }

    @Test
    //existing sff should be augmented with openflow node id
    public void testAugmentSffWithOpenFlowNodeId() throws Exception {
        final int index = 7;
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(testString);
        SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ipBuilder = new IpBuilder();

        //sff has got no node id, so it's returned without changes
        ServiceFunctionForwarder serviceFunctionForwarder = SfcOvsUtil.augmentSffWithOpenFlowNodeId(serviceFunctionForwarderBuilder.build());

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getName(), testString);

        //create sff data plane locator
        ipBuilder.setIp(new IpAddress(new Ipv4Address(testIpAddress + index)));

        //create sffOvsBridgeAugmentation
        ovsBridgeBuilder.setBridgeName(testBridgeName);
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());

        //mock node id
        nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(testIpAddress + index));
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getManagedByNodeId")).toReturn(nodeBuilder.build().getNodeId());


        //create sff with all parameters
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(createSffDataPlaneLocatorList(null, ipBuilder.build()))
                .addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build())
                .setKey(new ServiceFunctionForwarderKey(testIpAddress + index));

        serviceFunctionForwarder = SfcOvsUtil.augmentSffWithOpenFlowNodeId(serviceFunctionForwarderBuilder.build());

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getKey().getName(), testIpAddress + index);
        assertEquals("Must be equal", serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getKey().getName(), dplName);
        assertEquals("Must be equal", serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getDataPlaneLocator().getLocatorType().getImplementedInterface(), Ip.class);
    }

    @Test
    //get data path id from node id
    public void testGetOvsDataPathId() throws Exception {
        final int index = 8;

        //id does not exist, should return null
        DatapathId datapathId = Whitebox.invokeMethod(SfcOvsUtil.class, "getOvsDataPathId", new NodeId("fake id"));

        assertNull("Must be null", datapathId);

        nodeIID = createNodeIID(index);
        datapathId = Whitebox.invokeMethod(SfcOvsUtil.class, "getOvsDataPathId", InstanceIdentifier.keyOf(nodeIID).getNodeId());

        assertNotNull("Must not be null", datapathId);
        assertEquals(datapathId.getValue(), testDataPath);
    }

    @Test
    public void testGetLongFromDpid() throws Exception {
        Long result;
        //expected result of decoding
        Long expectedResult = 95075992133360L;

        result = Whitebox.invokeMethod(SfcOvsUtil.class, "getLongFromDpid", testDataPath);

        assertNotNull("Must not be null", result);
        assertEquals("Must be equal", result, expectedResult);
    }

    @Test
    //a test node will be created for testing purposes, in order to test all options
    public void testGetManagerNodeByIp() {

        //null ip address
        node = SfcOvsUtil.getManagerNodeByIp(null, executorService);

        assertNull("Must be null", node);

        //ipv6 address, mocked node
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable")).toReturn(null);

        node = SfcOvsUtil.getManagerNodeByIp(new IpAddress(new Ipv6Address(ipv6Address)), executorService);

        assertNull("Must be null", node);
    }

    @Test
    //there is a test which uses "real" node written in the data store
    public void testGetManagerNodeByIp1() {
        int index = 9;

        //real ipv4 address
        node = SfcOvsUtil.getManagerNodeByIp(new IpAddress(new Ipv4Address(testIpAddress + index)), executorService);

        assertNotNull("Must be not null", node);
        assertEquals("Must be equal", node.getKey().getNodeId().getValue(), testIpAddress + index);
    }

    @Test
    public void testGetOvsdbNodeAugmentation() {
        int index = 10;
        ovsdbNodeRef = new OvsdbNodeRef(createNodeIID(index));

        ovsdbNodeAugmentation = SfcOvsUtil.getOvsdbNodeAugmentation(ovsdbNodeRef, executorService);

        assertNotNull("Must not be null", ovsdbNodeAugmentation);
        assertEquals("Must be equal", ovsdbNodeAugmentation.getDbVersion(), "DbVersion_" + index);
        assertEquals("Must be equal", ovsdbNodeAugmentation.getOvsVersion(), "OvsVersion_" + index);
    }

    @Test
    public void testGetOvsdbNodeAugmentationNull() {
        int index = 11;
        ovsdbNodeRef = new OvsdbNodeRef(createNodeIID(index));

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable")).toReturn(null);

        ovsdbNodeAugmentation = SfcOvsUtil.getOvsdbNodeAugmentation(ovsdbNodeRef, executorService);

        assertNull("Must be null", ovsdbNodeAugmentation);
    }

    @Test
    public void testGetOvsdbNodeAugmentationNull1() {
        ovsdbNodeRef = new OvsdbNodeRef(createOvsdbBridgeIID(new NodeId(testString)));

        ovsdbNodeAugmentation = SfcOvsUtil.getOvsdbNodeAugmentation(ovsdbNodeRef, executorService);

        assertNull("Must be null", ovsdbNodeAugmentation);
    }

    private void createOvsdbNodes() {

        //Here are created all nodes required for testing

        allOk = true;
        boolean isCreated;

        for (int i = 1; i <= numberOfNodes; i++) {
            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setNodeId(new NodeId(Integer.toString(i)))
                    .setTerminationPoint(createTerminationPointList())
                    .addAugmentation(OvsdbNodeAugmentation.class, createOvsdbNodeAugmentation(i))
                    .addAugmentation(OvsdbBridgeAugmentation.class, createOvsdbBridgeAugmentation(i))
                    .setKey(new NodeKey(new NodeId(testIpAddress + Integer.toString(i))));
            if (i == 1) {
                isCreated = SfcDataStoreAPI.writePutTransactionAPI(createNodeIID(i),
                        nodeBuilder.build(), LogicalDatastoreType.CONFIGURATION);
            } else {
                isCreated = SfcDataStoreAPI.writePutTransactionAPI(createNodeIID(i),
                        nodeBuilder.build(), LogicalDatastoreType.OPERATIONAL);
            }
            if (!isCreated)
                allOk = false;
        }
        if (allOk)
            LOG.debug("All nodes has been created");
        else
            LOG.debug("All nodes has not been created");
    }

    //create termination point list and add termination point
    //needed to create a node
    private List<TerminationPoint> createTerminationPointList() {
        List<TerminationPoint> terminationPointList = new ArrayList<>();
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        terminationPointBuilder.setTpId(new TpId("tp_id"));
        terminationPointList.add(terminationPointBuilder.build());
        return terminationPointList;
    }

    //build ovsdb node augmentation
    //needed to create a node
    private OvsdbNodeAugmentation createOvsdbNodeAugmentation(int index) {
        OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(new IpAddress(new Ipv4Address(testIpAddress + index)));
        ovsdbNodeAugmentationBuilder.setDbVersion("DbVersion_" + index)
                .setOvsVersion("OvsVersion_" + index)
                .setConnectionInfo(connectionInfoBuilder.build());
        return ovsdbNodeAugmentationBuilder.build();
    }

    //build ovsdb augmentation with name and parent node
    //needed to create a node
    private OvsdbBridgeAugmentation createOvsdbBridgeAugmentation(int index) {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(createNodeIID(index)))
                .setDatapathId(new DatapathId(testDataPath));
        return ovsdbBridgeAugmentationBuilder.build();
    }

    //create node IID with desired key
    private InstanceIdentifier<Node> createNodeIID(int id) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testIpAddress + id)));

    }

    //create ovsdb bride IID
    private InstanceIdentifier<OvsdbBridgeAugmentation> createOvsdbBridgeIID(NodeId nodeId) {

        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId))
                .augmentation(OvsdbBridgeAugmentation.class);
    }

    //create sff data plane locator list
    private List<SffDataPlaneLocator> createSffDataPlaneLocatorList(Class transportType, LocatorType locatorType) {
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        SffOvsLocatorOptionsAugmentationBuilder sffOvsLocatorOptionsAugmentationBuilder = new SffOvsLocatorOptionsAugmentationBuilder();

        //create and set ovs options
        ovsOptionsBuilder.setLocalIp(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
                .setLocalIp(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
                .setDstPort(SfcOvsUtil.OVSDB_OPTION_DST_PORT)
                .setNsp(SfcOvsUtil.OVSDB_OPTION_NSP)
                .setNsi(SfcOvsUtil.OVSDB_OPTION_NSI)
                .setKey(SfcOvsUtil.OVSDB_OPTION_KEY);
        sffOvsLocatorOptionsAugmentationBuilder.setOvsOptions(ovsOptionsBuilder.build());

        //set data plane locator
        //noinspection unchecked
        dataPlaneLocatorBuilder.setTransport(transportType);
        dataPlaneLocatorBuilder.setLocatorType(locatorType);

        //set sff data plane locator
        sffDataPlaneLocatorBuilder.setName(dplName)
                .setDataPlaneLocator(dataPlaneLocatorBuilder.build())
                .addAugmentation(SffOvsLocatorOptionsAugmentation.class, sffOvsLocatorOptionsAugmentationBuilder.build());

        //add entry into data plane locator list
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        return sffDataPlaneLocatorList;
    }
}
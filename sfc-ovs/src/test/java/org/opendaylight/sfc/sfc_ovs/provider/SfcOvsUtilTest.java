package org.opendaylight.sfc.sfc_ovs.provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.*;

/**
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
    private static final String testBridgeName = "Test bridge name";
    private static final String testNode = "Test node";
    private static final String testString = "Test string";
    private static final String sffDataPlaneLocator = "sffDataPlaneLocator test";
    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private String testIpAddress = "";
    private InstanceIdentifier<Node> nodeIID;
    private InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID;
    private IpAddress ipAddress;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;
    private OvsNodeBuilder ovsNodeBuilder;
    private boolean result;
    private List<SffDataPlaneLocator> sffDataPlaneLocatorList;
    private ExecutorService executorService;
    private SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder;
    private DataPlaneLocatorBuilder dataPlaneLocatorBuilder;
    private ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder;
    private ServiceFunctionForwarder serviceFunctionForwarder;


    @Before
    public void init() {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executorService = opendaylightSfc.getExecutor();
    }

    @Test
    public void testCreateObject() {

        //just create an object of class SfcOvsUtil
        SfcOvsUtil sfcOvsUtil = new SfcOvsUtil();
        sfcOvsUtil.getClass();
    }

    @Test
    public void testSuccessfulSubmitCallable() throws Exception {

        //create test call() method
        class CallableTest implements Callable {

            @Override
            public String call() throws Exception {
                return testString;
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        Object object = SfcOvsUtil.submitCallable(new CallableTest(), executorService);

        assertNotNull("Must not be null", object);
        assertEquals("Must be equal", object, testString);
    }

    @Test
    public void testUnsuccessfulSubmitCallable() throws Exception {

        //call() method throws exception
        class CallableTest implements Callable {

            @Override
            public Object call() throws Exception {
                throw new InterruptedException();
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        Object object = SfcOvsUtil.submitCallable(new CallableTest(), executorService);

        assertNull("Must be null", object);
    }

    @Test
    public void testBuildOvsdbTopologyIID() {
        InstanceIdentifier<Topology> instanceIdentifierList = SfcOvsUtil.buildOvsdbTopologyIID();

        assertEquals("Must be equal", instanceIdentifierList.getTargetType().getName(), Topology.class.getName());
    }

    @Test
    public void testGetManagedByNodeId() throws Exception {
        ovsNodeBuilder = new OvsNodeBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        //create nodeIID
        nodeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)));

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(nodeIID));
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(nodeIID));

        NodeId nodeId = Whitebox.invokeMethod(SfcOvsUtil.class, "getManagedByNodeId", ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must not be null", nodeId);
        assertFalse("Must be false", nodeId.getValue().isEmpty());
        assertEquals("Must be equal", nodeId.getValue(), testNode + OVSDB_BRIDGE_PREFIX + testBridgeName);
    }

    @Test
    public void testBuildOvsdbNodeIID_OvsdbBridgeAugmentation() {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();

        //create nodeIID
        nodeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)));

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(nodeIID));
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(nodeIID));

        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must not be null", nodeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testNode + OVSDB_BRIDGE_PREFIX + testBridgeName);
    }

    @Test
    public void testBuildOvsdbNodeIID_String() {
        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(testString);

        Assert.assertEquals("Must be equal", InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testString);
    }

    @Test
    public void testBuildOvsdbNodeIID_NodeID() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(testNode));
        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(nodeBuilder.build().getNodeId());

        Assert.assertEquals("Must be equal", InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testNode);
    }

    @Test
    public void testBuildOvsdbBridgeIID_OvsdbBridgeAugmentation() {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();

        //create bridgeIID
        bridgeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)))
                .augmentation(OvsdbBridgeAugmentation.class);

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(bridgeIID));
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(bridgeIID));

        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(ovsdbBridgeAugmentationBuilder.build());

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal", bridgeIID.getTargetType(), OvsdbBridgeAugmentation.class);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(),
                testNode + OVSDB_BRIDGE_PREFIX + testBridgeName);
    }

    @Test
    public void testBuildOvsdbBridgeIID_String() {
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(testString);

        assertNotNull("Must not be null", bridgeIID);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(bridgeIID.firstIdentifierOf(Node.class)).getNodeId().getValue(), testString);
    }

    @Test
    public void testBuildOvsdbTerminationPointAugmentationIID() {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        bridgeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)))
                .augmentation(OvsdbBridgeAugmentation.class);

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(bridgeIID));

        //first parameter
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(bridgeIID));

        //second parameter
        ovsdbTerminationPointAugmentationBuilder.setName(testString);

        InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointAugmentationIID =
                SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(ovsdbBridgeAugmentationBuilder.build(), ovsdbTerminationPointAugmentationBuilder.build());

        assertNotNull("Must not be null", terminationPointAugmentationIID);
        assertEquals("Must be equal", terminationPointAugmentationIID.getTargetType(), OvsdbTerminationPointAugmentation.class);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(terminationPointAugmentationIID.firstIdentifierOf(Node.class)).getNodeId().getValue(),
                testNode + OVSDB_BRIDGE_PREFIX + testBridgeName);
        assertEquals("Must be equal", InstanceIdentifier.keyOf(terminationPointAugmentationIID.firstIdentifierOf(TerminationPoint.class)).getTpId().getValue(),
                testNode + OVSDB_BRIDGE_PREFIX + testBridgeName + OVSDB_TERMINATION_POINT_PREFIX + testString);
    }

    @Test
    public void testBuildOvsdbTerminationPointIID_Strings() {
        InstanceIdentifier<TerminationPoint> terminationPointIID = SfcOvsUtil.buildOvsdbTerminationPointIID(testString, sffDataPlaneLocator);

        Assert.assertEquals(InstanceIdentifier.keyOf(terminationPointIID).getTpId().getValue(), testString + OVSDB_TERMINATION_POINT_PREFIX + sffDataPlaneLocator);
    }

    @Test
    public void testConvertStringToIpAddress() throws Exception {
        //Incorrect Ip address format
        ipAddress = SfcOvsUtil.convertStringToIpAddress(testIpAddress);
        assertNull("Must be null", ipAddress);

        //Ip v4
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv4Address);
        assert ipAddress != null;
        assertEquals("Must be equal", ipAddress.getIpv4Address().getValue(), ipv4Address);

        //Ip v6
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv6Address);
        assert ipAddress != null;
        assertEquals("Must be equal", ipAddress.getIpv6Address().getValue(), ipv6Address);
    }

    @Test
    public void testConvertIpAddressToString() {
        //create Ip v4 address
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv4Address);

        testIpAddress = SfcOvsUtil.convertIpAddressToString(ipAddress);

        //Ip v4
        assertEquals("Must be equal", testIpAddress, ipAddress.getIpv4Address().getValue());

        //create Ip v6 address
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv6Address);

        testIpAddress = SfcOvsUtil.convertIpAddressToString(ipAddress);

        //Ip v6
        assertEquals("Must be equal", testIpAddress, ipAddress.getIpv6Address().getValue());
    }

    @Test
    public void testCreateAndDeleteOvsdbNode() {
        //create node
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(testNode))
                .setKey(new NodeKey(new NodeId(testNode + 1)));
        SfcDataStoreAPI.writePutTransactionAPI(createNodeIID(testNode + 1), nodeBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        //delete node only for sure)
        result = SfcOvsUtil.deleteOvsdbNode(createNodeIID(testNode + 1), executorService);

        assertNotNull("Must be not null", result);
        assertTrue("Must be true", result);
    }

    @Test
    public void testPutAndDeleteOvsdbTerminationPoint() {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        nodeIID = createNodeIID(testNode);
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorList = new ArrayList<>();
        SffOvsLocatorOptionsAugmentationBuilder sffOvsLocatorOptionsAugmentationBuilder = new SffOvsLocatorOptionsAugmentationBuilder();

        //set transport
        dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);

        //create ovs options
        ovsOptionsBuilder.setLocalIp(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
                .setLocalIp(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
                .setDstPort(SfcOvsUtil.OVSDB_OPTION_DST_PORT)
                .setNsp(SfcOvsUtil.OVSDB_OPTION_NSP)
                .setNsi(SfcOvsUtil.OVSDB_OPTION_NSI)
                .setKey(SfcOvsUtil.OVSDB_OPTION_KEY);

        //set options
        sffOvsLocatorOptionsAugmentationBuilder.setOvsOptions(ovsOptionsBuilder.build());

        //set name, dpl and augmentation
        sffDataPlaneLocatorBuilder.setName("Dpl")
                .setDataPlaneLocator(dataPlaneLocatorBuilder.build())
                .addAugmentation(SffOvsLocatorOptionsAugmentation.class, sffOvsLocatorOptionsAugmentationBuilder.build());

        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        result = SfcOvsUtil.putOvsdbTerminationPoints(createBridgeAugmentation(nodeIID), sffDataPlaneLocatorList, executorService);

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result);

        String sffName = testNode + OVSDB_BRIDGE_PREFIX + testBridgeName;
        result = SfcOvsUtil.deleteOvsdbTerminationPoint(SfcOvsUtil.buildOvsdbTerminationPointIID(sffName, "Dpl"), executorService);

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result);
    }

    @Test
    public void testAugmentSffWithOpenFlowNodeId_nullNodeId() {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(testString);

        serviceFunctionForwarder = SfcOvsUtil.augmentSffWithOpenFlowNodeId(serviceFunctionForwarderBuilder.build());

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getName(), testString);
    }

    @Test
    public void testAugmentSffWithOpenFlowNodeId() {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorList = new ArrayList<>();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();

        //set dpl
        dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        serviceFunctionForwarderBuilder.setName(testString)
                .setKey(new ServiceFunctionForwarderKey("Key"))
                .setSffDataPlaneLocator(sffDataPlaneLocatorList);

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOpenFlowNodeIdForSff")).toReturn("NodeId");

        serviceFunctionForwarder = SfcOvsUtil.augmentSffWithOpenFlowNodeId(serviceFunctionForwarderBuilder.build());

        assertNotNull("Must not be null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getKey().getName(), "Key");
        assertEquals("Must be equal",
                serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getDataPlaneLocator().getTransport().getName(),
                VxlanGpe.class.getName());
    }

    private InstanceIdentifier<Node> createNodeIID(String key) {
        nodeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(key)));

        return nodeIID;
    }

    private OvsdbBridgeAugmentation createBridgeAugmentation(InstanceIdentifier<?> nodeIID) {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(testBridgeName))
                .setManagedBy(new OvsdbNodeRef(nodeIID));

        return ovsdbBridgeAugmentationBuilder.build();
    }
}

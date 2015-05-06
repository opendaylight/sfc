package org.opendaylight.sfc.sfc_ovs.provider;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.powermock.reflect.Whitebox;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsUtil
 * <p/>
 * @since 2015-04-23
 */

public class SfcOvsUtilTest {
    private static final String ipv4Address = "170.0.0.1";
    private static final String ipv6Address = "0000:0000:0000:0000";
    private static final String testBridgeName = "Test bridge name";
    private static final String testNode = "Test node";
    private static final String testString = "Test string";
    private static final String sffName = "sffName test";
    private static final String sffDataPlaneLocator = "sffDataPlaneLocator test";
    private static String testIpAddress;
    private InstanceIdentifier<Node> nodeIID;
    private InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID;
    private IpAddress ipAddress;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;
    private OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder;
    private OvsNodeBuilder ovsNodeBuilder;

    @Test
    public void SfcOvsUtilTestObject() {
        SfcOvsUtil sfcOvsUtil = new SfcOvsUtil();
        sfcOvsUtil.getClass();
    }

    @Test
    public void buildOvsdbTopologyIIDTest() {
        InstanceIdentifier<Topology> instanceIdentifierList = SfcOvsUtil.buildOvsdbTopologyIID();

        //Build InstanceIdentifier<Topology>
        Assert.assertEquals(instanceIdentifierList.getTargetType().getName(), Topology.class.getName());
    }

    @Test
    public void getManagedByNodeIdTestWhereBridgeIsNull() throws Exception {
        //OvsdBridge is null
        try {
            Whitebox.invokeMethod(SfcOvsUtil.class, "getManagedByNodeId", ovsdbBridgeAugmentationBuilder);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void getManagedByNodeIdTestWhereBridgeNameIsNull() throws Exception {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(null);

        //BridgeName is null
        try {
            Whitebox.invokeMethod(SfcOvsUtil.class, "getManagedByNodeId", ovsdbBridgeAugmentationBuilder.build());
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void getManagedByNodeIdTestWhereManagedByIsNull() throws Exception {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();


        //ManagedBy is null
        try {
            Whitebox.invokeMethod(SfcOvsUtil.class, "getManagedByNodeId", ovsdbBridgeAugmentationBuilder.build());
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void getManagedByNodeIdTest() throws Exception {
        nodeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)));
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();

        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName));
        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(nodeIID));
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(nodeIID));
        Whitebox.invokeMethod(SfcOvsUtil.class, "getManagedByNodeId", ovsdbBridgeAugmentationBuilder.build());

        //NodeID test
        Assert.assertEquals(InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testNode);
    }

    @Test
    public void buildOvsdbNodeIIDTestWithOvsdbBridgeAugmentationParameter() {
        nodeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)));
        ovsNodeBuilder = new OvsNodeBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(nodeIID));
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName));
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(nodeIID));
        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(ovsdbBridgeAugmentationBuilder.build());

        //Node + bridgeName test
        Assert.assertEquals(InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testNode + "/bridge/" + testBridgeName);
    }

    @Test
    public void buildOvsdbNodeIIDTestWithStringParameter() {
        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(testString);

        //NodeId test
        Assert.assertEquals(InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testString);
    }

    @Test
    public void buildOvsdbNodeIIDTestWithNodeIDParameter() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(testNode));
        nodeIID = SfcOvsUtil.buildOvsdbNodeIID(nodeBuilder.build().getNodeId());

        //NodeId test
        Assert.assertEquals(InstanceIdentifier.keyOf(nodeIID).getNodeId().getValue(), testNode);
    }

    @Test
    public void buildOvsdbBridgeIIDTestWithOvsdbBridgeAugmentationParameter() {
        bridgeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)))
                .augmentation(OvsdbBridgeAugmentation.class);
        ovsNodeBuilder = new OvsNodeBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(bridgeIID));
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName));
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(bridgeIID));
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(ovsdbBridgeAugmentationBuilder.build());

        //OvsdbBridgeAugmentationTest
        Assert.assertEquals(bridgeIID.getTargetType(), OvsdbBridgeAugmentation.class);
    }

    @Test
    public void buildOvsdbBridgeIIDTestWithStringParameter() {
        bridgeIID = SfcOvsUtil.buildOvsdbBridgeIID(testString);

        //OvsdbBridgeAugmentationTest
        Assert.assertEquals(bridgeIID.getTargetType(), OvsdbBridgeAugmentation.class);
    }

    @Test
    public void buildOvsdbTerminationPointAugmentationIIDTestWhereOvsdbTerminationPointAugmentationIsNull() {
        //OvsdbTerminationPointAugmentationIsNull
        try {
            SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(null, null);
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }


    @Test
    public void buildOvsdbTerminationPointAugmentationIIDTestWhereTerminationPointNameIsNull() {

        //TerminationPoint is null
        try {
            SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(null, ovsdbTerminationPointAugmentationBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildOvsdbTerminationPointAugmentationIIDTestWhereOvsdbBridgeIsNull() {
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(testString);

        //OvsdbBridge is null
        try {
            SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(null, ovsdbTerminationPointAugmentationBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildOvsdbTerminationPointAugmentationIIDTest() {
        bridgeIID = InstanceIdentifier
                .create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(testNode)))
                .augmentation(OvsdbBridgeAugmentation.class);
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(testString);
        ovsNodeBuilder = new OvsNodeBuilder();

        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(bridgeIID));
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testBridgeName));
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(bridgeIID));
        InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointAugmentationIID = SfcOvsUtil.buildOvsdbTerminationPointAugmentationIID(ovsdbBridgeAugmentationBuilder.build(), ovsdbTerminationPointAugmentationBuilder.build());

        //buildOvsdbTerminationPointAugmentationIID test
        Assert.assertEquals(terminationPointAugmentationIID.getTargetType(), OvsdbTerminationPointAugmentation.class);
    }

    @Test
    public void buildOvsdbTerminationPointIIDTest() {
        InstanceIdentifier<TerminationPoint> terminationPointIID = SfcOvsUtil.buildOvsdbTerminationPointIID(sffName, "sffDataPlaneLocator test");

        //buildOvsdbTerminationPointIID test
        Assert.assertEquals(InstanceIdentifier.keyOf(terminationPointIID).getTpId().getValue(), sffName + "/terminationpoint/" + sffDataPlaneLocator);
    }

    @Test
    public void convertStringToIpAddressTestIncorrectString() {
        ipAddress = SfcOvsUtil.convertStringToIpAddress("");

        //Empty String
        Assert.assertEquals(ipAddress, null);
    }

    @Test
    public void convertStringToIpAddressTests() {
        //Incorrect Ip address format
        ipAddress = SfcOvsUtil.convertStringToIpAddress(testIpAddress);
        Assert.assertEquals(ipAddress.getIpv4Address(), null);

        //Ip v4 test
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv4Address);
        Assert.assertEquals(ipAddress.getIpv4Address().getValue(), ipv4Address);

        //Ip v6 test
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv6Address);
        Assert.assertEquals(ipAddress.getIpv6Address().getValue(), ipv6Address);
    }

    @Test
    public void convertIpAddressToStringTestIncorrectIpAddress() {
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv4Address);
        testIpAddress = SfcOvsUtil.convertIpAddressToString(ipAddress);

        //Ip v4 test
        Assert.assertEquals(testIpAddress, ipAddress.getIpv4Address().getValue());

        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv6Address);
        testIpAddress = SfcOvsUtil.convertIpAddressToString(ipAddress);

        //Ip v6 test
        Assert.assertEquals(testIpAddress, ipAddress.getIpv6Address().getValue());
    }

    @Test
    public void convertIpAddressToStringTests() {
        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv4Address);
        testIpAddress = SfcOvsUtil.convertIpAddressToString(ipAddress);

        //Ip v4 test
        Assert.assertEquals(testIpAddress, ipAddress.getIpv4Address().getValue());

        ipAddress = SfcOvsUtil.convertStringToIpAddress(ipv6Address);
        testIpAddress = SfcOvsUtil.convertIpAddressToString(ipAddress);

        //Ip v6 test
        Assert.assertEquals(testIpAddress, ipAddress.getIpv6Address().getValue());
    }
}
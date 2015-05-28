package org.opendaylight.sfc.sfc_ovs.provider.api;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsToSffMappingAPI
 * <p/>
 * @since 2015-05-26
 */

public class SfcOvsDataStoreAPITest extends AbstractDataBrokerTest {

    private static final String bridgeName = "bridge_name";
    private static final int numberOfNodes = 7;
    private final Object[] methodParams = new Object[2];
    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private final String dplName = "dpl name";
    private final String partialIp = "170.0.0.";
    private final Logger LOG = LoggerFactory.getLogger(SfcOvsDataStoreAPI.class);
    private SfcOvsDataStoreAPI sfcOvsDataStoreAPIObject;
    private SfcOvsDataStoreAPI.Method methodToCall;
    private Object testResult;
    private boolean allOk;

    @Before
    public void init() {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);

        //create nodes before testing
        createOvsdbNodes();
    }

    @After
    public void deleteAllNodes() {
        allOk = true;
        boolean isDeleted;

        for (int i = 1; i <= numberOfNodes; i++) {
            isDeleted = SfcDataStoreAPI.deleteTransactionAPI(createNodeIID(partialIp + i), LogicalDatastoreType.OPERATIONAL);
            if (!isDeleted)
                allOk = false;
        }

        if (allOk)
            LOG.debug("All created nodes has been deleted");
        else
            LOG.debug("Some of the created nodes has not been deleted");
    }

    @Test
    public void testReadOvsdbBridge() throws Exception {
        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_BRIDGE;
        methodParams[0] = createBridgeIID(partialIp + 2);

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
        assertEquals("Must be true", testResult, createOvsdbBridgeAugmentation(2));
    }

    @Test
    public void testPutOvsdbBridge() throws Exception {
        methodToCall = SfcOvsDataStoreAPI.Method.PUT_OVSDB_BRIDGE;
        methodParams[0] = createOvsdbBridgeAugmentation(3);

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
        assertTrue("Must be true", (boolean) testResult);
    }

    @Test
    public void testDeleteOvsdbNode() throws Exception {
        methodToCall = SfcOvsDataStoreAPI.Method.DELETE_OVSDB_NODE;
        methodParams[0] = createNodeIID(partialIp + 4);

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
        assertTrue("Must be true", (boolean) testResult);
    }

    @Test
    public void testPutAndDeleteOvsdbTerminationPoint() throws Exception {
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder.setName(dplName);
        SffDataPlaneLocator sffDataPlaneLocator = sffDataPlaneLocatorBuilder.build();

        //put tp
        methodToCall = SfcOvsDataStoreAPI.Method.PUT_OVSDB_TERMINATION_POINT;
        methodParams[0] = createOvsdbBridgeAugmentation(5);
        methodParams[1] = createOvsdbTerminationPointAugmentation();
        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
        assertTrue("Must be true", (boolean) testResult);

        //delete tp
        methodToCall = SfcOvsDataStoreAPI.Method.DELETE_OVSDB_TERMINATION_POINT;
        methodParams[0] = createOvsdbTerminationPointIID(partialIp + 5, sffDataPlaneLocator.getName());

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
        assertTrue("Must be true", (boolean) testResult);
    }

    @Test
    public void testReadOvsdbNodeByIp() throws Exception {

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_IP;
        methodParams[0] = partialIp + 6;

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
        assertEquals("Must be equal", ((Node) testResult).getKey().getNodeId().getValue(), partialIp + 6);
    }

    @Test
    public void testReadOvsdbNodeByRef() throws Exception {
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(createNodeIID(partialIp + 7));

        methodToCall = SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_REF;
        methodParams[0] = ovsdbNodeRef;

        sfcOvsDataStoreAPIObject = new SfcOvsDataStoreAPI(methodToCall, methodParams);
        testResult = sfcOvsDataStoreAPIObject.call();

        assertNotNull("Must not be null", testResult);
        assertEquals("Must be equal", ((Node) testResult).getKey().getNodeId().getValue(), partialIp + 7);
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

    private void createOvsdbNodes() {

        /*Here are created all nodes required for testing*/

        allOk = true;
        boolean isCreated;

        for (int i = 1; i <= numberOfNodes; i++) {
            OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();

            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setNodeId(new NodeId(Integer.toString(i)))
                    .setTerminationPoint(createTerminationPointList())
                    .addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugmentationBuilder.build())
                    .addAugmentation(OvsdbBridgeAugmentation.class, createOvsdbBridgeAugmentation(i))
                    .setKey(new NodeKey(new NodeId(partialIp + Integer.toString(i))));
            if (i == 1) {
                isCreated = SfcDataStoreAPI.writePutTransactionAPI(createNodeIID(partialIp + i),
                        nodeBuilder.build(), LogicalDatastoreType.CONFIGURATION);
            } else {
                isCreated = SfcDataStoreAPI.writePutTransactionAPI(createNodeIID(partialIp + i),
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

    private OvsdbBridgeAugmentation createOvsdbBridgeAugmentation(int index) {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(createNodeIID(partialIp + index)));
        return ovsdbBridgeAugmentationBuilder.build();
    }

    private OvsdbTerminationPointAugmentation createOvsdbTerminationPointAugmentation() {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(dplName);
        return ovsdbTerminationPointAugmentationBuilder.build();
    }

    private List<TerminationPoint> createTerminationPointList() {
        List<TerminationPoint> terminationPointList = new ArrayList<>();
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        String tpId = "tpId";
        terminationPointBuilder.setTpId(new TpId(tpId));
        terminationPointList.add(terminationPointBuilder.build());
        return terminationPointList;
    }

    private InstanceIdentifier<Node> createNodeIID(String sffName) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(sffName)));
    }

    private InstanceIdentifier<OvsdbBridgeAugmentation> createBridgeIID(String sffName) {
        return createNodeIID(sffName)
                .augmentation(OvsdbBridgeAugmentation.class);
    }

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
}
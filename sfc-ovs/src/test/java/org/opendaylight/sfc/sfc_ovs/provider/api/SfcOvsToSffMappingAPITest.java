package org.opendaylight.sfc.sfc_ovs.provider.api;

import org.junit.Test;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;


/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsToSffMappingAPI
 * <p/>
 * @since 2015-04-20
 */

public class SfcOvsToSffMappingAPITest {
    private static final String OVSDB_OPTION_LOCAL_IP = "172.0.0.0";
    private static final String OVSDB_OPTION_REMOTE_IP = "172.0.0.1";
    private static final String testName = "Test name";
    private static final String nodeId = "Node ID test";
    private static final String dataPathId = "00:00:00:00:00:00:00:00";
    private BridgeOtherConfigsBuilder bridgeOtherConfigsBuilder;
    private IpAddress ipAddress;
    private List<Options> options;
    private List<TerminationPoint> terminationPointList;
    private NodeBuilder nodeBuilder;
    private OptionsBuilder optionsBuilder;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;
    private OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder;
    private ServiceFunctionForwarder serviceFunctionForwarder;

    @Test
    public void testCreateSfcOvsToSffMappingAPIObject() {
        SfcOvsToSffMappingAPI sfcOvsToSffMappingAPI = new SfcOvsToSffMappingAPI();
        sfcOvsToSffMappingAPI.getClass();
    }

    @Test
    public void testBuildServiceFunctionForwarderFromNode_nullOvsdba() throws Exception {
        nodeBuilder = new NodeBuilder();

        serviceFunctionForwarder = SfcOvsToSffMappingAPI.buildServiceFunctionForwarderFromNode(nodeBuilder.build());

        assertNull("Must be not null", serviceFunctionForwarder);
    }

    @Test
    public void testBuildServiceFunctionForwarderFromNode() throws Exception {
        OvsdbBridgeName ovsdbBridgeName;
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        options = new ArrayList<>();
        optionsBuilder = new OptionsBuilder();
        terminationPointList = new ArrayList<>();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        nodeBuilder = new NodeBuilder();

        //set options
        optionsBuilder.setKey(new OptionsKey(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP))
                .setValue(OVSDB_OPTION_LOCAL_IP);
        options.add(optionsBuilder.build());

        ovsdbTerminationPointAugmentationBuilder.setName(testName)
                .setOptions(options);

        terminationPointBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbTerminationPointAugmentationBuilder.build());
        terminationPointList.add(terminationPointBuilder.build());

        ovsdbBridgeName = new OvsdbBridgeName(testName);
        ovsdbBridgeAugmentationBuilder.setBridgeName(ovsdbBridgeName)
                .setDatapathId(new DatapathId(dataPathId));
        nodeBuilder.setNodeId(new NodeId(nodeId))
                .setTerminationPoint(terminationPointList)
                .addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

        serviceFunctionForwarder = SfcOvsToSffMappingAPI.buildServiceFunctionForwarderFromNode(nodeBuilder.build());

        assertNotNull("Must be not null", serviceFunctionForwarder);
        assertEquals("Must be equal", serviceFunctionForwarder.getName(), nodeId);
    }

    @Test
    public void testBuildSffDataPlaneLocatorList() throws Exception {
        List<SffDataPlaneLocator> sffDataPlaneLocatorList;
        terminationPointList = new ArrayList<>();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        //empty list
        sffDataPlaneLocatorList = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildSffDataPlaneLocatorList", ovsdbBridgeAugmentationBuilder.build(), null);

        assertEquals("Must be equal", sffDataPlaneLocatorList, Collections.emptyList());

        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testName))
                .setBridgeUuid(ovsdbBridgeAugmentationBuilder.getBridgeUuid())
                .setDatapathId(new DatapathId(dataPathId));
        ovsdbTerminationPointAugmentationBuilder.setName(testName);
        terminationPointBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbTerminationPointAugmentationBuilder.build());
        terminationPointList.add(terminationPointBuilder.build());

        sffDataPlaneLocatorList = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildSffDataPlaneLocatorList", ovsdbBridgeAugmentationBuilder.build(), terminationPointList);

        assertEquals("Must be equal", sffDataPlaneLocatorList.get(0).getName(), testName);
    }

    @Test
    public void testBuildDataPlaneLocatorFromTerminationPoint() throws Exception {
        Class transportType;
        DataPlaneLocator dataPlaneLocator;
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        IpAddress ipAddress = SfcOvsUtil.convertStringToIpAddress(OVSDB_OPTION_LOCAL_IP);
        IpBuilder ipBuilder = new IpBuilder();
        options = new ArrayList<>();
        optionsBuilder = new OptionsBuilder();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        ipBuilder.setIp(ipAddress);
        dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
                .setValue(OVSDB_OPTION_LOCAL_IP);
        options.add(optionsBuilder.build());
        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_REMOTE_IP)
                .setValue(OVSDB_OPTION_REMOTE_IP);
        options.add(optionsBuilder.build());
        ovsdbTerminationPointAugmentationBuilder.setInterfaceType(InterfaceTypeVxlan.class)
                .setOptions(options);
        dataPlaneLocator = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildDataPlaneLocatorFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());

        //Correct transportType
        assertEquals("Must be equal", dataPlaneLocator.getTransport(), VxlanGpe.class);

        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder()
                .setInterfaceType(InterfaceTypePatch.class)
                .setOptions(options);
        dataPlaneLocator = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildDataPlaneLocatorFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());

        transportType = dataPlaneLocator.getTransport();

        //TransportType - second option
        assertEquals("Must be equal", Other.class, transportType);
    }

    @Test
    public void testBuildOvsOptionsFromTerminationPoint() throws Exception {
        final String OVSDB_OPTION_DST_PORT = "8080";
        final String OVSDB_OPTION_KEY_VALUE = "Key test value";
        final String OVSDB_OPTION_NSP_VALUE = "Nsp test value";
        final String OVSDB_OPTION_NSI_VALUE = "Nsi test value";
        OvsOptions ovsOptions;
        options = new ArrayList<>();
        optionsBuilder = new OptionsBuilder();

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)
                .setValue(OVSDB_OPTION_LOCAL_IP);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_REMOTE_IP)
                .setValue(OVSDB_OPTION_REMOTE_IP);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_DST_PORT)
                .setValue(OVSDB_OPTION_DST_PORT);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_KEY)
                .setValue(OVSDB_OPTION_KEY_VALUE);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSP)
                .setValue(OVSDB_OPTION_NSP_VALUE);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSI)
                .setValue(OVSDB_OPTION_NSI_VALUE);
        options.add(optionsBuilder.build());

        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setOptions(options);

        ovsOptions = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildOvsOptionsFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());

        String localAddress = ovsOptions.getLocalIp();
        String remoteAddress = ovsOptions.getRemoteIp();

        //Option list test
        assertEquals("Must be equal", OVSDB_OPTION_LOCAL_IP, localAddress);
        assertEquals("Must be equal", OVSDB_OPTION_REMOTE_IP, remoteAddress);
        assertEquals("Must be equal", OVSDB_OPTION_DST_PORT, ovsOptions.getDstPort());
        assertEquals("Must be equal", OVSDB_OPTION_KEY_VALUE, ovsOptions.getKey());
        assertEquals("Must be equal", OVSDB_OPTION_NSP_VALUE, ovsOptions.getNsp());
        assertEquals("Must be equal", OVSDB_OPTION_NSI_VALUE, ovsOptions.getNsi());
    }

    @Test
    public void testGetServiceForwarderNameFromNode() {
        nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(nodeId));
        String name;

        name = SfcOvsToSffMappingAPI.getServiceForwarderNameFromNode(nodeBuilder.build());

        assertEquals("Must be equal", name, nodeId);
    }

    @Test
    public void testGetOvsBridgeLocalIp1() throws Exception {
        List<BridgeOtherConfigs> bridgeOtherConfigsList = new ArrayList<>();
        bridgeOtherConfigsBuilder = new BridgeOtherConfigsBuilder();
        bridgeOtherConfigsBuilder.setBridgeOtherConfigKey("Test Key");
        bridgeOtherConfigsList.add(bridgeOtherConfigsBuilder.build());
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(bridgeOtherConfigsList);

        ipAddress = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "getOvsBridgeLocalIp", ovsdbBridgeAugmentationBuilder.build());

        //Key is not equal
        assertNull("Must be null", ipAddress);
    }

    @Test
    public void testGetOvsBridgeLocalIp2() throws Exception {
        List<BridgeOtherConfigs> bridgeOtherConfigsList = new ArrayList<>();
        bridgeOtherConfigsBuilder = new BridgeOtherConfigsBuilder();
        bridgeOtherConfigsBuilder.setBridgeOtherConfigKey(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        bridgeOtherConfigsBuilder.setBridgeOtherConfigValue(OVSDB_OPTION_LOCAL_IP);
        bridgeOtherConfigsList.add(bridgeOtherConfigsBuilder.build());
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(bridgeOtherConfigsList);

        ipAddress = SfcOvsToSffMappingAPI.getOvsBridgeLocalIp(ovsdbBridgeAugmentationBuilder.build());

        //Key is equal
        assertNotNull("Must not be null", ipAddress);
        assertEquals(ipAddress.getIpv4Address().getValue(), OVSDB_OPTION_LOCAL_IP);

    }
}






























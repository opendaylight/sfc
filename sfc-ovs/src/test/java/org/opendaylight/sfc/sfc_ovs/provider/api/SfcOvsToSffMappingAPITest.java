package org.opendaylight.sfc.sfc_ovs.provider.api;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsToSffMappingAPI
 * <p/>
 * @since 2015-04-20
 */

public class SfcOvsToSffMappingAPITest {
    private static final String OVSDB_OPTION_LOCAL_IP = "172.0.0.0";
    private static final String testName = "Test name";
    private static final String nodeId = "Node ID test";
    private BridgeOtherConfigsBuilder bridgeOtherConfigsBuilder;
    private IpAddress ipAddress;
    private List<Options> options;
    private List<TerminationPoint> terminationPointList;
    private NodeBuilder nodeBuilder;
    private OptionsBuilder optionsBuilder;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;
    private OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder;

    @Test
    public void createSfcOvsToSffMappingAPITest() {
        SfcOvsToSffMappingAPI sfcOvsToSffMappingAPI = new SfcOvsToSffMappingAPI();
        sfcOvsToSffMappingAPI.getClass();
    }

    @Test
    public void buildServiceFunctionForwarderFromNodeWhereNodeIsNull() {
        nodeBuilder = new NodeBuilder();
        nodeBuilder = null;

        //Node is null
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildServiceFunctionForwarderFromNode", nodeBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildServiceFunctionForwarderFromNodeWhereAugmentationIsNull() {
        nodeBuilder = new NodeBuilder();
        nodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, null);

        //OvsdbBridgeAugmentation is null
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildServiceFunctionForwarderFromNode", nodeBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildServiceFunctionForwarderFromNodeTest() throws Exception {
        OvsdbBridgeName ovsdbBridgeName;
        ServiceFunctionForwarder serviceFunctionForwarder;
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        nodeBuilder = new NodeBuilder();

        ovsdbBridgeName = new OvsdbBridgeName(testName);
        ovsdbBridgeAugmentationBuilder.setBridgeName(ovsdbBridgeName);
        ovsdbBridgeAugmentationBuilder.setDatapathId(new DatapathId("00:00:00:00:00:00:00:00"));
        nodeBuilder.setNodeId(new NodeId(nodeId));
        nodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

        //buildServiceFunctionForwarderFromNode test
        serviceFunctionForwarder = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildServiceFunctionForwarderFromNode", nodeBuilder.build());

        Assert.assertEquals(serviceFunctionForwarder.getName(), nodeId);
    }

    @Test
    public void buildSffDataPlaneLocatorListWhereOvsdbBridgeIsNull() throws Exception {
        terminationPointList = new ArrayList<>();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder = null;

        //OvsdbBridge is null
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildSffDataPlaneLocatorList", ovsdbBridgeAugmentationBuilder.build(), terminationPointList);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildSffDataPlaneLocatorListWhereTerminationPointIsNull() throws Exception {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        terminationPointList = new ArrayList<>();

        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testName));
        ovsdbBridgeAugmentationBuilder.setBridgeUuid(ovsdbBridgeAugmentationBuilder.getBridgeUuid());
        terminationPointList = null;

        //TerminationPoint is null
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildSffDataPlaneLocatorList", ovsdbBridgeAugmentationBuilder.build(), terminationPointList);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildSffDataPlaneLocatorListTest() throws Exception {
        List<SffDataPlaneLocator> sffDataPlaneLocatorList;
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        terminationPointList = new ArrayList<>();

        ovsdbBridgeAugmentationBuilder.setBridgeName(OvsdbBridgeName.getDefaultInstance(testName));
        ovsdbBridgeAugmentationBuilder.setBridgeUuid(ovsdbBridgeAugmentationBuilder.getBridgeUuid());
        ovsdbBridgeAugmentationBuilder.setDatapathId(new DatapathId("00:00:00:00:00:00:00:00"));
        ovsdbTerminationPointAugmentationBuilder.setName(testName);
        terminationPointBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbTerminationPointAugmentationBuilder.build());
        terminationPointList.add(terminationPointBuilder.build());

        //buildSffDataPlaneLocatorList test
        sffDataPlaneLocatorList = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildSffDataPlaneLocatorList", ovsdbBridgeAugmentationBuilder.build(), terminationPointList);

        Assert.assertEquals(sffDataPlaneLocatorList.get(0).getName(), testName);
    }

    @Test
    public void buildDataPlaneLocatorFromTerminationPointWhereTPIsNull() {
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder = null;

        //TerminationPoint is null
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildDataPlaneLocatorFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildDataPlaneLocatorFromTerminationPointWhereOptionIsNull() {
        options = new ArrayList<>();
        optionsBuilder = new OptionsBuilder();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        optionsBuilder.setValue(null);
        options.add(optionsBuilder.build());
        ovsdbTerminationPointAugmentationBuilder.setOptions(options);

        //Option ""local ip" has no value
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildDataPlaneLocatorFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildDataPlaneLocatorFromTerminationPointTest() throws Exception {
        Class transportType;
        DataPlaneLocator dataPlaneLocator;
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        IpAddress ipAddress = SfcOvsUtil.convertStringToIpAddress(OVSDB_OPTION_LOCAL_IP);
        IpBuilder ipBuilder = new IpBuilder();
        LocatorType locatorType;
        options = new ArrayList<>();
        optionsBuilder = new OptionsBuilder();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        ipBuilder.setIp(ipAddress);
        dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        optionsBuilder.setValue(OVSDB_OPTION_LOCAL_IP);
        options.add(optionsBuilder.build());
        ovsdbTerminationPointAugmentationBuilder.setInterfaceType(InterfaceTypeVxlan.class);
        ovsdbTerminationPointAugmentationBuilder.setOptions(options);
        dataPlaneLocator = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildDataPlaneLocatorFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());
        locatorType = dataPlaneLocator.getLocatorType();
        transportType = dataPlaneLocator.getTransport();

        //LocatorType test
        Assert.assertEquals(dataPlaneLocatorBuilder.getLocatorType(), locatorType);
        //Correct transportType
        Assert.assertEquals(VxlanGpe.class, transportType);

        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setInterfaceType(InterfaceTypePatch.class);
        ovsdbTerminationPointAugmentationBuilder.setOptions(options);
        dataPlaneLocator = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildDataPlaneLocatorFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());

        transportType = dataPlaneLocator.getTransport();
        //TransportType - second option
        Assert.assertEquals(Other.class, transportType);
    }

    @Test
    public void buildOvsOptionsFromTerminationPointWhereTerminationPointIsNull() {
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder = null;

        //TerminationPoint is null
        try {
            org.powermock.reflect.Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildOvsOptionsFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildOvsOptionsFromTerminationPointWhereListIsNull() {
        options = new ArrayList<>();
        optionsBuilder = new OptionsBuilder();
        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        optionsBuilder.setOption(null);
        optionsBuilder.setValue(null);
        options.add(optionsBuilder.build());
        ovsdbTerminationPointAugmentationBuilder.setOptions(options);

        //Options list is null
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildOvsOptionsFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildOvsOptionsFromTerminationPointTest() throws Exception {
        final String OVSDB_OPTION_REMOTE_IP = "172.0.0.1";
        final String OVSDB_OPTION_DST_PORT = "8080";
        final String OVSDB_OPTION_KEY_VALUE = "Key test value";
        final String OVSDB_OPTION_NSP_VALUE = "Nsp test value";
        final String OVSDB_OPTION_NSI_VALUE = "Nsi test value";
        OvsOptions ovsOptions;
        options = new ArrayList<>();
        optionsBuilder = new OptionsBuilder();

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        optionsBuilder.setValue(OVSDB_OPTION_LOCAL_IP);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_REMOTE_IP);
        optionsBuilder.setValue(OVSDB_OPTION_REMOTE_IP);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_DST_PORT);
        optionsBuilder.setValue(OVSDB_OPTION_DST_PORT);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_KEY);
        optionsBuilder.setValue(OVSDB_OPTION_KEY_VALUE);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSP);
        optionsBuilder.setValue(OVSDB_OPTION_NSP_VALUE);
        options.add(optionsBuilder.build());

        optionsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSI);
        optionsBuilder.setValue(OVSDB_OPTION_NSI_VALUE);
        options.add(optionsBuilder.build());

        ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setOptions(options);

        ovsOptions = org.powermock.reflect.Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "buildOvsOptionsFromTerminationPoint", ovsdbTerminationPointAugmentationBuilder.build());

        String localAddress = SfcOvsUtil.convertIpAddressToString(ovsOptions.getLocalIp());
        String remoteAddress = SfcOvsUtil.convertIpAddressToString(ovsOptions.getRemoteIp());

        //Option list test
        Assert.assertEquals(OVSDB_OPTION_LOCAL_IP, localAddress);
        Assert.assertEquals(OVSDB_OPTION_REMOTE_IP, remoteAddress);
        Assert.assertEquals(OVSDB_OPTION_DST_PORT, ovsOptions.getDstPort().getValue().toString());
        Assert.assertEquals(OVSDB_OPTION_KEY_VALUE, ovsOptions.getKey());
        Assert.assertEquals(OVSDB_OPTION_NSP_VALUE, ovsOptions.getNsp());
        Assert.assertEquals(OVSDB_OPTION_NSI_VALUE, ovsOptions.getNsi());
    }

    @Test
    public void getServiceForwarderNameFromNodeWhereNodeIsNull() {
        //Node is null
        try {
            SfcOvsToSffMappingAPI.getServiceForwarderNameFromNode(null);
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }


    @Test
    public void getServiceForwarderNameFromNodeTest() {
        nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(nodeId));
        String name;

        name = SfcOvsToSffMappingAPI.getServiceForwarderNameFromNode(nodeBuilder.build());

        //Node name test
        Assert.assertEquals(name, nodeId);
    }

    @Test
    public void getOvsBridgeLocalIpTestWhereOvsdbBridgeIsNull() {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder = null;

        //ovsdbBridge is null
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "getOvsBridgeLocalIp", ovsdbBridgeAugmentationBuilder);
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void getOvsBridgeLocalIpTestWhereBridgeOtherConfigIsNull() {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();

        //bridgeOtherConfig is empty
        try {
            Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "getOvsBridgeLocalIp", ovsdbBridgeAugmentationBuilder.build());
        } catch (Throwable exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void getOvsBridgeLocalIpTestWhereKeyIsNotEqual() throws Exception {
        List<BridgeOtherConfigs> bridgeOtherConfigsList = new ArrayList<>();
        bridgeOtherConfigsBuilder = new BridgeOtherConfigsBuilder();
        bridgeOtherConfigsBuilder.setBridgeOtherConfigKey("Test Key");
        bridgeOtherConfigsList.add(bridgeOtherConfigsBuilder.build());
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(bridgeOtherConfigsList);

        ipAddress = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "getOvsBridgeLocalIp", ovsdbBridgeAugmentationBuilder.build());

        //Key is not equal
        Assert.assertEquals(ipAddress, null);
    }

    @Test
    public void getOvsBridgeLocalIpTestWhereKeyIEqual() throws Exception {
        List<BridgeOtherConfigs> bridgeOtherConfigsList = new ArrayList<>();
        bridgeOtherConfigsBuilder = new BridgeOtherConfigsBuilder();
        bridgeOtherConfigsBuilder.setBridgeOtherConfigKey(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        bridgeOtherConfigsBuilder.setBridgeOtherConfigValue(OVSDB_OPTION_LOCAL_IP);
        bridgeOtherConfigsList.add(bridgeOtherConfigsBuilder.build());
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(bridgeOtherConfigsList);

        ipAddress = Whitebox.invokeMethod(SfcOvsToSffMappingAPI.class, "getOvsBridgeLocalIp", ovsdbBridgeAugmentationBuilder.build());

        //Key is equal
        Assert.assertEquals(ipAddress.getIpv4Address().getValue(), OVSDB_OPTION_LOCAL_IP);
    }
}






























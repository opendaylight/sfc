package org.opendaylight.sfc.sfc_ovs.provider.api;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.util.HopOvsdbBridgePair;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcSffToOvsMappingAPI
 * <p/>
 * @since 2015-04-22
 */

public class SfcSffToOvsMappingAPITest {
    private static final String OVSDB_OPTION_LOCAL_IP_VALUE = "172.0.0.0";
    private static final String OVSDB_OPTION_REMOTE_IP_VALUE = "172.0.0.1";
    private static final String OVSDB_OPTION_DST_PORT_VALUE = "8080";
    private static final String OVSDB_OPTION_KEY = "Key";
    private static final String OVSDB_OPTION_NSP = "Nsp";
    private static final String OVSDB_OPTION_NSI = "Nsi";
    private static final Long pathId = 25L;
    private static final Short hopNumberFrom = 10, hopNumberTo = 5, serviceIndex = 15;
    private static final String renderedServicePathName = "Test name";
    private static final String ipAddress = "170.0.0.1";
    private Class<? extends InterfaceTypeBase> interfaceTypeClass;
    private DataPlaneLocatorBuilder dataPlaneLocatorBuilder;
    private HopOvsdbBridgePair hopOvsdbBridgePairFrom, hopOvsdbBridgePairTo;
    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options> optionsList;
    private List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointAugmentationList;
    private List<SffDataPlaneLocator> sffDataPlaneLocatorList;
    private OvsBridgeBuilder ovsBridgeBuilder;
    private OvsdbBridgeAugmentation ovsdbBridgeAugmentation;
    private OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder;
    private OvsOptionsBuilder ovsOptionsBuilder;
    private RenderedServicePathBuilder renderedServicePathBuilder;
    private RenderedServicePathHopBuilder renderedServicePathHopBuilderFrom, renderedServicePathHopBuilderTo;
    private ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder;
    private ServiceFunctionForwarder1Builder serviceFunctionForwarder1Builder;
    private ServiceFunctionForwarder2Builder serviceFunctionForwarder2Builder;
    private SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder;
    private SffDataPlaneLocator2Builder sffDataPlaneLocator2Builder;

    @Test
    public void SfcSffToOvsMappingAPITestObject() {
        SfcSffToOvsMappingAPI sfcSffToOvsMappingAPI = new SfcSffToOvsMappingAPI();
        sfcSffToOvsMappingAPI.getClass();
    }

    @Test
    public void buildOvsdbBridgeAugmentationTestWhereSffIsNull() throws Exception {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder = null;

        //Sff is null
        try {
            Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildOvsdbBridgeAugmentation", serviceFunctionForwarderBuilder);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildOvsdbBridgeAugmentationTestWhereOvsBridgeAugmentationIsNull() throws Exception {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();

        ovsdbBridgeAugmentation = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildOvsdbBridgeAugmentation", serviceFunctionForwarderBuilder.build());

        //ovsBridgeAugmentation is null
        Assert.assertEquals(ovsdbBridgeAugmentation, null);

    }

    @Test
    public void buildOvsdbBridgeAugmentationTestWhereOvsBridgeIsNull() throws Exception {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarder2Builder = new ServiceFunctionForwarder2Builder();

        serviceFunctionForwarder2Builder.setOvsBridge(null);
        serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder2.class, serviceFunctionForwarder2Builder.build());

        //OvsBridge is null

        ovsdbBridgeAugmentation = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildOvsdbBridgeAugmentation", serviceFunctionForwarderBuilder.build());

        Assert.assertEquals(ovsdbBridgeAugmentation, null);

    }

    @Test
    public void buildOvsdbBridgeAugmentationTestWhereOvsNodeAugmentationIsNull() throws Exception {
        ovsBridgeBuilder = new OvsBridgeBuilder();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarder1Builder = new ServiceFunctionForwarder1Builder();
        serviceFunctionForwarder2Builder = new ServiceFunctionForwarder2Builder();

        ovsBridgeBuilder.setBridgeName("Test Name");
        ovsBridgeBuilder.setUuid(null);
        serviceFunctionForwarder2Builder.setOvsBridge(ovsBridgeBuilder.build());
        serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder1.class, null);
        serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder2.class, serviceFunctionForwarder2Builder.build());

        //OvsNode is null
        ovsdbBridgeAugmentation = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildOvsdbBridgeAugmentation", serviceFunctionForwarderBuilder.build());

        Assert.assertEquals(ovsdbBridgeAugmentation, null);
    }

    @Test
    public void buildOvsdbBridgeAugmentationTestWhereOvsNodeIsNull() throws Exception {
        ovsBridgeBuilder = new OvsBridgeBuilder();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarder1Builder = new ServiceFunctionForwarder1Builder();
        serviceFunctionForwarder2Builder = new ServiceFunctionForwarder2Builder();

        serviceFunctionForwarder1Builder.setOvsNode(null);
        ovsBridgeBuilder.setBridgeName("Test Name");
        ovsBridgeBuilder.setUuid(null);
        serviceFunctionForwarder2Builder.setOvsBridge(ovsBridgeBuilder.build());
        serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder1.class, serviceFunctionForwarder1Builder.build());
        serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder2.class, serviceFunctionForwarder2Builder.build());

        //OvsNode is null
        ovsdbBridgeAugmentation = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildOvsdbBridgeAugmentation", serviceFunctionForwarderBuilder.build());

        Assert.assertEquals(ovsdbBridgeAugmentation, null);
    }

    @Test
    public void buildOvsdbBridgeAugmentationTest() throws Exception {
        OvsNodeBuilder ovsNodeBuilder = new OvsNodeBuilder();
        ovsBridgeBuilder = new OvsBridgeBuilder();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarder1Builder = new ServiceFunctionForwarder1Builder();
        serviceFunctionForwarder2Builder = new ServiceFunctionForwarder2Builder();

        serviceFunctionForwarder1Builder.setOvsNode(null);
        ovsBridgeBuilder.setBridgeName("Test Name");
        ovsBridgeBuilder.setUuid(null);
        serviceFunctionForwarder2Builder.setOvsBridge(ovsBridgeBuilder.build());
        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
                                new NodeKey(new NodeId("testNode")));
        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(nodeIID));
        serviceFunctionForwarder1Builder.setOvsNode(ovsNodeBuilder.build());
        serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder1.class, serviceFunctionForwarder1Builder.build());
        serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder2.class, serviceFunctionForwarder2Builder.build());

        //buildOvsdbBridgeAugmentation test
        ovsdbBridgeAugmentation = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildOvsdbBridgeAugmentation", serviceFunctionForwarderBuilder.build());

        Assert.assertEquals(ovsdbBridgeAugmentation.getBridgeName().getValue(), "Test Name");
    }

    @Test
    public void buildTerminationPointAugmentationListWhereListIsEmpty() throws Exception {
        sffDataPlaneLocatorList = null;

        //List is null
        try {
            Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildTerminationPointAugmentationList", sffDataPlaneLocatorList);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildTerminationPointAugmentationLisTest() throws Exception {
        sffDataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        ovsOptionsBuilder = new OvsOptionsBuilder();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocator2Builder = new SffDataPlaneLocator2Builder();

        sffDataPlaneLocatorBuilder.setName("Test name");

        ovsOptionsBuilder.setLocalIp(SfcOvsUtil.convertStringToIpAddress(OVSDB_OPTION_LOCAL_IP_VALUE));
        ovsOptionsBuilder.setRemoteIp(SfcOvsUtil.convertStringToIpAddress(OVSDB_OPTION_REMOTE_IP_VALUE));
        ovsOptionsBuilder.setDstPort(PortNumber.getDefaultInstance(OVSDB_OPTION_DST_PORT_VALUE));
        ovsOptionsBuilder.setKey(OVSDB_OPTION_KEY);
        ovsOptionsBuilder.setNsp(OVSDB_OPTION_NSP);
        ovsOptionsBuilder.setNsi(OVSDB_OPTION_NSI);

        sffDataPlaneLocator2Builder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator2.class, sffDataPlaneLocator2Builder.build());
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        //buildTerminationPointAugmentationList test
        ovsdbTerminationPointAugmentationList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildTerminationPointAugmentationList", sffDataPlaneLocatorList);

        Assert.assertEquals(ovsdbTerminationPointAugmentationList.get(0).getName(), "Test name");

    }

    @Test
    public void getDataPlaneLocatorOptionsWhereDataPlaneLocatorIsNull() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        optionsList = new ArrayList<>();

        optionsList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorOptions", dataPlaneLocatorBuilder.build());

        //DataPlaneLocator is null
        Assert.assertEquals(optionsList.toString(), "[]");
    }

    @Test
    public void getDataPlaneLocatorOptionsTest() throws Exception {
        IpBuilder ipBuilder = new IpBuilder();
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        optionsList = new ArrayList<>();

        ipBuilder.setIp(SfcOvsUtil.convertStringToIpAddress(OVSDB_OPTION_LOCAL_IP_VALUE));
        dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        optionsList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorOptions", dataPlaneLocatorBuilder.build());

        //buildDataPlaneLocator test
        Assert.assertEquals(optionsList.get(0).getValue(), OVSDB_OPTION_LOCAL_IP_VALUE);
    }

    @Test
    public void getSffDataPlaneLocatorOptionsTestWhereDataPlaneLocatorIsNull() throws Exception {
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder = null;

        //DataPlaneLocator is null
        try {
            Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getSffDataPlaneLocatorOptions", sffDataPlaneLocatorBuilder);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void getSffDataPlaneLocatorOptionsTestWhereArrayIsEmpty() throws Exception {
        ovsOptionsBuilder = new OvsOptionsBuilder();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocator2Builder = new SffDataPlaneLocator2Builder();

        ovsOptionsBuilder.setLocalIp(null);
        ovsOptionsBuilder.setRemoteIp(null);
        ovsOptionsBuilder.setDstPort(null);
        ovsOptionsBuilder.setKey(null);
        ovsOptionsBuilder.setNsp(null);
        ovsOptionsBuilder.setNsi(null);
        sffDataPlaneLocator2Builder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator2.class, sffDataPlaneLocator2Builder.build());
        optionsList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getSffDataPlaneLocatorOptions", sffDataPlaneLocatorBuilder.build());

        //Array is empty
        Assert.assertEquals(optionsList.toString(), "[]");
    }

    @Test
    public void getSffDataPlaneLocatorOptionsTest() throws Exception {
        optionsList = new ArrayList<>();
        ovsOptionsBuilder = new OvsOptionsBuilder();
        ovsdbTerminationPointAugmentationList = new ArrayList<>();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocator2Builder = new SffDataPlaneLocator2Builder();

        ovsOptionsBuilder.setLocalIp(SfcOvsUtil.convertStringToIpAddress(OVSDB_OPTION_LOCAL_IP_VALUE));
        ovsOptionsBuilder.setRemoteIp(SfcOvsUtil.convertStringToIpAddress(OVSDB_OPTION_REMOTE_IP_VALUE));
        ovsOptionsBuilder.setDstPort(PortNumber.getDefaultInstance(OVSDB_OPTION_DST_PORT_VALUE));
        ovsOptionsBuilder.setKey(OVSDB_OPTION_KEY);
        ovsOptionsBuilder.setNsp(OVSDB_OPTION_NSP);
        ovsOptionsBuilder.setNsi(OVSDB_OPTION_NSI);

        sffDataPlaneLocator2Builder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator2.class, sffDataPlaneLocator2Builder.build());
        optionsList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getSffDataPlaneLocatorOptions", sffDataPlaneLocatorBuilder.build());

        //Test all options
        Assert.assertEquals(optionsList.get(0).getValue(), OVSDB_OPTION_LOCAL_IP_VALUE);
        Assert.assertEquals(optionsList.get(1).getValue(), OVSDB_OPTION_REMOTE_IP_VALUE);
        Assert.assertEquals(optionsList.get(2).getValue(), OVSDB_OPTION_DST_PORT_VALUE);
        Assert.assertEquals(optionsList.get(3).getValue(), OVSDB_OPTION_KEY);
        Assert.assertEquals(optionsList.get(4).getValue(), OVSDB_OPTION_NSP);
        Assert.assertEquals(optionsList.get(5).getValue(), OVSDB_OPTION_NSI);
    }

    @Test
    public void getDataPlaneLocatorInterfaceTypeTestWhereDataPlaneLocatorIsNull() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder = null;

        //DataPlaneLocator is null
        try {
            Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorInterfaceType", dataPlaneLocatorBuilder);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void getDataPlaneLocatorInterfaceTypeTestNoTransport() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        dataPlaneLocatorBuilder.setTransport(null);
        interfaceTypeClass = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorInterfaceType", dataPlaneLocatorBuilder.build());

        //Set null to transport
        Assert.assertEquals(interfaceTypeClass, InterfaceTypeInternal.class);
    }

    @Test
    public void getDataPlaneLocatorInterfaceTypeTestWithOther() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        dataPlaneLocatorBuilder.setTransport(Other.class);
        interfaceTypeClass = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorInterfaceType", dataPlaneLocatorBuilder.build());

        //Other.class
        Assert.assertEquals(interfaceTypeClass, InterfaceTypeInternal.class);
    }

    @Test
    public void getDataPlaneLocatorInterfaceTypeTestWithVxLan() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
        interfaceTypeClass = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorInterfaceType", dataPlaneLocatorBuilder.build());

        //Other.class
        Assert.assertEquals(interfaceTypeClass, InterfaceTypeVxlan.class);
    }

    @Test
    public void buildVxlanTunnelDataPlaneLocatorTestWhereRenderedServicePathIsNull() throws Exception {
        RenderedServicePath renderedServicePath = null;

        //RenderedServicePath is null
        try {
            Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildVxlanTunnelDataPlaneLocator", renderedServicePath, null, null);
        } catch (Exception exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildVxlanTunnelDataPlaneLocatorTestWhereHopOvsdbBridgePairFromIsNull() throws Exception {
        renderedServicePathBuilder = new RenderedServicePathBuilder();

        //HopOvsdbBridgePairFrom is null
        try {
            Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildVxlanTunnelDataPlaneLocator", renderedServicePathBuilder.build(), null, null);
        } catch (Exception exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildVxlanTunnelDataPlaneLocatorTestWhereHopOvsdbBridgePairToIsNull() throws Exception {
        renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilderFrom = new RenderedServicePathHopBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        hopOvsdbBridgePairFrom = new HopOvsdbBridgePair(renderedServicePathHopBuilderFrom.build(), ovsdbBridgeAugmentationBuilder.build());

        //HopOvsdbBridgePairTo is null
        try {
            Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildVxlanTunnelDataPlaneLocator", renderedServicePathBuilder.build(), hopOvsdbBridgePairFrom, null);
        } catch (Exception exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void buildVxlanTunnelDataPlaneLocatorTest() throws Exception {
        List<BridgeOtherConfigs> bridgeOtherConfigsList = new ArrayList<>();
        BridgeOtherConfigsBuilder bridgeOtherConfigsBuilder = new BridgeOtherConfigsBuilder();
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilderFrom = new RenderedServicePathHopBuilder();
        renderedServicePathHopBuilderTo = new RenderedServicePathHopBuilder();
        SffDataPlaneLocator sffDataPlaneLocator;

        renderedServicePathBuilder.setName(renderedServicePathName);
        renderedServicePathBuilder.setPathId(pathId);
        renderedServicePathHopBuilderFrom.setHopNumber(hopNumberFrom);
        renderedServicePathHopBuilderFrom.setServiceIndex(serviceIndex);
        renderedServicePathHopBuilderTo.setHopNumber(hopNumberTo);
        bridgeOtherConfigsBuilder.setBridgeOtherConfigKey(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        bridgeOtherConfigsBuilder.setBridgeOtherConfigValue(ipAddress);
        bridgeOtherConfigsList.add(bridgeOtherConfigsBuilder.build());
        ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(bridgeOtherConfigsList);
        hopOvsdbBridgePairFrom = new HopOvsdbBridgePair(renderedServicePathHopBuilderFrom.build(), ovsdbBridgeAugmentationBuilder.build());
        hopOvsdbBridgePairTo = new HopOvsdbBridgePair(renderedServicePathHopBuilderTo.build(), ovsdbBridgeAugmentationBuilder.build());

        sffDataPlaneLocator = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildVxlanTunnelDataPlaneLocator", renderedServicePathBuilder.build(), hopOvsdbBridgePairFrom, hopOvsdbBridgePairTo);

        //buildVxlanTunnelDataPlaneLocatorName tests
        Assert.assertEquals(sffDataPlaneLocator.getName(), renderedServicePathName + "-vxlan-" + hopNumberFrom + "to" + hopNumberTo);
        Assert.assertEquals(sffDataPlaneLocator.getAugmentation(SffDataPlaneLocator2.class).getOvsOptions().getNsi(), serviceIndex.toString());
        Assert.assertEquals(sffDataPlaneLocator.getAugmentation(SffDataPlaneLocator2.class).getOvsOptions().getNsp(), pathId.toString());
        Assert.assertEquals(sffDataPlaneLocator.getAugmentation(SffDataPlaneLocator2.class).getOvsOptions().getLocalIp().getIpv4Address().getValue(), ipAddress);
    }

    @Test
    public void buildVxlanTunnelDataPlaneLocatorNameTest() throws Exception {
        ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathHopBuilderFrom = new RenderedServicePathHopBuilder();
        renderedServicePathHopBuilderTo = new RenderedServicePathHopBuilder();
        String result;

        renderedServicePathBuilder.setName(renderedServicePathName);
        renderedServicePathHopBuilderFrom.setHopNumber(hopNumberFrom);
        renderedServicePathHopBuilderTo.setHopNumber(hopNumberTo);
        hopOvsdbBridgePairFrom = new HopOvsdbBridgePair(renderedServicePathHopBuilderFrom.build(), ovsdbBridgeAugmentationBuilder.build());
        hopOvsdbBridgePairTo = new HopOvsdbBridgePair(renderedServicePathHopBuilderTo.build(), ovsdbBridgeAugmentationBuilder.build());

        result = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "buildVxlanTunnelDataPlaneLocatorName", renderedServicePathBuilder.build(), hopOvsdbBridgePairFrom, hopOvsdbBridgePairTo);

        //buildVxlanTunnelDataPlaneLocatorName test
        Assert.assertEquals(result, renderedServicePathName + "-vxlan-" + hopNumberFrom + "to" + hopNumberTo);
    }
}













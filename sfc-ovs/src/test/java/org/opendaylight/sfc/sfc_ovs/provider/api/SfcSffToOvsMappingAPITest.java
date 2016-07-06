/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcSffToOvsMappingAPI
 * @since 2015-04-22
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOvsUtil.class)
public class SfcSffToOvsMappingAPITest {

    private static final String OVSDB_OPTION_LOCAL_IP_VALUE = "172.0.0.0";
    private static final String OVSDB_OPTION_REMOTE_IP_VALUE = "172.0.0.1";
    private static final String OVSDB_OPTION_DST_PORT_VALUE = "8080";
    private static final String OVSDB_OPTION_KEY = "Key";
    private static final String OVSDB_OPTION_NSP = "Nsp";
    private static final String OVSDB_OPTION_NSI = "Nsi";
    private static final String OVSDB_OPTION_NSHC1 = "Nshc1";
    private static final String OVSDB_OPTION_NSHC2 = "Nshc2";
    private static final String OVSDB_OPTION_NSHC3 = "Nshc3";
    private static final String OVSDB_OPTION_NSHC4 = "Nshc4";
    private static final String testString = "testString";
    private static final String bridgeName = "bridge name";
    private static final String uuid = "00000000-0000-0000-0000-000000000000";
    private Class<? extends InterfaceTypeBase> interfaceTypeClass;
    private DataPlaneLocatorBuilder dataPlaneLocatorBuilder;
    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options> optionsList;
    private List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointAugmentationList;
    private OvsBridgeBuilder ovsBridgeBuilder;
    private OvsdbBridgeAugmentation ovsdbBridgeAugmentation;
    private OvsOptionsBuilder ovsOptionsBuilder;
    private ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder;
    private SffOvsNodeAugmentationBuilder sffOvsNodeAugmentationBuilder;
    private SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder;
    private SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder;
    private SffOvsLocatorOptionsAugmentationBuilder sffOvsLocatorOptionsAugmentationBuilder;
    private ExecutorService executor;
    private OvsdbNodeAugmentationBuilder ovsdbNodeAugmentationBuilder;
    private ConnectionInfoBuilder connectionInfoBuilder;

    @Before
    public void setup() throws Exception {
        executor = Executors.newScheduledThreadPool(1);
    }

    @Test
    public void testSfcSffToOvsMappingAPITest() {
        SfcSffToOvsMappingAPI sfcSffToOvsMappingAPI = new SfcSffToOvsMappingAPI();
        sfcSffToOvsMappingAPI.getClass();
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation_NullOvsBridgeAugmentation() throws Exception {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();

        ovsdbBridgeAugmentation =
                SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarderBuilder.build(), executor);

        assertNull("Must be null", ovsdbBridgeAugmentation);
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation_NullOvsBridge() throws Exception {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();

        sffOvsBridgeAugmentationBuilder.setOvsBridge(null);
        serviceFunctionForwarderBuilder.addAugmentation(SffOvsBridgeAugmentation.class,
                sffOvsBridgeAugmentationBuilder.build());

        ovsdbBridgeAugmentation =
                SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarderBuilder.build(), executor);

        assertNull("Must be null", ovsdbBridgeAugmentation);
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation_NullOvsNode() throws Exception {
        ovsBridgeBuilder = new OvsBridgeBuilder();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        sffOvsNodeAugmentationBuilder = new SffOvsNodeAugmentationBuilder();
        sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();

        ovsBridgeBuilder.setBridgeName(bridgeName).setUuid(new Uuid(uuid));
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());

        serviceFunctionForwarderBuilder.setName(new SffName(testString))
            .addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build())
            .addAugmentation(SffOvsNodeAugmentation.class, sffOvsNodeAugmentationBuilder.build());

        ovsdbBridgeAugmentation =
                SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarderBuilder.build(), executor);

        assertNull("Must be null", ovsdbBridgeAugmentation);
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation_nullNode() throws Exception {
        ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
        connectionInfoBuilder = new ConnectionInfoBuilder();

        // build ovsNodeAugmentation
        connectionInfoBuilder.setLocalIp(new IpAddress(new Ipv4Address(OVSDB_OPTION_LOCAL_IP_VALUE)));
        ovsdbNodeAugmentationBuilder.setConnectionInfo(connectionInfoBuilder.build());

        // build ovsBridge
        ovsBridgeBuilder.setBridgeName(bridgeName).setUuid(new Uuid(uuid));
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());

        serviceFunctionForwarderBuilder.setName(new SffName(testString)).addAugmentation(SffOvsBridgeAugmentation.class,
                sffOvsBridgeAugmentationBuilder.build());

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOvsdbNodeAugmentation"))
            .toReturn(ovsdbNodeAugmentationBuilder.build());

        ovsdbBridgeAugmentation =
                SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarderBuilder.build(), executor);

        assertNull("Must be null", ovsdbBridgeAugmentation);
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation1() throws Exception {
        ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
        connectionInfoBuilder = new ConnectionInfoBuilder();
        NodeBuilder nodeBuilder = new NodeBuilder();

        // build ovsNodeAugmentation
        connectionInfoBuilder.setLocalIp(new IpAddress(new Ipv4Address(OVSDB_OPTION_LOCAL_IP_VALUE)));
        ovsdbNodeAugmentationBuilder.setConnectionInfo(connectionInfoBuilder.build());

        // build ovsBridge
        ovsBridgeBuilder.setBridgeName(bridgeName).setUuid(new Uuid(uuid));
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());

        serviceFunctionForwarderBuilder.setName(new SffName(testString)).addAugmentation(SffOvsBridgeAugmentation.class,
                sffOvsBridgeAugmentationBuilder.build());

        // create node
        nodeBuilder.setNodeId(new NodeId("NodeId"));

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOvsdbNodeAugmentation"))
            .toReturn(ovsdbNodeAugmentationBuilder.build());
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "lookupTopologyNode")).toReturn(nodeBuilder.build());

        ovsdbBridgeAugmentation =
                SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarderBuilder.build(), executor);

        assertNotNull("Must not be null", ovsdbBridgeAugmentation);
        assertEquals("Must be equal", ovsdbBridgeAugmentation.getBridgeName().getValue(), bridgeName);
        assertEquals("Must be equal", ovsdbBridgeAugmentation.getBridgeUuid().getValue(), uuid);
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation2() throws Exception {
        ovsBridgeBuilder = new OvsBridgeBuilder();
        OvsNodeBuilder ovsNodeBuilder = new OvsNodeBuilder();
        ovsdbNodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        sffOvsNodeAugmentationBuilder = new SffOvsNodeAugmentationBuilder();
        sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
        connectionInfoBuilder = new ConnectionInfoBuilder();

        // build ovsNodeAugmentation
        connectionInfoBuilder.setLocalIp(new IpAddress(new Ipv4Address(OVSDB_OPTION_LOCAL_IP_VALUE)));
        ovsdbNodeAugmentationBuilder.setConnectionInfo(connectionInfoBuilder.build());

        // build ovsBridge
        ovsBridgeBuilder.setBridgeName(bridgeName).setUuid(new Uuid(uuid));
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());

        // build ovsNode
        ovsNodeBuilder.setNodeId(new OvsdbNodeRef(SfcOvsUtil.buildOvsdbNodeIID(testString)));
        sffOvsNodeAugmentationBuilder.setOvsNode(ovsNodeBuilder.build());

        serviceFunctionForwarderBuilder.setName(new SffName(testString))
            .addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build())
            .addAugmentation(SffOvsNodeAugmentation.class, sffOvsNodeAugmentationBuilder.build());

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOvsdbNodeAugmentation"))
            .toReturn(ovsdbNodeAugmentationBuilder.build());

        ovsdbBridgeAugmentation =
                SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(serviceFunctionForwarderBuilder.build(), executor);

        assertNotNull("Must not be null", ovsdbBridgeAugmentation);
        assertEquals("Must be equal", ovsdbBridgeAugmentation.getBridgeName().getValue(), bridgeName);
        assertEquals("Must be equal", ovsdbBridgeAugmentation.getBridgeUuid().getValue(), uuid);
        assertEquals("Must be equal", ovsdbBridgeAugmentation.getControllerEntry().get(0).getTarget().getValue(),
                "tcp:" + OVSDB_OPTION_LOCAL_IP_VALUE + ":6653");
    }

    @Test
    public void testBuildTerminationPointAugmentationList() throws Exception {
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        ovsOptionsBuilder = new OvsOptionsBuilder();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffOvsLocatorOptionsAugmentationBuilder = new SffOvsLocatorOptionsAugmentationBuilder();

        sffDataPlaneLocatorBuilder.setName(new SffDataPlaneLocatorName(testString));

        ovsOptionsBuilder.setLocalIp(OVSDB_OPTION_LOCAL_IP_VALUE)
            .setRemoteIp(OVSDB_OPTION_REMOTE_IP_VALUE)
            .setDstPort(OVSDB_OPTION_DST_PORT_VALUE)
            .setKey(OVSDB_OPTION_KEY)
            .setNsp(OVSDB_OPTION_NSP)
            .setNsi(OVSDB_OPTION_NSI);

        sffOvsLocatorOptionsAugmentationBuilder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder
            .addAugmentation(SffOvsLocatorOptionsAugmentation.class, sffOvsLocatorOptionsAugmentationBuilder.build())
            .setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        ovsdbTerminationPointAugmentationList =
                SfcSffToOvsMappingAPI.buildTerminationPointAugmentationList(sffDataPlaneLocatorList);

        assertEquals("Must be equal", ovsdbTerminationPointAugmentationList.get(0).getName(), testString);
    }

    @Test
    public void testGetSffDataPlaneLocatorOptions_nullSffDplOvsOptions() throws Exception {
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        // TODO remove reflection for "getSffDataPlaneLocatorOptions"
        optionsList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getSffDataPlaneLocatorOptions",
                sffDataPlaneLocatorBuilder.build());

        assertEquals("Must be equal", optionsList, Collections.emptyList());
    }

    @Test
    public void testGetSffDataPlaneLocatorOption_nullOptions() throws Exception {
        optionsList = new ArrayList<>();
        ovsOptionsBuilder = new OvsOptionsBuilder();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffOvsLocatorOptionsAugmentationBuilder = new SffOvsLocatorOptionsAugmentationBuilder();

        sffOvsLocatorOptionsAugmentationBuilder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder.addAugmentation(SffOvsLocatorOptionsAugmentation.class,
                sffOvsLocatorOptionsAugmentationBuilder.build());
        // TODO remove reflection for "getSffDataPlaneLocatorOptions"
        optionsList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getSffDataPlaneLocatorOptions",
                sffDataPlaneLocatorBuilder.build());

        Assert.assertEquals("Local Ip must be Equal", optionsList, Collections.emptyList());
    }

    @Test
    public void getSffDataPlaneLocatorOptionsTest() throws Exception {
        optionsList = new ArrayList<>();
        ovsOptionsBuilder = new OvsOptionsBuilder();
        ovsdbTerminationPointAugmentationList = new ArrayList<>();
        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffOvsLocatorOptionsAugmentationBuilder = new SffOvsLocatorOptionsAugmentationBuilder();

        ovsOptionsBuilder.setLocalIp(OVSDB_OPTION_LOCAL_IP_VALUE)
            .setRemoteIp(OVSDB_OPTION_REMOTE_IP_VALUE)
            .setDstPort(OVSDB_OPTION_DST_PORT_VALUE)
            .setKey(OVSDB_OPTION_KEY)
            .setNsp(OVSDB_OPTION_NSP)
            .setNsi(OVSDB_OPTION_NSI)
            .setNshc1(OVSDB_OPTION_NSHC1)
            .setNshc2(OVSDB_OPTION_NSHC2)
            .setNshc3(OVSDB_OPTION_NSHC3)
            .setNshc4(OVSDB_OPTION_NSHC4);

        sffOvsLocatorOptionsAugmentationBuilder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder.addAugmentation(SffOvsLocatorOptionsAugmentation.class,
                sffOvsLocatorOptionsAugmentationBuilder.build());

        // TODO remove reflection for "getSffDataPlaneLocatorOptions"
        optionsList = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getSffDataPlaneLocatorOptions",
                sffDataPlaneLocatorBuilder.build());

        // Test all options
        assertEquals("Local Ip must be Equal", optionsList.get(0).getValue(), OVSDB_OPTION_LOCAL_IP_VALUE);
        assertEquals("Remote Ip must be Equal", optionsList.get(1).getValue(), OVSDB_OPTION_REMOTE_IP_VALUE);
        assertEquals("Dst must be Equal", optionsList.get(2).getValue(), OVSDB_OPTION_DST_PORT_VALUE);
        assertEquals("Key must be Equal", optionsList.get(3).getValue(), OVSDB_OPTION_KEY);
        assertEquals("Nsp must be Equal", optionsList.get(4).getValue(), OVSDB_OPTION_NSP);
        assertEquals("Nsi must be Equal", optionsList.get(5).getValue(), OVSDB_OPTION_NSI);
        assertEquals("Nshc1 must be Equal", optionsList.get(6).getValue(), OVSDB_OPTION_NSHC1);
        assertEquals("Nshc2 must be Equal", optionsList.get(7).getValue(), OVSDB_OPTION_NSHC2);
        assertEquals("Nshc3 must be Equal", optionsList.get(8).getValue(), OVSDB_OPTION_NSHC3);
        assertEquals("Nshc4 must be Equal", optionsList.get(9).getValue(), OVSDB_OPTION_NSHC4);
    }

    @Test
    public void testGetDataPlaneLocatorInterfaceType_NoTransport() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        // TODO remove reflection for "getDataPlaneLocatorInterfaceType"
        interfaceTypeClass = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorInterfaceType",
                dataPlaneLocatorBuilder.build());

        assertEquals("Must be Equal", interfaceTypeClass, InterfaceTypeInternal.class);
    }

    @Test
    public void testGetDataPlaneLocatorInterfaceType_Other() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        dataPlaneLocatorBuilder.setTransport(Other.class);

        // TODO remove reflection for "getDataPlaneLocatorInterfaceType"
        interfaceTypeClass = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorInterfaceType",
                dataPlaneLocatorBuilder.build());

        assertEquals("Must be Equal", interfaceTypeClass, InterfaceTypeInternal.class);
    }

    @Test
    public void testGetDataPlaneLocatorInterfaceType_VxLan() throws Exception {
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);

        //TODO remove reflection for "getDataPlaneLocatorInterfaceType"
        interfaceTypeClass = Whitebox.invokeMethod(SfcSffToOvsMappingAPI.class, "getDataPlaneLocatorInterfaceType",
                dataPlaneLocatorBuilder.build());

        assertEquals("Must be Equal", interfaceTypeClass, InterfaceTypeVxlan.class);
    }
}

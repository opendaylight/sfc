/**
 * Copyright (c) 2017, 2018 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.ovs.listener.SfcOvsSffEntryDataListener;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test suite for OVS SFF data listeners.
 *
 * @author ebrjohn
 *
 */
public class SfcOvsSffEntryDataListenerTest extends AbstractDataStoreManager {

    private static final IpAddress IP_ADDRESS = new IpAddress(new Ipv4Address("10.1.1.101"));
    private static final PortNumber PORT_NUMBER = new PortNumber(6633);

    // Class under test
    private SfcOvsSffEntryDataListener sfcOvsSffEntryDataListener;

    @Before
    public void before() {
        setupSfc();
        sfcOvsSffEntryDataListener = new SfcOvsSffEntryDataListener(getDataBroker());
    }

    @After
    public void after() throws Exception {
        sfcOvsSffEntryDataListener.close();
        close();
    }

    @Test
    public void testAddSffNoDpl() {
        final ServiceFunctionForwarder sff = buildServiceFunctionForwarderNoDPL();
        createOvsdbNodeForSff(IP_ADDRESS, PORT_NUMBER);

        sfcOvsSffEntryDataListener.add(InstanceIdentifier.create(ServiceFunctionForwarder.class), sff);

        NodeId ovsdbBridgeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(sff);
        // The DPL is used to lookup the topology node
        assertNull(ovsdbBridgeId);
        // No terminationPoints should be created since the DPL is null
        assertNull(getSffTerminationPoint(ovsdbBridgeId, sff));
    }

    @Test
    public void testAddSffWithDpl() {
        ServiceFunctionForwarder sff = buildServiceFunctionForwarderWithDpl();
        // For this test, If there's a DPL, there will only ever be just 1
        // SffDpl
        IpPortLocator ipLocator = (IpPortLocator) sff.getSffDataPlaneLocator().get(0).getDataPlaneLocator()
                .getLocatorType();
        createOvsdbNodeForSff(ipLocator.getIp(), ipLocator.getPort());

        sfcOvsSffEntryDataListener.add(InstanceIdentifier.create(ServiceFunctionForwarder.class), sff);

        NodeId ovsdbBridgeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(sff);
        assertNotNull(ovsdbBridgeId);
        assertNotNull(getSffTerminationPoint(ovsdbBridgeId, sff));
    }

    @Test
    public void testRemoveSff() {
        // First we need to create the ovsdbBridge and termination point
        // Then remove the SFF and test the ovsdbBridge and termination point
        // get deleted

        //
        // Add the SFF
        //
        ServiceFunctionForwarder sff = buildServiceFunctionForwarderWithDpl();
        // For this test, If there's a DPL, there will only ever be just 1
        // SffDpl
        IpPortLocator ipLocator = (IpPortLocator) sff.getSffDataPlaneLocator().get(0).getDataPlaneLocator()
                .getLocatorType();
        createOvsdbNodeForSff(ipLocator.getIp(), ipLocator.getPort());

        sfcOvsSffEntryDataListener.add(InstanceIdentifier.create(ServiceFunctionForwarder.class), sff);

        NodeId ovsdbBridgeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(sff);
        assertNotNull(ovsdbBridgeId);
        assertNotNull(getSffTerminationPoint(ovsdbBridgeId, sff));

        //
        // Now delete the SFF
        //
        sfcOvsSffEntryDataListener.remove(InstanceIdentifier.create(ServiceFunctionForwarder.class), sff);

        ovsdbBridgeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(sff);
        assertNull(getSffTerminationPoint(ovsdbBridgeId, sff));
    }

    @Test
    public void testUpdateSff() {
        // First add an SFF with no DPL and check nothing special is created
        // Then update the SFF and check that the ovsdbBridge and termination
        // point get created

        //
        // Create an SFF with no DPL
        //
        final ServiceFunctionForwarder originalSff = buildServiceFunctionForwarderNoDPL();
        createOvsdbNodeForSff(IP_ADDRESS, PORT_NUMBER);

        sfcOvsSffEntryDataListener.add(InstanceIdentifier.create(ServiceFunctionForwarder.class), originalSff);

        NodeId ovsdbBridgeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(originalSff);
        assertNull(ovsdbBridgeId);
        assertNull(getSffTerminationPoint(ovsdbBridgeId, originalSff));

        //
        // Now update the SFF, adding a DPL
        //
        ServiceFunctionForwarder updatedSff = buildServiceFunctionForwarderWithDpl();
        // For this test, If there's a DPL, there will only ever be just 1
        // SffDpl
        IpPortLocator ipLocator = (IpPortLocator) updatedSff.getSffDataPlaneLocator().get(0).getDataPlaneLocator()
                .getLocatorType();
        createOvsdbNodeForSff(ipLocator.getIp(), ipLocator.getPort());

        sfcOvsSffEntryDataListener
                .update(InstanceIdentifier.create(ServiceFunctionForwarder.class), originalSff, updatedSff);

        ovsdbBridgeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(updatedSff);
        assertNotNull(ovsdbBridgeId);
        assertNotNull(getSffTerminationPoint(ovsdbBridgeId, updatedSff));
    }

    private void createOvsdbNodeForSff(IpAddress remoteIp, PortNumber remotePort) {
        ConnectionInfoBuilder connInfoBuilder = new ConnectionInfoBuilder();
        connInfoBuilder.setRemoteIp(remoteIp);
        connInfoBuilder.setRemotePort(remotePort);

        OvsdbNodeAugmentationBuilder ovsdbNodeAugBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeAugBuilder.setConnectionInfo(connInfoBuilder.build());

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId("testNode"));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getNodeId()));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugBuilder.build());

        InstanceIdentifier<Node> ovsdbTopologyIID = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, nodeBuilder.getKey());
        SfcDataStoreAPI.writePutTransactionAPI(ovsdbTopologyIID, nodeBuilder.build(), LogicalDatastoreType.OPERATIONAL);
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarderNoDPL() {
        ServiceFunctionForwarderBuilder sffBuilder = buildSffCommon();

        // Notice: putting the sff now will cause the listener to execute,
        // before all test setup is completed
        // assertTrue(SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff));

        return sffBuilder.build();
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarderWithDpl() {
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();
        ovsOptionsBuilder.setExts("gpe");
        ovsOptionsBuilder.setKey("flow");
        ovsOptionsBuilder.setDstPort("6633");
        ovsOptionsBuilder.setRemoteIp("flow");
        SffOvsLocatorOptionsAugmentationBuilder sffOvsLocatorOptionsBuilder =
                new SffOvsLocatorOptionsAugmentationBuilder();
        sffOvsLocatorOptionsBuilder.setOvsOptions(ovsOptionsBuilder.build());

        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(IP_ADDRESS).setPort(PORT_NUMBER);

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        locatorBuilder.setName(new SffDataPlaneLocatorName("locator-1"))
                .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName("locator-1")))
                .setDataPlaneLocator(sffLocatorBuilder.build())
                .addAugmentation(SffOvsLocatorOptionsAugmentation.class, sffOvsLocatorOptionsBuilder.build());

        List<SffDataPlaneLocator> locatorList = new ArrayList<>();
        locatorList.add(locatorBuilder.build());

        ServiceFunctionForwarderBuilder sffBuilder = buildSffCommon();
        sffBuilder.setSffDataPlaneLocator(locatorList);

        // Notice: putting the sff now will cause the listener to execute,
        // before all test setup is completed
        // assertTrue(SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff));

        return sffBuilder.build();
    }

    // Internal method called by either build_service_function_forwarder_noDpl()
    // or buildServiceFunctionForwarderWithDpl()
    private ServiceFunctionForwarderBuilder buildSffCommon() {
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName("br-int");
        ovsBridgeBuilder.setUuid(new Uuid("12345678-1234-1234-1234-123456789012"));
        SffOvsBridgeAugmentationBuilder sffOvsBridgeAugBuilder = new SffOvsBridgeAugmentationBuilder();
        sffOvsBridgeAugBuilder.setOvsBridge(ovsBridgeBuilder.build());

        SffName name = new SffName("SFF1");
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setName(name);
        sffBuilder.setKey(new ServiceFunctionForwarderKey(name));
        sffBuilder.setServiceNode(null);
        sffBuilder.addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugBuilder.build());

        return sffBuilder;
    }

    private OvsdbTerminationPointAugmentation getSffTerminationPoint(NodeId ovsdbBridgeId,
            ServiceFunctionForwarder sff) {
        if (sff.getSffDataPlaneLocator() == null) {
            return null;
        }

        InstanceIdentifier<TerminationPoint> termPointIID = SfcOvsUtil.buildOvsdbTerminationPointIID(ovsdbBridgeId,
                sff.getSffDataPlaneLocator().get(0).getName().getValue());
        TerminationPoint termPoint = SfcDataStoreAPI.readTransactionAPI(termPointIID,
                LogicalDatastoreType.CONFIGURATION);

        if (termPoint == null) {
            return null;
        }

        return termPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
    }
}

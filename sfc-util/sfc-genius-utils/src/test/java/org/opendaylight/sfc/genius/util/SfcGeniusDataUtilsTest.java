/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit Testing for the SfcGeniusDataUtils class.
 *
 * @author Miguel Duarte (miguel.duarte.de.mora.barroso@ericsson.com)
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcGeniusUtilsDataGetter.class)
public class SfcGeniusDataUtilsTest {

    private final String ifName = "40c552e0-3695-472d-bace-7618786aba27";
    private final String macAddress = "b2:0e:19:ad:1e:22";
    private final BigInteger theDpnId = new BigInteger("79268612506848");
    private final String logicalIfName = "tap40c552e0-36";

    public SfcGeniusDataUtilsTest() {
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SfcGeniusUtilsDataGetter.class);
        PowerMockito.when(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceName(anyString()))
                .thenReturn(Optional.of(logicalIfName));

        PowerMockito.when(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(anyString()))
                .thenReturn(Optional.of(new InterfaceBuilder().withKey(new InterfaceKey(logicalIfName))
                        .setPhysAddress(new PhysAddress("11:22:33:44:55:66")).setLowerLayerIf(new ArrayList<String>() {
                            {
                                add("openflow:79268612506848:1");
                            }
                        }).setType(L2vlan.class).build()));

        OvsdbTerminationPointAugmentationBuilder ovsdbTpAug = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTpAug.setPortUuid(new Uuid("451f440a-a828-41ad-993c-93aaec43eb31"));
        ovsdbTpAug.setOfport((long) 1);
        List<InterfaceExternalIds> theInterfaces = new ArrayList<InterfaceExternalIds>() {
            {
                add(new InterfaceExternalIdsBuilder().setExternalIdKey("attached-mac").setExternalIdValue(macAddress)
                        .build());
            }
        };
        ovsdbTpAug.setInterfaceExternalIds(theInterfaces);

        TerminationPoint theTerminationPoint = new TerminationPointBuilder()
                .withKey(new TerminationPointKey(new TerminationPointKey(new TpId(logicalIfName))))
                .addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbTpAug.build()).build();

        String ovsdbNodeId = "ovsdb://uuid/e94d439a-57cf-4b8f-9247-56cdd692fa33/bridge/br-int";
        Node theOvsNode = new NodeBuilder().setNodeId(new NodeId(ovsdbNodeId))
                .setTerminationPoint(new ArrayList<TerminationPoint>() {
                    {
                        add(theTerminationPoint);
                    }
                }).build();

        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIID = InstanceIdentifier.create(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network
                .topology.rev131021.NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SfcGeniusConstants.OVSDB_TOPOLOGY_ID))
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network
                        .topology.rev131021.network.topology.topology.Node.class,
                        theOvsNode.key())
                .augmentation(OvsdbBridgeAugmentation.class);

        BridgeRefEntry theBridge = new BridgeRefEntryBuilder().withKey(new BridgeRefEntryKey(theDpnId))
                .setBridgeReference(new OvsdbBridgeRef(bridgeIID)).build();

        PowerMockito.when(SfcGeniusUtilsDataGetter.readOvsNodeInterfaces(anyString(), anyString()))
                .thenReturn(theInterfaces);
        PowerMockito.when(SfcGeniusUtilsDataGetter.getBridgeFromDpnId(any(BigInteger.class)))
                .thenReturn(Optional.of(theBridge));
    }

    /**
     * Negative test when the interface does not exist in the CONFIG DS.
     *
     */
    @Test(expected = RuntimeException.class)
    public void readMacAddressInterfaceDoesNotExist() {
        PowerMockito.when(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceName(ifName)).thenReturn(null);
        SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
    }

    /**
     * Positive test.
     *
     */
    @Test
    public void readRemoteMacAddress() {
        Optional<MacAddress> mac = SfcGeniusDataUtils.getServiceFunctionForwarderPortMacAddress(ifName);
        Assert.assertTrue(mac.isPresent());
    }

    /**
     * Negative test when the interface does not exist in the CONFIG DS.
     *
     */
    @Test(expected = RuntimeException.class)
    public void readRemoteMacAddressInterfaceDoesNotExist() {
        PowerMockito.when(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName)).thenReturn(null);
        SfcGeniusDataUtils.getServiceFunctionForwarderPortMacAddress(ifName);
    }

    /**
     * Negative test when the interface exists, but does not have a physical
     * address.
     */
    @Test(expected = RuntimeException.class)
    public void readRemoteMacAddressPhysicalAddressDoesNotExists() {
        PowerMockito.when(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName))
                .thenReturn(Optional.of(new InterfaceBuilder().withKey(new InterfaceKey(logicalIfName))
                        .setPhysAddress(null).setLowerLayerIf(new ArrayList<String>() {
                            {
                                add("openflow:79268612506848:1");
                            }
                        }).setType(L2vlan.class).build()));
        SfcGeniusDataUtils.getServiceFunctionForwarderPortMacAddress(ifName);
    }

    /**
     * Negative test when the interface features in the CONFIG DS but does not
     * feature in the OPERATIONAL DS.
     *
     */
    @Test(expected = RuntimeException.class)
    public void readMacAddressInterfaceNotProperlyConfigured() {
        PowerMockito.when(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName)).thenReturn(null);
        SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
    }

    /**
     * Negative test when the interface features in the CONFIG DS, features in
     * the OPERATIONAL DS, but we cannot retrieve a BridgeRefEntry from the
     * model.
     *
     */
    @Test(expected = RuntimeException.class)
    public void readMacAddressInterfaceWrongBridge() {
        PowerMockito.when(SfcGeniusUtilsDataGetter.getBridgeFromDpnId(theDpnId)).thenReturn(null);
        SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
    }

    /**
     * Negative test when the interface features in the CONFIG DS, features in
     * the OPERATIONAL DS, we can get a BridgeRefEntry, but the bridge does not
     * belong to an OVS switch.
     *
     */
    @Test(expected = RuntimeException.class)
    public void readMacAddressNotOvsBridge() {
        PowerMockito.when(SfcGeniusUtilsDataGetter.getBridgeFromDpnId(theDpnId))
                .thenReturn(Optional.of(new BridgeRefEntryBuilder().withKey(new BridgeRefEntryKey(theDpnId)).build()));
        SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
    }

    /**
     * Negative test when the interface features in the CONFIG DS, features in
     * the OPERATIONAL DS, we can get a BridgeRefEntry, the bridge belongs to an
     * OVS switch, but there's no TerminationPoint.
     *
     */
    @Test(expected = org.opendaylight.sfc.genius.util.SfcGeniusRuntimeException.class)
    public void readMacAddressInvalidDpnId() {
        PowerMockito.when(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName))
                .thenReturn(Optional.of(new InterfaceBuilder().withKey(new InterfaceKey(logicalIfName))
                        .setPhysAddress(new PhysAddress("52:c1:91:54:fc:7a")).setLowerLayerIf(new ArrayList<String>() {
                            {
                                add("qua-qua");
                            }
                        }).setType(L2vlan.class).build()));
        SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
    }

    /**
     * Negative test when the interface features in the CONFIG DS, features in
     * the OPERATIONAL DS, we can get a BridgeRefEntry, the bridge belongs to an
     * OVS switch, there's a TerminationPoint, *BUT* it does not have an
     * attached MAC.
     *
     */
    @Test
    public void readMacAddressInvalidTerminationPoint() {
        // this InterfaceExternalIds list does not have the
        // 'attached-mac-address' key
        List<InterfaceExternalIds> theInterfaces = new ArrayList<InterfaceExternalIds>() {
            {
                add(new InterfaceExternalIdsBuilder().build());
            }
        };
        PowerMockito.when(SfcGeniusUtilsDataGetter.readOvsNodeInterfaces(anyString(), anyString()))
                .thenReturn(theInterfaces);

        Optional<MacAddress> theMac = SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
        Assert.assertFalse(theMac.isPresent());
    }

    /**
     * Positive test, where we can get the MAC Address of a SF attached to a
     * SFF.
     *
     */
    @Test
    public void readMacAddress() {
        Optional<MacAddress> theMac = SfcGeniusDataUtils.getServiceFunctionMacAddress(ifName);
        Assert.assertTrue(theMac.isPresent());
        Assert.assertEquals(new MacAddress(macAddress), theMac.get());
    }

    /**
     * Test that there are no logical interfaces if there is no DPL set.
     */
    public void getSfLogicalInterfacesNoDpl() {
        SfName sf1Name = new SfName("dpi");
        String dpiIpAddress = "192.168.1.30";
        ServiceFunctionKey dpiKey = new ServiceFunctionKey(sf1Name);
        List<SfDataPlaneLocator> dpLocators = new ArrayList<SfDataPlaneLocator>() {
            {
                add(new SfDataPlaneLocatorBuilder()
                        .withKey(new SfDataPlaneLocatorKey(new SfDataPlaneLocatorName("dpi-1-dpl")))
                        .setServiceFunctionForwarder(new SffName("sfflogical1")).build());
            }
        };

        ServiceFunction dpiNode = new ServiceFunctionBuilder().withKey(dpiKey)
                .setIpMgmtAddress(IpAddressBuilder.getDefaultInstance(dpiIpAddress))
                .setRestUri(new Uri(dpiIpAddress.concat(":5000"))).setType(new SftTypeName("dpi"))
                .setSfDataPlaneLocator(dpLocators).build();

        List<String> sfLogicalInterfaces = SfcGeniusDataUtils.getSfLogicalInterfaces(dpiNode);
        Assert.assertTrue("There are no logical interfaces", sfLogicalInterfaces.isEmpty());
    }

    /**
     * Test that there are no logical interfaces if the DPL is not a of
     * logical interface type.
     */
    public void getSfLogicalInterfacesWithDifferentLocatorType() {
        SfName sf1Name = new SfName("dpi");
        String dpiIpAddress = "192.168.1.30";
        ServiceFunctionKey dpiKey = new ServiceFunctionKey(sf1Name);
        List<SfDataPlaneLocator> dpLocators = new ArrayList<SfDataPlaneLocator>() {
            {
                add(new SfDataPlaneLocatorBuilder()
                        .withKey(new SfDataPlaneLocatorKey(new SfDataPlaneLocatorName("dpi-1-dpl")))
                        .setServiceFunctionForwarder(new SffName("sfflogical1")).setLocatorType(new IpBuilder()
                                .setIp(IpAddressBuilder.getDefaultInstance(dpiIpAddress)).setPort(new PortNumber(8181))
                                .build())
                        .build());
            }
        };

        ServiceFunction dpiNode = new ServiceFunctionBuilder().withKey(dpiKey)
                .setIpMgmtAddress(IpAddressBuilder.getDefaultInstance(dpiIpAddress))
                .setRestUri(new Uri(dpiIpAddress.concat(":5000"))).setType(new SftTypeName("dpi"))
                .setSfDataPlaneLocator(dpLocators).build();

        List<String> sfLogicalInterfaces = SfcGeniusDataUtils.getSfLogicalInterfaces(dpiNode);
        Assert.assertTrue("There are no logical interfaces", sfLogicalInterfaces.isEmpty());
    }

    /**
     * Positive test, where we can get the names of the LogicalInterfaces to which
     * the SF is connected to.
     */
    @Test
    public void getSfLogicalInterfaces() {
        SfName sf1Name = new SfName("dpi");
        String dpiIpAddress = "192.168.1.30";
        ServiceFunctionKey dpiKey = new ServiceFunctionKey(sf1Name);
        List<SfDataPlaneLocator> dpLocators = new ArrayList<SfDataPlaneLocator>() {
            {
                add(new SfDataPlaneLocatorBuilder()
                        .withKey(new SfDataPlaneLocatorKey(new SfDataPlaneLocatorName("dpi-1-dpl")))
                        .setServiceFunctionForwarder(new SffName("sfflogical1"))
                        .setLocatorType(new LogicalInterfaceBuilder()
                                .setInterfaceName("40c552e0-3695-472d-bace-7618786aba27").build())
                        .build());
                add(new SfDataPlaneLocatorBuilder()
                        .withKey(new SfDataPlaneLocatorKey(new SfDataPlaneLocatorName("dpi-2-dpl")))
                        .setServiceFunctionForwarder(new SffName("sfflogical1"))
                        .setLocatorType(new LogicalInterfaceBuilder()
                                .setInterfaceName("12345678-3695-472d-bace-7618786aba27").build())
                        .build());
            }
        };

        ServiceFunction dpiNode = new ServiceFunctionBuilder().withKey(dpiKey)
                .setIpMgmtAddress(IpAddressBuilder.getDefaultInstance(dpiIpAddress))
                .setRestUri(new Uri(dpiIpAddress.concat(":5000"))).setType(new SftTypeName("dpi"))
                .setSfDataPlaneLocator(dpLocators).build();

        Assert.assertEquals(
                Arrays.asList("40c552e0-3695-472d-bace-7618786aba27", "12345678-3695-472d-bace-7618786aba27"),
                SfcGeniusDataUtils.getSfLogicalInterfaces(dpiNode));
    }


    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void getDpnIdFromLowerLayerIfListTooManyItems() throws Exception {
        try {
            SfcGeniusDataUtils.getDpnIdFromLowerLayerIfList(Arrays.asList("Item1", "Item2"));
        } catch (Exception e) {
            assertThat(e, is(Matchers.instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause(), is(Matchers.instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void getDpnIdFromLowerLayerIfListBadItem() throws Exception {
        try {
            SfcGeniusDataUtils.getDpnIdFromLowerLayerIfList(Collections.singletonList(""));
        } catch (Exception e) {
            assertThat(e, is(Matchers.instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause(), is(Matchers.instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void getDpnIdFromNullLowerLayerIfList() throws Exception {
        try {
            SfcGeniusDataUtils.getDpnIdFromLowerLayerIfList(null);
        } catch (Exception e) {
            assertThat(e, is(Matchers.instanceOf(SfcGeniusRuntimeException.class)));
            assertThat(e.getCause(), is(Matchers.instanceOf(IllegalArgumentException.class)));
        }
    }
}

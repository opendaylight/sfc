/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * SfcOvsUtil class contains various wrapper and utility methods
 * <p>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-04-01
 */

package org.opendaylight.sfc.sfc_ovs.provider;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcOvsUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsUtil.class);
    private static final String OVSDB_BRIDGE_PREFIX = "/bridge/";
    public static final String OVSDB_OPTION_LOCAL_IP = "local_ip";
    public static final String OVSDB_OPTION_REMOTE_IP = "remote_ip";
    public static final String OVSDB_OPTION_DST_PORT = "dst_port";
    public static final String OVSDB_OPTION_NSP = "nsp";
    public static final String OVSDB_OPTION_NSI = "nsi";
    public static final String OVSDB_OPTION_IN_NSP = "in_nsp";
    public static final String OVSDB_OPTION_IN_NSI = "in_nsi";
    public static final String OVSDB_OPTION_OUT_NSP = "out_nsp";
    public static final String OVSDB_OPTION_OUT_NSI = "out_nsi";
    public static final String OVSDB_OPTION_NSHC1 = "nshc1";
    public static final String OVSDB_OPTION_NSHC2 = "nshc2";
    public static final String OVSDB_OPTION_NSHC3 = "nshc3";
    public static final String OVSDB_OPTION_NSHC4 = "nshc4";
    public static final String OVSDB_OPTION_KEY = "key";
    public static final String OVSDB_OPTION_EXTS = "exts";
    public static final String OVSDB_OPTION_GPE = "gpe";
    public static final String OVSDB_OPTION_VALUE_FLOW = "flow";
    public static final String DPL_NAME_DPDK = "Dpdk";
    public static final String DPL_NAME_DPDKVHOST = "Dpdkvhost";
    public static final String DPL_NAME_DPDKVHOSTUSER = "Dpdkvhostuser";
    public static final String DPL_NAME_INTERNAL = "Internal";
    public static final PortNumber NSH_VXLAN_TUNNEL_PORT = new PortNumber(6633);

    /**
     * Submits callable for execution by given ExecutorService.
     * Thanks to this wrapper method, boolean result will be returned instead of Future.
     * <p>
     *
     * @param callable Callable
     * @param executor ExecutorService
     * @return true if callable completed successfully, otherwise false.
     */
    public static Object submitCallable(Callable callable, ExecutorService executor) {
        Future future = null;
        Object result = null;

        future = executor.submit(callable);

        try {
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("{} failed to: {}", callable.toString(), e);
        }

        return result;
    }

    /**
     * Method builds OVSDB Topology InstanceIdentifier
     * <p>
     *
     * @return InstanceIdentifier&lt;Topology&gt;
     */
    public static InstanceIdentifier<Topology> buildOvsdbTopologyIID() {
        InstanceIdentifier<Topology> ovsdbTopologyIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        return ovsdbTopologyIID;
    }

    /**
     * Method builds OVS NodeId which is based on:
     * 1. OVS Node InstanceIdentifier which manages the OVS Bridge
     * 2. OVS Bridge name
     * <p>
     * If the two aforementioned fields are missing, NullPointerException is raised.
     * <p>
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return NodeId
     */
    private static NodeId getManagedByNodeId(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot getManagedByNodeId, OvsdbBridgeAugmentation is null.");

        Preconditions.checkNotNull(ovsdbBridge.getBridgeName(), "Cannot build getManagedByNodeId, BridgeName is null.");
        Preconditions.checkNotNull(ovsdbBridge.getManagedBy(), "Cannot build getManagedByNodeId, ManagedBy is null.");
        String bridgeName = (ovsdbBridge.getBridgeName().getValue());
        InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) ovsdbBridge.getManagedBy().getValue();

        KeyedInstanceIdentifier keyedInstanceIdentifier =
                (KeyedInstanceIdentifier) nodeIID.firstIdentifierOf(Node.class);
        Preconditions.checkNotNull(keyedInstanceIdentifier,
                "Cannot build getManagedByNodeId, parent OVS Node is null.");

        NodeKey nodeKey = (NodeKey) keyedInstanceIdentifier.getKey();
        String nodeId = nodeKey.getNodeId().getValue();
        nodeId = nodeId.concat(OVSDB_BRIDGE_PREFIX + bridgeName);

        return new NodeId(nodeId);
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on OVS NodeId
     * <p>
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return InstanceIdentifier&lt;Node&gt;
     * @see SfcOvsUtil getManagedByNodeId
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(OvsdbBridgeAugmentation ovsdbBridge) {
        InstanceIdentifier<Node> nodeIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(getManagedByNodeId(ovsdbBridge)));

        return nodeIID;
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on Service Function Forwarder name.
     * Method will return valid InstanceIdentifier only if the given SFF name belongs to SFF
     * instance mapped to OVS.
     * <p>
     *
     * @param serviceFunctionForwarderName String
     * @return InstanceIdentifier&lt;Node&gt;
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(String serviceFunctionForwarderName) {
        InstanceIdentifier<Node> nodeIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId(serviceFunctionForwarderName)));

        return nodeIID;
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on NodeId.
     * <p>
     *
     * @param nodeId NodeId
     * @return InstanceIdentifier&lt;Node&gt;
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(NodeId nodeId) {
        InstanceIdentifier<Node> nodeIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(nodeId));

        return nodeIID;
    }

    /**
     * Method builds OVS BridgeAugmentation InstanceIdentifier which is based on OVS NodeId
     * <p>
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return InstanceIdentifier&lt;OvsdbBridgeAugmentation&gt;
     * @see SfcOvsUtil getManagedByNodeId
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(OvsdbBridgeAugmentation ovsdbBridge) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(getManagedByNodeId(ovsdbBridge)))
            .augmentation(OvsdbBridgeAugmentation.class);

        return bridgeEntryIID;
    }

    /**
     * Create a {@link InstanceIdentifier} {@link OvsdbBridgeAugmentation} based on the Topology
     * {@link NodeId}
     *
     * @param nodeId A topology {@link NodeId}
     * @return InstanceIdentifier&lt;OvsdbBridgeAugmentation&gt;
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(NodeId nodeId) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(nodeId))
            .augmentation(OvsdbBridgeAugmentation.class);

        return bridgeEntryIID;
    }

    /**
     * Method builds OVS BridgeAugmentation InstanceIdentifier which is based on OVS Bridge name
     * <p>
     *
     * @param serviceFunctionForwarderName serviceFunctionForwarderName String
     * @return InstanceIdentifier&lt;OvsdbBridgeAugmentation&gt;
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(String serviceFunctionForwarderName) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID =
                buildOvsdbNodeIID(serviceFunctionForwarderName).augmentation(OvsdbBridgeAugmentation.class);

        return bridgeEntryIID;
    }

    /**
     * Method builds OVS TerminationPointAugmentation InstanceIdentifier which is based on:
     * 1. OVS Node InstanceIdentifier which manages the OVS Bridge, to which the OVS
     * TerminationPoint is attached
     * 2. OVS Termination Point name
     * <p>
     * If the two aforementioned fields are missing, NullPointerException is raised.
     * <p>
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @param ovsdbTerminationPoint OvsdbTerminationPointAugmentation
     * @return InstanceIdentifier&lt;OvsdbTerminationPointAugmentation&gt;
     */
    public static InstanceIdentifier<OvsdbTerminationPointAugmentation> buildOvsdbTerminationPointAugmentationIID(
            OvsdbBridgeAugmentation ovsdbBridge, OvsdbTerminationPointAugmentation ovsdbTerminationPoint) {

        Preconditions.checkNotNull(ovsdbTerminationPoint,
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier, OvsdbTerminationPointAugmentation is null.");
        Preconditions.checkNotNull(ovsdbTerminationPoint.getName(),
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier, OvsdbTerminationPointAugmentation Name is null.");
        Preconditions.checkNotNull(ovsdbBridge,
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier, OvsdbBridgeAugmentation is null.");

        NodeId nodeId = getManagedByNodeId(ovsdbBridge);
        String terminationPointId = ovsdbTerminationPoint.getName();

        InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointIID =
                InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class, new NodeKey(nodeId))
                    .child(TerminationPoint.class, new TerminationPointKey(new TpId(terminationPointId)))
                    .augmentation(OvsdbTerminationPointAugmentation.class);

        return terminationPointIID;
    }

    /**
     * Method builds OVS TerminationPoint InstanceIdentifier which is based on SFF name and SFF
     * DataPlane locator name.
     * Method will return valid InstanceIdentifier only if the given SFF and SFF DataPlane locator
     * belongs to SFF instance mapped to OVS.
     * <p>
     *
     * @param sffName Service Function Forwarder Name
     * @param sffDataPlaneLocatorName Service Function Forwarder Data Plane locator name
     * @return InstanceIdentifier&lt;TerminationPoint&gt;
     */
    public static InstanceIdentifier<TerminationPoint> buildOvsdbTerminationPointIID(String sffName,
            String sffDataPlaneLocatorName) {
        InstanceIdentifier<TerminationPoint> terminationPointIID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId(sffName)))
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(sffDataPlaneLocatorName)));

        return terminationPointIID;
    }

    public static IpAddress convertStringToIpAddress(String ipAddressString) {
        Preconditions.checkNotNull(ipAddressString, "Supplied string value of ipAddress must not be null");

        try {
            return new IpAddress(new Ipv4Address(ipAddressString));
        } catch (Exception e) {
            LOG.debug("Supplied string value of ipAddress ({}) is not an instance of IPv4", ipAddressString);
        }

        try {
            return new IpAddress(new Ipv6Address(ipAddressString));
        } catch (Exception e) {
            LOG.debug("Supplied string value of ipAddress ({}) is not an instance of IPv6", ipAddressString);
        }

        LOG.error("Supplied string value of ipAddress ({}) cannot be converted to IpAddress object!", ipAddressString);
        return null;
    }

    public static String convertIpAddressToString(IpAddress ipAddress) {
        Preconditions.checkNotNull(ipAddress, "Supplied IpAddress value must not be null");

        try {
            Preconditions.checkArgument(ipAddress.getIpv4Address().getValue() != null);
            return ipAddress.getIpv4Address().getValue();
        } catch (Exception e) {
            LOG.debug("Supplied IpAddress value ({}) is not an instance of IPv4", ipAddress.toString());
        }

        Preconditions.checkArgument(ipAddress.getIpv6Address().getValue() != null);
        return ipAddress.getIpv6Address().getValue();
    }

    public static boolean putOvsdbTerminationPoints(OvsdbBridgeAugmentation ovsdbBridge,
                                                    List<SffDataPlaneLocator> sffDataPlaneLocatorList, ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        boolean result = true;
        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList =
                SfcSffToOvsMappingAPI.buildTerminationPointAugmentationList(sffDataPlaneLocatorList);

        for (OvsdbTerminationPointAugmentation ovsdbTerminationPoint : ovsdbTerminationPointList) {
            Object[] methodParameters = {ovsdbBridge, ovsdbTerminationPoint};
            SfcOvsDataStoreAPI sfcOvsDataStoreAPIPutTerminationPoint =
                    new SfcOvsDataStoreAPI(SfcOvsDataStoreAPI.Method.PUT_OVSDB_TERMINATION_POINT, methodParameters);
            boolean partialResult =
                    (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIPutTerminationPoint, executor);

            // once result is false, we will keep it false (it will be not overwritten with next
            // partialResults)
            if (result) {
                result = partialResult;
            }
        }

        return result;
    }

    public static boolean putOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge, ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        Object[] methodParameters = {ovsdbBridge};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIPutBridge =
                new SfcOvsDataStoreAPI(SfcOvsDataStoreAPI.Method.PUT_OVSDB_BRIDGE, methodParameters);
        return (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIPutBridge, executor);
    }

    public static boolean deleteOvsdbNode(InstanceIdentifier<Node> ovsdbNodeIID, ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        Object[] methodParameters = {ovsdbNodeIID};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIDeleteNode =
                new SfcOvsDataStoreAPI(SfcOvsDataStoreAPI.Method.DELETE_OVSDB_NODE, methodParameters);
        return (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIDeleteNode, executor);
    }

    public static boolean deleteOvsdbTerminationPoint(InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID,
            ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        Object[] methodParameters = {ovsdbTerminationPointIID};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIDeleteTerminationPoint =
                new SfcOvsDataStoreAPI(SfcOvsDataStoreAPI.Method.DELETE_OVSDB_TERMINATION_POINT, methodParameters);
        return (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIDeleteTerminationPoint, executor);
    }

    public static ServiceFunctionForwarder augmentSffWithOpenFlowNodeId(ServiceFunctionForwarder sff) {
        String ofNodeId = SfcOvsUtil.getOpenFlowNodeIdForSff(sff);

        if (ofNodeId != null) {
            SffOvsBridgeAugmentationBuilder sffOvsBrAugBuilder;
            OvsBridgeBuilder ovsBrBuilder;

            // if augmentation exists, create builders based on existing data
            SffOvsBridgeAugmentation sffOvsBrAug = sff.getAugmentation(SffOvsBridgeAugmentation.class);
            if (sffOvsBrAug != null) {
                sffOvsBrAugBuilder = new SffOvsBridgeAugmentationBuilder(sffOvsBrAug);

                OvsBridge ovsBridge = sffOvsBrAug.getOvsBridge();
                if (ovsBridge != null) {
                    ovsBrBuilder = new OvsBridgeBuilder(ovsBridge);
                } else {
                    ovsBrBuilder = new OvsBridgeBuilder();
                }

                // if not, create empty builders
            } else {
                sffOvsBrAugBuilder = new SffOvsBridgeAugmentationBuilder();
                ovsBrBuilder = new OvsBridgeBuilder();
            }
            ovsBrBuilder.setOpenflowNodeId(ofNodeId);
            sffOvsBrAugBuilder.setOvsBridge(ovsBrBuilder.build());

            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder(sff);
            sffBuilder.addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBrAugBuilder.build());
            return sffBuilder.build();
        }

        // if the OpenFlowNodeId does not exist, return the original SFF
        else {
            return sff;
        }
    }

    /**
     * This gets VxlanDataLocator
     *
     * @param sff - Service Function Forwarder
     * @return Ip
     */
    public static Ip getSffVxlanDataLocator(ServiceFunctionForwarder sff) {

        List<SffDataPlaneLocator> dplList = sff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator dpl: dplList) {
             if (dpl.getDataPlaneLocator() != null &&
                 dpl.getDataPlaneLocator().getTransport() == VxlanGpe.class) {
                 return (Ip) dpl.getDataPlaneLocator().getLocatorType();
             }
        }
        return null;
    }

    /**
     * This gets the OVSDB Manager Topology Node for the
     * {@link ServiceFunctionForwarder}, using the IP address
     * found in an IP based Data Plane Locator. If there isn't
     * an IP based Data Plane Locator, then this will return null.
     *
     * @param serviceFunctionForwarder - {@link ServiceFunctionForwarder}
     * @param executor - {@link ExecutorService}
     * @return {@link Node}
     */
    public static Node lookupTopologyNode(ServiceFunctionForwarder serviceFunctionForwarder, ExecutorService executor) {
        List<SffDataPlaneLocator> sffDplList = serviceFunctionForwarder.getSffDataPlaneLocator();
        IpAddress ip = null;

        if (sffDplList == null) {
            LOG.debug("No IP Data Plane Locator for Service Function Forwarder {}, ", serviceFunctionForwarder);
            return null;
        }

        /*
         * Go through the Data Plane Locators, looking for an IP-based
         * locator. If we find one, use the IP address from that as the
         * IP for the OVSDB manager connection.
         */
        for (SffDataPlaneLocator sffDpl : sffDplList) {
            if ((sffDpl.getDataPlaneLocator() != null) && sffDpl.getDataPlaneLocator().getLocatorType() != null) {
                Class<? extends DataContainer> locatorType =
                        sffDpl.getDataPlaneLocator().getLocatorType().getImplementedInterface();
                if (locatorType.isAssignableFrom(Ip.class)) {
                    Ip ipPortLocator = (Ip) sffDpl.getDataPlaneLocator().getLocatorType();
                    IpAddress ipAddress = new IpAddress(ipPortLocator.getIp().getValue());
                    ip = ipAddress;
                }
            }
        }
        if (ip == null) {
            LOG.debug("Could not get IP address for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }
        return SfcOvsUtil.getManagerNodeByIp(ip, executor);

    }

    public static String getOpenFlowNodeIdForSff(ServiceFunctionForwarder serviceFunctionForwarder) {
        Node managerNode =
                lookupTopologyNode(serviceFunctionForwarder, OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (managerNode == null) {
            LOG.warn("No Topology Node for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }
        SffOvsBridgeAugmentation serviceForwarderOvsBridgeAugmentation =
                serviceFunctionForwarder.getAugmentation(SffOvsBridgeAugmentation.class);
        if (serviceForwarderOvsBridgeAugmentation == null) {
            LOG.warn("No SffOvsBridgeAugmentation for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }
        OvsBridge serviceForwarderOvsBridge = serviceForwarderOvsBridgeAugmentation.getOvsBridge();

        if (serviceForwarderOvsBridge == null) {
            LOG.warn("No OvsBridge for SffOvsBridgeAugmentation in Service Function Forwarder {}",
                    serviceFunctionForwarder);
            return null;
        }

        OvsdbBridgeAugmentationBuilder builder = new OvsdbBridgeAugmentationBuilder();
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(SfcOvsUtil.buildOvsdbNodeIID(managerNode.getNodeId()));
        builder.setManagedBy(ovsdbNodeRef);
        builder.setBridgeName(new OvsdbBridgeName(serviceForwarderOvsBridge.getBridgeName()));

        NodeId nodeId = getManagedByNodeId(builder.build());
        DatapathId datapathId = getOvsDataPathId(nodeId);
        if (datapathId == null) {
            LOG.warn("No DatapathId for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }
        Long macLong = getLongFromDpid(datapathId.getValue());

        return "openflow:" + String.valueOf(macLong);
    }

    private static DatapathId getOvsDataPathId(NodeId nodeId) {
        Object[] methodParams = {SfcOvsUtil.buildOvsdbBridgeIID(nodeId)};
        SfcOvsDataStoreAPI readOvsdbBridge =
                new SfcOvsDataStoreAPI(SfcOvsDataStoreAPI.Method.READ_OVSDB_BRIDGE, methodParams);

        OvsdbBridgeAugmentation readBridge = (OvsdbBridgeAugmentation) SfcOvsUtil.submitCallable(readOvsdbBridge,
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());

        if (readBridge == null) {
            return null;
        }
        return readBridge.getDatapathId();
    }

    private static Long getLongFromDpid(String dpid) {
        String HEX = "0x";
        String[] addressInBytes = dpid.split(":");
        Long address = (Long.decode(HEX + addressInBytes[2]) << 40) | (Long.decode(HEX + addressInBytes[3]) << 32)
                | (Long.decode(HEX + addressInBytes[4]) << 24) | (Long.decode(HEX + addressInBytes[5]) << 16)
                | (Long.decode(HEX + addressInBytes[6]) << 8) | (Long.decode(HEX + addressInBytes[7]));
        return address;
    }

    public static Node getManagerNodeByIp(IpAddress ip, ExecutorService executor) {
        String ipAddressString = null;

        if ((ip == null) || ((ip.getIpv4Address() == null) && (ip.getIpv6Address() == null))) {
            LOG.warn("Invalid IP address");
            return null;
        }
        if (ip.getIpv4Address() != null) {
            ipAddressString = ip.getIpv4Address().getValue();
        } else if (ip.getIpv6Address() != null) {
            ipAddressString = ip.getIpv6Address().getValue();
        }
        Object[] methodParams = {ipAddressString};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPI =
                new SfcOvsDataStoreAPI(SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_IP, methodParams);
        Node node = (Node) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPI, executor);

        if (node != null && node.getNodeId() != null) {
            return node;
        } else {
            LOG.warn("OVS Node for IP address {} does not exist!", methodParams[0]);
            return null;
        }
    }

    public static OvsdbNodeAugmentation getOvsdbNodeAugmentation(OvsdbNodeRef nodeRef, ExecutorService executor) {
        Preconditions.checkNotNull(executor);
        if (nodeRef.getValue().getTargetType().equals(Node.class)) {
            Object[] methodParams = {nodeRef};
            SfcOvsDataStoreAPI readOvsdbNode =
                    new SfcOvsDataStoreAPI(SfcOvsDataStoreAPI.Method.READ_OVSDB_NODE_BY_REF, methodParams);

            Node ovsdbNode = (Node) SfcOvsUtil.submitCallable(readOvsdbNode, executor);

            if (ovsdbNode != null) {
                return ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
            } else {
                LOG.warn("Could not find ovsdb-node for connection for {}", ovsdbNode);
            }
        } else {
            LOG.warn("Bridge 'managedBy' non-ovsdb-node.  nodeRef {}", nodeRef);
        }
        return null;
    }

    interface OvsdbTPComp {
        public boolean compare(OvsdbTerminationPointAugmentation otp);
    }

    private static Long getOvsPort(String nodeName, OvsdbTPComp comp) {
        if (nodeName == null) {
            return null;
        }

        InstanceIdentifier<Topology> topoIID = buildOvsdbTopologyIID();
        Topology topo = SfcDataStoreAPI.readTransactionAPI(topoIID, LogicalDatastoreType.OPERATIONAL);

        if (topo == null) {
            return null;
        }

        List<Node> nodes = topo.getNode();
        if (nodes == null) {
           return null;
        }

        for (Node node : nodes) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
            List<TerminationPoint> tpList = node.getTerminationPoint();

            if (ovsdbBridgeAugmentation == null || tpList == null) {
                continue;
            }

            Long dpid = getLongFromDpid(ovsdbBridgeAugmentation.getDatapathId().getValue());
            if (nodeName.equals("openflow:" + String.valueOf(dpid))) {
                for (TerminationPoint tp : tpList) {
                    OvsdbTerminationPointAugmentation otp = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if (comp.compare(otp)) {
                        return otp.getOfport();
                    }
                }
            }
        }
        return null;
    }

    /**
     * This gets openflow port by port name
     * @param nodeName openflow node name
     * @param portName openflow port name
     * @return port number
     */
    public static Long getOfPortByName(String nodeName, String portName) {
        class PortNameCompare implements OvsdbTPComp {
            private String portName;
            public PortNameCompare(String portName) {
                this.portName = portName;
            }
            public boolean compare(OvsdbTerminationPointAugmentation otp) {
                if (otp == null)
                    return false;
                if (portName.equals(otp.getName()))
                    return true;
                return false;
            }
        }

        return getOvsPort(nodeName, new PortNameCompare(portName));
    }

    /**
     * This gets vxlan openflow port
     * @param nodeName openflow node name
     * @return port number
     */
    public static Long getVxlanOfPort(String nodeName) {
        class VxlanPortCompare implements OvsdbTPComp {
            public boolean compare(OvsdbTerminationPointAugmentation otp) {
                if (otp == null) {
                    return false;
                }

                if (otp.getInterfaceType() == InterfaceTypeVxlan.class) {
                   return true;
                }
                return false;
            }
        }
        return getOvsPort(nodeName, new VxlanPortCompare());
    }

    /**
     * This gets the vxlan-gpe openflow port
     * @param nodeName openflow node name
     * @return port number
     */
    public static Long getVxlanGpeOfPort(String nodeName) {
        class VxlanGpePortCompare implements OvsdbTPComp {
            public boolean compare(OvsdbTerminationPointAugmentation otp) {
                if (otp == null) {
                    return false;
                }

                if (otp.getInterfaceType() == InterfaceTypeVxlanGpe.class) {
                   return true;
                }

                // If the interface type is not VxlanGpe, then it may be Vxlan with the option exts=gpe set
                List<Options> options = otp.getOptions();
                if(options != null) {
                    for(Options option : options) {
                        if(option.getValue() != null && option.getOption() != null) {
                            if(option.getOption().equals(OVSDB_OPTION_EXTS) && option.getValue().equals(OVSDB_OPTION_GPE)) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            }
        }
        return getOvsPort(nodeName, new VxlanGpePortCompare());
    }
}

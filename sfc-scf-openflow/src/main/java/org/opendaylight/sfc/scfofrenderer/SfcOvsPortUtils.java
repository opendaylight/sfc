/*
 * Copyright (c) 2015 Intel Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsPortUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsPortUtils.class);

    private static String getLongFromDpid(String dpid) {
        String HEX = "0x";
        String[] addressInBytes = dpid.split(":");
        Long address = (Long.decode(HEX + addressInBytes[2]) << 40) | (Long.decode(HEX + addressInBytes[3]) << 32)
                | (Long.decode(HEX + addressInBytes[4]) << 24) | (Long.decode(HEX + addressInBytes[5]) << 16)
                | (Long.decode(HEX + addressInBytes[6]) << 8) | (Long.decode(HEX + addressInBytes[7]));
        return "openflow:" + String.valueOf(address);
    }

    public static Long getOfPortByName(String nodeName, String portName) {

        if (nodeName == null || portName == null) {
            return null;
        }

        LOG.debug("\ngetOfPortByName node: {} port: {}", nodeName, portName);

        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(new TopologyId("ovsdb:1")));
        Topology topo = SfcDataStoreAPI.readTransactionAPI(topoIID, LogicalDatastoreType.OPERATIONAL);

        if (topo == null) {
            return null;
        }

        List<Node> nodes = topo.getNode();

        StringBuffer sb = new StringBuffer();
        sb.append(nodeName).append(":");

        for (Node node : nodes) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
            List<TerminationPoint> tpList = node.getTerminationPoint();

            if (ovsdbBridgeAugmentation == null || tpList == null) {
                continue;
            }

            String dpid = getLongFromDpid(ovsdbBridgeAugmentation.getDatapathId().getValue());
            if (nodeName.equals(dpid)) {
                for (TerminationPoint tp : tpList) {
                    OvsdbTerminationPointAugmentation otp = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if (otp == null) {
                        continue;
                    }
                    if (otp.getName().equals(portName)) {
                        return otp.getOfport();
                    }
                }
            }
        }
        return null;
    }

    public static Long getVxlanOfPort(String nodeName) {
        if (nodeName == null) {
            return null;
        }

        LOG.debug("\ngetVxlanOfPort node: {}", nodeName);

        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(new TopologyId("ovsdb:1")));
        Topology topo = SfcDataStoreAPI.readTransactionAPI(topoIID, LogicalDatastoreType.OPERATIONAL);

        if (topo == null) {
            LOG.warn("\ngetVxlanOfPort doesn't find vxlan port in node: {}", nodeName);
            return null;
        }

        List<Node> nodes = topo.getNode();
        if (nodes == null) {
            LOG.warn("\ngetVxlanOfPort doesn't find vxlan port in node: {}", nodeName);
            return null;
        }

        for (Node node : nodes) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
            List<TerminationPoint> tpList = node.getTerminationPoint();

            if (ovsdbBridgeAugmentation == null || tpList == null) {
                continue;
            }

            String dpid = getLongFromDpid(ovsdbBridgeAugmentation.getDatapathId().getValue());
            if (nodeName.equals(dpid)) {
                for (TerminationPoint tp : tpList) {
                    OvsdbTerminationPointAugmentation otp = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if (otp == null) {
                        continue;
                    }
                    if (otp.getInterfaceType() == InterfaceTypeVxlan.class) {
                        return otp.getOfport();
                    }
                }
            }
        }
        LOG.warn("\ngetVxlanOfPort doesn't find vxlan port in node: {}", nodeName);
        return null;
    }

    public static String getSffOpenFlowNodeName(ServiceFunctionForwarder sff) {

        sff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(sff);
        if (sff == null) {
           return null;
        }

        // Check if its an service-function-forwarder-ovs augmentation
        // if it is, then get the open flow node id there
        SffOvsBridgeAugmentation ovsSff = sff.getAugmentation(SffOvsBridgeAugmentation.class);
        if (ovsSff != null) {
            if (ovsSff.getOvsBridge() != null) {
                return ovsSff.getOvsBridge().getOpenflowNodeId();
            }
        }

        return null;
    }

    public static Ip getSffIpDataLocator(ServiceFunctionForwarder sff, java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType> type) {
        List<SffDataPlaneLocator> dplList = sff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator dpl: dplList) {
             if (dpl.getDataPlaneLocator() != null &&
                 dpl.getDataPlaneLocator().getTransport() == type) {
                 return (Ip) dpl.getDataPlaneLocator().getLocatorType();
             }
        }
        return null;
   }
}

/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vpp_renderer.renderer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_vpp_renderer.listener.ServiceForwarderListener;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.vpp.rev160706.SffNetconfAugmentation;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VppSffManager {

    private static final Logger LOG = LoggerFactory.getLogger(VppSffManager.class);

    private final ServiceForwarderListener sffListener;
    private final VppNodeManager nodeManager;

    public VppSffManager(DataBroker dataBroker, VppNodeManager nodeManager) {
        // Register SFF listener
        this.nodeManager = nodeManager;
        sffListener = new ServiceForwarderListener(dataBroker, this);
    }

    public void disposeSff(ServiceFunctionForwarder sff, boolean disconnect) {
        boolean connected = false;
        SffNetconfAugmentation sffNetconfAugmentation = sff.getAugmentation(SffNetconfAugmentation.class);
        if (sffNetconfAugmentation == null) {
            return;
        }

        IpAddress forwarderMgmtIp = sff.getIpMgmtAddress();
        if (forwarderMgmtIp == null) {
            LOG.warn("Service function forwarder {} has no management Ip address",
                        sff.getName().getValue());
            return;
        }

        // Find appropriate node for SFF
        connected = false;
        for (Node netconfNode : nodeManager.getConnectedNodes().values()) {
            IpAddress netconfNodeIp = nodeManager.getNetconfNodeIp(netconfNode);
            if (netconfNodeIp.equals(forwarderMgmtIp)) {
                // Find the right mountpoint
                DataBroker mountPoint = nodeManager.getActiveMountPoints()
                        .get(netconfNode.getNodeId());
                if (mountPoint != null) {
                    connected = true;
                }
                break;
            }
        }
        if (disconnect) {
            nodeManager.unmountNode(sff.getName().getValue());
            LOG.info("SFF {} is unmounted by sfc vpp renderer", sff.getName());
        } else {
            if (!connected) {
                String deviceId = sff.getName().getValue();
                String deviceIp = forwarderMgmtIp.getIpv4Address().getValue();
                String devicePort = "7777";
                String username = "admin";
                String password = "admin";
                boolean isTcpOnly = true;
                nodeManager.mountNode(deviceId, deviceIp, devicePort, username, password, isTcpOnly);
                LOG.info("SFF {} is mounted by sfc vpp renderer", sff.getName());
            }
        }
    }

    public void unregisterSffListener() {
        sffListener.getRegistrationObject().close();
    }
}

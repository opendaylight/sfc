/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider.renderer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.sfc_netconf.provider.api.NetconfDataStoreAPI;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfUtils;
import org.opendaylight.sfc.sfc_netconf.provider.listener.ServiceForwarderListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.opendaylight.sfc.sfc_netconf.provider.api.NetconfDataStoreAPI.Transaction.DELETE_LOCAL;
import static org.opendaylight.sfc.sfc_netconf.provider.api.NetconfDataStoreAPI.Transaction.WRITE_LOCAL;

public class NetconfServiceForwarderMapper {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfServiceForwarderMapper.class);

    private final NodeManager nodeManager;
    private final ServiceForwarderListener sffListener;

    public NetconfServiceForwarderMapper(DataBroker dataBroker, NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        // Register SFF listener
        sffListener = new ServiceForwarderListener(dataBroker, this);
    }

    public void syncForwarders(List<ServiceFunctionForwarder> forwarders, boolean delete) {
        for (ServiceFunctionForwarder forwarder : forwarders) {
            IpAddress forwarderMgmtIp = forwarder.getIpMgmtAddress();
            if (forwarderMgmtIp == null) {
                LOG.warn("Service function forwarder {} has no management Ip address, cannot be created",
                        forwarder.getName().getValue());
                continue;
            }
            // Find appropriate node for SFF
            for (Node netconfNode : nodeManager.getConnectedNodes().values()) {
                IpAddress netconfNodeIp = nodeManager.getNetconfNodeIp(netconfNode);
                if (netconfNodeIp.equals(forwarderMgmtIp)) {
                    // Find the right mountpoint
                    DataBroker mountPoint = nodeManager.getActiveMountPoints()
                            .get(netconfNode.getNodeId());
                    if (mountPoint != null) {
                        for (SffDataPlaneLocator forwarderDpl : forwarder.getSffDataPlaneLocator()) {
                            DataPlaneLocator dpl = forwarderDpl.getDataPlaneLocator();
                            LocatorType locatorType = dpl.getLocatorType();
                            Ip sffIp = null;
                            if (locatorType instanceof Ip) {
                                LOG.debug("IP locator found: {} ", locatorType);
                                sffIp = (Ip) locatorType;
                            }
                            if (sffIp != null && sffIp.getIp() != null) {
                                IpAddress ipAddress = sffIp.getIp();
                                // Create/remove local SFF
                                org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder localForwarder =
                                        SfcNetconfUtils.createLocalForwarder(ipAddress);
                                if (localForwarder != null && !delete) {
                                    new NetconfDataStoreAPI(mountPoint, localForwarder.getLocal(), WRITE_LOCAL)
                                            .call();
                                }
                                if (localForwarder != null && delete) {
                                    new NetconfDataStoreAPI(mountPoint, localForwarder.getLocal(), DELETE_LOCAL)
                                            .call();

                                }
                            }
                        }
                    }
                } else {
                    LOG.warn("Node not found for SFF {}", forwarder.getName());
                }
            }
        }
    }

    public void unregisterSffListener() {
        sffListener.getRegistrationObject().close();
    }
}

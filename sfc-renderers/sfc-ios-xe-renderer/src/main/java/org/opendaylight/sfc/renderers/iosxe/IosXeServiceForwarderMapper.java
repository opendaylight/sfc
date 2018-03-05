/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.iosxe;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.renderers.iosxe.utils.IosXeDataStoreAPI;
import org.opendaylight.sfc.renderers.iosxe.utils.SfcIosXeUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IosXeServiceForwarderMapper {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeServiceForwarderMapper.class);

    private final NodeManager nodeManager;

    @Inject
    public IosXeServiceForwarderMapper(DataBroker dataBroker, NodeManager nodeManager) {
        this.nodeManager = nodeManager;
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
                                org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain
                                    .ServiceFunctionForwarder localForwarder =
                                    SfcIosXeUtils.createLocalForwarder(ipAddress);
                                if (localForwarder != null && !delete) {
                                    IosXeDataStoreAPI writeServiceFunction =
                                            new IosXeDataStoreAPI(mountPoint, localForwarder.getLocal(),
                                                                  IosXeDataStoreAPI.Transaction.WRITE_LOCAL,
                                                                  LogicalDatastoreType.CONFIGURATION);
                                    Object result = writeServiceFunction.call();
                                    if (Boolean.TRUE.equals(result)) {
                                        LOG.info("Local forwarder with ip {} created on node {}",
                                                forwarder.getIpMgmtAddress().toString(),
                                                netconfNode.getNodeId().getValue());
                                    }
                                }
                                if (localForwarder != null && delete) {
                                    IosXeDataStoreAPI writeServiceFunction =
                                            new IosXeDataStoreAPI(mountPoint, localForwarder.getLocal(),
                                                                  IosXeDataStoreAPI.Transaction.DELETE_LOCAL,
                                                                  LogicalDatastoreType.CONFIGURATION);
                                    Object result = writeServiceFunction.call();
                                    if (Boolean.TRUE.equals(result)) {
                                        LOG.info("Local forwarder removed from node {}", netconfNode.getNodeId()
                                                .getValue());
                                    }
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
}

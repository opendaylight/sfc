/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.renderer;

import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.DELETE_FUNCTION;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.WRITE_FUNCTION;

import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_ios_xe.provider.listener.ServiceFunctionListener;
import org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI;
import org.opendaylight.sfc.sfc_ios_xe.provider.utils.SfcIosXeUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.ConfigServiceChainSfModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.config.service.chain.sf.mode.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IosXeServiceFunctionMapper {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeServiceFunctionMapper.class);

    private final NodeManager nodeManager;
    private final ServiceFunctionListener sfListener;

    public IosXeServiceFunctionMapper(DataBroker dataBroker, NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        // Register SF listener
        sfListener = new ServiceFunctionListener(dataBroker, this);
    }

    public void syncFunctions(List<ServiceFunction> functions, boolean delete) {
        for (ServiceFunction function : functions) {
            IpAddress forwarderMgmtIp = function.getIpMgmtAddress();
            if (forwarderMgmtIp == null) {
                LOG.warn("Service function forwarder {} has no management Ip address, cannot be created",
                        function.getName().getValue());
                continue;
            }
            // Find appropriate node for SFF
            for (Node netconfNode : nodeManager.getConnectedNodes().values()) {
                IpAddress netconfNodeIp = nodeManager.getNetconfNodeIp(netconfNode);
                if (netconfNodeIp.equals(forwarderMgmtIp)) {
                    // Find the right mountpoint
                    DataBroker mountPoint = nodeManager.getActiveMountPoints()
                            .get(netconfNode.getNodeId());
                    if (mountPoint != null && !delete) {
                        org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction serviceFunction =
                                createNetconfServiceFunction(function);
                        if (serviceFunction != null) {
                            IosXeDataStoreAPI writeServiceFunction = new IosXeDataStoreAPI(mountPoint, serviceFunction,
                                    WRITE_FUNCTION, LogicalDatastoreType.CONFIGURATION);
                            Object result = writeServiceFunction.call();
                            if (result != null && result == Boolean.TRUE) {
                                LOG.info("Service function {} created on node {}", serviceFunction.getName(),
                                        netconfNode.getNodeId().getValue());
                            }
                        }
                    }
                    if (mountPoint != null && delete) {
                        org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction serviceFunction =
                                createNetconfServiceFunction(function);
                        if (serviceFunction != null) {
                            IosXeDataStoreAPI writeServiceFunction = new IosXeDataStoreAPI(mountPoint, serviceFunction.getKey(),
                                    DELETE_FUNCTION, LogicalDatastoreType.CONFIGURATION);
                            Object result = writeServiceFunction.call();
                            if (result != null && result == Boolean.TRUE) {
                                LOG.info("Service function {} removed", serviceFunction.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    private org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction createNetconfServiceFunction(
            ServiceFunction function) {
        SfName sfName = function.getName();

        SfDataPlaneLocator sfDataPlaneLocator = SfcIosXeUtils.getDplWithIpLocatorType(function
                .getSfDataPlaneLocator());
        if (sfDataPlaneLocator == null || sfDataPlaneLocator.getLocatorType() == null) {
            LOG.warn("Any suitable data plane locator has not been found for service function {}", function.getName()
                    .getValue());
            return null;
        }
        IpAddress sfDplIpAddress = null;
        LocatorType locatorType = sfDataPlaneLocator.getLocatorType();
        if (locatorType instanceof Ip) {
            sfDplIpAddress = ((Ip) locatorType).getIp();
        }
        if (sfDplIpAddress != null && sfDplIpAddress.getIpv4Address() != null) {
            // Ip Address
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setAddress(new Ipv4Address(sfDplIpAddress.getIpv4Address().getValue()));
            // Encapsulation
            Class<? extends SlTransportType> transport = sfDataPlaneLocator.getTransport();
            if (transport == org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Gre.class) {
                ConfigServiceChainSfModeBuilder sfModeBuilder = new ConfigServiceChainSfModeBuilder();
                sfModeBuilder.setIp(ipBuilder.build());
                ServiceFunctionBuilder netconfServiceFunction = new ServiceFunctionBuilder();
                netconfServiceFunction.setName(sfName.getValue())
                        .setKey(new ServiceFunctionKey(sfName.getValue()))
                        .setConfigServiceChainSfMode(sfModeBuilder.build());
                return netconfServiceFunction.build();
            }
        }
        return null;
    }

    public void unregisterSfListener() {
        sfListener.getRegistrationObject().close();
    }
}

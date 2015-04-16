/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.api;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder2;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator2;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map SFC Service Function Forwarder to OVS Bridge
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see SfcSffToOvsMappingAPI
 * <p/>
 * <p/>
 * <p/>
 * @since 2015-03-23
 */
public class SfcSffToOvsMappingAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcSffToOvsMappingAPI.class);

    public static OvsdbBridgeAugmentation buildOvsdbBridgeAugmentation(ServiceFunctionForwarder serviceFunctionForwarder) {
        Preconditions.checkNotNull(serviceFunctionForwarder);

        OvsdbBridgeAugmentationBuilder ovsdbBridgeBuilder = new OvsdbBridgeAugmentationBuilder();

        ServiceFunctionForwarder2 serviceForwarderOvsBridgeAugmentation =
                serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder2.class);
        try {
            Preconditions.checkNotNull(serviceForwarderOvsBridgeAugmentation);
            OvsBridge serviceForwarderOvsBridge = serviceForwarderOvsBridgeAugmentation.getOvsBridge();

            Preconditions.checkNotNull(serviceForwarderOvsBridge);
            ovsdbBridgeBuilder.setBridgeName(new OvsdbBridgeName(serviceForwarderOvsBridge.getBridgeName()));
            ovsdbBridgeBuilder.setBridgeUuid(serviceForwarderOvsBridge.getUuid());

        } catch (NullPointerException e) {
            LOG.warn("Cannot build OvsdbBridgeAugmentation. Missing OVS Bridge augmentation on SFF {}", serviceFunctionForwarder.getName());
            return null;
        }

        ServiceFunctionForwarder1 serviceForwarderOvsNodeAugmentation =
                serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder1.class);
        try {
            Preconditions.checkNotNull(serviceForwarderOvsNodeAugmentation);
            OvsNode serviceForwarderOvsNode = serviceForwarderOvsNodeAugmentation.getOvsNode();

            Preconditions.checkNotNull(serviceForwarderOvsNode);
            ovsdbBridgeBuilder.setManagedBy(serviceForwarderOvsNode.getNodeId());

        } catch (NullPointerException e) {
            LOG.warn("Cannot build OvsdbBridgeAugmentation. Missing OVS Node augmentation on SFF {}", serviceFunctionForwarder.getName());
            return null;
        }
        return ovsdbBridgeBuilder.build();
    }

    public static List<OvsdbTerminationPointAugmentation> buildTerminationPointAugmentationList(
            List<SffDataPlaneLocator> sffDataPlaneLocatorList) {

        Preconditions.checkNotNull(sffDataPlaneLocatorList,
                "Cannot build TerminationPointAugmentation, SffDataPlaneLocatorList is null.");

        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList = new ArrayList<>();

        for (SffDataPlaneLocator sffDataPlaneLocator : sffDataPlaneLocatorList) {
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = new OvsdbTerminationPointAugmentationBuilder();

            ovsdbTerminationPointBuilder.setName(sffDataPlaneLocator.getName());
            ovsdbTerminationPointBuilder.setInterfaceType(getDataPlaneLocatorInterfaceType(sffDataPlaneLocator.getDataPlaneLocator()));
            ovsdbTerminationPointBuilder.setOptions(getSffDataPlaneLocatorOptions(sffDataPlaneLocator));
            ovsdbTerminationPointList.add(ovsdbTerminationPointBuilder.build());
        }

        return ovsdbTerminationPointList;
    }

    private static List<Options> getDataPlaneLocatorOptions(DataPlaneLocator dataPlaneLocator) {
        Preconditions.checkNotNull(dataPlaneLocator, "Cannot determine DataPlaneLocator locator type, dataPlaneLocator is null.");
        List<Options> options = new ArrayList<>();

        try {
            Class<? extends DataContainer> locatorType = dataPlaneLocator.getLocatorType().getImplementedInterface();
            if (locatorType.isAssignableFrom(Ip.class)) {
                Ip ipPortLocator = (Ip) dataPlaneLocator.getLocatorType();

                OptionsBuilder optionsIpBuilder = new OptionsBuilder();
                optionsIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
                optionsIpBuilder.setValue(ipPortLocator.getIp().getIpv4Address().getValue());

                options.add(optionsIpBuilder.build());
            }
        } catch (NullPointerException e) {
            LOG.warn("Cannot determine DataPlaneLocator locator type, dataPlaneLocator.getLocatorType() is null.");
        }

        return options;
    }

    private static List<Options> getSffDataPlaneLocatorOptions(SffDataPlaneLocator sffDataPlaneLocator) {
        Preconditions.checkNotNull(sffDataPlaneLocator, "Cannot gather SffDataPlaneLocator Options, sffDataPlaneLocator is null.");
        List<Options> options = new ArrayList<>();

        SffDataPlaneLocator2 sffDataPlaneLocatorOvsOptions = sffDataPlaneLocator.getAugmentation(SffDataPlaneLocator2.class);
        OvsOptions ovsOptions = sffDataPlaneLocatorOvsOptions.getOvsOptions();

        try {
            Preconditions.checkNotNull(ovsOptions.getLocalIp());
            OptionsBuilder optionsLocalIpBuilder = new OptionsBuilder();
            optionsLocalIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
            optionsLocalIpBuilder.setValue(SfcOvsUtil.convertIpAddressToString(ovsOptions.getLocalIp()));
            options.add(optionsLocalIpBuilder.build());
        } catch (Exception e) {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        }

        try {
            Preconditions.checkNotNull(ovsOptions.getRemoteIp());
            OptionsBuilder optionsDstIpBuilder = new OptionsBuilder();
            optionsDstIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_REMOTE_IP);
            optionsDstIpBuilder.setValue(SfcOvsUtil.convertIpAddressToString(ovsOptions.getRemoteIp()));
            options.add(optionsDstIpBuilder.build());
        } catch (Exception e) {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_REMOTE_IP);
        }

        try {
            Preconditions.checkNotNull(ovsOptions.getDstPort());
            OptionsBuilder optionsDstPortBuilder = new OptionsBuilder();
            optionsDstPortBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_DST_PORT);
            optionsDstPortBuilder.setValue(ovsOptions.getDstPort().getValue().toString());
            options.add(optionsDstPortBuilder.build());
        } catch (Exception e) {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_DST_PORT);
        }

        try {
            Preconditions.checkNotNull(ovsOptions.getKey());
            OptionsBuilder optionsKeyBuilder = new OptionsBuilder();
            optionsKeyBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_KEY);
            optionsKeyBuilder.setValue(ovsOptions.getKey());
            options.add(optionsKeyBuilder.build());
        } catch (Exception e) {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_KEY);
        }

        try {
            Preconditions.checkNotNull(ovsOptions.getNsp());
            OptionsBuilder optionsNspBuilder = new OptionsBuilder();
            optionsNspBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSP);
            optionsNspBuilder.setValue(ovsOptions.getNsp());
            options.add(optionsNspBuilder.build());
        } catch (Exception e) {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSP);
        }

        try {
            Preconditions.checkNotNull(ovsOptions.getNsi());
            OptionsBuilder optionsNsiBuilder = new OptionsBuilder();
            optionsNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSI);
            optionsNsiBuilder.setValue(ovsOptions.getNsi());
            options.add(optionsNsiBuilder.build());
        } catch (Exception e) {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSI);
        }

        return options;
    }

    private static Class<? extends InterfaceTypeBase> getDataPlaneLocatorInterfaceType(DataPlaneLocator dataPlaneLocator) {
        Preconditions.checkNotNull(dataPlaneLocator, "Cannot determine DataPlaneLocator transport type, dataPlaneLocator is null.");

        if (dataPlaneLocator.getTransport() == Other.class) {
            return InterfaceTypeInternal.class;
        } else if (dataPlaneLocator.getTransport() == VxlanGpe.class) {
            return InterfaceTypeVxlan.class;
        } else {
            LOG.warn("Cannot determine DataPlaneLocator transport type, dataPlaneLocator.getTransport() is null.");

            //TODO: remove once MDSAL OVSDB will not require interface type to be specified
            LOG.warn("Falling back to InterfaceTypeInternal");
            return InterfaceTypeInternal.class;
        }
    }
}

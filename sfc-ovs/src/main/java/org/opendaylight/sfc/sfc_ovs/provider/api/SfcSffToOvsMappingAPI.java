/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map SFC Service Function Forwarder to OVS Bridge
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see SfcSffToOvsMappingAPI
 *      <p>
 * @since 2015-03-23
 */
public class SfcSffToOvsMappingAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcSffToOvsMappingAPI.class);
    private static final String OF_PORT = "6653";

    /**
     * Builds OvsdbBridgeAugmentation from ServiceFunctionForwarder object. Built augmentation is
     * intended to be written
     * into Configuration Ovsdb DataStore (network-topology/topology/ovsdb:1).
     * <p>
     *
     * @param serviceFunctionForwarder {@link ServiceFunctionForwarder}
     * @param executor - {@link ExecutorService}
     * @return {@link OvsdbBridgeAugmentation}
     */
    public static OvsdbBridgeAugmentation buildOvsdbBridgeAugmentation(
            ServiceFunctionForwarder serviceFunctionForwarder, ExecutorService executor) {
        Preconditions.checkNotNull(serviceFunctionForwarder);

        OvsdbBridgeAugmentationBuilder ovsdbBridgeBuilder = new OvsdbBridgeAugmentationBuilder();

        SffOvsBridgeAugmentation serviceForwarderOvsBridgeAugmentation =
                serviceFunctionForwarder.getAugmentation(SffOvsBridgeAugmentation.class);
        if (serviceForwarderOvsBridgeAugmentation != null) {
            OvsBridge serviceForwarderOvsBridge = serviceForwarderOvsBridgeAugmentation.getOvsBridge();

            if (serviceForwarderOvsBridge != null) {
                ovsdbBridgeBuilder.setBridgeName(new OvsdbBridgeName(serviceForwarderOvsBridge.getBridgeName()));
                ovsdbBridgeBuilder.setBridgeUuid(serviceForwarderOvsBridge.getUuid());
            } else {
                LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Bridge augmentation on SFF {}",
                        serviceFunctionForwarder.getName());
                return null;
            }

        } else {
            LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Bridge augmentation on SFF {}",
                    serviceFunctionForwarder.getName());
            return null;
        }

        SffOvsNodeAugmentation serviceForwarderOvsNodeAugmentation =
                serviceFunctionForwarder.getAugmentation(SffOvsNodeAugmentation.class);
        if (serviceForwarderOvsNodeAugmentation != null) {
            OvsNode serviceForwarderOvsNode = serviceForwarderOvsNodeAugmentation.getOvsNode();

            if (serviceForwarderOvsNode != null) {
                ovsdbBridgeBuilder.setManagedBy(serviceForwarderOvsNode.getNodeId());
                OvsdbNodeAugmentation ovsdbNodeAugmentation =
                        SfcOvsUtil.getOvsdbNodeAugmentation(serviceForwarderOvsNode.getNodeId(), executor);
                if (ovsdbNodeAugmentation != null) {
                    ovsdbBridgeBuilder.setControllerEntry(getControllerEntries(ovsdbNodeAugmentation));
                }
            } else {
                LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Node augmentation on SFF {}",
                        serviceFunctionForwarder.getName());
                return null;
            }

        } else {
            Node node = SfcOvsUtil.lookupTopologyNode(serviceFunctionForwarder, executor);
            if (node == null || node.getNodeId() == null) {
                LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Node augmentation on SFF {}",
                        serviceFunctionForwarder.getName());
                return null;
            }
            OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(SfcOvsUtil.buildOvsdbNodeIID(node.getNodeId()));
            ovsdbBridgeBuilder.setManagedBy(ovsdbNodeRef);
            OvsdbNodeAugmentation ovsdbNodeAugmentation = SfcOvsUtil.getOvsdbNodeAugmentation(ovsdbNodeRef, executor);
            if (ovsdbNodeAugmentation != null) {
                ovsdbBridgeBuilder.setControllerEntry(getControllerEntries(ovsdbNodeAugmentation));
            }
        }

        return ovsdbBridgeBuilder.build();
    }

    private static List<ControllerEntry> getControllerEntries(OvsdbNodeAugmentation connection) {
        ControllerEntryBuilder controllerBuilder = new ControllerEntryBuilder();
        List<ControllerEntry> result = new ArrayList<>();
        if (connection.getConnectionInfo().getLocalIp() != null) {
            String localIp = String.valueOf(connection.getConnectionInfo().getLocalIp().getValue());
            String targetString = "tcp:" + localIp + ":" + OF_PORT;
            controllerBuilder.setTarget(new Uri(targetString));
            result.add(controllerBuilder.build());
        }

        return result;
    }

    /**
     * Builds a list of OvsdbTerminationPointAugmentation from a list of SffDataPlanLocators.
     * In other words, it transforms ServiceForwarder DataPlane locators to OVS TerminationPoints.
     * <p>
     * Built list of augmentations is intended to be written into Configuration Ovsdb DataStore
     * (network-topology/topology/ovsdb:1).
     * <p>
     *
     * @param sffDataPlaneLocatorList List&lt;SffDataPlaneLocator&gt;
     * @return List&lt;OvsdbTerminationPointAugmentation&gt;
     */
    public static List<OvsdbTerminationPointAugmentation> buildTerminationPointAugmentationList(
            List<SffDataPlaneLocator> sffDataPlaneLocatorList) {

        Preconditions.checkNotNull(sffDataPlaneLocatorList,
                "Cannot build TerminationPointAugmentation, SffDataPlaneLocatorList is null.");

        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList = new ArrayList<>();

        for (SffDataPlaneLocator sffDataPlaneLocator : sffDataPlaneLocatorList) {
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder =
                    new OvsdbTerminationPointAugmentationBuilder();

            ovsdbTerminationPointBuilder.setName(sffDataPlaneLocator.getName().getValue());
            ovsdbTerminationPointBuilder
                .setInterfaceType(getDataPlaneLocatorInterfaceType(sffDataPlaneLocator.getDataPlaneLocator()));

            List<Options> optionsList = getSffDataPlaneLocatorOptions(sffDataPlaneLocator);
            if (!optionsList.isEmpty()) {
                ovsdbTerminationPointBuilder.setOptions(optionsList);
            }
            ovsdbTerminationPointList.add(ovsdbTerminationPointBuilder.build());
        }

        return ovsdbTerminationPointList;
    }

    /**
     * Get SFF dataplane Locator options such as remote_ip and local_ip.
     * <p>
     * Returns the list of all options associated with a data plane locator. The caller needs
     * to check array is empty and only then add to the associated builder object.
     * <p>
     *
     * @param sffDataPlaneLocator SffDataPlaneLocator
     * @return List&lt;Options&gt; if there are any or null otherwise
     */
    private static List<Options> getSffDataPlaneLocatorOptions(SffDataPlaneLocator sffDataPlaneLocator) {
        Preconditions.checkNotNull(sffDataPlaneLocator,
                "Cannot gather SffDataPlaneLocator Options, sffDataPlaneLocator is null.");
        List<Options> options = new ArrayList<>();

        SffOvsLocatorOptionsAugmentation sffDataPlaneLocatorOvsOptions =
                sffDataPlaneLocator.getAugmentation(SffOvsLocatorOptionsAugmentation.class);
        if (sffDataPlaneLocatorOvsOptions != null) {

            OvsOptions ovsOptions = sffDataPlaneLocatorOvsOptions.getOvsOptions();

            if (ovsOptions.getLocalIp() != null) {
                OptionsBuilder optionsLocalIpBuilder = new OptionsBuilder();
                optionsLocalIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
                optionsLocalIpBuilder.setValue(ovsOptions.getLocalIp());
                options.add(optionsLocalIpBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
            }

            if (ovsOptions.getRemoteIp() != null) {
                OptionsBuilder optionsDstIpBuilder = new OptionsBuilder();
                optionsDstIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_REMOTE_IP);
                optionsDstIpBuilder.setValue(ovsOptions.getRemoteIp());
                options.add(optionsDstIpBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_REMOTE_IP);
            }

            if (ovsOptions.getDstPort() != null) {
                OptionsBuilder optionsDstPortBuilder = new OptionsBuilder();
                optionsDstPortBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_DST_PORT);
                optionsDstPortBuilder.setValue(ovsOptions.getDstPort());
                options.add(optionsDstPortBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_DST_PORT);
            }

            if (ovsOptions.getKey() != null) {
                OptionsBuilder optionsKeyBuilder = new OptionsBuilder();
                optionsKeyBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_KEY);
                optionsKeyBuilder.setValue(ovsOptions.getKey());
                options.add(optionsKeyBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_KEY);
            }

            if (ovsOptions.getNsp() != null) {
                OptionsBuilder optionsNspBuilder = new OptionsBuilder();
                optionsNspBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSP);
                optionsNspBuilder.setValue(ovsOptions.getNsp());
                options.add(optionsNspBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSP);
            }

            if (ovsOptions.getNsi() != null) {
                OptionsBuilder optionsNsiBuilder = new OptionsBuilder();
                optionsNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSI);
                optionsNsiBuilder.setValue(ovsOptions.getNsi());
                options.add(optionsNsiBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSI);
            }

            if (ovsOptions.getExts() != null) {
                OptionsBuilder optionsExtsBuilder = new OptionsBuilder();
                optionsExtsBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_EXTS);
                optionsExtsBuilder.setValue(ovsOptions.getExts());
                options.add(optionsExtsBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_EXTS);
            }

            if (ovsOptions.getInNsp() != null) {
                OptionsBuilder optionsInNspBuilder = new OptionsBuilder();
                optionsInNspBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_IN_NSP);
                optionsInNspBuilder.setValue(ovsOptions.getInNsp());
                options.add(optionsInNspBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_IN_NSP);
            }

            if (ovsOptions.getInNsi() != null) {
                OptionsBuilder optionsInNsiBuilder = new OptionsBuilder();
                optionsInNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_IN_NSI);
                optionsInNsiBuilder.setValue(ovsOptions.getInNsi());
                options.add(optionsInNsiBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_IN_NSI);
            }

            if (ovsOptions.getOutNsp() != null) {
                OptionsBuilder optionsOutNspBuilder = new OptionsBuilder();
                optionsOutNspBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_OUT_NSP);
                optionsOutNspBuilder.setValue(ovsOptions.getOutNsp());
                options.add(optionsOutNspBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_OUT_NSP);
            }

            if (ovsOptions.getOutNsi() != null) {
                OptionsBuilder optionsOutNsiBuilder = new OptionsBuilder();
                optionsOutNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_OUT_NSI);
                optionsOutNsiBuilder.setValue(ovsOptions.getOutNsi());
                options.add(optionsOutNsiBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_OUT_NSI);
            }

            if (ovsOptions.getNshc1() != null) {
                OptionsBuilder optionsNsiBuilder = new OptionsBuilder();
                optionsNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSHC1);
                optionsNsiBuilder.setValue(ovsOptions.getNshc1());
                options.add(optionsNsiBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSHC1);
            }

            if (ovsOptions.getNshc2() != null) {
                OptionsBuilder optionsNsiBuilder = new OptionsBuilder();
                optionsNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSHC2);
                optionsNsiBuilder.setValue(ovsOptions.getNshc2());
                options.add(optionsNsiBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSHC2);
            }

            if (ovsOptions.getNshc3() != null) {
                OptionsBuilder optionsNsiBuilder = new OptionsBuilder();
                optionsNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSHC3);
                optionsNsiBuilder.setValue(ovsOptions.getNshc3());
                options.add(optionsNsiBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSHC3);
            }

            if (ovsOptions.getNshc4() != null) {
                OptionsBuilder optionsNsiBuilder = new OptionsBuilder();
                optionsNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSHC4);
                optionsNsiBuilder.setValue(ovsOptions.getNshc4());
                options.add(optionsNsiBuilder.build());
            } else {
                LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSHC4);
            }
        }

        return options;
    }

    private static Class<? extends InterfaceTypeBase> getDataPlaneLocatorInterfaceType(
            DataPlaneLocator dataPlaneLocator) {
        Preconditions.checkNotNull(dataPlaneLocator,
                "Cannot determine DataPlaneLocator transport type, dataPlaneLocator is null.");

        if (dataPlaneLocator.getTransport() == Other.class) {
            org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Other otherLocatorType =
                    (org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Other) dataPlaneLocator
                        .getLocatorType();
            if (otherLocatorType != null) {
                if (otherLocatorType.getOtherName().equals(SfcOvsUtil.DPL_NAME_DPDK)) {
                    return InterfaceTypeDpdk.class;
                } else if (otherLocatorType.getOtherName().equals(SfcOvsUtil.DPL_NAME_DPDKVHOST)) {
                    return InterfaceTypeDpdkvhost.class;
                } else if (otherLocatorType.getOtherName().equals(SfcOvsUtil.DPL_NAME_DPDKVHOSTUSER)) {
                    return InterfaceTypeDpdkvhostuser.class;
                } else {
                    return InterfaceTypeInternal.class;
                }
            } else {
                return InterfaceTypeInternal.class;
            }
        } else if (dataPlaneLocator.getTransport() == VxlanGpe.class) {
            return InterfaceTypeVxlan.class;
        } else if (dataPlaneLocator.getTransport() == Mac.class) {
            return InterfaceTypeSystem.class;
        } else {
            LOG.warn("Cannot determine DataPlaneLocator transport type, dataPlaneLocator.getTransport() is null.");

            // TODO: remove once MDSAL OVSDB will not require interface type to be specified
            LOG.warn("Falling back to InterfaceTypeInternal");
            return InterfaceTypeInternal.class;
        }
    }
}

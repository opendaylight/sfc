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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map OVS Bridge to SFC Service Function Forwarder
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see SfcOvsToSffMappingAPI
 * <p/>
 * <p/>
 * <p/>
 * @since 2015-03-10
 */
public class SfcOvsToSffMappingAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsToSffMappingAPI.class);

    /**
     * Returns an Service Function Forwarder object which can be stored
     * in DataStore. The returned object is built on basis of OVS Bridge & OVS Termination Points.
     * The ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param node Node Object
     * @return ServiceFunctionForwarder Object
     */
    public static ServiceFunctionForwarder buildServiceFunctionForwarderFromNode(Node node) {
        Preconditions.checkNotNull(node, "Cannot build Service Function Forwarder: OVS Node does not exist!");

        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        try {
            Preconditions.checkNotNull(ovsdbBridgeAugmentation);

            ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
            serviceFunctionForwarderBuilder.setName(getServiceForwarderNameFromNode(node));

            //add OVS Node ref to SFF
            ServiceFunctionForwarder1Builder ovsServiceForwarderAugmentation = new ServiceFunctionForwarder1Builder();
            OvsNodeBuilder ovsNodeBuilder = new OvsNodeBuilder();
            ovsNodeBuilder.setNodeId(ovsdbBridgeAugmentation.getManagedBy());
            ovsServiceForwarderAugmentation.setOvsNode(ovsNodeBuilder.build());
            serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder1.class, ovsServiceForwarderAugmentation.build());

            //add OVS Bridge to SFF
            ServiceFunctionForwarder2Builder ovsServiceForwarder2Augmentation = new ServiceFunctionForwarder2Builder();
            OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
            ovsBridgeBuilder.setBridgeName(ovsdbBridgeAugmentation.getBridgeName().getValue());
            ovsBridgeBuilder.setUuid(ovsdbBridgeAugmentation.getBridgeUuid());
            ovsServiceForwarder2Augmentation.setOvsBridge(ovsBridgeBuilder.build());
            serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder2.class, ovsServiceForwarder2Augmentation.build());

            //add SFF DP locators list to SFF
            serviceFunctionForwarderBuilder.setSffDataPlaneLocator(
                    buildSffDataPlaneLocatorList(ovsdbBridgeAugmentation, node.getTerminationPoint()));

            return serviceFunctionForwarderBuilder.build();

        } catch (NullPointerException e) {
            LOG.warn("Cannot build Service Function Forwarder: OVS Bridge does not exist!");
            return null;
        }
    }

    /**
     * Builds a SffDataPlaneLocator List based on OvsdbBridgeAugmentation and TerminationPoint List.
     * TerminationPoints in the list must be augmented with OvsdbTerminationPointAugmentation.
     * <p/>
     * The ovsdbBridge argument must be not null otherwise
     * NullPointerException will be raised.
     * <p/>
     * The terminationPointList must be not null othewrise
     * empty SffDataPlaneLocator List will be returned.
     *
     * @param ovsdbBridge          OvsdbBridgeAugmentation Object
     * @param terminationPointList List<TerminationPoint>
     * @return List<SffDataPlaneLocator> (filled or empty, but not null)
     */
    private static List<SffDataPlaneLocator> buildSffDataPlaneLocatorList(OvsdbBridgeAugmentation ovsdbBridge,
                                                                          List<TerminationPoint> terminationPointList) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot build SffDataPlaneLocator: OVS Bridge does not exist!");
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();

        try {
            Preconditions.checkNotNull(terminationPointList);

            //OVS bridge will be the same for all DP locators
            OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
            ovsBridgeBuilder.setBridgeName(ovsdbBridge.getBridgeName().getValue());
            ovsBridgeBuilder.setUuid(ovsdbBridge.getBridgeUuid());

            //fill SffDataPlaneLocatorList with DP locators
            for (TerminationPoint terminationPoint : terminationPointList) {
                OvsdbTerminationPointAugmentation terminationPointAugmentation =
                        terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);

                if (terminationPointAugmentation != null) {
                    SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
                    sffDataPlaneLocatorBuilder.setName(terminationPointAugmentation.getName());
                    DataPlaneLocator dataPlaneLocator = buildDataPlaneLocatorFromTerminationPoint(terminationPointAugmentation);
                    if (dataPlaneLocator != null) {
                        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocator);
                    }
                    SffDataPlaneLocator1Builder sffDataPlaneLocator1Builder = new SffDataPlaneLocator1Builder();
                    sffDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());
                    sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator1.class, sffDataPlaneLocator1Builder.build());

                    SffDataPlaneLocator2Builder sffDataPlaneLocatorOptionsBuilder = new SffDataPlaneLocator2Builder();
                    sffDataPlaneLocatorOptionsBuilder.setOvsOptions(buildOvsOptionsFromTerminationPoint(terminationPointAugmentation));
                    sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator2.class, sffDataPlaneLocatorOptionsBuilder.build());

                    sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());
                }
            }

        } catch (NullPointerException e) {
            LOG.debug("Cannot build SffDataPlaneLocatorList, Termination Point List is empty (null)");
        }

        return sffDataPlaneLocatorList;
    }

    /**
     * Builds a (SFF) DataPlaneLocator object based on OvsdbTerminationPointAugmentation.
     * <p/>
     * The terminationPoint argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param terminationPoint OvsdbTerminationPointAugmentation Object
     * @return (SFF) DataPlaneLocator object
     */
    private static DataPlaneLocator buildDataPlaneLocatorFromTerminationPoint(OvsdbTerminationPointAugmentation terminationPoint) {
        Preconditions.checkNotNull(terminationPoint);

        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        try {
            //set ip:port locator
            IpBuilder ipBuilder = new IpBuilder();

            List<Options> options = terminationPoint.getOptions();
            for (Options option : options) {
                switch (option.getOption()) {
                    case SfcOvsUtil.OVSDB_OPTION_LOCAL_IP:
                        IpAddress localIp = SfcOvsUtil.convertStringToIpAddress(option.getValue());
                        ipBuilder.setIp(localIp);
                        break;

                    case SfcOvsUtil.OVSDB_OPTION_SRC_PORT:
                        PortNumber srcPort = new PortNumber(Integer.parseInt(option.getValue()));
                        ipBuilder.setPort(srcPort);
                        break;
                }
            }
            dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        } catch (NullPointerException e) {
            LOG.warn("Cannot determine OVS TerminationPoint locator type: {}.", terminationPoint.getName());
        }

        //set transport type
        try {
            if (terminationPoint.getInterfaceType().isAssignableFrom(InterfaceTypeVxlan.class)) {
                dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
            } else {
                dataPlaneLocatorBuilder.setTransport(Other.class);
            }
        } catch (NullPointerException e) {
            LOG.warn("Cannot determine OVS TerminationPoint transport type: {}.", terminationPoint.getName());

            //TODO: remove once MDSAL OVSDB will not require interface type to be specified
            LOG.warn("Falling back to transport type: Other");
            dataPlaneLocatorBuilder.setTransport(Other.class);
        }

        return dataPlaneLocatorBuilder.build();
    }

    /**
     * Builds an OvsOptions object based on OvsdbTerminationPointAugmentation Options list.
     * <p/>
     * The terminationPoint argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param terminationPoint OvsdbTerminationPointAugmentation Object
     * @return OvsOptions object
     */
    private static OvsOptions buildOvsOptionsFromTerminationPoint(OvsdbTerminationPointAugmentation terminationPoint) {
        Preconditions.checkNotNull(terminationPoint);
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();

        try {
            List<Options> options = terminationPoint.getOptions();
            for (Options option : options) {
                switch (option.getOption()) {
                    case SfcOvsUtil.OVSDB_OPTION_LOCAL_IP:
                        IpAddress localIp = SfcOvsUtil.convertStringToIpAddress(option.getValue());
                        ovsOptionsBuilder.setLocalIp(localIp);
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_REMOTE_IP:
                        IpAddress remoteIp = SfcOvsUtil.convertStringToIpAddress(option.getValue());
                        ovsOptionsBuilder.setRemoteIp(remoteIp);
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_SRC_PORT:
                        PortNumber srcPort = new PortNumber(Integer.parseInt(option.getValue()));
                        ovsOptionsBuilder.setSrcPort(srcPort);
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_DST_PORT:
                        PortNumber dstPort = new PortNumber(Integer.parseInt(option.getValue()));
                        ovsOptionsBuilder.setDstPort(dstPort);
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_KEY:
                        ovsOptionsBuilder.setKey(option.getValue());
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_NSP:
                        ovsOptionsBuilder.setNsp(option.getValue());
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_NSI:
                        ovsOptionsBuilder.setNsi(option.getValue());
                        break;
                }
            }
        } catch (NullPointerException e) {
            LOG.warn("Cannot gather OVS TerminationPoint Options: {}.", terminationPoint.getName());
        }

        return ovsOptionsBuilder.build();
    }

    /**
     * Returns a Service Function Forwarder name. The name is based
     * on OVS node-id.
     * The node argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param node Node Object
     * @return Service Function Forwarder name
     */
    public static String getServiceForwarderNameFromNode(Node node) {
        Preconditions.checkNotNull(node);

        return node.getNodeId().getValue();
    }
}

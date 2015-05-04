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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
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

    private static final String OPENFLOW_PREFIX = "openflow:";
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsToSffMappingAPI.class);

    /**
     * Returns an Service Function Forwarder object which can be stored
     * in DataStore. The returned object is built on basis of OVS Bridge & OVS Termination Points.
     * The node argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param node Node Object
     * @return ServiceFunctionForwarder Object
     */
    public static ServiceFunctionForwarder buildServiceFunctionForwarderFromNode(Node node) {
        Preconditions.checkNotNull(node, "Cannot build Service Function Forwarder: OVS Node does not exist!");

        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation != null){

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
            ovsBridgeBuilder.setOpenflowNodeId(getOvsBridgeOpenflowNodeId(ovsdbBridgeAugmentation.getDatapathId()));
            ovsServiceForwarder2Augmentation.setOvsBridge(ovsBridgeBuilder.build());
            serviceFunctionForwarderBuilder.addAugmentation(ServiceFunctionForwarder2.class, ovsServiceForwarder2Augmentation.build());

            //add SFF DP locators list to SFF
            serviceFunctionForwarderBuilder.setSffDataPlaneLocator(
                    buildSffDataPlaneLocatorList(ovsdbBridgeAugmentation, node.getTerminationPoint()));

            return serviceFunctionForwarderBuilder.build();

        } else {
            LOG.debug("Not building Service Function Forwarder: passed Node parameter does not contain OvsdbBridgeAugmentation");
            return null;
        }
    }

    /**
     * Builds a SffDataPlaneLocator List based on OvsdbBridgeAugmentation and TerminationPoint List.
     * TerminationPoints in the list must be augmented with OvsdbTerminationPointAugmentation.
     * <p/>
     * The ovsdbBridge argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param ovsdbBridge          OvsdbBridgeAugmentation Object
     * @param terminationPointList List<TerminationPoint>
     * @return List<SffDataPlaneLocator> (filled or empty, but not null)
     */
    private static List<SffDataPlaneLocator> buildSffDataPlaneLocatorList(OvsdbBridgeAugmentation ovsdbBridge,
                                                                          List<TerminationPoint> terminationPointList) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot build SffDataPlaneLocator: OVS Bridge does not exist!");
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();

        if (terminationPointList != null){
            //OVS bridge will be the same for all DP locators
            OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
            ovsBridgeBuilder.setBridgeName(ovsdbBridge.getBridgeName().getValue());
            ovsBridgeBuilder.setUuid(ovsdbBridge.getBridgeUuid());
            ovsBridgeBuilder.setOpenflowNodeId(getOvsBridgeOpenflowNodeId(ovsdbBridge.getDatapathId()));

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

        } else {
            LOG.debug("Not building SffDataPlaneLocatorList, Termination Point List is null.");
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

        if (terminationPoint.getOptions() != null) {
            //set ip:port locator
            IpBuilder ipBuilder = new IpBuilder();

            for (Options option : terminationPoint.getOptions()) {
                switch (option.getOption()) {
                    case SfcOvsUtil.OVSDB_OPTION_LOCAL_IP:
                        IpAddress localIp = SfcOvsUtil.convertStringToIpAddress(option.getValue());
                        ipBuilder.setIp(localIp);
                        break;
                }
            }
            dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        } else  {
            LOG.debug("Not building OVS TerminationPoint ({}) locator type. TerminationPoint options are null", terminationPoint.getName());
        }

        //set transport type
        if (terminationPoint.getInterfaceType() != null) {
            if (terminationPoint.getInterfaceType().isAssignableFrom(InterfaceTypeVxlan.class)) {
                dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
            } else {
                dataPlaneLocatorBuilder.setTransport(Other.class);
            }

        } else {
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

        if (terminationPoint.getOptions() != null) {
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
        } else {
            LOG.debug("Not building OVS TerminationPoint Options: {}. TerminationPoint options are null.", terminationPoint.getName());
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

    /**
     * Returns IP address of OvsBridge specified in BridgeOtherConfigs.
     * The IP address is stored with key "local_ip".
     * <p/>
     * Parameter ovsdbBridge and ovsdbBridge.getBridgeOtherConfigs()
     * must be not null otherwise NullPointerException will be raised.
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return IpAddress object or null when no "local_ip" is present.
     */
    public static IpAddress getOvsBridgeLocalIp(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "OvsdbBridgeAugmentation is null, cannot get Bridge Local IP");
        Preconditions.checkNotNull(ovsdbBridge.getBridgeOtherConfigs(),
                "OvsdbBridgeAugmentation OtherConfig list is null, cannot get Bridge Local IP");

        List<BridgeOtherConfigs> otherConfigsList = ovsdbBridge.getBridgeOtherConfigs();

        for (BridgeOtherConfigs otherConfig : otherConfigsList) {
            if (otherConfig.getBridgeOtherConfigKey().equals(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP)) {
                return SfcOvsUtil.convertStringToIpAddress(otherConfig.getBridgeOtherConfigValue());
            }
        }
        return null;
    }

    /**
     * Returns OpenflowNodeId of OvsBridge specified by DatapathId.
     * OpenflowNodeId = openflow:hexTodec(DatapathId)
     * <p/>
     * Parameter DatapathId
     * must be not null otherwise NullPointerException will be raised.
     *
     * @param datapathId DatapathId
     * @return String OpenflowNodeId
     */
    private static String getOvsBridgeOpenflowNodeId(DatapathId datapathId) {
        Preconditions.checkNotNull(datapathId, "DatapathId is null, cannot get Bridge OpenflowNode Id");

        Long datapathIdDec = Long.parseLong(datapathId.getValue().replaceAll(":", ""), 16);
        return OPENFLOW_PREFIX + datapathIdDec.toString();
    }
}

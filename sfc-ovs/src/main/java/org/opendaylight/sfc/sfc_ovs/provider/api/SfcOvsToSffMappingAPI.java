/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.api;

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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.OtherBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
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

import java.util.ArrayList;
import java.util.List;

/**
 * This class has the APIs to map OVS Bridge to SFC Service Function Forwarder
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see SfcOvsToSffMappingAPI
 * <p>
 * @since 2015-03-10
 */
public class SfcOvsToSffMappingAPI {

    private static final String OPENFLOW_PREFIX = "openflow:";
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsToSffMappingAPI.class);

    /**
     * Returns an Service Function Forwarder object which can be stored
     * in DataStore. The returned object is built on basis of OVS Bridge &amp; OVS Termination Points.
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
            SffOvsNodeAugmentationBuilder sffOvsNodeAugmentationBuilder = new SffOvsNodeAugmentationBuilder();
            OvsNodeBuilder ovsNodeBuilder = new OvsNodeBuilder();
            ovsNodeBuilder.setNodeId(ovsdbBridgeAugmentation.getManagedBy());
            sffOvsNodeAugmentationBuilder.setOvsNode(ovsNodeBuilder.build());
            serviceFunctionForwarderBuilder.addAugmentation(SffOvsNodeAugmentation.class, sffOvsNodeAugmentationBuilder.build());

            //add OVS Bridge to SFF
            SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
            OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
            ovsBridgeBuilder.setBridgeName(ovsdbBridgeAugmentation.getBridgeName().getValue());
            ovsBridgeBuilder.setUuid(ovsdbBridgeAugmentation.getBridgeUuid());
            DatapathId datapathId = ovsdbBridgeAugmentation.getDatapathId();
            if (datapathId != null) {
                ovsBridgeBuilder.setOpenflowNodeId(getOvsBridgeOpenflowNodeId(ovsdbBridgeAugmentation.getDatapathId()));
            }
            sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());
            serviceFunctionForwarderBuilder.addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build());


            //add SFF DP locators list to SFF
            List<SffDataPlaneLocator> sffDataPlaneLocatorList =
                    buildSffDataPlaneLocatorList(ovsdbBridgeAugmentation, node.getTerminationPoint());
            if (!sffDataPlaneLocatorList.isEmpty()) {
                serviceFunctionForwarderBuilder.setSffDataPlaneLocator(sffDataPlaneLocatorList);
            }
            return serviceFunctionForwarderBuilder.build();

        } else {
            LOG.debug("Not building Service Function Forwarder: passed Node parameter does not contain OvsdbBridgeAugmentation");
            return null;
        }
    }

    /**
     * Builds a SffDataPlaneLocator List based on OvsdbBridgeAugmentation and TerminationPoint List.
     * TerminationPoints in the list must be augmented with OvsdbTerminationPointAugmentation.
     * <p>
     * The ovsdbBridge argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param ovsdbBridge          OvsdbBridgeAugmentation Object
     * @param terminationPointList List&lt;TerminationPoint&gt;
     * @return List&lt;SffDataPlaneLocator&gt; (filled or empty, but not null)
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
                    SffOvsLocatorBridgeAugmentationBuilder sffOvsLocatorBridgeAugmentationBuilder = new SffOvsLocatorBridgeAugmentationBuilder();
                    sffOvsLocatorBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder.build());
                    sffDataPlaneLocatorBuilder.addAugmentation(SffOvsLocatorBridgeAugmentation.class, sffOvsLocatorBridgeAugmentationBuilder.build());

                    SffOvsLocatorOptionsAugmentationBuilder sffDataPlaneLocatorOptionsBuilder = new SffOvsLocatorOptionsAugmentationBuilder();
                    OvsOptions ovsOptions =  buildOvsOptionsFromTerminationPoint(terminationPointAugmentation);
                    if (ovsOptions != null) {
                        sffDataPlaneLocatorOptionsBuilder.setOvsOptions(buildOvsOptionsFromTerminationPoint(terminationPointAugmentation));
                        sffDataPlaneLocatorBuilder.addAugmentation(SffOvsLocatorOptionsAugmentation.class, sffDataPlaneLocatorOptionsBuilder.build());
                    }

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
     * <p>
     * The terminationPoint argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param terminationPoint OvsdbTerminationPointAugmentation Object
     * @return (SFF) DataPlaneLocator object
     */
    private static DataPlaneLocator buildDataPlaneLocatorFromTerminationPoint(OvsdbTerminationPointAugmentation terminationPoint) {
        Preconditions.checkNotNull(terminationPoint);

        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        // Default if nothing is specified
        OtherBuilder otherBuilder = new OtherBuilder();
        otherBuilder.setOtherName("Other");
        dataPlaneLocatorBuilder.setLocatorType(otherBuilder.build());

        if (terminationPoint.getOptions() != null) {
            //set ip:port locator
            IpBuilder ipBuilder = new IpBuilder();

            for (Options option : terminationPoint.getOptions()) {
                switch (option.getOption()) {
                    case SfcOvsUtil.OVSDB_OPTION_LOCAL_IP:
                        IpAddress localIp = SfcOvsUtil.convertStringToIpAddress(option.getValue());
                        ipBuilder.setIp(localIp);
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_REMOTE_IP:
                        IpAddress remotelIp = SfcOvsUtil.convertStringToIpAddress(option.getValue());
                        ipBuilder.setIp(remotelIp);
                        break;
                }
            }
            dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        } else  {
            LOG.debug("Not building OVS TerminationPoint ({}) locator type. TerminationPoint options are null", terminationPoint.getName());
        }

        //set transport type
        dataPlaneLocatorBuilder.setTransport(Other.class);
        if ((terminationPoint.getInterfaceType() != null) &&
                terminationPoint.getInterfaceType().isAssignableFrom(InterfaceTypeVxlan.class)) {
                dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
        }
        return dataPlaneLocatorBuilder.build();
    }

    /**
     * Builds an OvsOptions object based on OvsdbTerminationPointAugmentation Options list.
     * <p>
     * The terminationPoint argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param terminationPoint OvsdbTerminationPointAugmentation Object
     * @return OvsOptions object
     */
    private static OvsOptions buildOvsOptionsFromTerminationPoint(OvsdbTerminationPointAugmentation terminationPoint) {
        Preconditions.checkNotNull(terminationPoint);
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();
        boolean options = false;

        if (terminationPoint.getOptions() != null) {
            List<Options> optionsList = terminationPoint.getOptions();
            for (Options option : optionsList) {
                switch (option.getOption()) {
                    case SfcOvsUtil.OVSDB_OPTION_LOCAL_IP:
                        ovsOptionsBuilder.setLocalIp(option.getValue());
                        options = true;
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_REMOTE_IP:
                        ovsOptionsBuilder.setRemoteIp(option.getValue());
                        options = true;
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_DST_PORT:
                        ovsOptionsBuilder.setDstPort(option.getValue());
                        options = true;
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_KEY:
                        ovsOptionsBuilder.setKey(option.getValue());
                        options = true;
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_NSP:
                        ovsOptionsBuilder.setNsp(option.getValue());
                        options = true;
                        break;
                    case SfcOvsUtil.OVSDB_OPTION_NSI:
                        ovsOptionsBuilder.setNsi(option.getValue());
                        options = true;
                        break;
                }
            }
        } else {
            LOG.debug("Not building OVS TerminationPoint Options: {}. TerminationPoint options are null.", terminationPoint.getName());
        }
        if (options) {
            return ovsOptionsBuilder.build();
        } else {
            return null;
        }
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

        LOG.error("Node id is: {}", node.getNodeId().getValue());
        return node.getNodeId().getValue();
    }

    /**
     * Returns IP address of OvsBridge specified in BridgeOtherConfigs.
     * The IP address is stored with key "local_ip".
     * <p>
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

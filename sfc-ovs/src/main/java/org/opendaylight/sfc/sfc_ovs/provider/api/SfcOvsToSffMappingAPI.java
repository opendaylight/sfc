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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
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

            //add SFF DP locators list to SFF
            serviceFunctionForwarderBuilder.setSffDataPlaneLocator(
                    buildSffDataPlaneLocatorList(ovsdbBridgeAugmentation, node.getTerminationPoint()));

            return serviceFunctionForwarderBuilder.build();

        } catch (NullPointerException e) {
            LOG.debug("Cannot build Service Function Forwarder: OVS Bridge does not exist!");
            return null;
        }
    }

    /**
     * Builds a SffDataPlaneLocator List based on OvsdbBridgeAugmentation and TerminationPoint List.
     * TerminationPoints in the list must be augmented with OvsdbTerminationPointAugmentation.
     *
     * The ovsdbBridge argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * The terminationPointList must be not null othewrise
     * empty SffDataPlaneLocator List will be returned.
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation Object
     * @param terminationPointList List<TerminationPoint>
     * @return List<SffDataPlaneLocator> (filled or empty, but not null)
     */
    private static List<SffDataPlaneLocator> buildSffDataPlaneLocatorList(OvsdbBridgeAugmentation ovsdbBridge,
                                                                          List<TerminationPoint> terminationPointList) {
        Preconditions.checkNotNull(ovsdbBridge);
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
                    sffDataPlaneLocatorBuilder.setDataPlaneLocator(buildDataPlaneLocatorFromTerminationPoint(terminationPointAugmentation));

                    SffDataPlaneLocator1Builder sffDataPlaneLocator1Builder = new SffDataPlaneLocator1Builder();
                    sffDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());
                    sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator1.class, sffDataPlaneLocator1Builder.build());

                    sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());
                }
            }

        } catch(NullPointerException e) {
            LOG.debug("Cannot build SffDataPlaneLocatorList, Termination Point List is empty (null)");
        }

        return sffDataPlaneLocatorList;
    }

    /**
     * Builds a (SFF) DataPlaneLocator object based on OvsdbTerminationPointAugmentation.
     *
     * The terminationPoint argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param terminationPoint OvsdbTerminationPointAugmentation Object
     * @return (SFF) DataPlaneLocator object
     */
    private static DataPlaneLocator buildDataPlaneLocatorFromTerminationPoint(OvsdbTerminationPointAugmentation terminationPoint) {
        Preconditions.checkNotNull(terminationPoint);

        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();

        if (terminationPoint.getInterfaceType().isAssignableFrom(InterfaceTypeVxlan.class)) {
            dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
        } else {
            dataPlaneLocatorBuilder.setTransport(Other.class);
        }

        return dataPlaneLocatorBuilder.build();
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

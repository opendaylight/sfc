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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private static ServiceFunctionForwarder buildServiceFunctionForwarderFromNode(Node node) {
        Preconditions.checkNotNull(node, "Cannot build Service Function Forwarder: OVS node does not exist!");

        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        Preconditions.checkNotNull(ovsdbBridgeAugmentation, "Cannot build Service Function Forwader: OVS bridge does not exist!");

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(getServiceForwarderNameFromOvsdbBridge(ovsdbBridgeAugmentation));

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(sffDataPlaneLocatorList);

        //OVS bridge will be the same for all DP locators
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(ovsdbBridgeAugmentation.getBridgeName().getValue());
        ovsBridgeBuilder.setUuid(ovsdbBridgeAugmentation.getBridgeUuid());

        //fill SffDataPlaneLocatorList with DP locators
        List<TerminationPoint> terminationPointList = node.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPointList) {
            OvsdbTerminationPointAugmentation terminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);

            SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
            sffDataPlaneLocatorBuilder.setName(terminationPointAugmentation.getName());

            SffDataPlaneLocator1Builder sffDataPlaneLocator1Builder = new SffDataPlaneLocator1Builder();
            sffDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());
            sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator1.class, sffDataPlaneLocator1Builder.build());

            sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());
        }

        return serviceFunctionForwarderBuilder.build();
    }

    /**
     * Returns a Service Function Forwarder name. The name is based
     * on OVS Bridge Name and Uuid.
     * The ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param ovsdbBridgeAugmentation ovsdbBridgeAugmentation Object
     * @return Service Function Forwarder name
     */
    public static String getServiceForwarderNameFromOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        Preconditions.checkNotNull(ovsdbBridgeAugmentation);

        OvsdbBridgeName bridgeName = ovsdbBridgeAugmentation.getBridgeName();
        Uuid uuid = ovsdbBridgeAugmentation.getBridgeUuid();

        return bridgeName.getValue() + " (" + uuid.getValue() + ")";
    }

    /**
     * Returns a Service Function Forwarder object which is build from OVSDB topology Node.
     *
     * The terminationPointIID argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param terminationPointIID InstanceIdentifier<OvsdbTerminationPointAugmentation>
     * @return Service Function Forwarder Object
     */
    public static ServiceFunctionForwarder getServiceForwarderForOvsdbTerminationPoint(InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointIID){
        Preconditions.checkNotNull(terminationPointIID);

        InstanceIdentifier<Node> nodeIID = terminationPointIID.firstIdentifierOf(Node.class);
        if (nodeIID != null) {
            Node node = SfcDataStoreAPI.readTransactionAPI(nodeIID, LogicalDatastoreType.OPERATIONAL);
            return buildServiceFunctionForwarderFromNode(node);
        }

        return null;
    }
}

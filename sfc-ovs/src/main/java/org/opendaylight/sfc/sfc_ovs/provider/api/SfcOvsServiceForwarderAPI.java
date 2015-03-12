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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map OVS Bridge to SFC Service Function Forwarder
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsServiceForwarderAPI
 * <p/>
 * <p/>
 * <p/>
 * @since 2015-03-10
 */
public class SfcOvsServiceForwarderAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsServiceForwarderAPI.class);


    /**
     * Returns an Service Function Forwarder object which can be stored
     * in DataStore. The returned object is built on basis of OVS Bridge.
     * The ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param ovsdbBridgeAugmentation ovsdbBridgeAugmentation Object
     * @return ServiceFunctionForwarder Object
     */
    public static ServiceFunctionForwarder buildServiceForwarderFromOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        Preconditions.checkNotNull(ovsdbBridgeAugmentation);

        OvsdbBridgeName bridgeName = ovsdbBridgeAugmentation.getBridgeName();
        Uuid uuid = ovsdbBridgeAugmentation.getBridgeUuid();

        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(bridgeName.getValue());
        ovsBridgeBuilder.setUuid(uuid);

        SffDataPlaneLocator1Builder sffDataPlaneLocator1Builder = new SffDataPlaneLocator1Builder();
        sffDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());

        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        //TODO: should be replaced once OVS interface name will be available
        sffDataPlaneLocatorBuilder.setName("interfaceName");
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator1.class, sffDataPlaneLocator1Builder.build());

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(uuid.getValue());
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(sffDataPlaneLocatorList);

        return serviceFunctionForwarderBuilder.build();
    }

    /**
     * Returns an Service Function Forwarder name. The name is based
     * on OVS Bridge Uuid.
     * The ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param ovsdbBridgeAugmentation ovsdbBridgeAugmentation Object
     * @return Service Function Forwarder name
     */
    public static String getServiceForwarderNameFromOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        Preconditions.checkNotNull(ovsdbBridgeAugmentation);

        Uuid uuid = ovsdbBridgeAugmentation.getBridgeUuid();
        return uuid.getValue();
    }

    public static OvsdbBridgeAugmentation getOvsdbBridgeFromServiceForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {
        Preconditions.checkNotNull(serviceFunctionForwarder);

        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        //We can use name provided by user - it does not have to be UUID. UUID will be created by OVSDB itself
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(serviceFunctionForwarder.getName()));


        return ovsdbBridgeAugmentationBuilder.build();
    }

    public static OvsdbTerminationPointAugmentation getTerminationPointFromSffDatePlaneLocator(SffDataPlaneLocator sffDataPlaneLocator) {
        Preconditions.checkNotNull(sffDataPlaneLocator);

        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder
                = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setName(sffDataPlaneLocator.getName());

        ovsdbTerminationPointAugmentationBuilder = getTerminationPointBuilderFromDataPlaneLocator(
                sffDataPlaneLocator.getDataPlaneLocator(), ovsdbTerminationPointAugmentationBuilder);

        return ovsdbTerminationPointAugmentationBuilder.build();
    }

    private static OvsdbTerminationPointAugmentationBuilder getTerminationPointBuilderFromDataPlaneLocator(
            DataPlaneLocator dataPlaneLocator, OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder) {
        Preconditions.checkNotNull(dataPlaneLocator);
        Preconditions.checkNotNull(ovsdbTerminationPointAugmentationBuilder);

        if (dataPlaneLocator.getTransport() == VxlanGpe.class) {
            ovsdbTerminationPointAugmentationBuilder.setInterfaceType(InterfaceTypeVxlan.class);
        }

        return ovsdbTerminationPointAugmentationBuilder;
    }


}

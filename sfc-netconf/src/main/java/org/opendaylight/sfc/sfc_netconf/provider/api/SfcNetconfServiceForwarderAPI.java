/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider.api;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class has the APIs to map Netconf to SFC Service Function Forwarder
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfServiceForwarderAPI
 * <p/>
 * <p/>
 * <p/>
 * @since 2015-03-10
 */
public class SfcNetconfServiceForwarderAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfServiceForwarderAPI.class);


    /**
     * Returns an Service Function Forwarder object which can be stored
     * in DataStore. The returned object is built on basis of OVS Bridge.
     * The ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param nnode Netconf node Object
     * @return ServiceFunctionForwarder Object
     */
    public static ServiceFunctionForwarder buildServiceForwarderFromNetonf(NetconfNode nnode) {
        Preconditions.checkNotNull(nnode);


        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        //TODO: should be replaced once OVS interface name will be available

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(nnode.getHost().toString());
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(sffDataPlaneLocatorList);

        return serviceFunctionForwarderBuilder.build();
    }

}

/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider.api;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;

/**
 * SfcNetconfServiceForwarderAPITest Tester.
 *
 * @author Hongli Chen (honglix.chen@intel.com)
 * @version 1.0
 * @since 2015-10-29
 */

public class SfcNetconfServiceForwarderAPITest extends AbstractDataBrokerTest {

    @Test
    public void testBuildServiceForwarderFromNetconf() {
        SffName sffName = new SffName("sff1");
        IpAddress ipAddress = new IpAddress(new Ipv4Address("192.168.1.2"));
        Host host = new Host(ipAddress);
        NetconfNode nnode = new NetconfNodeBuilder().setHost(host).build();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(sffName);
        serviceFunctionForwarderBuilder.setIpMgmtAddress(nnode.getHost().getIpAddress());
        ServiceFunctionForwarder sff = serviceFunctionForwarderBuilder.build();

        ServiceFunctionForwarder serviceFunctionForwarder = SfcNetconfServiceForwarderAPI.buildServiceForwarderFromNetconf("sff1", nnode);
        assertEquals("Must be equal", serviceFunctionForwarder, sff);
    }

}

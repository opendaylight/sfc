/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.netconf.provider.api;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev180703.NetconfNode;

/*
 * This class has the APIs to map Netconf to SFC Service Function Forwarder
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.netconf.provider.api.SfcNetconfServiceForwarderAPI
 * @since 2015-03-10
 */
public class SfcNetconfServiceForwarderAPI {

    protected SfcNetconfServiceForwarderAPI() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an Service Function Forwarder object which can be stored in
     * DataStore. The returned object is built on basis of OVS Bridge. The
     * ovsdbBridgeAugmentation argument must be not null otherwise
     * NullPointerException will be raised.
     *
     * @param nodeName
     *            SFF name
     *
     * @param nnode
     *            Netconf node Object
     * @return ServiceFunctionForwarder Object
     */
    public static ServiceFunctionForwarder buildServiceForwarderFromNetconf(String nodeName, NetconfNode nnode) {
        Preconditions.checkNotNull(nnode);

        SffName sffName = new SffName(nodeName);

        // TODO: should be replaced once OVS interface name will be available

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(sffName);
        IpAddress ipAddress = new IpAddress(nnode.getHost().getIpAddress().getValue());
        serviceFunctionForwarderBuilder.setIpMgmtAddress(ipAddress);
        return serviceFunctionForwarderBuilder.build();
    }
}

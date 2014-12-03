/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;

import java.util.ArrayList;
import java.util.List;

public class SimpleTestEntityBuilder {

    static ServiceFunction buildServiceFunction(String name, Class<? extends ServiceFunctionTypeIdentity> type,
                                                IpAddress ipMgmtAddress, SfDataPlaneLocator sfDataPlaneLocator,
                                                Boolean nshAware) {

        List<SfDataPlaneLocator> dsfDataPlaneLocatorList = new ArrayList<>();
        dsfDataPlaneLocatorList.add(sfDataPlaneLocator);

        return buildServiceFunction(name, type, ipMgmtAddress, dsfDataPlaneLocatorList, nshAware);
    }

    static ServiceFunction buildServiceFunction(String name, Class<? extends ServiceFunctionTypeIdentity> type,
                                                IpAddress ipMgmtAddress, List<SfDataPlaneLocator> dsfDataPlaneLocatorList,
                                                Boolean nshAware) {

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(name).setKey(new ServiceFunctionKey(name))
                .setType(type)
                .setIpMgmtAddress(ipMgmtAddress)
                .setSfDataPlaneLocator(dsfDataPlaneLocatorList)
                .setNshAware(nshAware);

        return sfBuilder.build();
    }

    static Ip buildLocatorTypeIp(IpAddress ipAddress, int port) {
        PortNumber portNumber = new PortNumber(port);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress)
                .setPort(portNumber);

        return ipBuilder.build();
    }

    static SfDataPlaneLocator buildSfDataPlaneLocator(String name, LocatorType locatorType,
                                                      String serviseFunctionForwarder,
                                                      Class<? extends SlTransportType> transport) {

        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder
                .setName(name).setKey(new SfDataPlaneLocatorKey(name))
                .setLocatorType(locatorType)
                .setServiceFunctionForwarder(serviseFunctionForwarder)
                .setTransport(transport);

        return locatorBuilder.build();
    }

    static ServiceFunctionForwarder buildServiceFunctionForwarder(String name,
                                                                  List<SffDataPlaneLocator> sffDataplaneLocatorList,
                                                                  List<ServiceFunctionDictionary> dictionaryList,
                                                                  String classifier) {
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setName(name).setKey(new ServiceFunctionForwarderKey(name))
                .setClassifier(classifier)
                .setSffDataPlaneLocator(sffDataplaneLocatorList)
                .setServiceFunctionDictionary(dictionaryList)
                .setServiceNode(null);

        return sffBuilder.build();
    }
}

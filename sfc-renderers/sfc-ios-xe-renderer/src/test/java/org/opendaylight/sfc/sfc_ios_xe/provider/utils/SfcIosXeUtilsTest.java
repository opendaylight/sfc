/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.utils;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcProviderServiceForwarderAPI.class})
public class SfcIosXeUtilsTest {

    private final String ipv4Address = "10.0.0.1";
    private final String forwarderName = "forwarder";

    @Test
    public void createLocalForwarder_nullIp() {
        assertNull(SfcIosXeUtils.createLocalForwarder(null));
    }

    @Test
    public void createLocalForwarder() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address(ipv4Address));
        ServiceFunctionForwarder result = SfcIosXeUtils.createLocalForwarder(ipAddress);
        assertNotNull(result);
        assertEquals(result.getLocal().getIp().getAddress().getValue(), ipv4Address);
    }

    @Test
    public void createRemoteForwarder_noForwarderFound() {
        SffName forwarderSff = new SffName(forwarderName);

        PowerMockito.stub(PowerMockito.method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder"))
                .toReturn(null);

        ServiceFfName result = SfcIosXeUtils.createRemoteForwarder(forwarderSff);
        assertNull(result);
    }

    @Test
    public void createRemoteForwarder_noIpLocatorType() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        List<SffDataPlaneLocator> sffDataPlaneLocators = new ArrayList<>();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        MacBuilder macBuilder = new MacBuilder();
        dataPlaneLocatorBuilder.setLocatorType(macBuilder.build());
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        sffDataPlaneLocators.add(sffDataPlaneLocatorBuilder.build());
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(sffDataPlaneLocators);

        SffName forwarderSff = new SffName(forwarderName);

        PowerMockito.stub(PowerMockito.method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder"))
                .toReturn(serviceFunctionForwarderBuilder.build());

        ServiceFfName result = SfcIosXeUtils.createRemoteForwarder(forwarderSff);
        assertNull(result);
    }

    @Test
    public void createRemoteForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        List<SffDataPlaneLocator> sffDataPlaneLocators = new ArrayList<>();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address(ipv4Address)))
                .setPort(new PortNumber(100));
        dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());
        sffDataPlaneLocators.add(sffDataPlaneLocatorBuilder.build());
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(sffDataPlaneLocators);

        SffName forwarderSff = new SffName(forwarderName);

        PowerMockito.stub(PowerMockito.method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder"))
                .toReturn(serviceFunctionForwarderBuilder.build());

        ServiceFfName result = SfcIosXeUtils.createRemoteForwarder(forwarderSff);
        assertNotNull(result);
        assertEquals(result.getIp().getAddress().getValue(), ipv4Address);
    }

    @Test
    public void getDplWithIpLocatorType_noIpLocator() {
        List<SfDataPlaneLocator> dataPlaneLocators = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder macLocatorTypeBuilder =
                new org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder();
        macLocatorTypeBuilder.setLocatorType(new MacBuilder().build());
        dataPlaneLocators.add(macLocatorTypeBuilder.build());
        SfDataPlaneLocator result = SfcIosXeUtils.getDplWithIpLocatorType(dataPlaneLocators);
        assertNull(result);
    }

    @Test
    public void getDplWithIpLocatorType() {
        List<SfDataPlaneLocator> dataPlaneLocators = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder macLocatorTypeBuilder =
                new org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder();
        macLocatorTypeBuilder.setLocatorType(new MacBuilder().build());
        SfDataPlaneLocator macLocatorDpl = macLocatorTypeBuilder.build();
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder ipLocatorTypeBuilder =
                new org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder();
        ipLocatorTypeBuilder.setLocatorType(new IpBuilder().build());
        SfDataPlaneLocator ipLocatorDpl = ipLocatorTypeBuilder.build();
        dataPlaneLocators.add(macLocatorDpl);
        dataPlaneLocators.add(ipLocatorDpl);
        SfDataPlaneLocator result = SfcIosXeUtils.getDplWithIpLocatorType(dataPlaneLocators);
        assertNotNull(result);
    }

    @Test
    public void createLocalSffIid() {
        InstanceIdentifier<Local> result = SfcIosXeUtils.createLocalSffIid();
        // Test IID
        InstanceIdentifier<Local> testIid = InstanceIdentifier.builder(Native.class).child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(Local.class).build();
        assertEquals(testIid, result);
    }

    @Test
    public void createRemoteSffIid() {
        ServiceFfNameBuilder serviceFfNameBuilder = new ServiceFfNameBuilder();
        serviceFfNameBuilder.setName(forwarderName);
        InstanceIdentifier<ServiceFfName> firstResult = SfcIosXeUtils.createRemoteSffIid(serviceFfNameBuilder.build());
        InstanceIdentifier<ServiceFfName> secondResult = SfcIosXeUtils.createRemoteSffIid(new SffName(forwarderName));
        // Test IID
        InstanceIdentifier<ServiceFfName> testIid = InstanceIdentifier.builder(Native.class).child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(ServiceFfName.class, new ServiceFfNameKey(forwarderName)).build();
        assertEquals(firstResult, testIid);
        assertEquals(secondResult, testIid);
        assertEquals(firstResult, secondResult);
    }

    @Test
    public void createSfIid() {
        String functionName = "function";
        InstanceIdentifier<ServiceFunction> result = SfcIosXeUtils.createSfIid(new ServiceFunctionKey(functionName));
        // Test IID
        InstanceIdentifier<ServiceFunction> testIid = InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(ServiceFunction.class, new ServiceFunctionKey(functionName))
                .build();
        assertEquals(testIid, result);
    }

    @Test
    public void createServicePathIid() {
        Long servicePathKey = 10L;
        InstanceIdentifier<ServicePath> result = SfcIosXeUtils.createServicePathIid(new ServicePathKey(servicePathKey));
        // Test IID
        InstanceIdentifier<ServicePath> testIid = InstanceIdentifier.builder(Native.class).child(ServiceChain.class)
                .child(ServicePath.class, new ServicePathKey(servicePathKey)).build();
        assertEquals(testIid, result);
    }
}

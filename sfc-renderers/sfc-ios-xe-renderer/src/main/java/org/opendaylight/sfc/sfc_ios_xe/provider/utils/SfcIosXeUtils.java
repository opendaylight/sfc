/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.utils;

import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.config.service.chain.grouping.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class SfcIosXeUtils {

    private static final String REMOTE = "Remote forwarder: ";

    /**
     * Creates local service function forwarder {@link ServiceFunctionForwarder} with respective IP address. Local
     * forwarder does not contain name, only IP address. Supports only IPv4
     *
     * @param ipAddress which will be set on service function forwarder
     * @return service function forwarder object for ios-xe device which contains {@link Local} SFF. Null if parameter
     * is not an IPv4 address
     */
    public static org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder createLocalForwarder(
            IpAddress ipAddress) {
        if (ipAddress != null && ipAddress.getIpv4Address() != null) {
            // Ip address
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setAddress(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address(ipAddress
                    .getIpv4Address().getValue()));
            LocalBuilder localBuilder = new LocalBuilder();
            localBuilder.setIp(ipBuilder.build());

            ServiceFunctionForwarderBuilder localForwarderBuilder = new ServiceFunctionForwarderBuilder();
            localForwarderBuilder.setLocal(localBuilder.build());
            return localForwarderBuilder.build();
        }
        return null;
    }

    /**
     * Creates remote service function forwarder (ios-xe SFC entity) {@link ServiceFfName}. Using name of the original
     * entity, whole SFF configuration is read from ODL CONF data store. This configuration is used to
     * build remote service function forwarder
     *
     * @param sffName name of the service function forwarder
     * @return remote SFF (ios-xe SFC entity), null if SFF does not contain data plane locator with IP locator type
     */
    public static ServiceFfName createRemoteForwarder(SffName sffName) {
        // Actually, local forwarder is without name. As a parameter, use SffName of appropriate sfc forwarder
        ServiceFunctionForwarder sfcForwarder = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        if (sfcForwarder == null) {
            return null;
        }
        Ipv4Address sffIp = null;
        for (SffDataPlaneLocator sffDpl : sfcForwarder.getSffDataPlaneLocator()) {
            DataPlaneLocator dataPlaneLocator = sffDpl.getDataPlaneLocator();
            LocatorType sffLocatorType = dataPlaneLocator.getLocatorType();
            if (sffLocatorType instanceof Ip && ((Ip) sffLocatorType).getIp() != null) {
                sffIp = ((Ip) sffLocatorType).getIp().getIpv4Address();
            }
        }
        if (sffIp == null) {
            return null;
        }
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setAddress(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address(sffIp.getValue()));
        ServiceFfNameBuilder ffNameBuilder = new ServiceFfNameBuilder();
        ffNameBuilder.setKey(new ServiceFfNameKey(sffName.getValue()))
                .setName(sffName.getValue())
                .setIp(ipBuilder.build())
                .setDescription(REMOTE + sffName.getValue());
        return ffNameBuilder.build();
    }

    /**
     * From the set of data plane locators, choose the one with IP locator type
     *
     * @param dataPlaneLocators set of locators
     * @return first DPL with IP locator type, null if no such locator is found
     */
    public static SfDataPlaneLocator getDplWithIpLocatorType(List<SfDataPlaneLocator> dataPlaneLocators) {
        if (dataPlaneLocators == null) {
            return null;
        }
        for (SfDataPlaneLocator sfDataPlaneLocator : dataPlaneLocators) {
            LocatorType locatorType = sfDataPlaneLocator.getLocatorType();
            if (locatorType instanceof Ip) {
                return sfDataPlaneLocator;
            }
        }
        return null;
    }

    /**
     * Creates instance identifier for {@link Local} service function forwarder. This IID does not include any key.
     * Every ios-xe device can contain just one local SFF, that means there is one Local SFF for mountpoint
     *
     * @return IID of the Local SFF
     */
    static InstanceIdentifier<Local> createLocalSffIid() {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(Local.class).build();
    }

    /**
     * Creates instance identifier for {@link ServiceFfName} service function forwarder. Particular key is created using
     * {@link ServiceFfName} object
     *
     * @return IID of the remote SFF
     */
    static InstanceIdentifier<ServiceFfName> createRemoteSffIid(@Nonnull ServiceFfName sffName) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(ServiceFfName.class, new ServiceFfNameKey(sffName.getName())).build();
    }

    /**
     * Creates instance identifier for {@link ServiceFfName} service function forwarder. Particular key is created using
     * {@link SffName} object
     *
     * @return IID of the remote SFF
     */
    static InstanceIdentifier<ServiceFfName> createRemoteSffIid(@Nonnull SffName sffName) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(ServiceFfName.class, new ServiceFfNameKey(sffName.getValue())).build();
    }

    /**
     * Creates instance identifier for {@link ServiceFunction}
     *
     * @return IID of the SF
     */
    static InstanceIdentifier<ServiceFunction> createSfIid(@Nonnull ServiceFunctionKey key) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(ServiceFunction.class, key)
                .build();
    }

    /**
     * Creates instance identifier for {@link ServicePath}
     *
     * @return IID of the SP
     */
    static InstanceIdentifier<ServicePath> createServicePathIid(@Nonnull ServicePathKey key) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(ServicePath.class, key).build();
    }

}

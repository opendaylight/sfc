package org.opendaylight.sfc.sfc_netconf.provider.api;


import com.google.common.base.Preconditions;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.config.service.chain.grouping.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class SfcNetconfServiceFunctionForwarderAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfServiceFunctionForwarderAPI.class);

    private static final String LOCAL = "Local forwarder ";
    private static final String REMOTE = "Remote forwarder ";

    public static org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder createLocalForwarder(
            ServiceFunctionForwarder forwarder, IpAddress ipAddress) {
        if(ipAddress.getIpv4Address() != null) {
            // Ip address
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setAddress(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address(ipAddress
            .getIpv4Address().getValue()));
            // Description
            String description = LOCAL + forwarder.getName().getValue();
            LocalBuilder localBuilder = new LocalBuilder();
            localBuilder.setDescription(description)
                .setIp(ipBuilder.build());

            ServiceFunctionForwarderBuilder localForwarderBuilder = new ServiceFunctionForwarderBuilder();
            localForwarderBuilder.setLocal(localBuilder.build());
            return  localForwarderBuilder.build();
        }
        return null;
    }

    public static org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder createRemoteForwarder(
            ServiceFunctionForwarder forwarder, IpAddress ipAddress) {
        if(ipAddress.getIpv4Address() != null) {
            // Ip address
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setAddress(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address(ipAddress
                    .getIpv4Address().getValue()));
            // Description
            String description = REMOTE + forwarder.getName().getValue();
            // Build remote SFF
            ServiceFfNameBuilder serviceFfNameBuilder = new ServiceFfNameBuilder()
                    .setName(forwarder.getName().getValue())
                    .setKey(new ServiceFfNameKey(forwarder.getName().getValue()))
                    .setDescription(description)
                    .setIp(ipBuilder.build());
            List<ServiceFfName> serviceFfNameList = new ArrayList<>();
            serviceFfNameList.add(serviceFfNameBuilder.build());

            ServiceFunctionForwarderBuilder remoteForwarderBuilder = new ServiceFunctionForwarderBuilder();
            remoteForwarderBuilder.setServiceFfName(serviceFfNameList);
            return remoteForwarderBuilder.build();
        }
        return null;
    }

    public static org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder convertLocalToRemote(
            SffName sffName) {
        // Actually, local forwarder is without name. As a parameter, use SffName of appropriate sfc forwarder
        ServiceFunctionForwarder sfcForwarder = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setAddress(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address(sfcForwarder
        .getIpMgmtAddress().getIpv4Address().getValue()));
        List<ServiceFfName> serviceFfNames = new ArrayList<>();
        ServiceFfNameBuilder ffNameBuilder = new ServiceFfNameBuilder();
        ffNameBuilder.setKey(new ServiceFfNameKey(sffName.getValue()))
                .setName(sffName.getValue())
                .setIp(ipBuilder.build())
                .setDescription(LOCAL + sffName.getValue());
        serviceFfNames.add(ffNameBuilder.build());
        ServiceFunctionForwarderBuilder remoteForwarderBuilder = new ServiceFunctionForwarderBuilder();
        remoteForwarderBuilder.setServiceFfName(serviceFfNames);
        return remoteForwarderBuilder.build();
    }

    public static InstanceIdentifier<Local> createLocalSffIid() {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(Local.class).build();
    }

    public static InstanceIdentifier<ServiceFfName> createRemoteSffIid(ServiceFfName sffName) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(ServiceFfName.class, new ServiceFfNameKey(sffName.getName())).build();
    }

    public static InstanceIdentifier<ServiceFfName> createRemoteSffIid(SffName sffName) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(ServiceFfName.class, new ServiceFfNameKey(sffName.getValue())).build();
    }
}

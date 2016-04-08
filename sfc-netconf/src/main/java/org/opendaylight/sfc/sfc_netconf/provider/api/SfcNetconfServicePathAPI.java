package org.opendaylight.sfc.sfc_netconf.provider.api;


import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcNetconfServicePathAPI {

    public static InstanceIdentifier<ServicePath> createServicePathIid(ServicePathKey key) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(ServicePath.class, key).build();
    }

}

package org.opendaylight.ofsfc.provider.utils;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SfcInstanceIdentifierUtils {

    private SfcInstanceIdentifierUtils() {
        throw new UnsupportedOperationException("Utility class should never be instantiated");
    }

    public static final InstanceIdentifier<ServiceFunctionPaths> createServiceFunctionPathsPath() {
        return InstanceIdentifier.builder(ServiceFunctionPaths.class).build();
    }

    public static final InstanceIdentifier<AccessLists> createServiceFunctionAclsPath() {
        return InstanceIdentifier.builder(AccessLists.class).build();
    }

}

package org.opendaylight.sfc.southbound;

import org.opendaylight.sfc.provider.SfcProviderServiceFunctionAPI;

public class JustChecking {

    public static void check() {
        SfcProviderServiceFunctionAPI.readServiceFunction("blabla");
    }
}

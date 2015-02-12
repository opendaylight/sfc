package org.opendaylight.sfc.provider;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;

public class SfcNetconfDataProvider implements Provider {

    DOMMountPointService mountService;
    static SfcNetconfDataProvider sfcNetconfDataProvider;

    /*
     * Factory method
     */
    public static SfcNetconfDataProvider GetNetconfDataProvider() {
        if (sfcNetconfDataProvider != null)
            return sfcNetconfDataProvider;
        else {
            sfcNetconfDataProvider = new SfcNetconfDataProvider();
            return sfcNetconfDataProvider;
        }
    }

    public DOMMountPointService getMountService() {
        return mountService;
    }

    public void setMountService(DOMMountPointService mountService) {
        this.mountService = mountService;
    }

    @Override
    public Collection<Provider.ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        final DOMMountPointService mountService = session.getService(DOMMountPointService.class);

    }
}

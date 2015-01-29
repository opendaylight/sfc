package org.opendaylight.sfc.provider;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;

public class GetNetconfDataProvider implements Provider {
    @Override
    public Collection<Provider.ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
    }
}

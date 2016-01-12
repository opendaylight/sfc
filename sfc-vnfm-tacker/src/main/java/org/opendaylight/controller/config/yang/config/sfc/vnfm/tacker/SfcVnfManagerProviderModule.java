package org.opendaylight.controller.config.yang.config.sfc.vnfm.tacker;

import org.opendaylight.sfc.tacker.api.SfcVnfManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcVnfManagerProviderModule extends org.opendaylight.controller.config.yang.config.sfc.vnfm.tacker.AbstractSfcVnfManagerProviderModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVnfManagerProviderModule.class);

    public SfcVnfManagerProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcVnfManagerProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc.vnfm.tacker.SfcVnfManagerProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        SfcVnfManagerProvider sfcVnfManagerProvider = new SfcVnfManagerProvider.SfcVnfManagerProviderBuilder().build();
        LOG.info("{} successfully started.", SfcVnfManagerProviderModule.class.getCanonicalName());
        return sfcVnfManagerProvider;
    }

}

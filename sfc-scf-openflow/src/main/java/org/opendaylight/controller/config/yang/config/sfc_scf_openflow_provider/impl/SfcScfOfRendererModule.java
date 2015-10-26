package org.opendaylight.controller.config.yang.config.sfc_scf_openflow_provider.impl;

import org.opendaylight.sfc.scfofrenderer.SfcScfOfRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcScfOfRendererModule extends org.opendaylight.controller.config.yang.config.sfc_scf_openflow_provider.impl.AbstractSfcScfOfRendererModule {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfRendererModule.class);

    public SfcScfOfRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcScfOfRendererModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_scf_openflow_provider.impl.SfcScfOfRendererModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final SfcScfOfRenderer sfcScfOfRenderer = new SfcScfOfRenderer(getDataBrokerDependency(), getNotificationServiceDependency());

        java.lang.AutoCloseable ret = new java.lang.AutoCloseable() {
            @Override
            public void close() throws Exception {
                sfcScfOfRenderer.close();
            }
        };

        LOG.info("SFC SCF OF Renderer initialized: (instance {})", ret);

        return ret;
    }

}

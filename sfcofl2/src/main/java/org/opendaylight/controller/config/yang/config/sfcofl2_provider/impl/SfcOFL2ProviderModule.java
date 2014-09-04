package org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ofsfc.provider.OpenflowSfcRenderer;
import org.opendaylight.ofsfc.provider.OpenflowSfcFlowProgrammer;

public class SfcOFL2ProviderModule extends
        org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.AbstractSfcOFL2ProviderModule {
    public SfcOFL2ProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcOFL2ProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.SfcOFL2ProviderModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        DataBroker dataBroker = getDataBrokerDependency();
        final OpenflowSfcRenderer openflowSfcRenderer = new OpenflowSfcRenderer(dataBroker);
        OpenflowSfcFlowProgrammer.createFlowProgrammer(dataBroker);

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                openflowSfcRenderer.close();
            }
        };
    }
}
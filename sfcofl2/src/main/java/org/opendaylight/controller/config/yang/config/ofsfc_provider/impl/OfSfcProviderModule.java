package org.opendaylight.controller.config.yang.config.ofsfc_provider.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.logback.SfcProviderLogbackLoader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ofsfc.provider.OpenFlowSfcRenderer;
import org.opendaylight.ofsfc.provider.OpenFlowSfcRenderer;
import org.opendaylight.ofsfc.provider.OpenFlowSfcRenderer;
import org.opendaylight.ofsfc.provider.OpenFlowSfcRenderer;
import org.opendaylight.ofsfc.provider.OpenflowSfcFlowProgrammer;
import org.opendaylight.ofsfc.provider.OpenflowSfpDataListener;
import org.opendaylight.ofsfc.provider.OpenFlowSfcRenderer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class OfSfcProviderModule extends
        org.opendaylight.controller.config.yang.config.ofsfc_provider.impl.AbstractOfSfcProviderModule {
    public OfSfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OfSfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.ofsfc_provider.impl.OfSfcProviderModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        SfcProviderLogbackLoader.loadSfcLogbackConfiguration();

        DataBroker dataBrokerService = getDataBrokerDependency();

        final OpenFlowSfcRenderer opendaylightSfc = new OpenFlowSfcRenderer();
        opendaylightSfc.setDataProvider(dataBrokerService);

        OpenflowSfcFlowProgrammer.createInstance(dataBrokerService);

        OpenflowSfpDataListener sfpDataListener = new OpenflowSfpDataListener();
        final ListenerRegistration<DataChangeListener> sfpDataChangeListenerRegistration = dataBrokerService
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.sfpEntryIID,
                        sfpDataListener, DataBroker.DataChangeScope.SUBTREE);

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                sfpDataChangeListenerRegistration.close();
                opendaylightSfc.close();
            }
        };
    }
}
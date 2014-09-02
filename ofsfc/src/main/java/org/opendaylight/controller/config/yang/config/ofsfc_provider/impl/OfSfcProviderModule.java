package org.opendaylight.controller.config.yang.config.ofsfc_provider.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.sfc.provider.logback.SfcProviderLogbackLoader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ofsfc.provider.OfSfcProvider;
import org.opendaylight.ofsfc.provider.OfSfcProviderSfEntryDataListener;
import org.opendaylight.ofsfc.provider.OfSfcProviderSfcEntryDataListener;
import org.opendaylight.ofsfc.provider.OfSfcProviderSffDataListener;
import org.opendaylight.ofsfc.provider.OfSfcProviderSffFlowWriter;
import org.opendaylight.ofsfc.provider.OfSfcProviderSfpDataListener;
import org.opendaylight.ofsfc.provider.OfSfcProviderSfpEntryDataListener;
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

        final OfSfcProvider opendaylightSfc = new OfSfcProvider();

        DataBroker dataBrokerService = getDataBrokerDependency();
        opendaylightSfc.setDataProvider(dataBrokerService);

        OfSfcProviderSffFlowWriter.createInstance(dataBrokerService);
        // final SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        OfSfcProviderSfpDataListener sfcProviderSfpDataListener = new OfSfcProviderSfpDataListener();

        // final ListenerRegistration<DataChangeListener>
        // snDataChangeListenerRegistration = dataBrokerService
        // .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
        // OfSfcProvider.ofsnIID,
        // sfcProviderSnDataListener, DataBroker.DataChangeScope.SUBTREE);

        final ListenerRegistration<DataChangeListener> sfpDataChangeListenerRegistration = dataBrokerService
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OfSfcProvider.ofsfpIID,
                        sfcProviderSfpDataListener, DataBroker.DataChangeScope.SUBTREE);

        // ServiceFunctionForwarder
        OfSfcProviderSffDataListener sfcProviderSffDataListener = new OfSfcProviderSffDataListener();
        final ListenerRegistration<DataChangeListener> sffDataChangeListenerRegistration = dataBrokerService
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OfSfcProvider.ofsffIID,
                        sfcProviderSffDataListener, DataBroker.DataChangeScope.SUBTREE);

        // ServiceFunction Entry
        OfSfcProviderSfEntryDataListener sfcProviderSfEntryDataListener = new OfSfcProviderSfEntryDataListener();
        final ListenerRegistration<DataChangeListener> sfEntryDataChangeListenerRegistration = dataBrokerService
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OfSfcProvider.ofsfEntryIID,
                        sfcProviderSfEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        // ServiceFunctionChainEntry
        OfSfcProviderSfcEntryDataListener sfcProviderSfcEntryDataListener = new OfSfcProviderSfcEntryDataListener();
        final ListenerRegistration<DataChangeListener> sfcEntryDataChangeListenerRegistration = dataBrokerService
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OfSfcProvider.ofsfcEntryIID,
                        sfcProviderSfcEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        // ServiceFunctionPathEntry Entry
        OfSfcProviderSfpEntryDataListener sfcProviderSfpEntryDataListener = new OfSfcProviderSfpEntryDataListener();
        final ListenerRegistration<DataChangeListener> sfpEntryDataChangeListenerRegistration = dataBrokerService
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OfSfcProvider.ofsfpEntryIID,
                        sfcProviderSfpEntryDataListener, DataBroker.DataChangeScope.SUBTREE);

        // ServiceFunctions
        // SfcProviderSfsDataListener sfcProviderSfsDataListener = new
        // SfcProviderSfsDataListener();
        // final ListenerRegistration<DataChangeListener>
        // sfsDataChangeListenerRegistration =
        // dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfsIID,
        // sfcProviderSfsDataListener);

        // ServiceFunctionChains
        // SfcProviderSfcDataListener sfcProviderSfcDataListener = new
        // SfcProviderSfcDataListener();
        // final ListenerRegistration<DataChangeListener>
        // sfcDataChangeListenerRegistration =
        // dataBrokerService.registerDataChangeListener( OpendaylightSfc.sfcIID,
        // sfcProviderSfcDataListener );

        /*
         * final BindingAwareBroker.RpcRegistration<ServiceFunctionService>
         * sfRpcRegistration = getRpcRegistryDependency()
         * .addRpcImplementation(ServiceFunctionService.class, sfcProviderRpc);
         * 
         * final BindingAwareBroker.RpcRegistration<ServiceFunctionChainService>
         * sfcRpcRegistration = getRpcRegistryDependency()
         * .addRpcImplementation(ServiceFunctionChainService.class,
         * sfcProviderRpc);
         * 
         * final BindingAwareBroker.RpcRegistration<ServiceNodeService>
         * snRpcRegistration = getRpcRegistryDependency()
         * .addRpcImplementation(ServiceNodeService.class, sfcProviderRpc);
         */

        // close()
        final class AutoCloseableSfc implements AutoCloseable {

            @Override
            public void close() throws Exception {
                sfEntryDataChangeListenerRegistration.close();
                // sfsDataChangeListenerRegistration.close();
                sfcEntryDataChangeListenerRegistration.close();
                sfpEntryDataChangeListenerRegistration.close();
                // snDataChangeListenerRegistration.close();
                sfpDataChangeListenerRegistration.close();
                // sfcDataChangeListenerRegistration.close();
                sffDataChangeListenerRegistration.close();
                // sfRpcRegistration.close();
                // sfcRpcRegistration.close();
                // snRpcRegistration.close();

                // runtimeReg.close();
                opendaylightSfc.close();
                // LOG.info("SFC provider (instance {}) torn down.", this);
            }
        }

        AutoCloseable ret = new AutoCloseableSfc();
        Object[] emptyObjArray = {}; // method putBootstrapData() has no
                                     // argument
        Class[] emptyClassArray = {};
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // scheduledExecutorService.schedule
        // (SfcProviderBootstrapRestAPI.getPutBootstrapData(emptyObjArray,
        // emptyClassArray), 15,
        // TimeUnit.SECONDS);
        // LOG.info("SFC provider (instance {}) initialized.", ret);
        return ret;
    }

}

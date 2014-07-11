package org.opendaylight.controller.config.yang.config.sfc_provider.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcProviderRpc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodeService;

public class SfcProviderModule extends org.opendaylight.controller.config.yang.config.sfc_provider.impl.AbstractSfcProviderModule {
    public SfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_provider.impl.SfcProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
        final SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        DataProviderService dataBrokerService = getDataBrokerDependency();
        opendaylightSfc.setDataProvider(dataBrokerService);

        // Register our OpendaylightSFs instance as the RPC implementation
        // for the ServiceFunctionService.
        final BindingAwareBroker.RpcRegistration<ServiceFunctionService> sfRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionService.class,
                                sfcProviderRpc);
        final BindingAwareBroker.RpcRegistration<ServiceFunctionChainService> sfcRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceFunctionChainService.class,
                                sfcProviderRpc);
        final BindingAwareBroker.RpcRegistration<ServiceNodeService> snRpcRegistration =
                getRpcRegistryDependency()
                        .addRpcImplementation(ServiceNodeService.class,
                                sfcProviderRpc);

        // Wrap SFs as AutoCloseable and close registrations to md-sal at
        // close(). The close method is where you would generally clean up
        // thread pools
        // etc.
        final class AutoCloseableSFs implements AutoCloseable {

            @Override
            public void close() throws Exception {
                sfRpcRegistration.close();
                sfcRpcRegistration.close();
                snRpcRegistration.close();
//                sfpRpcRegistration.close();
//                dataChangeListenerRegistration.close(); //closes the listener registrations (removes it)

                opendaylightSfc.close(); // remove operational data when
                // shutting down
            }
        }

        return new AutoCloseableSFs();
    }

}

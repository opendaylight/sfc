package org.opendaylight.controller.config.yang.config.service_function_provider.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.sfc.sf.OpendaylightSFs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.ServiceFunctionService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class ServiceFunctionsProviderModule
		extends
		org.opendaylight.controller.config.yang.config.service_function_provider.impl.AbstractServiceFunctionsProviderModule {
	public ServiceFunctionsProviderModule(
			org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public ServiceFunctionsProviderModule(
			org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			org.opendaylight.controller.config.yang.config.service_function_provider.impl.ServiceFunctionsProviderModule oldModule,
			java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void customValidation() {
		// add custom validation form module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final OpendaylightSFs opendaylightSFs = new OpendaylightSFs();

		DataProviderService dataBrokerService = getDataBrokerDependency();

		opendaylightSFs.setDataProvider(dataBrokerService, null);

		// Register our OpendaylightToaster instance as the RPC implementation
		// for the ToasterService.
		final BindingAwareBroker.RpcRegistration<ServiceFunctionService> rpcRegistration = 
				getRpcRegistryDependency()
				.addRpcImplementation(ServiceFunctionService.class,
						opendaylightSFs);

		final ListenerRegistration<DataChangeListener> dataChangeListenerRegistration = dataBrokerService
				.registerDataChangeListener(OpendaylightSFs.IID,
						opendaylightSFs);

		// Wrap SFs as AutoCloseable and close registrations to md-sal at
		// close(). The close method is where you would generally clean up
		// thread pools
		// etc.
		final class AutoCloseableSFs implements AutoCloseable {

			@Override
			public void close() throws Exception {
				rpcRegistration.close();
				dataChangeListenerRegistration.close(); //closes the listener registrations (removes it)
				
				opendaylightSFs.close(); // remove operational data when
											// shutting down
			}
		}

		return new AutoCloseableSFs();
	}

}

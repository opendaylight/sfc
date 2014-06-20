package org.opendaylight.controller.config.yang.config.test_consumer.impl;

import org.opendaylight.sfc.sf.consumer.api.ServiceFunctionConsumerImpl;
import org.opendaylight.sfc.sf.consumer.api.ServiceFunctionsConsumer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.service.functions.ServiceFunction;

public class ServiceFunctionsConsumerModule extends org.opendaylight.controller.config.yang.config.test_consumer.impl.AbstractServiceFunctionsConsumerModule {
    public ServiceFunctionsConsumerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ServiceFunctionsConsumerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.test_consumer.impl.ServiceFunctionsConsumerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
    	ServiceFunctionService toasterService = getRpcRegistryDependency().getRpcService(ServiceFunctionService.class);

        final ServiceFunctionConsumerImpl consumer = new ServiceFunctionConsumerImpl(toasterService);

        final ServiceFunctionsConsumerRuntimeRegistration runtimeReg =
                getRootRuntimeBeanRegistratorWrapper().register( consumer );
        
        final class AutoCloseableServiceFunctionsConsumer implements ServiceFunctionsConsumer, AutoCloseable {

            @Override
            public void close() throws Exception {
            	runtimeReg.close();
            }

			@Override
			public boolean updateServiceFunctions(ServiceFunction sf) {
				
				return consumer.updateServiceFunctions(sf);
			}

            
        }

        AutoCloseable ret = new AutoCloseableServiceFunctionsConsumer();
        return ret;
    }

}

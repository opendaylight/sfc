package org.opendaylight.controller.config.yang.config.sfc_test_consumer.impl;

import org.opendaylight.sfc.sfc_test_consumer.SfcTestConsumerImpl;
import org.opendaylight.sfc.sfc_test_consumer.SfcTestConsumer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.
        rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodeService;

public class SfcTestConsumerModule extends org.opendaylight.controller.config.yang.config.sfc_test_consumer.impl.AbstractSfcTestConsumerModule {
    public SfcTestConsumerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcTestConsumerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_test_consumer.impl.SfcTestConsumerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ServiceFunctionService sfService = getRpcRegistryDependency().getRpcService(ServiceFunctionService.class);
        ServiceFunctionChainService sfcService = getRpcRegistryDependency().getRpcService(ServiceFunctionChainService.class);
        ServiceNodeService snService = getRpcRegistryDependency().getRpcService(ServiceNodeService.class);

        final SfcTestConsumerImpl sfcTestConsumer = new SfcTestConsumerImpl(sfService, sfcService, snService);

        final SfcTestConsumerRuntimeRegistration runtimeReg =
                getRootRuntimeBeanRegistratorWrapper().register(sfcTestConsumer);

        final class AutoCloseableSfcTestConsumer implements SfcTestConsumer, AutoCloseable {

            @Override
            public void close() throws Exception {
                runtimeReg.close();
            }

        }

        AutoCloseable ret = new AutoCloseableSfcTestConsumer();
        return ret;
    }

}

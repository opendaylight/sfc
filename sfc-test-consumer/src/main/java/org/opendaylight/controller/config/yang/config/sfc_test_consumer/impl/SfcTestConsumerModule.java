/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.sfc_test_consumer.impl;

import org.opendaylight.sfc.sfc_test_consumer.SfcTestConsumer;
import org.opendaylight.sfc.sfc_test_consumer.SfcTestConsumerImpl;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;

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

        final SfcTestConsumerImpl sfcTestConsumer = new SfcTestConsumerImpl(sfService, sfcService);

        final SfcTestConsumerRuntimeRegistration runtimeReg =
                getRootRuntimeBeanRegistratorWrapper().register(sfcTestConsumer);

        final class AutoCloseableServiceFunctionsConsumer implements SfcTestConsumer, AutoCloseable {

            @Override
            public void close() throws Exception {
                runtimeReg.close();
            }

        }

        AutoCloseable ret = new AutoCloseableServiceFunctionsConsumer();
        return ret;
    }

}

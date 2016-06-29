package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sfc.bootstrap.impl.rev150804;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.sfc.bootstrap.SfcProviderBootstrapRestAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcBootstrapModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sfc.bootstrap.impl.rev150804.AbstractSfcBootstrapModule {
    private static final Logger LOG = LoggerFactory.getLogger(SfcBootstrapModule.class);

    public SfcBootstrapModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcBootstrapModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sfc.bootstrap.impl.rev150804.SfcBootstrapModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        AutoCloseable ret = new BootstrapSfcAutoCloseable();
        Object[] emptyObjArray = {}; // method putBootstrapData() has no argument
        Class[] emptyClassArray = {};
        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(1);
        scheduledExecutorService.schedule
                (SfcProviderBootstrapRestAPI.getPutBootstrapData(emptyObjArray, emptyClassArray), 15,
                        TimeUnit.SECONDS);

        scheduledExecutorService.shutdown();


        LOG.info("SFC bootstrap (instance {}) initialized.", ret);
        return ret;
    }

    public class BootstrapSfcAutoCloseable implements AutoCloseable {

        @Override
        public void close() throws Exception {
            // TODO Auto-generated method stub

        }
    }

}

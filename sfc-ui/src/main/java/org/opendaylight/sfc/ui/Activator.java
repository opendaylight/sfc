package org.opendaylight.sfc.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private ServiceTracker httpTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        httpTracker = new ServiceTracker(context, HttpService.class.getName(), null) {

            @Override
            public void removedService(ServiceReference reference, Object service) {

                try {
                    ((HttpService) service).unregister("/sfc");
                } catch (Exception exception) {
                    LOG.error("removedService", exception);
                }
            }

            @Override
            public Object addingService(ServiceReference reference) {
                // HTTP service is available, register our servlet...
                HttpService httpService = (HttpService) this.context.getService(reference);
                try {
                    HttpContext defaultHttpContext = httpService.createDefaultHttpContext();
                    httpService.registerResources("/sfc", "/pages", new SfcUiHttpContext(defaultHttpContext, "/pages/"));
                } catch (NamespaceException exception) {
                    LOG.error("addingService", exception);
                }
                return httpService;
            }
        };
        httpTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        httpTracker.close();
    }

}

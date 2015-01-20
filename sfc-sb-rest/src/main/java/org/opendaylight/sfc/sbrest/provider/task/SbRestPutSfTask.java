package org.opendaylight.sfc.sbrest.provider.task;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.opendaylight.sfc.sbrest.json.ExporterFactory;
import org.opendaylight.sfc.sbrest.json.SfExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;

import java.util.concurrent.Callable;

@Deprecated
public class SbRestPutSfTask implements Callable {
    private static final String ACCEPT = "application/json";
    private static final String HTTP_ERROR_MSG = "Failed, HTTP error code : ";
    private static final int HTTP_OK = 200;

    private ServiceFunction serviceFunction;
    private String urlMgmt;

    public SbRestPutSfTask(ServiceFunction serviceFunction, String urlMgmt) {
        this.serviceFunction = serviceFunction;
        this.urlMgmt = urlMgmt;
    }

    @Override
    public Object call() throws Exception {
        putServiceFunction(serviceFunction);
        return null;
    }


    private void putServiceFunction(ServiceFunction serviceFunction) {

        ExporterFactory ef = new SfExporterFactory();
        String jsonOutput = ef.getExporter().exportJson(serviceFunction);

        System.out.println("[:SF:]" + jsonOutput + "\n");

        if (false) {
            ClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create(clientConfig);

            ClientResponse clientResponse;

            clientResponse = client
                    .resource(urlMgmt).type(ACCEPT)
                    .put(ClientResponse.class, jsonOutput);


            if (clientResponse.getStatus() != HTTP_OK) {
                throw new UniformInterfaceException(HTTP_ERROR_MSG
                        + clientResponse.getStatus(),
                        clientResponse);
            }

            clientResponse.close();
        }

    }

}

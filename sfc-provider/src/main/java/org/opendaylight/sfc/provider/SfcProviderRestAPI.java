/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class provides a REST client implementation to
 * manage SB devices that provide a REST server interface.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderRestAPI extends SfcProviderAbstractRestAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderRestAPI.class);
    private static final String HTTP_ERROR_MSG = "Failed : HTTP error code : ";

    SfcProviderRestAPI (Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    private String getServiceFunctionForwarderURI(ServiceFunctionForwarder serviceFunctionForwarder) {
        return  "http://localhost:8181/restconf/config/" +
                "service-function-forwarder:service-function-forwarders/" +
                "service-function-forwarder/" + serviceFunctionForwarder.getName();
    }

    public void putServiceFunctionForwarder (ServiceFunctionForwarder serviceFunctionForwarder) {

        printTraceStart(LOG);

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);


        String sffJSON = getRESTObj(getServiceFunctionForwarderURI(serviceFunctionForwarder));

        ClientResponse putClientRemoteResponse;

        String sffURI = "http://127.0.0.1:5000/config/service" +
                "-function-forwarder:service-function-forwarders/service" +
                "-function-forwarder/" +
                serviceFunctionForwarder.getName();
        putClientRemoteResponse = client
                .resource(sffURI).type(MediaType.APPLICATION_JSON_TYPE)
                .put(ClientResponse.class, sffJSON);

        if (putClientRemoteResponse.getStatus() >= 300)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientRemoteResponse.getStatus(),
                    putClientRemoteResponse);
        }

        putClientRemoteResponse.close();
        printTraceStop(LOG);

    }


    public void deleteServiceFunctionForwarder (ServiceFunctionForwarder serviceFunctionForwarder)
    {

        printTraceStart(LOG);
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse deleteClientRemoteResponse;

        String sffURI = "http://127.0.0.1:5000/config/service" +
                "-function-forwarder:service-function-forwarders/service" +
                "-function-forwarder/" +
                serviceFunctionForwarder.getName();
        deleteClientRemoteResponse = client
                .resource(sffURI).type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);

        if (deleteClientRemoteResponse.getStatus() >= 300)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + deleteClientRemoteResponse.getStatus(),
                    deleteClientRemoteResponse);
        }

        deleteClientRemoteResponse.close();
        printTraceStop(LOG);
    }

    /**
     * GETs a Generic JSON REST Object and returns it to the caller
     * <p>
     * @param restURI URI String
     * @return The SFP in JSON format
     */
    private String getRESTObj(String restURI) {

        printTraceStart(LOG);
        final HTTPBasicAuthFilter basicAuthFilter = new HTTPBasicAuthFilter("admin", "admin");
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse;
        client.addFilter(basicAuthFilter);
        getClientResponse = client
                .resource(restURI)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(ClientResponse.class);
        if (getClientResponse.getStatus() != 200)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + getClientResponse.getStatus(),
                    getClientResponse);
        }
        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();
        printTraceStop(LOG);
        return jsonOutput;
    }

    private String getServiceFunctionPathURI(ServiceFunctionPath serviceFunctionPath) {
        return  "http://localhost:8181/restconf/config/service" +
                "-function-path:service-function-paths/" +
                "service-function-path/" + serviceFunctionPath.getName();
    }


    /**
     * Communicates SFP to SouthBound REST Server
     * <p>
     * @param serviceFunctionPath Service Function Path Name
     * @return The SFP in JSON format
     */
    public void putServiceFunctionPath (ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        String sfpJSON = getRESTObj(getServiceFunctionPathURI(serviceFunctionPath));

        ClientResponse putClientRemoteResponse;

        String sfpURI  = "http://127.0.0.1:5000/config/service" +
                "-function-path:service-function-paths/" +
                "service-function-path/" + serviceFunctionPath.getName();
        putClientRemoteResponse = client
                .resource(sfpURI).type(MediaType.APPLICATION_JSON_TYPE)
                .put(ClientResponse.class, sfpJSON);
        if (putClientRemoteResponse.getStatus() >= 300)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientRemoteResponse.getStatus(),
                    putClientRemoteResponse);
        }
        putClientRemoteResponse.close();
        printTraceStop(LOG);

    }

    public static  SfcProviderRestAPI getPutServiceFunctionForwarder (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionForwarder");
    }

    public static  SfcProviderRestAPI getDeleteServiceFunctionForwarder (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "deleteServiceFunctionForwarder");
    }

    public static  SfcProviderRestAPI getPutServiceFunctionPath (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionPath");
    }
}

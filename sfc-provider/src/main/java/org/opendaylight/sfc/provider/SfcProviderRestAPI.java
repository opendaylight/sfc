/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff
        .rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp
        .rev140701.service.function.paths.ServiceFunctionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;


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

    public void putServiceFunctionForwarders (ServiceFunctionForwarders serviceFunctionForwarders) {

        final HTTPBasicAuthFilter basicAuthFilter = new HTTPBasicAuthFilter("admin", "admin");
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        client.addFilter(basicAuthFilter);

        ClientResponse getClientResponse = null;
        try
        {
            getClientResponse = client
                    .resource("http://localhost:8181/restconf/config/service" +
                            "-function-forwarder:service-function-forwarders/")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        } catch (UniformInterfaceException e)
        {
            LOG.error("REST Server error. Status: {} Message: {}",
                    getClientResponse.getClientResponseStatus(),
                    e.getMessage());
        } catch (ClientHandlerException e)
        {
            LOG.error("{} : Could not communicate with REST Server ", e.getMessage());
        }

        if (getClientResponse.getStatus() != 200)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + getClientResponse.getStatus(),
                    getClientResponse);
        }

        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();

        ClientResponse putClientRemoteResponse = client
                .resource("http://127.0.0.1:5000/" +
                        "/config/service-function-forwarder:service-function" +
                        "-forwarders/").type(MediaType.APPLICATION_JSON_TYPE)
                .put(ClientResponse.class, jsonOutput);


        if (putClientRemoteResponse.getStatus() != 200)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientRemoteResponse.getStatus(),
                    putClientRemoteResponse);
        }

        putClientRemoteResponse.close();

    }

    /**
     * GETs a Service Function Path from REST Server and returns its
     * data in JSON format
     * <p>
     * @param serviceFunctionPath Service Function Path Name
     * @return The SFP in JSON format
     */
    private String getRESTServiceFunctionPath(ServiceFunctionPath serviceFunctionPath) {
        final HTTPBasicAuthFilter basicAuthFilter = new HTTPBasicAuthFilter("admin", "admin");
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse;
        try
        {
            client.addFilter(basicAuthFilter);
            String sfpURI  = "http://localhost:8181/restconf/config/service" +
                    "-function-path:service-function-paths/" +
                    "service-function-path/" + serviceFunctionPath.getName();
            getClientResponse = client
                    .resource(sfpURI)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        } catch (UniformInterfaceException e)
        {
            LOG.error("REST Server error. Message: {}",
                    e.getMessage());
            return null;
        } catch (ClientHandlerException e)
        {
            LOG.error("Could not communicate with REST Server: {} ", e.getMessage());
            return null;
        }

        if (getClientResponse.getStatus() != 200)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + getClientResponse.getStatus(),
                    getClientResponse);
        }
        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();
        return jsonOutput;
    }

    /**
     * Communicates SFP to SouthBound REST Server
     * <p>
     * @param serviceFunctionPath Service Function Path Name
     * @return The SFP in JSON format
     */
    public void putServiceFunctionPath (ServiceFunctionPath serviceFunctionPath) {

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        String sfpJSON = getRESTServiceFunctionPath(serviceFunctionPath);

        ClientResponse putClientRemoteResponse;
        try
        {
            String sfpURI  = "http://127.0.0.1:5000/config/service" +
                    "-function-path:service-function-paths/" +
                    "service-function-path/" + serviceFunctionPath.getName();
            putClientRemoteResponse = client
                    .resource(sfpURI).type(MediaType.APPLICATION_JSON_TYPE)
                    .put(ClientResponse.class, sfpJSON);
        } catch (UniformInterfaceException e)
        {
            LOG.error("REST Server error. Message: {}",
                    e.getMessage());
            return;
        } catch (ClientHandlerException e)
        {
            LOG.error("Could not communicate with REST Server: {} ", e.getMessage());
            return;
        }
        putClientRemoteResponse.close();

    }

    public static  SfcProviderRestAPI getPutServiceFunctionForwarders (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionForwarders");
    }

    public static  SfcProviderRestAPI getPutServiceFunctionPath (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionPath");
    }

}

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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private static final String ACCEPT = "application/json";
    private static final String HTTP_ERROR_MSG = "Failed : HTTP error code : ";

    SfcProviderRestAPI (Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public void putServiceFunctionForwarders (ServiceFunctionForwarders serviceFunctionForwarders) {

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse = client
                .resource("http://localhost:8080/restconf/config/service-function-forwarder:service-function-forwarders/")
                .accept(ACCEPT)
                .get(ClientResponse.class);

        if (getClientResponse.getStatus() != 200) {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + getClientResponse.getStatus(),
                    getClientResponse);
        }

        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();

        ClientResponse putClientRemoteResponse;

        putClientRemoteResponse = client
                .resource("http://localhost:5000/paths").type(ACCEPT)
                .put(ClientResponse.class, jsonOutput);


        if (putClientRemoteResponse.getStatus() != 200) {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientRemoteResponse.getStatus(),
                    putClientRemoteResponse);
        }

        putClientRemoteResponse.close();

        ClientResponse putClientLocalResponse= client
                .resource("http://localhost:5000/paths").type(ACCEPT)
                .put(ClientResponse.class, jsonOutput);

        if (putClientLocalResponse.getStatus() != 200) {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientLocalResponse.getStatus(),
                    putClientLocalResponse);
        }
        putClientLocalResponse.close();

    }

    public void putServiceFunctionPaths (ServiceFunctionPaths serviceFunctionPaths) {

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse = client
                .resource("http://localhost:8080/restconf/config/service-function-path:service-function-paths/")
                .accept(ACCEPT)
                .get(ClientResponse.class);

        if (getClientResponse.getStatus() != 200) {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + getClientResponse.getStatus(),
                    getClientResponse);
        }

        /*
        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();
        ClientResponse putClientRemoteResponse= client
                .resource("http://31.133.132.41:5000/paths").type("application/json")
                .put(ClientResponse.class, jsonOutput);

        putClientRemoteResponse.close();
        */

    }

    public static  SfcProviderRestAPI getPutServiceFunctionForwarders (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionForwarders");
    }

    public static  SfcProviderRestAPI getPutServiceFunctionPaths (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionPaths");
    }

}

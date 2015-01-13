/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider;

import java.util.concurrent.Callable;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class SbRestPutAclTask implements Callable {

    private static final String ACCEPT = "application/json";
    private static final String HTTP_ERROR_MSG = "Failed : HTTP error code : ";
    private static final String URL_DATA_SRC = "http://localhost:8181/restconf/config/ietf-acl:access-lists/access-list/";
    private static final String URL_DATA_TARGET = "/config/ietf-acl:access-lists/access-list/";
    private static final int HTTP_OK = 200;

    private String accessListName;
    private String urlMgmt;

    public SbRestPutAclTask(String accessListName, String urlMgmt){

        this.accessListName = accessListName;
        this.urlMgmt = urlMgmt;
    }

    @Override
    public Object call() throws Exception {
        putAccessList(accessListName);
        return null;
    }


    private void putAccessList(String accessListName) {
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        final HTTPBasicAuthFilter basicAuthFilter = new HTTPBasicAuthFilter("admin", "admin");
        client.addFilter(basicAuthFilter);

        ClientResponse getClientResponse = client
                .resource(URL_DATA_SRC + accessListName)
                .accept(ACCEPT)
                .get(ClientResponse.class);

        if (getClientResponse.getStatus() != HTTP_OK) {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + getClientResponse.getStatus(),
                    getClientResponse);
        }

        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();

        ClientResponse putClientRemoteResponse;

        putClientRemoteResponse = client
                .resource(urlMgmt + URL_DATA_TARGET + accessListName).type(ACCEPT)
                .put(ClientResponse.class, jsonOutput);


        if (putClientRemoteResponse.getStatus() != HTTP_OK) {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientRemoteResponse.getStatus(),
                    putClientRemoteResponse);
        }

        putClientRemoteResponse.close();

        ClientResponse putClientLocalResponse= client
                .resource(urlMgmt).type(ACCEPT)
                .put(ClientResponse.class, jsonOutput);

        if (putClientLocalResponse.getStatus() != HTTP_OK) {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientLocalResponse.getStatus(),
                    putClientLocalResponse);
        }
        putClientLocalResponse.close();

    }


}

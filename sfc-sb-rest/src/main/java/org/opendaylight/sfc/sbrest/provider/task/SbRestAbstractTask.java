/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.opendaylight.sfc.sbrest.json.ExporterFactory;

import java.util.concurrent.Callable;

abstract public class SbRestAbstractTask implements Callable {

    protected final String APPLICATION_JSON = "application/json";
    protected final String HTTP_ERROR_MSG = "Failed, HTTP error code : ";
    protected final int HTTP_OK = 200;

    protected RestOperation restOperation;
    protected String urlMgmt;
    protected ExporterFactory exporterFactory;
    protected String jsonObject = null;

    public SbRestAbstractTask(RestOperation restOperation, String urlMgmt) {

        this.restOperation = restOperation;
        this.urlMgmt = urlMgmt;
    }

    @Override
    public Object call() throws Exception {
        if (jsonObject != null) performOperation(jsonObject);
        return null;
    }

    private void performOperation(String json) {
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse clientRemoteResponse = null;

        switch (restOperation) {
            case PUT:
                clientRemoteResponse = client
                        .resource(urlMgmt).type(APPLICATION_JSON)
                        .put(ClientResponse.class, json);
                break;
            case POST:
                clientRemoteResponse = client
                        .resource(urlMgmt).type(APPLICATION_JSON)
                        .post(ClientResponse.class, json);
                break;
            case DELETE:
                clientRemoteResponse = client
                        .resource(urlMgmt).type(APPLICATION_JSON)
                        .delete(ClientResponse.class, json);
        }

        if (clientRemoteResponse != null) {
            if (clientRemoteResponse.getStatus() != HTTP_OK) {
                throw new UniformInterfaceException(HTTP_ERROR_MSG + clientRemoteResponse.getStatus(),
                        clientRemoteResponse);
            }
            clientRemoteResponse.close();
        }
    }

}

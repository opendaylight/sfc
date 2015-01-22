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
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class WsTask implements Runnable {

    protected final String APPLICATION_JSON = "application/json";
    protected final String HTTP_ERROR_MSG = "Failed, HTTP error code : ";
    protected final int HTTP_OK = 200;

    String url;
    RestOperation restOperation;
    String json;

    public WsTask(String url, RestOperation restOperation, String json) {

        this.url = url;
        this.restOperation = restOperation;
        this.json = json;
    }

    @Override
    public void run() {
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        ClientResponse clientRemoteResponse = null;
        WebResource.Builder wrb = client.resource(url).type(APPLICATION_JSON);
        switch (restOperation) {
            case PUT:
                clientRemoteResponse = wrb.put(ClientResponse.class, json);
                break;
            case POST:
                clientRemoteResponse = wrb.post(ClientResponse.class, json);
                break;
            case DELETE:
                clientRemoteResponse = wrb.delete(ClientResponse.class, json);
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

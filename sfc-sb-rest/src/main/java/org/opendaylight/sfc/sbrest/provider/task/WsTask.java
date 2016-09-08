/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsTask implements Runnable {

    protected static final String APPLICATION_JSON = "application/json";
    protected static final String HTTP_ERROR_MSG = "Failed, HTTP error code : ";
    protected static final int HTTP_OK = 200;
    private static final Logger LOG = LoggerFactory.getLogger(WsTask.class);

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
        ClientConfig clientConfig = new ClientConfig();
        Client client = ClientBuilder.newClient(clientConfig);
        Response clientRemoteResponse = null;
        WebTarget webTarget = null;
        Invocation.Builder targetBuilder = null;
        try {
            targetBuilder = client.target(url).request(MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (targetBuilder != null) {
            switch (restOperation) {
                case PUT:
                    try {
                        clientRemoteResponse = targetBuilder.put(Entity.text(json));
                    } catch(ResponseProcessingException e) {
                        LOG.error("Failed to communicate with REST Server: {} ", this.url);
                    } finally {
                        if (clientRemoteResponse != null) {
                            if (clientRemoteResponse.getStatus() == HTTP_OK) {
                                clientRemoteResponse.close();
                            }
                        }
                    }
                    break;
                case POST:
                    try {
                        clientRemoteResponse = targetBuilder.post(Entity.text(json));
                    } catch(ResponseProcessingException e) {
                        LOG.error("Failed to communicate with REST Server: {} ", this.url);
                    } finally {
                        if (clientRemoteResponse != null) {
                            if (clientRemoteResponse.getStatus() == HTTP_OK) {
                                clientRemoteResponse.close();
                            }
                        }
                    }
                    break;
                case DELETE:
                    try {
                        clientRemoteResponse = targetBuilder.delete();
                    } catch(ResponseProcessingException e) {
                        LOG.error("Failed to communicate with REST Server: {} ", this.url);
                    } finally {
                        if (clientRemoteResponse != null) {
                            if (clientRemoteResponse.getStatus() == HTTP_OK) {
                                clientRemoteResponse.close();
                            }
                        }
                    }
            }
        }
    }
}

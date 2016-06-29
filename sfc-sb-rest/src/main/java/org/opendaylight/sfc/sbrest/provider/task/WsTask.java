/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.net.ConnectException;
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
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        ClientResponse clientRemoteResponse = null;
        WebResource wr = null;
        WebResource.Builder wrb = null;
        try {
            wrb = client.resource(url).type(APPLICATION_JSON);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (wrb != null) {
            switch (restOperation) {
                case PUT:
                    try {
                        clientRemoteResponse = wrb.put(ClientResponse.class, json);
                    } catch (UniformInterfaceException e) {
                        // http://stackoverflow.com/questions/12502233/jersey-uniforminterfaceexception-trying-to-proxy-to-rest-post-service
                        LOG.error("REST Server error. Message: {}", e.getMessage());
                    } catch (ClientHandlerException e) {
                        if (e.getCause() instanceof ConnectException) {
                            LOG.error("Failed to communicate with REST Server: {} ", this.url);
                        } else {
                            LOG.error("ClientHandlerException on {}: {}", Thread.currentThread().getStackTrace()[1],
                                    e.getMessage());
                        }
                    } finally {
                        if (clientRemoteResponse != null) {
                            if (clientRemoteResponse.getStatus() != HTTP_OK) {
                                throw new UniformInterfaceException(HTTP_ERROR_MSG + clientRemoteResponse.getStatus(),
                                        clientRemoteResponse);
                            }
                            clientRemoteResponse.close();
                        }
                    }
                    break;
                case POST:
                    try {
                        clientRemoteResponse = wrb.post(ClientResponse.class, json);
                    } catch (UniformInterfaceException e) {
                        LOG.error("REST Server error. Message: {}", e.getMessage());
                    } catch (ClientHandlerException e) {
                        if (e.getCause() instanceof ConnectException) {
                            LOG.error("Failed to communicate with REST Server: {} ", this.url);
                        } else {
                            LOG.error("ClientHandlerException on {}: {}", Thread.currentThread().getStackTrace()[1],
                                    e.getMessage());
                        }
                    } finally {
                        if (clientRemoteResponse != null) {
                            if (clientRemoteResponse.getStatus() != HTTP_OK) {
                                throw new UniformInterfaceException(HTTP_ERROR_MSG + clientRemoteResponse.getStatus(),
                                        clientRemoteResponse);
                            }
                            clientRemoteResponse.close();
                        }
                    }
                    break;
                case DELETE:
                    try {
                        clientRemoteResponse = wrb.delete(ClientResponse.class);
                    } catch (UniformInterfaceException e) {
                        LOG.error("REST Server error. Message: {}", e.getMessage());
                    } catch (ClientHandlerException e) {
                        if (e.getCause() instanceof ConnectException) {
                            LOG.error("Failed to communicate with REST Server: {} ", this.url);
                        } else {
                            LOG.error("ClientHandlerException on {}: {}", Thread.currentThread().getStackTrace()[1],
                                    e.getMessage());
                        }
                    } finally {
                        if (clientRemoteResponse != null) {
                            if (clientRemoteResponse.getStatus() != HTTP_OK) {
                                throw new UniformInterfaceException(HTTP_ERROR_MSG + clientRemoteResponse.getStatus(),
                                        clientRemoteResponse);
                            }
                            clientRemoteResponse.close();
                        }
                    }
            }
        }
    }
}

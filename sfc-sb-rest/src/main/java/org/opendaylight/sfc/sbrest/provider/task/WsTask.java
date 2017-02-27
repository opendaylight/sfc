/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
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

        Client client = ClientBuilder.newClient();
        WebTarget wt = null;
        Invocation.Builder ib = null;
        try {
            wt = client.target(url);
            ib = wt.request(APPLICATION_JSON);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BiConsumer<Function<Entity<String>, Response>, String> callHttpMethod = (
                fun, json) -> {
                    Response clientRemoteResponse = null;
            try {
                clientRemoteResponse = fun.apply(Entity.json(json));
            } catch (ProcessingException pe) {
                LOG.error("REST Server error on {}. Message: {}",
                        Thread.currentThread().getStackTrace()[1],
                        pe.getMessage());
            } finally {
                if (clientRemoteResponse != null) {
                    if (clientRemoteResponse.getStatus() != HTTP_OK) {
                        throw new WebApplicationException(
                                HTTP_ERROR_MSG
                                        + clientRemoteResponse.getStatus(),
                                        clientRemoteResponse);
                    }
                    clientRemoteResponse.close();
                }
            }
        };

        if (ib != null) {
            switch (restOperation) {
                case PUT:
                    callHttpMethod.accept(ib::put, json);
                    break;
                case POST:
                    callHttpMethod.accept(ib::post, json);
                    break;
                case DELETE:
                    Response clientRemoteResponse = null;
                    try {
                        clientRemoteResponse = ib.delete(Response.class);
                    } catch (ProcessingException pe) {
                        LOG.error("REST Server error on {}. Message: {}",
                                Thread.currentThread().getStackTrace()[1],
                                pe.getMessage());
                    } finally {
                        if (clientRemoteResponse != null) {
                            if (clientRemoteResponse.getStatus() != HTTP_OK) {
                                throw new WebApplicationException(
                                        HTTP_ERROR_MSG
                                                + clientRemoteResponse.getStatus(),
                                        clientRemoteResponse);
                            }
                            clientRemoteResponse.close();
                        }
                    }
                    break;
            }
        }
    }
}

/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsTask implements Runnable {

    protected static final String HTTP_ERROR_MSG = "Failed, HTTP error code : ";
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

        final Entity<String> entity = Entity.entity(json, MediaType.APPLICATION_JSON);
        Builder wrb = client.target(url).request(MediaType.APPLICATION_JSON);

        try {
            Response response;
            switch (restOperation) {
                case PUT:
                    response = wrb.put(entity);
                    break;
                case POST:
                    response = wrb.post(entity);
                    break;
                case DELETE:
                    response = wrb.delete();
                    break;
                default:
                    LOG.warn("{} operation not implemented", restOperation);
                    return;
            }

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOG.error("{} operation to {} failed with HTTP status code {}", restOperation, url,
                        response.getStatus());
            }
        } catch (WebApplicationException | ProcessingException e) {
            LOG.error("{} operation to {} failed", restOperation, url, e);
        }
    }
}

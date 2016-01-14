/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.api;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.opendaylight.sfc.tacker.dto.*;
import org.opendaylight.sfc.vnfm.spi.SfcVnfManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.ServiceStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TackerManager implements SfcVnfManager, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TackerManager.class);
    private static final Gson GSON = new Gson();
    private static final Integer CONNECT_TIMEOUT_MILLISEC = 7000;
    private static final Integer READ_TIMEOUT_MILLISEC = 5000;
    private final Client client;
    private String baseUri;
    private int tackerPort;
    private int keystonePort;
    private Auth auth;

    private TackerManager(TackerManagerBuilder builder) {
        Preconditions.checkNotNull(builder.getBaseUri());
        Preconditions.checkArgument(builder.getTackerPort() != 0);
        Preconditions.checkArgument(builder.getKeystonePort() != 0);
        Preconditions.checkNotNull(builder.getAuth());

        this.baseUri = builder.getBaseUri();
        this.tackerPort = builder.getTackerPort();
        this.keystonePort = builder.getKeystonePort();
        this.auth = builder.getAuth();

        client = Client.create();
        client.setReadTimeout(READ_TIMEOUT_MILLISEC);
        client.setConnectTimeout(CONNECT_TIMEOUT_MILLISEC);
    }

    @Override
    public boolean createSf(ServiceFunctionType sfType) {

        WebResource webResource = client.resource(baseUri + ":" + tackerPort).path("/v1.0/vnfs");
        TackerRequest tackerRequest = TackerRequest.builder()
            .setAuth(this.auth)
            .setVnf(Vnf.builder()
                .setName(sfType.getType().getValue())
                .setAttributes(Attributes.builder().setServiceType(sfType.getType().getValue()).build())
                .build())
            .build();
        ClientResponse response = webResource.type(javax.ws.rs.core.MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, GSON.toJson(tackerRequest));

        if (response != null) {
            switch (response.getStatus()) {
                case 201:
                    String json = response.getEntity(String.class);
                    TackerResponse tackerResponse = GSON.fromJson(json, TackerResponse.class);
                    LOG.debug("VNF successfully created.");
                    LOG.debug(GSON.toJson(tackerResponse));
                    return true;
                case 401:
                    LOG.debug("Unauthorized! Wrong username or password.");
                    break;
                default:
                    TackerError error = GSON.fromJson(response.getEntity(String.class), TackerError.class);
                    LOG.debug(error.toString());
                    break;
            }
        }
        return false;
    }

    @Override
    public boolean deleteSf(ServiceFunction sf) {

        String vnfId = sf.getName().getValue();
        WebResource webResource = client.resource(baseUri + ":" + tackerPort).path("/v1.0/vnfs/" + vnfId);
        ClientResponse response =
                webResource.type(javax.ws.rs.core.MediaType.APPLICATION_JSON).delete(ClientResponse.class);

        if (response != null) {
            switch (response.getStatus()) {
                case 200:
                    LOG.debug("VNF:" + vnfId + " successfully deleted.");
                    return true;
                case 404:
                    LOG.debug("404 - Not Found:" + response.toString());
                    return false;
                case 405:
                    LOG.debug("405 - Method not found: " + response.toString());
                    return false;
                default:
                    TackerError error = GSON.fromJson(response.getEntity(String.class), TackerError.class);
                    LOG.debug(error.toString());
                    break;
            }
        }
        return false;
    }

    @Override
    public ServiceStatistics getSfStatistics(ServiceFunction sf) {
        // TODO implement method
        return null;
    }

    @Override
    public void close() throws Exception {
        this.client.destroy();
    }

    public static TackerManagerBuilder builder() {
        return new TackerManagerBuilder();
    }

    public static class TackerManagerBuilder {

        private String baseUri;
        private int tackerPort;
        private int keystonePort;
        private Auth auth;

        public String getBaseUri() {
            return baseUri;
        }

        public TackerManagerBuilder setBaseUri(String baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public int getTackerPort() {
            return tackerPort;
        }

        public TackerManagerBuilder setTackerPort(int tackerPort) {
            this.tackerPort = tackerPort;
            return this;
        }

        public int getKeystonePort() {
            return keystonePort;
        }

        public TackerManagerBuilder setKeystonePort(int keystonePort) {
            this.keystonePort = keystonePort;
            return this;
        }

        public Auth getAuth() {
            return auth;
        }

        public TackerManagerBuilder setAuth(Auth auth) {
            this.auth = auth;
            return this;
        }

        public TackerManager build() {
            return new TackerManager(this);
        }
    }
}

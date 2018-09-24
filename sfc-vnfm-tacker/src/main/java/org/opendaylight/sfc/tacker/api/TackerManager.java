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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.Date;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.opendaylight.sfc.tacker.dto.Attributes;
import org.opendaylight.sfc.tacker.dto.Auth;
import org.opendaylight.sfc.tacker.dto.KeystoneRequest;
import org.opendaylight.sfc.tacker.dto.TackerError;
import org.opendaylight.sfc.tacker.dto.TackerRequest;
import org.opendaylight.sfc.tacker.dto.TackerResponse;
import org.opendaylight.sfc.tacker.dto.Token;
import org.opendaylight.sfc.tacker.dto.Vnf;
import org.opendaylight.sfc.tacker.util.DateDeserializer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TackerManager implements AutoCloseableSfcVnfManager {

    private static final Logger LOG = LoggerFactory.getLogger(TackerManager.class);
    private static final DateDeserializer DATE_DESERIALIZER = new DateDeserializer();
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, DATE_DESERIALIZER).create();
    private static final Integer CONNECT_TIMEOUT_MILLISEC = 7000;
    private static final Integer READ_TIMEOUT_MILLISEC = 5000;
    private final Client client;
    private final String baseUri;
    private final int tackerPort;
    private final int keystonePort;
    private Token token;
    private final Auth auth;

    private TackerManager(TackerManagerBuilder builder) {
        Preconditions.checkNotNull(builder.getBaseUri());
        Preconditions.checkArgument(builder.getTackerPort() != 0);
        Preconditions.checkArgument(builder.getKeystonePort() != 0);
        Preconditions.checkNotNull(builder.getAuth());

        this.baseUri = builder.getBaseUri();
        this.tackerPort = builder.getTackerPort();
        this.keystonePort = builder.getKeystonePort();
        this.auth = builder.getAuth();

        client = ClientBuilder.newBuilder().property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT_MILLISEC)
                .property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MILLISEC).build();
    }

    @Override
    public boolean createSf(ServiceFunctionType sfType) {

        Token authToken = getToken();
        if (authToken == null) {
            LOG.error("Failed to Acquire Authentication token!");
            return false;
        }

        WebTarget webTarget = client.target(baseUri + ":" + tackerPort).path("/v1.0/vnfs");
        TackerRequest tackerRequest = TackerRequest.builder()
            .setVnf(Vnf.builder()
                .setName(sfType.getType().getValue())
                .setAttributes(Attributes.builder().setServiceType(sfType.getType().getValue()).build())
                .build())
            .build();

        Response response = webTarget.request(MediaType.APPLICATION_JSON)
            .header("X-Auth-Token", authToken.getId())
            .header("X-Auth-Project-Id", authToken.getTenant().getName()).post(Entity.entity(
                    GSON.toJson(tackerRequest), MediaType.APPLICATION_JSON));

        if (response != null) {
            switch (response.getStatus()) {
                case 201:
                    String json = response.readEntity(String.class);
                    TackerResponse tackerResponse = GSON.fromJson(json, TackerResponse.class);
                    LOG.info("VNF successfully created.");
                    LOG.debug("Response {}", GSON.toJson(tackerResponse));
                    return true;
                case 401:
                    LOG.debug("Unauthorized! Wrong username or password.");
                    break;
                default:
                    TackerError error = GSON.fromJson(response.readEntity(String.class), TackerError.class);
                    LOG.debug("Error {}", error.toString());
                    break;
            }
        }
        return false;
    }

    @Override
    public boolean deleteSf(ServiceFunction sf) {

        Token authToken = getToken();
        if (authToken == null) {
            LOG.error("Failed to Acquire Authentication token!");
            return false;
        }

        String vnfId = sf.getName().getValue();
        WebTarget webTarget = client.target(baseUri + ":" + tackerPort).path("/v1.0/vnfs/" + vnfId);
        Response response = webTarget.request(MediaType.APPLICATION_JSON)
            .header("X-Auth-Token", authToken.getId())
            .header("X-Auth-Project-Id", authToken.getTenant().getName())
            .delete();

        if (response != null) {
            switch (response.getStatus()) {
                case 200:
                    LOG.info("VNF: {} successfully deleted.", vnfId);
                    return true;
                case 404:
                    LOG.debug("404 - Not Found: {}", response.toString());
                    return false;
                case 405:
                    LOG.debug("405 - Method not found: {}", response.toString());
                    return false;
                default:
                    TackerError error = GSON.fromJson(response.readEntity(String.class), TackerError.class);
                    LOG.debug("Error {}", error.toString());
                    break;
            }
        }
        return false;
    }

    @Override
    public StatisticByTimestamp getSfStatistics(ServiceFunction sf) {
        // TODO implement method
        return null;
    }

    @Override
    public void close() {
        this.client.close();
    }

    private Token getToken() {
        if (this.token == null) {
            this.token = requestToken();
        }
        if (this.token != null) {
            Date currentDate = new Date();
            if (!(currentDate.getTime() < token.getExpires().getTime()
                    && currentDate.getTime() >= token.getIssuedAt().getTime())) {
                this.token = null;
            }
        }
        return this.token;
    }

    private Token requestToken() {
        WebTarget webTarget = client.target(baseUri + ":" + keystonePort).path("/v2.0/tokens");
        KeystoneRequest keystoneRequest = new KeystoneRequest(this.auth);

        Response response = webTarget.request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(GSON.toJson(keystoneRequest), MediaType.APPLICATION_JSON));

        if (response != null) {
            switch (response.getStatus()) {
                case 200:
                    String json = response.readEntity(String.class);
                    JsonObject jsonObject =
                            GSON.fromJson(json, JsonObject.class).getAsJsonObject("access").getAsJsonObject("token");

                    LOG.debug("Authentication token successfully created.");
                    return GSON.fromJson(jsonObject, Token.class);
                default:
                    LOG.debug("Response {}", response.readEntity(String.class));
                    break;
            }
        }
        return null;
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

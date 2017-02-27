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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Date;

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
import org.opendaylight.sfc.vnfm.spi.SfcVnfManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TackerManager implements SfcVnfManager, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TackerManager.class);
    private static final DateDeserializer DATE_DESERIALIZER = new DateDeserializer();
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, DATE_DESERIALIZER).create();
    private static final Integer CONNECT_TIMEOUT_MILLISEC = 7000;
    private static final Integer READ_TIMEOUT_MILLISEC = 5000;
    private final Client client;
    private String baseUri;
    private int tackerPort;
    private int keystonePort;
    private Token token;
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

        client = ClientBuilder.newClient();
        client.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MILLISEC);
        client.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT_MILLISEC);
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
                .header("X-Auth-Project-Id", authToken.getTenant().getName())
                .post(Entity.json(tackerRequest));

        if (response != null) {
            switch (response.getStatus()) {
                case 201:
                    TackerResponse tackerResponse = response.readEntity(TackerResponse.class);
                    LOG.info("VNF successfully created.");
                    LOG.debug(GSON.toJson(tackerResponse));
                    return true;
                case 401:
                    LOG.debug("Unauthorized! Wrong username or password.");
                    break;
                default:
                    TackerError error = GSON.fromJson(response.readEntity(String.class), TackerError.class);
                    LOG.debug(error.toString());
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
                    LOG.info("VNF:" + vnfId + " successfully deleted.");
                    return true;
                case 404:
                    LOG.debug("404 - Not Found:" + response.toString());
                    return false;
                case 405:
                    LOG.debug("405 - Method not found: " + response.toString());
                    return false;
                default:
                    TackerError error = GSON.fromJson(response.readEntity(String.class), TackerError.class);
                    LOG.debug(error.toString());
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
    public void close() throws Exception {
        this.client.close();
    }

    private Token getToken() {
        if (this.token == null) {
            this.token = requestToken();
        }
        if (this.token != null) {
            Date currentDate = new Date();
            if (!(currentDate.getTime() < token.getExpires().getTime()
                    && currentDate.getTime() >= token.getIssued_at().getTime())) {
                this.token = null;
            }
        }
        return this.token;
    }

    private Token requestToken() {
        WebTarget target = client.target(baseUri + ":" + keystonePort).path("/v2.0/tokens");
        KeystoneRequest keystoneRequest = new KeystoneRequest(this.auth);

        Response response = target.request()
            .post(Entity.json(keystoneRequest));

        if (response != null) {
            switch (response.getStatus()) {
                case 200:
                    String json = response.readEntity(String.class);
                    JsonObject jsonObject =
                            GSON.fromJson(json, JsonObject.class).getAsJsonObject("access").getAsJsonObject("token");

                    Token token = GSON.fromJson(jsonObject, Token.class);
                    LOG.debug("Authentication token successfully created.");
                    return token;
                default:
                    LOG.debug(response.readEntity(String.class));
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

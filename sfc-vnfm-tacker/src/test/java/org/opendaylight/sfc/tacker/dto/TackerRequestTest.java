/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TackerRequestTest {

    private static final Logger LOG = LoggerFactory.getLogger(TackerRequestTest.class);
    private static final Gson GSON = new Gson();
    private static TackerRequest tackerRequest;

    @Before
    public void init() {
        tackerRequest = TackerRequest.builder()
            .setAuth(Auth.builder()
                .setTenantName("admin")
                .setPasswordCredentials(new PasswordCredentials("admin", "devstack"))
                .build())
            .setVnf(Vnf.builder().setVnfdId("d770ddd7-6014-4191-92d8-a2cd7a6cecd8").build())
            .build();
    }

    @Test
    public void testSimpleJsonConversion() {
        String pathToJsonRequest = "/JsonData/request.json";
        String jsonFileString = null;
        String jsonObjectString;

        try {
            URL url = getClass().getResource(pathToJsonRequest);
            Path providerPath = Paths.get(url.toURI());
            byte[] encoded = Files.readAllBytes(providerPath);
            jsonFileString = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            LOG.error("Failed to...", e);
        }

        // convert predefined json string from file to TackerRequest object

        TackerRequest jsonFileRequest = GSON.fromJson(jsonFileString,TackerRequest.class);
        Assert.assertNotNull(jsonFileRequest);

        // convert TackerRequest object to json string and back to TackerRequest object
        jsonObjectString = GSON.toJson(tackerRequest);
        TackerRequest jsonObjectRequest = GSON.fromJson(jsonObjectString,TackerRequest.class);
        Assert.assertNotNull(jsonObjectRequest);

        // convert TackerRequest objects to json format and compare
        Assert.assertEquals(GSON.toJson(tackerRequest), GSON.toJson(jsonObjectRequest));
        Assert.assertEquals(GSON.toJson(tackerRequest), GSON.toJson(jsonFileRequest));
    }
}

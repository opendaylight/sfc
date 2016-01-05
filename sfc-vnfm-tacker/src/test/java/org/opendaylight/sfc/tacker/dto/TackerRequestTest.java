/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TackerRequestTest {

    private static final Logger LOG = LoggerFactory.getLogger(TackerRequest.class);
    private static TackerRequest tackerRequest;

    @Before
    public void init() {
        tackerRequest = new TackerRequest.TackerRequestBuilder()
            .setAuth(new Auth.AuthBuilder().setTenantName("admin")
                .setPasswordCredentials(new PasswordCredentials("admin", "devstack"))
                .build())
            .setVnf(new Vnf.VnfBuilder().setVnfd_id("d770ddd7-6014-4191-92d8-a2cd7a6cecd8").build())
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
        TackerRequest jsonFileRequest = new TackerRequest(jsonFileString);
        Assert.assertNotNull(jsonFileRequest);

        // convert TackerRequest object to json string and back to TackerRequest object
        jsonObjectString = tackerRequest.toJson();
        TackerRequest jsonObjectRequest = new TackerRequest(jsonObjectString);
        Assert.assertNotNull(jsonObjectRequest);

        // convert TackerRequest objects to json format and compare
        Assert.assertEquals(tackerRequest.toJson(), jsonObjectRequest.toJson());
        Assert.assertEquals(tackerRequest.toJson(), jsonFileRequest.toJson());
    }

    @Test
    public void testEmptyJson() {
        TackerRequest request = new TackerRequest("");
        Assert.assertNull(request.vnf);
        Assert.assertNull(request.auth);
    }
}

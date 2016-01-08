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

public class TackerResponseTest {

    private static final Logger LOG = LoggerFactory.getLogger(TackerResponseTest.class);
    private static TackerResponse tackerResponse;

    @Before
    public void init() {
        tackerResponse = TackerResponse.builder()
            .setVnf(Vnf.builder()
                .setStatus("PENDING_CREATE")
                .setName("")
                .setTenant_id("4dd6c1d7b6c94af980ca886495bcfed0")
                .setDescription("OpenWRT with services")
                .setInstance_id("4f0d6222-afa0-4f02-8e19-69e7e4fd7edc")
                .setMgmt_url(null)
                .setAttributes(Attributes.builder()
                    .setService_type("firewall")
                    .setHeat_template("description: OpenWRT with services\n"
                            + "                <sample_heat_template> type: OS::Nova::Server\n")
                    .setMonitoring_policy("noop")
                    .setFailure_policy("noop")
                    .build())
                .setId("e3158513-92f4-4587-b949-70ad0bcbb2dd")
                .setVnfd_id("247b045e-d64f-4ae0-a3b4-8441b9e5892c")
                .build())
            .build();
    }

    @Test
    public void testSimpleJsonConversion() {
        String pathToJsonRequest = "/JsonData/response.json";
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

        TackerResponse jsonFileResponse = new TackerResponse("");
        Assert.assertNull(jsonFileResponse.getVnf());

        // convert predefined json string from file to TackerRequest object
        jsonFileResponse = new TackerResponse(jsonFileString);
        Assert.assertNotNull(jsonFileResponse);

        // convert TackerRequest object to json string and back to TackerRequest object
        jsonObjectString = tackerResponse.toJson();
        TackerResponse jsonObjectRequest = new TackerResponse(jsonObjectString);
        Assert.assertNotNull(jsonObjectRequest);

        // convert TackerRequest objects to json format and compare
        Assert.assertEquals(tackerResponse.toJson(), jsonObjectRequest.toJson());
        Assert.assertEquals(tackerResponse.toJson(), jsonFileResponse.toJson());
    }
}

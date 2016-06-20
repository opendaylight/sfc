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


public class TackerResponseTest {

    private static final Logger LOG = LoggerFactory.getLogger(TackerResponseTest.class);
    private static final Gson GSON = new Gson();
    private static TackerResponse tackerResponse;

    @Before
    public void init() {
        tackerResponse = TackerResponse.builder()
            .setVnf(Vnf.builder()
                .setStatus("PENDING_CREATE")
                .setName("")
                .setTenantId("4dd6c1d7b6c94af980ca886495bcfed0")
                .setDescription("OpenWRT with services")
                .setInstanceId("4f0d6222-afa0-4f02-8e19-69e7e4fd7edc")
                .setMgmtUrl(null)
                .setAttributes(Attributes.builder()
                    .setServiceType("firewall")
                    .setHeatTemplate("description: OpenWRT with services\n"
                            + "                <sample_heat_template> type: OS::Nova::Server\n")
                    .setMonitoringPolicy("noop")
                    .setFailurePolicy("noop")
                    .build())
                .setId("e3158513-92f4-4587-b949-70ad0bcbb2dd")
                .setVnfdId("247b045e-d64f-4ae0-a3b4-8441b9e5892c")
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

        // convert predefined json string from file to TackerRequest object
        TackerResponse jsonFileResponse = GSON.fromJson(jsonFileString,TackerResponse.class);
        Assert.assertNotNull(jsonFileResponse);

        // convert TackerRequest object to json string and back to TackerRequest object
        jsonObjectString = GSON.toJson(tackerResponse);
        TackerResponse jsonObjectRequest = GSON.fromJson(jsonObjectString,TackerResponse.class);
        Assert.assertNotNull(jsonObjectRequest);

        // convert TackerRequest objects to json format and compare
        Assert.assertEquals(GSON.toJson(tackerResponse), GSON.toJson(jsonObjectRequest));
        Assert.assertEquals(GSON.toJson(tackerResponse), GSON.toJson(jsonFileResponse));
    }
}

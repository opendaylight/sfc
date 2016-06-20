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


public class TackerErrorTest {

    private static final String ERROR_MESSAGE = "device 464654654 could not be found";
    private static final String ERROR_DETAIL = "";
    private static final String ERROR_TYPE = "DeviceNotFound";
    private static final Gson GSON = new Gson();
    private static final Logger LOG = LoggerFactory.getLogger(TackerErrorTest.class);
    private static TackerError error;

    @Before
    public void init() {
        error = new TackerError(
                Error.builder().setMessage(ERROR_MESSAGE).setType(ERROR_TYPE).setDetail(ERROR_DETAIL).build());
    }

    @Test
    public void simpleConversionTest() {
        String pathToJsonError = "/JsonData/tackerError.json";
        String jsonFileString = null;

        try {
            URL url = getClass().getResource(pathToJsonError);
            Path providerPath = Paths.get(url.toURI());
            byte[] encoded = Files.readAllBytes(providerPath);
            jsonFileString = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            LOG.error("Failed to...", e);
        }
        TackerError testError = GSON.fromJson(jsonFileString, TackerError.class);

        Assert.assertTrue(testError.getMessage().equals(ERROR_MESSAGE));
        Assert.assertTrue(testError.getType().equals(ERROR_TYPE));
        Assert.assertTrue(testError.getDetail().equals(ERROR_DETAIL));

        String jsonError = GSON.toJson(testError);

        testError = GSON.fromJson(jsonError, TackerError.class);

        Assert.assertNotNull(testError);
        Assert.assertTrue(testError.getMessage().equals(error.getMessage()));
        Assert.assertTrue(testError.getType().equals(error.getType()));
        Assert.assertTrue(testError.getDetail().equals(error.getDetail()));
    }
}

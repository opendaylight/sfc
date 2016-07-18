/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.tacker.util.DateDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TokenTest {

    private static final Logger LOG = LoggerFactory.getLogger(TokenTest.class);
    private static final DateDeserializer DATE_DESERIALIZER = new DateDeserializer();
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, DATE_DESERIALIZER).create();
    private static Token token;

    @Before
    public void init() {
        Date issuedAt = new Date();
        Date expires = new Date();

        try {
            issuedAt = new SimpleDateFormat(DateDeserializer.DATE_FORMAT_STANDARD, Locale.US)
                .parse("2016-01-14T12:45:47.035+0000");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            expires = new SimpleDateFormat(DateDeserializer.DATE_FORMAT_ZULU, Locale.US)
                .parse("2016-01-14T13:45:47+0000");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        token = Token.builder()
            .setIssued_at(issuedAt)
            .setExpires(expires)
            .setId("7a17dc67ba284ab2beeccc21ce198626")
            .setTenant(Tenant.builder()
                .setDesription(null)
                .setEnabled(true)
                .setId("f3a250db7c374654854f56ad60caea66")
                .setName("admin")
                .build())
            .setAudit_ids(new String[] {"LUMVW2kmQU29kwkZv8VCZg"})
            .build();

    }

    @Test
    public void testSimpleJsonConversion() {
        String pathToJsonRequest = "/JsonData/keystone.json";
        String jsonFileString = null;

        try {
            URL url = getClass().getResource(pathToJsonRequest);
            Path providerPath = Paths.get(url.toURI());
            byte[] encoded = Files.readAllBytes(providerPath);
            jsonFileString = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            LOG.error("Failed to...", e);
        }

        // convert predefined json string from file to Token object ignoring other elements in json
        JsonObject jsonObject =
                GSON.fromJson(jsonFileString, JsonObject.class).getAsJsonObject("access").getAsJsonObject("token");

        Token jsonFileToken = GSON.fromJson(jsonObject, Token.class);
        Assert.assertNotNull(jsonFileToken);

        // check Tenant data
        Assert.assertTrue(jsonFileToken.getTenant().getId().equals(token.getTenant().getId()));
        Assert.assertTrue(jsonFileToken.getTenant().getDesription() == null);
        Assert.assertTrue(jsonFileToken.getTenant().getName().equals(token.getTenant().getName()));
        Assert.assertTrue(jsonFileToken.getTenant().isEnabled() == token.getTenant().isEnabled());

        // check Token data
        Assert.assertTrue(jsonFileToken.getId().equals(token.getId()));
        Assert.assertTrue(jsonFileToken.getAudit_ids()[0].equals(token.getAudit_ids()[0]));
        Assert.assertTrue(jsonFileToken.getExpires().compareTo((token.getExpires())) == 0);
        Assert.assertTrue(jsonFileToken.getIssued_at().compareTo((token.getIssued_at())) == 0);
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.config;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class represents configuration data from file configuration/startup/sfc_provider_config.json
 * <p/>
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-07-29
 */
public class SfcProviderConfig {
    private static final SfcProviderConfig INSTANCE = new SfcProviderConfig();

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderConfig.class);

    private JSONObject providerConfig;

    private SfcProviderConfig() {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get("configuration/startup/sfc_provider_config.json"));
            String jsonString = new String(encoded, StandardCharsets.UTF_8);
            try {
                this.providerConfig = new JSONObject(jsonString);
            } catch (JSONException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
                this.providerConfig = new JSONObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SfcProviderConfig getInstance() {
        return INSTANCE;
    }

    public JSONObject getProviderConfig() {
        return this.providerConfig;
    }

    public JSONObject getBootstrap() {
        try {
            return this.providerConfig.getJSONObject("bootstrap");
        } catch (JSONException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

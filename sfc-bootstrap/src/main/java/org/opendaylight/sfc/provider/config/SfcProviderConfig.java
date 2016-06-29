/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class represents configuration data from file configuration/startup/sfc_provider_config.json
 * <p>
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-07-29
 */
public class SfcProviderConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderConfig.class);
    private static final Path configFilePath = Paths.get("configuration/startup/sfc_provider_config.json");
    private static final SfcProviderConfig INSTANCE = new SfcProviderConfig();

    private JSONObject providerConfig;

    public static SfcProviderConfig getInstance() {
        return INSTANCE;
    }

    public boolean readConfigFile () {
        if (Files.exists(configFilePath)) {
            try {
                byte[] encoded = Files.readAllBytes(configFilePath);
                String jsonString = new String(encoded, StandardCharsets.UTF_8);
                try {
                    this.providerConfig = new JSONObject(jsonString);
                } catch (JSONException e) {
                    LOG.error(e.getMessage());
                    return false;
                }
            } catch (IOException e) {
                LOG.error("Error reading SFC Provider config file");
                return false;
            }
        } else {
            LOG.warn("SFC Provider file not found");
            return false;
        }
        return true;
    }

    public JSONObject getProviderConfig() {
        return this.providerConfig;
    }

    public JSONObject getJsonBootstrapObject() {
        try {
            return this.providerConfig.getJSONObject("bootstrap");
        } catch (JSONException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

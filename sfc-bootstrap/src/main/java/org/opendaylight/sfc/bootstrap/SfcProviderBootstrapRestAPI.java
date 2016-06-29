/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.bootstrap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.sfc.provider.config.SfcProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Filling the data store with prepared data through the RESTconf API.
 * <p>
 * Reads config files containing fully prepared RESTconf data from the configuration/startup/
 * (in the run script subdirectory). After every Maven "clean" command those files will be deleted
 * along with the whole /target directory, and after a rebuild they will be copied from
 * sfc-distribution/src/main/resources/configuration/startup
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-07-24
 */
public class SfcProviderBootstrapRestAPI extends SfcProviderAbstractRestAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderBootstrapRestAPI.class);

    SfcProviderBootstrapRestAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderBootstrapRestAPI getPutBootstrapData(Object[] params, Class[] paramsTypes) {
        return new SfcProviderBootstrapRestAPI(params, paramsTypes, "putBootstrapData");
    }

    public void putBootstrapData() {

        SfcProviderConfig providerConfig = SfcProviderConfig.getInstance();
        if (!providerConfig.readConfigFile()) {
            return;
        }

        JSONObject jo = providerConfig.getJsonBootstrapObject();

        JSONArray files;

        try {
            final String CONFIG_FILES_DIR = jo.getString("bootstrapDataDir");
            final String CONFIG_DATA_URL = jo.getString("configDataUrl");
            final String CONFIG_DATA_MIME_TYPE = jo.getString("configDataMimeType");
            final HTTPBasicAuthFilter basicAuthFilter = new HTTPBasicAuthFilter("admin", "admin");
            files = jo.getJSONArray("files");


            ClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create(clientConfig);
            client.addFilter(basicAuthFilter);

            if (files.length() > 0) {
                for (int i = 0; i < files.length(); i++) {
                    JSONObject o = files.getJSONObject(i);
                    String json;
                    String filename = o.getString("name");
                    String urlpath = o.getString("urlpath");
                    try {
                        byte[] encoded = Files.readAllBytes(Paths.get(CONFIG_FILES_DIR + filename));
                        json = new String(encoded, StandardCharsets.UTF_8);
                    } catch (FileNotFoundException e) {
                        LOG.error("\n***** Configuration file {} not found, passing *****\n", filename);
                        continue;
                    } catch (IOException e) {
                        LOG.error("\n***** Configuration file {} could not be" +
                                " read: {}", filename, e.getMessage());
                        break;
                    }
                    try {
                        new JSONObject(json);
                        ClientResponse putClientResponse = client
                                .resource(CONFIG_DATA_URL + urlpath)
                                .type(CONFIG_DATA_MIME_TYPE)
                                .put(ClientResponse.class, json);
                        putClientResponse.close();
                        if (putClientResponse.getStatus() != 200){
                            LOG.error("\n***** Unsuccessful PUT for file {}, HTTP response code {} *****\n", filename,
                                    putClientResponse.getStatus());
                        }
                    } catch (JSONException e) {
                        LOG.error("\n***** Invalid JSON in file {}, passing *****\n", filename);
                    }

                }
            }
        } catch (JSONException e) {
            LOG.warn("failed to ...." , e);
        }
    }

}

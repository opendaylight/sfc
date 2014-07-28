package org.opendaylight.sfc.provider.bootstrap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.opendaylight.sfc.provider.SfcProviderAbstractRestAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Filling the data store with prepared data through the RESTconf API.
 * <p/>
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

    private static final String CONFIG_FILES_DIR = "configuration/startup/";
    private static final String CONFIG_DATA_URL = "http://localhost:8080/restconf/config/";
    private static final String CONFIG_DATA_MIME_TYPE = "application/json";

    SfcProviderBootstrapRestAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderBootstrapRestAPI getBootstrapTest(Object[] params, Class[] paramsTypes) {
        return new SfcProviderBootstrapRestAPI(params, paramsTypes, "bootstrapTest");
    }

    public void bootstrapTest(ServiceFunctions sfs) {

        // RESTconf URL and corresponding JSON file
        final class ConfigFileData {
            public String urlpath;
            public String filename;

            public ConfigFileData(String urlpath, String filename) {
                this.urlpath = urlpath;
                this.filename = filename;
            }
        }

        // the order of the files will be important when ODL consistency control is implemented
        List<ConfigFileData> configList = new ArrayList<>();
        configList.add(new ConfigFileData(
                "service-function-forwarder:service-function-forwarders", "service-function-forwarders.json"));
        configList.add(new ConfigFileData(
                "service-function:service-functions", "service-functions.json"));
        configList.add(new ConfigFileData(
                "service-function-chain:service-function-chains", "service-function-chains.json"));
        configList.add(new ConfigFileData(
                "service-node:service-nodes", "service-nodes.json"));

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        for (ConfigFileData config : configList) {
            String json = "";
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(CONFIG_FILES_DIR + config.filename));
                json = new String(encoded, StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                LOG.info("\n***** Configuration file {} not found, passing *****\n", config.filename);
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            if (!"".equals(json)) {
                ClientResponse putClientResponse = client
                        .resource(CONFIG_DATA_URL + config.urlpath)
                        .type(CONFIG_DATA_MIME_TYPE)
                        .put(ClientResponse.class, json);
                putClientResponse.close();
            }
        }
    }

}

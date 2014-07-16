/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is the DataListener for SFF changes.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderRestAPI implements Runnable {

    private ServiceFunctionForwarders serviceFunctionForwarders;
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    public enum OperationType {CREATE, DELETE}

    private OperationType operation = OperationType.CREATE;

    SfcProviderRestAPI (ServiceFunctionForwarders sff, OperationType type) {
        this.serviceFunctionForwarders = sff;
        this.operation = type;
    }

    public void putServiceFunctionForwarder(ServiceFunctionForwarders serviceFunctionForwarders) {

        /*
        ObjectWriter ow = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true).writer().withDefaultPrettyPrinter();
        String jsonsff = null;
        try {
            jsonsff  = ow.writeValueAsString(serviceFunctionForwarders);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ClientConfig clientConfig = new DefaultClientConfig();
        //clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        jsonsff = jsonsff.replaceAll("\\s*\"implementedInterface.*", "");
        */

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse = client
                .resource("http://localhost:8080/restconf/config/service-function-forwarder:service-function-forwarders/")
                .accept("application/json")
                .get(ClientResponse.class);


        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();

        ClientResponse putClientResponse= client
                .resource("http://localhost:5000/paths").type("application/json")
                .put(ClientResponse.class, jsonOutput);

        putClientResponse.close();

    }

    public static  SfcProviderRestAPI getSfcProviderRestAPIPut(ServiceFunctionForwarders sff) {
        return new SfcProviderRestAPI(sff, OperationType.CREATE);
    }

    @Override
    public void run() {
        switch (operation) {
            case CREATE:
                putServiceFunctionForwarder(serviceFunctionForwarders);
                break;
            case DELETE:
                break;
        }

    }
}

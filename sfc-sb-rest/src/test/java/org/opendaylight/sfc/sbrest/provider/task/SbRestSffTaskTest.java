/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.provider.task;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.sbrest.json.SffExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

/**
 * This class contains unit tests for SbRestSffTask
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-18
 */

public class SbRestSffTaskTest {

    private static final String SFF_NAME = "Dummy_SFF";
    private static final String REST_URI = "http://localhost:5000";

    private ExecutorService executorService;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testSbRestSffTask() throws IOException {
        SbRestSffTask sbRestSffTask =
                new SbRestSffTask(RestOperation.PUT, this.buildServiceFunctionForwarder(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSffTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildServiceFunctionForwarderObjectNode()));
        assertTrue(sbRestSffTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSffTaskEmpty() throws IOException {
        SbRestSffTask sbRestSffTask =
                new SbRestSffTask(RestOperation.PUT, new ServiceFunctionForwarderBuilder().build(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSffTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildServiceFunctionForwarderTopNode()));
        assertNull(sbRestSffTask.restUriList);
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(SFF_NAME);
        serviceFunctionForwarderBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionForwarderBuilder.build();
    }

    private ObjectNode buildServiceFunctionForwarderObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sffNode = mapper.createObjectNode();
        sffNode.put(SffExporterFactory._NAME, SFF_NAME);
        sffNode.put(SffExporterFactory._REST_URI, REST_URI);

        ArrayNode sffArrayNode = mapper.createArrayNode();
        sffArrayNode.add(sffNode);

        topNode.put(SffExporterFactory._SERVICE_FUNCTION_FORWARDER, sffArrayNode);
        return topNode;
    }

    private ObjectNode buildServiceFunctionForwarderTopNode() {
        ObjectNode topNode = mapper.createObjectNode();
        ObjectNode sffNode = mapper.createObjectNode();
        ArrayNode sffArrayNode = mapper.createArrayNode();
        sffArrayNode.add(sffNode);

        topNode.put(SffExporterFactory._SERVICE_FUNCTION_FORWARDER, sffArrayNode);
        return topNode;
    }

}

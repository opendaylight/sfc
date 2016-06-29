/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.provider.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.sbrest.json.SffExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SbRestSffTask
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-18
 */

public class SbRestSffTaskTest {

    private static final SffName SFF_NAME = new SffName("Dummy_SFF");
    private static final String REST_URI = "http://localhost:5000";

    private ExecutorService executorService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testSbRestSffTask() throws IOException {
        SbRestSffTask sbRestSffTask =
                new SbRestSffTask(RestOperation.PUT, this.buildServiceFunctionForwarder(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSffTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionForwarderObjectNode()));
        assertTrue("Must be true", sbRestSffTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSffTask1() throws IOException {
        SbRestSffTask sbRestSffTask =
                new SbRestSffTask(RestOperation.DELETE, this.buildServiceFunctionForwarder(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSffTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionForwarderObjectNode1()));
        assertTrue("Must be true", sbRestSffTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSffTaskEmpty() throws IOException {
        SbRestSffTask sbRestSffTask =
                new SbRestSffTask(RestOperation.PUT, new ServiceFunctionForwarderBuilder().build(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSffTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionForwarderTopNode()));
        assertNull("Must be null", sbRestSffTask.restUriList);
    }

    // build service function forwarder, which is needed to create SbRestSffTask object
    private ServiceFunctionForwarder buildServiceFunctionForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(SFF_NAME);
        serviceFunctionForwarderBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionForwarderBuilder.build();
    }

    // returns object node with name & rest uri, uses FullTest.json
    private ObjectNode buildServiceFunctionForwarderObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sffNode = mapper.createObjectNode();
        sffNode.put(SffExporterFactory._NAME, SFF_NAME.getValue());
        sffNode.put(SffExporterFactory._REST_URI, REST_URI);

        ArrayNode sffArrayNode = mapper.createArrayNode();
        sffArrayNode.add(sffNode);

        topNode.put(SffExporterFactory._SERVICE_FUNCTION_FORWARDER, sffArrayNode);
        return topNode;
    }

    // returns object node with name only, uses NameOnly.json
    private ObjectNode buildServiceFunctionForwarderObjectNode1() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sffNode = mapper.createObjectNode();
        sffNode.put(SffExporterFactory._NAME, SFF_NAME.getValue());

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

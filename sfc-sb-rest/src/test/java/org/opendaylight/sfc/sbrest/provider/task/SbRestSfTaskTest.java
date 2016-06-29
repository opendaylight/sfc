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
import org.opendaylight.sfc.sbrest.json.SfExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SbRestSfTask
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-17
 */

public class SbRestSfTaskTest {

    private static final SfName SF_NAME = new SfName("Dummy_SF");
    private static final String REST_URI = "http://localhost:5000";
    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testSbRestSfTask() throws IOException {
        SbRestSfTask sbRestSfTask = new SbRestSfTask(RestOperation.PUT, this.buildServiceFunction(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionObjectNode()));
        assertTrue("Must be true", sbRestSfTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfTask1() throws IOException {
        SbRestSfTask sbRestSfTask =
                new SbRestSfTask(RestOperation.DELETE, this.buildServiceFunction(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(buildServiceFunctionObjectNode1()));
        assertTrue("Must be true", sbRestSfTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfTaskEmpty() throws IOException {
        SbRestSfTask sbRestSfTask =
                new SbRestSfTask(RestOperation.PUT, new ServiceFunctionBuilder().build(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionTopNode()));
        assertNull("Must be null", sbRestSfTask.restUriList);
    }

    // build service function, which is needed to create SbRestSfTask object
    private ServiceFunction buildServiceFunction() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME);
        serviceFunctionBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionBuilder.build();
    }

    // returns object node with name & rest uri, uses FullTest.json
    private ObjectNode buildServiceFunctionObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfNode = mapper.createObjectNode();
        sfNode.put(SfExporterFactory._NAME, SF_NAME.getValue());
        sfNode.put(SfExporterFactory._REST_URI, REST_URI);

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfNode);

        topNode.put(SfExporterFactory._SERVICE_FUNCTION, arrayNode);
        return topNode;
    }

    // returns object node with name only, uses NameOnly.json
    private ObjectNode buildServiceFunctionObjectNode1() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfNode = mapper.createObjectNode();
        sfNode.put(SfExporterFactory._NAME, SF_NAME.getValue());

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfNode);

        topNode.put(SfExporterFactory._SERVICE_FUNCTION, arrayNode);
        return topNode;
    }

    private ObjectNode buildServiceFunctionTopNode() {
        ObjectNode topNode = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();
        ObjectNode sfNode = mapper.createObjectNode();

        arrayNode.add(sfNode);
        topNode.put(SfExporterFactory._SERVICE_FUNCTION, arrayNode);
        return topNode;
    }
}

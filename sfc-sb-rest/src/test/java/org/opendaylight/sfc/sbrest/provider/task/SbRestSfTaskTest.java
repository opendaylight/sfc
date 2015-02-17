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

import org.opendaylight.sfc.sbrest.json.SfExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

/**
 * This class contains unit tests for SbRestSfTask
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-17
 */

public class SbRestSfTaskTest {

    private static final String SF_NAME = "Dummy_SF";
    private static final String REST_URI = "http://localhost:5000";

    private ExecutorService executorService;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testSbRestSfTask() throws IOException {
        SbRestSfTask sbRestSfTask = new SbRestSfTask(RestOperation.PUT, this.buildServiceFunction(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildServiceFunctionObjectNode()));
        assertTrue(sbRestSfTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfTaskEmpty() throws IOException {
        SbRestSfTask sbRestSfTask = new SbRestSfTask(RestOperation.PUT, new ServiceFunctionBuilder().build(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildServiceFunctionTopNode()));
        assertNull(sbRestSfTask.restUriList);
    }

    private ServiceFunction buildServiceFunction() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME);
        serviceFunctionBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionBuilder.build();
    }

    private ObjectNode buildServiceFunctionObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfNode = mapper.createObjectNode();
        sfNode.put(SfExporterFactory._NAME, SF_NAME);
        sfNode.put(SfExporterFactory._REST_URI, REST_URI);

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

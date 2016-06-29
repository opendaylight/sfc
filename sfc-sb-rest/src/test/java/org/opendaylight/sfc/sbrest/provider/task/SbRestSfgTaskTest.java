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
import org.opendaylight.sfc.sbrest.json.SfgExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * This class contains unit tests for SbRestSfgTaskTest
 *
 * @author Vladimir Lavor
 * @version 0.1
 * @see org.opendaylight.sfc.sbrest.provider.task.SbRestSfgTask
 * @since 2015-06-1
 */

public class SbRestSfgTaskTest {

    private static final String SFG_NAME = "Dummy_SFG";
    private static final String REST_URI = "http://localhost:5000";
    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    // SbRestAbstractClass creates string jsonObject from dataObject (service function group) & set
    // Rest uri list
    // contain of jsonObject also depends on rest operation
    // this jsonObject is then compared with object node created in this class
    public void testSbRestSfgTask() throws IOException {
        SbRestSfgTask sbRestSfgTask =
                new SbRestSfgTask(RestOperation.PUT, this.buildServiceFunctionGroup(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfgTask.jsonObject);
        assertNotNull("Must not be null", sbRestSfgTask.restUriList);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionGroupObjectNode()));
        assertTrue("Must be true", sbRestSfgTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfgTask1() throws IOException {
        SbRestSfgTask sbRestSfgTask =
                new SbRestSfgTask(RestOperation.DELETE, this.buildServiceFunctionGroup1(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfgTask.jsonObject);
        assertNull("Must be null", sbRestSfgTask.restUriList);
        assertTrue("Must be true", jsonObject.equals(buildServiceFunctionGroupObjectNode1()));
    }

    // build service function group, which is needed to create SbRestSfgTask object
    private ServiceFunctionGroup buildServiceFunctionGroup() {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        serviceFunctionGroupBuilder.setName(SFG_NAME);
        serviceFunctionGroupBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionGroupBuilder.build();
    }

    private ServiceFunctionGroup buildServiceFunctionGroup1() {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        serviceFunctionGroupBuilder.setName(SFG_NAME);

        return serviceFunctionGroupBuilder.build();
    }

    // returns object node with name & rest uri, uses FullTest.json
    private ObjectNode buildServiceFunctionGroupObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();
        ObjectNode sfgNode = mapper.createObjectNode();
        sfgNode.put(SfgExporterFactory._NAME, SFG_NAME);
        sfgNode.put(SfgExporterFactory._REST_URI, REST_URI);
        ArrayNode arrayNode = mapper.createArrayNode();

        arrayNode.add(sfgNode);
        topNode.put(SfgExporterFactory._SERVICE_FUNCTION_GROUP, arrayNode);
        return topNode;
    }

    // returns object node with name only, uses NameOnly.json
    private ObjectNode buildServiceFunctionGroupObjectNode1() {
        ObjectNode topNode = mapper.createObjectNode();
        ObjectNode sfgNode = mapper.createObjectNode();
        sfgNode.put(SfExporterFactory._NAME, SFG_NAME);
        ArrayNode arrayNode = mapper.createArrayNode();

        arrayNode.add(sfgNode);
        topNode.put(SfgExporterFactory._SERVICE_FUNCTION_GROUP, arrayNode);
        return topNode;
    }
}

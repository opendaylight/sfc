/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.opendaylight.sfc.sbrest.json.SfsExporterFactory;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;


import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SbRestSfsTaskTest
 *
 * @author Hongli Chen (honglix.chen@intel.com)
 * @version 0.1
 * @see SfsExporterFactory;
 * <p/>
 * @since 2015-09-21
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcProviderServiceFunctionAPI.class)
public class SbRestSfsTaskTest {

    private static final String SFS_NAME = "Dummy_SFS";
    private static final String REST_URI = "http://localhost:5000";

    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        Mockito.when(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(SFS_NAME))
                .thenReturn(this.buildServiceFunction());
    }

    @Test
    public void testSbRestSfsTask() throws IOException {
        SbRestSfsTask sbRestSfsTask = new SbRestSfsTask(RestOperation.PUT, this.buildServiceFunctionState(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfsTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionStateObjectNode()));
        assertTrue("Must be true", sbRestSfsTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfsTask1() throws IOException {
        SbRestSfsTask sbRestSfsTask = new SbRestSfsTask(RestOperation.DELETE, this.buildServiceFunctionState(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfsTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(buildServiceFunctionStateObjectNode1()));
        assertTrue("Must be true", sbRestSfsTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfsTaskEmpty() throws IOException {
        SbRestSfsTask sbRestSfsTask =
                new SbRestSfsTask(RestOperation.PUT, new ServiceFunctionStateBuilder().build(), executorService);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        Mockito.when(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(SFS_NAME))
                .thenReturn(new ServiceFunctionBuilder().build());

        JsonNode jsonObject = mapper.readTree(sbRestSfsTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionStateTopNode()));
        assertNull("Must be null", sbRestSfsTask.restUriList);
    }

    private ServiceFunction buildServiceFunction() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SFS_NAME);
        serviceFunctionBuilder.setRestUri(new Uri(REST_URI));
        return serviceFunctionBuilder.build();
    }

    //build service function, which is needed to create SbRestSfsTask object
    private ServiceFunctionState buildServiceFunctionState() {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName(SFS_NAME);

        return serviceFunctionStateBuilder.build();
    }

    //returns object node with name & rest uri, uses FullTest.json
    private ObjectNode buildServiceFunctionStateObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfsNode = mapper.createObjectNode();
        sfsNode.put(SfsExporterFactory._NAME, SFS_NAME);

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfsNode);

        topNode.put(SfsExporterFactory._SERVICE_FUNCTION_STATE, arrayNode);
        return topNode;
    }

    //returns object node with name only, uses NameOnly.json
    private ObjectNode buildServiceFunctionStateObjectNode1() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfsNode = mapper.createObjectNode();
        sfsNode.put(SfsExporterFactory._NAME, SFS_NAME);

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfsNode);

        topNode.put(SfsExporterFactory._SERVICE_FUNCTION_STATE, arrayNode);
        return topNode;
    }

    private ObjectNode buildServiceFunctionStateTopNode() {
        ObjectNode topNode = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();
        ObjectNode sfsNode = mapper.createObjectNode();

        arrayNode.add(sfsNode);
        topNode.put(SfsExporterFactory._SERVICE_FUNCTION_STATE, arrayNode);
        return topNode;
    }
}

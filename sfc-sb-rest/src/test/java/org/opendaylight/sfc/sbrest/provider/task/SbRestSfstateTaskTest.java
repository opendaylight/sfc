/*
 * Copyright (c) 2015 Intel Corp. and others. All rights reserved.
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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.sbrest.json.SfstateExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SbRestSfstateTaskTest
 *
 * @author Hongli Chen (honglix.chen@intel.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sbrest.json.SfstateExporterFactory
 * @since 2015-09-21
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcProviderServiceFunctionAPI.class)
public class SbRestSfstateTaskTest {

    private static final SfName SFSTATE_NAME = new SfName("Dummy_SFSTATE");
    private static final String REST_URI = "http://localhost:5000";

    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        Mockito.when(SfcProviderServiceFunctionAPI.readServiceFunction(SFSTATE_NAME))
            .thenReturn(this.buildServiceFunction());
    }

    @Test
    public void testSbRestSfstateTask() throws IOException {
        SbRestSfstateTask sbRestSfstateTask =
                new SbRestSfstateTask(RestOperation.PUT, this.buildServiceFunctionState(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfstateTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionStateObjectNode()));
        assertTrue("Must be true", sbRestSfstateTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfstateTask1() throws IOException {
        SbRestSfstateTask sbRestSfstateTask =
                new SbRestSfstateTask(RestOperation.DELETE, this.buildServiceFunctionState(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfstateTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(buildServiceFunctionStateObjectNode1()));
        assertTrue("Must be true", sbRestSfstateTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfstateTaskEmpty() throws IOException {
        SbRestSfstateTask sbRestSfstateTask =
                new SbRestSfstateTask(RestOperation.PUT, new ServiceFunctionStateBuilder().build(), executorService);
        PowerMockito.mockStatic(SfcProviderServiceFunctionAPI.class);
        Mockito.when(SfcProviderServiceFunctionAPI.readServiceFunction(SFSTATE_NAME))
            .thenReturn(new ServiceFunctionBuilder().build());

        JsonNode jsonObject = mapper.readTree(sbRestSfstateTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionStateTopNode()));
        assertNull("Must be null", sbRestSfstateTask.restUriList);
    }

    private ServiceFunction buildServiceFunction() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SFSTATE_NAME);
        serviceFunctionBuilder.setRestUri(new Uri(REST_URI));
        return serviceFunctionBuilder.build();
    }

    // build service function, which is needed to create SbRestSfstateTask object
    private ServiceFunctionState buildServiceFunctionState() {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName(SFSTATE_NAME);

        return serviceFunctionStateBuilder.build();
    }

    // returns object node with name & rest uri, uses FullTest.json
    private ObjectNode buildServiceFunctionStateObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfstateNode = mapper.createObjectNode();
        sfstateNode.put(SfstateExporterFactory._NAME, SFSTATE_NAME.getValue());

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfstateNode);

        topNode.put(SfstateExporterFactory._SERVICE_FUNCTION_STATE, arrayNode);
        return topNode;
    }

    // returns object node with name only, uses NameOnly.json
    private ObjectNode buildServiceFunctionStateObjectNode1() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfstateNode = mapper.createObjectNode();
        sfstateNode.put(SfstateExporterFactory._NAME, SFSTATE_NAME.getValue());

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfstateNode);

        topNode.put(SfstateExporterFactory._SERVICE_FUNCTION_STATE, arrayNode);
        return topNode;
    }

    private ObjectNode buildServiceFunctionStateTopNode() {
        ObjectNode topNode = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();
        ObjectNode sfstateNode = mapper.createObjectNode();

        arrayNode.add(sfstateNode);
        topNode.put(SfstateExporterFactory._SERVICE_FUNCTION_STATE, arrayNode);
        return topNode;
    }
}

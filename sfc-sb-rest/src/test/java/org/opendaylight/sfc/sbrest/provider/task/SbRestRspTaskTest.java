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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sbrest.json.RspExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SbRestRspTask
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-17
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcProviderServiceForwarderAPI.class)
public class SbRestRspTaskTest {

    private static final RspName RSP_NAME = new RspName("Dummy_RSP");
    private static final SffName SFF_NAME = new SffName("Dummy_SFF");
    private static final String REST_URI = "http://localhost:5000";
    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);

        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        Mockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(SFF_NAME))
            .thenReturn(this.buildServiceFunctionForwarder());
    }

    @Test
    public void testSbRestRspTask() throws IOException {
        SbRestRspTask sbRestRspTask =
                new SbRestRspTask(RestOperation.PUT, this.buildRenderedServicePath(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestRspTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildRenderedServicePathObjectNode()));
        assertTrue("Must be true", sbRestRspTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestRspTask1() throws IOException {
        SbRestRspTask sbRestRspTask =
                new SbRestRspTask(RestOperation.DELETE, this.buildRenderedServicePath(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestRspTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildRenderedServicePathObjectNode1()));
        assertTrue("Must be true", sbRestRspTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestRspTaskEmpty() throws IOException {
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        Mockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(SFF_NAME))
            .thenReturn(new ServiceFunctionForwarderBuilder().build());

        SbRestRspTask sbRestRspTask =
                new SbRestRspTask(RestOperation.PUT, new RenderedServicePathBuilder().build(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestRspTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildRenderedServicePathTopNode()));
        assertNull("Must be null", sbRestRspTask.restUriList);
    }

    // build rendered service path, which is needed to create SbRestRspTask object
    private RenderedServicePath buildRenderedServicePath() {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setName(RSP_NAME);

        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        renderedServicePathHopBuilder.setServiceFunctionForwarder(SFF_NAME);

        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();
        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);

        return renderedServicePathBuilder.build();
    }

    // returns sff with name & rest uri
    private ServiceFunctionForwarder buildServiceFunctionForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(SFF_NAME);
        serviceFunctionForwarderBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionForwarderBuilder.build();
    }

    private ObjectNode buildRenderedServicePathObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode rspNode = mapper.createObjectNode();
        rspNode.put(RspExporterFactory._NAME, RSP_NAME.getValue());

        ObjectNode hopNode = mapper.createObjectNode();
        hopNode.put(RspExporterFactory._SERVICE_FUNCTION_FORWARDER, SFF_NAME.getValue());

        ArrayNode hopArrayNode = mapper.createArrayNode();
        hopArrayNode.add(hopNode);

        rspNode.put(RspExporterFactory._RENDERED_SERVICE_PATH_HOP, hopArrayNode);

        ArrayNode rspArrayNode = mapper.createArrayNode();
        rspArrayNode.add(rspNode);

        topNode.put(RspExporterFactory._RENDERED_SERVICE_PATH, rspArrayNode);
        return topNode;
    }

    // returns object node with name only
    private ObjectNode buildRenderedServicePathObjectNode1() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode rspNode = mapper.createObjectNode();
        rspNode.put(RspExporterFactory._NAME, RSP_NAME.getValue());

        ArrayNode rspArrayNode = mapper.createArrayNode();
        rspArrayNode.add(rspNode);

        topNode.put(RspExporterFactory._RENDERED_SERVICE_PATH, rspArrayNode);
        return topNode;
    }

    private ObjectNode buildRenderedServicePathTopNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode rspNode = mapper.createObjectNode();

        ArrayNode rspArrayNode = mapper.createArrayNode();
        rspArrayNode.add(rspNode);

        topNode.put(RspExporterFactory._RENDERED_SERVICE_PATH, rspArrayNode);
        return topNode;
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * This class contains unit tests for RspExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */
public class RspExporterTest {

    private static final String FULL_JSON = "/RspJsonStrings/FullTest.json";
    private static final String NAME_ONLY_JSON = "/RspJsonStrings/NameOnly.json";

    // create string, that represents .json file
    private String gatherRenderedServicePathJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (RspTestValues rspTestValue : RspTestValues.values()) {
            jsonString = jsonString != null ? jsonString.replaceAll("\\b" + rspTestValue.name() + "\\b",
                    rspTestValue.getValue()) : null;
        }

        return jsonString;
    }

    private boolean testExportRspJson(String expectedResultFile, boolean nameOnly) throws IOException {
        RenderedServicePath renderedServicePath;
        String exportedRspString;
        RspExporterFactory rspExporterFactory = new RspExporterFactory();

        if (nameOnly) {
            renderedServicePath = this.buildRenderedServicePathNameOnly();
            exportedRspString = rspExporterFactory.getExporter().exportJsonNameOnly(renderedServicePath);
        } else {
            renderedServicePath = this.buildRenderedServicePath();
            exportedRspString = rspExporterFactory.getExporter().exportJson(renderedServicePath);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedRspJson =
                objectMapper.readTree(this.gatherRenderedServicePathJsonStringFromFile(expectedResultFile));
        JsonNode exportedRspJson = objectMapper.readTree(exportedRspString);

        return expectedRspJson.equals(exportedRspJson);
    }

    @Test
    public void testExportRspJsonFull() throws IOException {
        assertTrue(testExportRspJson(FULL_JSON, false));
    }

    @Test
    public void testExportRspJsonNameOnly() throws IOException {
        assertTrue(testExportRspJson(NAME_ONLY_JSON, true));
    }

    @Test
    // put wrong parameter, illegal argument exception expected
    public void testExportJsonException() throws Exception {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        RspExporter rspExporter = new RspExporter();

        try {
            rspExporter.exportJson(serviceFunctionGroupBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }

        try {
            rspExporter.exportJsonNameOnly(serviceFunctionGroupBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

    private RenderedServicePath buildRenderedServicePathNameOnly() {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setName(new RspName(RspTestValues.NAME.getValue()));

        return renderedServicePathBuilder.build();
    }

    private RenderedServicePath buildRenderedServicePath() {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();

        renderedServicePathBuilder.setName(new RspName(RspTestValues.NAME.getValue()))
            .setContextMetadata(RspTestValues.CONTEXT_METADATA.getValue())
            .setParentServiceFunctionPath(new SfpName(RspTestValues.PARENT_SERVICE_FUNCTION_PATH.getValue()))
            .setPathId(Long.parseLong(RspTestValues.PATH_ID.getValue()))
            .setServiceChainName(new SfcName(RspTestValues.SERVICE_CHAIN_NAME.getValue()))
            .setStartingIndex(Short.parseShort(RspTestValues.STARTING_INDEX.getValue()))
            .setRenderedServicePathHop(this.buildRenderedServicePathHops());

        return renderedServicePathBuilder.build();
    }

    private List<RenderedServicePathHop> buildRenderedServicePathHops() {
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();

        for (int index = 1; index <= Integer.valueOf(RspTestValues.HOP_NUMBER.getValue()); index++) {

            RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

            renderedServicePathHopBuilder.setHopNumber((short) index)
                .setServiceFunctionName(new SfName(RspTestValues.SERVICE_FUNCTION_NAME.getValue() + index))
                .setServiceFunctionForwarder(createServiceFunctionForwarder(index).getName())
                .setServiceIndex(Short.parseShort(RspTestValues.SERVICE_INDEX.getValue()));

            renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        }

        return renderedServicePathHopList;
    }

    private ServiceFunctionForwarder createServiceFunctionForwarder(int index) {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder
            .setName(new SffName(RspTestValues.SERVICE_FUNCTION_FORWARDER.getValue() + index));
        return serviceFunctionForwarderBuilder.build();
    }

    public enum RspTestValues {
        CONTEXT_METADATA("Context-metadata dummy"), NAME("SFC1-PATH1"), PARENT_SERVICE_FUNCTION_PATH(
                "SFC1-PATH1"), SERVICE_CHAIN_NAME("SFC1"), STARTING_INDEX("255"), PATH_ID("9"), HOP_NUMBER(
                        "3"), SERVICE_FUNCTION_NAME("SF"), SERVICE_FUNCTION_FORWARDER("SFF"), SERVICE_INDEX(
                                "255"), DATA_PLANE_LOCATOR("DPL");

        private final String value;

        RspTestValues(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}

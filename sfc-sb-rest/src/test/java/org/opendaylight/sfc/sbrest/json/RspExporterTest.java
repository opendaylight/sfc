/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;

import static org.junit.Assert.*;


/**
 * This class contains unit tests for RspExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-13
 */
public class RspExporterTest {

    public static final String FULL_JSON = "/RspJsonStrings/FullTest.json";
    public static final String NAME_ONLY_JSON = "/RspJsonStrings/NameOnly.json";


    public enum RspTestValues {
        NAME("SFC1-PATH1"),
        PARENT_SERVICE_FUNCTION_PATH("SFC1-PATH1"),
        SERVICE_CHAIN_NAME("SFC1"),
        STARTING_INDEX("255"),
        PATH_ID("9"),
        HOP_NUMBER("1"),
        SERVICE_FUNCTION_NAME("SF1"),
        SERVICE_FUNCTION_FORWARDER("SFF1"),
        SERVICE_INDEX("255");

        private String value;

        RspTestValues(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    private String gatherRenderedServicePathJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (RspTestValues rspTestValue : RspTestValues.values()) {
            jsonString = jsonString.replaceAll("\\b" + rspTestValue.name() + "\\b", rspTestValue.getValue());
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
        JsonNode expectedRspJson = objectMapper.readTree(this.gatherRenderedServicePathJsonStringFromFile(expectedResultFile));
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

    private RenderedServicePath buildRenderedServicePathNameOnly() {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();

        renderedServicePathBuilder.setName(RspTestValues.NAME.getValue());

        return renderedServicePathBuilder.build();
    }

    private RenderedServicePath buildRenderedServicePath() {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();

        renderedServicePathBuilder.setName(RspTestValues.NAME.getValue());
        renderedServicePathBuilder.setParentServiceFunctionPath(RspTestValues.PARENT_SERVICE_FUNCTION_PATH.getValue());
        renderedServicePathBuilder.setPathId(Long.parseLong(RspTestValues.PATH_ID.getValue()));
        renderedServicePathBuilder.setServiceChainName(RspTestValues.SERVICE_CHAIN_NAME.getValue());
        renderedServicePathBuilder.setStartingIndex(Short.parseShort(RspTestValues.STARTING_INDEX.getValue()));
        renderedServicePathBuilder.setRenderedServicePathHop(this.buildRenderedServicePathHops());

        return renderedServicePathBuilder.build();
    }

    private List<RenderedServicePathHop> buildRenderedServicePathHops() {
        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        renderedServicePathHopBuilder.setHopNumber(Short.parseShort(RspTestValues.HOP_NUMBER.getValue()));
        renderedServicePathHopBuilder.setServiceFunctionName(RspTestValues.SERVICE_FUNCTION_NAME.getValue());
        renderedServicePathHopBuilder.setServiceFunctionForwarder(RspTestValues.SERVICE_FUNCTION_FORWARDER.getValue());
        renderedServicePathHopBuilder.setServiceIndex(Short.parseShort(RspTestValues.SERVICE_INDEX.getValue()));

        renderedServicePathHopList.add(renderedServicePathHopBuilder.build());
        return renderedServicePathHopList;
    }
}

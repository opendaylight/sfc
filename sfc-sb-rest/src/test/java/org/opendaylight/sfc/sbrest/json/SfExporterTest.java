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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;

/**
 * This class contains unit tests for SfExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-13
 */
public class SfExporterTest {

    public static final String FULL_JSON = "/RspJsonStrings/FullTest.json";
    public static final String NAME_ONLY_JSON = "/RspJsonStrings/NameOnly.json";

    public enum SfTestValues implements TestValues {
        NAME("SF1");

        private String value;

        SfTestValues(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    private String gatherServiceFunctionJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (TestValues sfTestValue : SfTestValues.values()) {
            jsonString = jsonString.replaceAll("\\b" + sfTestValue.name() + "\\b", sfTestValue.getValue());
        }

        return jsonString;
    }

    private boolean testExportSfJson(String expectedResultFile, boolean nameOnly) throws IOException {
        ServiceFunction serviceFunction;
        String exportedSfString;
        SfExporterFactory sfExporterFactory = new SfExporterFactory();

        if (nameOnly) {
            serviceFunction = this.buildServiceFunctionNameOnly();
            exportedSfString = sfExporterFactory.getExporter().exportJsonNameOnly(serviceFunction);
        } else {
            serviceFunction = this.buildServiceFunction();
            exportedSfString = sfExporterFactory.getExporter().exportJson(serviceFunction);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedSfJson = objectMapper.readTree(this.gatherServiceFunctionJsonStringFromFile(expectedResultFile));
        JsonNode exportedSfJson = objectMapper.readTree(exportedSfString);

        return expectedSfJson.equals(exportedSfJson);
    }

}

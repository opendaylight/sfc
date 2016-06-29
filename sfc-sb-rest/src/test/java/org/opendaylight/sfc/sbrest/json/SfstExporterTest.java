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
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ShortestPath;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;


/**
 * This class contains unit tests for SfstExporter
 *
 * @author Vladimir Lavor
 * @version 0.1
 * @see org.opendaylight.sfc.sbrest.json.SfstExporter
 * @since 2015-05-28
 */

public class SfstExporterTest {

    private static final String SERVICE_FUNCTION_SCHEDULE_TYPE_PREFIX = "service-function-schedule-type:";
    private static final String FULL_JSON = "/SfstJsonStrings/FullTest.json";
    private static final String NAME_ONLY_JSON = "/SfstJsonStrings/NameOnly.json";

    // read .json in resources/SfstJsonStrings and create a string
    private String gatherServiceFunctionSchedulerTypeJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (SfstTestValues sfstTestValue : SfstTestValues.values()) {
            if (jsonString != null) {
                jsonString = jsonString.replaceAll("\\b" + sfstTestValue.name() + "\\b", sfstTestValue.getValue());
            }
        }

        return jsonString;
    }

    @Test
    public void testExportSfstJsonFull() throws IOException {
        assertTrue("Must be true", testExportSfstJson(FULL_JSON, false));
    }

    @Test
    public void testExportSfstJsonNameOnly() throws IOException {
        assertTrue("Must be true", testExportSfstJson(NAME_ONLY_JSON, true));
    }

    @Test
    // put wrong argument, illegal argument exception is expected
    public void testExportJsonException() throws Exception {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        SfstExporter sfstExporter = new SfstExporter();

        try {
            sfstExporter.exportJson(serviceFunctionForwarderBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }

        try {
            sfstExporter.exportJsonNameOnly(serviceFunctionForwarderBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

    // create a .json with specified attributes, then compare it with file in
    // Resources/SfstJsonString/*.json
    // result is a boolean value which depends on whether strings (.json files) are equals or not
    private boolean testExportSfstJson(String expectedResultFile, boolean nameOnly) throws IOException {
        ServiceFunctionSchedulerType serviceFunctionSchedulerType;
        String exportedSfstString;
        SfstExporterFactory sfstExporterFactory = new SfstExporterFactory();

        if (nameOnly) {
            serviceFunctionSchedulerType = this.buildServiceFunctionSchedulerTypeNameOnly();
            exportedSfstString = sfstExporterFactory.getExporter().exportJsonNameOnly(serviceFunctionSchedulerType);
        } else {
            serviceFunctionSchedulerType = this.buildServiceFunctionSchedulerType();
            exportedSfstString = sfstExporterFactory.getExporter().exportJson(serviceFunctionSchedulerType);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedSfstJson =
                objectMapper.readTree(this.gatherServiceFunctionSchedulerTypeJsonStringFromFile(expectedResultFile));
        JsonNode exportedSfstJson = objectMapper.readTree(exportedSfstString);

        return expectedSfstJson.equals(exportedSfstJson);
    }

    // create service function scheduler type with name only
    private ServiceFunctionSchedulerType buildServiceFunctionSchedulerTypeNameOnly() {
        ServiceFunctionSchedulerTypeBuilder serviceFunctionSchedulerTypeBuilder =
                new ServiceFunctionSchedulerTypeBuilder();
        serviceFunctionSchedulerTypeBuilder.setName(SfstTestValues.NAME.getValue());

        return serviceFunctionSchedulerTypeBuilder.build();
    }

    // create service function scheduler type and set all attributes needed for testing
    private ServiceFunctionSchedulerType buildServiceFunctionSchedulerType() {
        ServiceFunctionSchedulerTypeBuilder serviceFunctionSchedulerTypeBuilder =
                new ServiceFunctionSchedulerTypeBuilder();
        // noinspection unchecked
        serviceFunctionSchedulerTypeBuilder.setName(SfstTestValues.NAME.getValue())
            .setType(SfstTestValues.TYPE.getIdentity());

        return serviceFunctionSchedulerTypeBuilder.build();
    }

    // all attributes and their values necessary to create service function scheduler are defined
    // here
    // these enums are used here an also in respective .json in resources/SfstJsonStrings
    public enum SfstTestValues {
        NAME("SFST"), TYPE(SERVICE_FUNCTION_SCHEDULE_TYPE_PREFIX + ShortestPath.class.getSimpleName().toLowerCase(),
                ShortestPath.class);

        private String value = null;
        private boolean enabled;
        private Class identity;

        SfstTestValues(String value) {
            this.value = value;
        }

        SfstTestValues(String value, Class identity) {
            this.value = value;
            this.identity = identity;
        }

        public String getValue() {
            return this.value;
        }

        public Class getIdentity() {
            return this.identity;
        }
    }
}

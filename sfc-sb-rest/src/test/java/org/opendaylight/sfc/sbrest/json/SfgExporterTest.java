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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * This class contains unit tests for SfgExporter
 *
 * @author Vladimir Lavor
 * @version 0.1
 * @see org.opendaylight.sfc.sbrest.json.SfgExporter
 * @since 2015-05-28
 */

public class SfgExporterTest {

    private static final String SERVICE_FUNCTION_GROUP_TYPE_PREFIX = "service-function-type:";
    private static final String FULL_JSON = "/SfgJsonStrings/FullTest.json";
    private static final String NAME_ONLY_JSON = "/SfgJsonStrings/NameOnly.json";

    // read .json in resources/SfgJsonStrings and create a string
    private String gatherServiceFunctionGroupJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (SfgTestValues sfgTestValue : SfgTestValues.values()) {
            if (jsonString != null) {
                jsonString = jsonString.replaceAll("\\b" + sfgTestValue.name() + "\\b", sfgTestValue.getValue());
            }
        }

        return jsonString;
    }

    @Test
    public void testExportSfgJsonFull() throws IOException {
        assertTrue("Must be true", testExportSfgJson(FULL_JSON, false));
    }

    @Test
    public void testExportSfgJsonNameOnly() throws IOException {
        assertTrue("Must be true", testExportSfgJson(NAME_ONLY_JSON, true));
    }

    @Test
    // put wrong argument, illegal argument exception is expected
    public void testExportJsonException() throws Exception {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        SfgExporter sfgExporter = new SfgExporter();

        try {
            sfgExporter.exportJson(serviceFunctionForwarderBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }

        try {
            sfgExporter.exportJsonNameOnly(serviceFunctionForwarderBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

    // create a .json with specified attributes, then compare it with file in
    // Resources/SfgJsonString/*.json
    // result is a boolean value which depends on whether strings (.json files) are equals or not
    private boolean testExportSfgJson(String expectedResultFile, boolean nameOnly) throws IOException {
        ServiceFunctionGroup serviceFunctionGroup;
        String exportedSfgString;
        SfgExporterFactory sfgExporterFactory = new SfgExporterFactory();

        if (nameOnly) {
            serviceFunctionGroup = this.buildServiceFunctionGroupNameOnly();
            exportedSfgString = sfgExporterFactory.getExporter().exportJsonNameOnly(serviceFunctionGroup);
        } else {
            serviceFunctionGroup = this.buildServiceFunctionGroup();
            exportedSfgString = sfgExporterFactory.getExporter().exportJson(serviceFunctionGroup);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedSfgJson =
                objectMapper.readTree(this.gatherServiceFunctionGroupJsonStringFromFile(expectedResultFile));
        JsonNode exportedSfgJson = objectMapper.readTree(exportedSfgString);

        System.out.println("EXPECTED: " + expectedSfgJson);
        System.out.println("EXPORTED: " + exportedSfgJson);

        return expectedSfgJson.equals(exportedSfgJson);
    }

    // create service function group with name only
    private ServiceFunctionGroup buildServiceFunctionGroupNameOnly() {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        serviceFunctionGroupBuilder.setName(SfgTestValues.NAME.getValue());

        return serviceFunctionGroupBuilder.build();
    }

    // create service function group and set all attributes needed for testing
    private ServiceFunctionGroup buildServiceFunctionGroup() {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        // noinspection unchecked
        serviceFunctionGroupBuilder.setName(SfgTestValues.NAME.getValue())
            .setType(SfgTestValues.TYPE.getSftType())
            .setRestUri(new Uri(SfgTestValues.REST_URI.getValue()))
            .setIpMgmtAddress(new IpAddress(new Ipv4Address(SfgTestValues.IP_MGMT_ADDRESS.getValue())))
            .setAlgorithm(SfgTestValues.ALGORITHM.getValue())
            .setSfcServiceFunction(this.createSfcSfList());

        return serviceFunctionGroupBuilder.build();
    }

    // create list of service functions
    private List<SfcServiceFunction> createSfcSfList() {
        SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            sfcServiceFunctionBuilder
                .setName(new SfName(SfgTestValues.SERVICE_FUNCTION_NAME.getValue() + String.valueOf(i)));
            sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        }

        return sfcServiceFunctionList;
    }

    // all attributes and their values necessary to create service function group are defined here
    // these enums are used here an also in respective .json in resources/SfgJsonStrings
    public enum SfgTestValues {
        NAME("SFG"), REST_URI("http://localhost:5000/"), IP_MGMT_ADDRESS("10.0.0.1"), ALGORITHM("Alg1"), TYPE(
                SERVICE_FUNCTION_GROUP_TYPE_PREFIX + "firewall",
                new SftTypeName("firewall")), SERVICE_FUNCTION_NAME("SffName"), SERVICE_FUNCTION_KEY("SffKey");

        private final String value;
        private SftTypeName sftType;

        SfgTestValues(String value) {
            this.value = value;
        }

        SfgTestValues(String value, SftTypeName sftType) {
            this.value = value;
            this.sftType = sftType;
        }

        public String getValue() {
            return this.value;
        }

        public SftTypeName getSftType() {
            return this.sftType;
        }
    }
}

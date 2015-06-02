/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfgExporter
 * <p/>
 * @since 2015-05-28
 */

public class SfgExporterTest {

    private static final String SERVICE_FUNCTION_GROUP_TYPE_PREFIX = "service-function-type:";
    private static final String FULL_JSON = "/SfgJsonStrings/FullTest.json";
    private static final String NAME_ONLY_JSON = "/SfgJsonStrings/NameOnly.json";
    private ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder;
    private SfgExporter sfgExporter;

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
    public void testExportJsonException() throws Exception {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        sfgExporter = new SfgExporter();

        try {
            sfgExporter.exportJson(serviceFunctionForwarderBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void testExportJsonNameOnlyException() throws Exception {
        serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        sfgExporter = new SfgExporter();

        try {
            sfgExporter.exportJsonNameOnly(serviceFunctionForwarderBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

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
        JsonNode expectedSfgJson = objectMapper.readTree(this.gatherServiceFunctionGroupJsonStringFromFile(expectedResultFile));
        JsonNode exportedSfgJson = objectMapper.readTree(exportedSfgString);

        return expectedSfgJson.equals(exportedSfgJson);
    }

    private ServiceFunctionGroup buildServiceFunctionGroupNameOnly() {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        serviceFunctionGroupBuilder.setName(SfgTestValues.NAME.getValue());

        return serviceFunctionGroupBuilder.build();
    }

    private ServiceFunctionGroup buildServiceFunctionGroup() {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        //noinspection unchecked
        serviceFunctionGroupBuilder.setName(SfgTestValues.NAME.getValue())
                .setType(SfgTestValues.TYPE.getIdentity())
                .setRestUri(new Uri(SfgTestValues.REST_URI.getValue()))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(SfgTestValues.IP_MGMT_ADDRESS.getValue())))
                .setAlgorithm(SfgTestValues.ALGORITHM.getValue());

        return serviceFunctionGroupBuilder.build();
    }

    public enum SfgTestValues {
        NAME("SFG"),
        REST_URI("http://localhost:5000/"),
        IP_MGMT_ADDRESS("10.0.0.1"),
        ALGORITHM("Alg1"),
        TYPE(SERVICE_FUNCTION_GROUP_TYPE_PREFIX + Firewall.class.getName().toLowerCase(), Firewall.class);

        private final String value;
        private Class identity;

        SfgTestValues(String value) {
            this.value = value;
        }

        SfgTestValues(String value, Class identity) {
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



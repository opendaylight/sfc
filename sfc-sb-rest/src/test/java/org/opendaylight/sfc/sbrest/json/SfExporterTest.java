/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

/**
 * This class contains unit tests for SfExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-13
 */
public class SfExporterTest {

    public static final String FULL_JSON = "/SfJsonStrings/FullTest.json";
    public static final String NAME_ONLY_JSON = "/SfJsonStrings/NameOnly.json";

    public enum SfTestValues {
        NAME("SF1"),
        TYPE(SfExporter.SERVICE_FUNCTION_TYPE_PREFIX + "dpi", Dpi.class),
        REST_URI("http://localhost:5000/"),
        IP_MGMT_ADDRESS("127.0.0.1"),
        REQUEST_RECLASSIFICATION("true"),
        NSH_AWARE("true"),
        SF_LOCATOR_NAME("SF1-DP1"),
        SF_LOCATOR_SERVICE_FUNCTION_FORWARDER("SFF1");

        private String value;
        private Class identity;

        SfTestValues(String value) {
            this.value = value;
        }

        SfTestValues(String value, Class identity) {
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

    private String gatherServiceFunctionJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (SfTestValues sfTestValue : SfTestValues.values()) {
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

    @Test
    public void testExportRspJsonFull() throws IOException {
        assertTrue(testExportSfJson(FULL_JSON, false));
    }

    @Test
    public void testExportRspJsonNameOnly() throws IOException {
        assertTrue(testExportSfJson(NAME_ONLY_JSON, true));
    }

    private ServiceFunction buildServiceFunctionNameOnly() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SfTestValues.NAME.getValue());

        return serviceFunctionBuilder.build();
    }

    private ServiceFunction buildServiceFunction() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SfTestValues.NAME.getValue());
        serviceFunctionBuilder.setType(SfTestValues.TYPE.getIdentity());
        serviceFunctionBuilder.setRestUri(new Uri(SfTestValues.REST_URI.getValue()));
        serviceFunctionBuilder.setIpMgmtAddress(new IpAddress(new Ipv4Address(SfTestValues.IP_MGMT_ADDRESS.getValue())));
        serviceFunctionBuilder.setRequestReclassification(Boolean.parseBoolean(SfTestValues.REQUEST_RECLASSIFICATION.getValue()));
        serviceFunctionBuilder.setNshAware(Boolean.parseBoolean(SfTestValues.NSH_AWARE.getValue()));
        serviceFunctionBuilder.setSfDataPlaneLocator(this.buildSfDataPlaneLocator());

        return serviceFunctionBuilder.build();
    }

    private List<SfDataPlaneLocator> buildSfDataPlaneLocator() {
        List<SfDataPlaneLocator> sfDataPlaneLocatorList = new ArrayList<>();

        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder.setName(SfTestValues.SF_LOCATOR_NAME.getValue());
        sfDataPlaneLocatorBuilder.setServiceFunctionForwarder(SfTestValues.SF_LOCATOR_SERVICE_FUNCTION_FORWARDER.getValue());
        sfDataPlaneLocatorList.add(sfDataPlaneLocatorBuilder.build());

        return sfDataPlaneLocatorList;
    }

}

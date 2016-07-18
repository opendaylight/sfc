/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;

/**
 * This class contains unit tests for SfExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */
public class SfExporterTest {

    private static final String FULL_JSON = "/SfJsonStrings/FullTest.json";
    private static final String NAME_ONLY_JSON = "/SfJsonStrings/NameOnly.json";
    private static final int port1 = 6640, port2 = 6633;

    // create string, that represents .json file
    private String gatherServiceFunctionJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (SfTestValues sfTestValue : SfTestValues.values()) {
            jsonString = jsonString != null ? jsonString.replaceAll("\\b" + sfTestValue.name() + "\\b",
                    sfTestValue.getValue()) : null;
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
        JsonNode expectedSfJson =
                objectMapper.readTree(this.gatherServiceFunctionJsonStringFromFile(expectedResultFile));
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

    @Test
    // put wrong parameter, illegal argument exception expected
    public void testExportJsonException() throws Exception {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        SfExporter sfExporter = new SfExporter();

        try {
            sfExporter.exportJson(serviceFunctionGroupBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }

        try {
            sfExporter.exportJsonNameOnly(serviceFunctionGroupBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

    private ServiceFunction buildServiceFunctionNameOnly() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(new SfName(SfTestValues.NAME.getValue()));

        return serviceFunctionBuilder.build();
    }

    private ServiceFunction buildServiceFunction() {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        // noinspection unchecked
        serviceFunctionBuilder.setName(new SfName(SfTestValues.NAME.getValue()))
            .setType(SfTestValues.TYPE.getSftType())
            .setRestUri(new Uri(SfTestValues.REST_URI.getValue()))
            .setIpMgmtAddress(new IpAddress(new Ipv4Address(SfTestValues.IP_MGMT_ADDRESS.getValue())))
            .setRequestReclassification(Boolean.parseBoolean(SfTestValues.REQUEST_RECLASSIFICATION.getValue()))
            .setNshAware(Boolean.parseBoolean(SfTestValues.NSH_AWARE.getValue()))
            .setSfDataPlaneLocator(this.buildSfDataPlaneLocator());

        return serviceFunctionBuilder.build();
    }

    private List<SfDataPlaneLocator> buildSfDataPlaneLocator() {
        List<SfDataPlaneLocator> sfDataPlaneLocatorList = new ArrayList<>();

        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address(SfTestValues.IP_V4_ADDRESS.getValue())));
        ipBuilder.setPort(new PortNumber(Integer.valueOf(SfTestValues.PORT1.getValue())));

        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder.setName(new SfDataPlaneLocatorName(SfTestValues.SF_LOCATOR_NAME.getValue()))
            .setServiceFunctionForwarder(new SffName(SfTestValues.SF_LOCATOR_SERVICE_FUNCTION_FORWARDER.getValue()))
            .setLocatorType(ipBuilder.build());

        sfDataPlaneLocatorList.add(sfDataPlaneLocatorBuilder.build());

        ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv6Address(SfTestValues.IP_V6_ADDRESS.getValue())));
        ipBuilder.setPort(new PortNumber(Integer.valueOf(SfTestValues.PORT2.getValue())));

        sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder.setName(new SfDataPlaneLocatorName(SfTestValues.SF_LOCATOR_NAME.getValue()))
            .setServiceFunctionForwarder(new SffName(SfTestValues.SF_LOCATOR_SERVICE_FUNCTION_FORWARDER.getValue()))
            .setLocatorType(ipBuilder.build());

        sfDataPlaneLocatorList.add(sfDataPlaneLocatorBuilder.build());

        return sfDataPlaneLocatorList;
    }

    public enum SfTestValues {
        NAME("SF1"), TYPE(SfExporter.SERVICE_FUNCTION_TYPE_PREFIX + "dpi", new SftTypeName("dpi")), REST_URI(
                "http://localhost:5000/"), IP_MGMT_ADDRESS("127.0.0.1"), REQUEST_RECLASSIFICATION("true"), NSH_AWARE(
                        "true"), SF_LOCATOR_NAME("SF1-DP1"), SF_LOCATOR_SERVICE_FUNCTION_FORWARDER(
                                "SFF1"), IP_V4_ADDRESS("192.168.10.5"), IP_V6_ADDRESS("01:23:45:67:89:AB:CD:EF"), PORT1(
                                        "6640"), PORT2("6633");

        private final String value;
        private SftTypeName sftType;

        SfTestValues(String value) {
            this.value = value;
        }

        SfTestValues(String value, SftTypeName sftType) {
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

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.choice.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.choice.locator.type.FunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.choice.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.choice.locator.type.LispBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.choice.locator.type.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

/**
 * This class contains unit tests for SffExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-13
 */

public class ExporterUtilTest {

    public static final String IP_LOCATOR_JSON = "/UtilJsonStrings/IpLocatorTest.json";
    public static final String MAC_LOCATOR_JSON = "/UtilJsonStrings/MacLocatorTest.json";
    public static final String LISP_LOCATOR_JSON = "/UtilJsonStrings/LispLocatorTest.json";
    public static final String VXLAN_TRANSPORT_JSON = "/UtilJsonStrings/VxlanTransportTest.json";
    public static final String GRE_TRANSPORT_JSON = "/UtilJsonStrings/GreTransportTest.json";
    public static final String OTHER_TRANSPORT_JSON = "/UtilJsonStrings/OtherTransportTest.json";

    public enum UtilTestValues {
        IP("10.0.0.1"),
        PORT("5000"),
        MAC("11:22:33:44:55:66"),
        VLAN_ID("1234"),
        EID("127.0.0.1"),
        FUNCTION_NAME("locatorFunction1"),
        VXLAN_GPE(ExporterUtil.SERVICE_LOCATOR_PREFIX + "vxlan-gpe", VxlanGpe.class),
        GRE(ExporterUtil.SERVICE_LOCATOR_PREFIX + "gre", Gre.class),
        OTHER(ExporterUtil.SERVICE_LOCATOR_PREFIX + "other", Other.class);

        private String value;
        private Class identity;

        UtilTestValues(String value) {
            this.value = value;
        }

        UtilTestValues(String value, Class identity) {
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

    private String gatherUtilJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (UtilTestValues utilTestValue : UtilTestValues.values()) {
            jsonString = jsonString.replaceAll("\\b" + utilTestValue.name() + "\\b", utilTestValue.getValue());
        }

        return jsonString;
    }

    private boolean testExportUtilLocatorJson(String locatorTypeName, String expectedResultFile) throws IOException {
        DataPlaneLocator dataPlaneLocator = this.buildDataPlaneLocator(locatorTypeName);

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode expectedLocatorJson = objectMapper.readTree(this.gatherUtilJsonStringFromFile(expectedResultFile));
        JsonNode exportedLocatorJson = ExporterUtil.getDataPlaneLocatorObjectNode(dataPlaneLocator);

        return expectedLocatorJson.equals(exportedLocatorJson);
    }

    private boolean testExportUtilTransportJson(String transportTypeName, String expectedResultFile) throws IOException {
        DataPlaneLocator dataPlaneLocator = this.buildDataPlaneLocatorTransport(transportTypeName);

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode expectedLocatorJson = objectMapper.readTree(this.gatherUtilJsonStringFromFile(expectedResultFile));
        JsonNode exportedLocatorJson = ExporterUtil.getDataPlaneLocatorObjectNode(dataPlaneLocator);

        return expectedLocatorJson.equals(exportedLocatorJson);
    }

    @Test
    public void testExportUtilLocatorJsonIp() throws IOException {
        assertTrue(testExportUtilLocatorJson(ExporterUtil.IP, IP_LOCATOR_JSON));
    }

    @Test
    public void testExportUtilLocatorJsonMac() throws IOException {
        assertTrue(testExportUtilLocatorJson(ExporterUtil.MAC, MAC_LOCATOR_JSON));
    }

    @Test
    public void testExportUtilLocatorJsonLisp() throws IOException {
        assertTrue(testExportUtilLocatorJson(ExporterUtil.LISP, LISP_LOCATOR_JSON));
    }

    @Test
    public void testExportUtilTransportJsonVxlan() throws IOException {
        assertTrue(testExportUtilTransportJson(ExporterUtil.VXLAN_GPE, VXLAN_TRANSPORT_JSON));
    }

    @Test
    public void testExportUtilTransportJsonGre() throws IOException {
        assertTrue(testExportUtilTransportJson(ExporterUtil.GRE, GRE_TRANSPORT_JSON));
    }

    @Test
    public void testExportUtilTransportJsonOther() throws IOException {
        assertTrue(testExportUtilTransportJson(ExporterUtil.OTHER, OTHER_TRANSPORT_JSON));
    }

    private DataPlaneLocator buildDataPlaneLocator(String locatorTypeName) {
        LocatorType locatorType = null;

        switch (locatorTypeName) {
            case ExporterUtil.IP:
                IpBuilder ipBuilder = new IpBuilder();
                ipBuilder.setIp(new IpAddress(new Ipv4Address(UtilTestValues.IP.getValue())));
                ipBuilder.setPort(new PortNumber(Integer.parseInt(UtilTestValues.PORT.getValue())));
                locatorType = ipBuilder.build();
                break;
            case ExporterUtil.MAC:
                MacBuilder macBuilder = new MacBuilder();
                macBuilder.setMac(new MacAddress(UtilTestValues.MAC.getValue()));
                macBuilder.setVlanId(Integer.parseInt(UtilTestValues.VLAN_ID.getValue()));
                locatorType = macBuilder.build();
                break;
            case ExporterUtil.LISP:
                LispBuilder lispBuilder = new LispBuilder();
                lispBuilder.setEid(new IpAddress(new Ipv4Address(UtilTestValues.EID.getValue())));
                locatorType = lispBuilder.build();
                break;
            case ExporterUtil.FUNCTION:
                FunctionBuilder functionBuilder = new FunctionBuilder();
                functionBuilder.setFunctionName(UtilTestValues.FUNCTION_NAME.getValue());
                locatorType = functionBuilder.build();
                break;
        }

        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder.setLocatorType(locatorType);

        return dataPlaneLocatorBuilder.build();
    }

    private DataPlaneLocator buildDataPlaneLocatorTransport(String transportTypeName) {
        Class slTransportType = null;

        switch (transportTypeName) {
            case ExporterUtil.VXLAN_GPE:
                slTransportType = UtilTestValues.VXLAN_GPE.getIdentity();
                break;
            case ExporterUtil.GRE:
                slTransportType = UtilTestValues.GRE.getIdentity();
                break;
            case ExporterUtil.OTHER:
                slTransportType = UtilTestValues.OTHER.getIdentity();
        }

        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder.setTransport(slTransportType);

        return dataPlaneLocatorBuilder.build();
    }

}



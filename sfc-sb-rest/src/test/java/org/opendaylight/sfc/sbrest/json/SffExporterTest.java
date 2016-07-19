/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;

/**
 * This class contains unit tests for SffExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-13
 */

public class SffExporterTest {

    private static final String FULL_JSON = "/SffJsonStrings/FullTest.json";
    private static final String NAME_ONLY_JSON = "/SffJsonStrings/NameOnly.json";

    // create string, that represents .json file
    private String gatherServiceFunctionForwardersJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (SffTestValues sffTestValue : SffTestValues.values()) {
            jsonString = jsonString != null ? jsonString.replaceAll("\\b" + sffTestValue.name() + "\\b",
                    sffTestValue.getValue()) : null;
        }

        return jsonString;
    }

    @Test
    public void testExportSffJsonFull() throws IOException {
        assertTrue(testExportSffJson(FULL_JSON, false));
    }

    @Test
    public void testExportSffJsonNameOnly() throws IOException {
        assertTrue(testExportSffJson(NAME_ONLY_JSON, true));
    }

    @Test
    // put wrong parameter, illegal argument exception expected
    public void testExportJsonException() throws Exception {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        SffExporter sffExporter = new SffExporter();

        try {
            sffExporter.exportJson(serviceFunctionGroupBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }

        try {
            sffExporter.exportJsonNameOnly(serviceFunctionGroupBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

    private boolean testExportSffJson(String expectedResultFile, boolean nameOnly) throws IOException {
        ServiceFunctionForwarder serviceFunctionForwarder;
        String exportedSffString;
        SffExporterFactory sffExporterFactory = new SffExporterFactory();

        if (nameOnly) {
            serviceFunctionForwarder = this.buildServiceFunctionForwarderNameOnly();
            exportedSffString = sffExporterFactory.getExporter().exportJsonNameOnly(serviceFunctionForwarder);
        } else {
            serviceFunctionForwarder = this.buildServiceFunctionForwarder();
            exportedSffString = sffExporterFactory.getExporter().exportJson(serviceFunctionForwarder);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedSffJson =
                objectMapper.readTree(this.gatherServiceFunctionForwardersJsonStringFromFile(expectedResultFile));
        JsonNode exportedSffJson = objectMapper.readTree(exportedSffString);

        System.out.println("EXPECTED: " + expectedSffJson);
        System.out.println("CREATED:  " + exportedSffJson);

        return expectedSffJson.equals(exportedSffJson);
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarderNameOnly() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(new SffName(SffTestValues.NAME.getValue()));

        return serviceFunctionForwarderBuilder.build();
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(new SffName(SffTestValues.NAME.getValue()))
            .setRestUri(new Uri(SffTestValues.REST_URI.getValue()))
            .setIpMgmtAddress(new IpAddress(new Ipv4Address(SffTestValues.IP_MGMT_ADDRESS.getValue())))
            .setServiceNode(new SnName(SffTestValues.SERVICE_NODE.getValue()))
            .setSffDataPlaneLocator(this.buildSffDataPlaneLocator())
            .setServiceFunctionDictionary(this.buildServiceFunctionDictionary());

        return serviceFunctionForwarderBuilder.build();
    }

    // build sff data plane locator list with one sff data plane locator
    private List<SffDataPlaneLocator> buildSffDataPlaneLocator() {
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();

        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder
            .setName(new SffDataPlaneLocatorName(SffTestValues.SFF_DATA_PLANE_LOCATOR_NAME.getValue()))
            .addAugmentation(SffOvsLocatorBridgeAugmentation.class, this.buildSffDataPlaneLocator1());
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        return sffDataPlaneLocatorList;
    }

    // build data plane locator
    private SffOvsLocatorBridgeAugmentation buildSffDataPlaneLocator1() {
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(SffTestValues.SFF_DATA_PLANE_LOCATOR_BRIDGE_NAME.getValue())
            .setUuid(new Uuid(SffTestValues.SFF_DATA_PLANE_LOCATOR_UUID.getValue()));

        SffOvsLocatorBridgeAugmentationBuilder sffDataPlaneLocator1Builder =
                new SffOvsLocatorBridgeAugmentationBuilder();
        sffDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());

        return sffDataPlaneLocator1Builder.build();
    }

    private List<ServiceFunctionDictionary> buildServiceFunctionDictionary() {
        List<ServiceFunctionDictionary> serviceFunctionDictionaryList = new ArrayList<>();

        ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();
        // noinspection unchecked
        serviceFunctionDictionaryBuilder.setName(new SfName(SffTestValues.SF_DICTIONARY_NAME.getValue()))
            .setSffSfDataPlaneLocator(this.buildSffSfDataPlaneLocator());

        serviceFunctionDictionaryList.add(serviceFunctionDictionaryBuilder.build());

        return serviceFunctionDictionaryList;
    }

    private SffSfDataPlaneLocator buildSffSfDataPlaneLocator() {

        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder
            .setSfDplName(new SfDataPlaneLocatorName(SffTestValues.SF_DATA_PLANE_LOCATOR_NAME.getValue()));
        sffSfDataPlaneLocatorBuilder
            .setSffDplName(new SffDataPlaneLocatorName(SffTestValues.SFF_DATA_PLANE_LOCATOR_NAME.getValue()));

        return sffSfDataPlaneLocatorBuilder.build();
    }

    public enum SffTestValues {
        NAME("SFF1"), SFF_DATA_PLANE_LOCATOR_NAME("SFF1_DP1"), SFF_DATA_PLANE_LOCATOR_BRIDGE_NAME(
                "br-int"), SFF_DATA_PLANE_LOCATOR_UUID("4c3778e4-840d-47f4-b45e-0988e514d26c"), SF_DICTIONARY_NAME(
                        "SF1"), SF_DATA_PLANE_LOCATOR_NAME("SF1_DP1"), REST_URI(
                                "http://localhost:5000/"), IP_MGMT_ADDRESS("10.0.0.1"), SERVICE_NODE("SN1");

        private final String value;
        private SftTypeName sftType;

        SffTestValues(String value) {
            this.value = value;
        }

        SffTestValues(String value, SftTypeName sftType) {
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

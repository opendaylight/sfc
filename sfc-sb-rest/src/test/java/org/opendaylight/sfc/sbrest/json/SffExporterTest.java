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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffSfDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffSfDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;

import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SffExporter
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-13
 */

public class SffExporterTest {

    public static final String FULL_JSON = "/SffJsonStrings/FullTest.json";
    public static final String NAME_ONLY_JSON = "/SffJsonStrings/NameOnly.json";

    public enum SffTestValues {
        NAME("SFF1"),
        SFF_DATA_PLANE_LOCATOR_NAME("SFF1_DP1"),
        SFF_DATA_PLANE_LOCATOR_BRIDGE_NAME("br-int"),
        SFF_DATA_PLANE_LOCATOR_UUID("4c3778e4-840d-47f4-b45e-0988e514d26c"),
        SF_DICTIONARY_NAME("SF1"),
        SF_DICTIONARY_TYPE(SfExporter.SERVICE_FUNCTION_TYPE_PREFIX + "dpi", Dpi.class),
        SF_DATA_PLANE_LOCATOR_BRIDGE_NAME("br-tun"),
        REST_URI("http://localhost:5000/"),
        IP_MGMT_ADDRESS("10.0.0.1"),
        SERVICE_NODE("SN1");

        private String value;
        private Class identity;

        SffTestValues(String value) {
            this.value = value;
        }

        SffTestValues(String value, Class identity) {
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

    private String gatherServiceFunctionForwardersJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (SffTestValues sffTestValue : SffTestValues.values()) {
            jsonString = jsonString.replaceAll("\\b" + sffTestValue.name() + "\\b", sffTestValue.getValue());
        }

        return jsonString;
    }

    @Test
    public void testExportSffJsonFull() throws IOException {
        assertTrue(testExportSffJson(FULL_JSON, false));
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
        JsonNode expectedSffJson = objectMapper.readTree(this.gatherServiceFunctionForwardersJsonStringFromFile(expectedResultFile));
        JsonNode exportedSffJson = objectMapper.readTree(exportedSffString);

        return expectedSffJson.equals(exportedSffJson);
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarderNameOnly() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(SffTestValues.NAME.getValue());

        return serviceFunctionForwarderBuilder.build();
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(SffTestValues.NAME.getValue());
        serviceFunctionForwarderBuilder.setRestUri(new Uri(SffTestValues.REST_URI.getValue()));
        serviceFunctionForwarderBuilder.setIpMgmtAddress(new IpAddress(new Ipv4Address(SffTestValues.IP_MGMT_ADDRESS.getValue())));
        serviceFunctionForwarderBuilder.setServiceNode(SffTestValues.SERVICE_NODE.getValue());
        serviceFunctionForwarderBuilder.setSffDataPlaneLocator(this.buildSffDataPlaneLocator());
        serviceFunctionForwarderBuilder.setServiceFunctionDictionary(this.buildServiceFunctionDictionary());

        return serviceFunctionForwarderBuilder.build();
    }

    private List<SffDataPlaneLocator> buildSffDataPlaneLocator() {
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();

        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder.setName(SffTestValues.SFF_DATA_PLANE_LOCATOR_NAME.getValue());
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator1.class, this.buildSffDataPlaneLocator1());
        sffDataPlaneLocatorList.add(sffDataPlaneLocatorBuilder.build());

        return sffDataPlaneLocatorList;
    }

    private SffDataPlaneLocator1 buildSffDataPlaneLocator1() {
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(SffTestValues.SFF_DATA_PLANE_LOCATOR_BRIDGE_NAME.getValue());
        ovsBridgeBuilder.setUuid(new Uuid(SffTestValues.SFF_DATA_PLANE_LOCATOR_UUID.getValue()));

        SffDataPlaneLocator1Builder sffDataPlaneLocator1Builder = new SffDataPlaneLocator1Builder();
        sffDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());

        return sffDataPlaneLocator1Builder.build();
    }

    private List<ServiceFunctionDictionary> buildServiceFunctionDictionary() {
        List<ServiceFunctionDictionary> serviceFunctionDictionaryList = new ArrayList<>();

        ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();
        serviceFunctionDictionaryBuilder.setName(SffTestValues.SF_DICTIONARY_NAME.getValue());
        serviceFunctionDictionaryBuilder.setType(SffTestValues.SF_DICTIONARY_TYPE.getIdentity());
        serviceFunctionDictionaryBuilder.setSffSfDataPlaneLocator(this.buildSffSfDataPlaneLocator());

        serviceFunctionDictionaryList.add(serviceFunctionDictionaryBuilder.build());
        return serviceFunctionDictionaryList;
    }

    private SffSfDataPlaneLocator buildSffSfDataPlaneLocator() {
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder.addAugmentation(SffSfDataPlaneLocator1.class, this.buildSffSfDataPlaneLocator1());

        return sffSfDataPlaneLocatorBuilder.build();
    }

    private SffSfDataPlaneLocator1 buildSffSfDataPlaneLocator1() {
        OvsBridgeBuilder ovsBridgeBuilder = new OvsBridgeBuilder();
        ovsBridgeBuilder.setBridgeName(SffTestValues.SF_DATA_PLANE_LOCATOR_BRIDGE_NAME.getValue());

        SffSfDataPlaneLocator1Builder sffSfDataPlaneLocator1Builder = new SffSfDataPlaneLocator1Builder();
        sffSfDataPlaneLocator1Builder.setOvsBridge(ovsBridgeBuilder.build());

        return sffSfDataPlaneLocator1Builder.build();
    }
}



/*
 * Copyright (c) 2015 Intel Corp. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestampBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatistic;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter64;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1Builder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMonBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.Capabilities;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationKey;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SfstateExporter
 *
 * @author Hongli Chen (honglix.chen@intel.com)
 * @version 0.1
 * @since 2015-09-21
 */
public class SfstateExporterTest {

    private static final String FULL_JSON = "/SfstateJsonStrings/FullTest.json";
    private static final String NAME_ONLY_JSON = "/SfstateJsonStrings/NameOnly.json";

    // create string, that represents .json file
    private String gatherServiceFunctionStateJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (SfstateTestValues sfstateTestValue : SfstateTestValues.values()) {
            jsonString = jsonString != null ? jsonString.replaceAll("\\b" + sfstateTestValue.name() + "\\b",
                    sfstateTestValue.getValue()) : null;
        }

        return jsonString;
    }

    private boolean testExportSfstateJson(String expectedResultFile, boolean nameOnly) throws IOException {
        ServiceFunctionState serviceFunctionState;
        String exportedSfstateString;
        SfstateExporterFactory sfstateExporterFactory = new SfstateExporterFactory();

        if (nameOnly) {
            serviceFunctionState = this.buildServiceFunctionStateNameOnly();
            exportedSfstateString = sfstateExporterFactory.getExporter().exportJsonNameOnly(serviceFunctionState);
        } else {
            serviceFunctionState = this.buildServiceFunctionState();
            exportedSfstateString = sfstateExporterFactory.getExporter().exportJson(serviceFunctionState);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedSfstateJson =
                objectMapper.readTree(this.gatherServiceFunctionStateJsonStringFromFile(expectedResultFile));
        JsonNode exportedSfstateJson = objectMapper.readTree(exportedSfstateString);

        System.out.println("EXPECTED: " + expectedSfstateJson);
        System.out.println("CREATED:  " + exportedSfstateJson);

        return expectedSfstateJson.equals(exportedSfstateJson);
    }

    @Test
    public void testExportSfstateJsonFull() throws IOException {
        assertTrue(testExportSfstateJson(FULL_JSON, false));
    }

    @Test
    public void testExportSfstateJsonNameOnly() throws IOException {
        assertTrue(testExportSfstateJson(NAME_ONLY_JSON, true));
    }

    @Test
    // put wrong parameter, illegal argument exception expected
    public void testExportJsonException() throws Exception {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        SfstateExporter sfstateExporter = new SfstateExporter();

        try {
            sfstateExporter.exportJson(serviceFunctionStateBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }

        try {
            sfstateExporter.exportJsonNameOnly(serviceFunctionStateBuilder.build());
        } catch (Exception exception) {
            assertEquals("Must be true", exception.getClass(), IllegalArgumentException.class);
        }
    }

    private ServiceFunctionState buildServiceFunctionStateNameOnly() {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName(new SfName(SfstateTestValues.NAME.getValue()));

        return serviceFunctionStateBuilder.build();
    }

    private ServiceFunctionState buildServiceFunctionState() {
        DescriptionInfo descriptionInfo = buildSfDescriptionInfo();
        MonitoringInfo monitoringInfo = buildSfMonitoringInfo();
        SfcSfDescMon sfDescMon =
                new SfcSfDescMonBuilder().setMonitoringInfo(monitoringInfo).setDescriptionInfo(descriptionInfo).build();

        ServiceStatistic serviceStatistics =
                new ServiceStatisticBuilder()
                    .setBytesIn(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.BYTES_IN.getValue()))))
                    .setBytesOut(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.BYTES_OUT.getValue()))))
                    .setPacketsOut(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.PACKETS_OUT.getValue()))))
                    .setPacketsIn(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.PACKETS_IN.getValue()))))
                    .build();

        List<SfServicePath> sfServicePaths = new ArrayList<SfServicePath>();
        ServiceStatistic pathserviceStatistics =
                new ServiceStatisticBuilder()
                    .setBytesIn(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.PATH_BYTES_IN.getValue()))))
                    .setBytesOut(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.PATH_BYTES_OUT.getValue()))))
                    .setPacketsOut(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.PATH_PACKETS_OUT.getValue()))))
                    .setPacketsIn(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.PACKETS_IN.getValue()))))
                    .build();
        SfServicePathKey servicePathKey = new SfServicePathKey(new SfpName(SfstateTestValues.PATH_NAME.getValue()));

        StatisticByTimestamp pathStat = new StatisticByTimestampBuilder()
                .setTimestamp(BigInteger.valueOf(123456789l))
                .setServiceStatistic(pathserviceStatistics)
                .build();

        SfServicePath sfServicePath = new SfServicePathBuilder().setKey(servicePathKey)
            .setName(new SfpName(SfstateTestValues.PATH_NAME.getValue()))
            .setStatisticByTimestamp(Collections.singletonList(pathStat))
            .build();
        sfServicePaths.add(sfServicePath);
        ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(new SfName(SfstateTestValues.NAME.getValue()));

        StatisticByTimestamp sfStateStat = new StatisticByTimestampBuilder()
                .setTimestamp(BigInteger.valueOf(123456789l))
                .setServiceStatistic(serviceStatistics)
                .build();
        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder().setKey(serviceFunctionStateKey)
            .setStatisticByTimestamp(Collections.singletonList(sfStateStat))
            .setSfServicePath(sfServicePaths)
            .addAugmentation(ServiceFunctionState1.class, sfState1)
            .build();

        return serviceFunctionState;
    }

    private DescriptionInfo buildSfDescriptionInfo() {
        List<PortBandwidth> portBandwidthList = new ArrayList<PortBandwidth>();

        PortBandwidthKey portBandwidthKey1 = new PortBandwidthKey(Long.valueOf(SfstateTestValues.PORT_ID1.getValue()));
        PortBandwidthKey portBandwidthKey2 = new PortBandwidthKey(Long.valueOf(SfstateTestValues.PORT_ID2.getValue()));

        PortBandwidth portBandwidth1 =
                new PortBandwidthBuilder().setIpaddress(new Ipv4Address(SfstateTestValues.IPADDRESS1.getValue()))
                    .setKey(portBandwidthKey1)
                    .setMacaddress(new MacAddress(SfstateTestValues.MACADDRESS1.getValue()))
                    .setPortId(Long.valueOf(SfstateTestValues.PORT_ID1.getValue()))
                    .setSupportedBandwidth(Long.valueOf(SfstateTestValues.SUPPORTED_BANDWIDTH1.getValue()))
                    .build();

        PortBandwidth portBandwidth2 =
                new PortBandwidthBuilder().setIpaddress(new Ipv4Address(SfstateTestValues.IPADDRESS2.getValue()))
                    .setKey(portBandwidthKey2)
                    .setMacaddress(new MacAddress(SfstateTestValues.MACADDRESS2.getValue()))
                    .setPortId(Long.valueOf(SfstateTestValues.PORT_ID2.getValue()))
                    .setSupportedBandwidth(Long.valueOf(SfstateTestValues.SUPPORTED_BANDWIDTH2.getValue()))
                    .build();

        portBandwidthList.add(portBandwidth1);
        portBandwidthList.add(portBandwidth2);

        PortsBandwidth portsBandwidth = new PortsBandwidthBuilder().setPortBandwidth(portBandwidthList).build();

        Capabilities cap = new CapabilitiesBuilder().setPortsBandwidth(portsBandwidth)
            .setFIBSize(Long.valueOf(SfstateTestValues.FIB_SIZE.getValue()))
            .setRIBSize(Long.valueOf(SfstateTestValues.RIB_SIZE.getValue()))
            .setSupportedACLNumber(Long.valueOf(SfstateTestValues.SUPPORTED_ACL_NUMBER.getValue()))
            .setSupportedBandwidth(Long.valueOf(SfstateTestValues.WHOLE_SUPPORTED_BANDWIDTH.getValue()))
            .setSupportedPacketRate(Long.valueOf(SfstateTestValues.SUPPORTED_PACKET_RATE.getValue()))
            .build();

        DescriptionInfo descInfo = new DescriptionInfoBuilder().setCapabilities(cap)
            .setType(SfstateTestValues.TYPE.getValue())
            .setDataPlaneIp(new IpAddress(new Ipv4Address(SfstateTestValues.DATA_PLANE_IP.getValue())))
            .setDataPlanePort(new PortNumber(Integer.valueOf(SfstateTestValues.DATA_PLANE_PORT.getValue())))
            .setNumberOfDataports(Long.valueOf(SfstateTestValues.NUMBER_OF_DATAPORTS.getValue()))
            .build();
        return descInfo;
    }

    private MonitoringInfo buildSfMonitoringInfo() {
        List<PortBandwidthUtilization> portBandwidthUtilList = new ArrayList<PortBandwidthUtilization>();

        PortBandwidthUtilizationKey portBandwidthUtilKey1 =
                new PortBandwidthUtilizationKey(Long.valueOf(SfstateTestValues.PORT_ID1.getValue()));
        PortBandwidthUtilizationKey portBandwidthUtilKey2 =
                new PortBandwidthUtilizationKey(Long.valueOf(SfstateTestValues.PORT_ID2.getValue()));

        PortBandwidthUtilization portBandwidthUtil1 =
                new PortBandwidthUtilizationBuilder()
                    .setBandwidthUtilization(Long.valueOf(SfstateTestValues.PORT_BANDWIDTH_UTIL1.getValue()))
                    .setKey(portBandwidthUtilKey1)
                    .setRxBytes(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.RX_BYTES1.getValue()))))
                    .setRxPacket(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.RX_PACKET1.getValue()))))
                    .setRxPacketRate(Long.valueOf(SfstateTestValues.RX_PACKET_RATE1.getValue()))
                    .setRxBytesRate(Long.valueOf(SfstateTestValues.RX_BYTES_RATE1.getValue()))
                    .setTxBytes(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.TX_BYTES1.getValue()))))
                    .setTxPacket(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.TX_PACKET1.getValue()))))
                    .setTxPacketRate(Long.valueOf(SfstateTestValues.TX_PACKET_RATE1.getValue()))
                    .setTxBytesRate(Long.valueOf(SfstateTestValues.TX_BYTES_RATE1.getValue()))
                    .setPortId(Long.valueOf(SfstateTestValues.PORT_ID1.getValue()))
                    .build();

        PortBandwidthUtilization portBandwidthUtil2 =
                new PortBandwidthUtilizationBuilder()
                    .setBandwidthUtilization(Long.valueOf(SfstateTestValues.PORT_BANDWIDTH_UTIL2.getValue()))
                    .setKey(portBandwidthUtilKey2)
                    .setRxBytes(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.RX_BYTES2.getValue()))))
                    .setRxPacket(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.RX_PACKET2.getValue()))))
                    .setRxPacketRate(Long.valueOf(SfstateTestValues.RX_PACKET_RATE2.getValue()))
                    .setRxBytesRate(Long.valueOf(SfstateTestValues.RX_BYTES_RATE2.getValue()))
                    .setTxBytes(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.TX_BYTES2.getValue()))))
                    .setTxPacket(new ZeroBasedCounter64(
                            new Counter64(new BigInteger(SfstateTestValues.TX_PACKET2.getValue()))))
                    .setTxPacketRate(Long.valueOf(SfstateTestValues.TX_PACKET_RATE2.getValue()))
                    .setTxBytesRate(Long.valueOf(SfstateTestValues.TX_BYTES_RATE2.getValue()))
                    .setPortId(Long.valueOf(SfstateTestValues.PORT_ID2.getValue()))
                    .build();

        portBandwidthUtilList.add(portBandwidthUtil1);
        portBandwidthUtilList.add(portBandwidthUtil2);

        SFPortsBandwidthUtilization sfPortsBandwidthUtil =
                new SFPortsBandwidthUtilizationBuilder().setPortBandwidthUtilization(portBandwidthUtilList).build();

        ResourceUtilization resrcUtil = new ResourceUtilizationBuilder()
            .setAvailableMemory(Long.valueOf(SfstateTestValues.AVAILABLE_MEMORY.getValue()))
            .setBandwidthUtilization(Long.valueOf(SfstateTestValues.BANDWIDTH_UTIL.getValue()))
            .setCPUUtilization(Long.valueOf(SfstateTestValues.CPU_UTIL.getValue()))
            .setFIBUtilization(Long.valueOf(SfstateTestValues.FIB_UTIL.getValue()))
            .setRIBUtilization(Long.valueOf(SfstateTestValues.RIB_UTIL.getValue()))
            .setMemoryUtilization(Long.valueOf(SfstateTestValues.MEMORY_UTIL.getValue()))
            .setPacketRateUtilization(Long.valueOf(SfstateTestValues.PACKET_RATE_UTIL.getValue()))
            .setPowerUtilization(Long.valueOf(SfstateTestValues.POWER_UTIL.getValue()))
            .setSFPortsBandwidthUtilization(sfPortsBandwidthUtil)
            .build();

        // sf monitor data
        MonitoringInfo monInfo = new MonitoringInfoBuilder().setResourceUtilization(resrcUtil)
            .setLiveness(Boolean.parseBoolean(SfstateTestValues.LIVENESS.getValue()))
            .build();

        return monInfo;
    }

    public enum SfstateTestValues {
        NAME("SF1"), BYTES_IN("800"), BYTES_OUT("1600"), PACKETS_IN("100"), PACKETS_OUT("200"), PATH_NAME(
                "sfp1"), PATH_BYTES_IN("800"), PATH_BYTES_OUT("1600"), PATH_PACKETS_IN("100"), PATH_PACKETS_OUT(
                        "200"), LIVENESS("true"), PACKET_RATE_UTIL("35"), CPU_UTIL("15"), FIB_UTIL("25"), POWER_UTIL(
                                "20"), BANDWIDTH_UTIL("43"), RIB_UTIL("30"), MEMORY_UTIL("35"), AVAILABLE_MEMORY(
                                        "500"), PORT_ID1("1"), PORT_ID2("2"), TX_BYTES1("1600"), TX_BYTES2(
                                                "2400"), TX_BYTES_RATE1("30"), TX_BYTES_RATE2("35"), TX_PACKET1(
                                                        "200"), TX_PACKET2("300"), TX_PACKET_RATE1(
                                                                "32"), TX_PACKET_RATE2("36"), RX_PACKET1(
                                                                        "100"), RX_PACKET2("200"), RX_BYTES1(
                                                                                "800"), RX_BYTES2(
                                                                                        "1600"), RX_BYTES_RATE1(
                                                                                                "25"), RX_BYTES_RATE2(
                                                                                                        "35"), RX_PACKET_RATE1(
                                                                                                                "37"), RX_PACKET_RATE2(
                                                                                                                        "40"), PORT_BANDWIDTH_UTIL1(
                                                                                                                                "33"), PORT_BANDWIDTH_UTIL2(
                                                                                                                                        "30"), TYPE(
                                                                                                                                                "dpi"), DATA_PLANE_PORT(
                                                                                                                                                        "500"), DATA_PLANE_IP(
                                                                                                                                                                "192.168.1.1"), IPADDRESS1(
                                                                                                                                                                        "10.0.0.1"), IPADDRESS2(
                                                                                                                                                                                "10.0.0.2"), MACADDRESS1(
                                                                                                                                                                                        "01:1e:67:a2:5f:f6"), MACADDRESS2(
                                                                                                                                                                                                "01:1e:67:a2:5f:f7"), SUPPORTED_BANDWIDTH1(
                                                                                                                                                                                                        "10"), SUPPORTED_BANDWIDTH2(
                                                                                                                                                                                                                "20"), NUMBER_OF_DATAPORTS(
                                                                                                                                                                                                                        "2"), RIB_SIZE(
                                                                                                                                                                                                                                "200"), SUPPORTED_PACKET_RATE(
                                                                                                                                                                                                                                        "40"), SUPPORTED_ACL_NUMBER(
                                                                                                                                                                                                                                                "1000"), FIB_SIZE(
                                                                                                                                                                                                                                                        "300"), WHOLE_SUPPORTED_BANDWIDTH(
                                                                                                                                                                                                                                                                "30");

        private final String value;
        private Class identity;

        SfstateTestValues(String value) {
            this.value = value;
        }

        SfstateTestValues(String value, Class identity) {
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

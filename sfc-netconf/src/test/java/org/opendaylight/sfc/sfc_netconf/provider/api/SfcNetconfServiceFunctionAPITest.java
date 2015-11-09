/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.SfStateDescMonAugmentation;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.SfStateDescMonAugmentationBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(SfcNetconfSfDescriptionMonitorAPI.class)
public class SfcNetconfServiceFunctionAPITest extends AbstractDataBrokerTest {

    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private static final String IP_MGMT_ADDRESS = "192.168.1.2";
    private static final int DP_PORT = 6633;
    private static final SfName SF_NAME = new SfName("dummySF");
    private static final SfName SF_STATE_NAME = new SfName("dummySFS");

    @Before
    public void before() {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
    }

    @Test
    public void testBuildServiceFunctionFromNetconf() throws Exception {
        ServiceFunctionKey key = new ServiceFunctionKey(SF_NAME);
        IpBuilder ipBuilder = new IpBuilder();
        PortNumber portNumber = new PortNumber(DP_PORT);
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS));
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        SfDataPlaneLocatorName sfDplName = new SfDataPlaneLocatorName(IP_MGMT_ADDRESS);
        locatorBuilder.setName(sfDplName).setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);
        SfDataPlaneLocator sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME)
            .setKey(key)
            .setType(Firewall.class)
            .setIpMgmtAddress(ipAddress)
            .setSfDataPlaneLocator(dataPlaneLocatorList)
            .setNshAware(true);

        ServiceFunction serviceFunction = SfcNetconfServiceFunctionAPI.buildServiceFunctionFromNetconf(SF_NAME, ipAddress, portNumber, Firewall.class);
        assertNotNull("Must not be null", serviceFunction);
        assertEquals("Must be equal", serviceFunction, serviceFunctionBuilder.build());
    }

/*    @Test
    public void testGetServiceFunctionDescription() throws Exception {
        List<PortBandwidth> portBandwidthList = new ArrayList<>();

        Long[] data;
        data = new Long[10];
        for (int i = 0; i < 10; i++) {
            data[i] = Long.parseLong(Integer.toString(i + 1));
        }

        PortNumber portNumber = new PortNumber(DP_PORT);
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS));
        PortBandwidthKey portBandwidthKey = new PortBandwidthKey(data[0]);
        PortBandwidth portBandwidth = new PortBandwidthBuilder().setIpaddress(new Ipv4Address(IP_MGMT_ADDRESS))
            .setKey(portBandwidthKey)
            .setMacaddress(new MacAddress("00:1e:67:a2:5f:f4"))
            .setPortId(data[0])
            .setSupportedBandwidth(data[1])
            .build();
        portBandwidthList.add(portBandwidth);

        PortsBandwidth portsBandwidth = new PortsBandwidthBuilder().setPortBandwidth(portBandwidthList).build();
        // sf cap
        Capabilities cap = new CapabilitiesBuilder().setPortsBandwidth(portsBandwidth)
            .setFIBSize(data[2])
            .setRIBSize(data[3])
            .setSupportedACLNumber(data[4])
            .setSupportedBandwidth(data[5])
            .setSupportedPacketRate(data[6])
            .build();

        // sf description
        long numberOfDataports = 1;
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfo descInfoTemp =
                new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfoBuilder()
                    .setCapabilities(cap)
                    .setDataPlanePort(portNumber)
                    .setDataPlaneIp(ipAddress)
                    .setType("DPI")
                    .setNumberOfDataports(numberOfDataports).build();

        DescriptionInfo descInfo = new DescriptionInfoBuilder(descInfoTemp).build();

        GetSFDescriptionOutput getSFDescriptionOutput = new GetSFDescriptionOutputBuilder()
                                                            .setDescriptionInfo(descInfoTemp).build();
        PowerMockito.stub(PowerMockito.method(SfcNetconfSfDescriptionMonitorAPI.class, "getSFDescriptionInfoFromNetconf"))
            .toReturn(getSFDescriptionOutput);

        DescriptionInfo di = SfcNetconfServiceFunctionAPI.getServiceFunctionDescription("unittest-fw-1");
        assertNotNull("Must be not null", di);
        assertEquals("Must be equal", di, descInfo);
    }

    @Test
    public void testGetServiceFunctionMonitor()  throws Exception {
        List<PortBandwidthUtilization> portBandwidthUtilList = new ArrayList<>();

        Long[] data = new Long[10];
        for (int i = 0; i < 10; i++) {
            data[i] = Long.parseLong(Integer.toString(i + 1));
        }
        PortBandwidthUtilizationKey portBandwidthUtilKey = new PortBandwidthUtilizationKey(data[0]);
        PortBandwidthUtilization portBandwidthUtil = new PortBandwidthUtilizationBuilder()
            .setBandwidthUtilization(data[2]).setKey(portBandwidthUtilKey).setPortId(data[0]).build();
        portBandwidthUtilList.add(portBandwidthUtil);

        SFPortsBandwidthUtilization sfPortsBandwidthUtil =
                new SFPortsBandwidthUtilizationBuilder().setPortBandwidthUtilization(portBandwidthUtilList).build();

        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization resrcUtil =
            new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilizationBuilder().setAvailableMemory(data[1])
                .setBandwidthUtilization(data[2])
                .setCPUUtilization(data[3])
                .setFIBUtilization(data[4])
                .setRIBUtilization(data[5])
                .setMemoryUtilization(data[6])
                .setPacketRateUtilization(data[7])
                .setPowerUtilization(data[8])
                .setSFPortsBandwidthUtilization(sfPortsBandwidthUtil)
                .build();

        // sf monitor data
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfo monInfoTemp =
                new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfoBuilder()
                    .setResourceUtilization(resrcUtil)
                    .setLiveness(true).build();

        MonitoringInfo monInfo = new MonitoringInfoBuilder(monInfoTemp).build();
        GetSFMonitoringInfoOutput getSFMonitoringInfoOutput = new GetSFMonitoringInfoOutputBuilder()
                                                            .setMonitoringInfo(monInfoTemp).build();
        PowerMockito.stub(PowerMockito.method(SfcNetconfSfDescriptionMonitorAPI.class, "getSFMonitorInfoFromNetconf"))
            .toReturn(getSFMonitoringInfoOutput);

        MonitoringInfo mi = SfcNetconfServiceFunctionAPI.getServiceFunctionMonitor("unittest-fw-2");
        assertNotNull("Must be not null", mi);
        assertEquals("Must be equal", mi, monInfo);
    }*/

    @Test
    public void testCreateReadServiceFunctionDescription() {
        SfName sfName = new SfName("unittest-fw-1");
        //       ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
///        SfcSfDescMon sfDescMon;
        List<PortBandwidth> portBandwidthList = new ArrayList<>();

        Long[] data;
        data = new Long[10];
        for (int i = 0; i < 10; i++) {
            data[i] = Long.parseLong(Integer.toString(i + 1));
        }
        PortBandwidthKey portBandwidthKey = new PortBandwidthKey(data[0]);
        PortBandwidth portBandwidth = new PortBandwidthBuilder().setIpaddress(new Ipv4Address(IP_MGMT_ADDRESS))
            .setKey(portBandwidthKey)
            .setMacaddress(new MacAddress("00:1e:67:a2:5f:f4"))
            .setPortId(data[0])
            .setSupportedBandwidth(data[1])
            .build();
        portBandwidthList.add(portBandwidth);

        PortsBandwidth portsBandwidth = new PortsBandwidthBuilder().setPortBandwidth(portBandwidthList).build();
        // sf cap
        Capabilities cap = new CapabilitiesBuilder().setPortsBandwidth(portsBandwidth)
            .setFIBSize(data[2])
            .setRIBSize(data[3])
            .setSupportedACLNumber(data[4])
            .setSupportedBandwidth(data[5])
            .setSupportedPacketRate(data[6])
            .build();

        // sf description
        long numberOfDataports = 1;
        DescriptionInfo descInfo =
                new DescriptionInfoBuilder().setCapabilities(cap).setNumberOfDataports(numberOfDataports).build();

        /*sfDescMon = new SfcSfDescMonBuilder().setDescriptionInfo(descInfo).build();

        ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder().setKey(serviceFunctionStateKey)
            .addAugmentation(ServiceFunctionState1.class, sfState1)
            .build();
        SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);*/

        assertTrue(SfcNetconfServiceFunctionAPI.putServiceFunctionDescription(descInfo, sfName));

        SfcSfDescMon readSfcSfDescMon =
                SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfName);
        Long numPorts = 1L;
        assertNotNull("Must be not null", readSfcSfDescMon);
        assertEquals("Must be equal", cap, readSfcSfDescMon.getDescriptionInfo().getCapabilities());
        assertEquals("Must be equal", numPorts, readSfcSfDescMon.getDescriptionInfo().getNumberOfDataports());
    }

    @Test
    public void testCreateReadServiceFunctionMonitor() {
        SfName sfName = new SfName("unittest-fw-2");

        //SfcSfDescMon sfDescMon;

        ///ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(new SfName("unittest-fw-2"));

        List<PortBandwidthUtilization> portBandwidthUtilList = new ArrayList<>();

        Long[] data;
        data = new Long[10];
        for (int i = 0; i < 10; i++) {
            data[i] = Long.parseLong(Integer.toString(i + 1));
        }
        PortBandwidthUtilizationKey portBandwidthUtilKey = new PortBandwidthUtilizationKey(data[0]);
        PortBandwidthUtilization portBandwidthUtil = new PortBandwidthUtilizationBuilder()
            .setBandwidthUtilization(data[2]).setKey(portBandwidthUtilKey).setPortId(data[0]).build();
        portBandwidthUtilList.add(portBandwidthUtil);

        SFPortsBandwidthUtilization sfPortsBandwidthUtil =
                new SFPortsBandwidthUtilizationBuilder().setPortBandwidthUtilization(portBandwidthUtilList).build();

        ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setAvailableMemory(data[1])
            .setBandwidthUtilization(data[2])
            .setCPUUtilization(data[3])
            .setFIBUtilization(data[4])
            .setRIBUtilization(data[5])
            .setMemoryUtilization(data[6])
            .setPacketRateUtilization(data[7])
            .setPowerUtilization(data[8])
            .setSFPortsBandwidthUtilization(sfPortsBandwidthUtil)
            .build();

        // sf monitor data
        MonitoringInfo monInfo =
                new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).setLiveness(true).build();

        assertTrue(SfcNetconfServiceFunctionAPI.putServiceFunctionMonitor(monInfo, sfName));

        /*sfDescMon = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo).build();

        ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder().setKey(serviceFunctionStateKey)
            .addAugmentation(ServiceFunctionState1.class, sfState1)
            .build();
        assertTrue(SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState));*/
        SfcSfDescMon readSfcSfDescMon =
                SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfName);
        assertNotNull("Must be not null", readSfcSfDescMon);
        assertEquals("Must be equal", resrcUtil, readSfcSfDescMon.getMonitoringInfo().getResourceUtilization());
        assertTrue("Must be true", readSfcSfDescMon.getMonitoringInfo().isLiveness());
    }

    /*
     * test, whether is possible to put service function monitor & description into service function
     */
    @Test
    public void testPutServiceFunctionDescriptionAndMonitor() throws Exception {
        /* Build DescriptionInfo */
        List<PortBandwidth> portBandwidthList = new ArrayList<>();
        Long[] data;

        data = new Long[10];
        for (int i = 0; i < 10; i++) {
            data[i] = Long.parseLong(Integer.toString(i + 1));
        }
        PortBandwidthKey portBandwidthKey = new PortBandwidthKey(data[0]);
        PortBandwidth portBandwidth = new PortBandwidthBuilder().setIpaddress(new Ipv4Address(IP_MGMT_ADDRESS))
            .setKey(portBandwidthKey)
            .setMacaddress(new MacAddress("00:1e:67:a2:5f:f4"))
            .setPortId(data[0])
            .setSupportedBandwidth(data[1])
            .build();
        portBandwidthList.add(portBandwidth);

        PortsBandwidth portsBandwidth = new PortsBandwidthBuilder().setPortBandwidth(portBandwidthList).build();
        // sf cap
        Capabilities cap = new CapabilitiesBuilder().setPortsBandwidth(portsBandwidth)
            .setFIBSize(data[2])
            .setRIBSize(data[3])
            .setSupportedACLNumber(data[4])
            .setSupportedBandwidth(data[5])
            .setSupportedPacketRate(data[6])
            .build();

        long numberOfDataports = 1;
        DescriptionInfo descInfo =
                new DescriptionInfoBuilder().setDataPlaneIp(new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS)))
                    .setDataPlanePort(new PortNumber(DP_PORT))
                    .setType("firewall")
                    .setCapabilities(cap)
                    .setNumberOfDataports(numberOfDataports)
                    .build();

        /* Build MonitoringInfo, reuse data[] from the above part */
        List<PortBandwidthUtilization> portBandwidthUtilList = new ArrayList<>();
        PortBandwidthUtilizationKey portBandwidthUtilKey = new PortBandwidthUtilizationKey(data[0]);
        PortBandwidthUtilization portBandwidthUtil = new PortBandwidthUtilizationBuilder()
            .setBandwidthUtilization(data[2]).setKey(portBandwidthUtilKey).setPortId(data[0]).build();
        portBandwidthUtilList.add(portBandwidthUtil);

        SFPortsBandwidthUtilization sfPortsBandwidthUtil =
                new SFPortsBandwidthUtilizationBuilder().setPortBandwidthUtilization(portBandwidthUtilList).build();

        ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setAvailableMemory(data[1])
            .setBandwidthUtilization(data[2])
            .setCPUUtilization(data[3])
            .setFIBUtilization(data[4])
            .setRIBUtilization(data[5])
            .setMemoryUtilization(data[6])
            .setPacketRateUtilization(data[7])
            .setPowerUtilization(data[8])
            .setSFPortsBandwidthUtilization(sfPortsBandwidthUtil)
            .build();

        MonitoringInfo monInfo =
                new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).setLiveness(true).build();

        // push service function state with augmentation into data store
        // assertTrue("Must be true", writeServiceFunctionStateAugmentation());

        assertTrue("Must be true", SfcNetconfServiceFunctionAPI.putServiceFunctionDescription(descInfo, SF_NAME));

        assertTrue("Must be true", SfcNetconfServiceFunctionAPI.putServiceFunctionMonitor(monInfo, SF_NAME));
    }

    /**
     * Write service function state with augmentation
     */
    private boolean writeServiceFunctionStateAugmentation() {

        MonitoringInfoBuilder monitoringInfoBuilder = new MonitoringInfoBuilder();
        DescriptionInfoBuilder descriptionInfoBuilder = new DescriptionInfoBuilder();
        SfcSfDescMonBuilder sfcSfDescMonBuilder = new SfcSfDescMonBuilder();
        sfcSfDescMonBuilder.setMonitoringInfo(monitoringInfoBuilder.build())
            .setDescriptionInfo(descriptionInfoBuilder.build());

        SfStateDescMonAugmentationBuilder sfStateDescMonAugmentationBuilder = new SfStateDescMonAugmentationBuilder();
        sfStateDescMonAugmentationBuilder.setSfcSfDescMon(sfcSfDescMonBuilder.build());

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName(SF_STATE_NAME)
            .setKey(new ServiceFunctionStateKey(SF_STATE_NAME))
            .addAugmentation(SfStateDescMonAugmentation.class, sfStateDescMonAugmentationBuilder.build());

        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
            .child(ServiceFunctionState.class, new ServiceFunctionStateKey(SF_STATE_NAME))
            .build();

        return SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);
    }
}

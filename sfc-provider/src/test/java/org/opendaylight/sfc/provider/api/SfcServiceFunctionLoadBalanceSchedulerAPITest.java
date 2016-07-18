/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1Builder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMonBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilizationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcServiceFunctionLoadBalanceSchedulerAPITest extends AbstractDataStoreManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);

    private final List<SfDataPlaneLocator> sfDPLList = new ArrayList<>();
    private final List<ServiceFunction> sfList = new ArrayList<>();
    private ServiceFunctionChain sfChain;
    private ServiceFunctionPath sfPath;
    private SfcServiceFunctionSchedulerAPI scheduler;

    @Before
    public void before() {
        setOdlSfc();

        scheduler = new SfcServiceFunctionLoadBalanceSchedulerAPI();
        int maxTries;

        SfcName sfcName = new SfcName("loadbalance-unittest-chain-1");
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder().setName("firewall")
            .setKey(new SfcServiceFunctionKey("firewall"))
            .setType(new SftTypeName("firewall"))
            .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder().setName("dpi")
            .setKey(new SfcServiceFunctionKey("dpi"))
            .setType(new SftTypeName("dpi"))
            .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder().setName("nat")
            .setKey(new SfcServiceFunctionKey("nat"))
            .setType(new SftTypeName("napt44"))
            .build());

        sfChain = new ServiceFunctionChainBuilder().setName(sfcName)
            .setKey(new ServiceFunctionChainKey(sfcName))
            .setSfcServiceFunction(sfcServiceFunctionList)
            .build();

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey(new SfpName("key")));
        serviceFunctionPathBuilder.setPathId(1L);
        serviceFunctionPathBuilder.setServiceChainName(sfcName);
        List<ServicePathHop> sphs = new ArrayList<>();
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        sfPath = serviceFunctionPathBuilder.build();

        SfDataPlaneLocatorName sfDplName = new SfDataPlaneLocatorName("moscow-5.5.5.5:555-vxlan");
        SffName sffName = new SffName("sff-moscow");
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator(sfDplName,
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.5")), 555), sffName,
                VxlanGpe.class));

        sfDplName = new SfDataPlaneLocatorName("newyork-6.6.6.6:666-vxlan");
        sffName = new SffName("sff-newyork");
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator(sfDplName,
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("6.6.6.6")), 666), sffName,
                VxlanGpe.class));

        sfDplName = new SfDataPlaneLocatorName("paris-7.7.7.7:777-vxlan");
        sffName = new SffName("sff-paris");
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator(sfDplName,
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("7.7.7.7")), 777), sffName,
                VxlanGpe.class));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_100"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.100.101")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_110"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.110.101")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_120"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.120.101")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_dpi_100"), new SftTypeName("dpi"),
                new IpAddress(new Ipv4Address("192.168.100.102")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_dpi_110"), new SftTypeName("dpi"),
                new IpAddress(new Ipv4Address("192.168.110.102")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_dpi_120"), new SftTypeName("dpi"),
                new IpAddress(new Ipv4Address("192.168.120.102")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_nat_100"), new SftTypeName("napt44"),
                new IpAddress(new Ipv4Address("192.168.100.103")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_nat_110"), new SftTypeName("napt44"),
                new IpAddress(new Ipv4Address("192.168.110.103")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_nat_120"), new SftTypeName("napt44"),
                new IpAddress(new Ipv4Address("192.168.120.103")), sfDPLList.get(2), Boolean.FALSE));

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SF_IID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        // Wait a while in order to ensure they are really created

        for (ServiceFunction serviceFunction : sfList) {
            SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction);
        }

        /* Ensure all the ServiceFunctions in sfList are indeed created */
        for (ServiceFunction serviceFunction : sfList) {
            maxTries = 10;
            ServiceFunction sf2 = null;
            while (maxTries > 0) {
                sf2 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunction.getName());
                maxTries--;
                if (sf2 != null) {
                    break;
                }
            }
            LOG.debug("SfcServiceFunctionLoadBalanceSchedulerAPITest: getRead ServiceFunction {} {} times: {}",
                    serviceFunction.getName(), 10 - maxTries, (sf2 == null) ? "Failed" : "Successful");
        }

        // set CPUUtilization for SF
        String sfNameFW = "simple_fw_";
        for (int i = 100; i < 130; i = i + 10) {
            String sCount = i + "";
            SfName sfName = new SfName(sfNameFW.concat(sCount));
            int cpuUtil = i + 5;
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setCPUUtilization((long) cpuUtil).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

        String sfNameDPI = "simple_dpi_";
        for (int i = 100; i < 130; i = i + 10) {
            String sCount = i + "";
            SfName sfName = new SfName(sfNameDPI.concat(sCount));
            int cpuUtil = 190 - i;
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setCPUUtilization((long) cpuUtil).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

        String sfNameNAT = "simple_nat_";
        for (int i = 100; i < 130; i = i + 10) {
            String sCount = i + "";
            SfName sfName = new SfName(sfNameNAT.concat(sCount));

            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setCPUUtilization((long) (i - 90)).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

    }

    @Test
    public void testServiceFunctionLoadBalanceScheduler() {
        SfcProviderServiceChainAPI.putServiceFunctionChain(sfChain);
        ServiceFunctionChain sfc2 = SfcProviderServiceChainAPI.readServiceFunctionChain(sfChain.getName());

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfChain.getSfcServiceFunction());

        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("firewall"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("dpi"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("napt44"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        int serviceIndex = 255;
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("firewall"));
        List<SftServiceFunctionName> sftFirewallList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("dpi"));
        List<SftServiceFunctionName> sftDpiList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("napt44"));
        List<SftServiceFunctionName> sftNapt44List = serviceFunctionType.getSftServiceFunctionName();

        List<SfName> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);

        for (int i = 0; i < 3; i++) {
            SfName sfFWName = new SfName(sftFirewallList.get(i).getName());
            java.lang.Long cPUUtilization =
                    SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfFWName)
                        .getMonitoringInfo()
                        .getResourceUtilization()
                        .getCPUUtilization();
            assertNotNull(cPUUtilization);
        }

        for (int i = 0; i < 3; i++) {
            SfName sfDPIName = new SfName(sftDpiList.get(i).getName());
            java.lang.Long cPUUtilization =
                    SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfDPIName)
                        .getMonitoringInfo()
                        .getResourceUtilization()
                        .getCPUUtilization();
            assertNotNull(cPUUtilization);
        }

        for (int i = 0; i < 3; i++) {
            SfName sfNATName = new SfName(sftNapt44List.get(i).getName());
            java.lang.Long cPUUtilization =
                    SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfNATName)
                        .getMonitoringInfo()
                        .getResourceUtilization()
                        .getCPUUtilization();
            assertNotNull(cPUUtilization);
        }

        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0).getValue(), "simple_fw_100");
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1).getValue(), "simple_dpi_120");
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2).getValue(), "simple_nat_100");
    }

    @Test
    public void loadBalance__OverrideSingleHop() {
        Long pathId = 1L;
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey(new SfpName("key")));
        serviceFunctionPathBuilder.setPathId(pathId);
        serviceFunctionPathBuilder.setServiceChainName(sfChain.getName());

        List<ServicePathHop> sphs = new ArrayList<>();
        sphs.add(buildSFHop(new SffName("SFF2"), new SfName("hop-dpi"), (short) 1));
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        ServiceFunctionPath sfp = serviceFunctionPathBuilder.build();

        List<SfName> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, 1, sfp);
        assertEquals("simple_fw_100", serviceFunctionNameArrayList.get(0).getValue());
        assertEquals("hop-dpi", serviceFunctionNameArrayList.get(1).getValue());
    }

    @Test
    public void loadBalance__OverrideAllHops() {
        Long pathId = 1L;
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey(new SfpName("key")));
        serviceFunctionPathBuilder.setPathId(pathId);
        serviceFunctionPathBuilder.setServiceChainName(sfChain.getName());

        List<ServicePathHop> sphs = new ArrayList<>();
        sphs.add(buildSFHop(new SffName("SFF2"), new SfName("hop-dpi-0"), (short) 0));
        sphs.add(buildSFHop(new SffName("SFF2"), new SfName("hop-dpi-1"), (short) 1));
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        ServiceFunctionPath sfp = serviceFunctionPathBuilder.build();

        List<SfName> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, 1, sfp);
        assertEquals("hop-dpi-0", serviceFunctionNameArrayList.get(0).getValue());
        assertEquals("hop-dpi-1", serviceFunctionNameArrayList.get(1).getValue());
    }

    protected ServicePathHop buildSFHop(SffName sffName, SfName sfName, short index) {
        ServicePathHopBuilder sphb = new ServicePathHopBuilder();
        sphb.setHopNumber(index);
        sphb.setServiceFunctionForwarder(sffName);
        sphb.setServiceFunctionName(sfName);
        return sphb.build();
    }

}

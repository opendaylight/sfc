/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;


import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SfcServiceFunctionLoadBalanceSchedulerAPITest extends BaseSfcSchedulerAPITest {

    private static DataBroker dataBroker;
    private static ExecutorService executor;
    private static OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);

    List<SfDataPlaneLocator> sfDPLList = new ArrayList<>();
    List<ServiceFunction> sfList = new ArrayList<>();
    ServiceFunctionChain sfChain;
    private ServiceFunctionPath sfPath;
    SfcServiceFunctionSchedulerAPI scheduler;
    private static boolean setUpIsDone = false;

    @Before
    public void before() throws ExecutionException, InterruptedException {
        if(setUpIsDone == false){
            dataBroker = getDataBroker();
            opendaylightSfc.setDataProvider(dataBroker);
            executor = opendaylightSfc.getExecutor();
        }
        setUpIsDone = true;
        scheduler = new SfcServiceFunctionLoadBalanceSchedulerAPI();

        /* Delete all the content in SFC data store before unit test */
        int maxTries = 10;
        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;
        boolean emptyFlag = true;
        while (maxTries > 0) {
            emptyFlag = true;
            executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
            Thread.sleep(1000); //Wait for real delete

            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
            if (serviceFunctionType != null) {
                sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (sftServiceFunctionNameList.size() != 0) {
                    emptyFlag = false;
                }
            }

            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Dpi.class);
            if (serviceFunctionType != null) {
                sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (sftServiceFunctionNameList.size() != 0) {
                    emptyFlag = false;
                }
            }

            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Napt44.class);
            if (serviceFunctionType != null) {
                sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (sftServiceFunctionNameList.size() != 0) {
                    emptyFlag = false;
                }
            }

            maxTries--;
            if (emptyFlag == true) {
                break;
            }
        }
        LOG.debug("Empty SFC data store {} times: {}", 10 - maxTries, emptyFlag ? "Successful" : "Failed");

        String sfcName = "loadbalance-unittest-chain-1";
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder()
                                           .setName("firewall")
                                           .setKey(new SfcServiceFunctionKey("firewall"))
                                           .setType(Firewall.class)
                                           .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder()
                                           .setName("dpi")
                                           .setKey(new SfcServiceFunctionKey("dpi"))
                                           .setType(Dpi.class)
                                           .build());
        sfcServiceFunctionList.add(new SfcServiceFunctionBuilder()
                                           .setName("nat")
                                           .setKey(new SfcServiceFunctionKey("nat"))
                                           .setType(Napt44.class)
                                           .build());

        sfChain = new ServiceFunctionChainBuilder()
                .setName(sfcName)
                .setKey(new ServiceFunctionChainKey(sfcName))
                .setSfcServiceFunction(sfcServiceFunctionList)
                .build();

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey("key"));
        serviceFunctionPathBuilder.setPathId(1l);
        serviceFunctionPathBuilder.setServiceChainName(sfcName);
        List<ServicePathHop> sphs = new ArrayList<ServicePathHop>();
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        sfPath = serviceFunctionPathBuilder.build();

        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("moscow-5.5.5.5:555-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.5")), 555),
                "sff-moscow", VxlanGpe.class));
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("newyork-6.6.6.6:666-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("6.6.6.6")), 666),
                "sff-newyork", VxlanGpe.class));
        sfDPLList.add(SimpleTestEntityBuilder.buildSfDataPlaneLocator("paris-7.7.7.7:777-vxlan",
                SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("7.7.7.7")), 777),
                "sff-paris", VxlanGpe.class));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_100", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.101")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_110", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.110.101")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_120", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.120.101")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_100", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.100.102")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_110", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.110.102")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_dpi_120", Dpi.class,
                new IpAddress(new Ipv4Address("192.168.120.102")), sfDPLList.get(2), Boolean.FALSE));

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_100", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.100.103")), sfDPLList.get(0), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_110", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.110.103")), sfDPLList.get(1), Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_nat_120", Napt44.class,
                new IpAddress(new Ipv4Address("192.168.120.103")), sfDPLList.get(2), Boolean.FALSE));

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();
        //Wait a while in order to ensure they are really created
        Thread.sleep(1000);

        for (ServiceFunction serviceFunction: sfList) {
            SfcProviderServiceTypeAPI.createServiceFunctionTypeEntryExecutor(serviceFunction);
        }

        /* Ensure all the ServiceFunctions in sfList are indeed created */
        for (ServiceFunction serviceFunction : sfList) {
            maxTries = 10;
            ServiceFunction sf2 = null;
            while (maxTries > 0) {
                Object[] parameters2 = {serviceFunction.getName()};
                Class[] parameterTypes2 = {String.class};
                Object result = executor.submit(SfcProviderServiceFunctionAPI
                    .getRead(parameters2, parameterTypes2)).get();
                sf2 = (ServiceFunction) result;
                maxTries--;
                if (sf2 != null) {
                    break;
                }
                Thread.sleep(1000);
            }
            LOG.debug("SfcServiceFunctionLoadBalanceSchedulerAPITest: getRead ServiceFunction {} {} times: {}", serviceFunction.getName(), 10 - maxTries, (sf2 == null) ? "Failed" : "Successful");
        }

        // set CPUUtilization for SF
        String sfNameFW = "simple_fw_";
        for (int i=100; i<130; i=i+10){
            String sCount = i+"";
            String sfName = sfNameFW.concat(sCount);
            int cpuUtil = i+5;
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder()
                                                .setCPUUtilization((long)cpuUtil).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder()
                                         .setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder()
                                         .setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder()
                                                 .setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                                                            .setKey(serviceFunctionStateKey)
                                                            .addAugmentation(ServiceFunctionState1.class,sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

        String sfNameDPI = "simple_dpi_";
        for (int i=100; i<130; i=i+10){
            String sCount = i+"";
            String sfName = sfNameDPI.concat(sCount);
            int cpuUtil = 190-i;
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder()
                                                .setCPUUtilization((long)cpuUtil).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder()
                                         .setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder()
                                         .setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder()
                                                 .setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                                                            .setKey(serviceFunctionStateKey)
                                                            .addAugmentation(ServiceFunctionState1.class,sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

        String sfNameNAT = "simple_nat_";
        for (int i=100; i<130; i=i+10){
            String sCount = i+"";
            String sfName = sfNameNAT.concat(sCount);

            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder()
                                                .setCPUUtilization((long)(i-90)).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder()
                                         .setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder()
                                         .setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder()
                                                 .setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                                                            .setKey(serviceFunctionStateKey)
                                                            .addAugmentation(ServiceFunctionState1.class,sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

    }

//    @After
//    public void after() {
//        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//    }

    @Test
    public void testServiceFunctionLoadBalanceScheduler() throws ExecutionException, InterruptedException {
        Object[] sfcParameters = {sfChain};
        Class[] sfcParameterTypes = {ServiceFunctionChain.class};
        executor.submit(SfcProviderServiceChainAPI
                .getPut(sfcParameters, sfcParameterTypes)).get();

        Object[] parameters2 = {sfChain.getName()};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceChainAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfChain.getSfcServiceFunction());

        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Dpi.class);
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Napt44.class);
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 3);

        int serviceIndex = 255;
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Firewall.class);
        List<SftServiceFunctionName> sftFirewallList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Dpi.class);
        List<SftServiceFunctionName> sftDpiList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(Napt44.class);
        List<SftServiceFunctionName> sftNapt44List = serviceFunctionType.getSftServiceFunctionName();

        List<String> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);

        for (int i=0; i<3; i++){
            String sfFWName = sftFirewallList.get(i).getName();
            java.lang.Long cPUUtilization = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor(sfFWName)
                                            .getMonitoringInfo()
                                            .getResourceUtilization()
                                            .getCPUUtilization();
            assertNotNull(cPUUtilization);
        }

        for (int i=0; i<3; i++){
            String sfDPIName = sftDpiList.get(i).getName();
            java.lang.Long cPUUtilization = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor(sfDPIName)
                                            .getMonitoringInfo()
                                            .getResourceUtilization()
                                            .getCPUUtilization();
            assertNotNull(cPUUtilization);
        }

        for (int i=0; i<3; i++){
            String sfNATName = sftNapt44List.get(i).getName();
            java.lang.Long cPUUtilization = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor(sfNATName)
                                            .getMonitoringInfo()
                                            .getResourceUtilization()
                                            .getCPUUtilization();
            assertNotNull(cPUUtilization);
        }

        assertEquals("Must be equal", serviceFunctionNameArrayList.get(0), "simple_fw_100");
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(1), "simple_dpi_120");
        assertEquals("Must be equal", serviceFunctionNameArrayList.get(2), "simple_nat_100");
    }

    @Test
    public void loadBalance__OverrideSingleHop() throws Exception {
        Long pathId = 1L;
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey("key"));
        serviceFunctionPathBuilder.setPathId(pathId);
        serviceFunctionPathBuilder.setServiceChainName(sfChain.getName());

        List<ServicePathHop> sphs = new ArrayList<ServicePathHop>();
        sphs.add(buildSFHop("SFF2", "hop-dpi", (short)1));
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        ServiceFunctionPath sfp = serviceFunctionPathBuilder.build();

        List<String> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, 1, sfp);
        assertEquals("simple_fw_100", serviceFunctionNameArrayList.get(0));
        assertEquals("hop-dpi", serviceFunctionNameArrayList.get(1));
    }

    @Test
    public void loadBalance__OverrideAllHops() throws Exception {
        Long pathId = 1L;
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey("key"));
        serviceFunctionPathBuilder.setPathId(pathId);
        serviceFunctionPathBuilder.setServiceChainName(sfChain.getName());

        List<ServicePathHop> sphs = new ArrayList<ServicePathHop>();
        sphs.add(buildSFHop("SFF2", "hop-dpi-0", (short)0));
        sphs.add(buildSFHop("SFF2", "hop-dpi-1", (short)1));
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        ServiceFunctionPath sfp = serviceFunctionPathBuilder.build();

        List<String> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, 1, sfp);
        assertEquals("hop-dpi-0", serviceFunctionNameArrayList.get(0));
        assertEquals("hop-dpi-1", serviceFunctionNameArrayList.get(1));
    }


}

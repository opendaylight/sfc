/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1Builder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMonBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutputBuilder;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcProviderSfDescriptionMonitorAPI.class)
public class SfcProviderServiceFunctionAPITest extends AbstractDataBrokerTest {

    private static final String[] LOCATOR_IP_ADDRESS =
            {"196.168.55.1",
                    "196.168.55.2",
                    "196.168.55.3"};
    private static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101",
                    "196.168.55.102",
                    "196.168.55.103"};
    private static final String SF_NAME = "dummySF";
    private static final String SF_STATE_NAME = "dummySFS";
    private static final String SF_SERVICE_PATH = "dummySFSP";
    private static final String RSP_NAME = "dummyRSP";
    private static final int PORT = 555;
    private static ExecutorService executor;
    private static final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private static DataBroker dataBroker;
    private static boolean setUpIsDone = false;

    @Before
    public void before() throws InterruptedException {
        if(setUpIsDone == false){
            dataBroker = getDataBroker();
            opendaylightSfc.setDataProvider(dataBroker);
            executor = opendaylightSfc.getExecutor();
        }
        setUpIsDone = true;

        //clear data store
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        Thread.sleep(1000);
    }

//    @After
//    public void after() throws InterruptedException {
//        //clear data store
//        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
//        Thread.sleep(1000);
//    }

    @Test
    public void testCreateReadServiceFunction() throws ExecutionException, InterruptedException {

        String name = "unittest-fw-1";
        Class<? extends ServiceFunctionTypeIdentity> type = Firewall.class;
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(name);

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(LOCATOR_IP_ADDRESS[0]).setLocatorType(ipBuilder.build());
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(name).setKey(key)
                .setType(type)
                .setIpMgmtAddress(ipMgmtAddress)
                .setSfDataPlaneLocator(dataPlaneLocatorList);

        Object[] parameters = {sfBuilder.build()};
        Class[] parameterTypes = {ServiceFunction.class};

        executor.submit(SfcProviderServiceFunctionAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {name};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunction sf2 = (ServiceFunction) result;

        assertNotNull("Must be not null", sf2);
        assertEquals("Must be equal", sf2.getIpMgmtAddress(), ipMgmtAddress);
        assertEquals("Must be equal", sf2.getType(), type);
        assertEquals("Must be equal", sf2.getSfDataPlaneLocator(), dataPlaneLocatorList);
    }

    @Test
    public void testDeleteServiceFunction() throws ExecutionException, InterruptedException {

        String name = "unittest-fw-1";
        Class<? extends ServiceFunctionTypeIdentity> type = Firewall.class;
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(name);

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(LOCATOR_IP_ADDRESS[0]).setLocatorType(ipBuilder.build());
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(name).setKey(key)
                .setType(type)
                .setIpMgmtAddress(ipMgmtAddress)
                .setSfDataPlaneLocator(dataPlaneLocatorList);

        Object[] parameters = {sfBuilder.build()};
        Class[] parameterTypes = {ServiceFunction.class};

        executor.submit(SfcProviderServiceFunctionAPI
                .getPut(parameters, parameterTypes)).get();

        Object[] parameters2 = {name};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();

        assertNotNull("Must be not null", result);
        assertTrue("Must be ServiceFunction", result instanceof ServiceFunction);

        executor.submit(SfcProviderServiceFunctionAPI
                .getDelete(parameters2, parameterTypes2)).get();
        result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();

        assertNull("Must be null", result);
    }

    @Test
    public void testCreateReadServiceFunctions() throws ExecutionException, InterruptedException {

        final String[] sfName = {"unittest-fw-1", "unittest-fw-2", "unittest-fw-3"};
        final Class<? extends ServiceFunctionTypeIdentity> sfType = Firewall.class;
        final IpAddress[] ipMgmtAddress =
                {new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0])),
                        new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[1])),
                        new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[2]))};
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[3];
        ServiceFunctionKey[] key = new ServiceFunctionKey[3];
        for (int i = 0; i < 3; i++) {
            key[i] = new ServiceFunctionKey(sfName[i]);
        }

        final IpAddress[] locatorIpAddress = {new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0])),
                new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[1])),
                new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[2]))};
        PortNumber portNumber = new PortNumber(PORT);

        List<ServiceFunction> list = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(LOCATOR_IP_ADDRESS[i]).setLocatorType(ipBuilder.build());
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(sfName[i]).setKey(key[i])
                    .setType(sfType)
                    .setIpMgmtAddress(ipMgmtAddress[i])
                    .setSfDataPlaneLocator(dataPlaneLocatorList);
            list.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(list);

        executor.submit(SfcProviderServiceFunctionAPI.getPutAll
                (new Object[]{sfsBuilder.build()}, new Class[]{ServiceFunctions.class})).get();


        Object[] parameters2 = {sfName[1]};
        Class[] parameterTypes2 = {String.class};
        Object result = executor.submit(SfcProviderServiceFunctionAPI
                .getRead(parameters2, parameterTypes2)).get();
        ServiceFunction sf2 = (ServiceFunction) result;

        assertNotNull("Must be not null", sf2);
        assertEquals("Must be equal", sf2.getIpMgmtAddress(), ipMgmtAddress[1]);
        assertEquals("Must be equal", sf2.getType(), sfType);
        List<SfDataPlaneLocator> dataPlaneLocatorList2 = new ArrayList<>();
        dataPlaneLocatorList2.add(sfDataPlaneLocator[1]);
        assertEquals("Must be equal", sf2.getSfDataPlaneLocator(), dataPlaneLocatorList2);
    }

    @Test
    public void testCreateReadServiceFunctionDescription() throws ExecutionException, InterruptedException {
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey("unittest-fw-1");
        SfcSfDescMon sfDescMon;
        List<PortBandwidth> portBandwidthList = new ArrayList<>();

        Long[] data;
        data = new Long[10];
        for(int i = 0; i < 10; i++){
            data[i] = Long.parseLong(Integer.toString(i+1));
        }
        PortBandwidthKey portBandwidthKey = new PortBandwidthKey(data[0]);
        PortBandwidth portBandwidth= new PortBandwidthBuilder()
                .setIpaddress(new Ipv4Address(IP_MGMT_ADDRESS[1]))
                .setKey(portBandwidthKey)
                .setMacaddress(new MacAddress("00:1e:67:a2:5f:f4"))
                .setPortId(data[0])
                .setSupportedBandwidth(data[1]).build();
        portBandwidthList.add(portBandwidth);

        PortsBandwidth portsBandwidth = new PortsBandwidthBuilder()
                .setPortBandwidth(portBandwidthList).build();
        //sf cap
        Capabilities cap = new CapabilitiesBuilder()
                .setPortsBandwidth(portsBandwidth)
                .setFIBSize(data[2])
                .setRIBSize(data[3])
                .setSupportedACLNumber(data[4])
                .setSupportedBandwidth(data[5])
                .setSupportedPacketRate(data[6]).build();

        //sf description
        long numberOfDataports = 1;
        DescriptionInfo descInfo = new DescriptionInfoBuilder()
                .setCapabilities(cap)
                .setNumberOfDataports(numberOfDataports).build();

        sfDescMon = new SfcSfDescMonBuilder()
                .setDescriptionInfo(descInfo).build();

        ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey)
                .addAugmentation(ServiceFunctionState1.class, sfState1).build();
        SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);

        SfcSfDescMon readSfcSfDescMon =
                SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor("unittest-fw-1");
        Long numPorts = 1L;
        assertNotNull("Must be not null", readSfcSfDescMon);
        assertEquals("Must be equal", cap, readSfcSfDescMon.getDescriptionInfo().getCapabilities());
        assertEquals("Must be equal", numPorts, readSfcSfDescMon.getDescriptionInfo().getNumberOfDataports());
    }

    @Test
    public void testCreateReadServiceFunctionMonitor() throws ExecutionException, InterruptedException {
        SfcSfDescMon sfDescMon;

        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey("unittest-fw-2");

        List<PortBandwidthUtilization> portBandwidthUtilList = new ArrayList<>();

        Long[] data;
        data = new Long[10];
        for(int i = 0; i < 10; i++){
            data[i] = Long.parseLong(Integer.toString(i+1));
        }
        PortBandwidthUtilizationKey portBandwidthUtilKey = new PortBandwidthUtilizationKey(data[0]);
        PortBandwidthUtilization portBandwidthUtil = new PortBandwidthUtilizationBuilder()
                .setBandwidthUtilization(data[2])
                .setKey(portBandwidthUtilKey)
                .setPortId(data[0]).build();
        portBandwidthUtilList.add(portBandwidthUtil);

        SFPortsBandwidthUtilization sfPortsBandwidthUtil = new SFPortsBandwidthUtilizationBuilder()
                .setPortBandwidthUtilization(portBandwidthUtilList).build();

        ResourceUtilization resrcUtil = new ResourceUtilizationBuilder()
                .setAvailableMemory(data[1])
                .setBandwidthUtilization(data[2])
                .setCPUUtilization(data[3])
                .setFIBUtilization(data[4])
                .setRIBUtilization(data[5])
                .setMemoryUtilization(data[6])
                .setPacketRateUtilization(data[7])
                .setPowerUtilization(data[8])
                .setSFPortsBandwidthUtilization(sfPortsBandwidthUtil).build();

        //sf monitor data
        MonitoringInfo monInfo = new MonitoringInfoBuilder()
                .setResourceUtilization(resrcUtil)
                .setLiveness(true).build();

        sfDescMon = new SfcSfDescMonBuilder()
                .setMonitoringInfo(monInfo).build();

        ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey)
                .addAugmentation(ServiceFunctionState1.class,sfState1).build();
        SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        SfcSfDescMon readSfcSfDescMon =
                SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor("unittest-fw-2");
        assertNotNull("Must be not null", readSfcSfDescMon);
        assertEquals("Must be equal", resrcUtil, readSfcSfDescMon.getMonitoringInfo().getResourceUtilization());
        assertTrue("Must be true", readSfcSfDescMon.getMonitoringInfo().isLiveness());
    }

    /*
     * service function is created and then read
     * next part of this test removes service function from data store
     */
    @Test
    public void testCreateReadDeleteServiceFunction() throws Exception {

        //create service function and put it to data store
        boolean transactionSuccessful = writeRemoveServiceFunction(IP_MGMT_ADDRESS[1], true);

        assertTrue("Must be true", transactionSuccessful);

        //read service function with its name and return it
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(SF_NAME);

        assertNotNull("Must be not null", serviceFunction);
        assertEquals("Must be equal", serviceFunction.getIpMgmtAddress().getIpv4Address().getValue(), IP_MGMT_ADDRESS[1]);

        //now we delete that service function and check whether it was deleted or not
        transactionSuccessful = writeRemoveServiceFunction(IP_MGMT_ADDRESS[1], false);

        assertTrue("Must be true", transactionSuccessful);
    }

    /*
     * service function state is created and then read
     * next part of this test removes service function state from data store
     */
    @Test
    public void testCreateReadDeleteServiceFunctionState() throws Exception {

        //create service function state and put it to data store
        boolean transactionSuccessful = writeRemoveServiceFunctionState(true);

        assertTrue("Must be true", transactionSuccessful);

        //read service function state with its name
        //list of SfServicePath will be returned
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionStateExecutor(SF_STATE_NAME);

        assertNotNull("Must not be null", sfServicePathList);
        assertEquals("Must be equal", sfServicePathList.get(0).getName(), SF_SERVICE_PATH);

        //read service function state with its name
        //list of Strings representing paths will be returned
        List<String> rspList = SfcProviderServiceFunctionAPI.readServiceFunctionStateAsStringListExecutor(SF_STATE_NAME);

        assertNotNull("Must not be null", rspList);
        assertEquals("Must be equal", rspList.get(0), SF_SERVICE_PATH);

        //now we delete that service function state and check whether it was deleted or not
        transactionSuccessful = writeRemoveServiceFunctionState(false);

        assertTrue("Must be true", transactionSuccessful);
    }

    /*
     * list of service functions is created and read
     * next part of this test removes these service functions from data store
     */
    @Test
    public void testReadAllServiceFunctionsExecutor() throws Exception {

        //create service functions
        boolean transactionSuccessful = writeRemoveServiceFunctions(IP_MGMT_ADDRESS[0], Firewall.class, true);

        assertTrue("Must be true", transactionSuccessful);

        //read all service functions from data store
        ServiceFunctions serviceFunctionsResult = SfcProviderServiceFunctionAPI.readAllServiceFunctionsExecutor();

        assertNotNull("Must not be null", serviceFunctionsResult);
        assertEquals("Must be equal", serviceFunctionsResult.getServiceFunction().get(0).getName(), SF_NAME);
        assertEquals("Must be equal", serviceFunctionsResult.getServiceFunction().get(0).getIpMgmtAddress().getIpv4Address().getValue(), IP_MGMT_ADDRESS[0]);
        assertEquals("Must be equal", serviceFunctionsResult.getServiceFunction().get(0).getType(), Firewall.class);

        //delete these functions
        transactionSuccessful = writeRemoveServiceFunctions(IP_MGMT_ADDRESS[1], Firewall.class, false);

        assertTrue("Must be true", transactionSuccessful);
    }

    /*
     * service function state is created and a path is added into it
     * then, path will be removed
     */
    @Test
    public void testAddPathToServiceFunctionStateExecutorString() throws Exception {

        //first, create service function state without paths
        boolean transactionSuccessful = writeRemoveServiceFunctionState();

        assertTrue("Must be true", transactionSuccessful);

        //second, create path and write it into data store
        transactionSuccessful = (boolean)writeReturnPath(RSP_NAME + 1, SF_NAME + 1, true);

        assertTrue("Must be true", transactionSuccessful);

        //add this path to service function, a name of service path is used
        boolean result = SfcProviderServiceFunctionAPI.addPathToServiceFunctionStateExecutor(RSP_NAME + 1);

        assertTrue("Must be true", result);

        //now create another path, and put object as a parameter
        RenderedServicePath renderedServicePath = (RenderedServicePath)writeReturnPath(RSP_NAME + 2, SF_NAME + 2, false);

        //add this path to service function, an object of service path is used
        result = SfcProviderServiceFunctionAPI.addPathToServiceFunctionStateExecutor(renderedServicePath);

        assertTrue("Must be true", result);

        //create path object
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(RSP_NAME + 1);

        //delete both paths; first through created path object, second through path name
        result = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor(serviceFunctionPathBuilder.build());

        assertTrue("Must be true", result);

        result = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionStateExecutor(RSP_NAME + 2);

        assertTrue("Must be true", result);
    }

    /*
     * test, whether is possible to put service function monitor & description into service function
     */
    @Test
    public void testPutServiceFunctionDescriptionAndMonitor() throws Exception {

        //create description
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfoBuilder descriptionInfoBuilder
                = new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfoBuilder();
        GetSFDescriptionOutputBuilder getSFDescriptionOutputBuilder = new GetSFDescriptionOutputBuilder();

        descriptionInfoBuilder.setNumberOfDataports(1L);
        getSFDescriptionOutputBuilder.setDescriptionInfo(descriptionInfoBuilder.build());

        //create monitor
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfoBuilder monitoringInfoBuilder
                = new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfoBuilder();
        GetSFMonitoringInfoOutputBuilder getSFMonitoringInfoOutputBuilder = new GetSFMonitoringInfoOutputBuilder();

        getSFMonitoringInfoOutputBuilder.setMonitoringInfo(monitoringInfoBuilder.build());

        //push service function state with augmentation into data store
        boolean transactionSuccessful = writeServiceFunctionStateAugmentation();

        assertTrue("Must be true", transactionSuccessful);

        //build service function
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME)
                .setKey(new ServiceFunctionKey(SF_NAME))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[1])));

        PowerMockito.stub(PowerMockito.method(SfcProviderSfDescriptionMonitorAPI.class, "getSFDescriptionInfoFromNetconf")).toReturn(getSFDescriptionOutputBuilder.build());
        PowerMockito.stub(PowerMockito.method(SfcProviderSfDescriptionMonitorAPI.class, "getSFMonitorInfoFromNetconf")).toReturn(getSFMonitoringInfoOutputBuilder.build());

        boolean result = SfcProviderServiceFunctionAPI.putServiceFunctionDescriptionExecutor(serviceFunctionBuilder.build());

        assertTrue("Must be true", result);

        result = SfcProviderServiceFunctionAPI.putServiceFunctionMonitorExecutor(serviceFunctionBuilder.build());

        assertTrue("Must be true", result);
    }

    //write or remove service function
    private boolean writeRemoveServiceFunction(String ipAddress, boolean write) {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME)
                .setKey(new ServiceFunctionKey(SF_NAME))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)));

        InstanceIdentifier<ServiceFunction> sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, new ServiceFunctionKey(SF_NAME)).build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sfIID, serviceFunctionBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sfIID, LogicalDatastoreType.CONFIGURATION);
    }

    //write or remove service functions
    private boolean writeRemoveServiceFunctions(String ipAddress, Class<? extends ServiceFunctionTypeIdentity> type, boolean write) {
        ServiceFunctionsBuilder serviceFunctionsBuilder = new ServiceFunctionsBuilder();
        List<ServiceFunction> serviceFunctions = new ArrayList<>();
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();

        serviceFunctionBuilder.setName(SF_NAME)
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)))
                .setKey(new ServiceFunctionKey(SF_NAME))
                .setNshAware(true)
                .setType(type);
        serviceFunctions.add(serviceFunctionBuilder.build());
        serviceFunctionsBuilder.setServiceFunction(serviceFunctions);

        InstanceIdentifier<ServiceFunctions> sfsIID =
                InstanceIdentifier.builder(ServiceFunctions.class).build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sfsIID, serviceFunctionsBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sfsIID, LogicalDatastoreType.CONFIGURATION);
    }

    //write or remove service function state
    private boolean writeRemoveServiceFunctionState() {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        List<SfServicePath> sfServicePathList = new ArrayList<>();

        serviceFunctionStateBuilder.setName(SF_STATE_NAME)
                .setKey(new ServiceFunctionStateKey(SF_STATE_NAME))
                .setSfServicePath(sfServicePathList);

        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, new ServiceFunctionStateKey(SF_STATE_NAME))
                        .build();

        return SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
    }

    //write or remove service function state with path
    private boolean writeRemoveServiceFunctionState(boolean write) {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        List<SfServicePath> sfServicePathList = new ArrayList<>();

        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setName(SF_SERVICE_PATH)
                .setKey(new SfServicePathKey(SF_SERVICE_PATH));
        sfServicePathList.add(sfServicePathBuilder.build());

        serviceFunctionStateBuilder.setName(SF_STATE_NAME)
                .setKey(new ServiceFunctionStateKey(SF_STATE_NAME))
                .setSfServicePath(sfServicePathList);

        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, new ServiceFunctionStateKey(SF_STATE_NAME))
                        .build();
        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
    }

    //write or remove service function state with augmentation
    private boolean writeServiceFunctionStateAugmentation() {

        MonitoringInfoBuilder monitoringInfoBuilder1 = new MonitoringInfoBuilder();
        DescriptionInfoBuilder descriptionInfoBuilder1 = new DescriptionInfoBuilder();
        SfcSfDescMonBuilder sfcSfDescMonBuilder = new SfcSfDescMonBuilder();
        sfcSfDescMonBuilder.setMonitoringInfo(monitoringInfoBuilder1.build())
                .setDescriptionInfo(descriptionInfoBuilder1.build());

        ServiceFunctionState1Builder serviceFunctionState1Builder = new ServiceFunctionState1Builder();
        serviceFunctionState1Builder.setSfcSfDescMon(sfcSfDescMonBuilder.build());

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName(SF_STATE_NAME)
                .setKey(new ServiceFunctionStateKey(SF_STATE_NAME))
                .addAugmentation(ServiceFunctionState1.class, serviceFunctionState1Builder.build());

        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier
                .builder(ServiceFunctionsState.class)
                .child(ServiceFunctionState.class, new ServiceFunctionStateKey(SF_STATE_NAME))
                .build();


        return SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
    }

    //write path or write path and return path object
    private Object writeReturnPath(String pathName, String sfName, boolean write) {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        List<RenderedServicePathHop> renderedServicePathHops = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        renderedServicePathHopBuilder.setServiceFunctionName(sfName)
                .setKey(new RenderedServicePathHopKey(Short.valueOf("1")));
        renderedServicePathHops.add(renderedServicePathHopBuilder.build());

        renderedServicePathBuilder.setName(pathName)
                .setKey(new RenderedServicePathKey(pathName))
                .setRenderedServicePathHop(renderedServicePathHops);

        InstanceIdentifier<RenderedServicePath> rspIID =
                InstanceIdentifier.builder(RenderedServicePaths.class)
                        .child(RenderedServicePath.class, new RenderedServicePathKey(pathName))
                        .build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        else {
            SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(), LogicalDatastoreType.OPERATIONAL);
            return renderedServicePathBuilder.build();
        }
    }
}
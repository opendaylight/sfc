/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
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
import static org.junit.Assert.*;


public class SfcServiceFunctionLoadPathAwareSchedulerAPITest extends AbstractDataStoreManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionLoadPathAwareSchedulerAPITest.class);

    private final List<ServiceFunction> sfList = new ArrayList<>();
    private ServiceFunctionChain sfChain;
    private ServiceFunctionPath sfPath;
    private SfcServiceFunctionLoadPathAwareSchedulerAPI scheduler;

    @Before
    public void before() {
        setupSfc();

        scheduler = new SfcServiceFunctionLoadPathAwareSchedulerAPI();
        // build SFs

        final List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("196.168.55.1");
                add("196.168.55.2");
                add("196.168.55.3");
                add("196.168.55.4");
                add("196.168.55.5");
                add("196.168.55.6");
                add("196.168.55.7");
                add("196.168.55.8");
                add("196.168.55.9");
                add("196.168.55.10");
                add("196.168.55.11");
                add("196.168.55.12");
            }
        };

        final List<String> IP_MGMT_ADDRESS = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("196.168.55.101");
                add("196.168.55.102");
                add("196.168.55.103");
                add("196.168.55.104");
                add("196.168.55.105");
                add("196.168.55.106");
                add("196.168.55.107");
                add("196.168.55.108");
                add("196.168.55.109");
                add("196.168.55.110");
            }
        };

        final int PORT = 555;

        final List<SfName> SF_NAMES = new ArrayList<SfName>() {
            private static final long serialVersionUID = 1L;

            {
                add(new SfName("fw_1"));
                add(new SfName("dpi_1"));
                add(new SfName("fw_2"));
                add(new SfName("dpi_2"));
                add(new SfName("fw_3"));
                add(new SfName("dpi_3"));
                add(new SfName("nat_3"));
                add(new SfName("fw_4"));
                add(new SfName("dpi_4"));
                add(new SfName("nat_4"));
            }
        };

        final List<SftTypeName> SF_TYPES = new ArrayList<SftTypeName>() {
            private static final long serialVersionUID = 1L;

            {
                add(new SftTypeName("firewall"));
                add(new SftTypeName("dpi"));
                add(new SftTypeName("firewall"));
                add(new SftTypeName("dpi"));
                add(new SftTypeName("firewall"));
                add(new SftTypeName("dpi"));
                add(new SftTypeName("napt44"));
                add(new SftTypeName("firewall"));
                add(new SftTypeName("dpi"));
                add(new SftTypeName("napt44"));
            }
        };

        PortNumber portNumber = new PortNumber(PORT);
        for (int i = 0; i < SF_NAMES.size(); i++) {
            IpAddress ipMgmtAddr = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(i)));
            IpAddress dplIpAddr = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(i)));
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(dplIpAddr).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(i)))
                    .setLocatorType(ipBuilder.build());
            SfDataPlaneLocator sfDataPlaneLocator = locatorBuilder.build();
            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator);
            ServiceFunctionKey serviceFunctonKey = new ServiceFunctionKey(new SfName(SF_NAMES.get(i)));
            sfBuilder.setName(new SfName(SF_NAMES.get(i)))
                    .setKey(serviceFunctonKey)
                    .setType(SF_TYPES.get(i))
                    .setIpMgmtAddress(ipMgmtAddr)
                    .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        /* Must create ServiceFunctionType first */
        for (ServiceFunction serviceFunction : sfList) {
            boolean ret = SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction);
            LOG.debug("call createServiceFunctionTypeEntryExecutor for {}", serviceFunction.getName());
            assertTrue("Must be true", ret);
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);
        SfcDataStoreAPI.writePutTransactionAPI(SfcInstanceIdentifiers.SF_IID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        SfcName sfcName = new SfcName("LoadPathAware-unittest-chain-1");
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        List<String> sftNames = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("firewall");
                add("dpi");
                add("napt");
            }
        };
        List<SftTypeName> sftClasses = new ArrayList<SftTypeName>() {
            private static final long serialVersionUID = 1L;

            {
                add(new SftTypeName("firewall"));
                add(new SftTypeName("dpi"));
                add(new SftTypeName("napt44"));
            }
        };
        for (int i = 0; i < sftNames.size(); i++) {
            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            sfcServiceFunctionBuilder.setName(sftNames.get(i));
            sfcServiceFunctionBuilder.setKey(new SfcServiceFunctionKey(sftNames.get(i)));
            sfcServiceFunctionBuilder.setType(sftClasses.get(i));
            sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        }

        sfChain = new ServiceFunctionChainBuilder().setName(sfcName)
                .setKey(new ServiceFunctionChainKey(sfcName))
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(false)
                .build();

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey(new SfpName("key")));
        serviceFunctionPathBuilder.setPathId(1l);
        serviceFunctionPathBuilder.setServiceChainName(sfcName);
        List<ServicePathHop> sphs = new ArrayList<>();
        serviceFunctionPathBuilder.setServicePathHop(sphs);
        sfPath = serviceFunctionPathBuilder.build();

        // build SFFs
        String[][] TO_SFF_NAMES = {{"SFF2"}, {"SFF1","SFF3"}, {"SFF2","SFF4"},{"SFF3"}};

        List<SffName> SFF_NAMES = new ArrayList<SffName>() {
            private static final long serialVersionUID = 1L;

            {
                add(new SffName("SFF1"));
                add(new SffName("SFF2"));
                add(new SffName("SFF3"));
                add(new SffName("SFF4"));
            }
        };

        List<String> SFF_LOCATOR_IP = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("196.168.66.101");
                add("196.168.66.102");
                add("196.168.66.103");
                add("196.168.66.104");
            }
        };

        for (int i = 0; i < SFF_NAMES.size(); i++) {
            /*
             * ServiceFunctionForwarders connected to special SFF_NAMES[i]
             * * SFF_NAMES[1] is connected to SFF_NAMES[2]
             * * SFF_NAMES[2] is connected to SFF_NAMES[1] and SFF_NAMES[3]
             * * SFF_NAMES[3] is connected to SFF_NAMES[2] and SFF_NAMES[4]
             * * SFF_NAMES[4] is connected to SFF_NAMES[3]
            */
            List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                if (i == 0 & j == 1  || i==3 & j == 1) {
                    break;
                }
                ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
                ConnectedSffDictionary sffDictEntry =
                        sffDictionaryEntryBuilder.setName(new SffName(TO_SFF_NAMES[i][j])).build();
                sffDictionaryList.add(sffDictEntry);
            }

            // ServiceFunctions attached to SFF_NAMES[i]
            List<ServiceFunctionDictionary> sfDictionaryList = new ArrayList<>();
            ServiceFunction serviceFunction = null;
            for (int j = 0; j < 3; j++) {
                if (i == 0 ) {
                    if (j == 2) {
                        break;
                    }
                    serviceFunction = sfList.get(i * 3 + j);
                } else if (i == 1) {
                    if (j == 2) {
                        break;
                    }
                    serviceFunction = sfList.get(i * 3 + j - 1);
                } else {
                   serviceFunction = sfList.get(i * 3 + j - 2);
                }

                SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder =
                        new SffSfDataPlaneLocatorBuilder();
                sffSfDataPlaneLocatorBuilder.setSfDplName(serviceFunction.getSfDataPlaneLocator().get(0).getName());
                SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
                ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
                dictionaryEntryBuilder.setName(serviceFunction.getName())
                        .setKey(new ServiceFunctionDictionaryKey(serviceFunction.getName()))
                        .setSffSfDataPlaneLocator(sffSfDataPlaneLocator)
                        .setFailmode(Open.class)
                        .setSffInterfaces(null);
                ServiceFunctionDictionary sfDictEntry = dictionaryEntryBuilder.build();
                sfDictionaryList.add(sfDictEntry);
            }

            List<SffDataPlaneLocator> locatorList = new ArrayList<>();
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(SFF_LOCATOR_IP.get(i)))).setPort(new PortNumber(555));
            DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
            sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);
            SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SffDataPlaneLocatorName(SFF_LOCATOR_IP.get(i)))
                    .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName(SFF_LOCATOR_IP.get(i))))
                    .setDataPlaneLocator(sffLocatorBuilder.build());
            locatorList.add(locatorBuilder.build());
            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
            sffBuilder.setName(new SffName(SFF_NAMES.get(i)))
                    .setKey(new ServiceFunctionForwarderKey(new SffName(SFF_NAMES.get(i))))
                    .setSffDataPlaneLocator(locatorList)
                    .setServiceFunctionDictionary(sfDictionaryList)
                    .setConnectedSffDictionary(sffDictionaryList)
                    .setServiceNode(null);
            ServiceFunctionForwarder sff = sffBuilder.build();
            SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);
        }

        // Set CPUUtilization for each ServiceFunctions
        String sfNameFW = "fw_";
        for (int i = 1; i < 5; i = i + 1) {
            String sCount = i + "";
            SfName sfName = new SfName(sfNameFW.concat(sCount));
            int cpuUtil = 0;
            if ( i == 1) {
                cpuUtil = 15;
            } else {
                cpuUtil = (i-1) * 5;
            }
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setCPUUtilization((long) cpuUtil).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                    .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
            LOG.debug("Setting ServiceFunction {}'s CPU : {}",
                    sfName,cpuUtil);
        }

        String sfNameDPI = "dpi_";
        for (int i = 1; i < 5; i = i + 1) {
            String sCount = i + "";
            SfName sfName = new SfName(sfNameDPI.concat(sCount));
            int cpuUtil = i * 10;
            if (i == 4) {
               cpuUtil = 5;
            }
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setCPUUtilization((long) cpuUtil).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                    .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
            LOG.debug("Setting ServiceFunction {}'s CPU : {}",
                    sfName, cpuUtil);
        }

        String sfNameNAT = "nat_";
        for (int i = 3; i < 5; i = i + 1) {
            String sCount = i + "";
            SfName sfName = new SfName(sfNameNAT.concat(sCount));
            int cpuUtil = i*10 - 20;
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setCPUUtilization((long) cpuUtil).build();
            MonitoringInfo monInfo = new MonitoringInfoBuilder().setResourceUtilization(resrcUtil).build();
            SfcSfDescMon sfDescMon = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo).build();
            ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
            ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                    .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();
            SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
            LOG.debug("Setting ServiceFunction {}'s CPU : {}",
                    sfName, cpuUtil);
        }
    }

    @Test
    public void testSfcServiceFunctionLoadPathAwareScheduler() {
        int maxTries;

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
            LOG.debug("SfcServiceFunctionLoadPathAwareSchedulerAPITest: getRead ServiceFunction {} {} times: {}",
                    serviceFunction.getName(), 10 - maxTries, sf2 != null ? "Successful" : "Failed");
            assertNotNull("Must be not null", sf2);
            assertEquals("Must be equal", sf2.getName(), serviceFunction.getName());
            assertEquals("Must be equal", sf2.getType(), serviceFunction.getType());
        }

        SfcProviderServiceChainAPI.putServiceFunctionChain(sfChain);
        ServiceFunctionChain sfc2 = SfcProviderServiceChainAPI.readServiceFunctionChain(sfChain.getName());

        assertNotNull("Must be not null", sfc2);
        assertNotNull("Must be not null", sfChain.getSfcServiceFunction());
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfChain.getSfcServiceFunction());
        for (SfcServiceFunction sfcServiceFunction : sfChain.getSfcServiceFunction()) {
            LOG.debug("sfcServiceFunction.name = {}", sfcServiceFunction.getName());
            ServiceFunctionType serviceFunctionType =
                    SfcProviderServiceTypeAPI.readServiceFunctionType(sfcServiceFunction.getType());
            assertNotNull("Must be not null", serviceFunctionType);
        }


        ServiceFunctionType serviceFunctionType;
        List<SftServiceFunctionName> sftServiceFunctionNameList;

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("firewall"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 4);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("dpi"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 4);

        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("napt44"));
        sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        assertNotNull("Must be not null", sftServiceFunctionNameList);
        assertEquals("Must be equal", sftServiceFunctionNameList.size(), 2);

        int serviceIndex = 255;
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("firewall"));
        List<SftServiceFunctionName> sftFirewallList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("dpi"));
        List<SftServiceFunctionName> sftDpiList = serviceFunctionType.getSftServiceFunctionName();
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName("napt44"));
        List<SftServiceFunctionName> sftNapt44List = serviceFunctionType.getSftServiceFunctionName();

        List<SfName> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);

        for (int i = 0; i < 4; i++) {
            SfName sfFWName = new SfName(sftFirewallList.get(i).getName());
            java.lang.Long cPUUtilization =
                    SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfFWName)
                            .getMonitoringInfo()
                            .getResourceUtilization()
                            .getCPUUtilization();
            assertNotNull(cPUUtilization);
            LOG.debug("Reading CPU of ServiceFunction {} : {}",sfFWName, cPUUtilization);

        }

        for (int i = 0; i < 4; i++) {
            SfName sfDPIName = new SfName(sftDpiList.get(i).getName());
            java.lang.Long cPUUtilization =
                    SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfDPIName)
                            .getMonitoringInfo()
                            .getResourceUtilization()
                            .getCPUUtilization();
            assertNotNull(cPUUtilization);
            LOG.debug("Reading CPU of ServiceFunction {} : {}",sfDPIName, cPUUtilization);
        }

        for (int i = 0; i < 2; i++) {
            SfName sfNATName = new SfName(sftNapt44List.get(i).getName());
            java.lang.Long cPUUtilization =
                    SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfNATName)
                            .getMonitoringInfo()
                            .getResourceUtilization()
                            .getCPUUtilization();
            assertNotNull(cPUUtilization);
            LOG.debug("Reading CPU of ServiceFunction {} : {}", sfNATName, cPUUtilization);
        }


        ServiceFunction sfHop0 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(0));
        ServiceFunction sfHop1 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(1));
        ServiceFunction sfHop2 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(2));

        assertNotNull("Must be not null", sfHop0);
        assertEquals("Must be equal", sfHop0.getName().getValue(), "fw_2");
        assertNotNull("Must be not null", sfHop1);
        assertEquals("Must be equal", sfHop1.getName().getValue(), "dpi_1");
        assertNotNull("Must be not null", sfHop2);
        assertEquals("Must be equal", sfHop2.getName().getValue(), "nat_3");

        LOG.debug("The rendered service path for chain : {} => {} => {}", sfHop0.getName(), sfHop1.getName(), sfHop2.getName());
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
        LOG.debug("{} => {}", serviceFunctionNameArrayList.get(0).getValue(),serviceFunctionNameArrayList.get(1).getValue());
        assertEquals("fw_2", serviceFunctionNameArrayList.get(0).getValue());
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
LOG.debug("{} => {}", serviceFunctionNameArrayList.get(0).getValue(),serviceFunctionNameArrayList.get(1).getValue());
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

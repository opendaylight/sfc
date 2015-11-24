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
import static org.junit.Assert.assertTrue;

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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcServiceFunctionShortestPathSchedulerAPITest extends AbstractDataStoreManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionShortestPathSchedulerAPITest.class);
    private final List<ServiceFunction> sfList = new ArrayList<>();
    private ServiceFunctionChain sfChain;
    private ServiceFunctionPath sfPath;
    private SfcServiceFunctionShortestPathSchedulerAPI scheduler;

    @Before
    public void before() {
        setOdlSfc();

        scheduler = new SfcServiceFunctionShortestPathSchedulerAPI();
        // build SFs
        final String[] LOCATOR_IP_ADDRESS = {"196.168.55.1", "196.168.55.2", "196.168.55.3", "196.168.55.4",
                "196.168.55.5", "196.168.55.6", "196.168.55.7", "196.168.55.8", "196.168.55.9"};
        final String[] IP_MGMT_ADDRESS = {"196.168.55.101", "196.168.55.102", "196.168.55.103", "196.168.55.104",
                "196.168.55.105", "196.168.55.106", "196.168.55.107", "196.168.55.108", "196.168.55.109"};
        final int PORT = 555;
        final String[] SF_NAMES = {"simple_firewall_100", "simple_napt_100", "simple_dpi_100", "simple_firewall_110",
                "simple_napt_110", "simple_dpi_110", "simple_firewall_120", "simple_napt_120", "simple_dpi_120"};
        final Class[] SF_TYPES = {Firewall.class, Napt44.class, Dpi.class, Firewall.class, Napt44.class, Dpi.class,
                Firewall.class, Napt44.class, Dpi.class};

        PortNumber portNumber = new PortNumber(PORT);
        for (int i = 0; i < SF_NAMES.length; i++) {
            IpAddress ipMgmtAddr = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[i]));
            IpAddress dplIpAddr = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[i]));
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(dplIpAddr).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS[i])).setLocatorType(ipBuilder.build());
            SfDataPlaneLocator sfDataPlaneLocator = locatorBuilder.build();
            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator);
            ServiceFunctionKey serviceFunctonKey = new ServiceFunctionKey(new SfName(SF_NAMES[i]));
            sfBuilder.setName(new SfName(SF_NAMES[i]))
                .setKey(serviceFunctonKey)
                .setType(SF_TYPES[i])
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
        SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SF_IID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        SfcName sfcName = new SfcName("ShortestPath-unittest-chain-1");
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        String[] sftNames = {"firewall", "napt", "dpi"};
        Class[] sftClasses = {Firewall.class, Napt44.class, Dpi.class};
        for (int i = 0; i < sftNames.length; i++) {
            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            sfcServiceFunctionBuilder.setName(sftNames[i]);
            sfcServiceFunctionBuilder.setKey(new SfcServiceFunctionKey(sftNames[i]));
            sfcServiceFunctionBuilder.setType(sftClasses[i]);
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
        String[] SFF_NAMES = {"SFF1", "SFF2", "SFF3"};
        String[][] TO_SFF_NAMES = {{"SFF2", "SFF3"}, {"SFF3", "SFF1"}, {"SFF1", "SFF2"}};
        String[] SFF_LOCATOR_IP = {"196.168.66.101", "196.168.66.102", "196.168.66.103"};

        for (int i = 0; i < SFF_NAMES.length; i++) {
            // ServiceFunctionForwarders connected to SFF_NAMES[i]
            List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
                ConnectedSffDictionary sffDictEntry =
                        sffDictionaryEntryBuilder.setName(new SffName(TO_SFF_NAMES[i][j])).build();
                sffDictionaryList.add(sffDictEntry);
            }

            // ServiceFunctions attached to SFF_NAMES[i]
            List<ServiceFunctionDictionary> sfDictionaryList = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                ServiceFunction serviceFunction = sfList.get(i * 3 + j);
                SfDataPlaneLocator sfDPLocator = serviceFunction.getSfDataPlaneLocator().get(0);
                SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder =
                        new SffSfDataPlaneLocatorBuilder(sfDPLocator);
                SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
                ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
                dictionaryEntryBuilder.setName(serviceFunction.getName())
                    .setKey(new ServiceFunctionDictionaryKey(serviceFunction.getName()))
                    .setType(serviceFunction.getType())
                    .setSffSfDataPlaneLocator(sffSfDataPlaneLocator)
                    .setFailmode(Open.class)
                    .setSffInterfaces(null);
                ServiceFunctionDictionary sfDictEntry = dictionaryEntryBuilder.build();
                sfDictionaryList.add(sfDictEntry);
            }

            List<SffDataPlaneLocator> locatorList = new ArrayList<>();
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[i]))).setPort(new PortNumber(555));
            DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
            sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);
            SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SffDataPlaneLocatorName(SFF_LOCATOR_IP[i]))
                .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName(SFF_LOCATOR_IP[i])))
                .setDataPlaneLocator(sffLocatorBuilder.build());
            locatorList.add(locatorBuilder.build());
            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
            sffBuilder.setName(new SffName(SFF_NAMES[i]))
                .setKey(new ServiceFunctionForwarderKey(new SffName(SFF_NAMES[i])))
                .setSffDataPlaneLocator(locatorList)
                .setServiceFunctionDictionary(sfDictionaryList)
                .setConnectedSffDictionary(sffDictionaryList)
                .setServiceNode(null);
            ServiceFunctionForwarder sff = sffBuilder.build();
            SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);
        }
    }

    @Test
    public void testSfcServiceFunctionShortestPathScheduler() {
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
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPITest: getRead ServiceFunction {} {} times: {}",
                    serviceFunction.getName(), 10 - maxTries, (sf2 != null) ? "Successful" : "Failed");
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

        int serviceIndex = 255;

        List<SfName> serviceFunctionNameArrayList = scheduler.scheduleServiceFunctions(sfChain, serviceIndex, sfPath);
        assertNotNull("Must be not null", serviceFunctionNameArrayList);

        ServiceFunction sfHop0 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(0));
        ServiceFunction sfHop1 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(1));
        ServiceFunction sfHop2 = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionNameArrayList.get(2));

        assertNotNull("Must be not null", sfHop0);
        assertNotNull("Must be not null", sfHop1);
        assertNotNull("Must be not null", sfHop2);
        LOG.debug("The rendered service path for chain {}: {} => {} => {}", sfChain.getName(), sfHop0.getName(),
                sfHop1.getName(), sfHop2.getName());
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

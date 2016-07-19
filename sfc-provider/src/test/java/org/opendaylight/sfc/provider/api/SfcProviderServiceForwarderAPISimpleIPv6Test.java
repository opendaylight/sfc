/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for simple datastore operations on SFFs (i.e. Service Functions are created first)
 */

public class SfcProviderServiceForwarderAPISimpleIPv6Test extends AbstractDataStoreManager {

    @SuppressWarnings("serial")
    private static final List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {

        {
            add("fe80::5efe:c000:0201");
            add("ae80::5efe:c000:0201");
            add("be80::5efe:c000:0201");
            add("ce80::5efe:c000:0201");
            add("de80::5efe:c000:0201");
        }
    };
    @SuppressWarnings("serial")

    private static final List<String> IP_MGMT_ADDRESS = new ArrayList<String>() {

        {
            add("196.168.55.101");
            add("196.168.55.102");
            add("196.168.55.103");
            add("196.168.55.104");
            add("196.168.55.105");
        }
    };

    @SuppressWarnings("serial")
    private static final List<Integer> PORT = new ArrayList<Integer>() {

        {
            add(1111);
            add(2222);
            add(3333);
            add(4444);
            add(5555);
        }
    };

    @SuppressWarnings("serial")
    private static final List<SftTypeName> sfTypes = new ArrayList<SftTypeName>() {

        {
            add(new SftTypeName("firewall"));
            add(new SftTypeName("dpi"));
            add(new SftTypeName("napt44"));
            add(new SftTypeName("http-header-enrichment"));
            add(new SftTypeName("qos"));
        }
    };

    @SuppressWarnings("serial")
    private static final List<String> SF_ABSTRACT_NAMES = new ArrayList<String>() {

        {
            add("firewall");
            add("dpi");
            add("napt");
            add("http-header-enrichment");
            add("qos");
        }
    };

    private static final SfcName SFC_NAME = new SfcName("unittest-chain-1");
    private static final SfpName SFP_NAME = new SfpName("unittest-sfp-1");

    @SuppressWarnings("serial")
    private static final List<SfName> sfNames = new ArrayList<SfName>() {

        {
            add(new SfName("unittest-fw-1"));
            add(new SfName("unittest-dpi-1"));
            add(new SfName("unittest-napt-1"));
            add(new SfName("unittest-http-header-enrichment-1"));
            add(new SfName("unittest-qos-1"));
        }
    };

    @SuppressWarnings("serial")
    private final List<SffName> SFF_NAMES = new ArrayList<SffName>() {

        {
            add(new SffName("SFF1"));
            add(new SffName("SFF2"));
            add(new SffName("SFF3"));
            add(new SffName("SFF4"));
            add(new SffName("SFF5"));
        }
    };

    private final String[][] TO_SFF_NAMES =
            {{"SFF2", "SFF5"}, {"SFF3", "SFF1"}, {"SFF4", "SFF2"}, {"SFF5", "SFF3"}, {"SFF1", "SFF4"}};

    @SuppressWarnings("serial")
    private static final List<SffName> sffName = new ArrayList<SffName>() {

        {
            add(new SffName("unittest-forwarder-1"));
            add(new SffName("unittest-forwarder-2"));
            add(new SffName("unittest-forwarder-3"));
        }
    };

    @SuppressWarnings("serial")
    private final List<String> SFF_LOCATOR_IP = new ArrayList<String>() {

        {
            add("196.168.66.101");
            add("196.168.66.102");
            add("196.168.66.103");
            add("196.168.66.104");
            add("196.168.66.105");
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);

    private List<ServiceFunction> sfList = new ArrayList<>();

    @Before
    public void before() {
        setOdlSfc();

        // Create Service Functions
        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames.size()];
        final IpAddress[] locatorIpAddress = new IpAddress[sfNames.size()];
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[sfNames.size()];
        ServiceFunctionKey[] key = new ServiceFunctionKey[sfNames.size()];
        for (int i = 0; i < sfNames.size(); i++) {
            ipMgmtAddress[i] = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(0)));
            locatorIpAddress[i] = new IpAddress(new Ipv6Address(LOCATOR_IP_ADDRESS.get(0)));
            PortNumber portNumber = new PortNumber(PORT.get(i));
            key[i] = new ServiceFunctionKey(new SfName(sfNames.get(i)));

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(i)))
                .setLocatorType(ipBuilder.build())
                .setServiceFunctionForwarder(new SffName(SFF_NAMES.get(i)));
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(new SfName(sfNames.get(i)))
                .setKey(key[i])
                .setType(sfTypes.get(i))
                .setIpMgmtAddress(ipMgmtAddress[i])
                .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SF_IID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        // Create ServiceFunctionTypeEntry for all ServiceFunctions
        for (ServiceFunction serviceFunction : sfList) {
            boolean ret = SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction);
            LOG.debug("call createServiceFunctionTypeEntryExecutor for {}", serviceFunction.getName());
            assertTrue("Must be true", ret);
        }

        // Create Service Function Forwarders
        for (int i = 0; i < SFF_NAMES.size(); i++) {
            List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
                ConnectedSffDictionary sffDictEntry =
                        sffDictionaryEntryBuilder.setName(new SffName(TO_SFF_NAMES[i][j])).build();
                sffDictionaryList.add(sffDictEntry);
            }

            List<ServiceFunctionDictionary> sfDictionaryList = new ArrayList<>();
            ServiceFunction serviceFunction = sfList.get(i);
            SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
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

            List<SffDataPlaneLocator> locatorList = new ArrayList<>();
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(SFF_LOCATOR_IP.get(i)))).setPort(new PortNumber(PORT.get(i)));
            DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
            sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);
            SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
            SffDataPlaneLocatorName sffDplName = new SffDataPlaneLocatorName(SFF_LOCATOR_IP.get(i));
            locatorBuilder.setName(sffDplName)
                .setKey(new SffDataPlaneLocatorKey(sffDplName))
                .setDataPlaneLocator(sffLocatorBuilder.build());
            locatorList.add(locatorBuilder.build());
            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
            SffName sffName = new SffName(SFF_NAMES.get(i));
            sffBuilder.setName(sffName)
                .setKey(new ServiceFunctionForwarderKey(sffName))
                .setSffDataPlaneLocator(locatorList)
                .setServiceFunctionDictionary(sfDictionaryList)
                .setConnectedSffDictionary(sffDictionaryList)
                .setServiceNode(null);
            ServiceFunctionForwarder sff = sffBuilder.build();
            SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);
        }

        // Create Service Function Chain
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(SFC_NAME);
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < SF_ABSTRACT_NAMES.size(); i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction = sfcSfBuilder.setName(SF_ABSTRACT_NAMES.get(i))
                .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES.get(i)))
                .setType(sfTypes.get(i))
                .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(SFC_NAME).setKey(sfcKey).setSfcServiceFunction(sfcServiceFunctionList).setSymmetric(true);

        SfcProviderServiceChainAPI.putServiceFunctionChain(sfcBuilder.build());

        // Check if Service Function Chain was created
        ServiceFunctionChain sfc2 = SfcProviderServiceChainAPI.readServiceFunctionChain(SFC_NAME);

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(SFP_NAME).setServiceChainName(SFC_NAME).setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        boolean ret = SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath);
        assertTrue("Must be true", ret);
    }

    @Test
    public void testCreateReadUpdateServiceFunctionForwarder() {

        SffName name = new SffName(sffName.get(0));

        List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv6Address("fe80::5efe:c000:0202"))).setPort(new PortNumber(555));

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        SffDataPlaneLocatorName sffDplName = new SffDataPlaneLocatorName("locator-1");
        locatorBuilder.setName(sffDplName)
            .setKey(new SffDataPlaneLocatorKey(sffDplName))
            .setDataPlaneLocator(sffLocatorBuilder.build());

        locatorList.add(locatorBuilder.build());

        List<ServiceFunctionDictionary> dictionary = new ArrayList<>();

        ServiceFunction sf = sfList.get(0);
        sf.getSfDataPlaneLocator().get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder.setSfDplName(sf.getSfDataPlaneLocator().get(0).getName());
        SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder.setName(sf.getName())
            .setKey(new ServiceFunctionDictionaryKey(sf.getName()))
            .setSffSfDataPlaneLocator(sffSfDataPlaneLocator)
            .setFailmode(Open.class)
            .setSffInterfaces(null);

        ServiceFunctionDictionary dictionaryEntry = dictionaryEntryBuilder.build();
        dictionary.add(dictionaryEntry);

        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();

        ServiceFunctionForwarder sff = sffBuilder.setName(name)
            .setKey(new ServiceFunctionForwarderKey(name))
            .setSffDataPlaneLocator(locatorList)
            .setServiceFunctionDictionary(dictionary)
            .setServiceNode(null) // for consistency only; we are going to get rid of ServiceNodes
                                  // in the future
            .build();

        SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);
        ServiceFunctionForwarder sff2 = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(name);
        assertNotNull("Must be not null", sff2);
        assertEquals("Must be equal", sff2.getSffDataPlaneLocator(), locatorList);
        assertEquals("Must be equal", sff2.getServiceFunctionDictionary(), dictionary);
    }
}

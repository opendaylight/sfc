/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPITest;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.InstantiateServiceFunctionChainInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderRpcTest extends AbstractDataStoreManager {

    @SuppressWarnings("serial")
    private final List<SffName> sffNames = new ArrayList<SffName>() {

        {
            add(new SffName("sff1"));
            add(new SffName("sff2"));
            add(new SffName("sff3"));
            add(new SffName("sff4"));
        }
    };

    // XXX TODO Why can't we consolidate these two SFF lists ?
    @SuppressWarnings("serial")
    private final List<SffName> SFF_NAMES = new ArrayList<SffName>() {

        {
            add(new SffName("SFF1"));
            add(new SffName("SFF2"));
            add(new SffName("SFF3"));
            add(new SffName("SFF4"));
            add(new SffName("SFF4"));
        }
    };
    private final String[][] TO_SFF_NAMES =
            {{"SFF2", "SFF5"}, {"SFF3", "SFF1"}, {"SFF4", "SFF2"}, {"SFF5", "SFF3"}, {"SFF1", "SFF4"}};

    List<String> SFF_LOCATOR_IP = new ArrayList<String>() {

        {
            add("196.168.66.101");
            add("196.168.66.102");
            add("196.168.66.103");
            add("196.168.66.104");
            add("196.168.66.105");
        }
    };

    @SuppressWarnings("serial")
    private final List<SffDataPlaneLocatorName> sffDplNames = new ArrayList<SffDataPlaneLocatorName>() {

        {
            add(new SffDataPlaneLocatorName("sffDpl1"));
            add(new SffDataPlaneLocatorName("sffDpl2"));
            add(new SffDataPlaneLocatorName("sffDpl3"));
            add(new SffDataPlaneLocatorName("sffDpl4"));
        }
    };

    @SuppressWarnings("serial")
    private final List<SfName> sfNames = new ArrayList<SfName>() {

        {
            add(new SfName("sf1"));
            add(new SfName("sf2"));
            add(new SfName("sf3"));
            add(new SfName("sf4"));
            add(new SfName("sf5"));
            add(new SfName("sf6"));
            add(new SfName("sf7"));
            add(new SfName("sf8"));
        }
    };

    @SuppressWarnings("serial")
    private static final List<SfName> sfNames2 = new ArrayList<SfName>() {

        {
            add(new SfName("unittest-fw-1"));
            add(new SfName("unittest-dpi-1"));
            add(new SfName("unittest-napt-1"));
            add(new SfName("unittest-http-header-enrichment-1"));
            add(new SfName("unittest-qos-1"));
        }
    };

    @SuppressWarnings("serial")
    private final List<SfDataPlaneLocatorName> sfDplNames = new ArrayList<SfDataPlaneLocatorName>() {

        {
            add(new SfDataPlaneLocatorName("sfDpl1"));
            add(new SfDataPlaneLocatorName("sfDpl2"));
            add(new SfDataPlaneLocatorName("sfDpl3"));
            add(new SfDataPlaneLocatorName("sfDpl4"));
            add(new SfDataPlaneLocatorName("sfDpl5"));
            add(new SfDataPlaneLocatorName("sfDpl6"));
            add(new SfDataPlaneLocatorName("sfDpl7"));
            add(new SfDataPlaneLocatorName("sfDpl8"));
        }
    };

    @SuppressWarnings("serial")
    private final List<String> groupNames = new ArrayList<String>() {

        {
            add("group1");
            add("group2");
            add("group3");
            add("group4");
        }
    };

    @SuppressWarnings("serial")
    private final List<SfcName> chainNames = new ArrayList<SfcName>() {

        {
            add(new SfcName("chain1"));
            add(new SfcName("chain2"));
            add(new SfcName("chain3"));
        }
    };

    @SuppressWarnings("serial")
    private final List<SfpName> pathNames = new ArrayList<SfpName>() {

        {
            add(new SfpName("path1"));
            add(new SfpName("path2"));
            add(new SfpName("path3"));
        }
    };

    @SuppressWarnings("serial")
    private static final List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {

        {
            add("196.168.55.1");
            add("196.168.55.2");
            add("196.168.55.3");
            add("196.168.55.4");
            add("196.168.55.5");
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
    List<SftTypeName> sfTypes = new ArrayList<SftTypeName>() {

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
            add("napt44");
            add("http-header-enrichment");
            add("qos");
        }
    };

    private static final SfcName SFC_NAME = new SfcName("unittest-chain-1");
    private static final SfpName SFP_NAME = new SfpName("unittest-sfp-1");
    private static final RspName RSP_NAME = new RspName("unittest-rsp-1");

    private final List<ServiceFunction> sfList = new ArrayList<>();

    SfcProviderRpc sfcProviderRpc;
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);

    @Before
    public void setUp() {
        setOdlSfc();
        sfcProviderRpc = new SfcProviderRpc();
    }

    // auxiliary method
    private void init() {
        // Create Service Functions
        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames2.size()];
        final IpAddress[] locatorIpAddress = new IpAddress[sfNames2.size()];
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[sfNames2.size()];
        ServiceFunctionKey[] key = new ServiceFunctionKey[sfNames2.size()];
        for (int i = 0; i < sfNames2.size(); i++) {
            ipMgmtAddress[i] = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(0)));
            locatorIpAddress[i] = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(0)));
            PortNumber portNumber = new PortNumber(PORT.get(i));
            key[i] = new ServiceFunctionKey(sfNames2.get(i));

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(i)))
                .setLocatorType(ipBuilder.build())
                .setServiceFunctionForwarder(SFF_NAMES.get(i));
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(sfNames2.get(i))
                .setKey(key[i])
                .setType(sfTypes.get(i))
                .setIpMgmtAddress(ipMgmtAddress[i])
                .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfsIID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        // Create ServiceFunctionTypeEntry for all ServiceFunctions
        for (ServiceFunction serviceFunction : sfList) {
            boolean ret = SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction);
            LOG.debug("call createServiceFunctionTypeEntryExecutor for {}", serviceFunction.getName());
            assertTrue("Must be true", ret);
        }

        // Create Service Function Forwarders
        for (int i = 0; i < SFF_NAMES.size(); i++) {
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
            locatorBuilder.setName(new SffDataPlaneLocatorName(SFF_LOCATOR_IP.get(i)))
                .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName(SFF_LOCATOR_IP.get(i))))
                .setDataPlaneLocator(sffLocatorBuilder.build());
            locatorList.add(locatorBuilder.build());
            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
            sffBuilder.setName(SFF_NAMES.get(i))
                .setKey(new ServiceFunctionForwarderKey(SFF_NAMES.get(i)))
                .setSffDataPlaneLocator(locatorList)
                .setServiceFunctionDictionary(sfDictionaryList)
                .setConnectedSffDictionary(sffDictionaryList)
                .setServiceNode(null);
            ServiceFunctionForwarder sff = sffBuilder.build();
            InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, new ServiceFunctionForwarderKey(SFF_NAMES.get(i)))
                .build();
            SfcDataStoreAPI.writePutTransactionAPI(sffEntryIID, sff, LogicalDatastoreType.CONFIGURATION);
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

        InstanceIdentifier<ServiceFunctionChain> sfcIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
            .child(ServiceFunctionChain.class, sfcKey)
            .build();
        SfcDataStoreAPI.writePutTransactionAPI(sfcIID, sfcBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        // Check if Service Function Chain was created
        Object result = SfcDataStoreAPI.readTransactionAPI(sfcIID, LogicalDatastoreType.CONFIGURATION);
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

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
    public void createRenderedPathTest() throws Exception {
        init();

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue())
            .setParentServiceFunctionPath(SFP_NAME.getValue())
            .setSymmetric(true);
        CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath, createRenderedPathInput);

        // check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(SFF_NAMES.get(0));
        assertNotNull("Must be not null", sffServicePathList);
        // XXX TODO ... same problem... why equivalence between these two things? Its hidden in RPC.
        assertEquals(sffServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        // check if SF oper contains RSP
        // XXX TODO why refer to string directly when it appears in variables initialised at top ?
        List<SfServicePath> sfServicePathList =
                SfcProviderServiceFunctionAPI.readServiceFunctionState(new SfName("unittest-fw-1"));
        assertEquals(sfServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        // check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList =
                SfcProviderServicePathAPI.readServicePathState(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(SFP_NAME.getValue());
        ServiceFunctionClassifier serviceFunctionClassifier = serviceFunctionClassifierBuilder.build();

        InstanceIdentifier<ServiceFunctionClassifier> sclIID;
        ServiceFunctionClassifierKey serviceFunctionKey = new ServiceFunctionClassifierKey(SFP_NAME.getValue());
        sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
            .child(ServiceFunctionClassifier.class, serviceFunctionKey)
            .build();

        SfcDataStoreAPI.writePutTransactionAPI(sclIID, serviceFunctionClassifier, LogicalDatastoreType.CONFIGURATION);

        Future<RpcResult<CreateRenderedPathOutput>> result = sfcProviderRpc.createRenderedPath(createRenderedPathInput);
        assertNotNull(result);
        assertNotNull(result.get());
        assertNotNull(result.get().getResult());
        assertTrue(result.get().getErrors().isEmpty());
        assertTrue(result.get().isSuccessful());

        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(RSP_NAME);

        assertEquals(renderedServicePath.getName(), RSP_NAME);
        assertEquals(5, renderedServicePath.getRenderedServicePathHop().size());

        assertHop(0, new SffName("SFF1"), renderedServicePath.getRenderedServicePathHop().get(0));
        assertHop(1, new SffName("SFF2"), renderedServicePath.getRenderedServicePathHop().get(1));

        ServiceFunctionClassifier serviceFunctionClassifierFromDataStore =
                SfcDataStoreAPI.readTransactionAPI(sclIID, LogicalDatastoreType.CONFIGURATION);
        assertEquals(SFP_NAME.getValue(), serviceFunctionClassifierFromDataStore.getName());
    }

    private void assertHop(long hopNumber, SffName name, RenderedServicePathHop hop) {
        assertEquals(hopNumber, (long) hop.getHopNumber());
        assertEquals(name, hop.getServiceFunctionForwarder());
    }

    private void writeRSP() throws Exception {
        init();

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue())
            .setParentServiceFunctionPath(SFP_NAME.getValue())
            .setSymmetric(true);
        CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath, createRenderedPathInput);

        // check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(SFF_NAMES.get(1));
        assertNotNull("Must be not null", sffServicePathList);
        assertEquals(sffServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        // check if SF oper contains RSP
        List<SfServicePath> sfServicePathList =
                SfcProviderServiceFunctionAPI.readServiceFunctionState(new SfName("unittest-fw-1"));
        assertEquals(sfServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        // check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList =
                SfcProviderServicePathAPI.readServicePathState(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName(), RSP_NAME);

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(SFP_NAME.getValue());
        ServiceFunctionClassifier serviceFunctionClassifier = serviceFunctionClassifierBuilder.build();

        InstanceIdentifier<ServiceFunctionClassifier> sclIID;
        ServiceFunctionClassifierKey serviceFunctionKey = new ServiceFunctionClassifierKey(SFP_NAME.getValue());
        sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
            .child(ServiceFunctionClassifier.class, serviceFunctionKey)
            .build();

        SfcDataStoreAPI.writePutTransactionAPI(sclIID, serviceFunctionClassifier, LogicalDatastoreType.CONFIGURATION);

        sfcProviderRpc.createRenderedPath(createRenderedPathInput);
    }

    @Test
    public void readRenderedServicePathFirstHopTest() throws Exception {
        writeRSP();
        ReadRenderedServicePathFirstHopInputBuilder readRenderedServicePathFirstHopInputBuilder =
                new ReadRenderedServicePathFirstHopInputBuilder();
        readRenderedServicePathFirstHopInputBuilder.setName(RSP_NAME.getValue());
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInput =
                readRenderedServicePathFirstHopInputBuilder.build();

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result =
                sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInput);
        assertNotNull(result);
        assertNotNull(result.get());
        assertNotNull(result.get().getResult());
        assertTrue(result.get().getErrors().isEmpty());
        assertTrue(result.get().isSuccessful());
    }

    @Test
    public void readRenderedServicePathFirstHopElseTest() throws Exception {
        ReadRenderedServicePathFirstHopInputBuilder readRenderedServicePathFirstHopInputBuilder =
                new ReadRenderedServicePathFirstHopInputBuilder();
        readRenderedServicePathFirstHopInputBuilder.setName(RSP_NAME.getValue());
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInput =
                readRenderedServicePathFirstHopInputBuilder.build();

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result =
                sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInput);
        assertNotNull(result);
        assertNotNull(result.get());
        assertFalse(result.get().getErrors().isEmpty());
    }

    @Test
    public void putServiceFunctionChainsTest() {
        PutServiceFunctionChainsInputBuilder putServiceFunctionChainsInputBuilder =
                new PutServiceFunctionChainsInputBuilder();
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        serviceFunctionChainBuilder.setName(new SfcName("SFC1"))
            .setKey(new ServiceFunctionChainKey(new SfcName("SFC1")))
            .setSymmetric(false);
        ServiceFunctionChain serviceFunctionChain = serviceFunctionChainBuilder.build();
        List<ServiceFunctionChain> serviceFunctionChainList = new ArrayList<>();
        serviceFunctionChainList.add(serviceFunctionChain);
        putServiceFunctionChainsInputBuilder.setServiceFunctionChain(serviceFunctionChainList);
        PutServiceFunctionChainsInput putServiceFunctionChainsInput = putServiceFunctionChainsInputBuilder.build();
        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        sfcProviderRpc.putServiceFunctionChains(putServiceFunctionChainsInput);

        ServiceFunctionChainsBuilder serviceFunctionChainsBuilder = new ServiceFunctionChainsBuilder();
        serviceFunctionChainsBuilder = serviceFunctionChainsBuilder
            .setServiceFunctionChain(putServiceFunctionChainsInput.getServiceFunctionChain());
        serviceFunctionChainsBuilder.build();
        InstanceIdentifier<ServiceFunctionChains> SFC_IID =
                InstanceIdentifier.builder(ServiceFunctionChains.class).build();
        ServiceFunctionChains serviceFunctionChainsFromDataStore =
                SfcDataStoreAPI.readTransactionAPI(SFC_IID, LogicalDatastoreType.CONFIGURATION);
        assertNotNull(serviceFunctionChainsFromDataStore);
        assertEquals(new SfcName("SFC1"),
                serviceFunctionChainsFromDataStore.getServiceFunctionChain().get(0).getName());
    }

    @Test
    public void instantiateServiceFunctionChainTest() {
        InstantiateServiceFunctionChainInputBuilder instantiateServiceFunctionChainInput =
                new InstantiateServiceFunctionChainInputBuilder();
        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        assertNull(sfcProviderRpc.instantiateServiceFunctionChain(instantiateServiceFunctionChainInput.build()));
    }

    @Test
    public void getSfcProviderRpcTest() {
        assertNotNull(SfcProviderRpc.getSfcProviderRpc());
    }

    @Test
    public void readRspFirstHopBySftListTest() throws Exception {
        writeRSP();
        ReadRspFirstHopBySftListInputBuilder readRspFirstHopBySftListInputBuilder =
                new ReadRspFirstHopBySftListInputBuilder();
        List<SftTypeName> firewallList = new ArrayList<>();
        firewallList.add(new SftTypeName("firewall"));
        readRspFirstHopBySftListInputBuilder.setSftList(firewallList);
        ReadRspFirstHopBySftListInput readRspFirstHopBySftListInput = readRspFirstHopBySftListInputBuilder.build();

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        Future<RpcResult<ReadRspFirstHopBySftListOutput>> result =
                sfcProviderRpc.readRspFirstHopBySftList(readRspFirstHopBySftListInput);
        assertNotNull(result);
        assertEquals((long) 255, (long) result.get().getResult().getRenderedServicePathFirstHop().getStartingIndex());
        assertTrue(result.get().getErrors().isEmpty());
        assertTrue(result.get().isSuccessful());
    }

    @Test
    public void deleteRenderedPathTest() throws Exception {
        init();

        final RspName pathName1 = new RspName("rspName1");
        final RspName pathName2 = new RspName("rspName2");

        // create and delete one rendered service path
        assertRenderedServicePathDoesNotExist(pathName1);
        createRenderedServicePath(pathName1);
        assertRenderedServicePathExists(pathName1);
        deleteRenderedServicePath(pathName1);
        assertRenderedServicePathDoesNotExist(pathName1);

        // create and delete two rendered service paths
        assertRenderedServicePathDoesNotExist(pathName2);
        createRenderedServicePath(pathName1);
        createRenderedServicePath(pathName2);
        assertRenderedServicePathExists(pathName1);
        assertRenderedServicePathExists(pathName2);
        // delete the 1st path
        deleteRenderedServicePath(pathName1);
        assertRenderedServicePathDoesNotExist(pathName1);
        assertRenderedServicePathExists(pathName2);
        // delete the 2nd path
        deleteRenderedServicePath(pathName2);
        assertRenderedServicePathDoesNotExist(pathName1);
        assertRenderedServicePathDoesNotExist(pathName2);

        // delete non-existing path
        deleteRenderedServicePath(pathName1);
        assertRenderedServicePathDoesNotExist(pathName1);
    }

    private void createRenderedServicePath(RspName pathName) throws Exception {
        CreateRenderedPathInputBuilder inputBuilder = new CreateRenderedPathInputBuilder();
        inputBuilder.setName(pathName.getValue()).setParentServiceFunctionPath(SFP_NAME.getValue());
        CreateRenderedPathInput input = inputBuilder.build();
        Future<RpcResult<CreateRenderedPathOutput>> result = sfcProviderRpc.createRenderedPath(input);
        assertTrue("Failed to create rendered service path.",
                result != null && result.get() != null && result.get().isSuccessful());
    }

    private void deleteRenderedServicePath(RspName pathName) throws Exception {
        DeleteRenderedPathInputBuilder inputBuilder = new DeleteRenderedPathInputBuilder();
        inputBuilder.setName(pathName.getValue());
        DeleteRenderedPathInput input = inputBuilder.build();
        Future<RpcResult<DeleteRenderedPathOutput>> result = sfcProviderRpc.deleteRenderedPath(input);
        assertTrue("Failed to delete rendered service path.",
                result != null && result.get() != null && result.get().isSuccessful());
    }

    private static void assertRenderedServicePathExists(RspName pathName) {
        RenderedServicePath path = SfcProviderRenderedPathAPI.readRenderedServicePath(pathName);
        assertNotNull("Rendered service path not found.", path);
        assertEquals("Unexpected rendered service path name.", pathName, path.getName());
    }

    private static void assertRenderedServicePathDoesNotExist(RspName pathName) {
        RenderedServicePath path = SfcProviderRenderedPathAPI.readRenderedServicePath(pathName);
        assertNull("Unexpected rendered service path found.", path);
    }

    @Test
    public void putAndReadServiceFunctionTest() throws Exception {
        PutServiceFunctionInput putSfInput1 =
                createPutServiceFunctionInput(new SfName("sfName1"), new SftTypeName("firewall"), "192.168.50.80",
                        "192.168.50.85", 6644, new SfDataPlaneLocatorName("dpLocatorKey1"));

        createPutServiceFunctionInput(new SfName("sfName2"), new SftTypeName("dpi"),
                "192.168.50.90", "192.168.50.95", 6655, new SfDataPlaneLocatorName("dpLocatorKey2"));

        assertServiceFunctionDoesNotExist(putSfInput1.getName());
        putServiceFunction(putSfInput1);
        readAndAssertServiceFunction(putSfInput1);

        // assertServiceFunctionDoesNotExist(putSfInput2.getName());
        // putServiceFunction(putSfInput2);
        // readAndAssertServiceFunction(putSfInput1);
        // readAndAssertServiceFunction(putSfInput2);
    }

    private static PutServiceFunctionInput createPutServiceFunctionInput(SfName sfName, SftTypeName sfType,
            String ipMgmtAddress, String dpLocatorIpAddress, int dpLocatorPort, SfDataPlaneLocatorName dpLocatorKey) {

        // prepare ip builder for data plane locator
        IpAddress ipAddress = new IpAddress(dpLocatorIpAddress.toCharArray());
        PortNumber portNumber = new PortNumber(dpLocatorPort);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder = ipBuilder.setIp(ipAddress).setPort(portNumber);

        // prepare data plane locators
        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        SfDataPlaneLocatorKey sfDataPlaneLocatorKey = new SfDataPlaneLocatorKey(dpLocatorKey);
        sfDataPlaneLocatorBuilder.setKey(sfDataPlaneLocatorKey);
        sfDataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        sfDataPlaneLocatorBuilder.setTransport(SlTransportType.class);
        List<SfDataPlaneLocator> sfDataPlaneLocators = new ArrayList<>();
        sfDataPlaneLocators.add(sfDataPlaneLocatorBuilder.build());

        // create PutServiceFunctionInput
        PutServiceFunctionInputBuilder putServiceFunctionInputBuilder = new PutServiceFunctionInputBuilder();
        putServiceFunctionInputBuilder = putServiceFunctionInputBuilder.setName(sfName)
            .setType(sfType)
            .setIpMgmtAddress(new IpAddress(ipMgmtAddress.toCharArray()))
            .setSfDataPlaneLocator(sfDataPlaneLocators);
        return putServiceFunctionInputBuilder.build();
    }

    private void putServiceFunction(PutServiceFunctionInput putSfInput) throws Exception {
        Future<RpcResult<Void>> result = sfcProviderRpc.putServiceFunction(putSfInput);
        assertNotNull("Failed to put service function.",
                result != null && result.get() != null && result.get().isSuccessful());
        assert result != null;
        assertNull(result.get().getResult());
        assertNotNull(result.get().getErrors());
        assertTrue(result.get().getErrors().isEmpty());
    }

    private void readAndAssertServiceFunction(PutServiceFunctionInput putSfInput) throws Exception {
        ReadServiceFunctionInputBuilder readInputBuilder = new ReadServiceFunctionInputBuilder();
        readInputBuilder.setName(putSfInput.getName().getValue());
        ReadServiceFunctionInput readSfInput = readInputBuilder.build();
        Future<RpcResult<ReadServiceFunctionOutput>> readSfResult = sfcProviderRpc.readServiceFunction(readSfInput);
        assertNotNull("Failed to read service function",
                readSfResult != null && readSfResult.get() != null && readSfResult.get().isSuccessful());
        assert readSfResult != null;
        assertNotNull(readSfResult.get().getErrors());
        assertTrue(readSfResult.get().getErrors().isEmpty());

        ReadServiceFunctionOutput readSfOutput = readSfResult.get().getResult();
        assertNotNull("Service function not found.", readSfOutput);
        assertEquals("Unexpected SF name.", putSfInput.getName(), readSfOutput.getName());
        assertEquals("Unexpected SF type.", putSfInput.getType(), readSfOutput.getType());
        assertEquals("Unexpected IP mgmt address.", putSfInput.getIpMgmtAddress().getIpv4Address().getValue(),
                readSfOutput.getIpMgmtAddress().getIpv4Address().getValue());
        assertEquals("Bad number of data plane locators.", putSfInput.getSfDataPlaneLocator().size(),
                readSfOutput.getSfDataPlaneLocator().size());
        assertEquals("Unexpected data plane locator item value(s).", putSfInput.getSfDataPlaneLocator().get(0),
                readSfOutput.getSfDataPlaneLocator().get(0));
    }

    private static void assertServiceFunctionDoesNotExist(SfName sfName) {
        InstanceIdentifier<ServiceFunction> sfId = createServiceFunctionId(sfName);
        ServiceFunction sf = SfcDataStoreAPI.readTransactionAPI(sfId, LogicalDatastoreType.CONFIGURATION);
        assertNull("Service function must not exist.", sf);
    }

    private static InstanceIdentifier<ServiceFunction> createServiceFunctionId(SfName sfName) {
        ServiceFunctionKey sfKey = new ServiceFunctionKey(sfName);
        return InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class, sfKey).build();
    }

    /*
     * This test creates 3 rendered service paths, one symmetric, one asymmetric and the last one
     * also symmetric with
     * reverse classifier only
     */
    @Test
    public void testMultipleRsp() {
        List<SfName> firewallSfs = new ArrayList<>(); // list of all firewalls
        List<SfName> dpiSfs = new ArrayList<>(); // list of all dpi-s
        List<SfName> qosSfs = new ArrayList<>(); // list of all qos-s
        List<SfName> napt44Sfs = new ArrayList<>(); // list of all napt44
        List<SfName> chainSf1 = new ArrayList<>(); // list of Sf-s in chain 1
        List<SfName> chainSf2 = new ArrayList<>(); // list of Sf-s in chain 2
        List<SfName> chainSf3 = new ArrayList<>(); // list of Sf-s in chain 3
        Future futureTask1, futureTask2, futureTask3;
        RpcResult<CreateRenderedPathOutput> rpcResult1 = null, rpcResult2 = null, rpcResult3 = null;
        RenderedServicePath createdRsp1 = null, createdRsp2 = null, createdRsp3 = null;

        // 4x forwarder
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames.get(0), sffDplNames.get(0)));
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames.get(1), sffDplNames.get(1)));
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames.get(2), sffDplNames.get(2)));
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames.get(3), sffDplNames.get(3)));

        // 8 functions + lists
        // SFF1
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(0), new SftTypeName("firewall"), sfDplNames.get(0), sffNames.get(0)));
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(1), new SftTypeName("dpi"), sfDplNames.get(1), sffNames.get(0)));
        // SFF2
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(2), new SftTypeName("qos"), sfDplNames.get(2), sffNames.get(1)));
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(3), new SftTypeName("napt44"), sfDplNames.get(3), sffNames.get(1)));
        // SFF3
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(4), new SftTypeName("firewall"), sfDplNames.get(4), sffNames.get(2)));
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(5), new SftTypeName("dpi"), sfDplNames.get(5), sffNames.get(2)));
        // SFF4
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(6), new SftTypeName("qos"), sfDplNames.get(6), sffNames.get(3)));
        assertTrue("Must be true",
                createServiceFunction(sfNames.get(7), new SftTypeName("firewall"), sfDplNames.get(7), sffNames.get(3)));

        firewallSfs.add(sfNames.get(0));
        firewallSfs.add(sfNames.get(4));
        firewallSfs.add(sfNames.get(7));
        dpiSfs.add(sfNames.get(1));
        dpiSfs.add(sfNames.get(5));
        qosSfs.add(sfNames.get(2));
        qosSfs.add(sfNames.get(6));
        napt44Sfs.add(sfNames.get(3));

        // types
        assertTrue("Must be true", createServiceFunctionType(new SftTypeName("firewall"), firewallSfs));
        assertTrue("Must be true", createServiceFunctionType(new SftTypeName("dpi"), dpiSfs));
        assertTrue("Must be true", createServiceFunctionType(new SftTypeName("qos"), qosSfs));
        assertTrue("Must be true", createServiceFunctionType(new SftTypeName("napt44"), napt44Sfs));

        // groups
        assertTrue("Must be true", createServiceFunctionGroup(groupNames.get(0), firewallSfs, new SftTypeName("firewall")));
        assertTrue("Must be true", createServiceFunctionGroup(groupNames.get(1), dpiSfs, new SftTypeName("dpi")));
        assertTrue("Must be true", createServiceFunctionGroup(groupNames.get(2), qosSfs, new SftTypeName("qos")));
        assertTrue("Must be true", createServiceFunctionGroup(groupNames.get(3), qosSfs, new SftTypeName("napt44")));

        // chain + path 1 (SFF1 & SFF2)
        chainSf1.add(sfNames.get(0));
        chainSf1.add(sfNames.get(1));
        chainSf1.add(sfNames.get(2));
        chainSf1.add(sfNames.get(3));
        assertTrue("Must be true", createServiceFunctionChain(chainNames.get(0), chainSf1));
        assertTrue("Must be true", createServiceFunctionPath(pathNames.get(0), chainNames.get(0), false));

        // chain + path 2 (SFF3 & SFF4)
        chainSf2.add(sfNames.get(4));
        chainSf2.add(sfNames.get(5));
        chainSf2.add(sfNames.get(6));
        chainSf2.add(sfNames.get(7));
        assertTrue("Must be true", createServiceFunctionChain(chainNames.get(1), chainSf2));
        assertTrue("Must be true", createServiceFunctionPath(pathNames.get(1), chainNames.get(1), false));

        // chain + path 3 (all SF & SFF)
        for (SfName sfName : sfNames) {
            chainSf3.add(sfName);
        }
        assertTrue("Must be true", createServiceFunctionChain(chainNames.get(2), chainSf3));
        assertTrue("Must be true", createServiceFunctionPath(pathNames.get(2), chainNames.get(2), true));

        // create rendered service paths
        futureTask1 = sfcProviderRpc.createRenderedPath(createRenderedPathInput(pathNames.get(0).getValue(), true));
        futureTask2 = sfcProviderRpc.createRenderedPath(createRenderedPathInput(pathNames.get(1).getValue(), true));
        futureTask3 = sfcProviderRpc.createRenderedPath(createRenderedPathInput(pathNames.get(2).getValue(), true));

        // test
        try {
            // noinspection unchecked
            rpcResult1 = (RpcResult<CreateRenderedPathOutput>) futureTask1.get();
            assertTrue("Must be true", rpcResult1.isSuccessful());
            // noinspection unchecked
            rpcResult2 = (RpcResult<CreateRenderedPathOutput>) futureTask2.get();
            assertTrue("Must be true", rpcResult2.isSuccessful());
            // noinspection unchecked
            rpcResult3 = (RpcResult<CreateRenderedPathOutput>) futureTask3.get();
            assertTrue("Must be true", rpcResult3.isSuccessful());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        }

        // get created rendered service paths
        if (rpcResult1 != null)
            createdRsp1 =
                    SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName(rpcResult1.getResult().getName()));
        if (rpcResult2 != null)
            createdRsp2 =
                    SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName(rpcResult2.getResult().getName()));
        if (rpcResult3 != null)
            createdRsp3 =
                    SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName(rpcResult3.getResult().getName()));

        assertNotNull("Must not be null", createdRsp1);
        assertNotNull("Must not be null", createdRsp2);
        assertNotNull("Must not be null", createdRsp3);

        // check hops
        assertEquals("Must be equal", createdRsp1.getRenderedServicePathHop().size(), 4);
        assertEquals("Must be equal", createdRsp2.getRenderedServicePathHop().size(), 4);
        assertEquals("Must be equal", createdRsp3.getRenderedServicePathHop().size(), 8);

        // check symmetry and classifiers
        // path 1
        ServiceFunctionPath serviceFunctionPath1 = SfcProviderServicePathAPI.readServiceFunctionPath(pathNames.get(0));
        assertNotNull("Must not be null", serviceFunctionPath1);
        assertFalse("Must be false", serviceFunctionPath1.isSymmetric()); // path is not symmetric

        // path 2
        ServiceFunctionPath serviceFunctionPath2 = SfcProviderServicePathAPI.readServiceFunctionPath(pathNames.get(1));
        assertNotNull("Must not be null", serviceFunctionPath2);
        assertFalse("Must be false", serviceFunctionPath2.isSymmetric()); // path is not symmetric

        // path 3
        ServiceFunctionPath serviceFunctionPath3 = SfcProviderServicePathAPI.readServiceFunctionPath(pathNames.get(2));
        assertNotNull("Must not be null", serviceFunctionPath3);
        assertTrue("Must be true", serviceFunctionPath3.isSymmetric()); // path not symmetric

    }

    // auxiliary methods below

    /*
     *
     * create service function type
     * put type, and list of all service functions with that type
     */
    private boolean createServiceFunctionType(SftTypeName serviceType, List<SfName> sfNames) {
        List<SftServiceFunctionName> sftServiceFunctionNames = new ArrayList<>();
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder;

        for (SfName sfName : sfNames) {
            sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
            sftServiceFunctionNameBuilder.setName(sfName)
                .setKey(new SftServiceFunctionNameKey(sfName));
            sftServiceFunctionNames.add(sftServiceFunctionNameBuilder.build());
        }

        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder.setKey(new ServiceFunctionTypeKey(serviceType))
            .setSftServiceFunctionName(sftServiceFunctionNames);
        return SfcProviderServiceTypeAPI.putServiceFunctionType(serviceFunctionTypeBuilder.build());
    }

    /*
     * build service function forwarder with data plane locator
     * method needs forwarder name & data plane locator name
     */
    private boolean createServiceFunctionForwarder(SffName forwarderName, SffDataPlaneLocatorName locatorName) {
        List<SffDataPlaneLocator> sffDataPlaneLocator = new ArrayList<>();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder;

        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder.setName(locatorName).setKey(new SffDataPlaneLocatorKey(locatorName));
        sffDataPlaneLocator.add(sffDataPlaneLocatorBuilder.build());

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(forwarderName)
            .setKey(new ServiceFunctionForwarderKey(forwarderName))
            .setSffDataPlaneLocator(sffDataPlaneLocator);
        return SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(serviceFunctionForwarderBuilder.build());
    }

    /*
     * build service function with data plane locator
     * specify function name, type, locator name and name of appropriate forwarder
     */
    private boolean createServiceFunction(SfName functionName, SftTypeName functionType, SfDataPlaneLocatorName locatorName,
            SffName forwarderName) {
        List<SfDataPlaneLocator> sfDataPlaneLocator = new ArrayList<>();
        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();

        sfDataPlaneLocatorBuilder.setName(locatorName)
            .setKey(new SfDataPlaneLocatorKey(locatorName))
            .setServiceFunctionForwarder(forwarderName);
        sfDataPlaneLocator.add(sfDataPlaneLocatorBuilder.build());

        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(functionName)
            .setKey(new ServiceFunctionKey(functionName))
            .setSfDataPlaneLocator(sfDataPlaneLocator)
            .setType(functionType);

        return SfcProviderServiceFunctionAPI.putServiceFunction(serviceFunctionBuilder.build());
    }

    /*
     * create service function group containing service functions of specific type
     * specify group name, list of service functions and type (should match with sf type) of group
     */
    private boolean createServiceFunctionGroup(String groupName, List<SfName> sfNames, SftTypeName groupType) {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();

        /*
         * create service function list, using type SfcServiceFunction 150214
         * this is necessary for service function group
         * there is no info about service function type, it is specified in service function group
         */
        List<org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction> sfcServiceFunctionList =
                new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunctionBuilder sfcServiceFunctionBuilder =
                new org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunctionBuilder();

        for (SfName sfName : sfNames) {
            sfcServiceFunctionBuilder.setName(sfName)
                .setKey(new org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunctionKey(
                        sfName));
            sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        }

        serviceFunctionGroupBuilder.setName(groupName)
            .setKey(new ServiceFunctionGroupKey(groupName))
            .setSfcServiceFunction(sfcServiceFunctionList)
            .setType(groupType);

        return SfcProviderServiceFunctionGroupAPI.putServiceFunctionGroup(serviceFunctionGroupBuilder.build());
    }

    /*
     * create service function chain
     * specify chain name, list of service functions (all functions should be written into data
     * store and contain info
     * about type) and whether the chain si symmetric or not
     */
    private boolean createServiceFunctionChain(SfcName chainName, List<SfName> sfNames) {
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();

        /*
         * create service function list, using type SfcServiceFunction rev14071
         * this is necessary for service function chain
         * there is also info about service function type, because we cannot put this info directly
         * to chain
         */
        List<SfcServiceFunction> sfcSfgServiceFunctionList = new ArrayList<>();

        SfcServiceFunctionBuilder sfcSfgServiceFunctionBuilder = new SfcServiceFunctionBuilder();

        for (SfName sfName : sfNames) {
            sfcSfgServiceFunctionBuilder.setName(sfName.getValue())
                .setKey(new SfcServiceFunctionKey(sfName.getValue()))
                .setType(SfcProviderServiceFunctionAPI.readServiceFunction(sfName).getType());

            sfcSfgServiceFunctionList.add(sfcSfgServiceFunctionBuilder.build());
        }

        serviceFunctionChainBuilder.setName(chainName)
            .setKey(new ServiceFunctionChainKey(chainName))
            .setSfcServiceFunction(sfcSfgServiceFunctionList);

        return SfcProviderServiceChainAPI.putServiceFunctionChain(serviceFunctionChainBuilder.build());
    }

    /*
     * create service function path
     * specify path name, name of used chain and whether the path is symmetric or not
     */
    private boolean createServiceFunctionPath(SfpName pathName, SfcName chainName, boolean symmetric) {

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(pathName)
            .setKey(new ServiceFunctionPathKey(pathName))
            .setServiceChainName(chainName)
            .setSymmetric(symmetric);
        return SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPathBuilder.build());
    }

    /*
     * input parameter, needs path name, classifiers and if path is symmetric or not
     */

    private CreateRenderedPathInput createRenderedPathInput(String pathName, boolean symmetric) {

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setParentServiceFunctionPath(pathName).setSymmetric(symmetric);
        return createRenderedPathInputBuilder.build();
    }
}

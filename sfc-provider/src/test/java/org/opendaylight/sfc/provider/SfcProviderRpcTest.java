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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.ServiceFunctionClassifierState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.ServiceFunctionClassifierStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorKey;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.HttpHeaderEnrichment;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Qos;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderRpcTest extends AbstractDataStoreManager {

    private final String[] sffNames = {"sff1", "sff2", "sff3", "sff4"};
    private final String[] sffDplNames = {"sffDpl1", "sffDpl2", "sffDpl3", "sffDpl4"};
    private final String[] sfNames = {"sf1", "sf2", "sf3", "sf4", "sf5", "sf6", "sf7", "sf8"};
    private final String[] sfDplNames = {"sfDpl1", "sfDpl2", "sfDpl3", "sfDpl4", "sfDpl5", "sfDpl6", "sfDpl7", "sfDpl8"};
    private final String[] groupNames = {"group1", "group2", "group3", "group4"};
    private final String[] chainNames = {"chain1", "chain2", "chain3"};
    private final String[] pathNames = {"path1", "path2", "path3"};

    private static final String[] LOCATOR_IP_ADDRESS =
            {"196.168.55.1", "196.168.55.2", "196.168.55.3",
                    "196.168.55.4", "196.168.55.5"};
    private static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101", "196.168.55.102", "196.168.55.103",
                    "196.168.55.104", "196.168.55.105"};
    private static final int[] PORT = {1111, 2222, 3333, 4444, 5555};
    private static final Class[] sfTypes = {Firewall.class, Dpi.class, Napt44.class, HttpHeaderEnrichment.class, Qos.class};
    private static final String[] SF_ABSTRACT_NAMES = {"firewall", "dpi", "napt", "http-header-enrichment", "qos"};
    private static final String SFC_NAME = "unittest-chain-1";
    private static final String SFP_NAME = "unittest-sfp-1";
    private static final String RSP_NAME = "unittest-rsp-1";
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPITest.class);
    private static final String[] sfNames2 = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1", "unittest-http-header-enrichment-1", "unittest-qos-1"};
    private final String[] SFF_NAMES = {"SFF1", "SFF2", "SFF3", "SFF4", "SFF5"};
    private final String[][] TO_SFF_NAMES =
            {{"SFF2", "SFF5"}, {"SFF3", "SFF1"}, {"SFF4", "SFF2"}, {"SFF5", "SFF3"}, {"SFF1", "SFF4"}};
    private final String[] SFF_LOCATOR_IP =
            {"196.168.66.101", "196.168.66.102", "196.168.66.103", "196.168.66.104", "196.168.66.105"};
    private final List<ServiceFunction> sfList = new ArrayList<>();
    SfcProviderRpc sfcProviderRpc;

    @Before
    public void setUp() {
        setOdlSfc();
        sfcProviderRpc = new SfcProviderRpc();
    }

    //auxiliary method
    private void init() {
        // Create Service Functions
        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames2.length];
        final IpAddress[] locatorIpAddress = new IpAddress[sfNames2.length];
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[sfNames2.length];
        ServiceFunctionKey[] key = new ServiceFunctionKey[sfNames2.length];
        for (int i = 0; i < sfNames2.length; i++) {
            ipMgmtAddress[i] = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
            locatorIpAddress[i] = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
            PortNumber portNumber = new PortNumber(PORT[i]);
            key[i] = new ServiceFunctionKey(sfNames2[i]);

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(LOCATOR_IP_ADDRESS[i]).setLocatorType(ipBuilder.build()).setServiceFunctionForwarder(SFF_NAMES[i]);
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(sfNames2[i]).setKey(key[i])
                    .setType(sfTypes[i])
                    .setIpMgmtAddress(ipMgmtAddress[i])
                    .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        InstanceIdentifier<ServiceFunctions> sfsIID =
                InstanceIdentifier.builder(ServiceFunctions.class).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfsIID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        // Create ServiceFunctionTypeEntry for all ServiceFunctions
        for (ServiceFunction serviceFunction : sfList) {
            boolean ret = SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction);
            LOG.debug("call createServiceFunctionTypeEntryExecutor for {}", serviceFunction.getName());
            assertTrue("Must be true", ret);
        }

        // Create Service Function Forwarders
        for (int i = 0; i < SFF_NAMES.length; i++) {
            //ServiceFunctionForwarders connected to SFF_NAMES[i]
            List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
                ConnectedSffDictionary sffDictEntry = sffDictionaryEntryBuilder.setName(TO_SFF_NAMES[i][j]).build();
                sffDictionaryList.add(sffDictEntry);
            }

            //ServiceFunctions attached to SFF_NAMES[i]
            List<ServiceFunctionDictionary> sfDictionaryList = new ArrayList<>();
            ServiceFunction serviceFunction = sfList.get(i);
            SfDataPlaneLocator sfDPLocator = serviceFunction.getSfDataPlaneLocator().get(0);
            SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder(sfDPLocator);
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

            List<SffDataPlaneLocator> locatorList = new ArrayList<>();
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[i])))
                    .setPort(new PortNumber(PORT[i]));
            DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
            sffLocatorBuilder.setLocatorType(ipBuilder.build())
                    .setTransport(VxlanGpe.class);
            SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
            locatorBuilder.setName(SFF_LOCATOR_IP[i])
                    .setKey(new SffDataPlaneLocatorKey(SFF_LOCATOR_IP[i]))
                    .setDataPlaneLocator(sffLocatorBuilder.build());
            locatorList.add(locatorBuilder.build());
            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
            sffBuilder.setName(SFF_NAMES[i])
                    .setKey(new ServiceFunctionForwarderKey(SFF_NAMES[i]))
                    .setSffDataPlaneLocator(locatorList)
                    .setServiceFunctionDictionary(sfDictionaryList)
                    .setConnectedSffDictionary(sffDictionaryList)
                    .setServiceNode(null);
            ServiceFunctionForwarder sff = sffBuilder.build();
            InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).
                    child(ServiceFunctionForwarder.class, new ServiceFunctionForwarderKey(SFF_NAMES[i])).build();
            SfcDataStoreAPI.writePutTransactionAPI(sffEntryIID, sff, LogicalDatastoreType.CONFIGURATION);
        }

        //Create Service Function Chain
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(SFC_NAME);
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < SF_ABSTRACT_NAMES.length; i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                    sfcSfBuilder.setName(SF_ABSTRACT_NAMES[i])
                            .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES[i]))
                            .setType(sfTypes[i])
                            .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(SFC_NAME).setKey(sfcKey)
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(true);

        InstanceIdentifier<ServiceFunctionChain> sfcIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, sfcKey).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfcIID, sfcBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        //Check if Service Function Chain was created
        Object result = SfcDataStoreAPI.readTransactionAPI(sfcIID, LogicalDatastoreType.CONFIGURATION);
        ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(SFP_NAME)
                .setServiceChainName(SFC_NAME)
                .setSymmetric(true).setClassifier(SFP_NAME).setSymmetricClassifier(SFP_NAME);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        boolean ret = SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath);
        assertTrue("Must be true", ret);
    }

    @Test
    public void createRenderedPathTest() throws Exception{
        init();

        ServiceFunctionPath serviceFunctionPath =
                SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME).setParentServiceFunctionPath(SFP_NAME)
                .setClassifier(SFP_NAME).setSymmetricClassifier(SFP_NAME).setSymmetric(true);

        CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(
                serviceFunctionPath, createRenderedPathInput);

        //check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(SFF_NAMES[1]);
        assertNotNull("Must be not null", sffServicePathList);
        assertEquals(sffServicePathList.get(0).getName(), RSP_NAME);

        //check if SF oper contains RSP
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState("unittest-fw-1");
        assertEquals(sfServicePathList.get(0).getName(), RSP_NAME);

        //check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI.readServicePathState(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName(), RSP_NAME);

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(SFP_NAME);
        ServiceFunctionClassifier serviceFunctionClassifier = serviceFunctionClassifierBuilder.build();

        InstanceIdentifier<ServiceFunctionClassifier> sclIID;
        ServiceFunctionClassifierKey serviceFunctionKey = new ServiceFunctionClassifierKey(SFP_NAME);
        sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                .child(ServiceFunctionClassifier.class, serviceFunctionKey).build();

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

        assertHop(0, "SFF1", renderedServicePath.getRenderedServicePathHop().get(0));
        assertHop(1, "SFF2", renderedServicePath.getRenderedServicePathHop().get(1));

        ServiceFunctionClassifier serviceFunctionClassifierFromDataStore = SfcDataStoreAPI.readTransactionAPI(sclIID, LogicalDatastoreType.CONFIGURATION);
        assertEquals(SFP_NAME, serviceFunctionClassifierFromDataStore.getName());
    }

    private void assertHop(long hopNumber, String name, RenderedServicePathHop hop) {
        assertEquals(hopNumber, (long)hop.getHopNumber());
        assertEquals(name, hop.getServiceFunctionForwarder());
    }

    private void writeRSP() throws Exception{
        init();

        ServiceFunctionPath serviceFunctionPath =
                SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME).setParentServiceFunctionPath(SFP_NAME)
                .setClassifier(SFP_NAME).setSymmetricClassifier(SFP_NAME).setSymmetric(true);

        CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(
                serviceFunctionPath, createRenderedPathInput);

        //check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(SFF_NAMES[1]);
        assertNotNull("Must be not null", sffServicePathList);
        assertEquals(sffServicePathList.get(0).getName(), RSP_NAME);

        //check if SF oper contains RSP
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState("unittest-fw-1");
        assertEquals(sfServicePathList.get(0).getName(), RSP_NAME);

        //check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI.readServicePathState(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName(), RSP_NAME);

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();

        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(SFP_NAME);
        ServiceFunctionClassifier serviceFunctionClassifier = serviceFunctionClassifierBuilder.build();

        InstanceIdentifier<ServiceFunctionClassifier> sclIID;
        ServiceFunctionClassifierKey serviceFunctionKey = new ServiceFunctionClassifierKey(SFP_NAME);
        sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                .child(ServiceFunctionClassifier.class, serviceFunctionKey).build();

        SfcDataStoreAPI.writePutTransactionAPI(sclIID, serviceFunctionClassifier, LogicalDatastoreType.CONFIGURATION);

        Future<RpcResult<CreateRenderedPathOutput>> result = sfcProviderRpc.createRenderedPath(createRenderedPathInput);
    }

    @Test
    public void readRenderedServicePathFirstHopTest() throws Exception {
        writeRSP();
        ReadRenderedServicePathFirstHopInputBuilder readRenderedServicePathFirstHopInputBuilder = new ReadRenderedServicePathFirstHopInputBuilder();
        readRenderedServicePathFirstHopInputBuilder.setName(RSP_NAME);
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInput = readRenderedServicePathFirstHopInputBuilder.build();

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result = sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInput);
        assertNotNull(result);
        assertNotNull(result.get());
        assertNotNull(result.get().getResult());
        assertTrue(result.get().getErrors().isEmpty());
        assertTrue(result.get().isSuccessful());
    }

    @Test
    public void readRenderedServicePathFirstHopElseTest() throws Exception {
        ReadRenderedServicePathFirstHopInputBuilder readRenderedServicePathFirstHopInputBuilder = new ReadRenderedServicePathFirstHopInputBuilder();
        readRenderedServicePathFirstHopInputBuilder.setName(RSP_NAME);
        ReadRenderedServicePathFirstHopInput readRenderedServicePathFirstHopInput = readRenderedServicePathFirstHopInputBuilder.build();

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result = sfcProviderRpc.readRenderedServicePathFirstHop(readRenderedServicePathFirstHopInput);
        assertNotNull(result);
        assertNotNull(result.get());
        assertFalse(result.get().getErrors().isEmpty());
    }

    @Test
    public void putServiceFunctionChainsTest() {
        PutServiceFunctionChainsInputBuilder putServiceFunctionChainsInputBuilder = new PutServiceFunctionChainsInputBuilder();
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        serviceFunctionChainBuilder.setName("SFC1").setKey(new ServiceFunctionChainKey("SFC1")).setSymmetric(false);
        ServiceFunctionChain serviceFunctionChain = serviceFunctionChainBuilder.build();
        List<ServiceFunctionChain> serviceFunctionChainList = new ArrayList<>();
        serviceFunctionChainList.add(serviceFunctionChain);
        putServiceFunctionChainsInputBuilder.setServiceFunctionChain(serviceFunctionChainList);
        PutServiceFunctionChainsInput putServiceFunctionChainsInput = putServiceFunctionChainsInputBuilder.build();
        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        Future<RpcResult<Void>> result = sfcProviderRpc.putServiceFunctionChains(putServiceFunctionChainsInput);

        ServiceFunctionChainsBuilder serviceFunctionChainsBuilder = new ServiceFunctionChainsBuilder();
        serviceFunctionChainsBuilder = serviceFunctionChainsBuilder
                .setServiceFunctionChain(putServiceFunctionChainsInput.getServiceFunctionChain());
        ServiceFunctionChains sfcs = serviceFunctionChainsBuilder.build();
        InstanceIdentifier<ServiceFunctionChains> SFC_IID = InstanceIdentifier.builder(
                ServiceFunctionChains.class).build();
        ServiceFunctionChains serviceFunctionChainsFromDataStore = SfcDataStoreAPI.readTransactionAPI(SFC_IID, LogicalDatastoreType.CONFIGURATION);
        assertNotNull(serviceFunctionChainsFromDataStore);
        assertEquals("SFC1", serviceFunctionChainsFromDataStore.getServiceFunctionChain().get(0).getName());
    }

    @Test
    public void instantiateServiceFunctionChainTest() {
        InstantiateServiceFunctionChainInputBuilder instantiateServiceFunctionChainInput = new InstantiateServiceFunctionChainInputBuilder();
        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        assertNull(sfcProviderRpc.instantiateServiceFunctionChain(instantiateServiceFunctionChainInput.build()));
    }

    @Test
    public void getSfcProviderRpcTest(){
        assertNotNull(SfcProviderRpc.getSfcProviderRpc());
    }

    @Test
    public void readRspFirstHopBySftListTest() throws Exception{
        writeRSP();
        ReadRspFirstHopBySftListInputBuilder readRspFirstHopBySftListInputBuilder = new ReadRspFirstHopBySftListInputBuilder();
        List<java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity>> firewallList = new ArrayList<>();
        firewallList.add(Firewall.class);
        readRspFirstHopBySftListInputBuilder.setSftList(firewallList);
        ReadRspFirstHopBySftListInput readRspFirstHopBySftListInput = readRspFirstHopBySftListInputBuilder.build();

        SfcProviderRpc sfcProviderRpc = new SfcProviderRpc();
        Future<RpcResult<ReadRspFirstHopBySftListOutput>> result = sfcProviderRpc.readRspFirstHopBySftList(readRspFirstHopBySftListInput);
        assertNotNull(result);
        assertEquals((long) 255, (long) result.get().getResult().getRenderedServicePathFirstHop().getStartingIndex());
        assertTrue(result.get().getErrors().isEmpty());
        assertTrue(result.get().isSuccessful());
    }

    @Test
    public void deleteRenderedPathTest() throws Exception {
        init();

        final String pathName1 = "rspName1";
        final String pathName2 = "rspName2";

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

    private void createRenderedServicePath(String pathName) throws Exception {
        CreateRenderedPathInputBuilder inputBuilder = new CreateRenderedPathInputBuilder();
        inputBuilder.setName(pathName).setParentServiceFunctionPath(SFP_NAME);
        CreateRenderedPathInput input = inputBuilder.build();
        Future<RpcResult<CreateRenderedPathOutput>> result = sfcProviderRpc.createRenderedPath(input);
        assertTrue("Failed to create rendered service path.",
                result != null && result.get() != null && result.get().isSuccessful());
    }

    private void deleteRenderedServicePath(String pathName) throws Exception {
        DeleteRenderedPathInputBuilder inputBuilder = new DeleteRenderedPathInputBuilder();
        inputBuilder.setName(pathName);
        DeleteRenderedPathInput input = inputBuilder.build();
        Future<RpcResult<DeleteRenderedPathOutput>> result = sfcProviderRpc.deleteRenderedPath(input);
        assertTrue("Failed to delete rendered service path.",
                result != null && result.get() != null && result.get().isSuccessful());
    }

    private static void assertRenderedServicePathExists(String pathName) {
        RenderedServicePath path = SfcProviderRenderedPathAPI.readRenderedServicePath(pathName);
        assertNotNull("Rendered service path not found.", path);
        assertEquals("Unexpected rendered service path name.", pathName, path.getName());
    }

    private static void assertRenderedServicePathDoesNotExist(String pathName) {
        RenderedServicePath path = SfcProviderRenderedPathAPI.readRenderedServicePath(pathName);
        assertNull("Unexpected rendered service path found.", path);
    }

    @Test
    public void putAndReadServiceFunctionTest() throws Exception {
        PutServiceFunctionInput putSfInput1 = createPutServiceFunctionInput("sfName1",
                Firewall.class, "192.168.50.80", "192.168.50.85", 6644, "dpLocatorKey1");

        PutServiceFunctionInput putSfInput2 = createPutServiceFunctionInput("sfName2",
                Dpi.class, "192.168.50.90", "192.168.50.95", 6655, "dpLocatorKey2");

        assertServiceFunctionDoesNotExist(putSfInput1.getName());
        putServiceFunction(putSfInput1);
        readAndAssertServiceFunction(putSfInput1);

        assertServiceFunctionDoesNotExist(putSfInput2.getName());
        putServiceFunction(putSfInput2);
        readAndAssertServiceFunction(putSfInput1);
        readAndAssertServiceFunction(putSfInput2);
    }

    private static PutServiceFunctionInput createPutServiceFunctionInput(String sfName,
                                                                         Class<? extends ServiceFunctionTypeIdentity> sfType,
                                                                         String ipMgmtAddress,
                                                                         String dpLocatorIpAddress,
                                                                         int dpLocatorPort,
                                                                         String dpLocatorKey) {

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
        List<SfDataPlaneLocator> sfDataPlaneLocators = new ArrayList<>();
        sfDataPlaneLocators.add(sfDataPlaneLocatorBuilder.build());

        // create PutServiceFunctionInput
        PutServiceFunctionInputBuilder putServiceFunctionInputBuilder = new PutServiceFunctionInputBuilder();
        putServiceFunctionInputBuilder = putServiceFunctionInputBuilder.setName(sfName).setType(sfType)
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
        readInputBuilder.setName(putSfInput.getName());
        ReadServiceFunctionInput readSfInput = readInputBuilder.build();
        Future<RpcResult<ReadServiceFunctionOutput>> readSfResult = sfcProviderRpc
                .readServiceFunction(readSfInput);
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
        assertEquals("Bad number of data plane locators.",
                putSfInput.getSfDataPlaneLocator().size(), readSfOutput.getSfDataPlaneLocator().size());
        assertEquals("Unexpected data plane locator item value(s).",
                putSfInput.getSfDataPlaneLocator().get(0), readSfOutput.getSfDataPlaneLocator().get(0));
    }

    private static void assertServiceFunctionDoesNotExist(String sfName) {
        InstanceIdentifier<ServiceFunction> sfId = createServiceFunctionId(sfName);
        ServiceFunction sf = SfcDataStoreAPI.readTransactionAPI(sfId, LogicalDatastoreType.CONFIGURATION);
        assertNull("Service function must not exist.", sf);
    }

    private static InstanceIdentifier<ServiceFunction> createServiceFunctionId(String sfName) {
        ServiceFunctionKey sfKey = new ServiceFunctionKey(sfName);
        return InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, sfKey).build();
    }

    /*
     * This test creates 3 rendered service paths, one symmetric, one asymmetric and the last one also symmetric with
     * reverse classifier only
     */
    @Test
    public void testMultipleRsp() {
        List<String> firewallSfs = new ArrayList<>(); //list of all firewalls
        List<String> dpiSfs = new ArrayList<>(); //list of all dpi-s
        List<String> qosSfs = new ArrayList<>(); //list of all qos-s
        List<String> napt44Sfs = new ArrayList<>(); //list of all napt44
        List<String> chainSf1 = new ArrayList<>(); // list of Sf-s in chain 1
        List<String> chainSf2 = new ArrayList<>(); // list of Sf-s in chain 2
        List<String> chainSf3 = new ArrayList<>(); // list of Sf-s in chain 3
        Future futureTask1, futureTask2, futureTask3;
        RpcResult<CreateRenderedPathOutput> rpcResult1 = null, rpcResult2 = null, rpcResult3 = null;
        RenderedServicePath createdRsp1 = null, createdRsp2 = null, createdRsp3 = null;

        //classifiers
        String classifier = "classifier";
        assertTrue("Must be true", createClassifier(classifier));
        String reversedClassifier = "classifierRev";
        assertTrue("Must be true", createClassifier(reversedClassifier));

        //4x forwarder
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames[0], sffDplNames[0]));
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames[1], sffDplNames[1]));
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames[2], sffDplNames[2]));
        assertTrue("Must be true", createServiceFunctionForwarder(sffNames[3], sffDplNames[3]));

        //8 functions + lists
        //SFF1
        assertTrue("Must be true", createServiceFunction(sfNames[0], Firewall.class, sfDplNames[0], sffNames[0]));
        assertTrue("Must be true", createServiceFunction(sfNames[1], Dpi.class, sfDplNames[1], sffNames[0]));
        //SFF2
        assertTrue("Must be true", createServiceFunction(sfNames[2], Qos.class, sfDplNames[2], sffNames[1]));
        assertTrue("Must be true", createServiceFunction(sfNames[3], Napt44.class, sfDplNames[3], sffNames[1]));
        //SFF3
        assertTrue("Must be true", createServiceFunction(sfNames[4], Firewall.class, sfDplNames[4], sffNames[2]));
        assertTrue("Must be true", createServiceFunction(sfNames[5], Dpi.class, sfDplNames[5], sffNames[2]));
        //SFF4
        assertTrue("Must be true", createServiceFunction(sfNames[6], Qos.class, sfDplNames[6], sffNames[3]));
        assertTrue("Must be true", createServiceFunction(sfNames[7], Firewall.class, sfDplNames[7], sffNames[3]));

        firewallSfs.add(sfNames[0]);
        firewallSfs.add(sfNames[4]);
        firewallSfs.add(sfNames[7]);
        dpiSfs.add(sfNames[1]);
        dpiSfs.add(sfNames[5]);
        qosSfs.add(sfNames[2]);
        qosSfs.add(sfNames[6]);
        napt44Sfs.add(sfNames[3]);

        //types
        assertTrue("Must be true", createServiceFunctionType(Firewall.class, firewallSfs));
        assertTrue("Must be true", createServiceFunctionType(Dpi.class, dpiSfs));
        assertTrue("Must be true", createServiceFunctionType(Qos.class, qosSfs));
        assertTrue("Must be true", createServiceFunctionType(Napt44.class, napt44Sfs));

        //groups
        assertTrue("Must be true", createServiceFunctionGroup(groupNames[0], firewallSfs, Firewall.class));
        assertTrue("Must be true", createServiceFunctionGroup(groupNames[1], dpiSfs, Dpi.class));
        assertTrue("Must be true", createServiceFunctionGroup(groupNames[2], qosSfs, Qos.class));
        assertTrue("Must be true", createServiceFunctionGroup(groupNames[3], qosSfs, Napt44.class));

        //chain + path 1 (SFF1 & SFF2)
        chainSf1.add(sfNames[0]);
        chainSf1.add(sfNames[1]);
        chainSf1.add(sfNames[2]);
        chainSf1.add(sfNames[3]);
        assertTrue("Must be true", createServiceFunctionChain(chainNames[0], chainSf1));
        assertTrue("Must be true", createServiceFunctionPath(pathNames[0], chainNames[0], classifier, null));

        //chain + path 2 (SFF3 & SFF4)
        chainSf2.add(sfNames[4]);
        chainSf2.add(sfNames[5]);
        chainSf2.add(sfNames[6]);
        chainSf2.add(sfNames[7]);
        assertTrue("Must be true", createServiceFunctionChain(chainNames[1], chainSf2));
        assertTrue("Must be true", createServiceFunctionPath(pathNames[1], chainNames[1], reversedClassifier, null));

        //chain + path 3 (all SF & SFF)
        chainSf3.add(sfNames[0]);
        chainSf3.add(sfNames[1]);
        chainSf3.add(sfNames[2]);
        chainSf3.add(sfNames[3]);
        chainSf3.add(sfNames[4]);
        chainSf3.add(sfNames[5]);
        chainSf3.add(sfNames[6]);
        chainSf3.add(sfNames[7]);
        assertTrue("Must be true", createServiceFunctionChain(chainNames[2], chainSf3));
        assertTrue("Must be true", createServiceFunctionPath(pathNames[2], chainNames[2], classifier, reversedClassifier));

        //create rendered service paths
        futureTask1 = sfcProviderRpc.createRenderedPath(createRenderedPathInput(pathNames[0], classifier, null));
        futureTask2 = sfcProviderRpc.createRenderedPath(createRenderedPathInput(pathNames[1], reversedClassifier, null));
        futureTask3 = sfcProviderRpc.createRenderedPath(createRenderedPathInput(pathNames[2], classifier, reversedClassifier));

        //test
        try {
            //noinspection unchecked
            rpcResult1 = (RpcResult<CreateRenderedPathOutput>) futureTask1.get();
            assertTrue("Must be true", rpcResult1.isSuccessful());
            //noinspection unchecked
            rpcResult2 = (RpcResult<CreateRenderedPathOutput>) futureTask2.get();
            assertTrue("Must be true", rpcResult2.isSuccessful());
            //noinspection unchecked
            rpcResult3 = (RpcResult<CreateRenderedPathOutput>) futureTask3.get();
            assertTrue("Must be true", rpcResult3.isSuccessful());
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        }

        //get created rendered service paths
        if(rpcResult1 != null)
            createdRsp1 = SfcProviderRenderedPathAPI.readRenderedServicePath(rpcResult1.getResult().getName());
        if(rpcResult2 != null)
            createdRsp2 = SfcProviderRenderedPathAPI.readRenderedServicePath(rpcResult2.getResult().getName());
        if(rpcResult3 != null)
            createdRsp3 = SfcProviderRenderedPathAPI.readRenderedServicePath(rpcResult3.getResult().getName());

        assertNotNull("Must not be null", createdRsp1);
        assertNotNull("Must not be null", createdRsp2);
        assertNotNull("Must not be null", createdRsp3);

        //check hops
        assertEquals("Must be equal", createdRsp1.getRenderedServicePathHop().size(), 4);
        assertEquals("Must be equal", createdRsp2.getRenderedServicePathHop().size(), 4);
        assertEquals("Must be equal", createdRsp3.getRenderedServicePathHop().size(), 8);

        //check symmetry and classifiers
        //path 1
        ServiceFunctionPath serviceFunctionPath1 = SfcProviderServicePathAPI.readServiceFunctionPath(pathNames[0]);
        assertNotNull("Must not be null", serviceFunctionPath1);
        assertFalse("Must be false", serviceFunctionPath1.isSymmetric()); //path is not symmetric
        assertNull("Must be null", serviceFunctionPath1.getSymmetricClassifier()); //no symmetric classifier
        assertEquals("Must be equal", serviceFunctionPath1.getClassifier(), classifier); //classifier

        //path 2
        ServiceFunctionPath serviceFunctionPath2 = SfcProviderServicePathAPI.readServiceFunctionPath(pathNames[1]);
        assertNotNull("Must not be null", serviceFunctionPath2);
        assertFalse("Must be false", serviceFunctionPath2.isSymmetric()); //path is not symmetric
        assertNull("Must be null", serviceFunctionPath2.getSymmetricClassifier()); //no symmetric classifier
        assertEquals("Must be equal", serviceFunctionPath2.getClassifier(), reversedClassifier); //classifier

        //path 3
        ServiceFunctionPath serviceFunctionPath3 = SfcProviderServicePathAPI.readServiceFunctionPath(pathNames[2]);
        assertNotNull("Must not be null", serviceFunctionPath3);
        assertTrue("Must be true", serviceFunctionPath3.isSymmetric()); //path not symmetric
        assertEquals("Must be equal", serviceFunctionPath3.getSymmetricClassifier(), reversedClassifier); //symmetric classifier
        assertEquals("Must be equal", serviceFunctionPath3.getClassifier(), classifier); //classifier

        //read paths in classifier
        SclRenderedServicePath path = readClassifierRsp(classifier, rpcResult1.getResult().getName(), false);
        assertNotNull("Must not be null", path); //path 1 exists in classifier
        path = readClassifierRsp(classifier, rpcResult2.getResult().getName(), false);
        assertNull("Must be null", path); //path 2 does NOT exists in classifier
        path = readClassifierRsp(classifier, rpcResult3.getResult().getName(), false);
        assertNotNull("Must not be null", path); //path 3 exists in classifier

        //read paths in classifier
        path = readClassifierRsp(reversedClassifier, rpcResult1.getResult().getName(), true);
        assertNull("Must be null", path); //path 1 does NOT exists in classifier
        path = readClassifierRsp(reversedClassifier, rpcResult2.getResult().getName(), false); //this classifier is not reversed for rsp2
        assertNotNull("Must not be null", path); //path 2 exists in classifier
        path = readClassifierRsp(reversedClassifier, rpcResult3.getResult().getName(), true);
        assertNotNull("Must not be null", path); //path 3 exists both classifiers
    }

    //auxiliary methods below

    /*
     * create classifier and write into data store, the only parameter is classifier name
     */
    private boolean createClassifier(String classifierName) {
        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(classifierName)
                .setKey(new ServiceFunctionClassifierKey(classifierName));

        InstanceIdentifier<ServiceFunctionClassifier> sclIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                .child(ServiceFunctionClassifier.class, new ServiceFunctionClassifierKey(classifierName)).build();

       return SfcDataStoreAPI.writePutTransactionAPI(sclIID,
               serviceFunctionClassifierBuilder.build(), LogicalDatastoreType.CONFIGURATION);
    }

    /*
     * read all rendered service paths operating with classifier
     * method needs classifier name, rendered service path name and whether the classifier is reversed or not
     */
    private SclRenderedServicePath readClassifierRsp(String classifierName, String rpcName, boolean reversed) {

        if(reversed)
            rpcName += "-Reverse";

        InstanceIdentifier<SclRenderedServicePath> sclRspIID = InstanceIdentifier.builder(ServiceFunctionClassifiersState.class)
                .child(ServiceFunctionClassifierState.class, new ServiceFunctionClassifierStateKey(classifierName))
                .child(SclRenderedServicePath.class, new SclRenderedServicePathKey(rpcName)).build();

        return SfcDataStoreAPI.readTransactionAPI(sclRspIID, LogicalDatastoreType.OPERATIONAL);
    }

    /*
     * create service function type
     * put type, and list of all service functions with that type
     */
    private boolean createServiceFunctionType(Class<? extends ServiceFunctionTypeIdentity> serviceType, List<String> sfNames) {
        List<SftServiceFunctionName> sftServiceFunctionNames = new ArrayList<>();
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder;

        for (String sfName : sfNames) {
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
    private boolean createServiceFunctionForwarder(String forwarderName, String locatorName) {
        List<SffDataPlaneLocator> sffDataPlaneLocator = new ArrayList<>();
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder;

        sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        sffDataPlaneLocatorBuilder.setName(locatorName)
                .setKey(new SffDataPlaneLocatorKey(locatorName));
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
    private boolean createServiceFunction(String functionName, Class<? extends ServiceFunctionTypeIdentity> functionType, String locatorName, String forwarderName) {
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
    private boolean createServiceFunctionGroup(String groupName, List<String> sfNames, Class<? extends ServiceFunctionTypeIdentity> groupType) {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();

        /*
         * create service function list, using type SfcServiceFunction 150214
         * this is necessary for service function group
         * there is no info about service function type, it is specified in service function group
         */
        List<org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunctionBuilder sfcServiceFunctionBuilder =
                new org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunctionBuilder();

        for (String sfName : sfNames) {
            sfcServiceFunctionBuilder.setName(sfName)
                    .setKey(new org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunctionKey(sfName));
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
     * specify chain name, list of service functions (all functions should be written into data store and contain info
     * about type) and whether the chain si symmetric or not
     */
    private boolean createServiceFunctionChain(String chainName, List<String> sfNames) {
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();

        /*
         * create service function list, using type SfcServiceFunction rev14071
         * this is necessary for service function chain
         * there is also info about service function type, because we cannot put this info directly to chain
         */
        List<SfcServiceFunction> sfcSfgServiceFunctionList = new ArrayList<>();

        SfcServiceFunctionBuilder sfcSfgServiceFunctionBuilder = new SfcServiceFunctionBuilder();

        for (String sfName : sfNames) {
            sfcSfgServiceFunctionBuilder.setName(sfName)
                    .setKey(new SfcServiceFunctionKey(sfName))
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
     * specify path name, name of used chain, classifier name, reverse classifier name and whether the path is symmetric or not
     */
    private boolean createServiceFunctionPath(String pathName, String chainName, String classifierName, String classifierRevName) {
        boolean symmetric = false;
        if(classifierRevName != null)
            symmetric = true;

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(pathName)
                .setKey(new ServiceFunctionPathKey(pathName))
                .setServiceChainName(chainName)
                .setClassifier(classifierName)
                .setSymmetricClassifier(classifierRevName)
                .setSymmetric(symmetric);
        return SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPathBuilder.build());
    }

    /*
     * input parameter, needs path name, classifiers and if path is symmetric or not
     */
    private CreateRenderedPathInput createRenderedPathInput(String pathName, String classifierName, String classifierRevName) {
        boolean symmetric = false;

        if(classifierName != null)
            symmetric = true;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setParentServiceFunctionPath(pathName)
                .setClassifier(classifierName)
                .setSymmetricClassifier(classifierRevName)
                .setSymmetric(symmetric);

        return createRenderedPathInputBuilder.build();
    }
}
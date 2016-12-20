/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_pot_netconf.provider;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfNodeManager;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfIoam;

import org.opendaylight.sfc.pot.netconf.renderer.provider.api.SfcPotPolyAPI;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.sfc.provider.SfcProviderRpc;

import org.opendaylight.sfc.pot.provider.SfcPotRspProcessor;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.params.rev161205.PolyParameters;

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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* Note: Based on IosXeRspProcessorTest */
public class SfcPotNetconfRspProcessorTest extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfRspProcessorTest.class);
    private final List<ServiceFunction> sfList = new ArrayList<>();
    private DataBroker dataBroker;
    private final String mgmtIp = "10.0.0.1";
    private final String nodeIdString = "nodeId";
    private final long REFRESH_VALUE = 1000;
    private final long POT_NUM_PROFILES = 2;

    /* Note: following finals from SfcProviderRpcTest */
    @SuppressWarnings("serial")
    private final List<SffName> sffNames = new ArrayList<SffName>() {
        {
            add(new SffName("sff1"));
            add(new SffName("sff2"));
            add(new SffName("sff3"));
            add(new SffName("sff4"));
        }
    };

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
            //add("http-header-enrichment");
            //add("qos");
        }
    };

    private static final SfcName SFC_NAME = new SfcName("unittest-chain-1");
    private static final SfpName SFP_NAME = new SfpName("unittest-sfp-1");
    private static final RspName RSP_NAME = new RspName("ioam-test-rsp-rend");

    private SfcProviderRpc sfcProviderRpc;
    private SfcPotNetconfNodeManager nodeManager;
    private SfcPotNetconfIoam sfcPotNetconfIoam;

    @Before
    public void init() {
        dataBroker = getDataBroker();
        sfcProviderRpc = new SfcProviderRpc();
        SfcProviderRpc.setDataProviderAux(dataBroker);
        SfcDataStoreAPI.setDataProviderAux(dataBroker);
        nodeManager = mock(SfcPotNetconfNodeManager.class);
        prepareSfcEntities();
    }

    @Test
    public void updateRsp() {
        Map<NodeId, Node> nodeMap = new HashMap<>();
        NodeId nodeId = new NodeId(nodeIdString);
        NodeBuilder nodeBuilder = new NodeBuilder();
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setHost(new Host(new IpAddress(new Ipv4Address(mgmtIp))));
        nodeBuilder.setNodeId(nodeId)
                .addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        Node node = nodeBuilder.build();
        nodeMap.put(nodeId, node);

        Map<NodeId, DataBroker> nodeWithDataBrokerMap = new HashMap<>();
        nodeWithDataBrokerMap.put(nodeId, dataBroker);

        when(nodeManager.getNetconfNodeIp(node)).thenReturn(new IpAddress(new Ipv4Address(mgmtIp)));
        when(nodeManager.getNodeIdFromIpAddress(new IpAddress(new Ipv4Address(mgmtIp)))).thenReturn(nodeId);

        /* Enable SFC PoT on the RSP */
        RenderedServicePath rsp = enableSfcPot();
        assertNotNull(rsp);

        /* SB configuration generator and netconf handler */
        sfcPotNetconfIoam = new SfcPotNetconfIoam(nodeManager);
        sfcPotNetconfIoam.processRspUpdate(rsp);

        /* Verify calls done from inside the SB APIs */
        verify(nodeManager, times(1)).getNodeIdFromIpAddress(new IpAddress(new Ipv4Address(mgmtIp)));

        /* Verify Ioam PoT parameters generated from calls before */
        PolyParameters params = SfcPotPolyAPI.getInstance().getIoamPotParameters(0);

        /* Verify outcome from SB API config generation */
        assertNotNull(params);

        /* Verify refresh value stored in profiles */
        assertTrue(params.getRefreshPeriodValue() == REFRESH_VALUE);

        /* Note: Need to test timers, other parameters etc., in future. */
    }

    private RenderedServicePath enableSfcPot() {
        boolean ret;
        RenderedServicePathKey renderedServicePathKey = new RenderedServicePathKey(RSP_NAME);
        RenderedServicePath renderedServicePath = createTestRenderedServicePath();

        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey)
                .build();

        SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath,
                                                 LogicalDatastoreType.OPERATIONAL);

        SfcPotRspProcessor.enableSfcPot(renderedServicePath,
                                        null, REFRESH_VALUE, null, POT_NUM_PROFILES);

        return (SfcDataStoreAPI.readTransactionAPI(rspIID, LogicalDatastoreType.OPERATIONAL));
    }


    private RenderedServicePath createTestRenderedServicePath() {
        prepareSfcEntities();

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue())
            .setParentServiceFunctionPath(SFP_NAME.getValue())
            .setSymmetric(true);
        CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath, createRenderedPathInput);

        // Note: Intermediate checks skipped
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

        return (SfcProviderRenderedPathAPI.readRenderedServicePath(RSP_NAME));
    }

    /* From SfcProviderRpcTest */
    private void prepareSfcEntities() {
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
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(mgmtIp)))
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
}

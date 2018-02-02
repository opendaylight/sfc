/**
 * Copyright (c) 2018 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.testutils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.vpp.rev160706.SffNetconfAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.vpp.rev160706.SffNetconfAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.sfc.of.table.offsets.SfcOfTablesByBaseTable;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.sfc.of.table.offsets.SfcOfTablesByBaseTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.sfc.of.table.offsets.SfcOfTablesByBaseTableKey;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.RspLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.RspLogicalSffAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsTestUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsTestUtils.class);

    @SuppressWarnings("serial")
    public static final List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {

        {
            add("196.168.55.1");
            add("196.168.55.2");
            add("196.168.55.3");
            add("196.168.55.4");
            add("196.168.55.5");
        }
    };

    @SuppressWarnings("serial")
    public static final List<String> IP_MGMT_ADDRESS = new ArrayList<String>() {

        {
            add("196.168.55.101");
            add("196.168.55.102");
            add("196.168.55.103");
            add("196.168.55.104");
            add("196.168.55.105");
        }
    };

    @SuppressWarnings("serial")
    public static final List<SfName> SERVICE_FUNCTION_NAMES = new ArrayList<SfName>() {

        {
            add(new SfName("unittest-fw-1"));
            add(new SfName("unittest-dpi-1"));
            add(new SfName("unittest-napt-1"));
            add(new SfName("unittest-http-header-enrichment-1"));
            add(new SfName("unittest-qos-1"));
        }
    };

    @SuppressWarnings("serial")
    public static final List<Integer> PORT = new ArrayList<Integer>() {

        {
            add(1111);
            add(2222);
            add(3333);
            add(4444);
            add(5555);
        }
    };

    @SuppressWarnings("serial")
    public static final List<SftTypeName> SERVICE_FUNCTION_TYPES = new ArrayList<SftTypeName>() {

        {
            add(new SftTypeName("firewall"));
            add(new SftTypeName("dpi"));
            add(new SftTypeName("napt44"));
            add(new SftTypeName("http-header-enrichment"));
            add(new SftTypeName("qos"));
        }
    };

    @SuppressWarnings("serial")
    public static final List<String> SF_ABSTRACT_NAMES = new ArrayList<String>() {

        {
            add("firewall");
            add("dpi");
            add("napt");
            add("http-header-enrichment");
            add("qos");
        }
    };

    @SuppressWarnings("serial")
    public static final List<SffName> SFF_NAMES = new ArrayList<SffName>() {

        {
            add(new SffName("SFF1"));
            add(new SffName("SFF2"));
            add(new SffName("SFF3"));
            add(new SffName("SFF4"));
            add(new SffName("SFF5"));
        }
    };

    @SuppressWarnings("serial")
    public static final List<String> SFF_LOCATOR_IP = new ArrayList<String>() {

        {
            add("196.168.66.101");
            add("196.168.66.102");
            add("196.168.66.103");
            add("196.168.66.104");
            add("196.168.66.105");
        }
    };

    @SuppressWarnings("serial")
    public static final List<SfcName> SFC_NAMES = new ArrayList<SfcName>() {

        {
            add(new SfcName("UT-SFC1"));
            add(new SfcName("UT-SFC2"));
            add(new SfcName("UT-SFC3"));
        }
    };

    @SuppressWarnings("serial")
    public static final List<SfpName> SFP_NAMES = new ArrayList<SfpName>() {

        {
            add(new SfpName("UT-SFP1"));
            add(new SfpName("UT-SFP2"));
            add(new SfpName("UT-SFP3"));
        }
    };

    protected static final String OPENFLOW_NODEID = "openflow:112233445566";
    protected static short BASE_TABLE_NUMBER = 0;

    /**
     * Creates SFs, SFFs, SFCs, and SFPs. For each SFP the config RSP will be created SFP listener
     * and the operational RSP will be created by the RSP listener. Since this UT doesnt have the
     * listeners registered, we'll have to do the RSP creation steps manually.
     *
     * @param numRsps - number of SFCs, SFPs, and RSPs to create
     * @param isSymmetric - if the SFP and RSP should be symmetric
     * @return a list of the RSPs created
     */
    public List<RenderedServicePath> createOperationalRsps(int numRsps, boolean isSymmetric) {
        int numHops   = 2;
        int numSffs   = 1;
        int numSfs    = 2;

        List<ServiceFunction> sfList = createSfs(numSfs, SFF_NAMES.get(0));
        createOpenflowSffs(numSffs, sfList);
        List<ServiceFunctionChain> sfcList = createSfcs(numRsps, numHops);
        List<ServiceFunctionPath> sfpList = createSfps(sfcList, isSymmetric);

        // Create the RSPs
        List<RenderedServicePath> operRsps = new ArrayList<>();
        for (int i = 0; i < numRsps; i++) {
            ServiceFunctionPath sfp = sfpList.get(i);

            // Create the Config RSP, this normally happens in the SFP listener
            // This call will optionally create the symmetric RSP
            RenderedServicePath configRsp = SfcProviderRenderedPathAPI.createRenderedServicePathInConfig(sfp);

            // Create the Operational RSP, this normally happens in the RSP Config datastore listener
            RenderedServicePath operRsp = SfcProviderRenderedPathAPI
                    .createRenderedServicePathAndState(sfp, configRsp);
            createNextHopFlow(operRsp);
            operRsps.add(operRsp);
            LOG.info("createOperationalRsps Created Oper RSP [{}]", operRsp.getName().getValue());

            // Optionally create the Symmetric RSP
            if (isSymmetric) {
                configRsp = getConfigRsp(sfp, true);
                if (configRsp == null) {
                    LOG.warn("Could not get symmetric RSP for SFP [{}]", sfp.getName().getValue());
                    continue;
                }
                // Create the Symmetric Operational RSP
                operRsp = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfp, configRsp);
                createNextHopFlow(operRsp);
                operRsps.add(operRsp);
                LOG.info("createOperationalRsps Created Symmetric Oper RSP [{}]", operRsp.getName().getValue());
            }

        }

        return operRsps;
    }

    /**
     * Creates a simple RSP with Logical SFF augmentations added.
     * No SFFs are created, and the hop list is empty.
     *
     * @return an RSP setup for Logical SFFs
     */
    public RenderedServicePath createLogicalRsp() {
        RenderedServicePathHopBuilder rspHopBuilder = new RenderedServicePathHopBuilder();
        rspHopBuilder.setHopNumber((short) 1);
        rspHopBuilder.setServiceFunctionForwarder(SFF_NAMES.get(0));
        rspHopBuilder.setServiceFunctionName(SERVICE_FUNCTION_NAMES.get(0));
        rspHopBuilder.addAugmentation(RspLogicalSffAugmentation.class, new RspLogicalSffAugmentationBuilder().build());

        RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setRenderedServicePathHop(Collections.singletonList(rspHopBuilder.build()));

        return rspBuilder.build();
    }

    /**
     * Create a list of SFPs, one per input SFC.
     *
     * @param sfcList - list of SFCs to use to create the SFPs
     * @param isSymmetric - boolean flag to indicate if the SFPs should be symmetric or not
     *
     * @return a list of created SFPs
     */
    public List<ServiceFunctionPath> createSfps(List<ServiceFunctionChain> sfcList, boolean isSymmetric) {
        List<ServiceFunctionPath> sfpList = new ArrayList<>();

        for (int i = 0; i < sfcList.size(); i++) {
            ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
            pathBuilder.setName(SFP_NAMES.get(i))
                    .setServiceChainName(sfcList.get(i).getName())
                    .setSymmetric(isSymmetric);
            ServiceFunctionPath sfp = pathBuilder.build();
            assertTrue("SFP creation must return true",
                    SfcProviderServicePathAPI.putServiceFunctionPath(sfp));

            sfpList.add(sfp);
        }

        return sfpList;
    }

    /**
     * Create a list of SFCs.
     *
     * @param numSfcs - The number of SFCs to create
     * @param numHops - The number of hops to create for each SFC
     *
     * @return a list of created SFCs
     */
    public List<ServiceFunctionChain> createSfcs(int numSfcs, int numHops) {
        List<ServiceFunctionChain> sfcList = new ArrayList<>();

        for (int i = 0; i < numSfcs; i++) {
            List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
            for (int j = 0; j < numHops; j++) {
                SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
                SfcServiceFunction sfcServiceFunction = sfcSfBuilder.setName(SF_ABSTRACT_NAMES.get(j))
                        .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES.get(j)))
                        .setType(SERVICE_FUNCTION_TYPES.get(j))
                        .build();
                sfcServiceFunctionList.add(sfcServiceFunction);
            }
            ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
            sfcBuilder.setName(SFC_NAMES.get(i))
                    .setKey(new ServiceFunctionChainKey(SFC_NAMES.get(i)))
                    .setSfcServiceFunction(sfcServiceFunctionList);

            ServiceFunctionChain sfc = sfcBuilder.build();
            sfcList.add(sfc);
            SfcProviderServiceChainAPI.putServiceFunctionChain(sfc);

            // Check if Service Function Chain was created
            ServiceFunctionChain createdSfc = SfcProviderServiceChainAPI.readServiceFunctionChain(SFC_NAMES.get(i));
            assertNotNull("Created SFC must not be null", createdSfc);
        }

        return sfcList;
    }

    /**
     * Create a list of OVS OpenFlow-based SFFs.
     *
     * @param numSffs - The number of SFFs to create
     * @param sfList - the SFs to use when creating SFFs
     *
     * @return a list of created SFFs
     */
    public List<ServiceFunctionForwarder> createOpenflowSffs(int numSffs, List<ServiceFunction> sfList) {
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();
        List<ServiceFunctionForwarderBuilder> sffBuilders = createBaseSffsBuilder(numSffs, sfList);

        // This makes sure the SfcOpenFlowStatisticsReader will be returned by the SfcStatisticsFactory
        // Set the NodeId on the SFF, this is usually done by the SfcOvsListener
        for (ServiceFunctionForwarderBuilder sffBuilder : sffBuilders) {
            SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
            OvsBridgeBuilder bridgeBuilder = new OvsBridgeBuilder();
            bridgeBuilder.setOpenflowNodeId(OPENFLOW_NODEID);
            sffOvsBridgeAugmentationBuilder.setOvsBridge(bridgeBuilder.build());
            sffBuilder.addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build());

            ServiceFunctionForwarder sff = sffBuilder.build();
            sffList.add(sff);

            SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);
            setTableOffsets(sff.getName(), BASE_TABLE_NUMBER);
        }

        return sffList;
    }

    /**
     * For now, since VPP-based statistics arent implemented, just return a simple VPP SFF.
     *
     * @param numSffs - the number of SFFs to create
     * @param sfList - SFs to use when creating the SFFs
     *
     * @return a list of VPP SFFs
     */
    public List<ServiceFunctionForwarder> createVppSffs(int numSffs, List<ServiceFunction> sfList) {
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();
        List<ServiceFunctionForwarderBuilder> sffBuilders = createBaseSffsBuilder(numSffs, sfList);

        for (ServiceFunctionForwarderBuilder sffBuilder : sffBuilders) {
            SffNetconfAugmentationBuilder sffNetconfAugmentationBuilder = new SffNetconfAugmentationBuilder();
            sffBuilder.addAugmentation(SffNetconfAugmentation.class, sffNetconfAugmentationBuilder.build());

            sffList.add(sffBuilder.build());
        }

        return sffList;
    }

    /**
     * For now, since IOSXE-based statistics arent implemented, just return a simple SFF.
     *
     * @param numSffs - the number of SFFs to create
     * @param sfList - SFs to use when creating the SFFs
     *
     * @return a list of IOXE SFFs
     */
    public List<ServiceFunctionForwarder> createIosXeSffs(int numSffs, List<ServiceFunction> sfList) {
        return createBaseSffs(numSffs, sfList);
    }

    /**
     * Wrapper method around createBaseSffBuilder to just return built SFFs.
     *
     * @param numSffs - how many SFFs to create
     * @param sfList - a list of SFs to use when creating the SFFs
     *
     * @return a list of the created SFFs
     */
    public List<ServiceFunctionForwarder> createBaseSffs(int numSffs, List<ServiceFunction> sfList) {
        List<ServiceFunctionForwarderBuilder> sffBuilderList = createBaseSffsBuilder(numSffs, sfList);
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();

        for (ServiceFunctionForwarderBuilder sffBuilder : sffBuilderList) {
            sffList.add(sffBuilder.build());
        }

        return sffList;
    }

    private List<ServiceFunctionForwarderBuilder> createBaseSffsBuilder(int numSffs, List<ServiceFunction> sfList) {

        List<ServiceFunctionForwarderBuilder> sffList = new ArrayList<>();
        for (int i = 0; i < numSffs; i++) {
            ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = null;

            // TODO we need to iterate the sfList

            // Its possible for the sfList to be empty or to have fewer SFs than SFFs
            if (!sfList.isEmpty() && i <= sfList.size()) {
                // ServiceFunctions attached to SFF_NAMES[i]
                ServiceFunction serviceFunction = sfList.get(i);
                SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
                sffSfDataPlaneLocatorBuilder.setSfDplName(serviceFunction.getSfDataPlaneLocator().get(0).getName());

                dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
                dictionaryEntryBuilder.setName(serviceFunction.getName())
                        .setKey(new ServiceFunctionDictionaryKey(serviceFunction.getName()))
                        .setSffSfDataPlaneLocator(sffSfDataPlaneLocatorBuilder.build())
                        .setFailmode(Open.class)
                        .setSffInterfaces(null);
            }

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(SFF_LOCATOR_IP.get(i))))
                    .setPort(new PortNumber(PORT.get(i)));
            DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
            sffLocatorBuilder.setLocatorType(ipBuilder.build())
                    .setTransport(VxlanGpe.class);
            SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SffDataPlaneLocatorName(SFF_LOCATOR_IP.get(i)))
                    .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName(SFF_LOCATOR_IP.get(i))))
                    .setDataPlaneLocator(sffLocatorBuilder.build());

            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
            sffBuilder.setName(new SffName(SFF_NAMES.get(i)))
                    .setKey(new ServiceFunctionForwarderKey(new SffName(SFF_NAMES.get(i))))
                    .setSffDataPlaneLocator(Collections.singletonList(locatorBuilder.build()))
                    .setServiceNode(null);
            if (dictionaryEntryBuilder != null) {
                sffBuilder.setServiceFunctionDictionary(Collections.singletonList(dictionaryEntryBuilder.build()));
            }

            sffList.add(sffBuilder);
        }

        return sffList;
    }

    /**
     * Create a list of SFs.
     *
     * @param numSfs - The number of SFs to create
     * @param createSfOnSff - the SFF to create the SFF on
     *
     * @return a list of created SFs
     */
    public List<ServiceFunction> createSfs(int numSfs, SffName createSfOnSff) {
        final List<ServiceFunction> sfList = new ArrayList<>();

        for (int i = 0; i < numSfs; i++) {
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(0))))
                    .setPort(new PortNumber(PORT.get(i)));

            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(i)))
                    .setLocatorType(ipBuilder.build())
                    .setServiceFunctionForwarder(createSfOnSff);

            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(locatorBuilder.build());

            SfName sfName = SERVICE_FUNCTION_NAMES.get(i);
            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            sfBuilder.setName(sfName)
                    .setKey(new ServiceFunctionKey(sfName))
                    .setType(SERVICE_FUNCTION_TYPES.get(i))
                    .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(0))))
                    .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        SfcDataStoreAPI.writePutTransactionAPI(SfcInstanceIdentifiers.SF_IID, sfsBuilder.build(),
                LogicalDatastoreType.CONFIGURATION);

        // Create ServiceFunctionTypeEntry for all ServiceFunctions
        for (ServiceFunction serviceFunction : sfList) {
            LOG.debug("call createServiceFunctionTypeEntryExecutor for {}", serviceFunction.getName());
            assertTrue("ServiceFunctionTypeEntry creation must be true",
                    SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction));
        }

        return sfList;
    }

    /**
     * Given an RSP and hop number, return the corresponding SFF.
     *
     * @param rsp - the RSP that has the SFF to be returned
     * @param hopNumber - the hop on the RSP
     *
     * @return the indicated SFF from the RSP
     */
    public ServiceFunctionForwarder getSffFromRsp(RenderedServicePath rsp, int hopNumber) {
        List<RenderedServicePathHop> rspHops = rsp.getRenderedServicePathHop();
        assertTrue(rspHops.size() >= hopNumber);

        RenderedServicePathHop rspHop = rspHops.get(hopNumber);
        return SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(rspHop.getServiceFunctionForwarder());
    }

    /**
     * Store the OpenFlow table offsets for the SFF in the data store.
     *
     * @param sffName - the SFF to store the offsets for
     * @param tableBase - the base table to use for the table offsets
     */
    public void setTableOffsets(SffName sffName, long tableBase) {
        long tableBaseValue = tableBase < 0
                ? 0
                : tableBase;

        SfcOfTablesByBaseTableBuilder sfcOfTablesByBaseTableBuilder = new SfcOfTablesByBaseTableBuilder();
        sfcOfTablesByBaseTableBuilder.setSffName(sffName);
        sfcOfTablesByBaseTableBuilder.setKey(new SfcOfTablesByBaseTableKey(sffName));
        sfcOfTablesByBaseTableBuilder.setBaseTable(tableBaseValue);
        sfcOfTablesByBaseTableBuilder.setTransportIngressTable(tableBaseValue + 1);
        sfcOfTablesByBaseTableBuilder.setPathMapperTable(tableBaseValue + 2);
        sfcOfTablesByBaseTableBuilder.setPathMapperAclTable(tableBaseValue + 3);
        sfcOfTablesByBaseTableBuilder.setNextHopTable(tableBaseValue + 4);
        sfcOfTablesByBaseTableBuilder.setTransportEgressTable(tableBaseValue + 10);

        InstanceIdentifier<SfcOfTablesByBaseTable> iid = InstanceIdentifier.create(SfcOfTableOffsets.class)
                .child(SfcOfTablesByBaseTable.class, new SfcOfTablesByBaseTableKey(sffName));

        SfcDataStoreAPI.writeMergeTransactionAPI(iid, sfcOfTablesByBaseTableBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);
    }

    private RenderedServicePath getConfigRsp(ServiceFunctionPath sfp, boolean isReverse) {

        // The RSP name is based on the SFP name: "<SFP-Name>-<PathId>" and "<SFP-Name>-<PathId>-Reverse"

        InstanceIdentifier<RenderedServicePaths> rspIID = InstanceIdentifier.create(RenderedServicePaths.class);
        RenderedServicePaths rsps = SfcDataStoreAPI.readTransactionAPI(rspIID, LogicalDatastoreType.CONFIGURATION);
        List<RenderedServicePath> rspList = rsps.getRenderedServicePath();
        for (RenderedServicePath rsp : rspList) {
            if (rsp.getName().getValue().startsWith(sfp.getName().getValue())) {
                // If we're looking for the Reverse RSP, then check the suffix
                if (isReverse) {
                    if (rsp.getName().getValue().endsWith("Reverse")) {
                        // We found the reverse RSP
                        return rsp;
                    } else {
                        continue;
                    }
                } else {
                    // We found the RSP
                    return rsp;
                }
            }
        }

        return null;
    }

    private void createNextHopFlow(RenderedServicePath rsp) {
        // Create a NextHop flow for each RSP hop
        for (RenderedServicePathHop rspHop : rsp.getRenderedServicePathHop()) {
            StringJoiner flowName = new StringJoiner(OpenflowConstants.OF_NAME_DELIMITER);
            flowName.add(OpenflowConstants.OF_NAME_NEXT_HOP)
                    .add(String.valueOf(rspHop.getServiceIndex()))
                    .add(String.valueOf(rsp.getPathId()));
            FlowKey flowKey = new FlowKey(new FlowId(flowName.toString()));

            FlowBuilder flowBuilder = new FlowBuilder();
            flowBuilder.setFlowName(flowName.toString());
            flowBuilder.setKey(flowKey);

            FlowStatisticsBuilder flowStatsBuilder = new FlowStatisticsBuilder();
            flowStatsBuilder.setByteCount(new Counter64(new BigInteger("0")));
            flowStatsBuilder.setPacketCount(new Counter64(new BigInteger("0")));
            FlowStatisticsDataBuilder flowStatsDataBuilder = new FlowStatisticsDataBuilder();
            flowStatsDataBuilder.setFlowStatistics(flowStatsBuilder.build());
            flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatsDataBuilder.build());

            InstanceIdentifier<Flow> iidFlow = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, new NodeKey(new NodeId(OPENFLOW_NODEID)))
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class, new TableKey((short) (BASE_TABLE_NUMBER + 4)))
                    .child(Flow.class, flowKey).build();
            SfcDataStoreAPI.writeMergeTransactionAPI(iidFlow, flowBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        }
    }
}

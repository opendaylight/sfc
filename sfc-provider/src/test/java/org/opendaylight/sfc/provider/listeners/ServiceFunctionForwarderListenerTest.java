/*
 * Copyright (c) 2016, 2018 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test Suite to test the ServiceFunctionForwarderListener class.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
public class ServiceFunctionForwarderListenerTest extends AbstractDataStoreManager {

    // Class under test
    private ServiceFunctionForwarderListener serviceFunctionForwarderListener;

    @Before
    public void before() {
        setupSfc();
        serviceFunctionForwarderListener = new ServiceFunctionForwarderListener(getDataBroker());
    }

    @After
    public void after() throws Exception {
        close();
    }

    /**
     * Test that creates a Service Forwarder Function, calls listener
     * explicitly, verify that Service Function Forwarder was created and cleans
     * up.
     */
    @Test
    public void testOnServiceFunctionForwarderCreated() {
        ServiceFunctionForwarder serviceFunctionForwarder = buildServiceFunctionForwarder();

        serviceFunctionForwarderListener.add(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                             serviceFunctionForwarder);
    }

    /**
     * Test that removes a Service Function Forwarder, calls listener
     * explicitly, verify that the Service Function Forwarder was removed and
     * cleans up.
     */
    @Test
    public void testOnServiceFunctionForwarderRemoved() {
        ServiceFunctionForwarder serviceFunctionForwarder = buildServiceFunctionForwarder();

        serviceFunctionForwarderListener.remove(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                serviceFunctionForwarder);
    }

    /**
     * In this test we create a RSP and remove a SF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Remove first SFF used by RSP by explicitly calling the
     * listener - creates a IID and add to removedPaths data structure. This IID
     * points to the SF objects stored in the original data - Call listener
     * explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderRemovedWithRSP() throws Exception {
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePath();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SFF used by the RSP
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder serviceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(serviceFunctionForwarder);

        // The listener will remove the Service Function Forwarder Entry
        serviceFunctionForwarderListener.remove(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                serviceFunctionForwarder);
        // Verify that SFP was removed
        assertNull(SfcProviderServicePathAPI.readServiceFunctionPath(
                renderedServicePath.getParentServiceFunctionPath()));

        // The SFP State is removed in the SFP listener
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update first SFF used by RSP by explicitly calling the
     * listener - creates a IID and add to removedPaths data structure. This IID
     * points to the SF objects stored in the original data - Call listener
     * explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdatedIpMgmt() throws Exception {
        final String updatedManagementIP = "196.168.55.110";
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePath();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SFF used by the RSP
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. We change the management address
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(updatedManagementIP));
        updatedServiceFunctionForwarderBuilder.setIpMgmtAddress(updatedIpMgmtAddress);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will remove the Original Service Function Forwarder Entry and associated RSPs
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);
        assertNull(SfcProviderServicePathAPI.readServiceFunctionPath(
                renderedServicePath.getParentServiceFunctionPath()));

        // The SFP state is removed in the SFP listener

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID,
                LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update the Service node used by the SFF by setting it to
     * null, which should cause the RSP to be deleted. - creates a IID and add
     * to removedPaths data structure. This IID points to the SFF objects stored
     * in the original data - Call listener explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdatedServiceNode() throws Exception {
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePath();
        assertNotNull(renderedServicePath);

        // Get the original SFF
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the Service Node
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        updatedServiceFunctionForwarderBuilder.setServiceNode(null);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will remove the Original Service Function Forwarder
        // Entry and associated RSPs
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);
        assertNull(SfcProviderServicePathAPI.readServiceFunctionPath(
                renderedServicePath.getParentServiceFunctionPath()));

        // The SFP state is removed in the SFP listener

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID,
                LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update the SfDictionary by adding a new SF. The RSP should
     * NOT be deleted. - creates a IID and add to removedPaths data structure.
     * This IID points to the SFF objects stored in the original data - Call
     * listener explicitly. - Update the SfDictionary by removing the new SF.
     * The RSP should NOT be deleted. - Call listener explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdatedAddAndRemoveSfDict() throws Exception {
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePath();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the SffSfDictionary
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        addSfToSfDict(updatedServiceFunctionForwarderBuilder);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will NOT remove the RSP
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);
        assertNotNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was NOT removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNotNull(sffServicePathList);

        // Now we remove the added unused dictionary
        updatedServiceFunctionForwarder = originalServiceFunctionForwarder;
        originalServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will NOT remove the RSP
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);
        assertNotNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was NOT removed
        sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNotNull(sffServicePathList);

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID,
                LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update the SfDictionary by adding a removing a used SF
     * dictionary. The SFP should be deleted. - creates a IID and add to
     * removedPaths data structure. This IID points to the SFF objects stored in
     * the original data - Call listener explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderRemoveUsedSfDict() throws Exception {
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePath();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the SffSfDictionary
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        updatedServiceFunctionForwarderBuilder.setServiceFunctionDictionary(Collections.emptyList());
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will remove the SFP
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);
        assertNull(SfcProviderServicePathAPI.readServiceFunctionPath(
                renderedServicePath.getParentServiceFunctionPath()));

        // The State is removed by the SFP listener

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID,
                LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update the SFF DPL by removing an entry, which should cause
     * the RSP to be deleted. - creates a IID and add to removedPaths data
     * structure. This IID points to the SFF objects stored in the original data
     * - Call listener explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdateSffDpl() throws Exception {
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePath();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the DataPlaneLocator
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        removeAnySffDpl(updatedServiceFunctionForwarderBuilder);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will remove the SFP
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);
        assertNull(SfcProviderServicePathAPI.readServiceFunctionPath(
                renderedServicePath.getParentServiceFunctionPath()));

        // The State is removed by the SFP listener

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID,
                LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * Test that the removal of a SFF DPL that it is not used in any SF
     * dictionary entry, will not cause the removal of RSP that only traverses
     * that SFF.
     */
    @Test
    public void testOnServiceFunctionForwarderUpdateUnusedDplWontRemoveSingleSffRsp() throws Exception {
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePathSingleSff();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the DataPlaneLocator
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        removeSffDpl(updatedServiceFunctionForwarderBuilder, "196.168.66.106");
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will NOT remove the RSP
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);

        assertNotNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was NOT removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNotNull(sffServicePathList);

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID,
                LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * Test that the removal of a SFF DPL that it is used in a SF dictionary
     * entry, will cause the removal of RSP even though it only traverses that
     * SFF.
     */
    @Test
    public void testOnServiceFunctionForwarderUpdateUsedDplWillRemoveSingleSffRsp() throws Exception {
        RenderedServicePath renderedServicePath = buildAndCommitRenderedServicePathSingleSff();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the DataPlaneLocator
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        removeSffDpl(updatedServiceFunctionForwarderBuilder, "196.168.66.105");
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // The listener will remove the SFP
        serviceFunctionForwarderListener.update(InstanceIdentifier.create(ServiceFunctionForwarder.class),
                                                originalServiceFunctionForwarder, updatedServiceFunctionForwarder);
        assertNull(SfcProviderServicePathAPI.readServiceFunctionPath(
                renderedServicePath.getParentServiceFunctionPath()));

        // The SFP State is removed in the SFP listener

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID,
                LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID,
                LogicalDatastoreType.CONFIGURATION));
    }

    private void addSfToSfDict(ServiceFunctionForwarderBuilder sffBuilder) {
        List<ServiceFunctionDictionary> sffSfDict = sffBuilder.getServiceFunctionDictionary();
        List<ServiceFunctionDictionary> newSffSfDict = new ArrayList<>();

        // First create a builder by copying the existing entries
        if (sffSfDict != null) {
            newSffSfDict.addAll(sffSfDict);
        }

        // Now add another entry
        String newName = "NEW NAME";
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder.setSfDplName(new SfDataPlaneLocatorName(newName))
                .setSffDplName(new SffDataPlaneLocatorName(newName));
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder.setName(new SfName(newName))
                .setKey(new ServiceFunctionDictionaryKey(new SfName(newName)))
                .setSffSfDataPlaneLocator(sffSfDataPlaneLocatorBuilder.build()).setFailmode(Open.class)
                .setSffInterfaces(null);
        newSffSfDict.add(dictionaryEntryBuilder.build());

        sffBuilder.setServiceFunctionDictionary(newSffSfDict);
    }

    private void removeAnySffDpl(ServiceFunctionForwarderBuilder sffBuilder) {
        List<SffDataPlaneLocator> sffDplList = sffBuilder.getSffDataPlaneLocator();
        List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        // We want to remove a DPL entry, so just copy all but the last one
        for (int i = 0; i < sffDplList.size() - 1; i++) {
            locatorList.add(sffDplList.get(i));
        }

        sffBuilder.setSffDataPlaneLocator(locatorList);
    }

    private void removeSffDpl(ServiceFunctionForwarderBuilder sffBuilder, String dplName) {
        List<SffDataPlaneLocator> sffDplList = sffBuilder.getSffDataPlaneLocator();
        List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        // We want to remove a DPL entry, so just copy all but the last one
        for (SffDataPlaneLocator sffDataPlaneLocator : sffDplList) {
            if (Objects.equals(dplName, sffDataPlaneLocator.getName().getValue())) {
                continue;
            }
            locatorList.add(sffDataPlaneLocator);
        }

        sffBuilder.setSffDataPlaneLocator(locatorList);
    }

    /**
     * Builds a complete service Function Object.
     *
     * @return ServiceFunction object
     */
    @SuppressWarnings("serial")
    ServiceFunctionForwarder buildServiceFunctionForwarder() {
        final SffName name = new SffName("SFF1");
        List<SfName> sfNames = new ArrayList<SfName>() {
            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-dpi-1"));
                add(new SfName("unittest-napt-1"));
                add(new SfName("unittest-http-header-enrichment-1"));
                add(new SfName("unittest-qos-1"));
            }
        };
        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames.size()];

        final List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address("10.1.1.101"))).setPort(new PortNumber(555));

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        locatorBuilder.setName(new SffDataPlaneLocatorName("locator-1"))
                .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName("locator-1")))
                .setDataPlaneLocator(sffLocatorBuilder.build());

        locatorList.add(locatorBuilder.build());

        final List<ServiceFunctionDictionary> dictionary = new ArrayList<>();

        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder.setName(new SfDataPlaneLocatorName("unittest-fw-1"))
                .setKey(new SfDataPlaneLocatorKey(new SfDataPlaneLocatorName("unittest-fw-1")));

        SfDataPlaneLocator sfDataPlaneLocator = sfDataPlaneLocatorBuilder.build();
        List<ServiceFunction> sfList = new ArrayList<>();

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);
        sfBuilder.setName(new SfName(sfNames.get(0))).setKey(new ServiceFunctionKey(new SfName("unittest-fw-1")))
                .setType(new SftTypeName("firewall")).setIpMgmtAddress(ipMgmtAddress[0])
                .setSfDataPlaneLocator(dataPlaneLocatorList);
        sfList.add(sfBuilder.build());

        ServiceFunction sf = sfList.get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder.setSfDplName(sf.getSfDataPlaneLocator().get(0).getName());
        SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder.setName(sf.getName()).setKey(new ServiceFunctionDictionaryKey(sf.getName()))
                .setSffSfDataPlaneLocator(sffSfDataPlaneLocator).setFailmode(Open.class).setSffInterfaces(null);

        ServiceFunctionDictionary dictionaryEntry = dictionaryEntryBuilder.build();
        dictionary.add(dictionaryEntry);

        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        ServiceFunctionForwarder sff = sffBuilder.setName(name).setKey(new ServiceFunctionForwarderKey(name))
                .setSffDataPlaneLocator(locatorList).setServiceFunctionDictionary(dictionary).setServiceNode(null)
                .build();

        assertTrue(SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff));

        return sff;

    }

    /**
     * Builds and return a complete RSP Object that traverses a single SFF.
     * The SFF has one unused locator, "196.168.66.106".
     *
     * @return RSP object
     */
    @SuppressWarnings("serial")
    RenderedServicePath buildAndCommitRenderedServicePathSingleSff() throws Exception {

        final String[] managementIPAddress = {
            "196.168.55.101",
        };

        final String[] serviceFunctionTypes = {
            "firewall",
            "dpi",
            "napt44",
            "http-header-enrichment",
            "qos"
        };

        final String[] serviceFunctionAbstractNames = {
            "firewall",
            "dpi",
            "napt",
            "http-header-enrichment",
            "qos"
        };

        final String sfcName = "unittest-chain-1";
        final String sfpName = "unittest-sfp-1";
        final String rspName = "unittest-rsp-1";

        final String[][] rsp = {
                { "SFF1", "unittest-fw-1" },
                { "SFF1", "unittest-dpi-1" },
                { "SFF1", "unittest-napt-1" },
                { "SFF1", "unittest-http-header-enrichment-1" },
                { "SFF1", "unittest-qos-1" }
        };

        final String[][] sfLocatorIps = {
                {"196.168.55.1"},
                {"196.168.55.2"},
                {"196.168.55.3"},
                {"196.168.55.4"},
                {"196.168.55.5"}
        };

        final String[][] sffLocatorIps = {
            {
                "196.168.66.101",
                "196.168.66.102",
                "196.168.66.103",
                "196.168.66.104",
                "196.168.66.105",
                "196.168.66.106"
            }
        };

        return buildAndCommitRenderedServicePath(serviceFunctionTypes,
                serviceFunctionAbstractNames,
                managementIPAddress,
                sfLocatorIps,
                sffLocatorIps,
                rsp,
                sfcName,
                sfpName,
                rspName);
    }

    /**
     * Builds and return a complete RSP Object.
     *
     * @return RSP object
     */
    @SuppressWarnings("serial")
    RenderedServicePath buildAndCommitRenderedServicePath() throws Exception {

        final String[] managementIPAddress = {
            "196.168.55.101",
            "196.168.55.102",
            "196.168.55.103",
            "196.168.55.104",
            "196.168.55.105"
        };

        final String[] serviceFunctionTypes = {
            "firewall",
            "dpi",
            "napt44",
            "http-header-enrichment",
            "qos"
        };

        final String[] serviceFunctionAbstractNames = {
            "firewall",
            "dpi",
            "napt",
            "http-header-enrichment",
            "qos"
        };

        final String sfcName = "unittest-chain-1";
        final String sfpName = "unittest-sfp-1";
        final String rspName = "unittest-rsp-1";

        final String[][] rsp = {
                { "SFF1", "unittest-fw-1" },
                { "SFF2", "unittest-dpi-1" },
                { "SFF3", "unittest-napt-1" },
                { "SFF4", "unittest-http-header-enrichment-1" },
                { "SFF5", "unittest-qos-1" }
        };

        final String[][] sfLocatorIps = {
                {"196.168.55.1"},
                {"196.168.55.2"},
                {"196.168.55.3"},
                {"196.168.55.4"},
                {"196.168.55.5"}
        };

        final String[][] sffLocatorIps = {
                {"196.168.66.101"},
                {"196.168.66.102"},
                {"196.168.66.103"},
                {"196.168.66.104"},
                {"196.168.66.105"}
        };

        return buildAndCommitRenderedServicePath(serviceFunctionTypes,
                serviceFunctionAbstractNames,
                managementIPAddress,
                sfLocatorIps,
                sffLocatorIps,
                rsp,
                sfcName,
                sfpName,
                rspName);
    }

    private RenderedServicePath buildAndCommitRenderedServicePath(String[] serviceFunctionTypes,
                                                                  String[] serviceFunctionAbstractNames,
                                                                  String[] managementIPAddress,
                                                                  String[][] sfLocatorIps,
                                                                  String[][] sffLocatorIps,
                                                                  String[][] rsp,
                                                                  String sfcName,
                                                                  String sfpName,
                                                                  String rspName)
            throws InterruptedException {

        Map<String, ServiceFunction> sfMap = new HashMap<>();
        Map<String, ServiceFunctionForwarder> sffMap = new HashMap<>();

        // for each hop
        for (int i = 0; i < rsp.length; i++) {
            final int pos = i;
            final int numSfs = sfMap.size();
            final String sfName = rsp[pos][1];
            final int numSffs = sffMap.size();
            final String sffName = rsp[pos][0];

            // update service function
            ServiceFunction serviceFunction = sfMap.computeIfAbsent(sfName, (name) -> {
                List<SfDataPlaneLocator> sfDataPlaneLocatorList = Arrays
                        .stream(sfLocatorIps[numSfs])
                        .map(ipStr -> {
                            Ip ip = new IpBuilder()
                                    .setIp(new IpAddress(new Ipv4Address(ipStr)))
                                    .build();
                            return new SfDataPlaneLocatorBuilder()
                                    .setName(new SfDataPlaneLocatorName(ipStr))
                                    .setLocatorType(ip)
                                    .setServiceFunctionForwarder(new SffName(rsp[pos][0]))
                                    .build();
                        })
                        .collect(Collectors.toList());
                return new ServiceFunctionBuilder()
                        .setName(new SfName(name))
                        .setType(new SftTypeName(serviceFunctionTypes[numSfs]))
                        .setIpMgmtAddress(new IpAddress(new Ipv4Address(managementIPAddress[0])))
                        .setSfDataPlaneLocator(sfDataPlaneLocatorList)
                        .build();
            });
            assertTrue(SfcDataStoreAPI.writeMergeTransactionAPI(
                    SfcInstanceIdentifiers.SF_IID.child(ServiceFunction.class, serviceFunction.getKey()),
                    serviceFunction,
                    LogicalDatastoreType.CONFIGURATION));

            // update service function forwarder
            ServiceFunctionForwarder serviceFunctionForwarder = sffMap.computeIfAbsent(sffName, (name) -> {
                List<SffDataPlaneLocator> sffDataPlaneLocatorList = Arrays
                        .stream(sffLocatorIps[numSffs])
                        .map(ipStr -> {
                            Ip ip = new IpBuilder()
                                    .setIp(new IpAddress(new Ipv4Address(ipStr)))
                                    .build();
                            DataPlaneLocator locator = new DataPlaneLocatorBuilder()
                                    .setLocatorType(ip)
                                    .setTransport(VxlanGpe.class)
                                    .build();
                            return new SffDataPlaneLocatorBuilder()
                                    .setName(new SffDataPlaneLocatorName(ipStr))
                                    .setDataPlaneLocator(locator)
                                    .build();
                        })
                        .collect(Collectors.toList());
                return new ServiceFunctionForwarderBuilder()
                        .setName(new SffName(name))
                        .setSffDataPlaneLocator(sffDataPlaneLocatorList)
                        .setServiceFunctionDictionary(new ArrayList<>())
                        .setConnectedSffDictionary(new ArrayList<>())
                        .setIpMgmtAddress(new IpAddress(new Ipv4Address(managementIPAddress[pos])))
                        .setServiceNode(new SnName(sffName))
                        .build();
            });
            ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder;
            serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(serviceFunctionForwarder);

            // update SFF-SFF dictionary
            List<ConnectedSffDictionary> sffDictionaryList;
            sffDictionaryList = new ArrayList<>(serviceFunctionForwarderBuilder.getConnectedSffDictionary());
            // previous hop sff
            if (i > 0 && !Objects.equals(rsp[i - 1][0], sffName)) {
                ConnectedSffDictionary prevSff = new ConnectedSffDictionaryBuilder()
                        .setName(new SffName(rsp[i - 1][0]))
                        .build();
                if (!sffDictionaryList.contains(prevSff)) {
                    sffDictionaryList.add(prevSff);
                }
            }
            // next hop sff
            if (i < rsp.length - 1 && !Objects.equals(rsp[i + 1][0], sffName)) {
                ConnectedSffDictionary nextSff = new ConnectedSffDictionaryBuilder()
                        .setName(new SffName(rsp[i + 1][0]))
                        .build();
                if (!sffDictionaryList.contains(nextSff)) {
                    sffDictionaryList.add(nextSff);
                }
            }

            // update SF dictionary
            List<ServiceFunctionDictionary> sfDictionaryList;
            sfDictionaryList = serviceFunctionForwarderBuilder.getServiceFunctionDictionary();
            SffSfDataPlaneLocator sffSfDataPlaneLocator = new SffSfDataPlaneLocatorBuilder()
                    .setSfDplName(
                            serviceFunction.getSfDataPlaneLocator().stream()
                                    .filter(sfDpl -> sfDpl.getServiceFunctionForwarder().getValue().equals(sffName))
                                    .map(SfDataPlaneLocator::getName)
                                    .findAny()
                                    .orElse(null))
                    .setSffDplName(
                            serviceFunctionForwarder.getSffDataPlaneLocator().get(sfDictionaryList.size()).getName())
                    .build();
            ServiceFunctionDictionary sfDictEntry = new ServiceFunctionDictionaryBuilder()
                    .setSffSfDataPlaneLocator(sffSfDataPlaneLocator)
                    .setFailmode(Open.class)
                    .setSffInterfaces(null)
                    .setName(new SfName(sfName))
                    .build();
            if (!sfDictionaryList.contains(sfDictEntry)) {
                sfDictionaryList.add(sfDictEntry);
            }

            // finally update sff in db
            assertTrue(SfcDataStoreAPI.writeMergeTransactionAPI(
                    SfcInstanceIdentifiers.SFF_IID.child(
                            ServiceFunctionForwarder.class,
                            serviceFunctionForwarder.getKey()),
                    serviceFunctionForwarderBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION));
        }

        // add sf types
        sfMap.values().forEach(sf -> assertTrue(SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(sf)));

        // Create Service Function Chain
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < serviceFunctionAbstractNames.length; i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction = sfcSfBuilder.setName(serviceFunctionAbstractNames[i])
                    .setKey(new SfcServiceFunctionKey(serviceFunctionAbstractNames[i]))
                    .setType(new SftTypeName(serviceFunctionTypes[i])).build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(new SfcName(sfcName))
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(true);

        assertTrue(SfcProviderServiceChainAPI.putServiceFunctionChain(sfcBuilder.build()));

        ServiceFunctionChain readServiceFunctionChain;
        readServiceFunctionChain = SfcProviderServiceChainAPI.readServiceFunctionChain(new SfcName(sfcName));

        assertNotNull(readServiceFunctionChain);

        assertEquals("Must be equal", readServiceFunctionChain.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(sfpName)).setServiceChainName(new SfcName(sfcName)).setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        assertTrue(SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath));

        /* Create RenderedServicePath and reverse RenderedServicePath */
        RenderedServicePath renderedServicePath = null;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(rspName);
        createRenderedPathInputBuilder.setSymmetric(serviceFunctionPath.isSymmetric());
        renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());
        assertNotNull("Must be not null", renderedServicePath);
        return renderedServicePath;
    }
}

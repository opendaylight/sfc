/*
 * Copyright (c) 2016 Ericsson Spain and others. All rights reserved.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersBuilder;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

/**
 * Test Suite to test the ServiceFunctionForwarderListener class.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 */
public class ServiceFunctionForwarderListenerTest extends AbstractDataStoreManager {
    private final Collection<DataTreeModification<ServiceFunctionForwarder>> collection = new ArrayList<>();
    private DataTreeModification<ServiceFunctionForwarder> dataTreeModification;
    private DataObjectModification<ServiceFunctionForwarder> dataObjectModification;

    // Class under test
    private ServiceFunctionForwarderListener serviceFunctionForwarderListener;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        setupSfc();
        dataTreeModification = mock(DataTreeModification.class);
        dataObjectModification = mock(DataObjectModification.class);
        serviceFunctionForwarderListener = new ServiceFunctionForwarderListener(getDataBroker());
        serviceFunctionForwarderListener.init();
    }

    @After
    public void after() throws Exception {
        serviceFunctionForwarderListener.close();
        close();
    }

    /**
     * Test that creates a Service Forwarder Function, calls listener
     * explicitly, verify that Service Function Forwarder was created and cleans
     * up
     */
    @Test
    public void testOnServiceFunctionForwarderCreated() throws Exception {
        ServiceFunctionForwarder serviceFunctionForwarder = build_service_function_forwarder();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.WRITE);
        when(dataObjectModification.getDataBefore()).thenReturn(null);
        when(dataObjectModification.getDataAfter()).thenReturn(serviceFunctionForwarder);

        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
    }

    /**
     * Test that removes a Service Function Forwarder, calls listener
     * explicitly, verify that the Service Function Forwarder was removed and
     * cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderRemoved() throws Exception {
        ServiceFunctionForwarder serviceFunctionForwarder = build_service_function_forwarder();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.DELETE);
        when(dataObjectModification.getDataBefore()).thenReturn(serviceFunctionForwarder);

        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
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
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SFF used by the RSP
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder serviceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(serviceFunctionForwarder);

        // Now we prepare to remove the entry through the listener
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.DELETE);
        when(dataObjectModification.getDataBefore()).thenReturn(serviceFunctionForwarder);

        // The listener will remove the Service Function Forwarder Entry
        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
        Thread.sleep(2000);
        // Verify that RSP was removed
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that RSP was removed
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNull(sffServicePathList);

        // Clean up

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
    public void testOnServiceFunctionForwarderUpdated_UpdateIpMgmt() throws Exception {
        String UPDATED_IP_MGMT_ADDRESS = "196.168.55.110";
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SFF used by the RSP
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. We change the management address
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(
                originalServiceFunctionForwarder);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(UPDATED_IP_MGMT_ADDRESS));
        updatedServiceFunctionForwarderBuilder.setIpMgmtAddress(updatedIpMgmtAddress);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // Now we prepare to update the entry through the listener
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionForwarder);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionForwarder);

        // The listener will remove the Original Service Function Forwarder
        // Entry and associated RSPs
        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNull(sffServicePathList);
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will trigger a more complete
     * code coverage within the listener.
     * In order to simulate a removal from the data store this test does the following:
     * - Create RSP
     * - Update the Service node used by the SFF by setting it to null, which should cause the RSP to be deleted.
     * - creates a IID and add to removedPaths data structure. This IID points to the SFF objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdated_UpdateServiceNode() throws Exception {
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Get the original SFF
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the Service Node
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder =
                new ServiceFunctionForwarderBuilder(originalServiceFunctionForwarder);
        updatedServiceFunctionForwarderBuilder.setServiceNode(null);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // Now we prepare to update the entry through the listener
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionForwarder);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionForwarder);

        // The listener will remove the Original Service Function Forwarder
        // Entry and associated RSPs
        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNull(sffServicePathList);

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will trigger a more complete
     * code coverage within the listener.
     * In order to simulate a removal from the data store this test does the following:
     * - Create RSP
     * - Update the SfDictionary by adding a new SF. The RSP should NOT be deleted.
     * - creates a IID and add to removedPaths data structure. This IID points to the SFF objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Update the SfDictionary by removing the new SF. The RSP should NOT be deleted.
     * - Call listener explicitly.
     * - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdated_UpdateAddAndRemoveSfDict() throws Exception {
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the SffSfDictionary
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder =
                new ServiceFunctionForwarderBuilder(originalServiceFunctionForwarder);
        addSfToSfDict(updatedServiceFunctionForwarderBuilder);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // Now we prepare to update the entry through the listener
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionForwarder);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionForwarder);

        // The listener will NOT remove the RSP
        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNotNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was NOT removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNotNull(sffServicePathList);

        // Now we remove the added unused dictionary
        updatedServiceFunctionForwarder = originalServiceFunctionForwarder;
        originalServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionForwarder);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionForwarder);

        // The listener will NOT remove the RSP
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNotNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was NOT removed
        sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNotNull(sffServicePathList);

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will trigger a more complete
     * code coverage within the listener.
     * In order to simulate a removal from the data store this test does the following:
     * - Create RSP
     * - Update the SfDictionary by adding a removing a used SF dictionary. The RSP should  be deleted.
     * - creates a IID and add to removedPaths data structure. This IID points to the SFF objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdated_UpdateRemoveUsedSfDict() throws Exception {
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the SffSfDictionary
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder =
                new ServiceFunctionForwarderBuilder(originalServiceFunctionForwarder);
        updatedServiceFunctionForwarderBuilder.setServiceFunctionDictionary(Collections.emptyList());
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // Now we prepare to update the entry through the listener
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionForwarder);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionForwarder);

        // The listener will remove the RSP
        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNull(sffServicePathList);

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will trigger a more complete
     * code coverage within the listener.
     * In order to simulate a removal from the data store this test does the following:
     * - Create RSP
     * - Update the SFF DPL by removing an entry, which should cause the RSP to be deleted.
     * - creates a IID and add to removedPaths data structure. This IID points to the SFF objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Cleans up
     */
    @Test
    public void testOnServiceFunctionForwarderUpdated_UpdateSffDpl() throws Exception {
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder originalServiceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        assertNotNull(originalServiceFunctionForwarder);

        // Now we prepare the updated data. Change the DataPlaneLocator
        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder =
                new ServiceFunctionForwarderBuilder(originalServiceFunctionForwarder);
        removeSffDpl(updatedServiceFunctionForwarderBuilder);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();

        // Now we prepare to update the entry through the listener
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionForwarder);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionForwarder);

        // The listener will remove the RSP
        collection.add(dataTreeModification);
        serviceFunctionForwarderListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNull(sffServicePathList);
        /*
         * for (SffServicePath sffServicePath : sffServicePathList) {
         * assertNotEquals(sffServicePath.getName(), renderedServicePath.getName());
         * }
         */

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    private void addSfToSfDict(ServiceFunctionForwarderBuilder sffBuilder) {
        List<ServiceFunctionDictionary> sffSfDict = sffBuilder.getServiceFunctionDictionary();
        List<ServiceFunctionDictionary> newSffSfDict = new ArrayList<>();

        // First create a builder by copying the existing entries
        for(ServiceFunctionDictionary sffSfDictEntry : sffSfDict) {
            ServiceFunctionDictionaryBuilder sffSfDictBuilder = new ServiceFunctionDictionaryBuilder();
            sffSfDictBuilder.setName(sffSfDictEntry.getName())
            .setKey(sffSfDictEntry.getKey())
            .setSffSfDataPlaneLocator(sffSfDictEntry.getSffSfDataPlaneLocator())
            .setFailmode(Open.class)
            .setSffInterfaces(null);
            newSffSfDict.add(sffSfDictBuilder.build());
        }

        // Now add another entry
        String newName = "NEW NAME";
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder.setSfDplName(new SfDataPlaneLocatorName(newName))
        .setSffDplName(new SffDataPlaneLocatorName(newName));
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder.setName(new SfName(newName))
        .setKey(new ServiceFunctionDictionaryKey(new SfName(newName)))
        .setSffSfDataPlaneLocator(sffSfDataPlaneLocatorBuilder.build())
        .setFailmode(Open.class)
        .setSffInterfaces(null);
        newSffSfDict.add(dictionaryEntryBuilder.build());

        sffBuilder.setServiceFunctionDictionary(newSffSfDict);
    }

    private void removeSffDpl(ServiceFunctionForwarderBuilder sffBuilder) {
        List<SffDataPlaneLocator> sffDplList = sffBuilder.getSffDataPlaneLocator();
        List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        // We want to remove a DPL entry, so just copy all but the last one
        for(int i = 0; i < sffDplList.size()-1; i++) {
            SffDataPlaneLocator sffDpl = sffDplList.get(i);
            SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder(sffDpl);
            locatorList.add(locatorBuilder.build());
        }

        sffBuilder.setSffDataPlaneLocator(locatorList);
    }

    /**
     * Builds a complete service Function Object
     *
     * @return ServiceFunction object
     */
    @SuppressWarnings("serial")
    ServiceFunctionForwarder build_service_function_forwarder() {
        SffName name = new SffName("SFF1");
        List<SfName> sfNames = new ArrayList<SfName>() {

            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-dpi-1"));
                add(new SfName("unittest-napt-1"));
                add(new SfName("unittest-http-header-enrichment-1"));
                add(new SfName("unittest-qos-1"));
            }
        };
        IpAddress[] ipMgmtAddress = new IpAddress[sfNames.size()];

        List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address("10.1.1.101"))).setPort(new PortNumber(555));

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        locatorBuilder.setName(new SffDataPlaneLocatorName("locator-1"))
                .setKey(new SffDataPlaneLocatorKey(new SffDataPlaneLocatorName("locator-1")))
                .setDataPlaneLocator(sffLocatorBuilder.build());

        locatorList.add(locatorBuilder.build());

        List<ServiceFunctionDictionary> dictionary = new ArrayList<>();

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
     * Builds and return a complete RSP Object
     *
     * @return RSP object
     */
    @SuppressWarnings("serial")
    RenderedServicePath build_and_commit_rendered_service_path() throws Exception {

        List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {

            {
                add("196.168.55.1");
                add("196.168.55.2");
                add("196.168.55.3");
                add("196.168.55.4");
                add("196.168.55.5");
            }
        };

        List<String> IP_MGMT_ADDRESS = new ArrayList<String>() {

            {
                add("196.168.55.101");
                add("196.168.55.102");
                add("196.168.55.103");
                add("196.168.55.104");
                add("196.168.55.105");
            }
        };

        List<Integer> PORT = new ArrayList<Integer>() {

            {
                add(1111);
                add(2222);
                add(3333);
                add(4444);
                add(5555);
            }
        };

        List<SftTypeName> sfTypes = new ArrayList<SftTypeName>() {

            {
                add(new SftTypeName("firewall"));
                add(new SftTypeName("dpi"));
                add(new SftTypeName("napt44"));
                add(new SftTypeName("http-header-enrichment"));
                add(new SftTypeName("qos"));
            }
        };

        List<String> SF_ABSTRACT_NAMES = new ArrayList<String>() {

            {
                add("firewall");
                add("dpi");
                add("napt");
                add("http-header-enrichment");
                add("qos");
            }
        };

        SfcName SFC_NAME = new SfcName("unittest-chain-1");
        SfpName SFP_NAME = new SfpName("unittest-sfp-1");
        RspName RSP_NAME = new RspName("unittest-rsp-1");

        List<SfName> sfNames = new ArrayList<SfName>() {

            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-dpi-1"));
                add(new SfName("unittest-napt-1"));
                add(new SfName("unittest-http-header-enrichment-1"));
                add(new SfName("unittest-qos-1"));
            }
        };

        List<SffName> SFF_NAMES = new ArrayList<SffName>() {

            {
                add(new SffName("SFF1"));
                add(new SffName("SFF2"));
                add(new SffName("SFF3"));
                add(new SffName("SFF4"));
                add(new SffName("SFF5"));
            }
        };

        String[][] TO_SFF_NAMES = { { "SFF2", "SFF5" }, { "SFF3", "SFF1" }, { "SFF4", "SFF2" }, { "SFF5", "SFF3" },
                { "SFF1", "SFF4" } };

        List<String> SFF_LOCATOR_IP = new ArrayList<String>() {

            {
                add("196.168.66.101");
                add("196.168.66.102");
                add("196.168.66.103");
                add("196.168.66.104");
                add("196.168.66.105");
            }
        };

        List<ServiceFunction> sfList = new ArrayList<>();
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();

        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames.size()];
        final IpAddress[] locatorIpAddress = new IpAddress[sfNames.size()];
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[sfNames.size()];
        ServiceFunctionKey[] key = new ServiceFunctionKey[sfNames.size()];
        for (int i = 0; i < sfNames.size(); i++) {
            ipMgmtAddress[i] = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(0)));
            locatorIpAddress[i] = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(0)));
            PortNumber portNumber = new PortNumber(PORT.get(i));
            key[i] = new ServiceFunctionKey(new SfName(sfNames.get(i)));

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(i)))
                    .setLocatorType(ipBuilder.build()).setServiceFunctionForwarder(new SffName(SFF_NAMES.get(i)));
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(new SfName(sfNames.get(i))).setKey(key[i]).setType(sfTypes.get(i))
                    .setIpMgmtAddress(ipMgmtAddress[i]).setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        assertTrue(SfcDataStoreAPI.writePutTransactionAPI(SfcInstanceIdentifiers.SF_IID, sfsBuilder.build(),
                LogicalDatastoreType.CONFIGURATION));
        // executor.submit(SfcProviderServiceFunctionAPI.getPutAll(new
        // Object[]{sfsBuilder.build()},
        // new Class[]{ServiceFunctions.class})).get();
        Thread.sleep(1000); // Wait they are really created

        // Create ServiceFunctionTypeEntry for all ServiceFunctions
        for (ServiceFunction serviceFunction : sfList) {
            assertTrue(SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction));
        }

        // Create Service Function Forwarders
        for (int i = 0; i < SFF_NAMES.size(); i++) {
            // ServiceFunctionForwarders connected to SFF_NAMES[i]
            List<ConnectedSffDictionary> sffDictionaryList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                ConnectedSffDictionaryBuilder sffDictionaryEntryBuilder = new ConnectedSffDictionaryBuilder();
                ConnectedSffDictionary sffDictEntry = sffDictionaryEntryBuilder.setName(new SffName(TO_SFF_NAMES[i][j]))
                        .build();
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
                    .setSffSfDataPlaneLocator(sffSfDataPlaneLocator).setFailmode(Open.class).setSffInterfaces(null);
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
            sffBuilder.setName(new SffName(SFF_NAMES.get(i)))
                    .setKey(new ServiceFunctionForwarderKey(new SffName(SFF_NAMES.get(i))))
                    .setSffDataPlaneLocator(locatorList)
                    .setServiceFunctionDictionary(sfDictionaryList)
                    .setConnectedSffDictionary(sffDictionaryList)
                    .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(i))))
                    .setServiceNode(new SnName(SFF_NAMES.get(i).getValue()));
            ServiceFunctionForwarder sff = sffBuilder.build();
            sffList.add(sff);
        }
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        serviceFunctionForwardersBuilder.setServiceFunctionForwarder(sffList);
        assertTrue(SfcDataStoreAPI.writePutTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                serviceFunctionForwardersBuilder.build(), LogicalDatastoreType.CONFIGURATION));
        // Create Service Function Chain
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(SFC_NAME);
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < SF_ABSTRACT_NAMES.size(); i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction = sfcSfBuilder.setName(SF_ABSTRACT_NAMES.get(i))
                    .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES.get(i))).setType(sfTypes.get(i)).build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(SFC_NAME).setKey(sfcKey).setSfcServiceFunction(sfcServiceFunctionList).setSymmetric(true);

        assertTrue(SfcProviderServiceChainAPI.putServiceFunctionChain(sfcBuilder.build()));

        Thread.sleep(1000); // Wait SFC is really crated

        ServiceFunctionChain readServiceFunctionChain = SfcProviderServiceChainAPI.readServiceFunctionChain(SFC_NAME);

        assertNotNull(readServiceFunctionChain);

        assertEquals("Must be equal", readServiceFunctionChain.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFP_NAME)).setServiceChainName(SFC_NAME).setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        assertTrue(SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath));

        Thread.sleep(1000); // Wait they are really created

        /* Create RenderedServicePath and reverse RenderedServicePath */
        RenderedServicePath renderedServicePath = null;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue());
        createRenderedPathInputBuilder.setSymmetric(serviceFunctionPath.isSymmetric());
        try {
            renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                    createRenderedPathInputBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull("Must be not null", renderedServicePath);
        return renderedServicePath;
    }
}

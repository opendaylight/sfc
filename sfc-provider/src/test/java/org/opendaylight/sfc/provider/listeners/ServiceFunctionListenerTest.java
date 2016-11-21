/*
 * Copyright (c) 2016 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
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
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.TenantId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;

/**
 * Test Suite to test the ServiceFunctionListener class.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 */
public class ServiceFunctionListenerTest extends AbstractDataStoreManager {
    private final Collection<DataTreeModification<ServiceFunction>> collection = new ArrayList<>();
    private DataTreeModification<ServiceFunction> dataTreeModification;
    private DataObjectModification<ServiceFunction> dataObjectModification;

    // Class under test
    private ServiceFunctionListener serviceFunctionListener;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        setupSfc();
        dataTreeModification = mock(DataTreeModification.class);
        dataObjectModification = mock(DataObjectModification.class);
        serviceFunctionListener = new ServiceFunctionListener(getDataBroker());
        serviceFunctionListener.init();
    }

    @After
    public void after() throws Exception {
        serviceFunctionListener.close();
        close();
    }

    /**
     * Test that creates a Service Function, calls listener explicitly, verify
     * that Service Function Type was created and cleans up
     */
    @Test
    public void testOnServiceFunctionCreated() throws Exception {
        ServiceFunction serviceFunction = build_service_function();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.WRITE);
        when(dataObjectModification.getDataBefore()).thenReturn(null);
        when(dataObjectModification.getDataAfter()).thenReturn(serviceFunction);

        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);

        Thread.sleep(500);
        SftServiceFunctionName sftServiceFunctionName = SfcProviderServiceTypeAPI
                .readServiceFunctionTypeEntry(serviceFunction);
        assertNotNull(sftServiceFunctionName);
        assertEquals(sftServiceFunctionName.getName().getValue(), serviceFunction.getName().getValue());
        // Clean up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(serviceFunction));
    }

    /**
     * Test that removes a Service Function, calls listener explicitly, verify
     * that the Service Function Type was removed and cleans up
     */
    @Test
    public void testOnServiceFunctionRemoved() throws Exception {
        ServiceFunction serviceFunction = build_service_function();

        // First we create a Service Function Type Entry
        assertTrue(SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction));

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.DELETE);
        when(dataObjectModification.getDataBefore()).thenReturn(serviceFunction);

        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);

        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(serviceFunction));
    }

    /**
     * Test that updates a Service Function, creates the SF object and commits
     * to data store, creates a copy of the original SF and updates the type and
     * management address, then feeds the original and updated SFs to the
     * listener; finally asserts that the listener has removed the original and
     * created a new entry
     */
    @Test
    public void testOnServiceFunctionUpdated() throws Exception {
        String UPDATED_IP_MGMT_ADDRESS = "196.168.55.102";
        ServiceFunction originalServiceFunction = build_service_function();

        assertTrue(SfcProviderServiceFunctionAPI.putServiceFunction(originalServiceFunction));

        // Now we prepare the updated data. We change mgmt address and type
        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(UPDATED_IP_MGMT_ADDRESS));
        SftTypeName updatedType = new SftTypeName("dpi");
        updatedServiceFunctionBuilder.setIpMgmtAddress(updatedIpMgmtAddress).setType(updatedType);
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunction);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunction);

        // The listener will remove the Original Service Function Type Entry and
        // create a new one with the new type
        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));
        // Clean-up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
    }

    /**
     * In this test we create a RSP and remove a SF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Remove first SF used by RSP by explicitly calling the
     * listener - creates a IID and add to removedPaths data structure. This IID
     * points to the SF objects stored in the original data - Call listener
     * explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionRemovedWithRSP() throws Exception {
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP
        SfName sfName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionName();
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        assertNotNull(serviceFunction);

        // Now we prepare to remove the entry through the listener
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.DELETE);
        when(dataObjectModification.getDataBefore()).thenReturn(serviceFunction);

        // The listener will remove the Service Function Type Entry
        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));
        List<RspName> rspNameList = SfcProviderServiceFunctionAPI.getRspsBySfName(serviceFunction.getName());
        if (rspNameList != null) {
            for (RspName rspName : rspNameList) {
                assertNotEquals(rspName, renderedServicePath.getName());
            }
        }

        // Cleanup
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        // Dont remove the SF, it was removed in serviceFunctionListener.onDataTreeChanged()
        //assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update first SF used by RSP by explicitly calling the
     * listener - creates a IID and add to removedPaths data structure. This IID
     * points to the SF objects stored in the original data - Call listener
     * explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionUpdatedWithRSP_updateIpMgmt() throws Exception {
        String UPDATED_IP_MGMT_ADDRESS = "196.168.55.112";
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to update the first SF used by the RSP. SfName sfName =
        SfName sfName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionName();
        ServiceFunction originalServiceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        assertNotNull(originalServiceFunction);

        // Now we prepare the updated data. We change management address and type
        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(UPDATED_IP_MGMT_ADDRESS));
        updatedServiceFunctionBuilder.setIpMgmtAddress(updatedIpMgmtAddress);
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunction);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunction);

        // The listener will remove the Original Service Function Type Entry and
        // create a new one with the new type
        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        // The original SF type is only deleted if the SF type changes
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Clean-up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));
        List<RspName> rspNameList = SfcProviderServiceFunctionAPI.getRspsBySfName(originalServiceFunction.getName());
        if (rspNameList != null) {
            for (RspName rspName : rspNameList) {
                assertNotEquals(rspName, renderedServicePath.getName());
            }
        }

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        //assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update first SF used by RSP by explicitly calling the
     * listener - creates a IID and add to removedPaths data structure. This IID
     * points to the SF objects stored in the original data - Call listener
     * explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionUpdatedWithRSP_UpdateSfType() throws Exception {
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to update the first SF used by the RSP. SfName sfName =
        SfName sfName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionName();
        ServiceFunction originalServiceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        assertNotNull(originalServiceFunction);

        // Now we prepare the updated data. We change management address and type
        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        updatedServiceFunctionBuilder.setType(new SftTypeName("dpi"));
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunction);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunction);

        // The listener will remove the Original Service Function Type Entry and
        // create a new one with the new type
        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Clean-up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));
        List<RspName> rspNameList = SfcProviderServiceFunctionAPI.getRspsBySfName(originalServiceFunction.getName());
        if (rspNameList != null) {
            for (RspName rspName : rspNameList) {
                assertNotEquals(rspName, renderedServicePath.getName());
            }
        }

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        //assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update first SF used by RSP by explicitly calling the
     * listener - creates a IID and add to removedPaths data structure. This IID
     * points to the SF objects stored in the original data - Call listener
     * explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionUpdatedWithRSP_UpdateDpl() throws Exception {
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to update the first SF used by the RSP. SfName sfName =
        SfName sfName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionName();
        ServiceFunction originalServiceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        assertNotNull(originalServiceFunction);

        // Now we prepare the updated data. We change management address and type
        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        removeSfDpl(updatedServiceFunctionBuilder);
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunction);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunction);

        // The listener will remove the Original Service Function Type Entry and
        // create a new one with the new type
        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        // The original SF type is only deleted if the SF type changes
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Clean-up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));
        List<RspName> rspNameList = SfcProviderServiceFunctionAPI.getRspsBySfName(originalServiceFunction.getName());
        if (rspNameList != null) {
            for (RspName rspName : rspNameList) {
                assertNotEquals(rspName, renderedServicePath.getName());
            }
        }

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        //assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * In this test we create a RSP and update a SF used by it. This will
     * trigger a more complete code coverage within the listener. In order to
     * simulate a removal from the data store this test does the following: -
     * Create RSP - Update first SF used by RSP by explicitly calling the
     * listener - creates a IID and add to removedPaths data structure. This IID
     * points to the SF objects stored in the original data - Call listener
     * explicitly. - Cleans up
     */
    @Test
    public void testOnServiceFunctionUpdatedWithRSP_UpdateNoDeleteRsp() throws Exception {
        // Build the RSP in which the SF is included
        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to update the first SF used by the RSP. SfName sfName =
        SfName sfName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionName();
        ServiceFunction originalServiceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        assertNotNull(originalServiceFunction);

        // Now we prepare the updated data. Neither of these changes should trigger an RSP deletion
        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        updatedServiceFunctionBuilder
            .setTenantId(new TenantId("EMPTY"))
            .setRestUri(new Uri("EMPTY"));
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunction);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunction);

        // The listener will NOT remove anything
        collection.add(dataTreeModification);
        serviceFunctionListener.onDataTreeChanged(collection);
        Thread.sleep(500);
        // The original SF type is only deleted if the SF type changes
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));
        assertNotNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Clean-up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));
        assertTrue(SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName()));

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        //assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(SfcInstanceIdentifiers.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    private void removeSfDpl(ServiceFunctionBuilder sfBuilder) {
        List<SfDataPlaneLocator> sfDplList = sfBuilder.getSfDataPlaneLocator();
        List<SfDataPlaneLocator> locatorList = new ArrayList<>();

        // We want to remove a DPL entry, so just copy all but the last one
        for(int i = 0; i < sfDplList.size()-1; i++) {
            SfDataPlaneLocator sfDpl = sfDplList.get(i);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder(sfDpl);
            locatorList.add(locatorBuilder.build());
        }

        sfBuilder.setSfDataPlaneLocator(locatorList);
    }

    /**
     * Builds a complete service Function Object
     *
     * @return ServiceFunction object
     */
    ServiceFunction build_service_function() {

        List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("196.168.55.1");
                add("196.168.55.2");
                add("196.168.55.3");
            }
        };

        List<String> IP_MGMT_ADDRESS = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("196.168.55.101");
                add("196.168.55.102");
                add("196.168.55.103");
            }
        };
        String SF_NAME = "listernerSF";
        int PORT = 555;
        SftTypeName type = new SftTypeName("firewall");
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(0)));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(new SfName(SF_NAME));

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(0)));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(0))).setLocatorType(ipBuilder.build());
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(new SfName(SF_NAME)).setKey(key).setType(type).setIpMgmtAddress(ipMgmtAddress)
                .setSfDataPlaneLocator(dataPlaneLocatorList);

        ServiceFunction serviceFunction = sfBuilder.build();
        return serviceFunction;
    }

    /**
     * Builds and return a complete RSP Object
     *
     * @return RSP object
     */
    RenderedServicePath build_and_commit_rendered_service_path() throws Exception {
        List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("196.168.55.1");
                add("196.168.55.2");
                add("196.168.55.3");
                add("196.168.55.4");
                add("196.168.55.5");
            }
        };

        List<String> IP_MGMT_ADDRESS = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("196.168.55.101");
                add("196.168.55.102");
                add("196.168.55.103");
                add("196.168.55.104");
                add("196.168.55.105");
            }
        };

        List<Integer> PORT = new ArrayList<Integer>() {
            private static final long serialVersionUID = 1L;

            {
                add(1111);
                add(2222);
                add(3333);
                add(4444);
                add(5555);
            }
        };

        List<SftTypeName> sfTypes = new ArrayList<SftTypeName>() {
            private static final long serialVersionUID = 1L;

            {
                add(new SftTypeName("firewall"));
                add(new SftTypeName("dpi"));
                add(new SftTypeName("napt44"));
                add(new SftTypeName("http-header-enrichment"));
                add(new SftTypeName("qos"));
            }
        };

        List<String> SF_ABSTRACT_NAMES = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {
                add("firewall");
                add("dpi");
                add("napt");
                add("http-header-enrichment");
                add("qos");
            }
        };

        String SFC_NAME = "unittest-chain-1";
        String SFP_NAME = "unittest-sfp-1";
        String RSP_NAME = "unittest-rsp-1";

        List<SfName> sfNames = new ArrayList<SfName>() {
            private static final long serialVersionUID = 1L;

            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-dpi-1"));
                add(new SfName("unittest-napt-1"));
                add(new SfName("unittest-http-header-enrichment-1"));
                add(new SfName("unittest-qos-1"));
            }
        };

        List<SffName> SFF_NAMES = new ArrayList<SffName>() {
            private static final long serialVersionUID = 1L;

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
            private static final long serialVersionUID = 1L;

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
                    .setSffDataPlaneLocator(locatorList).setServiceFunctionDictionary(sfDictionaryList)
                    .setConnectedSffDictionary(sffDictionaryList).setServiceNode(null);
            ServiceFunctionForwarder sff = sffBuilder.build();
            sffList.add(sff);
        }
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        serviceFunctionForwardersBuilder.setServiceFunctionForwarder(sffList);
        assertTrue(SfcDataStoreAPI.writePutTransactionAPI(SfcInstanceIdentifiers.SFF_IID,
                serviceFunctionForwardersBuilder.build(), LogicalDatastoreType.CONFIGURATION));
        // Create Service Function Chain
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(new SfcName(SFC_NAME));
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < SF_ABSTRACT_NAMES.size(); i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction = sfcSfBuilder.setName(SF_ABSTRACT_NAMES.get(i))
                    .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES.get(i))).setType(sfTypes.get(i)).build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(new SfcName(SFC_NAME)).setKey(sfcKey).setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(true);

        assertTrue(SfcProviderServiceChainAPI.putServiceFunctionChain(sfcBuilder.build()));
        Thread.sleep(1000); // Wait SFC is really created

        ServiceFunctionChain readServiceFunctionChain = SfcProviderServiceChainAPI
                .readServiceFunctionChain(new SfcName(SFC_NAME));

        assertNotNull(readServiceFunctionChain);

        // assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", readServiceFunctionChain.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFP_NAME)).setServiceChainName(new SfcName(SFC_NAME)).setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        assertTrue(SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath));

        Thread.sleep(1000); // Wait they are really created

        /* Create RenderedServicePath and reverse RenderedServicePath */
        RenderedServicePath renderedServicePath = null;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME);
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

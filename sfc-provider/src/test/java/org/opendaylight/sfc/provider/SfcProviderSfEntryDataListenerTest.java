/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersBuilder;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SfcProviderSfEntryDataListener Tester.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 1.0
 * @since 2015-09-02
 */
public class SfcProviderSfEntryDataListenerTest extends AbstractDataStoreManager {

    private static final SfcProviderSfEntryDataListener sfEntryDataListener = new SfcProviderSfEntryDataListener();
    private static ListenerRegistration<DataChangeListener> sfEntryDataListenerRegistration;

    Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListenerTest.class);

    public ListenerRegistration<DataChangeListener> registerAsDataChangeListener() {
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.SF_ENTRY_IID,
                sfEntryDataListener, DataBroker.DataChangeScope.SUBTREE);
    }

    @Before
    public void before() throws Exception {
        setOdlSfc();
        // sfEntryDataListenerRegistration = registerAsDataChangeListener();
    }

    @After
    public void after() throws Exception {
        // sfEntryDataListenerRegistration.close();
    }

    /**
     * Creates SF object, call listeners explicitly, verify that SF Type was created,
     * cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_CreateData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        ServiceFunction serviceFunction = build_service_function();

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
            .child(ServiceFunction.class, serviceFunction.getKey())
            .build();

        createdData.put(sfEntryIID, serviceFunction);

        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        // Empty MAPs below
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);

        sfEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        SftServiceFunctionName sftServiceFunctionName =
                SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(serviceFunction);
        assertNotNull(sftServiceFunctionName);
        assertEquals(sftServiceFunctionName.getName(), serviceFunction.getName().getValue());
        /* clean up */
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(serviceFunction));
    }

    /**
     * In order to simulate a removal from the data store this test does the following:
     * - creates SF object and inserts it into an MAP data structure representing the original data
     * - creates a IID and add to removedPaths data structure. This IID points to the SF objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_RemoveData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        ServiceFunction serviceFunction = build_service_function();

        /* First we create a Service Function Type Entry */
        assertTrue(SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction));

        /* Now we prepare to remove the entry through the listener */
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
            .child(ServiceFunction.class, serviceFunction.getKey())
            .build();

        originalData.put(sfEntryIID, serviceFunction);
        removedPaths.add(sfEntryIID);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        /* The listener will remove the Service Function Type Entry */
        sfEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(serviceFunction));
    }

    /**
     * In order to simulate an update from the data store this test does the following:
     * - creates SF object and commits to data store
     * - Creates a copy of the original SF and updates the type and management address
     * - Feeds the original and updated SFs to the listener
     * - Asserts that the listener has removed the original and created a new entry
     * - Cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_UpdateSFData() throws Exception {
        String UPDATED_IP_MGMT_ADDRESS = "196.168.55.102";
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        /* Create and commit SF */
        ServiceFunction originalServiceFunction = build_service_function();
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
            .child(ServiceFunction.class, originalServiceFunction.getKey())
            .build();
        assertTrue(SfcProviderServiceFunctionAPI.putServiceFunction(originalServiceFunction));
        /* Since there is no listener we need to create a Service Function Type Entry */
        assertTrue(SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(originalServiceFunction));

        originalData.put(sfEntryIID, originalServiceFunction);

        /* Now we prepare the updated data. We change mgmt address and type */

        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(UPDATED_IP_MGMT_ADDRESS));
        SftType updatedType = new SftType("dpi");
        updatedServiceFunctionBuilder.setIpMgmtAddress(updatedIpMgmtAddress).setType(updatedType);
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();
        updatedData.put(sfEntryIID, updatedServiceFunction);

        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

        /*
         * The listener will remove the Original Service Function Type Entry and create a new one
         * with the new type
         */
        sfEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));

        // Clean-up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));
        assertTrue(SfcProviderServiceFunctionAPI.deleteServiceFunction(originalServiceFunction.getName()));
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));

    }

    /**
     * In this test we create a RSP and remove a SF used by it. This will trigger a more complete
     * code coverage within the listener.
     * In order to simulate a removal from the data store this test does the following:
     * - Create RSP
     * - Remove first SF used by RSP by explicitly calling the listener
     * - creates a IID and add to removedPaths data structure. This IID points to the SF objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_RemoveDataWithRSP() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to remove the first SF used by the RSP.
        SfName sfName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionName();
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        assertNotNull(serviceFunction);

        // Now we prepare to remove the entry through the listener

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
            .child(ServiceFunction.class, serviceFunction.getKey())
            .build();

        originalData.put(sfEntryIID, serviceFunction);
        removedPaths.add(sfEntryIID);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        // The listener will remove the Service Function Type Entry

        sfEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));
        List<RspName> rspNameList = SfcProviderServiceFunctionAPI.getRspsBySfName(serviceFunction.getName());
        if (rspNameList != null) {
            for (RspName rspName : rspNameList) {
                assertNotEquals(rspName, renderedServicePath.getName());
            }
        }

        /* Clean-up */

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFP_IID, LogicalDatastoreType.CONFIGURATION));

    }

    /**
     * In this test we create a RSP and update a SF used by it. This will trigger a more complete
     * code coverage within the listener.
     * In order to simulate a removal from the data store this test does the following:
     * - Create RSP
     * - Update first SF used by RSP by explicitly calling the listener
     * - creates a IID and add to removedPaths data structure. This IID points to the SF objects
     * stored in the
     * original data
     * - Call listener explicitly.
     * - Cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_UpdateSFDataWithRSP() throws Exception {
        String UPDATED_IP_MGMT_ADDRESS = "196.168.55.102";
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        RenderedServicePath renderedServicePath = build_and_commit_rendered_service_path();
        assertNotNull(renderedServicePath);

        // Prepare to update the first SF used by the RSP.
        SfName sfName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionName();
        ServiceFunction originalServiceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        assertNotNull(originalServiceFunction);

        // Now we prepare the updated data. We change mgmt address and type

        // Now we prepare to remove the entry through the listener
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
            .child(ServiceFunction.class, originalServiceFunction.getKey())
            .build();

        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(UPDATED_IP_MGMT_ADDRESS));
        SftType updatedType = new SftType("dpi");
        updatedServiceFunctionBuilder.setIpMgmtAddress(updatedIpMgmtAddress).setType(updatedType);
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();
        updatedData.put(sfEntryIID, updatedServiceFunction);

        originalData.put(sfEntryIID, originalServiceFunction);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

        /*
         * The listener will remove the Original Service Function Type Entry and create a new one
         * with the new type
         */
        sfEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));

        // Clean-up
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));
        List<RspName> rspNameList = SfcProviderServiceFunctionAPI.getRspsBySfName(originalServiceFunction.getName());
        if (rspNameList != null) {
            for (RspName rspName : rspNameList) {
                assertNotEquals(rspName, renderedServicePath.getName());
            }
        }

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFP_IID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     * Builds a complete service Function Object
     *
     * @return ServiceFunction object
     */
    ServiceFunction build_service_function() {
        // TODO re-write as Java ArrayList with correct types.
        String[] LOCATOR_IP_ADDRESS = {"196.168.55.1", "196.168.55.2", "196.168.55.3"};
        String[] IP_MGMT_ADDRESS = {"196.168.55.101", "196.168.55.102", "196.168.55.103"};
        String SF_NAME = "listernerSF";
        int PORT = 555;
        SftType type = new SftType("firewall");
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(new SfName(SF_NAME));

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS[0])).setLocatorType(ipBuilder.build());
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(new SfName(SF_NAME))
            .setKey(key)
            .setType(type)
            .setIpMgmtAddress(ipMgmtAddress)
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
        String[] LOCATOR_IP_ADDRESS = {"196.168.55.1", "196.168.55.2", "196.168.55.3", "196.168.55.4", "196.168.55.5"};
        String[] IP_MGMT_ADDRESS =
                {"196.168.55.101", "196.168.55.102", "196.168.55.103", "196.168.55.104", "196.168.55.105"};
        int[] PORT = {1111, 2222, 3333, 4444, 5555};
        @SuppressWarnings("serial")
        List<SftType> sfTypes = new ArrayList<SftType>() {

            {
                add(new SftType("firewall"));
                add(new SftType("dpi"));
                add(new SftType("napt44"));
                add(new SftType("http-header-enrichment"));
                add(new SftType("qos"));

            }
        };
        String[] SF_ABSTRACT_NAMES = {"firewall", "dpi", "napt", "http-header-enrichment", "qos"};
        String SFC_NAME = "unittest-chain-1";
        String SFP_NAME = "unittest-sfp-1";
        String RSP_NAME = "unittest-rsp-1";
        String[] sfNames = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1", "unittest-http-header-enrichment-1",
                "unittest-qos-1"};
        String[] SFF_NAMES = {"SFF1", "SFF2", "SFF3", "SFF4", "SFF5"};
        String[][] TO_SFF_NAMES =
                {{"SFF2", "SFF5"}, {"SFF3", "SFF1"}, {"SFF4", "SFF2"}, {"SFF5", "SFF3"}, {"SFF1", "SFF4"}};
        String[] SFF_LOCATOR_IP =
                {"196.168.66.101", "196.168.66.102", "196.168.66.103", "196.168.66.104", "196.168.66.105"};
        List<ServiceFunction> sfList = new ArrayList<>();
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();

        final IpAddress[] ipMgmtAddress = new IpAddress[sfNames.length];
        final IpAddress[] locatorIpAddress = new IpAddress[sfNames.length];
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[sfNames.length];
        ServiceFunctionKey[] key = new ServiceFunctionKey[sfNames.length];
        for (int i = 0; i < sfNames.length; i++) {
            ipMgmtAddress[i] = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
            locatorIpAddress[i] = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
            PortNumber portNumber = new PortNumber(PORT[i]);
            key[i] = new ServiceFunctionKey(new SfName(sfNames[i]));

            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS[i]))
                .setLocatorType(ipBuilder.build())
                .setServiceFunctionForwarder(new SffName(SFF_NAMES[i]));
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(new SfName(sfNames[i]))
                .setKey(key[i])
                .setType(sfTypes.get(i))
                .setIpMgmtAddress(ipMgmtAddress[i])
                .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        assertTrue(SfcProviderServiceFunctionAPI.putAllServiceFunctions(sfsBuilder.build()));
        // executor.submit(SfcProviderServiceFunctionAPI.getPutAll(new Object[]{sfsBuilder.build()},
        // new Class[]{ServiceFunctions.class})).get();
        Thread.sleep(1000); // Wait they are really created

        // Create ServiceFunctionTypeEntry for all ServiceFunctions
        for (ServiceFunction serviceFunction : sfList) {
            assertTrue(SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction));
        }

        // Create Service Function Forwarders
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
            ipBuilder.setIp(new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[i]))).setPort(new PortNumber(PORT[i]));
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
            sffList.add(sff);
            // assertTrue(SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(sff));
            // executor.submit(SfcProviderServiceForwarderAPI.getPut(new Object[]{sff}, new
            // Class[]{ServiceFunctionForwarder.class})).get();
        }
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        serviceFunctionForwardersBuilder.setServiceFunctionForwarder(sffList);
        assertTrue(SfcProviderServiceForwarderAPI
            .putAllServiceFunctionForwarders(serviceFunctionForwardersBuilder.build()));

        // Create Service Function Chain
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(new SfcName(SFC_NAME));
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < SF_ABSTRACT_NAMES.length; i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction = sfcSfBuilder.setName(SF_ABSTRACT_NAMES[i])
                .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES[i]))
                .setType(sfTypes.get(i))
                .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(new SfcName(SFC_NAME))
            .setKey(sfcKey)
            .setSfcServiceFunction(sfcServiceFunctionList)
            .setSymmetric(true);

        // Object[] parameters = {sfcBuilder.build()};
        // Class[] parameterTypes = {ServiceFunctionChain.class};

        assertTrue(SfcProviderServiceChainAPI.putServiceFunctionChain(sfcBuilder.build()));
        // executor.submit(SfcProviderServiceChainAPI
        // .getPut(parameters, parameterTypes)).get();
        Thread.sleep(1000); // Wait SFC is really crated

        // Check if Service Function Chain was created
        // Object[] parameters2 = {SFC_NAME};
        // Class[] parameterTypes2 = {String.class};
        // Object result = executor.submit(SfcProviderServiceChainAPI
        // .getRead(parameters2, parameterTypes2)).get();
        // ServiceFunctionChain sfc2 = (ServiceFunctionChain) result;

        ServiceFunctionChain readServiceFunctionChain =
                SfcProviderServiceChainAPI.readServiceFunctionChain(new SfcName(SFC_NAME));

        assertNotNull(readServiceFunctionChain);

        // assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", readServiceFunctionChain.getSfcServiceFunction(), sfcServiceFunctionList);

        /* Create ServiceFunctionPath */
        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFP_NAME)).setServiceChainName(new SfcName(SFC_NAME)).setSymmetric(true);
        ServiceFunctionPath serviceFunctionPath = pathBuilder.build();
        assertNotNull("Must be not null", serviceFunctionPath);
        assertTrue(SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath));
        // assertTrue("Must be true", ret);

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

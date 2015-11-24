/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import static org.junit.Assert.assertEquals;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorKey;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.HttpHeaderEnrichment;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Qos;
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
 * @since 2015-09-10
 */
public class SfcProviderSffEntryDataListenerTest extends AbstractDataStoreManager {

    private static final SfcProviderSffEntryDataListener sffEntryDataListener = new SfcProviderSffEntryDataListener();
    private static ListenerRegistration<DataChangeListener> sffEntryDataListenerRegistration;

    Logger LOG = LoggerFactory.getLogger(SfcProviderSffEntryDataListenerTest.class);

    @Before
    public void before() throws Exception {
        setOdlSfc();
    }

    @After
    public void after() throws Exception {}

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

        ServiceFunctionForwarder serviceFunctionForwarder = build_service_function_forwarder();

        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarder.getKey())
                    .build();

        createdData.put(sffEntryIID, serviceFunctionForwarder);

        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        // Empty MAPs below
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);

        sffEntryDataListener.onDataChanged(dataChangeEvent);
    }

    /**
     * In order to simulate a removal from the data store this test does the following:
     * - creates SFF object and inserts it into an MAP data structure representing the original data
     * - creates a IID and add to removedPaths data structure. This IID points to the SFF objects
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

        ServiceFunctionForwarder serviceFunctionForwarder = build_service_function_forwarder();

        /* Now we prepare to remove the entry through the listener */
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarder.getKey())
                    .build();

        originalData.put(sffEntryIID, serviceFunctionForwarder);
        removedPaths.add(sffEntryIID);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below otherwise we get NPE on the listener
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
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
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        LOG.info("sffNme: {}", sffName);
        ServiceFunctionForwarder serviceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        assertNotNull(serviceFunctionForwarder);

        // Now we prepare to remove the entry through the listener

        /* Now we prepare to remove the entry through the listener */
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarder.getKey())
                    .build();

        originalData.put(sffEntryIID, serviceFunctionForwarder);
        removedPaths.add(sffEntryIID);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        // The listener will remove the Service Function Type Entry

        sffEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(2000);
        // Verify that RSP was removed
        assertNull(SfcProviderRenderedPathAPI.readRenderedServicePath(renderedServicePath.getName()));

        // Verify that State was removed
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(sffName);
        assertNull(sffServicePathList);
        /*
         * for (SffServicePath sffServicePath : sffServicePathList) {
         * assertNotEquals(sffServicePath.getName(), renderedServicePath.getName());
         * }
         */

        /* Clean-up */

        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFC_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFF_IID, LogicalDatastoreType.CONFIGURATION));
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(OpendaylightSfc.SFP_IID, LogicalDatastoreType.CONFIGURATION));

    }

    /**
     * In this test we create a RSP and update a SFF used by it. This will trigger a more complete
     * code coverage within the listener.
     * In order to simulate a removal from the data store this test does the following:
     * - Create RSP
     * - Update first SF used by RSP by explicitly calling the listener
     * - creates a IID and add to removedPaths data structure. This IID points to the SFF objects
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

        // Prepare to remove the first SF used by the RSP.
        SffName sffName = renderedServicePath.getRenderedServicePathHop().get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarder serviceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        assertNotNull(serviceFunctionForwarder);

        // Now we prepare the updated data. We change mgmt address and type

        /* Now we prepare to remove the entry through the listener */
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarder.getKey())
                    .build();

        ServiceFunctionForwarderBuilder updatedServiceFunctionForwarderBuilder =
                new ServiceFunctionForwarderBuilder(serviceFunctionForwarder);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(UPDATED_IP_MGMT_ADDRESS));
        updatedServiceFunctionForwarderBuilder.setIpMgmtAddress(updatedIpMgmtAddress);
        ServiceFunctionForwarder updatedServiceFunctionForwarder = updatedServiceFunctionForwarderBuilder.build();
        updatedData.put(sffEntryIID, updatedServiceFunctionForwarder);

        originalData.put(sffEntryIID, serviceFunctionForwarder);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below or we get NPEs in the listener
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

        /*
         * The listener will remove the Original Service Function Type Entry and create a new one
         * with the new type
         */
        sffEntryDataListener.onDataChanged(dataChangeEvent);
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
    ServiceFunctionForwarder build_service_function_forwarder() {
        SffName name = new SffName("SFF1");
        String[] sfNames = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1", "unittest-http-header-enrichment-1",
                "unittest-qos-1"};
        IpAddress[] ipMgmtAddress = new IpAddress[sfNames.length];

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
        sfBuilder.setName(new SfName(sfNames[0]))
            .setKey(new ServiceFunctionKey(new SfName("unittest-fw-1")))
            .setType(Firewall.class)
            .setIpMgmtAddress(ipMgmtAddress[0])
            .setSfDataPlaneLocator(dataPlaneLocatorList);
        sfList.add(sfBuilder.build());

        ServiceFunction sf = sfList.get(0);
        SfDataPlaneLocator sfDPLocator = sf.getSfDataPlaneLocator().get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder(sfDPLocator);
        SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder.setName(sf.getName())
            .setKey(new ServiceFunctionDictionaryKey(sf.getName()))
            .setType(sf.getType())
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

        assertTrue(SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff));

        return sff;

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
        Class[] sfTypes = {Firewall.class, Dpi.class, Napt44.class, HttpHeaderEnrichment.class, Qos.class};
        String[] SF_ABSTRACT_NAMES = {"firewall", "dpi", "napt", "http-header-enrichment", "qos"};
        SfcName SFC_NAME = new SfcName("unittest-chain-1");
        SfpName SFP_NAME = new SfpName("unittest-sfp-1");
        RspName RSP_NAME = new RspName("unittest-rsp-1");
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
                .setType(sfTypes[i])
                .setIpMgmtAddress(ipMgmtAddress[i])
                .setSfDataPlaneLocator(dataPlaneLocatorList);
            sfList.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(sfList);

        assertTrue(SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SF_IID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION));
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
        }
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        serviceFunctionForwardersBuilder.setServiceFunctionForwarder(sffList);
        assertTrue(SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SFF_IID, serviceFunctionForwardersBuilder.build(), LogicalDatastoreType.CONFIGURATION));
        // Create Service Function Chain
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(SFC_NAME);
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (int i = 0; i < SF_ABSTRACT_NAMES.length; i++) {
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction = sfcSfBuilder.setName(SF_ABSTRACT_NAMES[i])
                .setKey(new SfcServiceFunctionKey(SF_ABSTRACT_NAMES[i]))
                .setType(sfTypes[i])
                .build();
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

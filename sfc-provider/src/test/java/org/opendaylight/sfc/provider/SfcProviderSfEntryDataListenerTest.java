/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;



/**
 * SfcProviderSfEntryDataListener Tester.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 1.0
 * @since 2015-09-02
 */
public class SfcProviderSfEntryDataListenerTest extends AbstractDataStoreManager {

    private static final String[] LOCATOR_IP_ADDRESS =
            {"196.168.55.1",
                    "196.168.55.2",
                    "196.168.55.3"};
    private static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101",
                    "196.168.55.102",
                    "196.168.55.103"};
    private static final String SF_NAME = "listernerSF";
    private static final String SF_STATE_NAME = "dummySFS";
    private static final String SF_SERVICE_PATH = "dummySFSP";
    private static final String RSP_NAME = "dummyRSP";
    private static final int PORT = 555;

    private static final SfcProviderSfEntryDataListener sfEntryDataListener = new SfcProviderSfEntryDataListener();

    public ListenerRegistration<DataChangeListener> registerAsDataChangeListener() {
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
            OpendaylightSfc.SF_ENTRY_IID, sfEntryDataListener,DataBroker.DataChangeScope.SUBTREE);
    }

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        setOdlSfc();
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change)
     */

    @Test
    public void testRegisterAsDataChangeListener() throws Exception {
        assertNotNull(registerAsDataChangeListener());
    }

    /**
     * Creates SF object, call listeners explicitly, verify that SF Type was created,
     * cleans up
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOnDataChanged_CreateData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        ServiceFunction serviceFunction = build_service_function();

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                child(ServiceFunction.class, serviceFunction.getKey()).build();

        createdData.put(sfEntryIID, serviceFunction);

        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        // Empty MAPs below
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);

        sfEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        SftServiceFunctionName sftServiceFunctionName = SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(serviceFunction);
        assertNotNull(sftServiceFunctionName);
        assertEquals(sftServiceFunctionName.getName(), SF_NAME);
        /* clean up */
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(serviceFunction));
    }


    /**
     * In order to simulate a removal from the data store this test does the following:
     * - creates SF object and inserts it into an MAP data structure representing the original data
     * - creates a IID and add to removedPaths data structure. This IID points to the SF objects stored in the
     *   original data
     * - Call listener explicitly.
     * - Cleans up
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOnDataChanged_RemoveData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        ServiceFunction serviceFunction = build_service_function();

        /* First we create a Service Function Type Entry */
        assertTrue(SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction));

        /* Now we prepare to remove the entry through the listener */
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                child(ServiceFunction.class, serviceFunction.getKey()).build();

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
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOnDataChanged_UpdateData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();



        /* Create and commit SF */
        ServiceFunction originalServiceFunction = build_service_function();
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                child(ServiceFunction.class, originalServiceFunction.getKey()).build();
        assertTrue(SfcProviderServiceFunctionAPI.putServiceFunctionExecutor(originalServiceFunction));
        originalData.put(sfEntryIID, originalServiceFunction);

        /* Now we prepare the updated data */

        ServiceFunctionBuilder updatedServiceFunctionBuilder = new ServiceFunctionBuilder(originalServiceFunction);
        IpAddress updatedIpMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[1]));
        Class<? extends ServiceFunctionTypeIdentity> updatedType = Dpi.class;
        updatedServiceFunctionBuilder.setIpMgmtAddress(updatedIpMgmtAddress).setType(updatedType);
        ServiceFunction updatedServiceFunction = updatedServiceFunctionBuilder.build();
        updatedData.put(sfEntryIID, updatedServiceFunction);


        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        // Empty MAPs below
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

        /* The listener will remove the Original Service Function Type Entry and create a new one
         * with the new type
         */
        sfEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        assertNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(originalServiceFunction));
        assertNotNull(SfcProviderServiceTypeAPI.readServiceFunctionTypeEntry(updatedServiceFunction));
        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(updatedServiceFunction));

    }

    ServiceFunction build_service_function() {
        Class<? extends ServiceFunctionTypeIdentity> type = Firewall.class;
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[0]));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(SF_NAME);

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS[0]));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(LOCATOR_IP_ADDRESS[0]).setLocatorType(ipBuilder.build());
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(SF_NAME).setKey(key)
                .setType(type)
                .setIpMgmtAddress(ipMgmtAddress)
                .setSfDataPlaneLocator(dataPlaneLocatorList);

        ServiceFunction serviceFunction = sfBuilder.build();
        return serviceFunction;
    }

}

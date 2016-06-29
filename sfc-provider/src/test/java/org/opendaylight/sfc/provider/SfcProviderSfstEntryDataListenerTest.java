/*
 * Copyright (c) 2015 Intel Corp. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

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
import org.opendaylight.sfc.provider.api.SfcProviderScheduleTypeAPI;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * SfcProviderSfstEntryDataListener Tester.
 *
 * @author Hongli Chen (honglix.chen@intel.com)
 * @version 1.0
 * @since 2015-09-28
 */
public class SfcProviderSfstEntryDataListenerTest extends AbstractDataStoreManager {

    private static final SfcProviderSfstEntryDataListener sfstEntryDataListener =
            new SfcProviderSfstEntryDataListener();
    private static ListenerRegistration<DataChangeListener> sfstEntryDataListenerRegistration;

    Logger LOG = LoggerFactory.getLogger(SfcProviderSfstEntryDataListenerTest.class);

    public ListenerRegistration<DataChangeListener> registerAsDataChangeListener() {
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.SFST_ENTRY_IID,
                sfstEntryDataListener, DataBroker.DataChangeScope.SUBTREE);
    }

    @Before
    public void before() throws Exception {
        setOdlSfc();
    }

    @After
    public void after() throws Exception {}

    /**
     * Creates SFST object, call listeners explicitly
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

        ServiceFunctionSchedulerType serviceFunctionSchedulerType = build_service_function_scheduler_type();

        assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerType));
        InstanceIdentifier<ServiceFunctionSchedulerType> sfstEntryIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                    .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerType.getKey())
                    .build();

        createdData.put(sfstEntryIID, serviceFunctionSchedulerType);

        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        // Empty MAPs below
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);

        sfstEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);

        ServiceFunctionSchedulerType sfst =
                SfcProviderScheduleTypeAPI.readServiceFunctionScheduleType(serviceFunctionSchedulerType.getType());
        assertNotNull(sfst);
        assertEquals(serviceFunctionSchedulerType, sfst);
        Thread.sleep(500);
        // Clean-up
        assertTrue(
                SfcProviderScheduleTypeAPI.deleteServiceFunctionScheduleType(serviceFunctionSchedulerType.getType()));
        Thread.sleep(500);
    }

    /**
     * In order to simulate a removal from the data store this test does the following:
     * - creates SFST object and inserts it into an MAP data structure representing the original
     * data
     * - creates a IID and add to removedPaths data structure. This IID points to the SFST objects
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

        ServiceFunctionSchedulerType serviceFunctionSchedulerType = build_service_function_scheduler_type();

        InstanceIdentifier<ServiceFunctionSchedulerType> sfstEntryIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                    .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerType.getKey())
                    .build();

        originalData.put(sfstEntryIID, serviceFunctionSchedulerType);
        removedPaths.add(sfstEntryIID);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);

        sfstEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        assertNull(SfcProviderScheduleTypeAPI.readServiceFunctionScheduleType(serviceFunctionSchedulerType.getType()));
    }

    /**
     * In order to simulate an update from the data store this test does the following:
     * - creates SFST object and commits to data store
     * - Creates a copy of the original SFST and updates the NAME
     * - Feeds the original and updated SFSTs to the listener
     * - Asserts that the listener has removed the original and created a new entry
     * - Cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testOnDataChanged_UpdateSFSTData() throws Exception {
        String SchedulerTypeName = "Random1";
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

        /* Create and commit SFST */
        ServiceFunctionSchedulerType serviceFunctionSchedulerType = build_service_function_scheduler_type();

        InstanceIdentifier<ServiceFunctionSchedulerType> sfstEntryIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                    .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerType.getKey())
                    .build();
        assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerType));

        originalData.put(sfstEntryIID, serviceFunctionSchedulerType);

        ServiceFunctionSchedulerTypeBuilder updatedServiceFunctionSchedulerTypeBuilder =
                new ServiceFunctionSchedulerTypeBuilder(serviceFunctionSchedulerType);

        updatedServiceFunctionSchedulerTypeBuilder.setName(SchedulerTypeName);
        ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType =
                updatedServiceFunctionSchedulerTypeBuilder.build();
        assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(updatedServiceFunctionSchedulerType));
        updatedData.put(sfstEntryIID, updatedServiceFunctionSchedulerType);

        when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
        when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
        when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
        when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

        /*
         * The listener will remove the Original Service Function Type Entry and create a new one
         * with the new type
         */
        sfstEntryDataListener.onDataChanged(dataChangeEvent);
        Thread.sleep(500);
        ServiceFunctionSchedulerType sfst = SfcProviderScheduleTypeAPI
            .readServiceFunctionScheduleType(updatedServiceFunctionSchedulerType.getType());
        assertNotNull(sfst);
        assertEquals(updatedServiceFunctionSchedulerType.getName(), sfst.getName());

        // Clean-up
        assertTrue(
                SfcProviderScheduleTypeAPI.deleteServiceFunctionScheduleType(serviceFunctionSchedulerType.getType()));
        Thread.sleep(500);
    }

    /**
     * Builds a complete service Function Schedule Type Object
     *
     * @return ServiceFunctionSchedulerType object
     */
    ServiceFunctionSchedulerType build_service_function_scheduler_type() {
        String SFST_NAME = "listernerSFST";
        Boolean enabledStatus = true;
        Class<? extends ServiceFunctionSchedulerTypeIdentity> schedulerType = Random.class;

        ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(schedulerType);

        ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder.setName(SFST_NAME).setKey(key).setType(schedulerType).setEnabled(enabledStatus);

        ServiceFunctionSchedulerType serviceFunctionSchedulerType = sfstBuilder.build();
        return serviceFunctionSchedulerType;
    }

    /**
     * verify the number of enabled algorithm type,
     * cleans up
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testNumOfEnabledAlgorithmType() throws Exception {
        int count = 0;
        int numOfTrue = 1;
        build_service_function_scheduler_types();
        InstanceIdentifier<ServiceFunctionSchedulerTypes> sfstsEntryIID =
                InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class).build();

        count = countNumOfEnabledAlgorithmType();
        assertEquals("Must be equal", numOfTrue, count);

        // Clean-up
        assertTrue(SfcDataStoreAPI.deleteTransactionAPI(sfstsEntryIID, LogicalDatastoreType.CONFIGURATION));
        Thread.sleep(500);
    }

    /**
     * Builds a complete service Function Schedule Types Object
     */
    public void build_service_function_scheduler_types() throws Exception {
        List<String> SFST_NAMES = new ArrayList<String>() {
            {
                add("listernerSFST1");
                add("listernerSFST2");
                add("listernerSFST3");
            }
        };
        Class[] schedulerTypes = {Random.class, RoundRobin.class, LoadBalance.class};
        Boolean enabledStatus = true;

        for (int i = 0; i < SFST_NAMES.size(); i++) {
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent =
                    Mockito.mock(AsyncDataChangeEvent.class);
            Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
            Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
            Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
            Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();

            ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(schedulerTypes[i]);
            ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
            sfstBuilder.setName(SFST_NAMES.get(i)).setKey(key).setType(schedulerTypes[i]).setEnabled(enabledStatus);

            ServiceFunctionSchedulerType serviceFunctionSchedulerType = sfstBuilder.build();

            assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerType));
            InstanceIdentifier<ServiceFunctionSchedulerType> sfstEntryIID =
                    InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class)
                        .child(ServiceFunctionSchedulerType.class, serviceFunctionSchedulerType.getKey())
                        .build();

            createdData.put(sfstEntryIID, serviceFunctionSchedulerType);

            when(dataChangeEvent.getUpdatedData()).thenReturn(updatedData);
            when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
            when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
            when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

            // The listener will remove the Original Service Function Type Entry and create a new
            // one
            // with the new type
            sfstEntryDataListener.onDataChanged(dataChangeEvent);
            Thread.sleep(500);
        }
    }

    public int countNumOfEnabledAlgorithmType() throws Exception {
        int count = 0;
        ServiceFunctionSchedulerTypes sfsts = SfcProviderScheduleTypeAPI.readAllServiceFunctionScheduleTypes();
        List<ServiceFunctionSchedulerType> sfstList = sfsts.getServiceFunctionSchedulerType();

        for (ServiceFunctionSchedulerType sfst : sfstList) {
            boolean enabled = sfst.isEnabled();
            if (enabled == true) {
                count++;
            }
        }
        return count;
    }
}

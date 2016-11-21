/*
 * Copyright (c) 2015 Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.listeners;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.api.SfcProviderScheduleTypeAPI;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.*;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test Suite to test the ServiceFunctionSchedulerTypeListener class.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */
public class ServiceFunctionSchedulerTypeListenerTest extends AbstractDataStoreManager {
    private final Collection<DataTreeModification<ServiceFunctionSchedulerType>> collection = new ArrayList<>();
    private DataTreeModification<ServiceFunctionSchedulerType> dataTreeModification;
    DataObjectModification<ServiceFunctionSchedulerType> dataObjectModification;

    private final String SFST_NAME = "listernerSFST";
    private final List<String> SFST_NAMES = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;

        {
            add("listernerSFST1");
            add("listernerSFST2");
            add("listernerSFST3");
        }
    };
    private final Class<? extends ServiceFunctionSchedulerTypeIdentity> SFST_TYPE = Random.class;
    @SuppressWarnings("rawtypes")
    private final Class[] SFST_TYPES = {ShortestPath.class, RoundRobin.class, LoadBalance.class};


    // Class under test
    private ServiceFunctionSchedulerTypeListener serviceFunctionSchedulerTypeListener;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        setupSfc();
        dataTreeModification = mock(DataTreeModification.class);
        dataObjectModification = mock(DataObjectModification.class);
        serviceFunctionSchedulerTypeListener = new ServiceFunctionSchedulerTypeListener(getDataBroker());
        serviceFunctionSchedulerTypeListener.init();
    }

    @After
    public void after() throws Exception {
        serviceFunctionSchedulerTypeListener.close();
        close();
    }

    /**
     * Creates SFST object, call listeners explicitly
     * cleans up
     */
    @Test
    public void testOnServiceFunctionSchedulerTypeCreated() throws Exception {

        // Builds a List of Service Function Scheduler Type Objects:
        // ShortestPath, RoundRobin and LoadBalance
        buildServiceFunctionSchedulerTypes();

        // Builds a complete Random Service Function Scheduler Type Object
        ServiceFunctionSchedulerType serviceFunctionSchedulerType = buildServiceFunctionSchedulerType(true);

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.WRITE);
        when(dataObjectModification.getDataBefore()).thenReturn(null);
        when(dataObjectModification.getDataAfter()).thenReturn(serviceFunctionSchedulerType);
        collection.add(dataTreeModification);
        serviceFunctionSchedulerTypeListener.onDataTreeChanged(collection);

        Thread.sleep(500);

        // Check none Scheduler Type is enabled and clean-up
        assertEquals(countNumOfEnabledAlgorithmType(),0);
    }

    /**
     * Deletes SFST object, call listeners explicitly
     * cleans up
     */
    @Test
    public void testOnServiceFunctionSchedulerTypeRemoved() throws Exception {

        // Builds a List of Service Function Scheduler Type Objects:
        // ShortestPath, RoundRobin and LoadBalance
        buildServiceFunctionSchedulerTypes();

        // Builds a complete Random Service Function Scheduler Type Object
        ServiceFunctionSchedulerType serviceFunctionSchedulerType = buildServiceFunctionSchedulerType(false);
        assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerType));

        Thread.sleep(500);

        // Check the new Service Function Scheduler Type Object is the desired one
        ServiceFunctionSchedulerType sfst =
                SfcProviderScheduleTypeAPI.readServiceFunctionScheduleType(serviceFunctionSchedulerType.getType());
        assertNotNull(sfst);
        assertEquals(serviceFunctionSchedulerType, sfst);


        // We trigger the removal of the new service Function Scheduler Type Object
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.DELETE);
        when(dataObjectModification.getDataBefore()).thenReturn(serviceFunctionSchedulerType);
        collection.add(dataTreeModification);
        serviceFunctionSchedulerTypeListener.onDataTreeChanged(collection);

        Thread.sleep(500);


        // Check just one Scheduler Type is enabled and clean-up
        assertEquals(countNumOfEnabledAlgorithmType(),1);
    }

    /**
     * Updates SFST object, call listeners explicitly
     * cleans up
     */
    @Test
    public void testOnServiceFunctionSchedulerTypeUpdated() throws Exception {

        // Builds a List of Service Function Scheduler Type Objects:
        // ShortestPath, RoundRobin and LoadBalance
        buildServiceFunctionSchedulerTypes();

        // Builds a Random Service Function Scheduler Type Object with isEnabled set to FALSE
        ServiceFunctionSchedulerType originalServiceFunctionSchedulerType = buildServiceFunctionSchedulerType(false);
        assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(originalServiceFunctionSchedulerType));
        Thread.sleep(500);

        // Check the new Service Function Scheduler Type Object is the desired one
        ServiceFunctionSchedulerType sfst =
                SfcProviderScheduleTypeAPI.readServiceFunctionScheduleType(originalServiceFunctionSchedulerType.getType());
        assertEquals(originalServiceFunctionSchedulerType, sfst);
        assertFalse (sfst.isEnabled());
        assertEquals(countNumOfEnabledAlgorithmType(),1);

        // Modify the last Service Function Scheduler Type Object created setting isEnabled to TRUE

        ServiceFunctionSchedulerTypeBuilder updatedServiceFunctionSchedulerTypeBuilder =
                new ServiceFunctionSchedulerTypeBuilder(originalServiceFunctionSchedulerType);

        updatedServiceFunctionSchedulerTypeBuilder.setEnabled(true);
        ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType =
                updatedServiceFunctionSchedulerTypeBuilder.build();

        // We trigger the update of the service Function Scheduler Type Object
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionSchedulerType);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionSchedulerType);
        collection.add(dataTreeModification);
        serviceFunctionSchedulerTypeListener.onDataTreeChanged(collection);

        Thread.sleep(500);

        // Check that the Random Scheduler Type is the only one enabled and clean-up
        assertEquals(countNumOfEnabledAlgorithmType(),0);
    }

    /**
     * Builds a complete service Function Scheduler Type Object
     *
     * @return ServiceFunctionSchedulerType object
     */
    public ServiceFunctionSchedulerType buildServiceFunctionSchedulerType(boolean enabledStatus) {

        Boolean enabled;
        enabled = enabledStatus;

        ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(SFST_TYPE);

        ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder.setName(SFST_NAME).setKey(key).setType(SFST_TYPE).setEnabled(enabled);

        return sfstBuilder.build();
    }
    /**
     * Builds a complete service Function Scheduler Types Object
     */
    @SuppressWarnings("unchecked")
    public void buildServiceFunctionSchedulerTypes() throws Exception {

        Boolean enabledStatus = true;

        for (int i = 0; i < SFST_NAMES.size(); i++) {
            ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(SFST_TYPES[i]);
            ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
            sfstBuilder.setName(SFST_NAMES.get(i)).setKey(key).setType(SFST_TYPES[i]).setEnabled(enabledStatus);
            ServiceFunctionSchedulerType serviceFunctionSchedulerType;
            serviceFunctionSchedulerType = sfstBuilder.build();
            assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerType));
            Thread.sleep(500);
        }
    }


    private int countNumOfEnabledAlgorithmType() throws Exception {
        int count = 0;
        ServiceFunctionSchedulerTypes sfsts = SfcProviderScheduleTypeAPI.readAllServiceFunctionScheduleTypes();
        List<ServiceFunctionSchedulerType> sfstList = sfsts.getServiceFunctionSchedulerType();

        for (ServiceFunctionSchedulerType sfst : sfstList) {
            boolean enabled = sfst.isEnabled();
            if ( enabled ) {
                count++;
            }
            // Clean-up
            assertTrue(SfcProviderScheduleTypeAPI.deleteServiceFunctionScheduleType(sfst.getType()));
            Thread.sleep(500);
        }
        return count;
    }

}

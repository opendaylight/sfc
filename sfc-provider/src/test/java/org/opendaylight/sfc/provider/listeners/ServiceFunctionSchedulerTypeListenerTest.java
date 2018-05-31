/*
 * Copyright (c) 2015, 2018 Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.api.SfcProviderScheduleTypeAPI;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ShortestPath;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test Suite to test the ServiceFunctionSchedulerTypeListener class.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */
public class ServiceFunctionSchedulerTypeListenerTest extends AbstractDataStoreManager {

    private static final String SFST_NAME = "listernerSFST";
    private static final List<String> SFST_NAMES = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;

        {
            add("listernerSFST1");
            add("listernerSFST2");
            add("listernerSFST3");
        }
    };

    private static final Class<? extends ServiceFunctionSchedulerTypeIdentity> SFST_TYPE = Random.class;
    @SuppressWarnings("rawtypes")
    private static final Class[] SFST_TYPES = { ShortestPath.class, RoundRobin.class, LoadBalance.class };

    // Class under test
    private ServiceFunctionSchedulerTypeListener serviceFunctionSchedulerTypeListener;

    @Before
    public void before() {
        setupSfc();
        serviceFunctionSchedulerTypeListener = new ServiceFunctionSchedulerTypeListener(getDataBroker());
    }

    @After
    public void after() throws Exception {
        close();
    }

    /**
     * Creates SFST object, call listeners explicitly cleans up.
     */
    @Test
    public void testOnServiceFunctionSchedulerTypeCreated() throws Exception {
        // Builds a List of Service Function Scheduler Type Objects:
        // ShortestPath, RoundRobin and LoadBalance
        buildServiceFunctionSchedulerTypes();
        assertEquals(1, countNumOfEnabledAlgorithmType());

        // Builds a complete Random Service Function Scheduler Type Object
        ServiceFunctionSchedulerType serviceFunctionSchedulerType = buildServiceFunctionSchedulerType(true);

        serviceFunctionSchedulerTypeListener
                .add(InstanceIdentifier.create(ServiceFunctionSchedulerType.class), serviceFunctionSchedulerType);

        // Check none Scheduler Type is enabled and clean-up
        assertEquals(0, countNumOfEnabledAlgorithmType());
    }

    /**
     * Deletes SFST object, call listeners explicitly cleans up.
     */
    @Test
    public void testOnServiceFunctionSchedulerTypeRemoved() throws Exception {
        // Builds a List of Service Function Scheduler Type Objects:
        // ShortestPath, RoundRobin and LoadBalance
        buildServiceFunctionSchedulerTypes();

        // Builds a complete Random Service Function Scheduler Type Object
        ServiceFunctionSchedulerType serviceFunctionSchedulerType = buildServiceFunctionSchedulerType(false);
        assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerType));

        // Check the new Service Function Scheduler Type Object is the desired one
        ServiceFunctionSchedulerType sfst = SfcProviderScheduleTypeAPI
                .readServiceFunctionScheduleType(serviceFunctionSchedulerType.getType());
        assertNotNull(sfst);
        assertEquals(serviceFunctionSchedulerType, sfst);

        // We trigger the removal of the new service Function Scheduler Type Object
        serviceFunctionSchedulerTypeListener.remove(InstanceIdentifier.create(ServiceFunctionSchedulerType.class),
                                                    serviceFunctionSchedulerType);

        // Check just one Scheduler Type is enabled and clean-up
        assertEquals(1, countNumOfEnabledAlgorithmType());
    }

    /**
     * Updates SFST object, call listeners explicitly cleans up.
     */
    @Test
    public void testOnServiceFunctionSchedulerTypeUpdated() throws Exception {
        // Builds a List of Service Function Scheduler Type Objects:
        // ShortestPath, RoundRobin and LoadBalance
        buildServiceFunctionSchedulerTypes();

        // Builds a Random Service Function Scheduler Type Object with isEnabled
        // set to FALSE
        ServiceFunctionSchedulerType originalServiceFunctionSchedulerType = buildServiceFunctionSchedulerType(false);
        assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(originalServiceFunctionSchedulerType));

        // Check the new Service Function Scheduler Type Object is the desired
        // one
        ServiceFunctionSchedulerType sfst = SfcProviderScheduleTypeAPI
                .readServiceFunctionScheduleType(originalServiceFunctionSchedulerType.getType());
        assertEquals(originalServiceFunctionSchedulerType, sfst);
        assertFalse(sfst.isEnabled());
        assertEquals(1, countNumOfEnabledAlgorithmType());

        // Modify the last Service Function Scheduler Type Object created
        // setting isEnabled to TRUE
        ServiceFunctionSchedulerTypeBuilder updatedServiceFunctionSchedulerTypeBuilder =
                new ServiceFunctionSchedulerTypeBuilder(originalServiceFunctionSchedulerType);

        updatedServiceFunctionSchedulerTypeBuilder.setEnabled(true);
        ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType = updatedServiceFunctionSchedulerTypeBuilder
                .build();

        // We trigger the update of the service Function Scheduler Type Object
        serviceFunctionSchedulerTypeListener
                .update(InstanceIdentifier.create(ServiceFunctionSchedulerType.class),
                        originalServiceFunctionSchedulerType, updatedServiceFunctionSchedulerType);

        // Check that the Random Scheduler Type is the only one enabled and
        // clean-up
        assertEquals(0, countNumOfEnabledAlgorithmType());
    }

    /**
     * Builds a complete service Function Scheduler Type Object.
     *
     * @return ServiceFunctionSchedulerType object
     */
    private ServiceFunctionSchedulerType buildServiceFunctionSchedulerType(boolean enabledStatus) {

        Boolean enabled;
        enabled = enabledStatus;

        ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(SFST_TYPE);

        ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder.setName(SFST_NAME).withKey(key).setType(SFST_TYPE).setEnabled(enabled);

        return sfstBuilder.build();
    }

    /**
     * Builds a complete service Function Scheduler Types Object.
     */
    private void buildServiceFunctionSchedulerTypes() throws Exception {

        Boolean enabledStatus = true;

        for (int i = 0; i < SFST_NAMES.size(); i++) {
            ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(SFST_TYPES[i]);
            ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
            sfstBuilder.setName(SFST_NAMES.get(i)).withKey(key).setType(SFST_TYPES[i]).setEnabled(enabledStatus);
            enabledStatus = false;
            ServiceFunctionSchedulerType serviceFunctionSchedulerType;
            serviceFunctionSchedulerType = sfstBuilder.build();
            assertTrue(SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerType));
        }
    }

    private long countNumOfEnabledAlgorithmType() {
        return Optional.ofNullable(SfcProviderScheduleTypeAPI.readAllServiceFunctionScheduleTypes())
                .map(ServiceFunctionSchedulerTypes::getServiceFunctionSchedulerType)
                .orElse(Collections.emptyList())
                .stream()
                .filter(ServiceFunctionSchedulerType::isEnabled)
                .count();
    }
}

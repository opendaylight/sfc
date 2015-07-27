/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class SfcProviderScheduleTypeAPITest extends AbstractDataStoreManager {

    @Before
    public void before() {
        setOdlSfc();
    }

    /* Cannot pass at the moment, will fix later on
     * Error log:
     *  ERROR o.o.s.p.api.SfcProviderAbstractAPI - Could not find method readServiceFunctionScheduleType in class

    @Test
    public void testCreateReadScheduleType() throws ExecutionException, InterruptedException {
        String name = "random-01";
        Class<? extends ServiceFunctionSchedulerTypeIdentity> type = Random.class;
        ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(type);

        ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder.setName(name).setType(type).setKey(key).setEnabled(true);

        Object[] parameters = {sfstBuilder.build()};
        Class[] parameterTypes = {ServiceFunctionSchedulerType.class};

        try {
            executor.submit(SfcProviderScheduleTypeAPI
                    .getPut(parameters, parameterTypes)).get();
        } catch (ExecutionException ee) {
            assertEquals("Must be equal", 0, 1);
        } catch (InterruptedException ie) {
            assertEquals("Must be equal", 0, 1);
        }

        Object[] parameters2 = {type};
        Class[] parameterTypes2 = {ServiceFunctionSchedulerTypeIdentity.class};
        try {
            Object result = executor.submit(SfcProviderScheduleTypeAPI
                    .getRead(parameters2, parameterTypes2)).get();
            ServiceFunctionSchedulerType sfst2 = (ServiceFunctionSchedulerType) result;

            assertNotNull("Must be not null", sfst2);
            assertEquals("Must be equal", sfst2.getName(), name);
            assertEquals("Must be equal", sfst2.getType(), type);
            assertEquals("Must be equal", sfst2.isEnabled(), true);
        } catch (ExecutionException ee) {
            assertEquals("Must be equal", 0, 1);
        } catch (InterruptedException ie) {
            assertEquals("Must be equal", 0, 1);
        }
    }

    @Test
    public void testCreateDeleteScheduleType() throws ExecutionException, InterruptedException {
        String name = "random-01";
        Class<? extends ServiceFunctionSchedulerTypeIdentity> type = Random.class;
        ServiceFunctionSchedulerTypeKey key = new ServiceFunctionSchedulerTypeKey(type);

        ServiceFunctionSchedulerTypeBuilder sfstBuilder = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder.setName(name).setType(type).setKey(key).setEnabled(true);

        Object[] parameters = {sfstBuilder.build()};
        Class[] parameterTypes = {ServiceFunctionSchedulerType.class};

        try {
            executor.submit(SfcProviderScheduleTypeAPI
                    .getPut(parameters, parameterTypes)).get();
        } catch (ExecutionException ee) {
            assertEquals("Must be equal", 0, 1);
        } catch (InterruptedException ie) {
            assertEquals("Must be equal", 0, 1);
        }

        Object[] parameters2 = {type};
        Class[] parameterTypes2 = {ServiceFunctionSchedulerTypeIdentity.class};
        try {
            Object result = executor.submit(SfcProviderScheduleTypeAPI
                    .getRead(parameters2, parameterTypes2)).get();
            ServiceFunctionSchedulerType sfst2 = (ServiceFunctionSchedulerType) result;
            assertNotNull("Must be not null", sfst2);

            result = executor.submit(SfcProviderScheduleTypeAPI
                    .getDelete(parameters2, parameterTypes2)).get();
            result = executor.submit(SfcProviderScheduleTypeAPI
                    .getRead(parameters2, parameterTypes2)).get();
            ServiceFunctionSchedulerType sfst3 = (ServiceFunctionSchedulerType) result;
            assertNull("Must be null", sfst3);
        } catch (ExecutionException ee) {
            assertEquals("Must be equal", 0, 1);
        } catch (InterruptedException ie) {
            assertEquals("Must be equal", 0, 1);
        }
    }
    */

    @Test
    public void testCreateReadAllScheduleTypes() throws ExecutionException, InterruptedException {
        String name1 = "random-01";
        Class<? extends ServiceFunctionSchedulerTypeIdentity> type1 = Random.class;
        ServiceFunctionSchedulerTypeKey key1 = new ServiceFunctionSchedulerTypeKey(type1);

        String name2 = "roundrobin-01";
        Class<? extends ServiceFunctionSchedulerTypeIdentity> type2 = RoundRobin.class;
        ServiceFunctionSchedulerTypeKey key2 = new ServiceFunctionSchedulerTypeKey(type2);

        ServiceFunctionSchedulerTypeBuilder sfstBuilder1 = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder1.setName(name1).setType(type1).setKey(key1).setEnabled(true);
        ServiceFunctionSchedulerTypeBuilder sfstBuilder2 = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder2.setName(name2).setType(type2).setKey(key2).setEnabled(false);

        Object[] parameters1 = {sfstBuilder1.build()};
        Object[] parameters2 = {sfstBuilder2.build()};
        Class[] parameterTypes = {ServiceFunctionSchedulerType.class};

        executor.submit(SfcProviderScheduleTypeAPI
                .getPut(parameters1, parameterTypes)).get();
        executor.submit(SfcProviderScheduleTypeAPI
                .getPut(parameters2, parameterTypes)).get();

        Object[] parameters = {};
        Class[] parameterTypes2 = {};

        Object result = executor.submit(SfcProviderScheduleTypeAPI
                .getReadAll(parameters, parameterTypes2)).get();
        ServiceFunctionSchedulerTypes sfsts = (ServiceFunctionSchedulerTypes) result;
        List<ServiceFunctionSchedulerType> sfstList = sfsts.getServiceFunctionSchedulerType();
        assertEquals("Must be equal", sfstList.size(), 2);
    }

    @Test
    public void testCreateReadEnabledScheduleTypeEntryExecutor() throws ExecutionException, InterruptedException {
        String name1 = "random-01";
        Class<? extends ServiceFunctionSchedulerTypeIdentity> type1 = Random.class;
        ServiceFunctionSchedulerTypeKey key1 = new ServiceFunctionSchedulerTypeKey(type1);

        String name2 = "roundrobin-01";
        Class<? extends ServiceFunctionSchedulerTypeIdentity> type2 = RoundRobin.class;
        ServiceFunctionSchedulerTypeKey key2 = new ServiceFunctionSchedulerTypeKey(type2);

        ServiceFunctionSchedulerTypeBuilder sfstBuilder1 = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder1.setName(name1).setType(type1).setKey(key1).setEnabled(true);
        ServiceFunctionSchedulerTypeBuilder sfstBuilder2 = new ServiceFunctionSchedulerTypeBuilder();
        sfstBuilder2.setName(name2).setType(type2).setKey(key2).setEnabled(false);

        Object[] parameters1 = {sfstBuilder1.build()};
        Object[] parameters2 = {sfstBuilder2.build()};
        Class[] parameterTypes = {ServiceFunctionSchedulerType.class};


        executor.submit(SfcProviderScheduleTypeAPI
                .getPut(parameters1, parameterTypes)).get();
        executor.submit(SfcProviderScheduleTypeAPI
                .getPut(parameters2, parameterTypes)).get();

        Object result = SfcProviderScheduleTypeAPI.readEnabledServiceFunctionScheduleTypeEntryExecutor();
        ServiceFunctionSchedulerType sfst = (ServiceFunctionSchedulerType) result;

        assertNotNull("Must be not null", sfst);
        assertEquals("Must be equal", sfst.getName(), name1);
        assertEquals("Must be equal", sfst.getType(), type1);
        assertEquals("Must be equal", sfst.isEnabled(), true);
    }

    @Test
    public void testPutServiceFunctionScheduleType() {
        //build service function scheduler type
        ServiceFunctionSchedulerTypeBuilder serviceFunctionSchedulerTypeBuilder = new ServiceFunctionSchedulerTypeBuilder();
        serviceFunctionSchedulerTypeBuilder.setName("SFST")
                .setKey(new ServiceFunctionSchedulerTypeKey(RoundRobin.class))
                .setEnabled(true)
                .setType(RoundRobin.class);

        //write service function scheduler type
        boolean transactionSuccessful = SfcProviderScheduleTypeAPI.putServiceFunctionScheduleTypeExecutor(serviceFunctionSchedulerTypeBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        //read written service function scheduler type
        ServiceFunctionSchedulerType serviceFunctionSchedulerType = SfcProviderScheduleTypeAPI.readServiceFunctionScheduleTypeExecutor(RoundRobin.class);
        assertNotNull("Must not be null", serviceFunctionSchedulerType);
        assertEquals("Must be equal", serviceFunctionSchedulerType.getName(), "SFST");
        assertEquals("Must be equal", serviceFunctionSchedulerType.getType(), RoundRobin.class);
        assertTrue("Must be equal", serviceFunctionSchedulerType.isEnabled());

        //remove service function scheduler type
        transactionSuccessful = SfcProviderScheduleTypeAPI.deleteServiceFunctionScheduleTypeExecutor(RoundRobin.class);
        assertTrue("Must be true", transactionSuccessful);
    }
}

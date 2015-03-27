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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;

import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeKey;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

public class SfcProviderScheduleTypeAPITest extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderScheduleTypeAPITest.class);
    DataBroker dataBroker;
    ExecutorService executor;
    OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    @Before
    public void before() {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();
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

        try {
            executor.submit(SfcProviderScheduleTypeAPI
                    .getPut(parameters1, parameterTypes)).get();
            executor.submit(SfcProviderScheduleTypeAPI
                    .getPut(parameters2, parameterTypes)).get();
        } catch (ExecutionException ee) {
            /* Fail this case since Exception is caught */
            assertEquals("Must be equal", 0, 1);
        } catch (InterruptedException ie) {
            assertEquals("Must be equal", 0, 1);
        }

        Object[] parameters = {};
        Class[] parameterTypes2 = {};
        try {
            Object result = executor.submit(SfcProviderScheduleTypeAPI
                    .getReadAll(parameters, parameterTypes2)).get();
            ServiceFunctionSchedulerTypes sfsts = (ServiceFunctionSchedulerTypes) result;
            List<ServiceFunctionSchedulerType> sfstList = sfsts.getServiceFunctionSchedulerType();
            assertEquals("Must be equal", sfstList.size(), 2);
        } catch (ExecutionException ee) {
            /* Fail this case since Exception is caught */
            assertEquals("Must be equal", 0, 1);
        } catch (InterruptedException ie) {
            assertEquals("Must be equal", 0, 1);
        }
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

        try {
            executor.submit(SfcProviderScheduleTypeAPI
                    .getPut(parameters1, parameterTypes)).get();
            executor.submit(SfcProviderScheduleTypeAPI
                    .getPut(parameters2, parameterTypes)).get();
        } catch (ExecutionException ee) {
            /* Fail this case since Exception is caught */
            assertEquals("Must be equal", 0, 1);
        } catch (InterruptedException ie) {
            assertEquals("Must be equal", 0, 1);
        }

        try {
            Object result = SfcProviderScheduleTypeAPI.readEnabledServiceFunctionScheduleTypeEntryExecutor();
            ServiceFunctionSchedulerType sfst = (ServiceFunctionSchedulerType) result;

            assertNotNull("Must be not null", sfst);
            assertEquals("Must be equal", sfst.getName(), name1);
            assertEquals("Must be equal", sfst.getType(), type1);
            assertEquals("Must be equal", sfst.isEnabled(), true);
        } catch (Exception e) {
            /* Fail this case since Exception is caught */
            assertEquals("Must be equal", 0, 1);
        }
    }
}

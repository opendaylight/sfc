/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.List;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SfcProviderScheduleTypeAPITest extends AbstractDataStoreManager {

    @Before
    public void before() {
        setOdlSfc();
    }

    @Test
    public void testCreateReadAllScheduleTypes() {
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

        SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstBuilder1.build());
        SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstBuilder2.build());

        ServiceFunctionSchedulerTypes sfsts = SfcProviderScheduleTypeAPI.readAllServiceFunctionScheduleTypes();
        List<ServiceFunctionSchedulerType> sfstList = sfsts.getServiceFunctionSchedulerType();
        assertEquals("Must be equal", sfstList.size(), 2);
    }

    @Test
    public void testCreateReadEnabledScheduleTypeEntryExecutor() {
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

        SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstBuilder1.build());
        SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstBuilder2.build());

        Object result = SfcProviderScheduleTypeAPI.readEnabledServiceFunctionScheduleTypeEntry();
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
        boolean transactionSuccessful = SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(serviceFunctionSchedulerTypeBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        //read written service function scheduler type
        ServiceFunctionSchedulerType serviceFunctionSchedulerType = SfcProviderScheduleTypeAPI.readServiceFunctionScheduleType(RoundRobin.class);
        assertNotNull("Must not be null", serviceFunctionSchedulerType);
        assertEquals("Must be equal", serviceFunctionSchedulerType.getName(), "SFST");
        assertEquals("Must be equal", serviceFunctionSchedulerType.getType(), RoundRobin.class);
        assertTrue("Must be equal", serviceFunctionSchedulerType.isEnabled());

        //remove service function scheduler type
        transactionSuccessful = SfcProviderScheduleTypeAPI.deleteServiceFunctionScheduleType(RoundRobin.class);
        assertTrue("Must be true", transactionSuccessful);
    }
}

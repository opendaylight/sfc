/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * This class contains unit tests for SfcServiceFunctionRandomSchedulerAPI.
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-06-30
 */
public class SfcServiceFunctionRandomSchedulerAPITest extends AbstractDataStoreManager {

    private static final SfcName SFC_NAME = new SfcName("sfcName");
    private static final String SF_NAME_BASE = "sfName";
    private static final SfName SF_NAME1 = new SfName("sfName1");
    private static final SfName SF_NAME2 = new SfName("sfName2");
    private static final SfName SF_NAME3 = new SfName("sfName3");
    private static final SfpName SFP_NAME = new SfpName("sfpName");
    private static final SffName SFF_NAME = new SffName("sffName");
    private static final String SFG_NAME = "sfgName";
    private SfcServiceFunctionRandomSchedulerAPI scheduler;

    @Before
    public void before() {
        setupSfc();
        scheduler = new SfcServiceFunctionRandomSchedulerAPI();
    }

    @After
    public void after() throws ExecutionException, InterruptedException {
        close();
    }

    /*
     * returns service functions name list from service function chain
     */
    @Test
    public void testServiceFunctionRandomScheduler() {
        // list of all service function names, the one returned by
        // scheduleServiceFunction have to
        // be the same
        List<SfName> serviceFunctions = new ArrayList<>();
        serviceFunctions.add(SF_NAME1);
        serviceFunctions.add(SF_NAME2);
        serviceFunctions.add(SF_NAME3);

        List<SfName> result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255,
                createServiceFunctionPath());
        assertNotNull("Must be not null", result);
        assertTrue("Must be equal", result.containsAll(serviceFunctions));
    }

    /*
     * from existing service functions and types, ans service function is found
     * and returned as a string
     */
    @Test
    public void testServiceFunctionRandomScheduler1() {

        // create empty path
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        // no types are written, should return null
        List<SfName> result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255,
                serviceFunctionPathBuilder.build());

        assertNull("Must be null", result);

        // write types
        boolean transactionSuccessful = writeTypes(true);

        assertTrue("Must be true", transactionSuccessful);

        // no functions are written, should return empty array
        result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255,
                serviceFunctionPathBuilder.build());

        assertTrue("Must be true", result.contains(null));

        // write functions of all types
        transactionSuccessful = writeServiceFunction("Firewall", true);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Dpi", true);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Qos", true);
        assertTrue("Must be true", transactionSuccessful);

        result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255,
                serviceFunctionPathBuilder.build());

        // expected list
        List<SfName> serviceFunctionTypes = new ArrayList<>();
        serviceFunctionTypes.add(new SfName(SF_NAME_BASE + "Firewall"));
        serviceFunctionTypes.add(new SfName(SF_NAME_BASE + "Dpi"));
        serviceFunctionTypes.add(new SfName(SF_NAME_BASE + "Qos"));

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result.containsAll(serviceFunctionTypes));

        // remove functions and types
        transactionSuccessful = writeServiceFunction("Firewall", false);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Dpi", false);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Qos", false);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeTypes(false);
        assertTrue("Must be true", transactionSuccessful);
    }

    // create service function chain with three entries
    private ServiceFunctionChain createServiceFunctionChain() {
        SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        sfcServiceFunctionBuilder.setName(SF_NAME1.getValue()).withKey(new SfcServiceFunctionKey(SF_NAME1.getValue()))
                .setType(new SftTypeName("firewall"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME2.getValue()).withKey(new SfcServiceFunctionKey(SF_NAME2.getValue()))
                .setType(new SftTypeName("dpi"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME3.getValue()).withKey(new SfcServiceFunctionKey(SF_NAME3.getValue()))
                .setType(new SftTypeName("qos"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());

        ServiceFunctionChainBuilder serviceFunctionChainBuilder =
                new ServiceFunctionChainBuilder();
        serviceFunctionChainBuilder.setName(SFC_NAME).withKey(new ServiceFunctionChainKey(SFC_NAME)).setSymmetric(true)
                .setSfcServiceFunction(sfcServiceFunctionList);

        return serviceFunctionChainBuilder.build();
    }

    // create service function list
    private List<SftServiceFunctionName> createSftServiceFunctionNames(String serviceFunctionType) {
        List<SftServiceFunctionName> sftServiceFunctionNames = new ArrayList<>();

        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();

        SfName sfName = new SfName(SF_NAME_BASE + serviceFunctionType);

        sftServiceFunctionNameBuilder.setName(sfName).withKey(new SftServiceFunctionNameKey(sfName));
        sftServiceFunctionNames.add(sftServiceFunctionNameBuilder.build());

        return sftServiceFunctionNames;
    }

    // create service function path
    private ServiceFunctionPath createServiceFunctionPath() {
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        List<ServicePathHop> servicePathHopList = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();
            servicePathHopBuilder.setHopNumber((short) i).withKey(new ServicePathHopKey((short) i))
                    .setServiceFunctionForwarder(SFF_NAME).setServiceFunctionGroupName(SFG_NAME)
                    .setServiceIndex((short) (i + 1)).setServiceFunctionName(new SfName(SF_NAME_BASE + (i + 1)));
            servicePathHopList.add(servicePathHopBuilder.build());
        }

        serviceFunctionPathBuilder.setName(SFP_NAME).withKey(new ServiceFunctionPathKey(SFP_NAME))
                .setServicePathHop(servicePathHopList);

        return serviceFunctionPathBuilder.build();
    }

    // write types
    private boolean writeTypes(boolean write) {
        List<ServiceFunctionType> serviceFunctionTypeList = new ArrayList<>();

        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder.setSftServiceFunctionName(createSftServiceFunctionNames("Firewall"))
                .setType(new SftTypeName("firewall"));
        serviceFunctionTypeList.add(serviceFunctionTypeBuilder.build());

        serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder.setSftServiceFunctionName(createSftServiceFunctionNames("Dpi"))
                .setType(new SftTypeName("dpi"));
        serviceFunctionTypeList.add(serviceFunctionTypeBuilder.build());

        serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder.setSftServiceFunctionName(createSftServiceFunctionNames("Qos"))
                .setType(new SftTypeName("qos"));
        serviceFunctionTypeList.add(serviceFunctionTypeBuilder.build());

        ServiceFunctionTypesBuilder serviceFunctionTypesBuilder =
                new ServiceFunctionTypesBuilder();
        serviceFunctionTypesBuilder.setServiceFunctionType(serviceFunctionTypeList);

        InstanceIdentifier<ServiceFunctionTypes> sftIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .build();

        if (write) {
            return SfcDataStoreAPI.writePutTransactionAPI(sftIID, serviceFunctionTypesBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION);
        } else {
            return SfcDataStoreAPI.deleteTransactionAPI(sftIID, LogicalDatastoreType.CONFIGURATION);
        }
    }

    // write service function
    private boolean writeServiceFunction(String sfType, boolean write) {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(new SfName(SF_NAME_BASE + sfType))
                .withKey(new ServiceFunctionKey(new SfName(SF_NAME_BASE + sfType))).setType(new SftTypeName("firewall"));
        InstanceIdentifier<ServiceFunction> sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, new ServiceFunctionKey(new SfName(SF_NAME_BASE + sfType))).build();

        if (write) {
            return SfcDataStoreAPI.writePutTransactionAPI(sfIID, serviceFunctionBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION);
        } else {
            return SfcDataStoreAPI.deleteTransactionAPI(sfIID, LogicalDatastoreType.CONFIGURATION);
        }
    }
}

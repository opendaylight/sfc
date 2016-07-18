/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
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
import org.powermock.reflect.Whitebox;

/**
 * This class contains unit tests for SfcServiceFunctionRoundRobinSchedulerAPI
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-06-29
 */
public class SfcServiceFunctionRoundRobinSchedulerAPITest extends AbstractDataStoreManager {

    private static final SfcName SFC_NAME = new SfcName("sfcName");
    private static final String SF_NAME_BASE = "sfName";
    private static final SfpName SFP_NAME = new SfpName("sfpName");
    private static final SffName SFF_NAME = new SffName("sffName");
    private static final String SFG_NAME = "sfgName";

    @Before
    public void before() {
        setOdlSfc();
    }

    /*
     * returns service functions name list from service function chain
     */
    @Test
    public void testServiceFunctionRoundRobinScheduler() {
        SfcServiceFunctionRoundRobinSchedulerAPI scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();

        List<SfName> result =
                scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255, createServiceFunctionPath());

        // list of all service function names, the one returned by scheduleServiceFunction have to
        // be the same
        List<SfName> serviceFunctions = new ArrayList<>();
        serviceFunctions.add(new SfName(SF_NAME_BASE + 1));
        serviceFunctions.add(new SfName(SF_NAME_BASE + 2));
        serviceFunctions.add(new SfName(SF_NAME_BASE + 3));

        assertNotNull("Must be not null", result);
        assertTrue("Must be true", result.containsAll(serviceFunctions));
    }

    /*
     * from existing service function types, and service function is found and returned as a string
     */
    @Test
    public void testServiceFunctionRoundRobinScheduler1() throws IllegalAccessException {

        // before test, private static variable mapCountRoundRobin has to be restored to original
        // state
        Whitebox.getField(SfcServiceFunctionRoundRobinSchedulerAPI.class, "mapCountRoundRobin").set(HashMap.class,
                new HashMap<>());

        SfcServiceFunctionRoundRobinSchedulerAPI scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();

        // create empty path
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        // no types are written, should return null
        List<SfName> result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255,
                serviceFunctionPathBuilder.build());

        assertNull("Must be null", result);

        // write types
        boolean transactionSuccessful = writeTypes(true);

        assertTrue("Must be true", transactionSuccessful);

        result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255,
                serviceFunctionPathBuilder.build());

        List<SfName> serviceFunctionTypes = new ArrayList<>();
        serviceFunctionTypes.add(new SfName(SF_NAME_BASE + "Firewall"));
        serviceFunctionTypes.add(new SfName(SF_NAME_BASE + "Dpi"));
        serviceFunctionTypes.add(new SfName(SF_NAME_BASE + "Qos"));

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result.containsAll(serviceFunctionTypes));

        // remove types
        transactionSuccessful = writeTypes(false);

        assertTrue("Must be true", transactionSuccessful);
    }

    // create service function chain with three entries
    private ServiceFunctionChain createServiceFunctionChain() {
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        sfcServiceFunctionBuilder.setName(SF_NAME_BASE + 1)
            .setKey(new SfcServiceFunctionKey(SF_NAME_BASE + 1))
            .setType(new SftTypeName("firewall"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME_BASE + 2)
            .setKey(new SfcServiceFunctionKey(SF_NAME_BASE + 2))
            .setType(new SftTypeName("dpi"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME_BASE + 3)
            .setKey(new SfcServiceFunctionKey(SF_NAME_BASE + 3))
            .setType(new SftTypeName("qos"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());

        serviceFunctionChainBuilder.setName(SFC_NAME)
            .setKey(new ServiceFunctionChainKey(SFC_NAME))
            .setSymmetric(true)
            .setSfcServiceFunction(sfcServiceFunctionList);

        return serviceFunctionChainBuilder.build();
    }

    // create service function list
    private List<SftServiceFunctionName> createSftServiceFunctionNames(String serviceFunctionType) {
        List<SftServiceFunctionName> sftServiceFunctionNames = new ArrayList<>();

        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();

        SfName sfName = new SfName(SF_NAME_BASE + serviceFunctionType);

        sftServiceFunctionNameBuilder.setName(sfName)
            .setKey(new SftServiceFunctionNameKey(sfName));
        sftServiceFunctionNames.add(sftServiceFunctionNameBuilder.build());

        return sftServiceFunctionNames;
    }

    // create service function path
    private ServiceFunctionPath createServiceFunctionPath() {
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        List<ServicePathHop> servicePathHopList = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();
            servicePathHopBuilder.setHopNumber((short) i)
                .setKey(new ServicePathHopKey((short) i))
                .setServiceFunctionForwarder(SFF_NAME)
                .setServiceFunctionGroupName(SFG_NAME)
                .setServiceIndex((short) (i + 1))
                .setServiceFunctionName(new SfName(SF_NAME_BASE + (i + 1)));
            servicePathHopList.add(servicePathHopBuilder.build());
        }

        serviceFunctionPathBuilder.setName(SFP_NAME)
            .setKey(new ServiceFunctionPathKey(SFP_NAME))
            .setServicePathHop(servicePathHopList);

        return serviceFunctionPathBuilder.build();
    }

    // write or remove types
    private boolean writeTypes(boolean write) {
        ServiceFunctionTypesBuilder serviceFunctionTypesBuilder = new ServiceFunctionTypesBuilder();
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

        serviceFunctionTypesBuilder.setServiceFunctionType(serviceFunctionTypeList);

        InstanceIdentifier<ServiceFunctionTypes> sftIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sftIID, serviceFunctionTypesBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sftIID, LogicalDatastoreType.CONFIGURATION);
    }
}

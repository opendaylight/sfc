/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SfcProviderServiceTypeAPITest extends AbstractDataStoreManager{

    SfcProviderServiceTypeAPILocal sfcProviderServiceTypeAPILocal;

    @Before
    public void before() {
        setOdlSfc();
        Object[] params = {"hello"};
        sfcProviderServiceTypeAPILocal = new SfcProviderServiceTypeAPILocal(params);
    }

    @Test
    public void testGetPut(){
        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();

        Class serviceFunctionTypeIdentity = Firewall.class;
        serviceFunctionTypeBuilder.setKey(new ServiceFunctionTypeKey(serviceFunctionTypeIdentity)).setType(serviceFunctionTypeIdentity);
        ServiceFunctionType serviceFunctionType = serviceFunctionTypeBuilder.build();
        Object[] serviceTypeObj = {serviceFunctionType};
        Class[] serviceTypeClass = {Firewall.class};
        SfcProviderServiceTypeAPI sfcProviderServiceTypeAPI = SfcProviderServiceTypeAPI.getPut(serviceTypeObj, serviceTypeClass);
        assertNotNull(sfcProviderServiceTypeAPI);
        assertEquals("Incorrectly returned value of SfcProviderServiceTypeAPI.", "putServiceFunctionType", sfcProviderServiceTypeAPI.getMethodName());
    }

    @Test
    public void testPutServiceFunctionType() throws Exception{
        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        Class serviceFunctionTypeIdentity = Firewall.class;
        serviceFunctionTypeBuilder.setKey(new ServiceFunctionTypeKey(serviceFunctionTypeIdentity)).setType(serviceFunctionTypeIdentity);
        ServiceFunctionType serviceFunctionType = serviceFunctionTypeBuilder.build();

        assertTrue(sfcProviderServiceTypeAPILocal.putServiceFunctionType(serviceFunctionType));

        InstanceIdentifier<ServiceFunctionType> sftEntryIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class)
                        .child(ServiceFunctionType.class, serviceFunctionType.getKey()).build();
        ServiceFunctionType serviceFunctionTypeRead = SfcDataStoreAPI.readTransactionAPI(sftEntryIID, LogicalDatastoreType.CONFIGURATION);
        assertEquals(serviceFunctionType, serviceFunctionTypeRead);
    }

    @Test
    public void testDeleteServiceFunctionTypeEntryExecutor(){
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName("SF1").setKey(new ServiceFunctionKey("SF1")).setType(Firewall.class);
        ServiceFunction serviceFunction = serviceFunctionBuilder.build();

        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(serviceFunction.getName());

        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(new ServiceFunctionTypeKey(Firewall.class));
        InstanceIdentifier<SftServiceFunctionName> sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class, sftServiceFunctionNameKey).build();

        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder.setName("SF1").setKey(new SftServiceFunctionNameKey("SF1"));
        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameBuilder.build();

        SfcDataStoreAPI.writePutTransactionAPI(sftentryIID, sftServiceFunctionName, LogicalDatastoreType.CONFIGURATION);

        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntryExecutor(serviceFunction));
        assertNull(SfcDataStoreAPI.readTransactionAPI(sftentryIID, LogicalDatastoreType.CONFIGURATION));
    }

    @Test
    public void testPutAllServiceFunctionTypes() throws Exception{
        ServiceFunctionTypesBuilder serviceFunctionTypesBuilder = new ServiceFunctionTypesBuilder();
        List<ServiceFunctionType> serviceFunctionTypeList = new ArrayList<>();
        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        Class serviceFunctionTypeIdentity = Firewall.class;
        serviceFunctionTypeBuilder.setKey(new ServiceFunctionTypeKey(serviceFunctionTypeIdentity)).setType(serviceFunctionTypeIdentity);
        ServiceFunctionType serviceFunctionType = serviceFunctionTypeBuilder.build();
        serviceFunctionTypeList.add(serviceFunctionType);
        serviceFunctionTypesBuilder.setServiceFunctionType(serviceFunctionTypeList);
        ServiceFunctionTypes serviceFunctionTypes = serviceFunctionTypesBuilder.build();
        assertTrue(sfcProviderServiceTypeAPILocal.putAllServiceFunctionTypes(serviceFunctionTypes));

        InstanceIdentifier<ServiceFunctionTypes> sftEntryIID =
                InstanceIdentifier.builder(ServiceFunctionTypes.class).build();
        ServiceFunctionTypes serviceFunctionTypesRead = SfcDataStoreAPI.readTransactionAPI(sftEntryIID, LogicalDatastoreType.CONFIGURATION);
        assertEquals(serviceFunctionTypes, serviceFunctionTypesRead);
    }

    @Test
    public void testReadAllServiceFunctionTypes(){
        ServiceFunctionTypesBuilder serviceFunctionTypesBuilder = new ServiceFunctionTypesBuilder();
        List<ServiceFunctionType> serviceFunctionTypeList = new ArrayList<>();
        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        Class serviceFunctionTypeIdentity = Firewall.class;
        serviceFunctionTypeBuilder.setKey(new ServiceFunctionTypeKey(serviceFunctionTypeIdentity)).setType(serviceFunctionTypeIdentity);
        ServiceFunctionType serviceFunctionType = serviceFunctionTypeBuilder.build();
        serviceFunctionTypeList.add(serviceFunctionType);
        serviceFunctionTypesBuilder.setServiceFunctionType(serviceFunctionTypeList);
        ServiceFunctionTypes serviceFunctionTypes = serviceFunctionTypesBuilder.build();
        sfcProviderServiceTypeAPILocal.putAllServiceFunctionTypes(serviceFunctionTypes);

        ServiceFunctionTypes outputSFTypes = sfcProviderServiceTypeAPILocal.readAllServiceFunctionTypes();
        assertNotNull("Variable has not been set correctly.", outputSFTypes);
        assertEquals("Types do not match.", serviceFunctionTypes, outputSFTypes);
    }

    @Test
    public void testDeleteAllServiceFunctionTypes(){
        ServiceFunctionTypesBuilder serviceFunctionTypesBuilder = new ServiceFunctionTypesBuilder();
        List<ServiceFunctionType> serviceFunctionTypeList = new ArrayList<>();
        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        Class serviceFunctionTypeIdentity = Firewall.class;
        serviceFunctionTypeBuilder.setKey(new ServiceFunctionTypeKey(serviceFunctionTypeIdentity)).setType(serviceFunctionTypeIdentity);
        ServiceFunctionType serviceFunctionType = serviceFunctionTypeBuilder.build();
        serviceFunctionTypeList.add(serviceFunctionType);
        serviceFunctionTypesBuilder.setServiceFunctionType(serviceFunctionTypeList);
        ServiceFunctionTypes serviceFunctionTypes = serviceFunctionTypesBuilder.build();
        sfcProviderServiceTypeAPILocal.putAllServiceFunctionTypes(serviceFunctionTypes);

        sfcProviderServiceTypeAPILocal.deleteAllServiceFunctionTypes();

        ServiceFunctionTypes outputSFTypes = sfcProviderServiceTypeAPILocal.readAllServiceFunctionTypes();
        assertNull("Variable has not been set correctly.", outputSFTypes);
    }

    private class SfcProviderServiceTypeAPILocal extends SfcProviderServiceTypeAPI{

        SfcProviderServiceTypeAPILocal(Object[] params) {
            super(params, "m");
        }
    }
}
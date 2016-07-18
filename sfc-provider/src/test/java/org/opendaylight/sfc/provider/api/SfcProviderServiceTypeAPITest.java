/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class SfcProviderServiceTypeAPITest extends AbstractDataStoreManager {

    SfcProviderServiceTypeAPI sfcProviderServiceTypeAPILocal;

    @Before
    public void before() {
        setOdlSfc();
        sfcProviderServiceTypeAPILocal = new SfcProviderServiceTypeAPI();
    }

    @Test
    public void testPutServiceFunctionType() throws Exception {
        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        SftTypeName sftType = new SftTypeName("firewall");
        serviceFunctionTypeBuilder.setKey(new ServiceFunctionTypeKey(sftType))
            .setType(sftType)
            .setBidirectionality(false)
            .setNshAware(false)
            .setRequestReclassification(false)
            .setSymmetry(false);
        ServiceFunctionType serviceFunctionType = serviceFunctionTypeBuilder.build();

        assertTrue(SfcProviderServiceTypeAPI.putServiceFunctionType(serviceFunctionType));

        InstanceIdentifier<ServiceFunctionType> sftEntryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, serviceFunctionType.getKey())
            .build();
        ServiceFunctionType serviceFunctionTypeRead =
                SfcDataStoreAPI.readTransactionAPI(sftEntryIID, LogicalDatastoreType.CONFIGURATION);
        assertEquals(serviceFunctionType, serviceFunctionTypeRead);
    }

    @Test
    public void testDeleteServiceFunctionTypeEntryExecutor() {
        SfName sfName = new SfName("SF1");

        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(sfName).setKey(new ServiceFunctionKey(sfName)).setType(new SftTypeName("firewall"));
        ServiceFunction serviceFunction = serviceFunctionBuilder.build();

        SftServiceFunctionNameKey sftServiceFunctionNameKey =
                new SftServiceFunctionNameKey(sfName);

        ServiceFunctionTypeKey serviceFunctionTypeKey =
                new ServiceFunctionTypeKey(new ServiceFunctionTypeKey(new SftTypeName("firewall")));
        InstanceIdentifier<SftServiceFunctionName> sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
            .child(ServiceFunctionType.class, serviceFunctionTypeKey)
            .child(SftServiceFunctionName.class, sftServiceFunctionNameKey)
            .build();

        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder.setName(sfName)
            .setKey(new SftServiceFunctionNameKey(sfName));
        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameBuilder.build();

        SfcDataStoreAPI.writePutTransactionAPI(sftentryIID, sftServiceFunctionName, LogicalDatastoreType.CONFIGURATION);

        assertTrue(SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(serviceFunction));
        assertNull(SfcDataStoreAPI.readTransactionAPI(sftentryIID, LogicalDatastoreType.CONFIGURATION));
    }
}

/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.ServiceFunctionGroupAlgorithms;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithm;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithmKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SfcProviderServiceFunctionGroupAlgAPITest extends AbstractDataStoreManager {

    @Before
    public void before() {
        setOdlSfc();
    }

    @Test
    public void readServiceFunctionGroupAlgTest(){

        ServiceFunctionGroupAlgorithmBuilder serviceFunctionGroupAlgorithmBuilder = new ServiceFunctionGroupAlgorithmBuilder();
        serviceFunctionGroupAlgorithmBuilder.setName("SFG1").setKey(new ServiceFunctionGroupAlgorithmKey("SFG1"));
        ServiceFunctionGroupAlgorithm serviceFunctionGroupAlgorithm = serviceFunctionGroupAlgorithmBuilder.build();
        ServiceFunctionGroupAlgorithmKey serviceFunctionGroupAlgorithmKey = new ServiceFunctionGroupAlgorithmKey("SFG1");
        InstanceIdentifier<ServiceFunctionGroupAlgorithm> sfgAlgIID = InstanceIdentifier.builder(ServiceFunctionGroupAlgorithms.class).child(ServiceFunctionGroupAlgorithm.class, serviceFunctionGroupAlgorithmKey).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfgAlgIID, serviceFunctionGroupAlgorithm, LogicalDatastoreType.CONFIGURATION);

        assertNotNull(SfcProviderServiceFunctionGroupAlgAPI.readServiceFunctionGroupAlg("SFG1"));

        ServiceFunctionGroupAlgorithm sfga = SfcDataStoreAPI.readTransactionAPI(sfgAlgIID, LogicalDatastoreType.CONFIGURATION);
        assertEquals(serviceFunctionGroupAlgorithm, sfga);
    }
}

/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/*
package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarderKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SfcProviderServiceClassifierAPITest extends AbstractDataStoreManager {

    @Before
    public void setUp() throws Exception {
        setOdlSfc();
    }

    private void writeClassifierToStore(ServiceFunctionClassifier clsf) {
        InstanceIdentifier<ServiceFunctionClassifier> clsfId =
                InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                    .child(ServiceFunctionClassifier.class, new ServiceFunctionClassifierKey(clsf.getName()))
                    .build();
        boolean result = SfcDataStoreAPI.writePutTransactionAPI(clsfId, clsf, LogicalDatastoreType.CONFIGURATION);
        assertTrue("Failed to write classifier to data store.", result);
    }

    private static ServiceFunctionClassifier createClassifier(String clsfName, String accessList, String sffName) {
        SclServiceFunctionForwarderBuilder sffBuilder = new SclServiceFunctionForwarderBuilder();
        sffBuilder.setName(sffName);
        sffBuilder.setKey(new SclServiceFunctionForwarderKey(sffName));
        List<SclServiceFunctionForwarder> sfForwarders = new ArrayList<SclServiceFunctionForwarder>();
        sfForwarders.add(sffBuilder.build());

        ServiceFunctionClassifierBuilder clsfBuilder = new ServiceFunctionClassifierBuilder();
        clsfBuilder.setName(clsfName);
        clsfBuilder.setKey(new ServiceFunctionClassifierKey(clsfName));
        clsfBuilder.setAccessList(accessList);
        clsfBuilder.setSclServiceFunctionForwarder(sfForwarders);
        return clsfBuilder.build();
    }

    private static void assertClassifierDoesNotExists(String clsfName) {
        ServiceFunctionClassifier clsf = SfcProviderServiceClassifierAPI.readServiceClassifier(clsfName);
        assertNull("Unexpected classifier found.", clsf);
    }

    private static void readAndAssertClassifier(ServiceFunctionClassifier expectedClsf) {
        ServiceFunctionClassifier clsf = SfcProviderServiceClassifierAPI.readServiceClassifier(expectedClsf.getName());
        assertNotNull("Classifier not found.", clsf);
        assertEquals(expectedClsf.getName(), clsf.getName());
        assertEquals(expectedClsf.getKey().getName(), clsf.getKey().getName());
        assertEquals(expectedClsf.getAccessList(), clsf.getAccessList());
        assertEquals(expectedClsf.getSclServiceFunctionForwarder(), clsf.getSclServiceFunctionForwarder());
    }

}
*/

/*
 * Copyright (c) 2016, 2018 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.Acl;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;

/**
 * Test Suite to test the ServiceFunctionClassifierListener class.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */
public class ServiceFunctionClassifierListenerTest extends AbstractDataStoreManager {

    private static final String SFC_NAME = "listernerSFC";
    private static final String ACL_NAME = "aclName";
    private static final String ACL_NAME2 = "aclName2";
    private static final java.lang.Class<? extends AclBase> ACL_TYPE = Ipv4Acl.class;

    // Class under test
    private ServiceFunctionClassifierListener serviceFunctionClassifierListener;

    @Before
    public void before() {
        setupSfc();
        serviceFunctionClassifierListener = new ServiceFunctionClassifierListener(getDataBroker());
        serviceFunctionClassifierListener.register();
    }

    @After
    public void after() throws Exception {
        serviceFunctionClassifierListener.close();
        close();
    }

    /**
     * Test that creates a Service Function Classifier, calls listener
     * explicitly, verify that (ACL, Classifier) entry into ACL was created and
     * cleans up.
     */
    @Test
    public void testOnServiceFunctionClassifierCreated() throws Exception {
        ServiceFunctionClassifier serviceFunctionClassifier = buildServiceFunctionClassifier();

        // We trigger the adding of <ACL, Classifier> entry into ACL
        serviceFunctionClassifierListener.add(serviceFunctionClassifier);

        Thread.sleep(500);

        // We verify the adding of <ACL, Classifier> entry into ACL
        AccessListState acl = SfcProviderAclAPI.readAccessListState(serviceFunctionClassifier.getAcl().getName(),
                serviceFunctionClassifier.getAcl().getType());
        assertNotNull(acl);
        // Clean up
        assertTrue(SfcProviderAclAPI.deleteClassifierFromAccessListState(serviceFunctionClassifier.getAcl().getName(),
                serviceFunctionClassifier.getAcl().getType(), serviceFunctionClassifier.getName()));

    }

    /**
     * Test that deletes a Service Function Classifier, calls listener
     * explicitly, verify that (ACL, Classifier) entry into ACL was removed and
     * cleans up.
     */
    @Test
    public void testOnServiceFunctionClassifierRemoved() throws Exception {
        // We create a ServiceFunctionClassifier
        ServiceFunctionClassifier serviceFunctionClassifier = buildServiceFunctionClassifier();
        assertNull(SfcProviderAclAPI.readAccessListState(serviceFunctionClassifier.getAcl().getName(),
                serviceFunctionClassifier.getAcl().getType()));

        // We create a <ACL, Classifier> entry into ACL
        assertTrue(SfcProviderAclAPI.addClassifierToAccessListState(serviceFunctionClassifier.getAcl().getName(),
                serviceFunctionClassifier.getAcl().getType(), serviceFunctionClassifier.getName()));

        List<AclServiceFunctionClassifier> sfc = SfcProviderAclAPI
                .readAccessListState(serviceFunctionClassifier.getAcl().getName(),
                        serviceFunctionClassifier.getAcl().getType())
                .getAclServiceFunctionClassifier();
        assertTrue(sfc.get(0).getName().equals(SFC_NAME));

        serviceFunctionClassifierListener.remove(serviceFunctionClassifier);

        Thread.sleep(500);

        // We verify the removal of <ACL, Classifier> entry from ACL by checking
        // no ServiceFunctionClassifier is assigned to the ACL
        assertEquals(SfcProviderAclAPI.readAccessListState(serviceFunctionClassifier.getAcl().getName(),
                serviceFunctionClassifier.getAcl().getType()).getAclServiceFunctionClassifier().size(), 0);
    }

    /**
     * Test that updates a Service Function Classifier, calls listener
     * explicitly, verify that (ACL, Classifier) entry into ACL was updated and
     * cleans up.
     */
    @Test
    public void testOnServiceFunctionClassifierUpdated() throws Exception {
        // We create the Original ServiceFunctionClassifier
        ServiceFunctionClassifier originalServiceFunctionClassifier = buildServiceFunctionClassifier();
        assertNull(SfcProviderAclAPI.readAccessListState(originalServiceFunctionClassifier.getAcl().getName(),
                originalServiceFunctionClassifier.getAcl().getType()));

        // We create a <ACL, Classifier> entry into ACL
        assertTrue(SfcProviderAclAPI.addClassifierToAccessListState(
                originalServiceFunctionClassifier.getAcl().getName(),
                originalServiceFunctionClassifier.getAcl().getType(), originalServiceFunctionClassifier.getName()));

        List<AclServiceFunctionClassifier> sfc = SfcProviderAclAPI
                .readAccessListState(originalServiceFunctionClassifier.getAcl().getName(),
                        originalServiceFunctionClassifier.getAcl().getType())
                .getAclServiceFunctionClassifier();
        assertTrue(sfc.get(0).getName().equals(SFC_NAME));

        // Now we prepare the Updated ServiceFunctionClassifier. We change the
        // original ACL name
        ServiceFunctionClassifierBuilder updatedServiceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder(
                originalServiceFunctionClassifier);
        AclBuilder aclBuilder2 = new AclBuilder();
        aclBuilder2.setName(ACL_NAME2);
        aclBuilder2.setType(ACL_TYPE);

        Acl dummyACL = aclBuilder2.build();
        updatedServiceFunctionClassifierBuilder.setAcl(dummyACL);
        ServiceFunctionClassifier updatedServiceFunctionClassifier = updatedServiceFunctionClassifierBuilder.build();

        serviceFunctionClassifierListener.update(originalServiceFunctionClassifier, updatedServiceFunctionClassifier);

        Thread.sleep(500);

        // We verify the removal of the Original <ACL, Classifier> entry from
        // ACL by checking
        // no ServiceFunctionClassifier is assigned to the ACL
        assertEquals(
                SfcProviderAclAPI
                        .readAccessListState(originalServiceFunctionClassifier.getAcl().getName(),
                                originalServiceFunctionClassifier.getAcl().getType())
                        .getAclServiceFunctionClassifier().size(),
                0);

        // We verify the addition of the Updated <ACL, Classifier> entry to ACL
        // by checking
        // the ServiceFunctionClassifier is assigned to the new ACL

        assertEquals(SfcProviderAclAPI.readAccessListState(updatedServiceFunctionClassifier.getAcl().getName(),
                updatedServiceFunctionClassifier.getAcl().getType()).getAclName(), ACL_NAME2);
        sfc = SfcProviderAclAPI.readAccessListState(updatedServiceFunctionClassifier.getAcl().getName(),
                updatedServiceFunctionClassifier.getAcl().getType()).getAclServiceFunctionClassifier();
        assertTrue(sfc.get(0).getName().equals(SFC_NAME));

    }

    /**
     * Builds a complete Service Function Classifier Object.
     *
     * @return ServiceFunctionClassifier object
     */
    private ServiceFunctionClassifier buildServiceFunctionClassifier() {
        AclBuilder aclBuilder = new AclBuilder();
        aclBuilder.setName(ACL_NAME);
        aclBuilder.setType(ACL_TYPE);
        Acl dummyACL = aclBuilder.build();
        ServiceFunctionClassifierBuilder sfcBuilder = new ServiceFunctionClassifierBuilder();
        sfcBuilder.setName(SFC_NAME);
        sfcBuilder.setAcl(dummyACL);

        return sfcBuilder.build();
    }
}

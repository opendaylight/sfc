/*
 * Copyright (c) 2016, 2018 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.listeners;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.scfofrenderer.processors.SfcScfOfProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.Acl;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;

/**
 * Test Suite to test the SfcScfOfDataListener class.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */

public class SfcScfOfDataListenerTest {

    private static final String SFC_NAME = "listenerSFC";
    private static final String ACL_NAME = "aclName";
    private static final String ACL_NAME2 = "aclName2";
    private static final java.lang.Class<? extends AclBase> ACL_TYPE = Ipv4Acl.class;

    @Mock
    private DataBroker dataProvider;

    @Mock
    private SfcScfOfProcessor sfcScfOfProcessor;

    // Class under test
    private SfcScfOfDataListener sfcScfOfDataListener;

    @Before
    public void before() {
        initMocks(this);
        sfcScfOfDataListener = new SfcScfOfDataListener(dataProvider, sfcScfOfProcessor);
        sfcScfOfDataListener.register();
    }

    @After
    public void after() {
        sfcScfOfDataListener.close();
    }

    /**
     * Test that creates a Service Function Classifier, calls listener
     * explicitly.
     */
    @Test
    public void testOnSfcScfOfDataCreated() throws Exception {
        ServiceFunctionClassifier serviceFunctionClassifier = buildServiceFunctionClassifier();

        sfcScfOfDataListener.add(serviceFunctionClassifier);

        Thread.sleep(500);

        // We verify createdServiceFunctionClassifier has been called
        verify(sfcScfOfProcessor).createdServiceFunctionClassifier(serviceFunctionClassifier);
    }

    /**
     * Test that deletes a Service Function Classifier, calls listener
     * explicitly.
     */
    @Test
    public void testOnSfcScfOfDataRemoved() throws Exception {
        ServiceFunctionClassifier serviceFunctionClassifier = buildServiceFunctionClassifier();

        sfcScfOfDataListener.remove(serviceFunctionClassifier);

        Thread.sleep(500);

        // We verify deletedServiceFunctionClassifier has been called
        verify(sfcScfOfProcessor).deletedServiceFunctionClassifier(serviceFunctionClassifier);
    }

    /**
     * Test that updates a Service Function Classifier, calls listener
     * explicitly.
     */
    @Test
    public void testOnSfcScfOfDataUpdated() throws Exception {
        ServiceFunctionClassifier originalServiceFunctionClassifier = buildServiceFunctionClassifier();
        sfcScfOfProcessor.createdServiceFunctionClassifier(originalServiceFunctionClassifier);

        // Now we prepare the Updated ServiceFunctionClassifier. We change the
        // original ACL name
        ServiceFunctionClassifierBuilder updatedServiceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder(
                originalServiceFunctionClassifier);
        AclBuilder aclBuilder2 = new AclBuilder();
        aclBuilder2.setName(ACL_NAME2);
        aclBuilder2.setType(ACL_TYPE);
        Acl aclDummy2 = aclBuilder2.build();
        updatedServiceFunctionClassifierBuilder.setAcl(aclDummy2);
        ServiceFunctionClassifier updatedServiceFunctionClassifier = updatedServiceFunctionClassifierBuilder.build();

        sfcScfOfDataListener.update(originalServiceFunctionClassifier, updatedServiceFunctionClassifier);

        Thread.sleep(500);

        // We verify createdServiceFunctionClassifier and
        // deletedServiceFunctionClassifier have been called
        verify(sfcScfOfProcessor).deletedServiceFunctionClassifier(originalServiceFunctionClassifier);
        verify(sfcScfOfProcessor).createdServiceFunctionClassifier(updatedServiceFunctionClassifier);
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
        Acl aclDummy = aclBuilder.build();
        ServiceFunctionClassifierBuilder sfcBuilder = new ServiceFunctionClassifierBuilder();
        sfcBuilder.setName(SFC_NAME);
        sfcBuilder.setAcl(aclDummy);

        return sfcBuilder.build();
    }
}

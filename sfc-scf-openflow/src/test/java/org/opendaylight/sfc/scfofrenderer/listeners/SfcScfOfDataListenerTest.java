/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.listeners;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.sfc.scfofrenderer.processors.SfcScfOfProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.Acl;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;
import java.util.ArrayList;
import java.util.Collection;
import static org.mockito.Mockito.*;



/**
 * Test Suite to test the SfcScfOfDataListener class.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */

public class SfcScfOfDataListenerTest {

    private final Collection<DataTreeModification<ServiceFunctionClassifier>> collection = new ArrayList<>();
    private DataTreeModification<ServiceFunctionClassifier> dataTreeModification;
    private DataObjectModification<ServiceFunctionClassifier> dataObjectModification;

    private DataBroker dataProvider;
    private SfcScfOfProcessor sfcScfOfProcessor;

    private final String SFC_NAME = "listernerSFC";
    private final String ACL_NAME = "aclName";
    private final String ACL_NAME2 = "aclName2";
    private final java.lang.Class<? extends AclBase> ACL_TYPE = Ipv4Acl.class;

    // Class under test
    private SfcScfOfDataListener sfcScfOfDataListener;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        dataProvider = mock(DataBroker.class);
        sfcScfOfProcessor = mock(SfcScfOfProcessor.class);


        dataTreeModification = mock(DataTreeModification.class);
        dataObjectModification = mock(DataObjectModification.class);
        sfcScfOfDataListener = new SfcScfOfDataListener(dataProvider,sfcScfOfProcessor);
        sfcScfOfDataListener.init();
    }

    @After
    public void after() throws Exception {
        sfcScfOfDataListener.close();
    }

    /**
     * Test that creates a Service Function Classifier, calls listener explicitly
     */
    @Test
    public void testOnSfcScfOfDataCreated() throws Exception {
        ServiceFunctionClassifier serviceFunctionClassifier = buildServiceFunctionClassifier();

        // We trigger the adding of a Service Function Classifier

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.WRITE);
        when(dataObjectModification.getDataBefore()).thenReturn(null);
        when(dataObjectModification.getDataAfter()).thenReturn(serviceFunctionClassifier);

        collection.add(dataTreeModification);
        sfcScfOfDataListener.onDataTreeChanged(collection);

        Thread.sleep(500);

        // We verify createdServiceFunctionClassifier has been called
        verify(sfcScfOfProcessor).createdServiceFunctionClassifier(serviceFunctionClassifier);

    }

    /**
     * Test that deletes a Service Function Classifier, calls listener explicitly
     */
    @Test
    public void testOnSfcScfOfDataRemoved() throws Exception {
        ServiceFunctionClassifier serviceFunctionClassifier = buildServiceFunctionClassifier();

        // We trigger the removal of a Service Function Classifier

        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.DELETE);
        when(dataObjectModification.getDataBefore()).thenReturn(serviceFunctionClassifier);

        collection.add(dataTreeModification);
        sfcScfOfDataListener.onDataTreeChanged(collection);

        Thread.sleep(500);

        // We verify deletedServiceFunctionClassifier has been called
        verify(sfcScfOfProcessor).deletedServiceFunctionClassifier(serviceFunctionClassifier);

    }

    /**
     * Test that updates a Service Function Classifier, calls listener explicitly
     */
    @Test
    public void testOnSfcScfOfDataUpdated() throws Exception {
        ServiceFunctionClassifier originalServiceFunctionClassifier = buildServiceFunctionClassifier();
        sfcScfOfProcessor.createdServiceFunctionClassifier(originalServiceFunctionClassifier);

        // Now we prepare the Updated ServiceFunctionClassifier. We change the original ACL name
        ServiceFunctionClassifierBuilder updatedServiceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder(originalServiceFunctionClassifier);
        AclBuilder aclBuilder2 = new AclBuilder();
        aclBuilder2.setName(ACL_NAME2);
        aclBuilder2.setType(ACL_TYPE);
        Acl acl_dummy2 = aclBuilder2.build();
        updatedServiceFunctionClassifierBuilder.setAcl(acl_dummy2);
        ServiceFunctionClassifier updatedServiceFunctionClassifier = updatedServiceFunctionClassifierBuilder.build();

        // We trigger the updating of a Service Function Classifier
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(dataObjectModification.getDataBefore()).thenReturn(originalServiceFunctionClassifier);
        when(dataObjectModification.getDataAfter()).thenReturn(updatedServiceFunctionClassifier);

        collection.add(dataTreeModification);
        sfcScfOfDataListener.onDataTreeChanged(collection);

        Thread.sleep(500);

        // We verify createdServiceFunctionClassifier and deletedServiceFunctionClassifier have been called
        verify(sfcScfOfProcessor).deletedServiceFunctionClassifier(originalServiceFunctionClassifier);
        verify(sfcScfOfProcessor).createdServiceFunctionClassifier(updatedServiceFunctionClassifier);

    }

    /**
     * Builds a complete Service Function Classifier Object
     *
     * @return ServiceFunctionClassifier object
     */
    private ServiceFunctionClassifier buildServiceFunctionClassifier() {
        AclBuilder aclBuilder = new AclBuilder();
        aclBuilder.setName(ACL_NAME);
        aclBuilder.setType(ACL_TYPE);
        Acl acl_dummy = aclBuilder.build();
        ServiceFunctionClassifierBuilder sfcBuilder = new ServiceFunctionClassifierBuilder();
        sfcBuilder.setName(SFC_NAME);
        sfcBuilder.setAcl(acl_dummy);

        return sfcBuilder.build();
    }


}

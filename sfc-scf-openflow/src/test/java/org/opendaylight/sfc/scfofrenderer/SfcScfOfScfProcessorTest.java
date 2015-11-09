/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntries;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcProviderAclAPI.class)
public class SfcScfOfScfProcessorTest {

    private SfcScfOfScfProcessor sfcScfProcessor;
    private ServiceFunctionClassifier scf;
    private Acl acl;
    private AccessListEntries accessListEntries;


    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        sfcScfProcessor = new SfcScfOfScfProcessor();
        scf = mock(ServiceFunctionClassifier.class);
        acl = mock(Acl.class);
        accessListEntries = mock(AccessListEntries.class);

        when(scf.getAccessList()).thenReturn("acl");
        when(acl.getAclName()).thenReturn("aclName");

        PowerMockito.stub(PowerMockito.method(SfcProviderAclAPI.class, "readAccessList"))
            .toReturn(acl);
    }

    @Test
    public void createdServiceFunctionClassifier() {
        sfcScfProcessor.createdServiceFunctionClassifier(scf);
    }

    @Test
    public void deletedServiceFunctionClassifier() {
        sfcScfProcessor.deletedServiceFunctionClassifier(scf);
    }
}

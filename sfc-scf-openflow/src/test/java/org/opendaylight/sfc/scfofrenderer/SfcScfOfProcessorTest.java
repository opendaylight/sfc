/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;


@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcProviderAclAPI.class, SfcProviderServiceForwarderAPI.class, SfcOvsPortUtils.class, SfcScfOfUtils.class})
public class SfcScfOfProcessorTest {

    private SfcScfOfProcessor sfcScfProcessor;
    private ServiceFunctionClassifier scf;
    private Acl acl;
    private AccessListEntries accessListEntries;
    private List<Ace> acesList;
    private List<SclServiceFunctionForwarder> sfflist;
    private ServiceFunctionForwarder sff;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        sfcScfProcessor = new SfcScfOfProcessor();
        scf = mock(ServiceFunctionClassifier.class);
        acl = mock(Acl.class);
        Ace ace = mock(Ace.class);
        acesList = new ArrayList<Ace>();
        acesList.add(ace);
        accessListEntries = mock(AccessListEntries.class);

        sfflist = new ArrayList<SclServiceFunctionForwarder>();
        SclServiceFunctionForwarder sclSff = mock(SclServiceFunctionForwarder.class);
        sfflist.add(sclSff);

        when(sclSff.getName()).thenReturn("sffName");
        when(scf.getSclServiceFunctionForwarder()).thenReturn(sfflist);
        when(scf.getAccessList()).thenReturn("acl");
        when(acl.getAclName()).thenReturn("aclName");
        when(acl.getAccessListEntries()).thenReturn(accessListEntries);
        when(accessListEntries.getAce()).thenReturn(acesList);
  
        PowerMockito.stub(PowerMockito.method(SfcProviderAclAPI.class, "readAccessList"))
            .toReturn(acl);

        sff = mock(ServiceFunctionForwarder.class);
        PowerMockito.stub(PowerMockito.method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder"))
            .toReturn(sff);
        
        PowerMockito.stub(PowerMockito.method(SfcOvsPortUtils.class, "getSffOpenFlowNodeName"))
            .toReturn("sff");

        PowerMockito.stub(PowerMockito.method(SfcOvsPortUtils.class, "getVxlanOfPort"))
            .toReturn(0L);

        PowerMockito.stub(PowerMockito.method(SfcScfOfUtils.class, "initClassifierTable"))
            .toReturn(true);

        PowerMockito.stub(PowerMockito.method(SfcScfOfUtils.class, "createClassifierOutFlow"))
            .toReturn(true);

        PowerMockito.stub(PowerMockito.method(SfcScfOfUtils.class, "createClassifierInFlow"))
            .toReturn(true);

        PowerMockito.stub(PowerMockito.method(SfcScfOfUtils.class, "createClassifierRelayFlow"))
            .toReturn(true);
    }

    @Test
    public void createdServiceFunctionClassifier() {
        assertFalse(sfcScfProcessor.createdServiceFunctionClassifier(null));
        assertTrue(sfcScfProcessor.createdServiceFunctionClassifier(scf));
    }

    @Test
    public void deletedServiceFunctionClassifier() {
        assertFalse(sfcScfProcessor.deletedServiceFunctionClassifier(null));
        assertTrue(sfcScfProcessor.deletedServiceFunctionClassifier(scf));
    }
}

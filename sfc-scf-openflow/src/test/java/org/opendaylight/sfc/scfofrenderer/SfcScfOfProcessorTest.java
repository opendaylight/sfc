/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;
@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcProviderAclAPI.class, SfcProviderServiceForwarderAPI.class, SfcOvsUtil.class, SfcScfOfUtils.class})
public class SfcScfOfProcessorTest {

    private SfcScfOfProcessor sfcScfProcessor;
    private ServiceFunctionClassifier scf;
    private Acl acl;
    private AccessListEntries accessListEntries;
    private List<Ace> acesList;
    private List<SclServiceFunctionForwarder> sfflist;
    private ServiceFunctionForwarder sff;

    private void initTest() {
        sfcScfProcessor = new SfcScfOfProcessor();
        scf = mock(ServiceFunctionClassifier.class);
        acl = mock(Acl.class);
        Ace ace = mock(Ace.class);
        acesList = new ArrayList<>();
        acesList.add(ace);
        accessListEntries = mock(AccessListEntries.class);

        sfflist = new ArrayList<>();
        SclServiceFunctionForwarder sclSff = mock(SclServiceFunctionForwarder.class);
        sfflist.add(sclSff);

        when(sclSff.getName()).thenReturn("sffName");
        when(scf.getSclServiceFunctionForwarder()).thenReturn(sfflist);
        when(scf.getAcl()).thenReturn(mock(org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.Acl.class));
        when(acl.getAclName()).thenReturn("aclName");
        when(acl.getAccessListEntries()).thenReturn(accessListEntries);
        when(accessListEntries.getAce()).thenReturn(acesList);

        PowerMockito.stub(PowerMockito.method(SfcProviderAclAPI.class, "readAccessList"))
            .toReturn(acl);

        sff = mock(ServiceFunctionForwarder.class);
        PowerMockito.stub(PowerMockito.method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder"))
            .toReturn(sff);

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOpenFlowNodeIdForSff"))
            .toReturn("sff");

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort"))
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
    public void testCreatedServiceFunctionClassifier() {
        initTest();
        assertFalse(sfcScfProcessor.createdServiceFunctionClassifier(null));

        initTest();
        assertTrue(sfcScfProcessor.createdServiceFunctionClassifier(scf));

        initTest();
        when(acl.getAccessListEntries()).thenReturn(null);
        assertFalse(sfcScfProcessor.createdServiceFunctionClassifier(scf));

        initTest();
        when(accessListEntries.getAce()).thenReturn(null);
        assertFalse(sfcScfProcessor.createdServiceFunctionClassifier(scf));

        initTest();
        PowerMockito.stub(PowerMockito.method(SfcProviderAclAPI.class, "readAccessList"))
            .toReturn(null);
        assertFalse(sfcScfProcessor.createdServiceFunctionClassifier(scf));

        initTest();
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOpenFlowNodeIdForSff"))
            .toReturn(null);
        assertTrue(sfcScfProcessor.createdServiceFunctionClassifier(scf));

        initTest();
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort"))
            .toReturn(null);
        assertTrue(sfcScfProcessor.createdServiceFunctionClassifier(scf));
    }

    @Test
    public void testDeletedServiceFunctionClassifier() {
        initTest();
        assertFalse(sfcScfProcessor.deletedServiceFunctionClassifier(null));

        initTest();
        assertTrue(sfcScfProcessor.deletedServiceFunctionClassifier(scf));

        initTest();
        when(acl.getAccessListEntries()).thenReturn(null);
        assertFalse(sfcScfProcessor.deletedServiceFunctionClassifier(scf));

        initTest();
        when(accessListEntries.getAce()).thenReturn(null);
        assertFalse(sfcScfProcessor.deletedServiceFunctionClassifier(scf));

        initTest();
        PowerMockito.stub(PowerMockito.method(SfcProviderAclAPI.class, "readAccessList"))
            .toReturn(null);
        assertFalse(sfcScfProcessor.deletedServiceFunctionClassifier(scf));

        initTest();
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOpenFlowNodeIdForSff"))
            .toReturn(null);
        assertTrue(sfcScfProcessor.deletedServiceFunctionClassifier(scf));

        initTest();
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort"))
            .toReturn(null);
        assertTrue(sfcScfProcessor.deletedServiceFunctionClassifier(scf));
    }
}

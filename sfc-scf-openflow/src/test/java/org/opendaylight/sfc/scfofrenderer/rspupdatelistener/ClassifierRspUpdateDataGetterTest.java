/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.rspupdatelistener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcDataStoreAPI.class)
public class ClassifierRspUpdateDataGetterTest {
    @Mock
    AccessLists accessLists;

    @Mock
    ServiceFunctionClassifiers classifierContainer;

    @Mock
    ServiceFunctionClassifier theClassifier;

    @Mock
    SclServiceFunctionForwarder sclClassifier;

    @Mock
    Acl acl;

    @Mock
    AccessListEntries aces;

    @Mock
    Ace ace;

    @Mock
    Actions actions;

    @Mock
    Actions1 findAbetterNameForThis;

    @Mock
    AclRenderedServicePath aclRsp;

    @Mock
    org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.Acl classifierAcl;

    @Mock
    SclServiceFunctionForwarder sclForwarder;

    private List<Acl> aclList = new ArrayList<>();

    private List<Ace> aceList = new ArrayList<>();

    private List<ServiceFunctionClassifier> classifierList = new ArrayList<>();

    private List<SclServiceFunctionForwarder> sclClassifierList = new ArrayList<>();

    public ClassifierRspUpdateDataGetterTest() {
        initMocks(this);
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SfcDataStoreAPI.class);

        when(accessLists.getAcl()).thenReturn(aclList);

        aclList.add(acl);

        when(acl.getAclName()).thenReturn("acl1");

        when(acl.getAccessListEntries()).thenReturn(aces);

        when(aces.getAce()).thenReturn(aceList);

        aceList.add(ace);

        when(ace.getActions()).thenReturn(actions);

        when(actions.getAugmentation(eq(Actions1.class))).thenReturn(findAbetterNameForThis);

        when(findAbetterNameForThis.getSfcAction()).thenReturn(aclRsp);

        when(aclRsp.getRenderedServicePath()).thenReturn("rsp1");


        when(classifierAcl.getName()).thenReturn("acl1");

        when(theClassifier.getAcl()).thenReturn(classifierAcl);

        classifierList.add(theClassifier);

        when(sclForwarder.getName()).thenReturn("c1");

        sclClassifierList.add(sclForwarder);

        when(theClassifier.getSclServiceFunctionForwarder()).thenReturn(sclClassifierList);
    }

    @Test
    public void testAclFilteringByRspName() {
        PowerMockito.when(SfcDataStoreAPI
                .readTransactionAPI(any(), any())).thenReturn(accessLists);
        RspName theRspName = new RspName("rsp1");

        ClassifierRspUpdateDataGetter rspUpdateListener = new ClassifierRspUpdateDataGetter();
        Assert.assertFalse(rspUpdateListener.filterAclsByRspName(theRspName).isEmpty());
        Assert.assertEquals(1, rspUpdateListener.filterAclsByRspName(theRspName).size());
        Assert.assertEquals("acl1", rspUpdateListener.filterAclsByRspName(theRspName).get(0).getAclName());
    }

    @Test
    public void testClassifierFilteringByAclName() {
        PowerMockito.when(SfcDataStoreAPI
                .readTransactionAPI(any(), any())).thenReturn(classifierContainer);

        when(classifierContainer.getServiceFunctionClassifier()).thenReturn(classifierList);

        ClassifierRspUpdateDataGetter rspUpdateListener = new ClassifierRspUpdateDataGetter();
        Assert.assertFalse(rspUpdateListener.filterClassifierNodesByAclName("acl1").isEmpty());
        Assert.assertEquals(1, rspUpdateListener.filterClassifierNodesByAclName("acl1").size());
        Assert.assertEquals("c1", rspUpdateListener.filterClassifierNodesByAclName("acl1").get(0).getName());
    }
}

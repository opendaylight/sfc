/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.processors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterImpl;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        SfcProviderAclAPI.class,
        SfcProviderServiceForwarderAPI.class,
        SfcProviderRenderedPathAPI.class,
        SfcOvsUtil.class,
        SfcScfOfUtils.class,
        OpenflowClassifierProcessor.class,
        SfcNshHeader.class})
public class SfcScfOfProcessorTest {

    private SfcScfOfProcessor sfcScfProcessor;
    private ServiceFunctionClassifier scf;
    private Acl acl;
    private AccessListEntries accessListEntries;
    private List<Ace> acesList;
    private List<SclServiceFunctionForwarder> sfflist;
    private ServiceFunctionForwarder sff;
    private DataBroker dataBroker;


    private void initTest() {
        ReadWriteTransaction readWriteTransaction = mock(ReadWriteTransaction.class);

        dataBroker = mock(DataBroker.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(readWriteTransaction);

        SfcOfFlowWriterInterface openflowWriter = spy(new SfcOfFlowWriterImpl(dataBroker));
        Mockito.doNothing().when(openflowWriter).flushFlows();

        OpenflowClassifierProcessor classifierProcessor = mock(OpenflowClassifierProcessor.class);
        when(classifierProcessor.processClassifier(any(SclServiceFunctionForwarder.class), any(Acl.class), anyBoolean()))
                .thenReturn(Collections.emptyList());

        sfcScfProcessor = new SfcScfOfProcessor(openflowWriter, classifierProcessor);

        scf = mock(ServiceFunctionClassifier.class);
        acl = mock(Acl.class);

        // must mock the ACE object (the ACL *must* figure at least one ACE)
        acesList = new ArrayList<Ace>() {{ add(mock(Ace.class)); }};
        accessListEntries = mock(AccessListEntries.class);

        sfflist = new ArrayList<>();
        SclServiceFunctionForwarder sclSff = mock(SclServiceFunctionForwarder.class);
        sfflist.add(sclSff);

        // mock the classifier
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

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getOvsPort"))
                .toReturn(2L);

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getVxlanOfPort"))
            .toReturn(0L);
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

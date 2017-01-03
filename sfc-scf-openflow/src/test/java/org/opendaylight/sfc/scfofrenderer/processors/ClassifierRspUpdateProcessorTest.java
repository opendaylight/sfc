/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.processors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.ClassifierAclDataBuilder;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.LogicallyAttachedClassifier;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.BridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcProviderServiceForwarderAPI.class,
        SfcNshHeader.class})
public class ClassifierRspUpdateProcessorTest {
    @Mock
    private LogicallyAttachedClassifier classifierInterface;

    @Mock
    private RenderedServicePath newRsp;

    @Mock
    private SclServiceFunctionForwarder sffClassifier;

    @Mock
    private ServiceFunctionClassifier scf;

    @Mock
    private Acl acl;

    @Mock
    private AccessListEntries accessListEntries;

    @Mock
    private ServiceFunctionForwarder sff;

    @Mock
    private FlowBuilder theFlow;

    private FlowDetails theFlowDetails;

    private final ClassifierRspUpdateProcessor theUpdateProcessor;

    private static final String FIRST_SF_DPN_ID_STRING = "1234567890";
    private static final String FIRST_SF_NODE_NAME = String.format("openflow:%s", FIRST_SF_DPN_ID_STRING);
    private static final String INTERFACE_TO_CLASSIFY = "750135c0-67a9-4fc1-aac0-1359ae7944d4";

    public ClassifierRspUpdateProcessorTest() {
        initMocks(this);
        theUpdateProcessor = new ClassifierRspUpdateProcessor(classifierInterface);
    }

    @Before
    public void setUp() {
        theFlowDetails = new ClassifierHandler().addRspRelatedFlowIntoNode(FIRST_SF_NODE_NAME, theFlow, 666L);

        Mockito.when(classifierInterface.getNodeName(any(String.class))).thenReturn(Optional.of(FIRST_SF_NODE_NAME));
        Mockito.when(classifierInterface.getInPort(any(String.class), any(String.class))).thenReturn(Optional.of(2L));

        Mockito.when(classifierInterface.createClassifierOutFlow(any(String.class),
                any(Match.class),
                any(SfcNshHeader.class),
                any(String.class))).thenReturn(theFlowDetails);

        Mockito.when(classifierInterface.initClassifierTable(any(String.class))).thenReturn(theFlowDetails);

        when(sffClassifier.getName()).thenReturn("sffName");
        when(sffClassifier.getAttachmentPointType())
                .thenReturn(new InterfaceBuilder().setInterface(INTERFACE_TO_CLASSIFY).build());

        when(scf.getSclServiceFunctionForwarder())
                .thenReturn(new ArrayList<SclServiceFunctionForwarder>(){{ add(sffClassifier); }});

        when(scf.getAcl()).thenReturn(mock(org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf
                        .rev140701.service.function.classifiers.service.function.classifier.Acl.class));

        when(acl.getAclName()).thenReturn("aclName");
        when(acl.getAccessListEntries()).thenReturn(accessListEntries);

        when(accessListEntries.getAce()).thenReturn(new ClassifierAclDataBuilder().mockAces(1));

        // mock the SFF
        when(sff.getSffDataPlaneLocator()).thenReturn(null);
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        PowerMockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(any(SffName.class)))
                .thenReturn(sff);

        // mock the static NSH header object
        PowerMockito.mockStatic(SfcNshHeader.class);
        SfcNshHeader theNshHeader = new SfcNshHeader()
                .setFirstSfName(new SfName("sf#1"))
                .setNshEndNsi((short) 254)
                .setNshMetaC1(123L)
                .setNshMetaC2(321L)
                .setNshMetaC3(2323L)
                .setNshMetaC4(3232L)
                .setNshNsp(666L)
                .setNshStartNsi((short) 255)
                .setSffName(new SffName("sff#1"))
                .setVxlanIpDst(new Ipv4Address("192.168.1.1"))
                .setVxlanUdpPort(new PortNumber(8080));
        PowerMockito.when(SfcNshHeader.getSfcNshHeader(any(RspName.class))).thenReturn(theNshHeader);
        PowerMockito.when(SfcNshHeader.getSfcNshHeader(any(RenderedServicePath.class))).thenReturn(theNshHeader);
    }

    @Test
    public void updateClassifierOK() {
        List<FlowDetails> flowList = theUpdateProcessor.processClassifier(sffClassifier, acl, newRsp);
        Assert.assertFalse(flowList.isEmpty());
        Assert.assertEquals(2, flowList.size());
    }

    @Test
    public void updateClassifierInvalidACL() {
        // this ACL does not feature any ACEs
        when(accessListEntries.getAce()).thenReturn(Collections.emptyList());
        List<FlowDetails> flowList = theUpdateProcessor.processClassifier(sffClassifier, acl, newRsp);
        Assert.assertTrue(flowList.isEmpty());
    }

    @Test
    public void updateClassifierInvalidClassifier() {
        // this classifier does not have attachment points
        when(sffClassifier.getAttachmentPointType()).thenReturn(null);
        List<FlowDetails> flowList = theUpdateProcessor.processClassifier(sffClassifier, acl, newRsp);
        Assert.assertTrue(flowList.isEmpty());
    }

    @Test
    public void updateClassifierWrongAttachmentType() {
        // for this one, we set the attachment point as a bridge, which LogicalSFF does not allow
        when(sffClassifier.getAttachmentPointType()).thenReturn(new BridgeBuilder().build());
        List<FlowDetails> flowList = theUpdateProcessor.processClassifier(sffClassifier, acl, newRsp);
        Assert.assertTrue(flowList.isEmpty());
    }
}

/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicallyAttachedClassifier;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.SourcePortRangeBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcGeniusDataUtils.class,
        SfcGeniusRpcClient.class,
        SfcNshHeader.class,
        SfcProviderServiceFunctionAPI.class,
        SfcProviderServiceForwarderAPI.class,
        LogicalClassifierDataGetter.class,
        SfcOvsUtil.class})
public class OpenflowClassifierProcessorTest {
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
    ReadWriteTransaction readWriteTransaction;

    @Mock
    LogicalClassifierDataGetter dataGetter;

    @Mock
    RpcProviderRegistry rpcProvider;

    ClassifierInterface classifierInterface;

    private String interfaceToClassify;

    private static final String FIRST_SF_NODE_NAME = "openflow:1234567890";

    public OpenflowClassifierProcessorTest() {
        initMocks(this);
        interfaceToClassify = "750135c0-67a9-4fc1-aac0-1359ae7944d4";
    }

    @Before
    public void setUp() {
        when(sffClassifier.getName()).thenReturn("sffName");
        when(sffClassifier.getAttachmentPointType())
                .thenReturn(new InterfaceBuilder()
                        .setInterface(interfaceToClassify)
                        .build());
        when(scf.getSclServiceFunctionForwarder())
                .thenReturn(new ArrayList<SclServiceFunctionForwarder>(){{ add(sffClassifier); }});
        when(scf.getAcl())
                .thenReturn(mock(org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf
                        .rev140701.service.function.classifiers.service.function.classifier.Acl.class));
        when(acl.getAclName()).thenReturn("aclName");
        when(acl.getAccessListEntries()).thenReturn(accessListEntries);
        when(accessListEntries.getAce()).thenReturn(mockAces(1));

        PowerMockito.mockStatic(LogicalClassifierDataGetter.class);
        PowerMockito.when(LogicalClassifierDataGetter.getOpenflowPort(anyString())).thenReturn(Optional.of(2L));

        classifierInterface = Mockito.spy(new LogicallyAttachedClassifier(sff, dataGetter));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        //doReturn(true).when(classifierInterface).usesLogicalInterfaces();

        // mock the SFF
        when(sff.getSffDataPlaneLocator()).thenReturn(null);
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        PowerMockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(any(SffName.class)))
                .thenReturn(sff);

        PowerMockito.stub(PowerMockito.method(SfcNshHeader.class, "getSfcNshHeader"))
                .toReturn(new SfcNshHeader()
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
                        .setVxlanUdpPort(new PortNumber(8080)));

        PowerMockito.mockStatic(SfcOvsUtil.class);
        PowerMockito.when(SfcOvsUtil.getVxlanOfPort(anyString())).thenReturn(4L);
    }

    @Test
    public void addClassifierOK() {
        // TODO - logical SFF scenario
    }

    @Test
    public void addClassifierLegacyScenario() {
        // must set the usesLogicalInterfaces = false
        when(sff.getSffDataPlaneLocator()).thenReturn(new ArrayList<>());

        // disable DPDK extensions
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(anyString(), anyString())).thenReturn(null);

        classifierInterface = Mockito.spy(new BareClassifier(sff));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());

        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);

        Assert.assertFalse(theFlows.isEmpty());

        // install table miss, install classifier "out" flow, and install classifier 'in' flow for the reverse RSP
        Assert.assertEquals(2 + 1, theFlows.size());
        }

    @Test
    public void addClassifierDpdkScenario() {
        // must set the usesLogicalInterfaces = false
        when(sff.getSffDataPlaneLocator()).thenReturn(new ArrayList<>());

        // enable DPDK extensions
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(anyString(), anyString())).thenReturn(3L);

        classifierInterface = Mockito.spy(new BareClassifier(sff));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);

        Assert.assertFalse(theFlows.isEmpty());

        // since we have DPDK extensions enabled, 2 extra flows are written
        Assert.assertEquals(2 + 2 + 1, theFlows.size());
        }

    @Test
    public void addClassifierEmptyAcl() {
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        when(accessListEntries.getAce()).thenReturn(new ArrayList<>());

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);

        Assert.assertTrue(theFlows.isEmpty());
    }

    @Test
    public void addClassifierEmptyClassifier() {
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        PowerMockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(any(SffName.class)))
                .thenReturn(null);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);

        Assert.assertTrue(theFlows.isEmpty());
    }

    @Test
    public void removeClassifierOK() {
        // TODO - logical SFF scenario
    }

    @Test
    public void removeClassifierLegacyScenario() {
        // must set the usesLogicalInterfaces = false
        when(sff.getSffDataPlaneLocator()).thenReturn(new ArrayList<>());

        // disable DPDK extensions
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(anyString(), anyString())).thenReturn(null);

        classifierInterface = Mockito.spy(new BareClassifier(sff));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);
        Assert.assertFalse(theFlows.isEmpty());

        // remove classifier "out" flow, and classifier 'in' flow for the reverse RSP
        Assert.assertEquals(1 + 1, theFlows.size());
        }

    @Test
    public void removeClassifierDpdkScenario() {
        // in the current behaviour, DPDK flows are not delete - no clue why. thus, this behaviour is kept.
        // must set the usesLogicalInterfaces = false
        when(sff.getSffDataPlaneLocator()).thenReturn(new ArrayList<>());

        // disable DPDK extensions
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(anyString(), anyString())).thenReturn(null);

        classifierInterface = Mockito.spy(new BareClassifier(sff));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);

        Assert.assertFalse(theFlows.isEmpty());

        // remove classifier "out" flow, and classifier 'in' flow for the reverse RSP
        Assert.assertEquals(1 + 1, theFlows.size());
    }


    @Test
    public void removeClassifierEmptyAcl() {
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        when(accessListEntries.getAce()).thenReturn(new ArrayList<>());
        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);
        Assert.assertTrue(theFlows.isEmpty());
    }

    @Test
    public void removeClassifierEmptyClassifier() {
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, rpcProvider, classifierInterface);

        PowerMockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(any(SffName.class)))
                .thenReturn(null);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);

        Assert.assertTrue(theFlows.isEmpty());
    }

    public List<Ace> mockAces(final int nMatches) {
        String srcNetwork = "192.168.2.0/24";
        String dstNetwork = "192.168.2.0/24";
        String rspPrefix = "RSP_";
        int srcLowerPort = 80;
        int dstLowerPort = 80;
        short protocol = SfcOpenflowUtils.IP_PROTOCOL_TCP;

        List<Ace> theAces = new ArrayList<>();
        for (int i = 0; i < nMatches; i++) {
            String rspName = rspPrefix + Integer.toString(i / 2 + 1);
            theAces.add(new AceBuilder()
                    .setRuleName(String.format("ACE%d", i))
                    .setActions(buildActions(rspName))
                    .setMatches(buildMatches(srcNetwork, dstNetwork, srcLowerPort, dstLowerPort, protocol)).build());
        }

        return theAces;
    }

    private Matches buildMatches(String srcNetwork, String dstNetwork, int srcLowerPort, int dstLowerPort, short protocol) {
        AceIpv4 ipv4  = new AceIpv4Builder()
                .setSourceIpv4Network(new Ipv4Prefix(srcNetwork))
                .setDestinationIpv4Network(new Ipv4Prefix(dstNetwork))
                .build();

        AceIp ip = new AceIpBuilder()
                .setAceIpVersion(ipv4)
                .setProtocol(protocol)
                .setSourcePortRange(new SourcePortRangeBuilder()
                        .setLowerPort(new PortNumber(srcLowerPort))
                        .build())
                .setDestinationPortRange(new DestinationPortRangeBuilder()
                        .setLowerPort(new PortNumber(dstLowerPort))
                        .build())
                .build();

        Matches matches = new MatchesBuilder()
                .setAceType(ip)
                .build();

        return matches;
    }

    private Actions buildActions(String rspName) {
        Actions1Builder actions1Builder = new Actions1Builder()
                .setSfcAction(new AclRenderedServicePathBuilder()
                        .setRenderedServicePath(rspName).build());

        Actions actions = new ActionsBuilder()
                .addAugmentation(Actions1.class, actions1Builder.build())
                .build();

        return actions;
    }
}

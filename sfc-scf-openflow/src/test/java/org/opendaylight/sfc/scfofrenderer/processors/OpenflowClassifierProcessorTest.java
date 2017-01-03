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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.scfofrenderer.ClassifierAclDataBuilder;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.BareClassifier;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.LogicallyAttachedClassifier;
import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.RspLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
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

    @Mock
    RenderedServicePath rsp;

    @Mock
    List<RenderedServicePathHop> theHops;

    @Mock
    RenderedServicePathHop hop;

    @Mock
    RspLogicalSffAugmentation logicalSffAugmentation;

    private LogicallyAttachedClassifier classifierInterface;

    private final String interfaceToClassify;

    private static final String FIRST_SF_NODE_NAME = "openflow:1234567890";

    private static final DpnIdType FIRST_SF_DATAPLANE_ID = new DpnIdType(new BigInteger("1234567890"));

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowClassifierProcessorTest.class);

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
        when(accessListEntries.getAce()).thenReturn(new ClassifierAclDataBuilder().mockAces(1));

        PowerMockito.mockStatic(LogicalClassifierDataGetter.class);
        PowerMockito.when(LogicalClassifierDataGetter.getOpenflowPort(anyString())).thenReturn(Optional.of(2L));
        PowerMockito.when(LogicalClassifierDataGetter.getDpnIdFromNodeName(any(String.class)))
                .thenReturn(FIRST_SF_DATAPLANE_ID);

        classifierInterface = Mockito.spy(new LogicallyAttachedClassifier(dataGetter));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        //doReturn(true).when(classifierInterface).usesLogicalInterfaces();

        // mock the SFF
        when(sff.getSffDataPlaneLocator()).thenReturn(null);
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        PowerMockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(any(SffName.class)))
                .thenReturn(sff);

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

        PowerMockito.mockStatic(SfcOvsUtil.class);
        PowerMockito.when(SfcOvsUtil.getVxlanOfPort(anyString())).thenReturn(4L);
        PowerMockito.when(SfcOvsUtil.getSffVxlanDataLocator(any(ServiceFunctionForwarder.class)))
                .thenReturn(new IpBuilder()
                        .setIp(new IpAddress(new Ipv4Address("192.168.2.2")))
                        .setPort(new PortNumber(80))
                        .build());
    }

    @Test
    public void addClassifierOK() {
        when(dataGetter.getFirstHopDataplaneId(any(RenderedServicePath.class)))
                .thenReturn(Optional.of(FIRST_SF_DATAPLANE_ID));

        LogicallyAttachedClassifier classifierInterface = Mockito.spy(new LogicallyAttachedClassifier(dataGetter));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, classifierInterface, new BareClassifier());

        // disable DPDK flows
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(anyString(), anyString())).thenReturn(null);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);
        Assert.assertFalse(theFlows.isEmpty());
        Assert.assertEquals(2, theFlows.size());
    }

    @Test
    public void addClassifierLegacyScenario() {
        // must set the usesLogicalInterfaces = false
        when(sff.getSffDataPlaneLocator()).thenReturn(new ArrayList<>());

        // disable DPDK extensions
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(anyString(), anyString())).thenReturn(null);

        BareClassifier bareClassifierHandler = Mockito.spy(new BareClassifier(sff));
        LogicallyAttachedClassifier logicallyAttachedClassifierHandler = new LogicallyAttachedClassifier(dataGetter);

        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(bareClassifierHandler).getNodeName(anyString());

        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                logicallyAttachedClassifierHandler,
                bareClassifierHandler);

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

        BareClassifier bareClassifierHandler = Mockito.spy(new BareClassifier(sff));

        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(bareClassifierHandler).getNodeName(anyString());
        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                new LogicallyAttachedClassifier(dataGetter),
                bareClassifierHandler);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);

        Assert.assertFalse(theFlows.isEmpty());

        // since we have DPDK extensions enabled, 2 extra flows are written
        Assert.assertEquals(2 + 2 + 1, theFlows.size());
    }

    @Test
    public void addClassifierEmptyAcl() {
        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                 classifierInterface,
                new BareClassifier());

        when(accessListEntries.getAce()).thenReturn(new ArrayList<>());

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);

        Assert.assertTrue(theFlows.isEmpty());
    }

    @Test
    public void addClassifierEmptyClassifier() {
        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                new LogicallyAttachedClassifier(dataGetter),
                new BareClassifier());

        PowerMockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(any(SffName.class)))
                .thenReturn(null);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, true);

        Assert.assertTrue(theFlows.isEmpty());
    }

    @Test
    public void removeClassifierOK() {
        when(dataGetter.getFirstHopDataplaneId(any(RenderedServicePath.class)))
                .thenReturn(Optional.of(FIRST_SF_DATAPLANE_ID));

        LogicallyAttachedClassifier classifierInterface = Mockito.spy(new LogicallyAttachedClassifier(dataGetter));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());

         OpenflowClassifierProcessor classifierManager =
                new OpenflowClassifierProcessor(readWriteTransaction, classifierInterface, new BareClassifier());

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);

        Assert.assertFalse(theFlows.isEmpty());
        // TODO - should we remove the "MatchAny" from the classifier?... (new behaviour)
        theFlows.forEach(flow -> LOG.info("The flow: {}", flow.getFlow()));
        Assert.assertEquals(1, theFlows.size());
    }

    @Test
    public void removeClassifierLegacyScenario() {
        // must set the usesLogicalInterfaces = false
        when(sff.getSffDataPlaneLocator()).thenReturn(new ArrayList<>());

        // disable DPDK extensions
        PowerMockito.when(SfcOvsUtil.getDpdkOfPort(anyString(), anyString())).thenReturn(null);

        BareClassifier classifierInterface = Mockito.spy(new BareClassifier(sff));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                new LogicallyAttachedClassifier(dataGetter),
                classifierInterface);

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

        BareClassifier classifierInterface = Mockito.spy(new BareClassifier(sff));
        doReturn(Optional.of(FIRST_SF_NODE_NAME)).when(classifierInterface).getNodeName(anyString());
        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                new LogicallyAttachedClassifier(dataGetter),
                classifierInterface);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);

        Assert.assertFalse(theFlows.isEmpty());

        // remove classifier "out" flow, and classifier 'in' flow for the reverse RSP
        Assert.assertEquals(1 + 1, theFlows.size());
    }

    @Test
    public void removeClassifierEmptyAcl() {
        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                classifierInterface,
                new BareClassifier());

        when(accessListEntries.getAce()).thenReturn(new ArrayList<>());
        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);
        Assert.assertTrue(theFlows.isEmpty());
    }

    @Test
    public void removeClassifierEmptyClassifier() {
        OpenflowClassifierProcessor classifierManager = new OpenflowClassifierProcessor(readWriteTransaction,
                new LogicallyAttachedClassifier(dataGetter),
                new BareClassifier());

        PowerMockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(any(SffName.class)))
                .thenReturn(null);

        List<FlowDetails> theFlows = classifierManager.processClassifier(sffClassifier, acl, false);

        Assert.assertTrue(theFlows.isEmpty());
    }
}

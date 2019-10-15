/*
 * Copyright (c) 2016, 2018 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.scfofrenderer.listeners;

import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.processors.ClassifierRspUpdateProcessor;
import org.opendaylight.sfc.scfofrenderer.rspupdatelistener.ClassifierRspUpdateDataGetter;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class ClassifierRspsUpdateListenerTest {

    @Mock
    ClassifierRspUpdateProcessor theClassifierProcessor;

    @Mock
    SfcOfFlowWriterInterface theOpenflowWriter;

    @Mock
    ClassifierRspUpdateDataGetter theUpdateDataGetter;

    @Mock
    LogicalClassifierDataGetter theDataGetter;

    @Mock
    RenderedServicePath oldRsp;

    @Mock
    RenderedServicePath newRsp;

    @Mock
    SclServiceFunctionForwarder theClassifier;

    @Mock
    Acl theAcl;

    @InjectMocks
    ClassifierRspsUpdateListener theUpdateListener;

    private final DpnIdType oldDataplaneId;

    private final DpnIdType newDataplaneId;

    public ClassifierRspsUpdateListenerTest() {
        initMocks(this);
        oldDataplaneId = new DpnIdType(Uint64.valueOf("1234567890"));
        newDataplaneId = new DpnIdType(Uint64.valueOf("9876543210"));
        Mockito.doReturn(Uint32.ZERO).when(oldRsp).getPathId();
    }

    @Before
    public void setUp() {
        Mockito.doNothing().when(theOpenflowWriter).deleteFlowSet();
        Mockito.doNothing().when(theOpenflowWriter).flushFlows();

        Mockito.when(oldRsp.getName()).thenReturn(new RspName(""));
        Mockito.when(theAcl.getAclName()).thenReturn("");

        List<Acl> aclList = new ArrayList<>() {
            {
                add(theAcl);
            }
        };
        Mockito.when(theUpdateDataGetter.filterAclsByRspName(Mockito.any(RspName.class))).thenReturn(aclList);

        List<SclServiceFunctionForwarder> classifierList = new ArrayList<>() {
            {
                add(theClassifier);
            }
        };
        Mockito.when(theUpdateDataGetter.filterClassifierNodesByAclName(Mockito.any(String.class)))
                .thenReturn(classifierList);

        Mockito.when(theDataGetter.getFirstHopDataplaneId(oldRsp)).thenReturn(Optional.of(oldDataplaneId));
        Mockito.when(theDataGetter.getFirstHopDataplaneId(newRsp)).thenReturn(Optional.of(newDataplaneId));
    }

    @Test
    public void testRenderRsp() {
        theUpdateListener.add(InstanceIdentifier.create(RenderedServicePath.class), newRsp);
        Mockito.verifyZeroInteractions(theOpenflowWriter);
        Mockito.verifyZeroInteractions(theClassifierProcessor);
    }

    @Test
    public void testUpdateRsp() {
        theUpdateListener.update(InstanceIdentifier.create(RenderedServicePath.class), oldRsp, newRsp);

        Mockito.verify(theOpenflowWriter).deleteRspFlows(oldRsp.getPathId().toJava());
        Mockito.verify(theOpenflowWriter).deleteFlowSet();
        Mockito.verify(theClassifierProcessor).processClassifier(theClassifier, theAcl, newRsp);
    }

    @Test
    public void testDeleteRsp() {
        theUpdateListener.remove(InstanceIdentifier.create(RenderedServicePath.class), oldRsp);

        /*
         * Mockito.verify(theOpenflowWriter).deleteRspFlows(oldRsp.getPathId());
         * Mockito.verify(theOpenflowWriter).deleteFlowSet();
         * Mockito.verifyZeroInteractions(theClassifierProcessor);
         */
    }
}

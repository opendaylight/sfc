/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.listeners;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.sfc.ofrenderer.processors.SfcOfRspProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SfcOfRspDataListenerTest {
    @Mock
    DataBroker dataBroker;

    @Mock
    SfcOfRspProcessor sfcOfRspProcessor;

    @Mock
    DataTreeModification<RenderedServicePath> dataTreeModificationRsp;

    @Mock
    DataObjectModification<RenderedServicePath> dataObjectModificationRsp;

    Collection<DataTreeModification<RenderedServicePath>> rspModifications;

    SfcOfRspDataListener sfcOfRspDataListener;

    @Before
    public void setup() {
        sfcOfRspDataListener = new SfcOfRspDataListener(dataBroker, sfcOfRspProcessor);
        rspModifications = Collections.singletonList(dataTreeModificationRsp);
        when(dataTreeModificationRsp.getRootNode()).thenReturn(dataObjectModificationRsp);
    }

    @Test
    public void onDataTreeChangedAdd() throws Exception {
        RenderedServicePath addedRsp = new RenderedServicePathBuilder().setName(new RspName("RSP1")).build();
        when(dataObjectModificationRsp.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(dataObjectModificationRsp.getDataAfter()).thenReturn(addedRsp);
        sfcOfRspDataListener.onDataTreeChanged(rspModifications);
        verify(sfcOfRspProcessor).processRenderedServicePath(same(addedRsp));
        verifyNoMoreInteractions(sfcOfRspProcessor);
    }

    @Test
    public void onDataTreeChangedRerender() throws Exception {
        RenderedServicePath rerenderRsp = new RenderedServicePathBuilder().setName(new RspName("RSP1")).build();
        when(dataObjectModificationRsp.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(dataObjectModificationRsp.getDataAfter()).thenReturn(rerenderRsp);
        when(dataObjectModificationRsp.getDataBefore()).thenReturn(rerenderRsp);
        sfcOfRspDataListener.onDataTreeChanged(rspModifications);
        verify(sfcOfRspProcessor).processRenderedServicePath(same(rerenderRsp));
        verify(sfcOfRspProcessor).deleteRenderedServicePath(same(rerenderRsp));
        verifyNoMoreInteractions(sfcOfRspProcessor);
    }

    @Test
    public void onDataTreeChangedUpdateUnsupported() throws Exception {
        RenderedServicePath oldRsp = new RenderedServicePathBuilder().setName(new RspName("RSP1")).build();
        RenderedServicePath newRsp = new RenderedServicePathBuilder().setName(new RspName("RSP2")).build();
        when(dataObjectModificationRsp.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(dataObjectModificationRsp.getDataAfter()).thenReturn(newRsp);
        when(dataObjectModificationRsp.getDataBefore()).thenReturn(oldRsp);
        sfcOfRspDataListener.onDataTreeChanged(rspModifications);
        verifyNoMoreInteractions(sfcOfRspProcessor);
    }

    @Test
    public void onDataTreeChangedDelete() throws Exception {
        RenderedServicePath oldRsp = new RenderedServicePathBuilder().setName(new RspName("RSP1")).build();
        when(dataObjectModificationRsp.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(dataObjectModificationRsp.getDataBefore()).thenReturn(oldRsp);
        sfcOfRspDataListener.onDataTreeChanged(rspModifications);
        verify(sfcOfRspProcessor).deleteRenderedServicePath(same(oldRsp));
        verifyNoMoreInteractions(sfcOfRspProcessor);
    }

}

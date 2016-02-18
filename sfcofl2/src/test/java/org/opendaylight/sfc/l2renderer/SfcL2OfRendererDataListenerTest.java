/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerInterface;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2OfRendererDataListener;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfig;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfigBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2OfRendererDataListenerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2OfRendererDataListenerTest.class);
    private SfcL2OfRendererDataListener sfcL2OfRendererDataListener;
    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;

    public SfcL2OfRendererDataListenerTest() {
        LOG.info("SfcL2OfRendererDataListenerTest constructor");
        initFlowProgrammer();
        DataBroker dataBroker = mock(DataBroker.class);
        SfcSynchronizer sfcSynchronizer = mock(SfcSynchronizer.class);

        this.sfcL2OfRendererDataListener =
                new SfcL2OfRendererDataListener(dataBroker, this.sfcL2FlowProgrammer, sfcSynchronizer);
    }

    @Test
    public void invalidTableOffset() {
        // Negatives cant be tested here since setting Table Offset causes an exception in:
        //    SfcOfRendererConfigBuilder.checkSfcOfTableOffsetRange()

        // Table Offset must be greater than 1
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = createSfcOfRendererConfig(0, 100);
        this.sfcL2OfRendererDataListener.onDataChanged(change);
        verifySettersNotCalled();

        // Table Offset must be greater than 1
        initFlowProgrammer();
        change = createSfcOfRendererConfig(1, 100);
        this.sfcL2OfRendererDataListener.onDataChanged(change);
        verifySettersNotCalled();

        // Table Offset must be less than 246 (255-maxTableOffset())
        initFlowProgrammer();
        change = createSfcOfRendererConfig(250, 100);
        this.sfcL2OfRendererDataListener.onDataChanged(change);
        verifySettersNotCalled();
    }

    @Test
    public void invalidTableEgress() {
        // Table Egress cannot be negative
        // This cant be tested here since setting Table Egress causes an exception in:
        //    SfcOfRendererConfigBuilder.checkSfcOfAppEgressTableOffsetRange()

        // Table Egress cannot be in the tableOffset range
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = null;
        for(int i = 100; i < 110; ++i) {
            change = createSfcOfRendererConfig(100, i);
            this.sfcL2OfRendererDataListener.onDataChanged(change);
            verifySettersNotCalled();
        }
    }

    @Test
    public void offsetLessThanEgress() throws InterruptedException {
        // Test offset values less than egress values
        int offset = 20;
        int egress = 80;
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = createSfcOfRendererConfig(offset, egress);
        this.sfcL2OfRendererDataListener.onDataChanged(change);
        Thread.sleep(100); // Let the thread finish
        verifySettersCalled(offset, egress);
    }

    @Test
    public void offsetGreaterThanEgress() throws InterruptedException {
        // Test offset values greater than egress values
        int offset = 200;
        int egress = 10;
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = createSfcOfRendererConfig(offset, egress);
        this.sfcL2OfRendererDataListener.onDataChanged(change);
        Thread.sleep(100); // Let the thread finish
        verifySettersCalled(offset, egress);
    }

    @Test
    public void egressSetZero() throws InterruptedException {
        // Test egress set to 0
        int offset = 100;
        int egress = 0;
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = createSfcOfRendererConfig(offset, egress);
        this.sfcL2OfRendererDataListener.onDataChanged(change);
        Thread.sleep(100); // Let the thread finish
        verifySettersCalled(offset, egress);
    }

    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> createSfcOfRendererConfig(int tableOffset, int tableEgress) {
        SfcOfRendererConfigBuilder configBuilder = new SfcOfRendererConfigBuilder();
        configBuilder.setSfcOfTableOffset((short) tableOffset);
        configBuilder.setSfcOfAppEgressTableOffset((short) tableEgress);
        SfcOfRendererConfig entryValue = configBuilder.build();

        InstanceIdentifier<SfcOfRendererConfig> entryKey = InstanceIdentifier.builder(SfcOfRendererConfig.class).build();
        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
        when(change.getCreatedData()).thenReturn(entrySet);

        return change;
    }

    private void verifySettersCalled(int offset, int egress) {
        assertEquals(this.sfcL2FlowProgrammer.getTableBase(), offset);
        assertEquals(this.sfcL2FlowProgrammer.getTableEgress(), egress);
    }

    private void verifySettersNotCalled() {
        assertEquals(this.sfcL2FlowProgrammer.getTableBase(),   SfcL2FlowProgrammerOFimpl.APP_COEXISTENCE_NOT_SET);
        assertEquals(this.sfcL2FlowProgrammer.getTableEgress(), SfcL2FlowProgrammerOFimpl.APP_COEXISTENCE_NOT_SET);
    }

    private void initFlowProgrammer() {
        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammerOFimpl(mock(SfcL2FlowWriterInterface.class));
    }
}

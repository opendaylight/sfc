/*
 * Copyright (c) 2014, 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.listeners;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfig;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfigBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOfRendererDataListenerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRendererDataListenerTest.class);
    private static final long SLEEP = 100; // milliseconds to sleep after
                                            // onDataTreeChanged is called.
    private final SfcOfRendererDataListener sfcOfRendererDataListener;
    private final SfcOfFlowProgrammerInterface sfcOfFlowProgrammer;

    public SfcOfRendererDataListenerTest() {
        LOG.info("SfcOfRendererDataListenerTest constructor");
        this.sfcOfFlowProgrammer = new SfcOfFlowProgrammerImpl(
                mock(SfcOfFlowWriterInterface.class));
        DataBroker dataBroker = mock(DataBroker.class);
        SfcSynchronizer sfcSynchronizer = mock(SfcSynchronizer.class);

        this.sfcOfRendererDataListener = new SfcOfRendererDataListener(dataBroker, this.sfcOfFlowProgrammer,
                sfcSynchronizer);
    }

    @Test
    public void invalidTableOffset() throws InterruptedException {
        // Negatives cant be tested here since setting Table Offset causes an
        // exception in:
        // SfcOfRendererConfigBuilder.checkSfcOfTableOffsetRange()

        // Table Offset must be greater than 1
        this.sfcOfRendererDataListener.onDataTreeChanged(createSfcOfRendererConfig(0, 100));
        Thread.sleep(SLEEP); // otherwise the failure is not detected
        verifySettersNotCalled();

        // Table Offset must be greater than 1
        this.sfcOfRendererDataListener.onDataTreeChanged(createSfcOfRendererConfig(1, 100));
        Thread.sleep(SLEEP); // otherwise the failure is not detected
        verifySettersNotCalled();

        // Table Offset must be less than 246 (255-maxTableOffset())
        this.sfcOfRendererDataListener.onDataTreeChanged(createSfcOfRendererConfig(250, 100));
        Thread.sleep(SLEEP); // otherwise the failure is not detected
        verifySettersNotCalled();
    }

    @Test
    public void invalidTableEgress() throws InterruptedException {
        // Table Egress cannot be negative
        // This cant be tested here since setting Table Egress causes an
        // exception in:
        // SfcOfRendererConfigBuilder.checkSfcOfAppEgressTableOffsetRange()

        // Table Egress cannot be in the tableOffset range
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = null;
        for (int i = 100; i < 110; ++i) {
            this.sfcOfRendererDataListener.onDataTreeChanged(createSfcOfRendererConfig(100, i));
            Thread.sleep(SLEEP); // otherwise the failure is not detected
            verifySettersNotCalled();
        }
    }

    @Test
    public void offsetLessThanEgress() throws InterruptedException {
        // Test offset values less than egress values
        int offset = 20;
        int egress = 80;
        this.sfcOfRendererDataListener.onDataTreeChanged(createSfcOfRendererConfig(offset, egress));
        Thread.sleep(100); // Let the thread finish
        verifySettersCalled(offset, egress);
    }

    @Test
    public void offsetGreaterThanEgress() throws InterruptedException {
        // Test offset values greater than egress values
        int offset = 200;
        int egress = 10;
        this.sfcOfRendererDataListener.onDataTreeChanged(createSfcOfRendererConfig(offset, egress));
        Thread.sleep(100); // Let the thread finish
        verifySettersCalled(offset, egress);
    }

    @Test
    public void egressSetZero() throws InterruptedException {
        // Test egress set to 0
        int offset = 100;
        int egress = 0;
        this.sfcOfRendererDataListener.onDataTreeChanged(createSfcOfRendererConfig(offset, egress));
        Thread.sleep(100); // Let the thread finish
        verifySettersCalled(offset, egress);
    }

    @SuppressWarnings("unchecked")
    private Collection<DataTreeModification<SfcOfRendererConfig>> createSfcOfRendererConfig(int tableOffset,
            int tableEgress) {
        SfcOfRendererConfigBuilder configBuilder = new SfcOfRendererConfigBuilder();
        configBuilder.setSfcOfTableOffset((short) tableOffset);
        configBuilder.setSfcOfAppEgressTableOffset((short) tableEgress);
        SfcOfRendererConfig config = configBuilder.build();

        InstanceIdentifier<SfcOfRendererConfig> iid = InstanceIdentifier.builder(SfcOfRendererConfig.class).build();
        DataTreeModification<SfcOfRendererConfig> mockChange = mock(DataTreeModification.class);
        DataObjectModification<SfcOfRendererConfig> mockModification = mock(DataObjectModification.class);
        when(mockModification.getDataAfter()).thenReturn(config);
        when(mockModification.getModificationType()).thenReturn(ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, iid));
        when(mockChange.getRootNode()).thenReturn(mockModification);

        return Collections.singletonList(mockChange);
    }

    private void verifySettersCalled(int offset, int egress) {
        assertEquals(this.sfcOfFlowProgrammer.getTableBase(), offset);
        assertEquals(this.sfcOfFlowProgrammer.getTableEgress(), egress);
    }

    private void verifySettersNotCalled() {
        assertEquals(this.sfcOfFlowProgrammer.getTableBase(), SfcOfFlowProgrammerImpl.APP_COEXISTENCE_NOT_SET);
        assertEquals(this.sfcOfFlowProgrammer.getTableEgress(), SfcOfFlowProgrammerImpl.APP_COEXISTENCE_NOT_SET);
    }
}

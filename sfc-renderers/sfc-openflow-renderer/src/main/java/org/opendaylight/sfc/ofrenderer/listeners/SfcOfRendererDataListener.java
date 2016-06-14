/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.listeners;

import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataListener to listen for SFC OpenFlow Renderer data store changes.
 *
 * @author ebrjohn
 *
 */
public class SfcOfRendererDataListener extends SfcOfAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRendererDataListener.class);
    private SfcOfFlowProgrammerInterface sfcOfFlowProgrammer;
    private SfcSynchronizer sfcSynchronizer;
    private ExecutorService threadExecutor;

    public SfcOfRendererDataListener(
            DataBroker dataBroker,
            SfcOfFlowProgrammerInterface sfcOfFlowProgrammer,
            SfcSynchronizer sfcSynchronizer) {
        setDataBroker(dataBroker);
        setIID(InstanceIdentifier.builder(SfcOfRendererConfig.class).build());
        registerAsDataChangeListener(LogicalDatastoreType.CONFIGURATION);
        this.sfcOfFlowProgrammer = sfcOfFlowProgrammer;
        this.sfcSynchronizer = sfcSynchronizer;
        threadExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // SFC OF Renderer config create
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            if (entry.getValue() instanceof SfcOfRendererConfig) {
                LOG.info("SfcOfRendererDataListener.onDataChanged create SFC OF Renderer config {}",
                        ((SfcOfRendererConfig) entry.getValue()));
                processConfig((SfcOfRendererConfig) entry.getValue());
            }
        }

        // SFC OF Renderer config update
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getValue() instanceof SfcOfRendererConfig) {
                LOG.info("SfcOfRendererDataListener.onDataChanged update SFC OF Renderer config {}",
                        ((SfcOfRendererConfig) entry.getValue()));
                processConfig((SfcOfRendererConfig) entry.getValue());
            }
        }

        // Not interested in deleted data
    }

    /**
     * Process an OpenFlow Renderer configuration change. Only creates and
     * updates are handled
     * @param config the configuration details
     */
    private void processConfig(SfcOfRendererConfig config) {
        if(verifyMaxTableId(config.getSfcOfTableOffset(), this.sfcOfFlowProgrammer.getMaxTableOffset()) == null) {
            return;
        }

        if(verifyMaxTableId(config.getSfcOfAppEgressTableOffset(), (short) 0) == null) {
            return;
        }

        // See SfcOfFlowProgrammerImpl.getTableId
        final int MAGIC_NUMBER_IN_SFCOFLOWPROGRAMMERIMPL = 2;
        if(config.getSfcOfTableOffset() < MAGIC_NUMBER_IN_SFCOFLOWPROGRAMMERIMPL) {
            LOG.error("Error SfcOfTableOffset value [{}]", config.getSfcOfTableOffset());
            return;
        }

        // Cant set the egress table negative
        if(config.getSfcOfAppEgressTableOffset() < 0) {
            LOG.error("Error SfcOfAppEgressTableOffset value [{}]", config.getSfcOfAppEgressTableOffset());
            return;
        }

        // Check that the egress value is not set in the middle of the SFC table range
        // Example: tableBase = 20, SfcMaxTableOffset=10, then the SFC tables
        //          would be in the range [20..30]. So an egress value of 25
        //          would be invalid
        if(config.getSfcOfAppEgressTableOffset() >= config.getSfcOfTableOffset() &&
           config.getSfcOfAppEgressTableOffset() <= config.getSfcOfTableOffset() + this.sfcOfFlowProgrammer.getMaxTableOffset()) {
            LOG.error("Error SfcOfAppEgressTableOffset value [{}] cant be in the SFC table range [{}..{}]",
                    config.getSfcOfAppEgressTableOffset(),
                    config.getSfcOfTableOffset(),
                    config.getSfcOfTableOffset()+this.sfcOfFlowProgrammer.getMaxTableOffset());

            return;
        }

        UpdateOpenFlowTableOffsets updateThread =
                new UpdateOpenFlowTableOffsets(
                        config.getSfcOfTableOffset(),
                        config.getSfcOfAppEgressTableOffset());

        try {
            threadExecutor.submit(updateThread);
        } catch(Exception e) {
            LOG.error("Error executing UpdateOpenFlowTableOffsets thread [{}]", e.toString());
        }
    }

    /**
     * Verify that the given tableOffset and optional maxTable is in range
     *
     * @param tableOffset the tableOffset to verify
     * @param maxTable optionally the number of tables beyond tableOffset to be used
     * @return a valid TableId or null if invalid
     */
    public TableId verifyMaxTableId(short tableOffset, short maxTable) {
        try {
            return new TableId((short) (tableOffset + maxTable));
        } catch (IllegalArgumentException e) {
            LOG.error("SfcOfRendererDataListener::verifyMaxTableId invalid table offset [{}] maxTable [{}]",
                    tableOffset, maxTable);
            return null;
        }
    }

    /**
     * A thread to update the OpenFlow table offsets. A thread is needed, since
     * we cant just update the table offsets while an RSP is being processed,
     * so we need to wait until RSP processing is completed.
     *
     * @author ebrjohn
     *
     */
    private class UpdateOpenFlowTableOffsets implements Runnable {

        private short sfcOffsetTable;
        private short sfcAppEgressTable;

        public UpdateOpenFlowTableOffsets(short sfcOffsetTable, short sfcAppEgressTable) {
            this.sfcOffsetTable = sfcOffsetTable;
            this.sfcAppEgressTable = sfcAppEgressTable;
        }

        @Override
        public void run() {
            try {
                sfcSynchronizer.lock();
                sfcOfFlowProgrammer.setTableBase(this.sfcOffsetTable);
                sfcOfFlowProgrammer.setTableEgress(this.sfcAppEgressTable);

                LOG.info("UpdateOpenFlowTableOffsets complete tableOffset [{}] egressTable [{}]",
                        this.sfcOffsetTable, this.sfcAppEgressTable);
            } finally {
                sfcSynchronizer.unlock();
            }
        }
    }
}

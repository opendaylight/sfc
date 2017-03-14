/**
 * Copyright (c) 2014, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.ofrenderer.listeners.SfcOfRendererDataListener;
import org.opendaylight.sfc.ofrenderer.listeners.SfcOfRspDataListener;
import org.opendaylight.sfc.ofrenderer.listeners.SfcOfSfgDataListener;
import org.opendaylight.sfc.ofrenderer.openflow.SfcFlowProgrammerBase;
import org.opendaylight.sfc.ofrenderer.openflow.SfcIpv4PacketInHandler;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOpenFlowConfig;
import org.opendaylight.sfc.ofrenderer.processors.SfcOfRspProcessor;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfBaseProviderUtils;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfProviderUtils;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterImpl;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the sfc openflow renderer (blueprint-instantiated).
 */
public class SfcOfRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRenderer.class);
    private final SfcOfRspProcessor sfcOfRspProcessor;

    private final Registration pktInRegistration;

    SfcOfFlowWriterImpl sfcOfFlowWriterImpl = null;
    SfcOfRspDataListener openflowRspDataListener = null;
    SfcOfRendererDataListener sfcOfRendererListener = null;
    SfcIpv4PacketInHandler packetInHandler = null;
    SfcOfSfgDataListener sfcOfSfgDataListener = null;

    public SfcOfRenderer(DataBroker dataBroker, NotificationProviderService notificationService,
            RpcProviderRegistry rpcProviderRegistry) {
        LOG.info("SfcOfRenderer starting the SfcOfRenderer plugin...");

        // Handles writing Flows to the data store
        this.sfcOfFlowWriterImpl = new SfcOfFlowWriterImpl();
        this.sfcOfFlowWriterImpl.setDataProvider(dataBroker);

        // Create a common SfcOpenFlowConfig to be used by all SFC OpenFlow entities
        SfcOpenFlowConfig sfcOfConfig = SfcFlowProgrammerBase.createDefaultSfcOpenFlowConfig();

        // Synchronizes the RspProcessor and the RendererListener
        SfcSynchronizer sfcSynchronizer = new SfcSynchronizer();
        SfcOfBaseProviderUtils sfcOfProviderUtils = new SfcOfProviderUtils();

        // The RspProcessor is called by the RspDataListener to process the RSP and write the corresponding flows
        this.sfcOfRspProcessor = new SfcOfRspProcessor(this.sfcOfFlowWriterImpl, sfcOfConfig, sfcOfProviderUtils,
                sfcSynchronizer, rpcProviderRegistry, dataBroker);

        // Listens for RSP CRUD
        this.openflowRspDataListener = new SfcOfRspDataListener(dataBroker, sfcOfRspProcessor);

        // Listens for SFC OpenFlow config changes, like application coexistence settings
        this.sfcOfRendererListener = new SfcOfRendererDataListener(dataBroker, sfcOfConfig, sfcSynchronizer);

        // PacketIn Handler for VLAN/MPLS transports
        this.packetInHandler = new SfcIpv4PacketInHandler(this.sfcOfFlowWriterImpl, sfcOfConfig);
        this.pktInRegistration = notificationService.registerNotificationListener(packetInHandler);

        this.sfcOfSfgDataListener = new SfcOfSfgDataListener(
                dataBroker, this.sfcOfFlowWriterImpl, sfcOfConfig, sfcOfProviderUtils);

        LOG.info("SfcOfRenderer successfully started the SfcOfRenderer plugin");
    }

    public SfcOfRspDataListener getSfcOfRspDataListener() {
        return this.openflowRspDataListener;
    }

    public SfcIpv4PacketInHandler getSfcIpv4PacketInHandler() {
        return this.packetInHandler;
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws Exception {
        LOG.info("SfcOfRenderer auto-closed");
        try {
            if (this.sfcOfFlowWriterImpl != null) {
                this.sfcOfFlowWriterImpl.shutdown();
            }

            if (this.pktInRegistration != null) {
                this.pktInRegistration.close();
            }

            if (this.sfcOfRendererListener != null) {
                this.sfcOfRendererListener.closeDataChangeListener();
            }

            if (this.openflowRspDataListener != null) {
                this.openflowRspDataListener.close();
            }

            if (this.sfcOfSfgDataListener != null) {
                this.sfcOfSfgDataListener.closeDataChangeListener();
            }

        } finally {
            openflowRspDataListener = null;
        }
    }
}

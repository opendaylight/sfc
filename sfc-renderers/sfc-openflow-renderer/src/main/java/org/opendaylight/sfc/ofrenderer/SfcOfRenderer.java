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
import org.opendaylight.sfc.ofrenderer.openflow.SfcIpv4PacketInHandler;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
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
public final class SfcOfRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRenderer.class);
    private final SfcOfRspProcessor sfcOfRspProcessor;

    private final SfcOfFlowProgrammerInterface sfcOfFlowProgrammer;
    private final Registration pktInRegistration;
    private final SfcSynchronizer sfcSynchronizer;

    private SfcOfRspDataListener openflowRspDataListener;
    private SfcOfSfgDataListener sfcOfSfgDataListener;
    private SfcIpv4PacketInHandler packetInHandler;
    private SfcOfRendererDataListener sfcOfRendererListener;

    public SfcOfRenderer(DataBroker dataBroker, NotificationProviderService notificationService,
                          RpcProviderRegistry rpcProviderRegistry) {
        LOG.info("SfcOfRenderer starting the SfcOfRenderer plugin...");

        this.sfcSynchronizer = new SfcSynchronizer();
        SfcOfFlowWriterImpl sfcofflowwriterimpl = new SfcOfFlowWriterImpl();
        sfcofflowwriterimpl.setDataProvider(dataBroker);
        this.sfcOfFlowProgrammer = new SfcOfFlowProgrammerImpl(sfcofflowwriterimpl);
        SfcOfBaseProviderUtils sfcOfProviderUtils = new SfcOfProviderUtils();
        this.sfcOfRspProcessor = new SfcOfRspProcessor(sfcOfFlowProgrammer, sfcOfProviderUtils, sfcSynchronizer,
                rpcProviderRegistry, dataBroker);

        this.openflowRspDataListener = new SfcOfRspDataListener(dataBroker, sfcOfRspProcessor);
        this.sfcOfSfgDataListener = new SfcOfSfgDataListener(dataBroker, sfcOfFlowProgrammer, sfcOfProviderUtils);
        this.sfcOfRendererListener = new SfcOfRendererDataListener(dataBroker, sfcOfFlowProgrammer, sfcSynchronizer);

        this.packetInHandler = new SfcIpv4PacketInHandler((SfcOfFlowProgrammerImpl) sfcOfFlowProgrammer);
        this.pktInRegistration = notificationService.registerNotificationListener(packetInHandler);

        LOG.info("SfcOfRenderer successfully started the SfcOfRenderer plugin");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws Exception {
        LOG.info("SfcOfRenderer auto-closed");
        try {
            if (sfcOfFlowProgrammer != null) {
                sfcOfFlowProgrammer.shutdown();
            }
            if (pktInRegistration != null) {
                pktInRegistration.close();
            }
            openflowRspDataListener.close();
        } finally {
            openflowRspDataListener = null;
        }
    }
}

/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer;

import org.opendaylight.sfc.ofrenderer.openflow.SfcL2FlowProgrammerOFimpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcL2OfRendererDataListener;
import org.opendaylight.sfc.ofrenderer.openflow.SfcL2FlowWriterImpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.sfc.ofrenderer.openflow.SfcIpv4PacketInHandler;
import org.opendaylight.sfc.ofrenderer.openflow.SfcL2FlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.sfg.SfcL2SfgDataListener;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Registration;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// This class is instantiated from:
//      org.opendaylight.controller.config.yang.config.sfc_ofrenderer.impl.SfcOfRendererModule.createInstance()
// It is a general entry point for the sfc-openflow-renderer feature/plugin
//

public class SfcOfRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRenderer.class);

    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    private Registration pktInRegistration;
    private SfcSynchronizer sfcSynchronizer;
    private SfcL2FlowWriterInterface sfcL2FlowWriter = null;

    SfcL2RspDataListener openflowRspDataListener = null;
    SfcL2SfgDataListener sfcL2SfgDataListener = null;
    SfcIpv4PacketInHandler packetInHandler = null;
    SfcL2OfRendererDataListener sfcOfRendererListener = null;

    public SfcOfRenderer(DataBroker dataBroker, NotificationProviderService notificationService) {
        LOG.info("SfcOfRenderer starting the SfcOfRenderer plugin...");

        this.sfcSynchronizer = new SfcSynchronizer();
        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammerOFimpl(new SfcL2FlowWriterImpl());
        SfcOfBaseProviderUtils sfcOfProviderUtils = new SfcOfProviderUtils();
        this.openflowRspDataListener = new SfcL2RspDataListener(dataBroker, sfcL2FlowProgrammer, sfcOfProviderUtils, sfcSynchronizer);
        this.sfcL2SfgDataListener = new SfcL2SfgDataListener(dataBroker, sfcL2FlowProgrammer, sfcOfProviderUtils);
        this.sfcOfRendererListener = new SfcL2OfRendererDataListener(dataBroker, sfcL2FlowProgrammer, sfcSynchronizer);

        this.packetInHandler = new SfcIpv4PacketInHandler((SfcL2FlowProgrammerOFimpl) sfcL2FlowProgrammer);
        this.pktInRegistration = notificationService.registerNotificationListener(packetInHandler);

        LOG.info("SfcOfRenderer successfully started the SfcOfRenderer plugin");
    }

    public SfcL2RspDataListener getSfcL2RspDataListener() {
        return this.openflowRspDataListener;
    }

    public SfcIpv4PacketInHandler getSfcIpv4PacketInHandler() {
        return this.packetInHandler;
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        LOG.info("SfcOfRenderer auto-closed");
        try {
            if(sfcL2FlowProgrammer != null) {
                sfcL2FlowProgrammer.shutdown();
            }
            if(pktInRegistration != null) {
                pktInRegistration.close();
            }
        } catch(Exception e) {
            LOG.error("SfcOfRenderer auto-closed exception {}", e.getMessage());
        }
    }
}

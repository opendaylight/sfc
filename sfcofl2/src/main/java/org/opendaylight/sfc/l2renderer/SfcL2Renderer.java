/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2OfRendererDataListener;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterImpl;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowWriterInterface;
import org.opendaylight.sfc.l2renderer.openflow.SfcIpv4PacketInHandler;
import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerInterface;
import org.opendaylight.sfc.l2renderer.sfg.SfcL2SfgDataListener;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Registration;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// This class is instantiated from:
//      org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.SfcOFL2ProviderModule.createInstance()
// It is a general entry point for the sfcofl2 feature/plugin
//

public class SfcL2Renderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2Renderer.class);

    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    private Registration pktInRegistration;
    private SfcSynchronizer sfcSynchronizer;
    private SfcL2FlowWriterInterface sfcL2FlowWriter = null;

    SfcL2RspDataListener openflowRspDataListener = null;
    SfcL2SfgDataListener sfcL2SfgDataListener = null;
    SfcIpv4PacketInHandler packetInHandler = null;
    SfcL2OfRendererDataListener sfcOfRendererListener = null;

    public SfcL2Renderer(DataBroker dataBroker, NotificationProviderService notificationService) {
        LOG.info("SfcL2Renderer starting the SfcL2Renderer plugin...");

        this.sfcSynchronizer = new SfcSynchronizer();
        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammerOFimpl(new SfcL2FlowWriterImpl());
        SfcL2BaseProviderUtils sfcL2ProviderUtils = new SfcL2ProviderUtils();
        this.openflowRspDataListener = new SfcL2RspDataListener(dataBroker, sfcL2FlowProgrammer, sfcL2ProviderUtils, sfcSynchronizer);
        this.sfcL2SfgDataListener = new SfcL2SfgDataListener(dataBroker, sfcL2FlowProgrammer, sfcL2ProviderUtils);
        this.sfcOfRendererListener = new SfcL2OfRendererDataListener(dataBroker, sfcL2FlowProgrammer, sfcSynchronizer);

        this.packetInHandler = new SfcIpv4PacketInHandler((SfcL2FlowProgrammerOFimpl) sfcL2FlowProgrammer);
        this.pktInRegistration = notificationService.registerNotificationListener(packetInHandler);

        LOG.info("SfcL2Renderer successfully started the SfcL2Renderer plugin");
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
        LOG.info("SfcL2Renderer auto-closed");
        try {
            if(sfcL2FlowProgrammer != null) {
                sfcL2FlowProgrammer.shutdown();
            }
            if(pktInRegistration != null) {
                pktInRegistration.close();
            }
        } catch(Exception e) {
            LOG.error("SfcL2Renderer auto-closed exception {}", e.getMessage());
        }
    }
}

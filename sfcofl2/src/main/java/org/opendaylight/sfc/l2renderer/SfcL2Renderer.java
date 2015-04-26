/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer;

import org.opendaylight.sfc.l2renderer.openflow.SfcL2FlowProgrammerOFimpl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// This class is instantiated from:
//      org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.SfcOFL2ProviderModule.createInstance()
// It is a general entry point for the sfcofl2 feature/plugin
//

public class SfcL2Renderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2Renderer.class);
    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer = null;
    SfcL2RspDataListener openflowRspDataListener = null;
    private SfcL2SfgDataListener sfcL2SfgDataListener = null;

    public SfcL2Renderer(DataBroker dataBroker) {
        LOG.info("SfcL2Renderer starting the SfcL2Renderer plugin...");

        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammerOFimpl();
        this.openflowRspDataListener = new SfcL2RspDataListener(dataBroker, sfcL2FlowProgrammer);
        this.sfcL2SfgDataListener = new SfcL2SfgDataListener(dataBroker, sfcL2FlowProgrammer);

        LOG.info("SfcL2Renderer successfully started the SfcL2Renderer plugin");
    }

    public SfcL2RspDataListener getSfcL2RspDataListener() {
        return this.openflowRspDataListener;
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
        } catch(Exception e) {
            LOG.error("SfcL2Renderer auto-closed exception {}", e.getMessage());
        }
    }
}
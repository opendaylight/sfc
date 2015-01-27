/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer;

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
    private SfcL2FlowProgrammer sfcL2FlowProgrammer;

    public SfcL2Renderer(DataBroker dataBroker) {
        LOG.info("SfcL2Renderer starting the SfcL2Renderer plugin...");

        this.sfcL2FlowProgrammer = new SfcL2FlowProgrammer();
        SfcL2SfpDataListener openflowSfpDataListener = new SfcL2SfpDataListener(dataBroker, sfcL2FlowProgrammer);
        SfcL2AclDataListener openflowAclDataListener = new SfcL2AclDataListener(dataBroker, sfcL2FlowProgrammer);

        LOG.info("SfcL2Renderer successfully started the SfcL2Renderer plugin");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        LOG.info("SfcL2Renderer auto-closed");
        sfcL2FlowProgrammer.shutdown();
    }
}
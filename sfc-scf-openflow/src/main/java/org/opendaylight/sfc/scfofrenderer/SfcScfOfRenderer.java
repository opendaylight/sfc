/**
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//
// This class is instantiated from:
//      org.opendaylight.controller.config.yang.config.sfc_scf_ofrenderer.impl.SfcScfOfRendererModule.createInstance()
// It is a general entry point for the sfc-scf-openflow feature/plugin
//

public class SfcScfOfRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfRenderer.class);
    SfcScfOfDataListener sfcScfDataListener = null;

    public SfcScfOfRenderer(DataBroker dataBroker, NotificationProviderService notificationService) {
        LOG.info("SfcScfOfRenderer starting the SfcScfOfRenderer plugin...");

        this.sfcScfDataListener = new SfcScfOfDataListener(dataBroker, new SfcScfOfProcessor());

        LOG.info("SfcScfOfRenderer successfully started the SfcScfOfRenderer plugin");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        LOG.info("SfcScfOfRenderer auto-closed");
        try {
        } catch(Exception e) {
            LOG.error("SfcScfOfRenderer auto-closed exception {}", e.getMessage());
        }
    }
}

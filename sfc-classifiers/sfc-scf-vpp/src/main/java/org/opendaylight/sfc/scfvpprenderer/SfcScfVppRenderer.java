/*
 * Copyright (c) 2016 Intel, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfvpprenderer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.scfvpprenderer.listeners.SfcScfVppDataListener;
import org.opendaylight.sfc.scfvpprenderer.processors.VppClassifierProcessor;
import org.opendaylight.sfc.scfvpprenderer.processors.VppNodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the sfc-scf-vpp (blueprint-instantiated).
 */
public class SfcScfVppRenderer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SfcScfVppRenderer.class);
    private final SfcScfVppDataListener sfcScfListener;

    public SfcScfVppRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        VppNodeManager vppNodeManager = new VppNodeManager(dataBroker, bindingAwareBroker);

        VppClassifierProcessor classifierProcessor = new VppClassifierProcessor(vppNodeManager);
        sfcScfListener = new SfcScfVppDataListener(dataBroker, classifierProcessor);
        LOG.info("SfcScfVppRenderer successfully started");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() {
        LOG.info("SfcScfVppRenderer auto-closed");
        sfcScfListener.close();
    }
}

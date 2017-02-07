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
import org.opendaylight.sfc.scfvpprenderer.processors.VppNodeManager;
import org.opendaylight.sfc.scfvpprenderer.processors.VppClassifierProcessor;
import org.opendaylight.sfc.scfvpprenderer.listeners.SfcScfVppDataListener;

/**
 * Initialize all necessary sfc vpp classifier renderer components
 */
public class SfcScfVppRenderer {
    private SfcScfVppDataListener sfcScfListener;

    public SfcScfVppRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        VppNodeManager vppNodeManager = new VppNodeManager(dataBroker, bindingAwareBroker);

        VppClassifierProcessor classifierProcessor = new VppClassifierProcessor(vppNodeManager);
        sfcScfListener = new SfcScfVppDataListener(dataBroker, classifierProcessor);
    }

    public void close() throws Exception {
        sfcScfListener.close();
    }
}

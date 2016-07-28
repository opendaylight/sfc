/*
 * Copyright (c) 2016 Intel, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vpp_renderer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppNodeManager;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppRspProcessor;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppSffManager;

/**
 * Initialize all necessary vpp renderer components
 */
public class SfcVppRenderer {
    private final VppNodeManager nodeManager;
    private final VppRspProcessor rspProcessor;
    private final VppSffManager sffManager;

    public SfcVppRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        nodeManager = new VppNodeManager(dataBroker, bindingAwareBroker);
        rspProcessor = new VppRspProcessor(dataBroker, nodeManager);
        sffManager = new VppSffManager(dataBroker, nodeManager);
    }

    public void unregisterListeners() {
        nodeManager.unregisterVppNodeListener();
        sffManager.unregisterSffListener();
        rspProcessor.unregisterRspListener();
    }
}

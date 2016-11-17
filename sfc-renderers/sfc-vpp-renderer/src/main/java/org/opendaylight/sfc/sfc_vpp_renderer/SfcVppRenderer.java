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
import org.opendaylight.sfc.sfc_vpp_renderer.listener.RenderedPathListener;
import org.opendaylight.sfc.sfc_vpp_renderer.listener.ServiceForwarderListener;
import org.opendaylight.sfc.sfc_vpp_renderer.listener.VppNodeListener;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppNodeManager;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppRspProcessor;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppSffManager;

/**
 * Initialize all necessary vpp renderer components
 */
public class SfcVppRenderer {
    private final RenderedPathListener rspListener;
    private final ServiceForwarderListener sffListener;
    private final VppNodeListener vppNodeListener;

    public SfcVppRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        VppNodeManager vppNodeManager = new VppNodeManager(dataBroker, bindingAwareBroker);
        vppNodeListener = new VppNodeListener(dataBroker, vppNodeManager);

        VppSffManager sffManager = new VppSffManager(dataBroker, vppNodeManager);
        sffListener = new ServiceForwarderListener(dataBroker, sffManager);

        VppRspProcessor rspProcessor = new VppRspProcessor(vppNodeManager);
        rspListener = new RenderedPathListener(dataBroker, rspProcessor);
    }

    public void close() throws Exception {
        vppNodeListener.close();
        rspListener.close();
        sffListener.close();
    }
}

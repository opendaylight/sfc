/*
 * Copyright (c) 2016 Intel, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.vpp;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.renderers.vpp.listeners.RenderedPathListener;
import org.opendaylight.sfc.renderers.vpp.listeners.ServiceForwarderListener;
import org.opendaylight.sfc.renderers.vpp.listeners.VppNodeListener;

/**
 * Initialize all necessary vpp renderer components.
 */
public class SfcVppRenderer {
    private final RenderedPathListener rspListener;
    private final ServiceForwarderListener sffListener;
    private final VppNodeListener vppNodeListener;

    public SfcVppRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        VppNodeManager vppNodeManager = new VppNodeManager(bindingAwareBroker);
        vppNodeListener = new VppNodeListener(dataBroker, vppNodeManager);

        VppSffManager sffManager = new VppSffManager(vppNodeManager);
        sffListener = new ServiceForwarderListener(dataBroker, sffManager);

        VppRspProcessor rspProcessor = new VppRspProcessor(vppNodeManager);
        rspListener = new RenderedPathListener(dataBroker, rspProcessor);
    }

    public void close() {
        vppNodeListener.close();
        rspListener.close();
        sffListener.close();
    }
}

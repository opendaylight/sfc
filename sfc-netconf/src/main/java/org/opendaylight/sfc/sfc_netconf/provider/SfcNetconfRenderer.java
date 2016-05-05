/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.NodeManager;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.NetconfRspProcessor;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.NetconfServiceFunctionMapper;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.NetconfServiceForwarderMapper;

/**
 * Initialize all necessary IOS-XE renderer components
 */
public class SfcNetconfRenderer {

    private final NodeManager nodeManager;
    private final NetconfServiceForwarderMapper forwarderMapper;
    private final NetconfServiceFunctionMapper functionMapper;
    private final NetconfRspProcessor rspProcessor;

    public SfcNetconfRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        nodeManager = new NodeManager(dataBroker, bindingAwareBroker);
        forwarderMapper = new NetconfServiceForwarderMapper(dataBroker, nodeManager);
        functionMapper = new NetconfServiceFunctionMapper(dataBroker, nodeManager);
        rspProcessor = new NetconfRspProcessor(dataBroker, nodeManager);
    }

    public void unregisterListeners() {
        nodeManager.unregisterNodeListener();
        forwarderMapper.unregisterSffListener();
        functionMapper.unregisterSfListener();
        rspProcessor.unregisterRspListener();
    }
}

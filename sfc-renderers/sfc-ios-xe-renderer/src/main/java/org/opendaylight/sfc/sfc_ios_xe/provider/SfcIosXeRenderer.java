/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.IosXeRspProcessor;
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.IosXeServiceForwarderMapper;
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.IosXeServiceFunctionMapper;
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.NodeManager;

/**
 * Initialize all necessary IOS-XE renderer components
 */
public class SfcIosXeRenderer {

    private final NodeManager nodeManager;
    private final IosXeServiceForwarderMapper forwarderMapper;
    private final IosXeServiceFunctionMapper functionMapper;
    private final IosXeRspProcessor rspProcessor;

    public SfcIosXeRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        nodeManager = new NodeManager(dataBroker, bindingAwareBroker);
        forwarderMapper = new IosXeServiceForwarderMapper(dataBroker, nodeManager);
        functionMapper = new IosXeServiceFunctionMapper(dataBroker, nodeManager);
        rspProcessor = new IosXeRspProcessor(dataBroker, nodeManager);
    }

    public void unregisterListeners() {
        nodeManager.unregisterNodeListener();
        forwarderMapper.unregisterSffListener();
        functionMapper.unregisterSfListener();
        rspProcessor.unregisterRspListener();
    }
}

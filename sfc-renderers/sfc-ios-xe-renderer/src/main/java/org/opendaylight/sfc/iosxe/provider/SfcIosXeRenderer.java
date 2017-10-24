/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.iosxe.provider;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.iosxe.provider.renderer.IosXeRspProcessor;
import org.opendaylight.sfc.iosxe.provider.renderer.IosXeServiceForwarderMapper;
import org.opendaylight.sfc.iosxe.provider.renderer.IosXeServiceFunctionMapper;
import org.opendaylight.sfc.iosxe.provider.renderer.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialize all necessary IOS-XE renderer components.
 */
public class SfcIosXeRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(SfcIosXeRenderer.class);

    private NodeManager nodeManager;
    private IosXeServiceForwarderMapper forwarderMapper;
    private IosXeServiceFunctionMapper functionMapper;
    private IosXeRspProcessor rspProcessor;
    private final DataBroker dataBroker;
    private final BindingAwareBroker bindingAwareBroker;

    public SfcIosXeRenderer(final DataBroker dataBroker, final BindingAwareBroker bindingAwareBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.bindingAwareBroker = Preconditions.checkNotNull(bindingAwareBroker);
    }

    public void initialize() {
        forwarderMapper = new IosXeServiceForwarderMapper(dataBroker, nodeManager);
        functionMapper = new IosXeServiceFunctionMapper(dataBroker, nodeManager);
        rspProcessor = new IosXeRspProcessor(dataBroker, nodeManager);
        LOG.info("sfc-ios-xe-renderer started");
    }

    public void unregisterListeners() {
        forwarderMapper.unregisterSffListener();
        functionMapper.unregisterSfListener();
        rspProcessor.unregisterRspListener();
        LOG.info("sfc-ios-xe-renderer closed");
    }
}

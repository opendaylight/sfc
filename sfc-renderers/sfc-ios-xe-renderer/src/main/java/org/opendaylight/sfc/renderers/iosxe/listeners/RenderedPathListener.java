/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.iosxe.listeners;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.renderers.iosxe.IosXeRspProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class RenderedPathListener extends AbstractSyncDataTreeChangeListener<RenderedServicePaths> {

    private final IosXeRspProcessor rspProcessor;

    @Inject
    public RenderedPathListener(DataBroker dataBroker, IosXeRspProcessor rspProcessor) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(RenderedServicePaths.class));
        this.rspProcessor = rspProcessor;
    }

    @Override
    public void add(@NonNull InstanceIdentifier<RenderedServicePaths> instanceIdentifier,
                    @NonNull RenderedServicePaths renderedServicePaths) {
        update(instanceIdentifier, renderedServicePaths, renderedServicePaths);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<RenderedServicePaths> instanceIdentifier,
                       @NonNull RenderedServicePaths renderedServicePaths) {
        renderedServicePaths.getRenderedServicePath().forEach(rspProcessor::deleteRsp);
    }

    @Override
    public void update(@NonNull InstanceIdentifier<RenderedServicePaths> instanceIdentifier,
                       @NonNull RenderedServicePaths originalRenderedServicePaths,
                       @NonNull RenderedServicePaths updatedRenderedServicePaths) {
        if (updatedRenderedServicePaths.getRenderedServicePath() != null) {
            updatedRenderedServicePaths.getRenderedServicePath().forEach(rspProcessor::updateRsp);
        }
    }
}

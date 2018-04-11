/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.vpp.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.renderers.vpp.VppRspProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RenderedPathListener extends AbstractSyncDataTreeChangeListener<RenderedServicePath> {

    private static final Logger LOG = LoggerFactory.getLogger(RenderedPathListener.class);

    private final VppRspProcessor rspProcessor;

    @Inject
    public RenderedPathListener(DataBroker dataBroker, VppRspProcessor rspProcessor) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(RenderedServicePaths.class).child(RenderedServicePath.class));
        this.rspProcessor = rspProcessor;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                    @Nonnull RenderedServicePath renderedServicePath) {
        LOG.debug("RSP created");
        this.rspProcessor.updateRsp(renderedServicePath);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @Nonnull RenderedServicePath renderedServicePath) {
        LOG.debug("RSP deleted");
        this.rspProcessor.deleteRsp(renderedServicePath);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @Nonnull RenderedServicePath originalRenderedServicePath,
                       @Nonnull RenderedServicePath updatedRenderedServicePath) {
        // Needn't care it
    }
}

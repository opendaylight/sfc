/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vpp_renderer.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppRspProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RenderedPathListener extends AbstractDataTreeChangeListener<RenderedServicePaths> {

    private final VppRspProcessor rspProcessor;
    private final ListenerRegistration<RenderedPathListener> vppRspListenerRegistration;
    private static final Logger LOG = LoggerFactory.getLogger(RenderedPathListener.class);

    public RenderedPathListener(DataBroker dataBroker, VppRspProcessor rspProcessor) {
        this.rspProcessor = rspProcessor;
        // Register listener
        final DataTreeIdentifier<RenderedServicePaths> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(RenderedServicePaths.class));
        vppRspListenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (vppRspListenerRegistration != null) {
            vppRspListenerRegistration.close();
        }
    }

    @Override
    protected void add(RenderedServicePaths newDataObject) {
        LOG.info("RSPs created");
        newDataObject.getRenderedServicePath().forEach(rspProcessor::updateRsp);
    }

    @Override
    protected void remove(RenderedServicePaths removedDataObject) {
        LOG.info("RSPs deleted");
        removedDataObject.getRenderedServicePath().forEach(rspProcessor::deleteRsp);
    }

    @Override
    protected void update(RenderedServicePaths originalDataObject, RenderedServicePaths updatedDataObject) {
        LOG.info("RSPs updated");
        updatedDataObject.getRenderedServicePath().forEach(rspProcessor::updateRsp);
    }
}

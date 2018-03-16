/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestRspTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestRspEntryDataListener extends AbstractSyncDataTreeChangeListener<RenderedServicePath> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestRspEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestRspEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, SfcInstanceIdentifiers.RSP_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                    @Nonnull RenderedServicePath renderedServicePath) {
        update(instanceIdentifier,
               renderedServicePath, renderedServicePath);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @Nonnull RenderedServicePath renderedServicePath) {
        LOG.debug("Deleted Rendered Service Path Name: {}", renderedServicePath.getName());
        new SbRestRspTask(RestOperation.DELETE, renderedServicePath, executorService).run();
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @Nonnull RenderedServicePath originalRenderedServicePath,
                       @Nonnull RenderedServicePath updatedRenderedServicePath) {
        LOG.debug("Updated Rendered Service Path: {}", updatedRenderedServicePath.getName());
        new SbRestRspTask(RestOperation.PUT, updatedRenderedServicePath, executorService).run();
    }
}

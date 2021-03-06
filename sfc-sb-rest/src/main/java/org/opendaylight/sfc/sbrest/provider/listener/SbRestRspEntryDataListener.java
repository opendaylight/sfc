/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
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
    public void add(@NonNull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                    @NonNull RenderedServicePath renderedServicePath) {
        update(instanceIdentifier,
               renderedServicePath, renderedServicePath);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @NonNull RenderedServicePath renderedServicePath) {
        LOG.debug("Deleted Rendered Service Path Name: {}", renderedServicePath.getName());
        new SbRestRspTask(RestOperation.DELETE, renderedServicePath, executorService).run();
    }

    @Override
    public void update(@NonNull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @NonNull RenderedServicePath originalRenderedServicePath,
                       @NonNull RenderedServicePath updatedRenderedServicePath) {
        LOG.debug("Updated Rendered Service Path: {}", updatedRenderedServicePath.getName());
        new SbRestRspTask(RestOperation.PUT, updatedRenderedServicePath, executorService).run();
    }
}

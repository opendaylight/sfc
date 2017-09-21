/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestRspTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestRspEntryDataListener extends SbRestAbstractDataListener<RenderedServicePath> {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestRspEntryDataListener.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public SbRestRspEntryDataListener(DataBroker dataBroker) {
        super(dataBroker, SfcInstanceIdentifiers.RSP_ENTRY_IID, LogicalDatastoreType.OPERATIONAL);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<RenderedServicePath>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<RenderedServicePath> change: changes) {
            DataObjectModification<RenderedServicePath> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    RenderedServicePath updatedPath = rootNode.getDataAfter();
                    LOG.debug("\nUpdated Rendered Service Path: {}", updatedPath.getName());

                    RestOperation restOp = rootNode.getDataBefore() == null ? RestOperation.POST : RestOperation.PUT;
                    executor.execute(new SbRestRspTask(restOp, updatedPath, executor));
                    break;
                case DELETE:
                    RenderedServicePath originalPath = rootNode.getDataBefore();
                    LOG.debug("\nDeleted Rendered Service Path Name: {}", originalPath.getName());

                    executor.execute(new SbRestRspTask(RestOperation.DELETE, originalPath, executor));
                    break;
                default:
                    break;
            }
        }

        printTraceStop(LOG);
    }
}

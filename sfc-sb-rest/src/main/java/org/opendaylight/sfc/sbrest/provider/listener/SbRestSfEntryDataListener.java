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
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestSfEntryDataListener extends SbRestAbstractDataListener<ServiceFunction> {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfEntryDataListener.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public SbRestSfEntryDataListener(DataBroker dataBroker) {
        super(dataBroker, SfcInstanceIdentifiers.SF_ENTRY_IID, LogicalDatastoreType.CONFIGURATION);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceFunction>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<ServiceFunction> change: changes) {
            DataObjectModification<ServiceFunction> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    ServiceFunction updatedServiceFunction = rootNode.getDataAfter();
                    LOG.debug("\nUpdated Service Function Name: {}", updatedServiceFunction.getName());

                    executor.execute(new SbRestSfTask(RestOperation.PUT, updatedServiceFunction, executor));
                    break;
                case DELETE:
                    ServiceFunction originalServiceFunction = rootNode.getDataBefore();
                    LOG.debug("\nDeleted Service Function Name: {}", originalServiceFunction.getName());

                    executor.execute(new SbRestSfTask(RestOperation.DELETE, originalServiceFunction, executor));
                    break;
                default:
                    break;
            }
        }

        printTraceStop(LOG);
    }
}

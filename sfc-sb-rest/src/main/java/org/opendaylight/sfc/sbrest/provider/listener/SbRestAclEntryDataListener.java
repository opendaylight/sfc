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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestAclTask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestAclEntryDataListener extends SbRestAbstractDataListener<Acl> {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestAclEntryDataListener.class);

    public SbRestAclEntryDataListener(DataBroker dataBroker, ExecutorService executor) {
        super(dataBroker, SfcInstanceIdentifiers.ACL_ENTRY_IID, LogicalDatastoreType.CONFIGURATION, executor);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Acl>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<Acl> change: changes) {
            DataObjectModification<Acl> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    Acl updatedAcl = rootNode.getDataAfter();
                    LOG.debug("\nUpdated Access List Name: {}", updatedAcl.getAclName());

                    RestOperation restOp = rootNode.getDataBefore() == null ? RestOperation.POST : RestOperation.PUT;
                    executor().execute(new SbRestAclTask(restOp, updatedAcl, executor()));
                    break;
                case DELETE:
                    Acl originalAcl = rootNode.getDataBefore();
                    LOG.debug("\nDeleted Access List Name: {}", originalAcl.getAclName());

                    executor().execute(new SbRestAclTask(RestOperation.DELETE, originalAcl, executor()));
                    break;
                default:
                    break;
            }
        }

        printTraceStop(LOG);
    }
}

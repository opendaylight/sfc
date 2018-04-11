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
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestAclTask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestAclEntryDataListener extends AbstractSyncDataTreeChangeListener<Acl> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestAclEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestAclEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.ACL_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@Nonnull Acl acl) {
        update(acl, acl);
    }

    @Override
    public void remove(@Nonnull Acl acl) {
        LOG.debug("Deleted Access List Name: {}", acl.getAclName());
        new SbRestAclTask(RestOperation.DELETE, acl, executorService).run();
    }

    @Override
    public void update(@Nonnull Acl originalAcl, @Nonnull Acl updatedAcl) {
        LOG.debug("Updated Access List Name: {}", updatedAcl.getAclName());
        new SbRestAclTask(RestOperation.PUT, updatedAcl, executorService).run();
    }
}

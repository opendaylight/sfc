/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.utils;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SfcPotNetconfReaderWriterAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfReaderWriterAPI.class);

    private SfcPotNetconfReaderWriterAPI() {
    }

    public static <T extends DataObject> boolean put(DataBroker broker, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<T> iid, T dataObject) {
        try {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            tx.put(logicalDatastoreType, iid, dataObject);
            ListenableFuture<Void> future = tx.submit();
            future.get();

            return true;
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("iOAM:PoT:SB:Netconf put to nodeid: failed:", e);
            return false;
        }
    }

    public static <T extends DataObject> boolean delete(DataBroker broker, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<T> iid) {
        try {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            tx.delete(logicalDatastoreType, iid);
            ListenableFuture<Void> future = tx.submit();
            future.get();

            return true;
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("iOAM:PoT:SB:Netconf delete to nodeid failed:", e);
            return false;
        }
    }
}

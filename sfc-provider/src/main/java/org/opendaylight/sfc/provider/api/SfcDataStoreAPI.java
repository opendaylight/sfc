/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around DataStore APIs. These methods take care of retries and
 * callbacks automatically.
 *
 * <p>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-11-22
 */
public final class SfcDataStoreAPI {
    private static DataBroker dataBroker = null;
    private static final Logger LOG = LoggerFactory.getLogger(SfcDataStoreAPI.class);

    // blueprint setter
    // FIXME - Suppress FB violation. This class should really be a normal instance and not use statics.
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void setDataProvider(DataBroker broker) {
        SfcDataStoreAPI.dataBroker = broker;
    }

    // Auxiliary static setter just for testing, because in UT we can't use
    // blueprint, so the injection should be manual
    public static void setDataProviderAux(DataBroker broker) {
        SfcDataStoreAPI.dataBroker = broker;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean deleteTransactionAPI(
            InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType) {
        if (dataBroker == null) {
            LOG.error("deleteTransactionAPI: dataBroker not initialized!");
            return false;
        }

        FluentFuture<? extends CommitInfo> commitFuture = deleteTransactionAsyncAPI(deleteIID, logicalDatastoreType);
        try {
            commitFuture.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("deleteTransactionAPI: Transaction failed", e);
            return false;
        }
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject>
        FluentFuture<? extends CommitInfo> deleteTransactionAsyncAPI(
            InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType) {
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(logicalDatastoreType, deleteIID);

        return writeTx.commit();
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writeMergeTransactionAPI(
            InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        if (dataBroker == null) {
            LOG.error("writeMergeTransactionAPI: dataBroker not initialized!");
            return false;
        }

        FluentFuture<? extends CommitInfo> commitFuture =
                writeMergeTransactionAsyncAPI(addIID, data, logicalDatastoreType);

        try {
            commitFuture.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("writeMergeTransactionAPI: Transaction failed", e);
            return false;
        }
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject>
        FluentFuture<? extends CommitInfo> writeMergeTransactionAsyncAPI(
            InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(logicalDatastoreType, addIID, data, true);

        return writeTx.commit();
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writePutTransactionAPI(
            InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        if (dataBroker == null) {
            LOG.error("writePutTransactionAPI: dataBroker not initialized!");
            return false;
        }

        FluentFuture<? extends CommitInfo> commitFuture =
                writePutTransactionAsyncAPI(addIID, data, logicalDatastoreType);

        try {
            commitFuture.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("writePutTransactionAPI: Transaction failed", e);
            return false;
        }
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject>
        FluentFuture<? extends CommitInfo> writePutTransactionAsyncAPI(
            InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(logicalDatastoreType, addIID, data, true);

        return writeTx.commit();
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> U readTransactionAPI(
            InstanceIdentifier<U> readIID, LogicalDatastoreType logicalDatastoreType) {
        U ret = null;

        if (dataBroker == null) {
            LOG.error("readTransactionAPI: dataBroker not initialized!");
            return ret;
        }
        ListenableFuture<Optional<U>> submitFuture = readTransactionAsyncAPI(readIID, logicalDatastoreType);
        Optional<U> optionalDataObject;
        try {
            optionalDataObject = submitFuture.get();
            if (optionalDataObject != null && optionalDataObject.isPresent()) {
                ret = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read", Thread.currentThread().getStackTrace()[1]);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("failed to ....", e);
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject>
        ListenableFuture<Optional<U>> readTransactionAsyncAPI(
            InstanceIdentifier<U> readIID, LogicalDatastoreType logicalDatastoreType) {
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();

        return readTx.read(logicalDatastoreType, readIID);
    }
}

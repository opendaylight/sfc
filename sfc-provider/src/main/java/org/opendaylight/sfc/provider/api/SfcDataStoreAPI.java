/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
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
    private static DataBroker dataProvider = null;
    private static final Logger LOG = LoggerFactory.getLogger(SfcDataStoreAPI.class);

    // blueprint setter
    // FIXME - Suppress FB violation. This class should really be a normal instance and not use statics.
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void setDataProvider(DataBroker broker) {
        dataProvider = broker;
    }

    // Auxiliary static setter just for testing, because in UT we can't use
    // blueprint,
    // so the injection should be manual
    public static void setDataProviderAux(DataBroker broker) {
        dataProvider = broker;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean deleteTransactionAPI(
            InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType) {
        boolean ret = false;

        if (dataProvider == null) {
            LOG.error("deleteTransactionAPI: dataProvider not initialized!");
            return ret;
        }
        WriteTransaction writeTx = dataProvider.newWriteOnlyTransaction();
        writeTx.delete(logicalDatastoreType, deleteIID);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("deleteTransactionAPI: Transaction failed", e);
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writeMergeTransactionAPI(
            InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        boolean ret = false;
        if (dataProvider == null) {
            LOG.error("writeMergeTransactionAPI: dataProvider not initialized!");
            return ret;
        }
        WriteTransaction writeTx = dataProvider.newWriteOnlyTransaction();
        writeTx.merge(logicalDatastoreType, addIID, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("writeMergeTransactionAPI: Transaction failed", e);
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writePutTransactionAPI(
            InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        boolean ret = false;
        if (dataProvider == null) {
            LOG.error("writePutTransactionAPI: dataProvider not initialized!");
            return ret;
        }
        WriteTransaction writeTx = dataProvider.newWriteOnlyTransaction();
        writeTx.put(logicalDatastoreType, addIID, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("writePutTransactionAPI: Transaction failed", e);
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> U readTransactionAPI(
            InstanceIdentifier<U> readIID, LogicalDatastoreType logicalDatastoreType) {
        U ret = null;

        if (dataProvider == null) {
            LOG.error("readTransactionAPI: dataProvider not initialized!");
            return ret;
        }
        ReadOnlyTransaction readTx = dataProvider.newReadOnlyTransaction();
        Optional<U> optionalDataObject;
        CheckedFuture<Optional<U>, ReadFailedException> submitFuture = readTx.read(logicalDatastoreType, readIID);
        try {
            optionalDataObject = submitFuture.checkedGet();
            if (optionalDataObject != null && optionalDataObject.isPresent()) {
                ret = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read", Thread.currentThread().getStackTrace()[1]);
            }
        } catch (ReadFailedException e) {
            LOG.warn("failed to ....", e);
        }
        return ret;
    }
}

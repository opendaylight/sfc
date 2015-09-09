/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around DataStore APIs. These methods take care of retries and callbacks
 * automatically.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-11-22
 */
public class SfcDataStoreAPI {

    // Hiding the implicit public constructor
    private SfcDataStoreAPI() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(SfcDataStoreAPI.class);

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean deleteTransactionAPI
            (InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType)  {
        boolean ret;

        WriteTransaction writeTx = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newWriteOnlyTransaction();
        writeTx.delete(logicalDatastoreType, deleteIID);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("deleteTransactionAPI: Transaction failed. Message: {}", e.getMessage());
            ret = false;
        }
        return ret;
    }


    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writeMergeTransactionAPI
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        boolean ret;

        WriteTransaction writeTx = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newWriteOnlyTransaction();
        writeTx.merge(logicalDatastoreType, addIID, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("writeMergeTransactionAPI: Transaction failed. Message: {}", e.getMessage());
            ret = false;
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writePutTransactionAPI
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType)  {
        boolean ret;
        WriteTransaction writeTx = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newWriteOnlyTransaction();
        writeTx.put(logicalDatastoreType, addIID, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("writePutTransactionAPI: Transaction failed. Message: {}", e.getMessage());
            ret = false;
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> U readTransactionAPI
            (InstanceIdentifier<U> readIID, LogicalDatastoreType logicalDatastoreType)  {
        U ret = null;
        ReadOnlyTransaction readTx = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newReadOnlyTransaction();
        Optional<U> optionalDataObject;
        CheckedFuture<Optional<U>, ReadFailedException> submitFuture = readTx.read(logicalDatastoreType, readIID);
        try {
            optionalDataObject = submitFuture.checkedGet();
            if (optionalDataObject != null
                    && optionalDataObject.isPresent()) {
                ret = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read",
                        Thread.currentThread().getStackTrace()[1]);
            }
        } catch (ReadFailedException e) {
            LOG.warn("failed to ...." , e);
        }
        return ret;
    }
}

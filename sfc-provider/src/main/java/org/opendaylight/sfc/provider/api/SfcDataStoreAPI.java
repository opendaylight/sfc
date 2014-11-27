package org.opendaylight.sfc.provider.api;

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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

    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static final Logger LOG = LoggerFactory.getLogger(SfcDataStoreAPI.class);
    private static final DataBroker dataBroker = odlSfc.getDataProvider();

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean deleteTransactionAPI
            (InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType)  {
        boolean ret = false;

        SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(logicalDatastoreType, deleteIID);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        Futures.addCallback(submitFuture, sfcDataStoreCallback, odlSfc.executor);
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed. Message: {}", e.getMessage());
            ret = false;
        }
        return ret;
    }
    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writeMergeTransactionAPI
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        boolean ret = false;

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(logicalDatastoreType, addIID, data, true);
        SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        Futures.addCallback(submitFuture, sfcDataStoreCallback, odlSfc.executor);
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed. Message: {}", e.getMessage());
            ret = false;
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writePutTransactionAPI
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType)  {
        boolean ret = false;
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(logicalDatastoreType, addIID, data, true);
        SfcDataStoreCallback sfcDataStoreCallback = new SfcDataStoreCallback();
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        Futures.addCallback(submitFuture, sfcDataStoreCallback, odlSfc.executor);
        try {
            submitFuture.checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed. Message: {}", e.getMessage());
            ret = false;
        }
        return ret;
    }

    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> boolean writeSynchPutTransactionAPI
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType)  {
        boolean ret = false;
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(logicalDatastoreType, addIID, data, true);
        writeTx.commit();
        ret = true;
        return ret;
    }

}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.util.concurrent.FutureCallback;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the general DataStore checked future
 * callback. transaction_progress denotes whether the
 * transaction is still in progress, in which case
 * transaction_successful value does not reflect the
 * outcome of the transaction.
 * <p/>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * <p/>
 * @since       2014-06-30
 */
public class SfcDataStoreCallback implements FutureCallback<Void>
{
    private static final Logger LOG = LoggerFactory.getLogger(SfcDataStoreCallback.class);
    private boolean transaction_successful;
    private boolean transaction_progress;

    public SfcDataStoreCallback()
    {
        this.transaction_progress = true;
    }

    public boolean getTransactioSuccessful() {return transaction_successful; }

    public boolean getTransactioProgress() {return transaction_progress; }

    @Override
    public void onSuccess(final Void result)
    {
        // Commited successfully
        this.transaction_successful = true;
        this.transaction_progress = false;
    }

    @Override
    public void onFailure(final Throwable t)
    {
        // Transaction failed
        this.transaction_successful = false;
        this.transaction_progress = false;

        if (t instanceof OptimisticLockFailedException)
        {
            // Failed because of concurrent transaction modifying same data
        } else
        {
            // Some other type of TransactionCommitFailedException
        }
    }
}

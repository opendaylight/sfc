/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds SFC Concurrency high level APIs.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2015-06-23
 */

public final class SfcConcurrencyAPI {
    private static final Lock SFC_LOCK = new ReentrantLock();
    private static final Lock SFC_PATHID_LOCK = new ReentrantLock();

    private static final Logger LOG = LoggerFactory.getLogger(SfcConcurrencyAPI.class);

    private SfcConcurrencyAPI() {
    }

    public static boolean getLock() {
        try {
            if (SFC_LOCK.tryLock(2000, TimeUnit.MILLISECONDS)) {
                return true;
            }
        } catch (InterruptedException e) {
            LOG.error("Failed to Acquire Lock");
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public static void releaseLock() {
        SFC_LOCK.unlock();
    }

    public static boolean getPathIdLock() {
        try {
            if (SFC_PATHID_LOCK.tryLock(2000, TimeUnit.MILLISECONDS)) {
                return true;
            }
        } catch (InterruptedException e) {
            LOG.error("Failed to Acquire Lock");
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public static void releasePathIdLock() {
        SFC_PATHID_LOCK.unlock();
    }
}

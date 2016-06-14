/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.utils;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple synchronization class used to synchronize different events in SFC.
 * Encapsulates the usage of a ReentrantLock.
 *
 * @author ebrjohn
 *
 */
public class SfcSynchronizer {
    private ReentrantLock lock;

    public SfcSynchronizer() {
        lock = new ReentrantLock();
    }

    /**
     * To be called by threads that need to lock.
     * This is a blocking call.
     */
    public void lock() {
        lock.lock();
    }

    /**
     * Query if the lock is available
     *
     * @return true if no threads are waiting on the lock, false otherwise
     */
    public boolean isLocked() {
        return lock.isLocked();
    }

    /**
     * Release the lock
     */
    public void unlock() {
        lock.unlock();
    }
}

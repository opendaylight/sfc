/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.opendaylight.sfc.provider.api.SfcThreadPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcFixedThreadPoolImpl implements SfcThreadPoolService {

    private static final Logger LOG = LoggerFactory.getLogger(SfcFixedThreadPoolImpl.class);
    private final ExecutorService executor;
    private final long shutdownTime;
    private static SfcFixedThreadPoolImpl instance;

    public SfcFixedThreadPoolImpl(int threadCount, ThreadFactory factory, long shutdownTime) {
        this.executor = Executors.newFixedThreadPool(threadCount, factory);
        this.shutdownTime = shutdownTime;
        if (instance == null) {
            instance = this;
        }
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
        if (!executor.awaitTermination(shutdownTime, TimeUnit.SECONDS)) {
            LOG.error("Executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = executor.shutdownNow();
            LOG.error("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
        }
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    public static SfcFixedThreadPoolImpl getSfcFixedThreadPoolImpl() {
        return SfcFixedThreadPoolImpl.instance;
    }
}

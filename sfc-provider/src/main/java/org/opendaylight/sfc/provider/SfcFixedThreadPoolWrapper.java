/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class SfcFixedThreadPoolWrapper implements ThreadPool, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcFixedThreadPoolWrapper.class);
    private final ThreadPoolExecutor executor;

    public SfcFixedThreadPoolWrapper(int threadCount, boolean isDaemon, String nameFormat) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount,
                new ThreadFactoryBuilder().setNameFormat(nameFormat).setDaemon(isDaemon).build());
    }

    @Override
    public void close () {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SfcProviderUtils.SHUTDOWN_TIME, TimeUnit.SECONDS)) {
                LOG.error("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = executor.shutdownNow();
                LOG.error("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
            }
        } catch (InterruptedException e) {
            LOG.error("Executor Thread interrupted while waiting", e);
        }
    }

    @Override
    public ExecutorService getExecutor() {
        return Executors.unconfigurableExecutorService(executor);
    }

    @Override
    public int getMaxThreadCount() {
        return executor.getMaximumPoolSize();
    }
}

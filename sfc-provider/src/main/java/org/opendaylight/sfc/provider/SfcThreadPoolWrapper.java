/*
 * Copyright (c) 2016 Ericsson Spain S.A.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.config.threadpool.util.FixedThreadPoolWrapper;
import java.util.concurrent.ThreadFactory;
import static org.opendaylight.sfc.provider.OpendaylightSfc.EXECUTOR_THREAD_POOL_SIZE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This the main SfcThreadPoolWrapper Provider class.
 * It is instantiated from the SFCProviderModule class.
 * This class should be instantiated from blueprint, so couldn't be a singleton never.
 * <p>
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @author Guillermo Tomasini (gtomasini@gmail.com)
 * @version 0.1
 * @since 2016-09-30
 */
public class SfcThreadPoolWrapper implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SfcThreadPoolWrapper.class);
    private static FixedThreadPoolWrapper poolWrapperInstance=null;
    private static final ThreadFactory THREAD_FACTORY =
               new ThreadFactoryBuilder().setNameFormat("SFC-%d").setDaemon(false).build();
    private static SfcThreadPoolWrapper instance=null;
    private static int poolSize = EXECUTOR_THREAD_POOL_SIZE;

    public SfcThreadPoolWrapper(int poolsize){
        if (poolsize > poolSize)
            poolSize=poolsize;
        poolWrapperInstance =new FixedThreadPoolWrapper(poolSize, THREAD_FACTORY);
        instance = this;
        LOG.info("SfcThreadPoolWrapper Initialized.");
    }

    public static ExecutorService getExecutor() {
        return poolWrapperInstance.getExecutor();
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        poolWrapperInstance.close();
    }

    public static SfcThreadPoolWrapper getInstance(){
        return instance;
    }

    public static SfcThreadPoolWrapper getThreadpoolwrapper(){
        return instance;
    }
}

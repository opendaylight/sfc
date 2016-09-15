/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import org.opendaylight.controller.config.threadpool.util.FixedThreadPoolWrapper;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class SfcFixedThreadPoolWrapper extends FixedThreadPoolWrapper {

    public SfcFixedThreadPoolWrapper(int threadCount, boolean isDaemon, String nameFormat) {
        super(threadCount, new ThreadFactoryBuilder().setNameFormat(nameFormat).setDaemon(isDaemon).build());
    }
}

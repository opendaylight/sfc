/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.sfc.sbrest.json.ExporterFactory;
import org.opendaylight.yangtools.yang.binding.DataObject;


abstract public class SbRestAbstractTask implements Runnable {

    protected static final int THREAD_POOL_SIZE = 50;
    protected ExecutorService taskExecutor;
    protected ExecutorService odlExecutor;

    protected RestOperation restOperation;
    protected ExporterFactory exporterFactory;
    protected String jsonObject = null;

    protected List<String> restUriList = null;

    public SbRestAbstractTask(RestOperation restOperation, ExecutorService odlExecutor) {

        this.restOperation = restOperation;
        this.taskExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.odlExecutor = odlExecutor;
    }

    @Override
    public void run() {
        submitTasks(jsonObject);
        taskExecutor.shutdown();
    }

    private void submitTasks(String json) {
        if (this.restUriList != null && this.restUriList.size() > 0) {
            for (String restUri : this.restUriList) {
                taskExecutor.submit(new WsTask(restUri, restOperation, json));
            }
        }
    }

    abstract protected void setRestUriList(DataObject o);

}

/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import org.opendaylight.sfc.sbrest.json.ExporterFactory;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class SbRestAbstractTask implements Runnable {

    protected ExecutorService odlExecutor;

    protected RestOperation restOperation;
    protected ExporterFactory exporterFactory;
    protected String jsonObject = null;

    protected List<String> restUriList = null;

    public SbRestAbstractTask(RestOperation restOperation, ExecutorService odlExecutor) {

        this.restOperation = restOperation;
        this.odlExecutor = odlExecutor;
    }

    @Override
    public void run() {
        submitTasks(jsonObject);
    }

    private void submitTasks(String json) {
        if (this.restUriList != null && this.restUriList.size() > 0) {
            for (String restUri : this.restUriList) {
                odlExecutor.execute(new WsTask(restUri, restOperation, json));
            }
        }
    }

    protected abstract void setRestUriList(DataObject dataObject);
}

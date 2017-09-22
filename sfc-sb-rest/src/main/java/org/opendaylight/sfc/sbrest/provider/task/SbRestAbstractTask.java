/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.sfc.sbrest.json.ExporterFactory;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class SbRestAbstractTask implements Runnable {

    private final ExecutorService odlExecutor;
    private final RestOperation restOperation;
    private final String jsonObject;
    private final List<String> restUriList = new ArrayList<>();

    public SbRestAbstractTask(@Nonnull RestOperation restOperation, @Nonnull ExporterFactory exporterFactory,
            @Nullable DataObject dataObject, @Nonnull ExecutorService odlExecutor) {
        this.restOperation = restOperation;
        this.odlExecutor = odlExecutor;

        if (dataObject == null) {
            this.jsonObject = null;
        } else if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        }
    }

    @Override
    public void run() {
        for (String restUri : this.restUriList) {
            odlExecutor.execute(new WsTask(restUri, restOperation, jsonObject));
        }
    }

    protected void addRestUri(String uri) {
        restUriList.add(uri);
    }

    @VisibleForTesting
    String getJsonObject() {
        return jsonObject;
    }

    @VisibleForTesting
    List<String> getRestUriListCopy() {
        return Collections.unmodifiableList(restUriList);
    }
}

/*
 * Copyright (c) 2015, 2017 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import java.util.concurrent.ExecutorService;
import org.opendaylight.sfc.sbrest.json.SfstExporterFactory;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestSfstTask extends SbRestAbstractTask {
    private static final String SFST_REST_URI =
            "/config/service-function-scheduler-type:service-function-scheduler-types/service-function-scheduler-type/";
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfstTask.class);

    public SbRestSfstTask(RestOperation restOperation, ServiceFunctionSchedulerType dataObject,
            ExecutorService odlExecutor) {
        super(restOperation, new SfstExporterFactory(), dataObject, odlExecutor);
        setRestUriList(dataObject);
    }

    private void setRestUriList(ServiceFunctionSchedulerType obj) {
        String restUri = SFST_REST_URI + obj.getName();
        addRestUri(restUri);
        LOG.info("SF Schedule Type will be send to REST URI {}", restUri);
    }
}

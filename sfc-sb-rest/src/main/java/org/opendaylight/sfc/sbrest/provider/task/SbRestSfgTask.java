/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import org.opendaylight.sfc.sbrest.json.SfgExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SbRestSfgTask extends SbRestAbstractTask {

    private static final String SFG_REST_URI = "/config/service-function-group:service-function-groups/service-function-group/";
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfgTask.class);

    public SbRestSfgTask(RestOperation restOperation, ServiceFunctionGroup dataObject, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        this.exporterFactory = new SfgExporterFactory();
        if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        }
        setRestUriList(dataObject);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        ServiceFunctionGroup obj = (ServiceFunctionGroup) dataObject;

        if (obj.getRestUri() != null) {
            String restUri = obj.getRestUri().getValue() + SFG_REST_URI + obj.getName();
            this.restUriList = new ArrayList<>();
            this.restUriList.add(restUri);
            LOG.info("SFG will be send to REST URI {}", restUri);
        } else {
            this.restUriList = null;
        }
    }
}

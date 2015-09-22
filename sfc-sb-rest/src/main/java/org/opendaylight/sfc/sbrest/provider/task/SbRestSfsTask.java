/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import org.opendaylight.sfc.sbrest.json.SfsExporterFactory;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class SbRestSfsTask extends SbRestAbstractTask {
    private static final String SFS_REST_URI = "/operational/service-function:service-functions-state/service-function-state/";
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfsTask.class);

    public SbRestSfsTask(RestOperation restOperation, ServiceFunctionState dataObject, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        this.exporterFactory = new SfsExporterFactory();
        if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        }
        setRestUriList(dataObject);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        ServiceFunctionState obj = (ServiceFunctionState) dataObject;

        this.restUriList = new ArrayList<>();

        if(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(obj.getName())!=null) {
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(obj.getName());
            if (serviceFunction.getRestUri() != null) {
                String restUri = serviceFunction.getRestUri().getValue() + SFS_REST_URI + obj.getName();
                this.restUriList.add(restUri);
                LOG.info("Service Function state will be sent to REST URI {}", restUri);
            } else {
                this.restUriList = null;
            }
        } else {
            this.restUriList = null;
        }

    }

}

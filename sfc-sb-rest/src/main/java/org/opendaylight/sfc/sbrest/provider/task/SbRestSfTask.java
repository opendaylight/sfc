/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import org.opendaylight.sfc.sbrest.json.SfExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class SbRestSfTask extends SbRestAbstractTask {

    public SbRestSfTask(RestOperation restOperation, ServiceFunction dataObject, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        this.exporterFactory = new SfExporterFactory();
        if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        }
        setRestUriList(dataObject);
        System.out.println("*** jsonObject=" + jsonObject);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        ServiceFunction obj = (ServiceFunction) dataObject;

        this.restUriList = new ArrayList<>();
        this.restUriList.add(obj.getRestUri().getValue()
                + "/config/service-function:service-functions/service-function/" + obj.getName());
    }
}

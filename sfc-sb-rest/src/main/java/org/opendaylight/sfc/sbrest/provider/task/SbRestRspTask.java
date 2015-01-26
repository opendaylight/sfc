/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sbrest.json.RspExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class SbRestRspTask extends SbRestAbstractTask {

    public SbRestRspTask(RestOperation restOperation, RenderedServicePath dataObject, ExecutorService odlExecutor) {

        super(restOperation, odlExecutor);
        this.exporterFactory = new RspExporterFactory();
        this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        //System.out.println("*** RSP JSON:" + this.jsonObject);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        RenderedServicePath obj = (RenderedServicePath) dataObject;
        List<RenderedServicePathHop> hopList = obj.getRenderedServicePathHop();

        this.restUriList = new ArrayList<>();

        for (RenderedServicePathHop hop : hopList) {
            ServiceFunctionForwarder sff =
                    SfcProviderServiceForwarderAPI
                            .readServiceFunctionForwarderExecutor(hop.getServiceFunctionForwarder());
            this.restUriList.add(sff.getRestUri().getValue()
                    + "/operational/rendered-service-path:rendered-service-paths/rendered-service-path/"
                    + obj.getName());
        }
    }

}

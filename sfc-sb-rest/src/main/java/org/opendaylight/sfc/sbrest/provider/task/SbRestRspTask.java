/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sbrest.json.RspExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SbRestRspTask extends SbRestAbstractTask {

    private static final String RSP_REST_URI = "/operational/rendered-service-path:rendered-service-paths/rendered-service-path/";
    private static final Logger LOG = LoggerFactory.getLogger(SbRestRspTask.class);

    public SbRestRspTask(RestOperation restOperation, RenderedServicePath dataObject, ExecutorService odlExecutor) {

        super(restOperation, odlExecutor);
        this.exporterFactory = new RspExporterFactory();
        if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        }
        setRestUriList(dataObject);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        RenderedServicePath obj = (RenderedServicePath) dataObject;
        List<RenderedServicePathHop> hopList = obj.getRenderedServicePathHop();

        this.restUriList = new ArrayList<>();

        if (hopList != null) {
            for (RenderedServicePathHop hop : hopList) {
                ServiceFunctionForwarder sff =
                        SfcProviderServiceForwarderAPI
                                .readServiceFunctionForwarder(hop.getServiceFunctionForwarder());
                if (sff != null && sff.getRestUri() != null) {
                    RspName rspName = obj.getName();
                    if (rspName != null) {
                        String restUri = sff.getRestUri().getValue() + RSP_REST_URI + rspName.getValue();
                        this.restUriList.add(restUri);
                        LOG.info("RSP will be send to REST URI {}", restUri);
                    }
                }
            }
        }

        if (this.restUriList != null && this.restUriList.isEmpty()) {
            this.restUriList = null;
        }
    }
}

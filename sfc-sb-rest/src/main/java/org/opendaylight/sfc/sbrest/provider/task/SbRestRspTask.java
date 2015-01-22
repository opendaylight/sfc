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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class SbRestRspTask extends SbRestAbstractTask {

    public SbRestRspTask(RestOperation restOperation, RenderedServicePath dataObject, ExecutorService odlExecutor) {

        super(restOperation, odlExecutor);

        this.exporterFactory = new RspExporterFactory();
        this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        RenderedServicePath obj = (RenderedServicePath) dataObject;
        List<RenderedServicePathHop> hopList = obj.getRenderedServicePathHop();

        this.restUriList = new ArrayList<>();

        // TODO this will change once there will be real reference
        List<String> sffRefList = new ArrayList<>();
        for (RenderedServicePathHop hop : hopList) {
            sffRefList.add(hop.getServiceFunctionForwarder());
        }

        // getting all SFFs from datastore
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();
        try {
            Object o = odlExecutor.submit(SfcProviderServiceForwarderAPI
                    .getReadAll(new Object[]{}, new Class[]{})).get();
            ServiceFunctionForwarders serviceFunctionForwarders = (ServiceFunctionForwarders) o;
            sffList = serviceFunctionForwarders.getServiceFunctionForwarder();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // Map for search by SFF name
        Map<String, ServiceFunctionForwarder> sffMap = new HashMap<>();
        for (ServiceFunctionForwarder sff : sffList) {
            sffMap.put(sff.getName(), sff);
        }
        for (String sffRef : sffRefList) {
            if (sffMap.containsKey(sffRef)) {
                this.restUriList.add(sffMap.get(sffRef).getRestUri().getValue()
                        + "/operational/rendered-service-path:rendered-service-paths/rendered-service-path/" + obj.getName());
            }
        }

    }

}

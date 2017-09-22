/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import java.util.concurrent.ExecutorService;
import org.opendaylight.sfc.sbrest.json.SffExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestSffTask extends SbRestAbstractTask {
    private static final String SFF_REST_URI =
            "/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/";
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSffTask.class);

    public SbRestSffTask(RestOperation restOperation, ServiceFunctionForwarder dataObject,
            ExecutorService odlExecutor) {
        super(restOperation, new SffExporterFactory(), dataObject, odlExecutor);
        setRestUriList(dataObject);
    }

    private void setRestUriList(ServiceFunctionForwarder obj) {
        if (obj.getRestUri() != null) {
            SffName sffName = obj.getName();
            if (sffName != null) {
                String restUri = obj.getRestUri().getValue() + SFF_REST_URI + sffName.getValue();
                addRestUri(restUri);
                LOG.info("SFF will be send to REST URI {}", restUri);
            }
        }
    }
}

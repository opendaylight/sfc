/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.task;

import org.opendaylight.sfc.sbrest.json.SffExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;

public class SbRestSffTask extends SbRestAbstractTask {

    public SbRestSffTask(RestOperation restOperation, ServiceFunctionForwarder dataObject, String urlMgmt) {

        super(restOperation, urlMgmt);

        this.exporterFactory = new SffExporterFactory();
        this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
    }

}

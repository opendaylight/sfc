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

import org.opendaylight.sfc.sbrest.json.AclExporterFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class SbRestAclTask extends SbRestAbstractTask {

    public SbRestAclTask(RestOperation restOperation, AccessList dataObject, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        this.exporterFactory = new AclExporterFactory();
        if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        }
        setRestUriList(dataObject);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        AccessList obj = (AccessList) dataObject;

        //rest uri list should be created from Classifier SFFs. Classifier will be taken from ACL operation data store <ACL, Classifier>
        //this prevents from looping through all classifiers and looking from ACL.

        this.restUriList = new ArrayList<>();
        this.restUriList.add("http://localhost:5000/config/ietf-acl:access-lists/access-list/" + obj.getAclName());
//        this.restUriList.add(obj.getRestUri().getValue()
//                + "/config/service-function:service-functions/service-function/" + obj.getName());
    }
}

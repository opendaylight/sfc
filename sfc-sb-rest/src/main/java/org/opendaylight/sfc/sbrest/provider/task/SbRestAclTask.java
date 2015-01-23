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

import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sbrest.json.AclExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestAclTask extends SbRestAbstractTask {

    private static final String ACL_REST_URI = "/config/ietf-acl:access-lists/access-list/";
    private static final Logger LOG = LoggerFactory.getLogger(SbRestAclTask.class);


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

    public SbRestAclTask(RestOperation restOperation, AccessList dataObject,
                         List<SclServiceFunctionForwarder> sclServiceForwarderList, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        this.exporterFactory = new AclExporterFactory();
        if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = exporterFactory.getExporter().exportJson(dataObject);
        }
        setRestUriList(dataObject, sclServiceForwarderList);
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        AccessList obj = (AccessList) dataObject;
        this.restUriList = new ArrayList<>();

        //rest uri list should be created from Classifier SFFs. Classifier will be taken from ACL operational data store <ACL, Classifier>
        //this prevents from looping through all classifiers and looking from ACL.
        AccessListState accessListState = SfcProviderAclAPI.readAccessListStateExecutor(obj.getAclName());
        if (accessListState != null) {
            List<AclServiceFunctionClassifier> serviceClassifierList = accessListState.getAclServiceFunctionClassifier();

            //loop through all classifiers listed in ACL State and get REST URIs from the Classifier's SFFs
            if (serviceClassifierList != null) {
                for (AclServiceFunctionClassifier aclServiceClassifier : serviceClassifierList) {
                    ServiceFunctionClassifier serviceClassifier =
                            SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(aclServiceClassifier.getName());

                    List<SclServiceFunctionForwarder> sclServiceForwarderList = serviceClassifier.getSclServiceFunctionForwarder();

                    if (sclServiceForwarderList != null) {
                        for (SclServiceFunctionForwarder sclServiceForwarder : sclServiceForwarderList) {
                            ServiceFunctionForwarder serviceForwarder =
                                    SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sclServiceForwarder.getName());

                            if (serviceForwarder.getRestUri() != null && !serviceForwarder.getRestUri().getValue().isEmpty()) {
                                String restUri = serviceForwarder.getRestUri().getValue() + ACL_REST_URI + obj.getAclName();
                                this.restUriList.add(restUri);
                                LOG.info("ACL will be send to REST URI {}", restUri);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void setRestUriList(DataObject dataObject, List<SclServiceFunctionForwarder> sclServiceForwarderList) {
        AccessList obj = (AccessList) dataObject;
        this.restUriList = new ArrayList<>();

        if (sclServiceForwarderList != null) {
            for (SclServiceFunctionForwarder sclServiceForwarder : sclServiceForwarderList) {
                ServiceFunctionForwarder serviceForwarder =
                        SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sclServiceForwarder.getName());

                if (serviceForwarder.getRestUri() != null && !serviceForwarder.getRestUri().getValue().isEmpty()) {
                    String restUri = serviceForwarder.getRestUri().getValue() + ACL_REST_URI + obj.getAclName();
                    this.restUriList.add(restUri);
                    LOG.info("ACL will be send to REST URI {}", restUri);
                }
            }
        }
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbRestAclTask extends SbRestAbstractTask {

    private static final String ACL_REST_URI = "/config/ietf-access-control-list:access-lists/acl/";
    private static final Logger LOG = LoggerFactory.getLogger(SbRestAclTask.class);

    public SbRestAclTask(RestOperation restOperation, Acl dataObject, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        setJsonObject(restOperation, dataObject);
        setRestUriList(dataObject);
    }

    public SbRestAclTask(RestOperation restOperation, Acl dataObject,
            List<SclServiceFunctionForwarder> sclServiceForwarderList, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        setJsonObject(restOperation, dataObject);
        setRestUriList(dataObject, sclServiceForwarderList);
    }

    public SbRestAclTask(RestOperation restOperation, String accessListName,
            List<SclServiceFunctionForwarder> sclServiceForwarderList, ExecutorService odlExecutor) {
        super(restOperation, odlExecutor);
        setRestUriList(accessListName, sclServiceForwarderList);
    }

    private void setJsonObject(RestOperation restOperation, Acl dataObject) {
        this.exporterFactory = new AclExporterFactory();
        if (restOperation.equals(RestOperation.DELETE)) {
            this.jsonObject = this.exporterFactory.getExporter().exportJsonNameOnly(dataObject);
        } else {
            this.jsonObject = this.exporterFactory.getExporter().exportJson(dataObject);
        }
    }

    @Override
    protected void setRestUriList(DataObject dataObject) {
        Acl accessList = (Acl) dataObject;
        String accessListName = null;

        if (accessList != null) {
            accessListName = accessList.getAclName();
        }

        // rest uri list should be created from Classifier SFFs. Classifier will be taken from ACL
        // operational data store <ACL, Classifier>
        // this prevents from looping through all classifiers and looking from ACL.
        AccessListState accessListState = SfcProviderAclAPI.readAccessListState(accessListName);
        if (accessListState != null) {
            List<AclServiceFunctionClassifier> serviceClassifierList =
                    accessListState.getAclServiceFunctionClassifier();

            // loop through all classifiers listed in ACL State and get REST URIs from the
            // Classifier's SFFs
            if (serviceClassifierList != null) {
                for (AclServiceFunctionClassifier aclServiceClassifier : serviceClassifierList) {
                    ServiceFunctionClassifier serviceClassifier =
                            SfcProviderServiceClassifierAPI.readServiceClassifier(aclServiceClassifier.getName());

                    if (serviceClassifier != null) {
                        List<SclServiceFunctionForwarder> sclServiceForwarderList =
                                serviceClassifier.getSclServiceFunctionForwarder();
                        this.restUriList =
                                this.getRestUriListFromSclServiceForwarderList(sclServiceForwarderList, accessListName);
                    }
                }
            }
        }
    }

    protected void setRestUriList(DataObject dataObject, List<SclServiceFunctionForwarder> sclServiceForwarderList) {
        Acl accessList = (Acl) dataObject;

        if (accessList != null) {
            this.restUriList =
                    this.getRestUriListFromSclServiceForwarderList(sclServiceForwarderList, accessList.getAclName());
        }
    }

    protected void setRestUriList(String accessListName, List<SclServiceFunctionForwarder> sclServiceForwarderList) {
        this.restUriList = this.getRestUriListFromSclServiceForwarderList(sclServiceForwarderList, accessListName);
    }

    private ArrayList<String> getRestUriListFromSclServiceForwarderList(
            List<SclServiceFunctionForwarder> sclServiceForwarderList, String accessListName) {
        ArrayList<String> sffRestUriList = new ArrayList<>();

        if (sclServiceForwarderList != null && accessListName != null && !accessListName.isEmpty()) {
            for (SclServiceFunctionForwarder sclServiceForwarder : sclServiceForwarderList) {
                SffName sffName = new SffName(sclServiceForwarder.getName());
                ServiceFunctionForwarder serviceForwarder =
                        SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

                if (serviceForwarder != null && serviceForwarder.getRestUri() != null
                        && !serviceForwarder.getRestUri().getValue().isEmpty()) {
                    String restUri = serviceForwarder.getRestUri().getValue() + ACL_REST_URI + accessListName;
                    sffRestUriList.add(restUri);
                    LOG.info("ACL will be send to REST URI {}", restUri);
                }
            }
        }

        return sffRestUriList.isEmpty() ? null : sffRestUriList;
    }
}

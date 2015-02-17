/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.provider.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sbrest.json.AclExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

/**
 * This class contains unit tests for SbRestAclTask
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-17
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcProviderAclAPI.class, SfcProviderServiceClassifierAPI.class, SfcProviderServiceForwarderAPI.class})
public class SbRestAclTaskTest {

    private static final String ACL_NAME = "Dummy_ACL";
    private static final String CLASSIFIER_NAME = "Dummy_Classifier";
    private static final String SFF_NAME = "Dummy_SFF";
    private static final String REST_URI = "http://localhost:5000";

    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testSbRestAclTask() throws IOException {
        PowerMockito.mockStatic(SfcProviderAclAPI.class);
        Mockito.when(SfcProviderAclAPI.readAccessListStateExecutor(ACL_NAME))
                .thenReturn(this.buildAccessListState());

        PowerMockito.mockStatic(SfcProviderServiceClassifierAPI.class);
        Mockito.when(SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(CLASSIFIER_NAME))
                .thenReturn(this.buildServiceFunctionClassifier());

        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        Mockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(SFF_NAME))
                .thenReturn(this.buildServiceFunctionForwarder());

        SbRestAclTask sbRestAclTask = new SbRestAclTask(RestOperation.PUT, this.buildAccessList(), executorService);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonObject = mapper.readTree(sbRestAclTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildAccessListObjectNode()));
    }

    private AccessList buildAccessList() {
        AccessListBuilder accessListBuilder = new AccessListBuilder();
        accessListBuilder.setAclName(ACL_NAME);

        return accessListBuilder.build();
    }

    private AccessListState buildAccessListState() {
        AccessListStateBuilder accessListStateBuilder = new AccessListStateBuilder();
        accessListStateBuilder.setAclName(ACL_NAME);

        AclServiceFunctionClassifierBuilder aclServiceFunctionClassifierBuilder = new AclServiceFunctionClassifierBuilder();
        aclServiceFunctionClassifierBuilder.setName(CLASSIFIER_NAME);

        List<AclServiceFunctionClassifier> aclServiceFunctionClassifierList = new ArrayList<>();
        aclServiceFunctionClassifierList.add(aclServiceFunctionClassifierBuilder.build());

        accessListStateBuilder.setAclServiceFunctionClassifier(aclServiceFunctionClassifierList);
        return accessListStateBuilder.build();
    }

    private ServiceFunctionClassifier buildServiceFunctionClassifier() {
        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(CLASSIFIER_NAME);

        SclServiceFunctionForwarderBuilder sclServiceFunctionForwarderBuilder = new SclServiceFunctionForwarderBuilder();
        sclServiceFunctionForwarderBuilder.setName(SFF_NAME);

        List<SclServiceFunctionForwarder> sclServiceFunctionForwarderList = new ArrayList<>();
        sclServiceFunctionForwarderList.add(sclServiceFunctionForwarderBuilder.build());

        serviceFunctionClassifierBuilder.setSclServiceFunctionForwarder(sclServiceFunctionForwarderList);
        return serviceFunctionClassifierBuilder.build();
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(SFF_NAME);
        serviceFunctionForwarderBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionForwarderBuilder.build();
    }

    private ObjectNode buildAccessListObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode accessListNode = mapper.createObjectNode();
        accessListNode.put(AclExporterFactory._ACL_NAME, ACL_NAME);

        ObjectNode accessListArrayNode = mapper.createObjectNode();
        accessListNode.put(AclExporterFactory._ACCESS_LIST, accessListNode);

        topNode.put(AclExporterFactory._ACCESS_LIST, accessListArrayNode);
        return topNode;
    }
}

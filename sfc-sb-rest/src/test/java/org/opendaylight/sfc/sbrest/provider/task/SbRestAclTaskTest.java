/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.provider.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sbrest.json.AclExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.AccessListStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.access.list.state.AclServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SbRestAclTask
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-02-17
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcProviderAclAPI.class, SfcProviderServiceClassifierAPI.class, SfcProviderServiceForwarderAPI.class})
public class SbRestAclTaskTest {

    private static final String ACL_NAME = "Dummy_ACL";
    private static final java.lang.Class<? extends AclBase> ACL_TYPE = Ipv4Acl.class;
    private static final String CLASSIFIER_NAME = "Dummy_Classifier";
    private static final SffName SFF_NAME = new SffName("Dummy_SFF");
    private static final String REST_URI = "http://localhost:5000";

    private ExecutorService executorService;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    // some mocked methods are prepared here
    public void init() {
        executorService = Executors.newFixedThreadPool(10);

        PowerMockito.stub(PowerMockito.method(SfcProviderAclAPI.class, "readAccessListState", String.class, Class.class))
            .toReturn(this.buildAccessListState());

        PowerMockito
            .stub(PowerMockito.method(SfcProviderServiceClassifierAPI.class, "readServiceClassifier", String.class))
            .toReturn(this.buildServiceFunctionClassifier());

        PowerMockito.stub(PowerMockito.method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder",
                SffName.class))
            .toReturn(this.buildServiceFunctionForwarder());
    }

    @Test
    public void testSbRestAclTask() throws IOException {
        SbRestAclTask sbRestAclTask = new SbRestAclTask(RestOperation.PUT, this.buildAccessList(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestAclTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildAccessListObjectNode()));
        assertTrue(sbRestAclTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestAclTask1() throws IOException {
        SbRestAclTask sbRestAclTask = new SbRestAclTask(RestOperation.DELETE, this.buildAccessList(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestAclTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildAccessListObjectNode()));
        assertTrue(sbRestAclTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestAclTaskEmpty() throws IOException {
        PowerMockito.mockStatic(SfcProviderServiceForwarderAPI.class);
        Mockito.when(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(SFF_NAME))
            .thenReturn(new ServiceFunctionForwarderBuilder().build());

        SbRestAclTask sbRestAclTask = new SbRestAclTask(RestOperation.PUT, this.buildAccessList(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestAclTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildAccessListObjectNode()));
        assertNull(sbRestAclTask.restUriList);
    }

    @Test
    public void testSbRestAclTaskAclNameForwarderList() throws IOException {
        SbRestAclTask sbRestAclTask = new SbRestAclTask(RestOperation.PUT, ACL_NAME, ACL_TYPE,
                this.buildServiceFunctionClassifier().getSclServiceFunctionForwarder(), executorService);

        assertNull(sbRestAclTask.jsonObject);
        assertTrue(sbRestAclTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestAclTaskAclObjectForwarderList() throws IOException {
        SbRestAclTask sbRestAclTask = new SbRestAclTask(RestOperation.PUT, this.buildAccessList(),
                this.buildServiceFunctionClassifier().getSclServiceFunctionForwarder(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestAclTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildAccessListObjectNode()));
        assertTrue(sbRestAclTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestAclTaskForwarderListEmpty() throws IOException {
        SclServiceFunctionForwarderBuilder sclServiceFunctionForwarderBuilder =
                new SclServiceFunctionForwarderBuilder();
        sclServiceFunctionForwarderBuilder.setName(SFF_NAME.getValue());

        List<SclServiceFunctionForwarder> sclServiceFunctionForwarderList = new ArrayList<>();
        sclServiceFunctionForwarderList.add(sclServiceFunctionForwarderBuilder.build());

        SbRestAclTask sbRestAclTask = new SbRestAclTask(RestOperation.PUT, this.buildAccessList(),
                sclServiceFunctionForwarderList, executorService);

        JsonNode jsonObject = mapper.readTree(sbRestAclTask.jsonObject);
        assertTrue(jsonObject.equals(this.buildAccessListObjectNode()));
        assertTrue(sbRestAclTask.restUriList.get(0).contains(REST_URI));
    }

    // build access list
    private Acl buildAccessList() {
        AclBuilder aclBuilder = new AclBuilder();
        aclBuilder.setAclName(ACL_NAME);
        aclBuilder.setAclType(ACL_TYPE);

        return aclBuilder.build();
    }

    // this method mocks readAccessListStateExecutor method
    private AccessListState buildAccessListState() {
        AccessListStateBuilder accessListStateBuilder = new AccessListStateBuilder();
        accessListStateBuilder.setAclName(ACL_NAME);
        accessListStateBuilder.setAclType(ACL_TYPE);

        AclServiceFunctionClassifierBuilder aclServiceFunctionClassifierBuilder =
                new AclServiceFunctionClassifierBuilder();
        aclServiceFunctionClassifierBuilder.setName(CLASSIFIER_NAME);

        List<AclServiceFunctionClassifier> aclServiceFunctionClassifierList = new ArrayList<>();
        aclServiceFunctionClassifierList.add(aclServiceFunctionClassifierBuilder.build());

        accessListStateBuilder.setAclServiceFunctionClassifier(aclServiceFunctionClassifierList);
        return accessListStateBuilder.build();
    }

    // this method mocks readServiceClassifierExecutor method
    private ServiceFunctionClassifier buildServiceFunctionClassifier() {
        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(CLASSIFIER_NAME);

        SclServiceFunctionForwarderBuilder sclServiceFunctionForwarderBuilder =
                new SclServiceFunctionForwarderBuilder();
        sclServiceFunctionForwarderBuilder.setName(SFF_NAME.getValue());

        List<SclServiceFunctionForwarder> sclServiceFunctionForwarderList = new ArrayList<>();
        sclServiceFunctionForwarderList.add(sclServiceFunctionForwarderBuilder.build());

        serviceFunctionClassifierBuilder.setSclServiceFunctionForwarder(sclServiceFunctionForwarderList);
        return serviceFunctionClassifierBuilder.build();
    }

    // this method mocks readServiceFunctionForwarderExecutor method
    private ServiceFunctionForwarder buildServiceFunctionForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(SFF_NAME);
        serviceFunctionForwarderBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionForwarderBuilder.build();
    }

    private ObjectNode buildAccessListObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode accessListNode = mapper.createObjectNode();
        accessListNode.put(AclExporterFactory._ACL_NAME, ACL_NAME);

        ArrayNode accessListArrayNode = mapper.createArrayNode();
        accessListArrayNode.add(accessListNode);

        topNode.put(AclExporterFactory._ACL, accessListArrayNode);
        return topNode;
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;


import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Gre;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This class contains unit tests for class SfcProviderServicePathAPI
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-07-29
 */

public class SfcProviderServicePathAPITest extends AbstractDataStoreManager {

    @Before
    public void before() {
        setOdlSfc();
    }

    // test if service path has any constraints
    @Test
    public void testIsDefaultServicePath() {

        //add path hop list
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setServicePathHop(new ArrayList<ServicePathHop>());
        boolean result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        //set transport type
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setTransportType(VxlanGpe.class);
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        //set starting index
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setStartingIndex((short) 255);
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        //set path id
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setPathId(1L);
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        //nothing is set
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertTrue("Must be true", result);
    }

    //add and read service function path state
    @Test
    public void testReadAndDeleteServicePathState() {
        String sfpKey = "sfpKey";
        String rspKey = "rspKey";

        //create rendered service path list with one entry
        List<SfpRenderedServicePath> renderedServicePaths = new ArrayList<>();
        SfpRenderedServicePathBuilder sfpRenderedServicePathBuilder = new SfpRenderedServicePathBuilder();
        sfpRenderedServicePathBuilder.setKey(new SfpRenderedServicePathKey(rspKey));
        renderedServicePaths.add(sfpRenderedServicePathBuilder.build());

        //create service path state
        ServiceFunctionPathStateBuilder serviceFunctionPathStateBuilder = new ServiceFunctionPathStateBuilder();
        serviceFunctionPathStateBuilder.setKey(new ServiceFunctionPathStateKey(sfpKey))
                .setSfpRenderedServicePath(renderedServicePaths);

        InstanceIdentifier<ServiceFunctionPathState> sfpIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(sfpKey)).build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfpIID,
                serviceFunctionPathStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        //read service path state
        List<SfpRenderedServicePath> sfpList = SfcProviderServicePathAPI.readServicePathState(sfpKey);

        assertNotNull("Must not be null", sfpList);
        assertEquals("Must be equal", sfpList.get(0).getName(), rspKey);

        //delete service path state
        transactionSuccessful = SfcProviderServicePathAPI.deleteServicePathState(sfpKey);
        assertTrue("Must be true", transactionSuccessful);

        //try to read again, must be null
        sfpList = SfcProviderServicePathAPI.readServicePathState(sfpKey);

        assertNull("Must be null", sfpList);
    }

    //create service function path state and put rendered service path into it
    @Test
    public void testAddRenderedPathToServicePathState() {
        String sfpKey = "sfpKey";
        String rspKey = "rspKey";

        //create service path state
        ServiceFunctionPathStateBuilder serviceFunctionPathStateBuilder = new ServiceFunctionPathStateBuilder();
        serviceFunctionPathStateBuilder.setName(sfpKey)
                .setKey(new ServiceFunctionPathStateKey(sfpKey));

        boolean transactionSuccessful = SfcProviderServicePathAPI.addRenderedPathToServicePathState(serviceFunctionPathStateBuilder.build().getName(), rspKey);
        assertTrue("Must be true", transactionSuccessful);

        //check if path is already added
        List<SfpRenderedServicePath> sfpList = SfcProviderServicePathAPI.readServicePathState(sfpKey);
        assertNotNull("Must be not null", sfpList);
        assertEquals("Must be equal", sfpList.get(0).getName(), rspKey);
    }

    //put service function path into data store, read it and remove
    @Test
    public void testPutReadDeleteServiceFunctionPath() {
        String testClassifier = "classifier";
        String sfpName = "sfpName";

        //put service function path
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(sfpName)
                .setKey(new ServiceFunctionPathKey(sfpName))
                .setClassifier(testClassifier);

        boolean transactionSuccessful = SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPathBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        //read service function path
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(sfpName);
        assertNotNull("Must not be null", serviceFunctionPath);
        assertEquals("Must be equal", serviceFunctionPath.getClassifier(), testClassifier);

        //delete service function path
        transactionSuccessful = SfcProviderServicePathAPI.deleteServiceFunctionPath(sfpName);
        assertTrue("Must be true", transactionSuccessful);
    }

    //put service function paths, read all, delete all
    @Test
    public void testReadAllServiceFunctionPaths() throws Exception {
        String sfpName = "SFP";

        //create service function paths
        List<ServiceFunctionPath> serviceFunctionPaths = new ArrayList<>();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(sfpName + 1)
                .setKey(new ServiceFunctionPathKey(sfpName + 1))
                .setTransportType(VxlanGpe.class);
        serviceFunctionPaths.add(serviceFunctionPathBuilder.build());

        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(sfpName + 2)
                .setKey(new ServiceFunctionPathKey(sfpName + 2))
                .setTransportType(Gre.class);
        serviceFunctionPaths.add(serviceFunctionPathBuilder.build());

        ServiceFunctionPathsBuilder serviceFunctionPathsBuilder = new ServiceFunctionPathsBuilder();
        serviceFunctionPathsBuilder.setServiceFunctionPath(serviceFunctionPaths);
        ServiceFunctionPaths writtenPaths = serviceFunctionPathsBuilder.build();

        //put all paths
        boolean transactionSuccessful = SfcProviderServicePathAPI.putAllServiceFunctionPaths(writtenPaths);
        assertTrue("Must be true", transactionSuccessful);

        //read all paths
        ServiceFunctionPaths getPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        assertNotNull("Must not be null", getPaths);
        assertTrue("Must be true", getPaths.equals(writtenPaths));

        transactionSuccessful = SfcProviderServicePathAPI.deleteAllServiceFunctionPaths();
        assertTrue("Must be true", transactionSuccessful);
    }


    @Test
    public void testDeleteServicePathContainingFunction() {

        //create service function
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName("SF1")
                .setKey(new ServiceFunctionKey("SF1"));
        ServiceFunction serviceFunction = serviceFunctionBuilder.build();

        //write rendered service path
        long pathId = SfcServicePathId.check_and_allocate_pathid();

        assertNotEquals("Must be not equal", pathId, -1);

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setName("SP1")
                .setKey(new RenderedServicePathKey("SP1"))
                .setPathId(pathId);

        InstanceIdentifier<RenderedServicePath> rspIID =
                InstanceIdentifier.builder(RenderedServicePaths.class)
                        .child(RenderedServicePath.class, new RenderedServicePathKey("SP1"))
                        .build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        //create service function state
        List<SfServicePath> sfServicePathList = new ArrayList<>();
        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setName("SP1")
                .setKey(new SfServicePathKey("SP1"));
        sfServicePathList.add(sfServicePathBuilder.build());

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName("SF1")
                .setKey(new ServiceFunctionStateKey("SF1"))
                .setSfServicePath(sfServicePathList);
        ServiceFunctionState serviceFunctionState = serviceFunctionStateBuilder.build();

        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey("SF1");
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfStateIID,
                serviceFunctionState, LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        //delete path
        transactionSuccessful = SfcProviderServicePathAPI.deleteServicePathContainingFunction(serviceFunction);
        assertTrue("Must be true", transactionSuccessful);
    }
}

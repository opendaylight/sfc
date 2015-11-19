/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
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

        // add path hop list
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setServicePathHop(new ArrayList<ServicePathHop>());
        boolean result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        // set transport type
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setTransportType(VxlanGpe.class);
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        // set starting index
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setStartingIndex((short) 255);
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        // set path id
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setPathId(1L);
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertFalse("Must be false", result);

        // nothing is set
        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        result = SfcProviderServicePathAPI.isDefaultServicePath(serviceFunctionPathBuilder.build());
        assertTrue("Must be true", result);
    }

    // add and read service function path state
    @Test
    public void testReadAndDeleteServicePathState() {
        SfpName sfpKey = new SfpName("sfpKey");
        RspName rspKey = new RspName("rspKey");

        // create rendered service path list with one entry
        List<SfpRenderedServicePath> renderedServicePaths = new ArrayList<>();
        SfpRenderedServicePathBuilder sfpRenderedServicePathBuilder = new SfpRenderedServicePathBuilder();
        sfpRenderedServicePathBuilder.setKey(new SfpRenderedServicePathKey(rspKey));
        renderedServicePaths.add(sfpRenderedServicePathBuilder.build());

        // create service path state
        ServiceFunctionPathStateBuilder serviceFunctionPathStateBuilder = new ServiceFunctionPathStateBuilder();
        serviceFunctionPathStateBuilder.setKey(new ServiceFunctionPathStateKey(sfpKey))
            .setSfpRenderedServicePath(renderedServicePaths);

        InstanceIdentifier<ServiceFunctionPathState> sfpIID =
                InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                    .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(sfpKey))
                    .build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfpIID,
                serviceFunctionPathStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        // read service path state
        List<SfpRenderedServicePath> sfpList = SfcProviderServicePathAPI.readServicePathState(sfpKey);

        assertNotNull("Must not be null", sfpList);
        assertEquals("Must be equal", sfpList.get(0).getName(), rspKey);

        // delete service path state
        transactionSuccessful = SfcProviderServicePathAPI.deleteServicePathState(sfpKey);
        assertTrue("Must be true", transactionSuccessful);

        // try to read again, must be null
        sfpList = SfcProviderServicePathAPI.readServicePathState(sfpKey);

        assertNull("Must be null", sfpList);
    }

    // create service function path state and put rendered service path into it
    @Test
    public void testAddRenderedPathToServicePathState() {
        SfpName sfpKey = new SfpName("sfpKey");
        RspName rspKey = new RspName("rspKey");

        // create service path state
        ServiceFunctionPathStateBuilder serviceFunctionPathStateBuilder = new ServiceFunctionPathStateBuilder();
        serviceFunctionPathStateBuilder.setName(sfpKey).setKey(new ServiceFunctionPathStateKey(sfpKey));

        boolean transactionSuccessful = SfcProviderServicePathAPI
            .addRenderedPathToServicePathState(serviceFunctionPathStateBuilder.build().getName(), rspKey);
        assertTrue("Must be true", transactionSuccessful);

        // check if path is already added
        List<SfpRenderedServicePath> sfpList = SfcProviderServicePathAPI.readServicePathState(sfpKey);
        assertNotNull("Must be not null", sfpList);
        assertEquals("Must be equal", sfpList.get(0).getName(), rspKey);
    }

    // put service function path into data store, read it and remove
    @Test
    public void testPutReadDeleteServiceFunctionPath() {
        String testClassifier = "classifier";
        SfpName sfpName = new SfpName("sfpKey");

        // put service function path
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(sfpName)
            .setKey(new ServiceFunctionPathKey(sfpName))
            .setClassifier(testClassifier);

        boolean transactionSuccessful =
                SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPathBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        // read service function path
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(sfpName);
        assertNotNull("Must not be null", serviceFunctionPath);
        assertEquals("Must be equal", serviceFunctionPath.getClassifier(), testClassifier);

        // delete service function path
        transactionSuccessful = SfcProviderServicePathAPI.deleteServiceFunctionPath(sfpName);
        assertTrue("Must be true", transactionSuccessful);
    }

    // put service function paths, read all, delete all
    @Test
    public void testReadAllServiceFunctionPaths() throws Exception {
        SfpName sfpName1 = new SfpName("SFP1");
        SfpName sfpName2 = new SfpName("SFP2");

        // create service function paths
        List<ServiceFunctionPath> serviceFunctionPaths = new ArrayList<>();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(sfpName1)
            .setKey(new ServiceFunctionPathKey(sfpName1))
            .setSymmetric(false)
            .setTransportType(VxlanGpe.class);
        serviceFunctionPaths.add(serviceFunctionPathBuilder.build());

        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(sfpName2)
            .setKey(new ServiceFunctionPathKey(sfpName2))
            .setSymmetric(false)
            .setTransportType(Gre.class);
        serviceFunctionPaths.add(serviceFunctionPathBuilder.build());

        ServiceFunctionPathsBuilder serviceFunctionPathsBuilder = new ServiceFunctionPathsBuilder();
        serviceFunctionPathsBuilder.setServiceFunctionPath(serviceFunctionPaths);
        ServiceFunctionPaths writtenPaths = serviceFunctionPathsBuilder.build();

        // put all paths
        boolean transactionSuccessful = SfcProviderServicePathAPI.putAllServiceFunctionPaths(writtenPaths);
        assertTrue("Must be true", transactionSuccessful);

        // read all paths
        ServiceFunctionPaths getPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        assertNotNull("Must not be null", getPaths);
        assertEquals(writtenPaths, getPaths);

        transactionSuccessful = SfcProviderServicePathAPI.deleteAllServiceFunctionPaths();
        assertTrue("Must be true", transactionSuccessful);
    }

    @Test
    public void testDeleteServicePathContainingFunction() {

        SfName sfName = new SfName("SF1");
        RspName rspName = new RspName("SP1");
        SfpName sfpName = new SfpName(rspName.getValue());

        // create service function
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(sfName).setKey(new ServiceFunctionKey(sfName));
        ServiceFunction serviceFunction = serviceFunctionBuilder.build();

        // write rendered service path
        long pathId = SfcServicePathId.check_and_allocate_pathid();

        assertNotEquals("Must be not equal", pathId, -1);

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setName(rspName).setKey(new RenderedServicePathKey(rspName)).setPathId(pathId);

        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
            .child(RenderedServicePath.class, new RenderedServicePathKey(rspName))
            .build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(rspIID,
                renderedServicePathBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        // create service function state
        List<SfServicePath> sfServicePathList = new ArrayList<>();
        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setName(sfpName).setKey(new SfServicePathKey(sfpName));
        sfServicePathList.add(sfServicePathBuilder.build());

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName(sfName)
            .setKey(new ServiceFunctionStateKey(sfName))
            .setSfServicePath(sfServicePathList);
        ServiceFunctionState serviceFunctionState = serviceFunctionStateBuilder.build();

        ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
            .child(ServiceFunctionState.class, serviceFunctionStateKey)
            .build();

        transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionState,
                LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        // delete path
        transactionSuccessful = SfcProviderServicePathAPI.deleteServicePathContainingFunction(serviceFunction);
        assertTrue("Must be true", transactionSuccessful);
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Gre;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(sfpIID, LogicalDatastoreType.OPERATIONAL);
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

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).build();

        // put all paths
        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfpsIID, writtenPaths, LogicalDatastoreType.CONFIGURATION);
        assertTrue("Must be true", transactionSuccessful);

        // read all paths
        ServiceFunctionPaths getPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        assertNotNull("Must not be null", getPaths);
        assertTrue(writtenPaths.getServiceFunctionPath().containsAll(getPaths.getServiceFunctionPath()));

        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION);
        assertTrue("Must be true", transactionSuccessful);
    }
}

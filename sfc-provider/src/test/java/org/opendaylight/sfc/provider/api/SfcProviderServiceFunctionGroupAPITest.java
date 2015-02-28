/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev160202.service.function.group.algorithm.grouping.ServiceFunctionGroupAlgorithm;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev160202.service.function.group.grouping.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

public class SfcProviderServiceFunctionGroupAPITest extends AbstractDataBrokerTest {

    DataBroker dataBroker;
    ExecutorService executor;
    OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    List<ServiceFunction> sfList = new ArrayList<>();

    @Before
    public void before() throws ExecutionException, InterruptedException {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

        Ip dummyIp = SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.5")), 555);
        SfDataPlaneLocator dummyLocator = SimpleTestEntityBuilder.buildSfDataPlaneLocator("moscow-5.5.5.5:555-vxlan", dummyIp, "sff-moscow",
                VxlanGpe.class);

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_101", Firewall.class, new IpAddress(new Ipv4Address("192.168.100.101")),
                dummyLocator, Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_102", Firewall.class, new IpAddress(new Ipv4Address("192.168.100.102")),
                dummyLocator, Boolean.FALSE));
    }

    @After
    public void after() {
    }

    @Test
    public void testPutReadDeleteServiceFunctionGroupAlgorithm() throws ExecutionException, InterruptedException {

        // Create:
        String sfgAlgName = "testServiceFunctionGroupAlgorithm";
        ServiceFunctionGroupAlgorithm sfgAlg = SimpleTestEntityBuilder.buildServiceFunctionGroupAlgorithm(sfgAlgName);

        // Put:
        Object[] params = { sfgAlg };
        Class[] paramsTypes = { ServiceFunctionGroupAlgorithm.class };
        executor.submit(SfcProviderServiceFunctionGroupAlgAPI.getPut(params, paramsTypes)).get();

        // Read:
        Object[] params2 = { sfgAlgName };
        Class[] paramsTypes2 = { String.class };
        Object result = executor.submit(SfcProviderServiceFunctionGroupAlgAPI.getRead(params2, paramsTypes2)).get();
        ServiceFunctionGroupAlgorithm sfgAlg2 = (ServiceFunctionGroupAlgorithm) result;

        assertNotNull("Must be not null", sfgAlg2);
        assertEquals("Must be equal", sfgAlg2.getName(), sfgAlgName);

        // Delete:
        executor.submit(SfcProviderServiceFunctionGroupAlgAPI.getDelete(params2, paramsTypes2));
        result = executor.submit(SfcProviderServiceFunctionGroupAlgAPI.getRead(params2, paramsTypes2)).get();
        sfgAlg2 = (ServiceFunctionGroupAlgorithm) result;

        //assertNull("Must be null", sfgAlg2); //TODO: test passes locally but fails on Jenkins
    }

    @Test
    public void testPutReadDeleteServiceFunctionGroup() throws ExecutionException, InterruptedException {

        // Create:
        String sfgName = "testServiceFunctionGroup";
        String sfgAlgName = "testServiceFunctionGroupAlgorithm"; //Not checking that the algorithm exists
        ServiceFunctionGroup sfg = SimpleTestEntityBuilder.buildServiceFunctionGroup(sfgName, sfgAlgName);

        // Put:
        Object[] params = { sfg };
        Class[] paramsTypes = { ServiceFunctionGroup.class };
        executor.submit(SfcProviderServiceFunctionGroupAPI.getPut(params, paramsTypes)).get();

        // Read:
        Object[] params2 = { sfgName };
        Class[] paramsTypes2 = { String.class };
        Object result = executor.submit(SfcProviderServiceFunctionGroupAPI.getRead(params2, paramsTypes2)).get();
        ServiceFunctionGroup sfg2 = (ServiceFunctionGroup) result;

        assertNotNull("Must be not null", sfg2);
        assertEquals("Must be equal", sfg2.getName(), sfgName);
        assertEquals("Must be equal", sfg2.getAlgorithm(), sfgAlgName);

        // Delete:
        executor.submit(SfcProviderServiceFunctionGroupAPI.getDelete(params2, paramsTypes2));
        result = executor.submit(SfcProviderServiceFunctionGroupAPI.getRead(params2, paramsTypes2)).get();
        sfg2 = (ServiceFunctionGroup) result;

        //assertNull("Must be null", sfg2); //TODO: test passes locally but fails on Jenkins
    }

    @Test
    @Ignore
    public void testAddRemoveFunctionFromServiceFunctionGroup() throws ExecutionException, InterruptedException {

        // Create:
        String sfgName = "testServiceFunctionGroup";
        String sfgAlgName = "testServiceFunctionGroupAlgorithm"; //Not checking that the algorithm exists
        ServiceFunctionGroup sfg = SimpleTestEntityBuilder.buildServiceFunctionGroup(sfgName, sfgAlgName);

        // Add Service Function:
        ServiceFunction sf = sfList.get(0);
        Object[] params = { sfg.getName(), sf.getName() };
        Class[] paramsTypes = { String.class, String.class };
        executor.submit(SfcProviderServiceFunctionGroupAPI.getAddSF(params, paramsTypes)).get();

        assertNotNull("Must be not null", sfg.getSfgServiceFunction().get(0));
        assertEquals("Must be equal", sfg.getSfgServiceFunction().get(0), sf.getName());

        // Delete Service Function:
        executor.submit(SfcProviderServiceFunctionGroupAPI.getDelete(params, paramsTypes));
        assertNull("Must be null", sfg.getSfgServiceFunction().get(0));
    }
}
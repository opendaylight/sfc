/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

public class SfcProviderServicePathAPITest extends AbstractDataBrokerTest {

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
        SfDataPlaneLocator dummyLocator = SimpleTestEntityBuilder.buildSfDataPlaneLocator("moscow-5.5.5.5:555-vxlan", dummyIp, "sff-moscow", VxlanGpe.class);

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_101", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.101")), dummyLocator, Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_102", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.102")), dummyLocator, Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction("simple_fw_103", Firewall.class,
                new IpAddress(new Ipv4Address("192.168.100.103")), dummyLocator, Boolean.FALSE));
    }

    @After
    public void after() {
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
    }

    @Test
    public void testCreatePathWithNamesOnly() throws ExecutionException, InterruptedException {

        String sfcName = "unittest-sfp-chain-1";
        ServiceFunctionChainKey sfcKey = new ServiceFunctionChainKey(sfcName);

        String pathName = "unittest-sfp-path-1";
        ServiceFunctionPathKey pathKey = new ServiceFunctionPathKey(pathName);

        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (ServiceFunction serviceFunction : sfList) {

            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                    sfcServiceFunctionBuilder.setName(serviceFunction.getName())
                            .setKey(new SfcServiceFunctionKey(serviceFunction.getName()))
                            .setType(serviceFunction.getType())
                            .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(sfcName).setKey(sfcKey)
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(false);

        Object[] sfcParameters = {sfcBuilder.build()};
        Class[] sfcParameterTypes = {ServiceFunctionChain.class};

        executor.submit(SfcProviderServiceChainAPI
                .getPut(sfcParameters, sfcParameterTypes)).get();

        ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(pathName).setKey(pathKey)
                .setServiceChainName(sfcName);

        ServiceFunctionPath path = pathBuilder.build();

        Object[] pathParameters = {path};
        Class[] pathParameterTypes = {ServiceFunctionPath.class};

        Object result1 = executor.submit(SfcProviderServicePathAPI
                .getPut(pathParameters, pathParameterTypes)).get();
        boolean ret = (boolean) result1;

        Object[] pathParameters2 = {pathName};
        Class[] pathParameterTypes2 = {String.class};
        Object result2 = executor.submit(SfcProviderServicePathAPI
                .getRead(pathParameters2, pathParameterTypes2)).get();

        ServiceFunctionPath path2 = (ServiceFunctionPath) result2;

        assertNotNull("Must be not null", path2);
        assertEquals("Must be equal", path2.getServiceChainName(), sfcName);
    }

    @Test
    public void testReadAllServiceFunctionPaths() throws Exception {
        ServiceFunctionPaths serviceFunctionPaths;
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ServiceFunctionPathsBuilder serviceFunctionPathsBuilder = new ServiceFunctionPathsBuilder();
        String[] testValues = {"1", "Test Name", "Sfp Key"};

        List<ServiceFunctionPath> sffList = new ArrayList<>();

        //set all tested attributes
        serviceFunctionPathBuilder.setPathId(Long.valueOf(testValues[0]))
                .setServiceChainName(testValues[1])
                .setKey(new ServiceFunctionPathKey(testValues[2]))
                .setTransportType(VxlanGpe.class);
        sffList.add(serviceFunctionPathBuilder.build());

        serviceFunctionPathsBuilder.setServiceFunctionPath(sffList);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.
                builder(ServiceFunctionPaths.class).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfpsIID, serviceFunctionPathsBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        serviceFunctionPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();

        assertNotNull("Must not be null", serviceFunctionPaths);
        assertFalse("Must be false", serviceFunctionPaths.getServiceFunctionPath().isEmpty());
        assertEquals("Must be equal", serviceFunctionPaths.getServiceFunctionPath().get(0).getPathId(), Long.valueOf(testValues[0]));
        assertEquals("Must be equal", serviceFunctionPaths.getServiceFunctionPath().get(0).getServiceChainName(), testValues[1]);
        assertEquals("Must be equal", serviceFunctionPaths.getServiceFunctionPath().get(0).getKey().getName(), testValues[2]);
        assertEquals("Must be equal", serviceFunctionPaths.getServiceFunctionPath().get(0).getTransportType(), VxlanGpe.class);
    }
}

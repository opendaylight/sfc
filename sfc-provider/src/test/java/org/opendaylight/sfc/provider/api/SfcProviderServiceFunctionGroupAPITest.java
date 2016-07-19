/*
 * Copyright (c) 2014 Contextream, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithm;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.ServiceFunctionGroups;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.ServiceFunctionGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcProviderServiceFunctionGroupAPITest extends AbstractDataStoreManager {

    private static final String ALGORITHM = "algorithm";
    private static final SfName SF_NAME1 = new SfName("sfName1");
    private static final SfName SF_NAME2 = new SfName("sfName2");
    private static final String SFG_NAME = "sfgName";
    private static final String IP_V4_ADDRESS = "192.168.10.1";
    private static final String IP_V6_ADDRESS = "01:23:45:67:89:AB:CD:EF";
    private final List<ServiceFunction> sfList = new ArrayList<>();

    @Before
    public void before() {
        setOdlSfc();

        Ip dummyIp = SimpleTestEntityBuilder.buildLocatorTypeIp(new IpAddress(new Ipv4Address("5.5.5.5")), 555);
        SfDataPlaneLocator dummyLocator =
                SimpleTestEntityBuilder.buildSfDataPlaneLocator(new SfDataPlaneLocatorName("moscow-5.5.5.5:555-vxlan"),
                        dummyIp, new SffName("sff-moscow"), VxlanGpe.class);

        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_101"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.100.101")), dummyLocator, Boolean.FALSE));
        sfList.add(SimpleTestEntityBuilder.buildServiceFunction(new SfName("simple_fw_102"), new SftTypeName("firewall"),
                new IpAddress(new Ipv4Address("192.168.100.102")), dummyLocator, Boolean.FALSE));
    }

    @Test
    public void testPutReadDeleteServiceFunctionGroupAlgorithm() {

        // Create:
        String sfgAlgName = "testServiceFunctionGroupAlgorithm";
        ServiceFunctionGroupAlgorithm sfgAlg = SimpleTestEntityBuilder.buildServiceFunctionGroupAlgorithm(sfgAlgName);

        // Put:
        SfcProviderServiceFunctionGroupAlgAPI.putServiceFunctionGroupAlgorithm(sfgAlg);

        // Read:
        ServiceFunctionGroupAlgorithm sfgAlg2 =
                SfcProviderServiceFunctionGroupAlgAPI.readServiceFunctionGroupAlg(sfgAlgName);

        assertNotNull("Must be not null", sfgAlg2);
        assertEquals("Must be equal", sfgAlg2.getName(), sfgAlgName);

        // Delete:
        SfcProviderServiceFunctionGroupAlgAPI.deleteServiceFunctionGroupAlgorithm(sfgAlgName);
        sfgAlg2 = SfcProviderServiceFunctionGroupAlgAPI.readServiceFunctionGroupAlgorithm(sfgAlgName);
        assertNull("Must be null", sfgAlg2); // TODO: test passes locally but fails on Jenkins
    }

    @Test
    public void testPutReadDeleteServiceFunctionGroup() {

        // Create:
        String sfgName = "testServiceFunctionGroup";
        String sfgAlgName = "testServiceFunctionGroupAlgorithm"; // Not checking that the algorithm
                                                                 // exists
        ServiceFunctionGroup sfg = SimpleTestEntityBuilder.buildServiceFunctionGroup(sfgName, sfgAlgName);

        // Put:
        SfcProviderServiceFunctionGroupAPI.putServiceFunctionGroup(sfg);

        // Read:
        ServiceFunctionGroup sfg2 = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroup(sfgName);

        assertNotNull("Must be not null", sfg2);
        assertEquals("Must be equal", sfg2.getName(), sfgName);
        assertEquals("Must be equal", sfg2.getAlgorithm(), sfgAlgName);

        // Delete:
        SfcProviderServiceFunctionGroupAPI.deleteServiceFunctionGroup(sfgName);
        sfg2 = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroup(sfgName);

        assertNull("Must be null", sfg2);
    }

    @Test
    @Ignore
    public void testAddRemoveFunctionFromServiceFunctionGroup() {

        // Create:
        String sfgName = "testServiceFunctionGroup";
        String sfgAlgName = "testServiceFunctionGroupAlgorithm"; // Not checking that the algorithm
                                                                 // exists
        ServiceFunctionGroup sfg = SimpleTestEntityBuilder.buildServiceFunctionGroup(sfgName, sfgAlgName);

        // Add Service Function:
        ServiceFunction sf = sfList.get(0);
        SfcProviderServiceFunctionGroupAPI.addServiceFunctionToGroup(sfg.getName(), sf.getName());
        assertNotNull("Must be not null", sfg.getSfcServiceFunction().get(0));

        // Delete Service Function:
        SfcProviderServiceFunctionGroupAPI.removeServiceFunctionFromGroup(sfg.getName(), sf.getName());
        assertNull("Must be null", sfg.getSfcServiceFunction().get(0));
    }

    /*
     * Create two service function groups with different types, then read an delete them
     */
    @Test
    public void testPutGetDeleteServiceFunctionGroup() {

        // create service function groups, types firewall and dpi
        ServiceFunctionGroupsBuilder serviceFunctionGroupsBuilder = new ServiceFunctionGroupsBuilder();
        serviceFunctionGroupsBuilder.setServiceFunctionGroup(createServiceFunctionGroupList());

        InstanceIdentifier<ServiceFunctionGroups> sfgIID =
                InstanceIdentifier.builder(ServiceFunctionGroups.class).build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfgIID,
                serviceFunctionGroupsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        assertTrue("Must be true", transactionSuccessful);

        // get service function by type
        ServiceFunctionGroup serviceFunctionGroup =
                SfcProviderServiceFunctionGroupAPI.getServiceFunctionGroupByType(new SftTypeName("firewall"));

        assertNotNull("Must be not null", serviceFunctionGroup);
        assertEquals("Must be equal", serviceFunctionGroup.getIpMgmtAddress().getIpv4Address().getValue(),
                IP_V4_ADDRESS);
        assertEquals("Must be equal", serviceFunctionGroup.getType(), new SftTypeName("firewall"));

        serviceFunctionGroup = SfcProviderServiceFunctionGroupAPI.getServiceFunctionGroupByType(new SftTypeName("dpi"));

        assertNotNull("Must be not null", serviceFunctionGroup);
        assertEquals("Must be equal", serviceFunctionGroup.getIpMgmtAddress().getIpv6Address().getValue(),
                IP_V6_ADDRESS);
        assertEquals("Must be equal", serviceFunctionGroup.getType(), new SftTypeName("dpi"));

        // get service function group by name
        serviceFunctionGroup = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroup(SFG_NAME + 1);

        assertNotNull("Must be not null", serviceFunctionGroup);
        assertEquals("Must be equal", serviceFunctionGroup.getName(), SFG_NAME + 1);
        assertEquals("Must be equal", serviceFunctionGroup.getIpMgmtAddress().getIpv4Address().getValue(),
                IP_V4_ADDRESS);
        assertEquals("Must be equal", serviceFunctionGroup.getType(), new SftTypeName("firewall"));

        serviceFunctionGroup = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroup(SFG_NAME + 2);

        assertNotNull("Must be not null", serviceFunctionGroup);
        assertEquals("Must be equal", serviceFunctionGroup.getName(), SFG_NAME + 2);
        assertEquals("Must be equal", serviceFunctionGroup.getIpMgmtAddress().getIpv6Address().getValue(),
                IP_V6_ADDRESS);
        assertEquals("Must be equal", serviceFunctionGroup.getType(), new SftTypeName("dpi"));

        // delete transaction
        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(sfgIID, LogicalDatastoreType.CONFIGURATION);

        assertTrue("Must be true", transactionSuccessful);
    }

    @Test
    public void testGetSfgNameList() {

        // create service function groups, types firewall and dpi
        ServiceFunctionGroupsBuilder serviceFunctionGroupsBuilder = new ServiceFunctionGroupsBuilder();
        serviceFunctionGroupsBuilder.setServiceFunctionGroup(createServiceFunctionGroupList());

        InstanceIdentifier<ServiceFunctionGroups> sfgIID =
                InstanceIdentifier.builder(ServiceFunctionGroups.class).build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfgIID,
                serviceFunctionGroupsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        assertTrue("Must be true", transactionSuccessful);

        // create service function chain
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        serviceFunctionChainBuilder.setSfcServiceFunction(createSfcServiceFunctionList());

        List<String> sfgNameList =
                SfcProviderServiceFunctionGroupAPI.getSfgNameList(serviceFunctionChainBuilder.build());

        assertNotNull("Must be not null", sfgNameList);
        assertEquals("Must be equal", sfgNameList.toString(), "[" + SFG_NAME + 1 + ", " + SFG_NAME + 2 + "]");
    }

    // create sfg list with two entries
    private List<ServiceFunctionGroup> createServiceFunctionGroupList() {

        List<ServiceFunctionGroup> serviceFunctionGroupList = new ArrayList<>();
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();

        // ip v4 group
        serviceFunctionGroupBuilder.setName(SFG_NAME + 1)
            .setKey(new ServiceFunctionGroupKey(SFG_NAME + 1))
            .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_V4_ADDRESS)))
            .setAlgorithm(ALGORITHM + 1)
            .setGroupId(100L)
            .setType(new SftTypeName("firewall"));
        serviceFunctionGroupList.add(serviceFunctionGroupBuilder.build());

        // ipv6 group
        serviceFunctionGroupBuilder.setName(SFG_NAME + 2)
            .setKey(new ServiceFunctionGroupKey(SFG_NAME + 2))
            .setIpMgmtAddress(new IpAddress(new Ipv6Address(IP_V6_ADDRESS)))
            .setAlgorithm(ALGORITHM + 2)
            .setGroupId(101L)
            .setType(new SftTypeName("dpi"));
        serviceFunctionGroupList.add(serviceFunctionGroupBuilder.build());

        return serviceFunctionGroupList;
    }

    // create sfc service function list
    private List<SfcServiceFunction> createSfcServiceFunctionList() {

        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        // first entry
        // XXX Note SFC YANG model says that this Service Function Name IS NOT the name of a Service
        // Function, so
        // not sure exactly what this is testing.
        SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME1.getValue())
            .setKey(new SfcServiceFunctionKey(SF_NAME1.getValue()))
            .setType(new SftTypeName("firewall"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());

        // second entry
        sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME2.getValue())
            .setKey(new SfcServiceFunctionKey(SF_NAME2.getValue()))
            .setType(new SftTypeName("dpi"));
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());

        return sfcServiceFunctionList;
    }
}

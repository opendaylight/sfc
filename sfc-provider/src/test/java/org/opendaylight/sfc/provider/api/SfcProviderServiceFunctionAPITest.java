/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcProviderServiceFunctionAPITest extends AbstractDataStoreManager {

    private static final List<String> LOCATOR_IP_ADDRESS = new ArrayList<String>() {

        {
            add("196.168.55.1");
            add("196.168.55.2");
            add("196.168.55.3");
        }
    };

    private static final List<String> IP_MGMT_ADDRESS = new ArrayList<String>() {

        {
            add("196.168.55.101");
            add("196.168.55.102");
            add("196.168.55.103");
        }
    };
    private static final SfName SF_STATE_NAME = new SfName("dummySFS");
    private static final SfpName SF_SERVICE_PATH = new SfpName("dummySFSP");
    private static final SfName SF_NAME = new SfName("dummySF");
    private static final SfName SF_NAME1 = new SfName("dummySF1");
    private static final RspName RSP_NAME1 = new RspName("dummyRSP1");
    private static final SfName SF_NAME2 = new SfName("dummySF2");
    private static final RspName RSP_NAME2 = new RspName("dummyRSP2");
    private static final int PORT = 555;

    @Before
    public void before() {
        setOdlSfc();
    }

    @Test
    public void testCreateReadServiceFunction() {

        SfName name = new SfName("unittest-fw-1");
        SftTypeName type = new SftTypeName("firewall");
        IpAddress ipMgmtAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(0)));
        SfDataPlaneLocator sfDataPlaneLocator;
        ServiceFunctionKey key = new ServiceFunctionKey(name);

        IpAddress ipAddress = new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(0)));
        PortNumber portNumber = new PortNumber(PORT);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(0)))
            .setLocatorType(ipBuilder.build())
            .setTransport(SlTransportType.class);
        sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(name)
            .setKey(key)
            .setType(type)
            .setIpMgmtAddress(ipMgmtAddress)
            .setSfDataPlaneLocator(dataPlaneLocatorList);

        SfcProviderServiceFunctionAPI.putServiceFunction(sfBuilder.build());
        ServiceFunction sf2 = SfcProviderServiceFunctionAPI.readServiceFunction(name);

        assertNotNull("Must be not null", sf2);
        assertEquals("Must be equal", sf2.getIpMgmtAddress(), ipMgmtAddress);
        assertEquals("Must be equal", sf2.getType(), type);
        assertEquals("Must be equal", sf2.getSfDataPlaneLocator(), dataPlaneLocatorList);
    }

    @Test
    public void testCreateReadServiceFunctions() throws ExecutionException, InterruptedException {

        final List<SfName> sfName = new ArrayList<SfName>() {

            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-fw-2"));
                add(new SfName("unittest-fw-3"));
            }
        };
        final SftTypeName sfType = new SftTypeName("firewall");
        final IpAddress[] ipMgmtAddress = {new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(0))),
                new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(1))),
                new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS.get(2)))};
        SfDataPlaneLocator[] sfDataPlaneLocator = new SfDataPlaneLocator[3];
        ServiceFunctionKey[] key = new ServiceFunctionKey[3];
        for (int i = 0; i < 3; i++) {
            key[i] = new ServiceFunctionKey(new SfName(sfName.get(i)));
        }

        final IpAddress[] locatorIpAddress = {new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(0))),
                new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(1))),
                new IpAddress(new Ipv4Address(LOCATOR_IP_ADDRESS.get(2)))};
        PortNumber portNumber = new PortNumber(PORT);

        List<ServiceFunction> list = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(locatorIpAddress[i]).setPort(portNumber);
            SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
            locatorBuilder.setName(new SfDataPlaneLocatorName(LOCATOR_IP_ADDRESS.get(i)))
                .setLocatorType(ipBuilder.build())
                .setTransport(SlTransportType.class);
            sfDataPlaneLocator[i] = locatorBuilder.build();

            ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
            List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
            dataPlaneLocatorList.add(sfDataPlaneLocator[i]);
            sfBuilder.setName(new SfName(sfName.get(i)))
                .setKey(key[i])
                .setType(sfType)
                .setIpMgmtAddress(ipMgmtAddress[i])
                .setSfDataPlaneLocator(dataPlaneLocatorList);
            list.add(sfBuilder.build());
        }

        ServiceFunctionsBuilder sfsBuilder = new ServiceFunctionsBuilder();
        sfsBuilder.setServiceFunction(list);

        SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SF_IID, sfsBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        ServiceFunction sf2 = SfcProviderServiceFunctionAPI.readServiceFunction(new SfName(sfName.get(1)));

        assertNotNull("Must be not null", sf2);
        assertEquals("Must be equal", sf2.getIpMgmtAddress(), ipMgmtAddress[1]);
        assertEquals("Must be equal", sf2.getType(), sfType);
        List<SfDataPlaneLocator> dataPlaneLocatorList2 = new ArrayList<>();
        dataPlaneLocatorList2.add(sfDataPlaneLocator[1]);
        assertEquals("Must be equal", sf2.getSfDataPlaneLocator(), dataPlaneLocatorList2);
    }

    /*
     * service function is created and then read
     * next part of this test removes service function from data store
     */
    @Test
    public void testCreateReadDeleteServiceFunction() throws Exception {

        // create service function and put it to data store
        boolean transactionSuccessful = writeRemoveServiceFunction(IP_MGMT_ADDRESS.get(1), true);

        assertTrue("Must be true", transactionSuccessful);

        // read service function with its name and return it
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(SF_NAME);

        assertNotNull("Must be not null", serviceFunction);
        assertEquals("Must be equal", serviceFunction.getIpMgmtAddress().getIpv4Address().getValue(),
                IP_MGMT_ADDRESS.get(1));

        // now we delete that service function and check whether it was deleted or not
        transactionSuccessful = writeRemoveServiceFunction(IP_MGMT_ADDRESS.get(1), false);

        assertTrue("Must be true", transactionSuccessful);
    }

    /*
     * service function state is created and then read
     * next part of this test removes service function state from data store
     */
    @Test
    public void testCreateReadDeleteServiceFunctionState() throws Exception {

        // create service function state and put it to data store
        boolean transactionSuccessful = writeRemoveServiceFunctionState(true);

        assertTrue("Must be true", transactionSuccessful);

        // read service function state with its name
        // list of SfServicePath will be returned
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(SF_STATE_NAME);

        assertNotNull("Must not be null", sfServicePathList);
        assertEquals("Must be equal", sfServicePathList.get(0).getName(), SF_SERVICE_PATH);

        // read service function state with its name
        // list of Strings representing paths will be returned
        List<RspName> rspList = SfcProviderServiceFunctionAPI.getRspsBySfName(SF_STATE_NAME);

        assertNotNull("Must not be null", rspList);
        assertEquals("Must be equal", rspList.get(0).getValue(), SF_SERVICE_PATH.getValue());

        // now we delete that service function state and check whether it was deleted or not
        transactionSuccessful = writeRemoveServiceFunctionState(false);

        assertTrue("Must be true", transactionSuccessful);
    }

    /*
     * list of service functions is created and read
     * next part of this test removes these service functions from data store
     */
    @Test
    public void testReadAllServiceFunctionsExecutor() throws Exception {

        // create service functions
        boolean transactionSuccessful =
                writeRemoveServiceFunctions(IP_MGMT_ADDRESS.get(0), new SftTypeName("firewall"), true);

        assertTrue("Must be true", transactionSuccessful);

        // read all service functions from data store
        ServiceFunctions serviceFunctionsResult = SfcProviderServiceFunctionAPI.readAllServiceFunctions();

        assertNotNull("Must not be null", serviceFunctionsResult);
        assertEquals("Must be equal", serviceFunctionsResult.getServiceFunction().get(0).getName(), SF_NAME);
        assertEquals("Must be equal",
                serviceFunctionsResult.getServiceFunction().get(0).getIpMgmtAddress().getIpv4Address().getValue(),
                IP_MGMT_ADDRESS.get(0));
        assertEquals("Must be equal", serviceFunctionsResult.getServiceFunction().get(0).getType(),
                new SftTypeName("firewall"));

        // delete these functions
        transactionSuccessful = writeRemoveServiceFunctions(IP_MGMT_ADDRESS.get(1), new SftTypeName("firewall"), false);

        assertTrue("Must be true", transactionSuccessful);
    }

    /*
     * service function state is created and a path is added into it
     * then, path will be removed
     */
    @Test
    public void testAddPathToServiceFunctionStateExecutorString() throws Exception {

        // first, create service function state without paths
        boolean transactionSuccessful = writeRemoveServiceFunctionState();

        assertTrue("Must be true", transactionSuccessful);

        // second, create path and write it into data store
        transactionSuccessful = (boolean) writeReturnPath(RSP_NAME1, SF_NAME1, true);

        assertTrue("Must be true", transactionSuccessful);

        RenderedServicePath renderedServicePath = (RenderedServicePath) writeReturnPath(RSP_NAME1, SF_NAME1, false);
        // add this path to service function
        boolean result = SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(renderedServicePath);

        assertTrue("Must be true", result);

        // now create another path, and put object as a parameter
        renderedServicePath = (RenderedServicePath) writeReturnPath(RSP_NAME2, SF_NAME2, false);

        // add this path to service function, an object of service path is used
        result = SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(renderedServicePath);

        assertTrue("Must be true", result);

        // delete both paths
        result = SfcProviderServiceFunctionAPI
            .deleteServicePathFromServiceFunctionState(new SfpName(RSP_NAME1.getValue()));

        assertTrue("Must be true", result);

        result = SfcProviderServiceFunctionAPI
            .deleteServicePathFromServiceFunctionState(new SfpName(RSP_NAME2.getValue()));

        assertTrue("Must be true", result);
    }

    // write or remove service function
    private boolean writeRemoveServiceFunction(String ipAddress, boolean write) {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME)
            .setKey(new ServiceFunctionKey(SF_NAME))
            .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)));

        InstanceIdentifier<ServiceFunction> sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
            .child(ServiceFunction.class, new ServiceFunctionKey(SF_NAME))
            .build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sfIID, serviceFunctionBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sfIID, LogicalDatastoreType.CONFIGURATION);
    }

    // write or remove service functions
    private boolean writeRemoveServiceFunctions(String ipAddress, SftTypeName type, boolean write) {
        ServiceFunctionsBuilder serviceFunctionsBuilder = new ServiceFunctionsBuilder();
        List<ServiceFunction> serviceFunctions = new ArrayList<>();
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();

        serviceFunctionBuilder.setName(SF_NAME)
            .setIpMgmtAddress(new IpAddress(new Ipv4Address(ipAddress)))
            .setKey(new ServiceFunctionKey(SF_NAME))
            .setNshAware(true)
            .setType(type);
        serviceFunctions.add(serviceFunctionBuilder.build());
        serviceFunctionsBuilder.setServiceFunction(serviceFunctions);

        InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sfsIID, serviceFunctionsBuilder.build(),
                    LogicalDatastoreType.CONFIGURATION);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sfsIID, LogicalDatastoreType.CONFIGURATION);
    }

    // write or remove service function state
    private boolean writeRemoveServiceFunctionState() {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        List<SfServicePath> sfServicePathList = new ArrayList<>();

        serviceFunctionStateBuilder.setName(SF_STATE_NAME)
            .setKey(new ServiceFunctionStateKey(SF_STATE_NAME))
            .setSfServicePath(sfServicePathList);

        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
            .child(ServiceFunctionState.class, new ServiceFunctionStateKey(SF_STATE_NAME))
            .build();

        return SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);
    }

    // write or remove service function state with path
    private boolean writeRemoveServiceFunctionState(boolean write) {
        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        List<SfServicePath> sfServicePathList = new ArrayList<>();

        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setName(SF_SERVICE_PATH).setKey(new SfServicePathKey(SF_SERVICE_PATH));
        sfServicePathList.add(sfServicePathBuilder.build());

        serviceFunctionStateBuilder.setName(SF_STATE_NAME)
            .setKey(new ServiceFunctionStateKey(SF_STATE_NAME))
            .setSfServicePath(sfServicePathList);

        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
            .child(ServiceFunctionState.class, new ServiceFunctionStateKey(SF_STATE_NAME))
            .build();
        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(),
                    LogicalDatastoreType.OPERATIONAL);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
    }

    // write path or write path and return path object
    private Object writeReturnPath(RspName pathName, SfName sfName, boolean write) {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        List<RenderedServicePathHop> renderedServicePathHops = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        renderedServicePathHopBuilder.setServiceFunctionName(sfName)
            .setKey(new RenderedServicePathHopKey(Short.valueOf("1")));
        renderedServicePathHops.add(renderedServicePathHopBuilder.build());

        renderedServicePathBuilder.setName(pathName)
            .setKey(new RenderedServicePathKey(pathName))
            .setRenderedServicePathHop(renderedServicePathHops);

        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
            .child(RenderedServicePath.class, new RenderedServicePathKey(pathName))
            .build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(),
                    LogicalDatastoreType.OPERATIONAL);
        else {
            SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(),
                    LogicalDatastoreType.OPERATIONAL);
            return renderedServicePathBuilder.build();
        }
    }
}

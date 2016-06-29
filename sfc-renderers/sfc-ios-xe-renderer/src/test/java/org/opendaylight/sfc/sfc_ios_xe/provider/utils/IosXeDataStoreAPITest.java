/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.utils;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.ConfigServiceChainSfModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.config.service.chain.sf.mode.EncapsulationBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.config.service.chain.sf.mode.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.ConfigServiceChainPathModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.ServiceIndexBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.DELETE_FUNCTION;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.DELETE_LOCAL;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.DELETE_PATH;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.DELETE_REMOTE;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.READ_FUNCTION;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.READ_LOCAL;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.READ_REMOTE;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.WRITE_FUNCTION;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.WRITE_LOCAL;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.WRITE_PATH;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.WRITE_REMOTE;


public class IosXeDataStoreAPITest extends AbstractDataBrokerTest {

    private final OpendaylightSfc odl = new OpendaylightSfc();
    private final String REMOTE_FORWARDER = "remote-forwarder";
    private final String SERVICE_NAME = "service-function";
    private IosXeDataStoreAPI iosXeDataStoreAPI;
    private DataBroker mountpoint;

    @Before
    public void init() {
        // Initialize datastore
        mountpoint = getDataBroker();
        odl.setDataProvider(mountpoint);
    }

    @Test
    public void writeServiceFunction() {
        ServiceFunction data = buildTestServiceFunction();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_FUNCTION, LogicalDatastoreType.CONFIGURATION);
        boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
    }

    @Test
    public void readServiceFunction() {
        ServiceFunction data = buildTestServiceFunction();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SfName(SERVICE_NAME), READ_FUNCTION,
                LogicalDatastoreType.CONFIGURATION);
        // Read empty datastore
        ServiceFunction function = (ServiceFunction) iosXeDataStoreAPI.call();
        assertNull(function);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_FUNCTION,
                LogicalDatastoreType.CONFIGURATION);
        // Write service function
        Boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SfName(SERVICE_NAME), READ_FUNCTION,
                LogicalDatastoreType.CONFIGURATION);
        // Read again
        function = (ServiceFunction) iosXeDataStoreAPI.call();
        assertEquals(buildTestServiceFunction(), function);
    }

    @Test
    public void deleteServiceFunction() {
        ServiceFunction data = buildTestServiceFunction();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_FUNCTION,
                LogicalDatastoreType.CONFIGURATION);
        // Put service function
        boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SfName(SERVICE_NAME), READ_FUNCTION,
                LogicalDatastoreType.CONFIGURATION);
        // Read it
        ServiceFunction function = (ServiceFunction) iosXeDataStoreAPI.call();
        assertEquals(buildTestServiceFunction(), function);
        // Remove
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new ServiceFunctionKey(SERVICE_NAME), DELETE_FUNCTION,
                LogicalDatastoreType.CONFIGURATION);
        result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SfName(SERVICE_NAME), READ_FUNCTION,
                LogicalDatastoreType.CONFIGURATION);
        // Read again, should be null
        function = (ServiceFunction) iosXeDataStoreAPI.call();
        assertNull(function);
    }

    @Test
    public void writeLocalServiceForwarder() {
        Local data = buildLocalServiceForwarder();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_LOCAL, LogicalDatastoreType.CONFIGURATION);
        boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
    }

    @Test
    public void readLocalServiceForwarder() {
        Local data = buildLocalServiceForwarder();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, null, READ_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        Local forwarder = (Local) iosXeDataStoreAPI.call();
        assertNull(forwarder);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        Boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, null, READ_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        forwarder = (Local) iosXeDataStoreAPI.call();
        assertEquals(buildLocalServiceForwarder(), forwarder);
    }

    @Test
    public void deleteLocalServiceForwarder() {
        Local data = buildLocalServiceForwarder();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        // Put service function
        boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, null, READ_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        // Read it
        Local forwarder = (Local) iosXeDataStoreAPI.call();
        assertEquals(buildLocalServiceForwarder(), forwarder);
        // Remove
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, null, DELETE_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, null, READ_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        // Read again, should be null
        forwarder = (Local) iosXeDataStoreAPI.call();
        assertNull(forwarder);
    }

    @Test
    public void writeRemoteServiceForwarder() {
        ServiceFfName data = buildRemoteServiceForwarder();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_REMOTE, LogicalDatastoreType.CONFIGURATION);
        boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
    }

    @Test
    public void readRemoteServiceForwarder() {
        ServiceFfName data = buildRemoteServiceForwarder();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SffName(REMOTE_FORWARDER), READ_REMOTE,
                LogicalDatastoreType.CONFIGURATION);
        ServiceFfName forwarder = (ServiceFfName) iosXeDataStoreAPI.call();
        assertNull(forwarder);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_REMOTE, LogicalDatastoreType.CONFIGURATION);
        Boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SffName(REMOTE_FORWARDER), READ_REMOTE,
                LogicalDatastoreType.CONFIGURATION);
        forwarder = (ServiceFfName) iosXeDataStoreAPI.call();
        assertEquals(buildRemoteServiceForwarder(), forwarder);
    }

    @Test
    public void deleteRemoteServiceForwarder() {
        ServiceFfName data = buildRemoteServiceForwarder();
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_REMOTE,
                LogicalDatastoreType.CONFIGURATION);
        // Put service function
        boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SffName(REMOTE_FORWARDER), READ_REMOTE,
                LogicalDatastoreType.CONFIGURATION);
        // Read it
        ServiceFfName forwarder = (ServiceFfName) iosXeDataStoreAPI.call();
        assertEquals(buildRemoteServiceForwarder(), forwarder);
        // Remove
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, DELETE_REMOTE, LogicalDatastoreType.CONFIGURATION);
        result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new SffName(REMOTE_FORWARDER), READ_LOCAL,
                LogicalDatastoreType.CONFIGURATION);
        // Read again, should be null
        forwarder = (ServiceFfName) iosXeDataStoreAPI.call();
        assertNull(forwarder);
    }

    @Test
    public void writeRemovePath() {
        ServicePath data = buildServicePath();
        // Write
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, data, WRITE_PATH, LogicalDatastoreType.CONFIGURATION);
        boolean result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
        // Delete
        iosXeDataStoreAPI = new IosXeDataStoreAPI(mountpoint, new ServicePathKey(1L), DELETE_PATH,
                LogicalDatastoreType.CONFIGURATION);
        result = (boolean) iosXeDataStoreAPI.call();
        assertTrue(result);
    }

    private ServiceFunction buildTestServiceFunction() {
        ConfigServiceChainSfModeBuilder sfModeBuilder = new ConfigServiceChainSfModeBuilder();
        sfModeBuilder.setIp(new IpBuilder().setAddress(new Ipv4Address("10.0.0.1")).build())
                .setEncapsulation(new EncapsulationBuilder().setNone(true).build());
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SERVICE_NAME)
                .setKey(new ServiceFunctionKey(SERVICE_NAME))
                .setConfigServiceChainSfMode(sfModeBuilder.build());
        return serviceFunctionBuilder.build();
    }

    private Local buildLocalServiceForwarder() {
        LocalBuilder localBuilder = new LocalBuilder();
        localBuilder.setIp(new org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.config.service.chain.grouping.IpBuilder()
                .setAddress(new Ipv4Address("100.0.0.1")).build());
        return localBuilder.build();
    }

    private ServiceFfName buildRemoteServiceForwarder() {
        ServiceFfNameBuilder serviceFfNameBuilder = new ServiceFfNameBuilder();
        serviceFfNameBuilder.setName(REMOTE_FORWARDER)
                .setKey(new ServiceFfNameKey(REMOTE_FORWARDER))
                .setIp(new org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.config.service.chain.grouping.IpBuilder()
                        .setAddress(new Ipv4Address("200.0.0.1")).build());
        return serviceFfNameBuilder.build();
    }

    private ServicePath buildServicePath() {
        ConfigServiceChainPathModeBuilder configServiceChainPathModeBuilder = new ConfigServiceChainPathModeBuilder();
        configServiceChainPathModeBuilder.setServiceIndex(new ServiceIndexBuilder().build());
        ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
        servicePathBuilder.setKey(new ServicePathKey(1L))
                .setServicePathId(1L)
                .setConfigServiceChainPathMode(configServiceChainPathModeBuilder.build());
        return servicePathBuilder.build();
    }

}





























/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.Open;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Tests for simple datastore operations on SFFs (i.e. Service Functions are created first)
 */
public class SfcProviderServiceForwarderAPISimpleTest extends AbstractDataStoreManager {

    SffName sffName = new SffName("SFF1");
    private final RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
    private final RenderedServicePathHop renderedServicePathHop = renderedServicePathHopBuilder
        .setKey(new RenderedServicePathHopKey((short) 3)).setServiceFunctionForwarder(sffName).build();
    ServiceFunctionForwarder sff;

    @Before
    public void before() {
        setOdlSfc();
        RspName rspName = new RspName("RSP1");
        SffName sffName = new SffName("SFF1");

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();

        long pathId = SfcServicePathId.check_and_allocate_pathid();

        assertNotEquals("Must be not equal", pathId, -1);

        renderedServicePathBuilder.setName(rspName).setKey(new RenderedServicePathKey(rspName)).setPathId(pathId);

        List<RenderedServicePathHop> renderedServicePathHopList = new ArrayList<>();
        renderedServicePathHopList.add(renderedServicePathHop);

        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);
        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
            .child(RenderedServicePath.class, new RenderedServicePathKey(rspName))
            .build();
        SfcDataStoreAPI.writePutTransactionAPI(rspIID, renderedServicePathBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);

        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setName(sffName).setKey(new ServiceFunctionForwarderKey(sffName));
        ServiceFunctionForwarder sff = sffBuilder.build();

        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(sffName);
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                    .build();

        SfcDataStoreAPI.writePutTransactionAPI(sffEntryIID, sff, LogicalDatastoreType.CONFIGURATION);
    }

    @SuppressWarnings("serial")
    @Test
    public void testCreateReadUpdateServiceFunctionForwarder() {
        SffName name = new SffName("SFF1");
        SffDataPlaneLocatorName sffDplName = new SffDataPlaneLocatorName("locator-1");
        SfDataPlaneLocatorName sfDplName = new SfDataPlaneLocatorName("untitest-fw-1");
        List<SfName> sfNames = new ArrayList<SfName>() {

            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-dpi-1"));
                add(new SfName("unittest-napt-1"));
                add(new SfName("unittest-http-header-enrichment-1"));
                add(new SfName("unittest-qos-1"));
            }
        };
        IpAddress[] ipMgmtAddress = new IpAddress[sfNames.size()];

        List<SffDataPlaneLocator> locatorList = new ArrayList<>();

        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address("10.1.1.101"))).setPort(new PortNumber(555));

        DataPlaneLocatorBuilder sffLocatorBuilder = new DataPlaneLocatorBuilder();
        sffLocatorBuilder.setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        locatorBuilder.setName(sffDplName)
            .setKey(new SffDataPlaneLocatorKey(sffDplName))
            .setDataPlaneLocator(sffLocatorBuilder.build());

        locatorList.add(locatorBuilder.build());

        List<ServiceFunctionDictionary> dictionary = new ArrayList<>();

        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder.setName(sfDplName).setKey(new SfDataPlaneLocatorKey(sfDplName));

        SfDataPlaneLocator sfDataPlaneLocator = sfDataPlaneLocatorBuilder.build();
        List<ServiceFunction> sfList = new ArrayList<>();

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);
        sfBuilder.setName(new SfName(sfNames.get(0)))
            .setKey(new ServiceFunctionKey(new SfName("unittest-fw-1")))
            .setType(new SftTypeName("firewall"))
            .setIpMgmtAddress(ipMgmtAddress[0])
            .setSfDataPlaneLocator(dataPlaneLocatorList);
        sfList.add(sfBuilder.build());

        ServiceFunction sf = sfList.get(0);
        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder.setSfDplName(sf.getSfDataPlaneLocator().get(0).getName());
        SffSfDataPlaneLocator sffSfDataPlaneLocator = sffSfDataPlaneLocatorBuilder.build();
        ServiceFunctionDictionaryBuilder dictionaryEntryBuilder = new ServiceFunctionDictionaryBuilder();
        dictionaryEntryBuilder.setName(sf.getName())
            .setKey(new ServiceFunctionDictionaryKey(sf.getName()))
            .setSffSfDataPlaneLocator(sffSfDataPlaneLocator)
            .setFailmode(Open.class)
            .setSffInterfaces(null);

        ServiceFunctionDictionary dictionaryEntry = dictionaryEntryBuilder.build();
        dictionary.add(dictionaryEntry);

        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();

        ServiceFunctionForwarder sff = sffBuilder.setName(name)
            .setKey(new ServiceFunctionForwarderKey(name))
            .setSffDataPlaneLocator(locatorList)
            .setServiceFunctionDictionary(dictionary)
            .setServiceNode(null) // for consistency only; we are going to get rid of ServiceNodes
                                  // in the future
            .build();

        SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff);

        ServiceFunctionForwarder sff2 = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(name);

        assertNotNull("Must be not null", sff2);
        assertEquals("Must be equal", sff2.getSffDataPlaneLocator(), locatorList);
        assertEquals("Must be equal", sff2.getServiceFunctionDictionary(), dictionary);

    }

    /*
     * test creates service function forwarder and four rendered service paths
     * these paths are added to service function forwarder state with different methods
     * all paths are then checked whether they are already in sff state or not
     * all paths are removed with different ways and checked whether they has been really removed
     */
    @Test
    public void testAddDeletePathsFromServiceForwarderState() {
        SffName sff = new SffName("sff");
        RspName rsp1 = new RspName("rsp1");
        RspName rsp2 = new RspName("rsp2");
        RspName rsp3 = new RspName("rsp3");
        RspName rsp4 = new RspName("rsp4");

        // create service function forwarder and write it into data store
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(sff).setKey(new ServiceFunctionForwarderKey(sff));

        boolean transactionSuccessful =
                SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(serviceFunctionForwarderBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        // create list of path for testing purposes
        List<RenderedServicePath> sffServicePathTestList = new ArrayList<>();
        sffServicePathTestList.add(createRenderedServicePath(rsp1, sff, (short) 1));
        sffServicePathTestList.add(createRenderedServicePath(rsp2, sff, (short) 2));
        sffServicePathTestList.add(createRenderedServicePath(rsp3, sff, (short) 3));
        sffServicePathTestList.add(createRenderedServicePath(rsp4, sff, (short) 4));

        // add four paths to service function forwarder state via rsp objects
        transactionSuccessful =
                SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(sffServicePathTestList.get(0));
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful =
                SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(sffServicePathTestList.get(1));
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful =
                SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(sffServicePathTestList.get(2));
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful =
                SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(sffServicePathTestList.get(3));
        assertTrue("Must be true", transactionSuccessful);

        // read service function forwarder state from data store, it should return list of all four
        // paths
        List<SffServicePath> sffServicePaths = SfcProviderServiceForwarderAPI.readSffState(sff);
        assertNotNull("Must be not null", sffServicePaths);
        assertEquals("Must be equal", sffServicePaths.size(), 4);

        // remove path 1 via service path object
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
        serviceFunctionPathBuilder.setName(new SfpName(rsp1.getValue()));
        transactionSuccessful =
                SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(serviceFunctionPathBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        // remove path 2 via rendered service path name
        transactionSuccessful = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rsp2);
        assertTrue("Must be true", transactionSuccessful);

        // remove paths 3 & 4 via list of rendered service path names
        List<RspName> rspNames = new ArrayList<>();
        rspNames.add(new RspName("rsp3"));
        rspNames.add(new RspName("rsp4"));
        transactionSuccessful = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspNames);
        assertTrue("Must be true", transactionSuccessful);

        // read service function forwarder state from data store, paths are removed, should return
        // null
        sffServicePaths = SfcProviderServiceForwarderAPI.readSffState(sff);
        assertNull("Must be null", sffServicePaths);

        // remove written forwarder
        transactionSuccessful = SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarder(sff);
        assertTrue("Must be true", transactionSuccessful);
    }

    @Test
    public void testPutServiceFunctionForwarderExecutor() {
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        SffName sffName = new SffName("SFF1");
        sffBuilder.setName(sffName).setKey(new ServiceFunctionForwarderKey(sffName));
        ServiceFunctionForwarder sff = sffBuilder.build();
        assertTrue(SfcProviderServiceForwarderAPI.putServiceFunctionForwarder(sff));
    }

    @Test
    public void testDeleteServiceFunctionForwarder() {
        SffName sffName = new SffName("SFF1");
        assertTrue(SfcProviderServiceForwarderAPI.deleteServiceFunctionForwarder(sffName));
    }

    private RenderedServicePath createRenderedServicePath(RspName rspName, SffName sffName, short pathKey) {

        // create rendered service path and write to data store
        List<RenderedServicePathHop> renderedServicePathHops = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        renderedServicePathHopBuilder.setKey(new RenderedServicePathHopKey(pathKey))
            .setServiceFunctionForwarder(sffName);
        renderedServicePathHops.add(renderedServicePathHopBuilder.build());

        long pathId = SfcServicePathId.check_and_allocate_pathid();

        assertNotEquals("Must be not equal", pathId, -1);

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setName(rspName)
            .setKey(new RenderedServicePathKey(rspName))
            .setRenderedServicePathHop(renderedServicePathHops)
            .setPathId(pathId);

        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
            .child(RenderedServicePath.class, new RenderedServicePathKey(rspName))
            .build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(rspIID,
                renderedServicePathBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        return renderedServicePathBuilder.build();
    }

}

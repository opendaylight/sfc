/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Nsh;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Transport;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ShortestPath;
import org.powermock.reflect.Whitebox;

public class SfcProviderRenderedPathAPITest extends AbstractSfcRendererServicePathAPITest {

    @Before
    public void before() throws Exception {
        // AbstractDataStoreManager.setupSfc() initializes a new dataBroker
        // for each test, thus starting with a clean data store
        setupSfc();
    }

    @After
    public void after() throws Exception {
        // AbstractDataStoreManager.close() deletes all SFC entries from the
        // data store
        close();
    }

    @Test
    // test, whether scheduler type create right scheduler instance
    public void testGetServiceFunctionScheduler() throws Exception {
        // TODO remove reflection for "getServiceFunctionScheduler"
        SfcServiceFunctionSchedulerAPI rrResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class,
                "getServiceFunctionScheduler", RoundRobin.class);
        assertEquals("Must be equal", rrResult.getClass(), SfcServiceFunctionRoundRobinSchedulerAPI.class);
        SfcServiceFunctionSchedulerAPI lbResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class,
                "getServiceFunctionScheduler", LoadBalance.class);
        assertEquals("Must be equal", lbResult.getClass(), SfcServiceFunctionLoadBalanceSchedulerAPI.class);
        SfcServiceFunctionSchedulerAPI rnResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class,
                "getServiceFunctionScheduler", Random.class);

        assertEquals("Must be equal", rnResult.getClass(), SfcServiceFunctionRandomSchedulerAPI.class);
        SfcServiceFunctionSchedulerAPI spResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class,
                "getServiceFunctionScheduler", ShortestPath.class);
        assertEquals("Must be equal", spResult.getClass(), SfcServiceFunctionShortestPathSchedulerAPI.class);
        SfcServiceFunctionSchedulerAPI rsResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class,
                "getServiceFunctionScheduler", ServiceFunctionSchedulerTypeIdentity.class);
        assertEquals("Must be equal", rsResult.getClass(), SfcServiceFunctionRandomSchedulerAPI.class);
    }

    @Test
    public void testReadRenderedServicePathFirstHop() {
        super.init();

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        /* Create RenderedServicePath and reverse RenderedServicePath */
        RenderedServicePath renderedServicePath = null;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue());

        renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());
        assertNotNull("Must be not null", renderedServicePath);

        RenderedServicePath revRenderedServicePath = SfcProviderRenderedPathAPI
                .createSymmetricRenderedServicePathAndState(renderedServicePath);
        assertNotNull("Must be not null", revRenderedServicePath);

        RenderedServicePathFirstHop firstHop;
        RenderedServicePathFirstHop lastHop;
        firstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(renderedServicePath.getName());
        assertNotNull("Must be not null", firstHop);
        lastHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(revRenderedServicePath.getName());
        assertNotNull("Must be not null", lastHop);
        LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
        LOG.debug("Last hop IP: {}, port: {}", lastHop.getIp().toString(), lastHop.getPort());
        assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP.get(0))));
        assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT.get(0)));
        assertEquals("Must be equal", lastHop.getIp(),
                new IpAddress(new Ipv4Address(SFF_LOCATOR_IP.get(SFF_LOCATOR_IP.size() - 1))));
        assertEquals("Must be equal", lastHop.getPort(), new PortNumber(PORT.get(PORT.size() - 1)));
        SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
        SfcProviderRenderedPathAPI.deleteRenderedServicePath(revRenderedServicePath.getName());
    }

    @Test
    public void testReadRspFirstHopBySftList() {
        init();

        RenderedServicePathFirstHop firstHop = SfcProviderRenderedPathAPI.readRspFirstHopBySftList(null,
                SERVICE_FUNCTION_TYPES);
        assertNotNull("Must be not null", firstHop);
        assertNull("Symmetric Path must not be created", firstHop.getSymmetricPathId());
        LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
        assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP.get(0))));
        assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT.get(0)));
    }

    @Test
    public void testReadRspFirstHopBySftList_SymmetricRsp() {
        // Create the SF types with Bidirectionality=true, which
        // should cause the RSP to be created symmetrically
        initWithTypes(true);

        RenderedServicePathFirstHop firstHop = SfcProviderRenderedPathAPI.readRspFirstHopBySftList(null,
                SERVICE_FUNCTION_TYPES);
        assertNotNull("Must not be null", firstHop);
        assertNotNull("Symmetric Path must be created", firstHop.getSymmetricPathId());
    }

    @SuppressWarnings(value = { "serial", "static-access" })
    @Test
    public void testCreateRenderedServicePathHopList() {
        init();

        List<SfName> sfNameList = new ArrayList<SfName>() {

            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-dpi-1"));
                add(new SfName("unittest-napt-1"));
            }
        };
        final int startingIndex = 255;

        List<RenderedServicePathHop> rspHopList;

        // sfNameList and sfgNameList null
        rspHopList = SfcProviderRenderedPathAPI.createRenderedServicePathHopList(null, null, startingIndex);
        assertNull("Must be null", rspHopList);

        // usual behavior
        rspHopList = SfcProviderRenderedPathAPI.createRenderedServicePathHopList(sfNameList, null, startingIndex);
        assertEquals("Size must be equal", sfNameList.size(), rspHopList.size());
        assertEquals("SI must be equal", rspHopList.get(0).getServiceIndex().intValue(), startingIndex);
        assertEquals("SF name must be equal", rspHopList.get(0).getServiceFunctionName(), sfNameList.get(0));
        assertEquals("SI must be equal", rspHopList.get(1).getServiceIndex().intValue(), startingIndex - 1);
        assertEquals("SF name must be equal", rspHopList.get(1).getServiceFunctionName(), sfNameList.get(1));
        assertEquals("SI must be equal", rspHopList.get(2).getServiceIndex().intValue(), startingIndex - 2);
        assertEquals("SF name must be equal", rspHopList.get(2).getServiceFunctionName(), sfNameList.get(2));

        sfNameList = new ArrayList<SfName>() {

            {
                add(new SfName("unittest-fw-1"));
                add(new SfName("unittest-blabla-1"));
                add(new SfName("unittest-napt-1"));
            }
        };
        // unittest-blabla-1 SF does not exist
        rspHopList = SfcProviderRenderedPathAPI.createRenderedServicePathHopList(sfNameList, null, startingIndex);
        assertNull("Must be null", rspHopList);
    }

    @Test
    public void testCreateRenderedServicePathAndState() {
        init();

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue());

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());

        // check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI
                .readSffState(new SffName(SFF_NAMES.get(1)));
        assertNotNull("Must be not null", sffServicePathList);
        assertEquals(sffServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        // check if SF oper contains RSP
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI
                .readServiceFunctionState(new SfName("unittest-fw-1"));
        assertEquals(sfServicePathList.get(0).getName().getValue(), RSP_NAME.getValue());

        // check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI
                .readServicePathState(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName(), RSP_NAME);
    }

    @SuppressWarnings("static-access")
    @Test
    /*
     * there are null test cases of this method using partial mock
     */
    public void testCreateRenderedServicePathEntryUnsuccessful() throws Exception {
        setupSfc();

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        serviceFunctionPathBuilder.setServiceChainName(null);

        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();

        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();

        RenderedServicePath testRenderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathEntry(
                serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        // method "readServiceFunctionTypeExecutor" returns null, so there is no
        // list of SF
        assertNull("Must be null", testRenderedServicePath);
    }

    @SuppressWarnings("static-access")
    @Test
    /*
     * there are null test cases of this method
     */
    public void testCreateRenderedServicePathEntryUnsuccessful1() throws Exception {
        setupSfc();
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();
        RenderedServicePath testRenderedServicePath;

        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);

        testRenderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathEntry(
                serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        // method "createRenderedServicePathHopList", so there is no RSP hop
        // list
        assertNull("Must be null", testRenderedServicePath);
    }

    @Test
    /*
     * there are null test cases of this method
     */
    public void testCreateRenderedServicePathEntryUnsuccessful2() throws Exception {
        setupSfc();
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        createRenderedPathInputBuilder.setName(RSP_NAME.getValue());
        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);
        serviceFunctionPathBuilder.setTransportType(Mpls.class);

        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();
        RenderedServicePath testRenderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathEntry(
                serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        assertNull("Must be null", testRenderedServicePath);
    }

    @Test
    /*
     * there is successful test with all attributes correctly set
     */
    public void testCreateRenderedServicePathEntrySuccessful() throws Exception {
        init();

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        createRenderedPathInputBuilder.setName(null);
        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);
        serviceFunctionPathBuilder.setTransportType(Mac.class);
        serviceFunctionPathBuilder.setSfcEncapsulation(Transport.class);
        serviceFunctionPathBuilder.setContextMetadata("CMD");
        serviceFunctionPathBuilder.setVariableMetadata("VMD");

        serviceFunctionPathBuilder.setName(SFP_NAME);

        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();
        RenderedServicePath testRenderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathEntry(
                serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        assertNotNull("Must not be null", testRenderedServicePath);
        assertEquals("Must be equal", testRenderedServicePath.getServiceChainName(), SFC_NAME);
        assertEquals("Must be equal", testRenderedServicePath.getTransportType(), Mac.class);
        assertEquals("Must be equal", testRenderedServicePath.getSfcEncapsulation(), Transport.class);
        assertEquals("Must be equal", testRenderedServicePath.getContextMetadata(), "CMD");
        assertEquals("Must be equal", testRenderedServicePath.getVariableMetadata(), "VMD");
    }

    @Test
    /*
     * there is successful test with some attributes correctly set, and expect
     * default values for the others
     */
    public void testCreateRenderedServicePathEntrySuccessfulDefaults() throws Exception {
        init();

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        createRenderedPathInputBuilder.setName(null);
        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);
        serviceFunctionPathBuilder.setContextMetadata("CMD");
        serviceFunctionPathBuilder.setVariableMetadata("VMD");

        serviceFunctionPathBuilder.setName(SFP_NAME);

        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();
        RenderedServicePath testRenderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathEntry(
                serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        assertNotNull("Must not be null", testRenderedServicePath);
        assertEquals("Must be equal", testRenderedServicePath.getServiceChainName(), SFC_NAME);
        assertEquals("Must be equal", testRenderedServicePath.getTransportType(), VxlanGpe.class);
        assertEquals("Must be equal", testRenderedServicePath.getSfcEncapsulation(), Nsh.class);
        assertEquals("Must be equal", testRenderedServicePath.getContextMetadata(), "CMD");
        assertEquals("Must be equal", testRenderedServicePath.getVariableMetadata(), "VMD");
    }

    /*
     * Test that a Service Function with OneChainOnly set True, can only be used in one RSP
     */
    @Test
    public void testCreateRenderedServicePathAndStateOneChainSfTrue() {
        // Instead of calling the generic init(), first call
        // initSfsOneChainOnly() then call the rest of the
        // init methods like init() does internally.
        initSfsOneChainOnly(true);
        initSffs();
        initSfcs();
        initSfps();

        // Get the SFP that was already created in initSfps(), to be used to create the RSP next
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        // Create the RSP, the first one should be created successfully
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue());
        RenderedServicePath rsp1 = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());
        assertNotNull("Must be not null", rsp1);

        // Now, try creating a second RSP with the same SFP
        // This should fail, since the SFs are already used, and have OneChainOnly set true
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP2_NAME.getValue());
        RenderedServicePath rsp2 = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());
        assertNull("Must be null", rsp2);
    }

    /*
     * Test that a Service Function with OneChainOnly set False, can be used in multiple RSPs
     */
    @Test
    public void testCreateRenderedServicePathAndStateOneChainSfFalse() {
        // Instead of calling the generic init(), first call
        // initSfsOneChainOnly() then call the rest of the
        // init methods like init() does internally.
        initSfsOneChainOnly(false);
        initSffs();
        initSfcs();
        initSfps();

        // Get the SFP that was already created in initSfps(), to be used to create the RSP next
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        // Create the RSP, the first one should be created successfully
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME.getValue());
        RenderedServicePath rsp1 = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());
        assertNotNull("Must be not null", rsp1);

        // Now, try creating a second RSP with the same SFP
        // This should pass, since OneChainOnly is set false
        createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP2_NAME.getValue());
        RenderedServicePath rsp2 = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath,
                createRenderedPathInputBuilder.build());
        assertNotNull("Must be null", rsp2);
    }
}

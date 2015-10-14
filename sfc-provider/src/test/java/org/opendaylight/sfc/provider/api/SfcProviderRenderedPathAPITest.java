/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.HttpHeaderEnrichment;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Qos;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ShortestPath;
import org.powermock.reflect.Whitebox;

public class SfcProviderRenderedPathAPITest extends AbstractSfcRendererServicePathAPITest {

    @Test
    //test, whether scheduler type create right scheduler instance
    public void testGetServiceFunctionScheduler() throws Exception {
        SfcServiceFunctionSchedulerAPI rrResult, lbResult, rnResult, spResult, rsResult;

        rrResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class, "getServiceFunctionScheduler", RoundRobin.class);
        lbResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class, "getServiceFunctionScheduler", LoadBalance.class);
        rnResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class, "getServiceFunctionScheduler", Random.class);
        spResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class, "getServiceFunctionScheduler", ShortestPath.class);
        rsResult = Whitebox.invokeMethod(SfcProviderRenderedPathAPI.class, "getServiceFunctionScheduler",
                ServiceFunctionSchedulerTypeIdentity.class);

        assertEquals("Must be equal", rrResult.getClass(), SfcServiceFunctionRoundRobinSchedulerAPI.class);
        assertEquals("Must be equal", lbResult.getClass(), SfcServiceFunctionLoadBalanceSchedulerAPI.class);
        assertEquals("Must be equal", rnResult.getClass(), SfcServiceFunctionRandomSchedulerAPI.class);
        assertEquals("Must be equal", spResult.getClass(), SfcServiceFunctionShortestPathSchedulerAPI.class);
        assertEquals("Must be equal", rsResult.getClass(), SfcServiceFunctionRandomSchedulerAPI.class);
    }

    @Test
    public void testReadRenderedServicePathFirstHop() {
        super.init();

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        /* Create RenderedServicePath and reverse RenderedServicePath */
        RenderedServicePath renderedServicePath = null;
        RenderedServicePath revRenderedServicePath = null;

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME);
        createRenderedPathInputBuilder.setSymmetric(serviceFunctionPath.isSymmetric());
        try {
            renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(serviceFunctionPath, createRenderedPathInputBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull("Must be not null", renderedServicePath);

        try {
            revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        assertNotNull("Must be not null", revRenderedServicePath);

        RenderedServicePathFirstHop firstHop;
        RenderedServicePathFirstHop lastHop;
        firstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(renderedServicePath.getName());
        assertNotNull("Must be not null", firstHop);
        lastHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(revRenderedServicePath.getName());
        assertNotNull("Must be not null", lastHop);
        LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
        LOG.debug("Last hop IP: {}, port: {}", lastHop.getIp().toString(), lastHop.getPort());
        assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[0])));
        assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT[0]));
        assertEquals("Must be equal", lastHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[SFF_LOCATOR_IP.length - 1])));
        assertEquals("Must be equal", lastHop.getPort(), new PortNumber(PORT[PORT.length - 1]));
        SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
        SfcProviderRenderedPathAPI.deleteRenderedServicePath(revRenderedServicePath.getName());
    }

    @Test
    public void testReadRspFirstHopBySftList() {
        init();

        List<Class<? extends ServiceFunctionTypeIdentity>> sftList = new ArrayList<>();
        sftList.add(Firewall.class);
        sftList.add(Dpi.class);
        sftList.add(Napt44.class);
        sftList.add(HttpHeaderEnrichment.class);
        sftList.add(Qos.class);
        assertEquals("sftList size should be 5", sftList.size(), 5);
        RenderedServicePathFirstHop firstHop = null;
        try {
            firstHop = SfcProviderRenderedPathAPI.readRspFirstHopBySftList(null, sftList);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        assertNotNull("Must be not null", firstHop);
        LOG.debug("First hop IP: {}, port: {}", firstHop.getIp().toString(), firstHop.getPort());
        assertEquals("Must be equal", firstHop.getIp(), new IpAddress(new Ipv4Address(SFF_LOCATOR_IP[0])));
        assertEquals("Must be equal", firstHop.getPort(), new PortNumber(PORT[0]));
    }

    @Test
    public void testCreateRenderedServicePathHopList() {
        init();

        final String[] tmpSfNames = {"unittest-fw-1", "unittest-dpi-1", "unittest-napt-1"};
        List<String> sfNameList = Arrays.asList(tmpSfNames);
        final int startingIndex = 255;

        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI();

        List<RenderedServicePathHop> rspHopList;

        //sfNameList and sfgNameList null
        rspHopList = sfcProviderRenderedPathAPI.createRenderedServicePathHopList(null, null, startingIndex);
        assertNull("Must be null", rspHopList);

        //usual behaviour
        rspHopList = sfcProviderRenderedPathAPI.createRenderedServicePathHopList(sfNameList, null, startingIndex);
        assertEquals("Size must be equal", sfNameList.size(), rspHopList.size());
        assertEquals("SI must be equal", rspHopList.get(0).getServiceIndex().intValue(), startingIndex);
        assertEquals("SF name must be equal", rspHopList.get(0).getServiceFunctionName(), sfNameList.get(0));
        assertEquals("SI must be equal", rspHopList.get(1).getServiceIndex().intValue(), startingIndex - 1);
        assertEquals("SF name must be equal", rspHopList.get(1).getServiceFunctionName(), sfNameList.get(1));
        assertEquals("SI must be equal", rspHopList.get(2).getServiceIndex().intValue(), startingIndex - 2);
        assertEquals("SF name must be equal", rspHopList.get(2).getServiceFunctionName(), sfNameList.get(2));

        final String[] sfNamesNotExisting = {"unittest-fw-1", "unittest-blabla-1", "unittest-napt-1"};
        sfNameList = Arrays.asList(sfNamesNotExisting);
        //unittest-blabla-1 SF does not exist
        rspHopList = sfcProviderRenderedPathAPI.createRenderedServicePathHopList(sfNameList, null, startingIndex);
        assertNull("Must be null", rspHopList);
    }

    @Test
    public void testCreateRenderedServicePathAndState() {
        init();

        ServiceFunctionPath serviceFunctionPath =
                SfcProviderServicePathAPI.readServiceFunctionPath(SFP_NAME);
        assertNotNull("Must be not null", serviceFunctionPath);

        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        createRenderedPathInputBuilder.setName(RSP_NAME);

        SfcProviderRenderedPathAPI.createRenderedServicePathAndState(
                serviceFunctionPath, createRenderedPathInputBuilder.build());

        //check if SFF oper contains RSP
        List<SffServicePath> sffServicePathList = SfcProviderServiceForwarderAPI.readSffState(SFF_NAMES[1]);
        assertNotNull("Must be not null", sffServicePathList);
        assertEquals(sffServicePathList.get(0).getName(), RSP_NAME);

        //check if SF oper contains RSP
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState("unittest-fw-1");
        assertEquals(sfServicePathList.get(0).getName(), RSP_NAME);

        //check if SFP oper contains RSP
        List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI.readServicePathState(SFP_NAME);
        assertEquals(sfpRenderedServicePathList.get(0).getName(), RSP_NAME);
    }

    @Test
    /*
     * there are null test cases of this method using partial mock
     */
    public void testCreateRenderedServicePathEntryUnsuccessful() throws Exception {
        Object[] params = new Object[0];
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI();
        RenderedServicePath testRenderedServicePath;

        serviceFunctionPathBuilder.setServiceChainName(null);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build());

        //SFC does not exist, should be null
        assertNull("Must be null", testRenderedServicePath);

        serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();

        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);

        //PowerMockito.stub(PowerMockito.method(SfcProviderServiceTypeAPI.class, "readServiceFunctionTypeExecutor")).toReturn(null);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        //method "readServiceFunctionTypeExecutor" returns null, so there is no list of SF
        assertNull("Must be null", testRenderedServicePath);

    }

    @Test
     /*
     * there are null test cases of this method
     */
    public void testCreateRenderedServicePathEntryUnsuccessful1() throws Exception {
        Object[] params = new Object[0];
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI();
        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();
        RenderedServicePath testRenderedServicePath;

        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        //method "createRenderedServicePathHopList", so there is no RSP hop list
        assertNull("Must be null", testRenderedServicePath);
    }

    @Test
    /*
     * there are null test cases of this method
     */
    public void testCreateRenderedServicePathEntryUnsuccessful2() throws Exception {
        Object[] params = new Object[0];
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI();
        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();
        RenderedServicePath testRenderedServicePath;

        createRenderedPathInputBuilder.setName(RSP_NAME);
        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);
        serviceFunctionPathBuilder.setTransportType(Mpls.class);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        assertNull("Must be null", testRenderedServicePath);
    }

    @Test
    /*
     * there is successful test with all attributes correctly set
     */
    public void testCreateRenderedServicePathEntrySuccessful() throws Exception {
        init();

        Object[] params = new Object[0];
        CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        SfcProviderRenderedPathAPI sfcProviderRenderedPathAPI = new SfcProviderRenderedPathAPI();
        SfcServiceFunctionSchedulerAPI testScheduler = new SfcServiceFunctionRandomSchedulerAPI();
        RenderedServicePath testRenderedServicePath;

        createRenderedPathInputBuilder.setName(null);
        serviceFunctionPathBuilder.setServiceChainName(SFC_NAME);
        serviceFunctionPathBuilder.setTransportType(null);

        testRenderedServicePath = sfcProviderRenderedPathAPI
                .createRenderedServicePathEntry(serviceFunctionPathBuilder.build(), createRenderedPathInputBuilder.build(), testScheduler);

        assertNotNull("Must not be null", testRenderedServicePath);
        assertEquals("Must be equal", testRenderedServicePath.getServiceChainName(), SFC_NAME);
        assertEquals("Must be equal", testRenderedServicePath.getTransportType(), VxlanGpe.class);
    }
}

/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.logicalclassifier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.RspLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcGeniusDataUtils.class})
public class LogicalClassifierDataGetterTest {

    @Mock
    SfcGeniusRpcClient geniusRpc;

    @Mock
    RenderedServicePath rsp;

    @Mock
    List<RenderedServicePathHop> theHops;

    @Mock
    RenderedServicePathHop hop;

    @Mock
    RspLogicalSffAugmentation logicalSffAugmentation;

    private final LogicalClassifierDataGetter dataGetter;

    private static final String FIRST_SF_NEUTRON_PORT = "eba7a40e-a8f7-4b25-b1a2-4128f0da988e";
    private static final String FIRST_SF_DPN_ID = "1234567890";
    private static final String FIRST_SF_NODE_NAME = "openflow:" + FIRST_SF_DPN_ID;
    private static final String FIRST_SF_TUNNEL_IF = "tunaa-1234";
    private static final DpnIdType DATAPLANE_ID = new DpnIdType(new BigInteger(FIRST_SF_DPN_ID));

    public LogicalClassifierDataGetterTest() {
        initMocks(this);
        dataGetter = new LogicalClassifierDataGetter(geniusRpc);
    }

    @Before
    public void setUp() {
        when(rsp.getRenderedServicePathHop()).thenReturn(theHops);

        when(theHops.get(any(Integer.class))).thenReturn(hop);

        when(hop.getAugmentation(eq(RspLogicalSffAugmentation.class))).thenReturn(logicalSffAugmentation);

        when(logicalSffAugmentation.getDpnId()).thenReturn(DATAPLANE_ID);

        PowerMockito.mockStatic(SfcGeniusDataUtils.class);
        List<String> theInterfaces = new ArrayList<String>() {{
            add("openflow:1234567890:2");
        }} ;
        PowerMockito.when(SfcGeniusDataUtils.getInterfaceLowerLayerIf(anyString())).thenReturn(theInterfaces);

        when(geniusRpc.getDpnIdFromInterfaceNameFromGeniusRPC(anyString()))
                .thenReturn(Optional.of(new DpnIdType(new BigInteger(FIRST_SF_DPN_ID))));

        when(geniusRpc.getEgressActionsFromGeniusRPC(anyString(), any(Boolean.class), any(Integer.class)))
                .thenReturn(Optional.of(new ArrayList<Action>() {{ add(new ActionBuilder().build()) ;}}));

        when(geniusRpc.getTargetInterfaceFromGeniusRPC(any(DpnIdType.class), any(DpnIdType.class)))
                .thenReturn(Optional.of(FIRST_SF_NEUTRON_PORT));
    }

    @Test
    public void testFetchingFirstHopDataplaneId() {
        Assert.assertEquals(
                FIRST_SF_DPN_ID,
                dataGetter.getFirstHopDataplaneId(rsp).get().getValue().toString());
    }

    @Test
    public void testFetchingFirstHopDataplaneIdInvalidAugmentation() {
        when(hop.getAugmentation(any())).thenReturn(null);
        Assert.assertFalse(dataGetter.getFirstHopDataplaneId(rsp).isPresent());
    }

    @Test
    public void getTunnelInterfaceNamePositive() {
        DpnIdType srcDpn = new DpnIdType(new BigInteger(FIRST_SF_DPN_ID));
        DpnIdType dstDpn = new DpnIdType(new BigInteger("9876543210"));
        Assert.assertEquals(FIRST_SF_NEUTRON_PORT, dataGetter.getInterfaceBetweenDpnIds(srcDpn, dstDpn).get());
    }

    @Test
    public void getTunnelInterfaceNameNegative() {
        DpnIdType srcDpn = new DpnIdType(new BigInteger(FIRST_SF_DPN_ID));
        DpnIdType dstDpn = new DpnIdType(new BigInteger("9876543210"));
        when(geniusRpc.getTargetInterfaceFromGeniusRPC(any(DpnIdType.class), any(DpnIdType.class)))
                .thenReturn(Optional.empty());
        Assert.assertFalse(dataGetter.getInterfaceBetweenDpnIds(srcDpn, dstDpn).isPresent());
    }

    @Test
    public void getEgressActionsForTunnelPositive() {
        Assert.assertNotNull(dataGetter.getEgressActionsForTunnelInterface(FIRST_SF_TUNNEL_IF, 0));
        Assert.assertFalse(dataGetter.getEgressActionsForTunnelInterface(FIRST_SF_TUNNEL_IF, 0).isEmpty());
    }

    @Test
    public void getEgressActionsForTunnelNegative() {
        when(geniusRpc.getEgressActionsFromGeniusRPC(anyString(), any(Boolean.class), any(Integer.class)))
                .thenReturn(Optional.empty());
        Assert.assertNotNull(dataGetter.getEgressActionsForTunnelInterface(FIRST_SF_TUNNEL_IF, 0));
        Assert.assertTrue(dataGetter.getEgressActionsForTunnelInterface(FIRST_SF_TUNNEL_IF, 0).isEmpty());
    }

    @Test
    public void getNodeNamePositive() {
        Optional<String> theNodeName = dataGetter.getNodeName(FIRST_SF_NEUTRON_PORT);
        Assert.assertTrue(theNodeName.isPresent());
        Assert.assertEquals(FIRST_SF_NODE_NAME, theNodeName.get());
    }

    @Test
    public void getNodeNameNegative () {
        when(geniusRpc.getDpnIdFromInterfaceNameFromGeniusRPC(anyString())).thenReturn(Optional.empty());
        Optional<String> theNodeName = dataGetter.getNodeName(FIRST_SF_NEUTRON_PORT);
        Assert.assertFalse(theNodeName.isPresent());
    }

    @Test
    public void getDpnIdFromNodeNamePositive() {
        DpnIdType theDpn = LogicalClassifierDataGetter.getDpnIdFromNodeName(FIRST_SF_NODE_NAME);
        Assert.assertEquals(new DpnIdType(new BigInteger(FIRST_SF_DPN_ID)), theDpn);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDpnIdFromNodeNameNegative() {
        LogicalClassifierDataGetter.getDpnIdFromNodeName("openflow:blabla:321");
    }

    @Test
    public void getOpenflowPortPositive() {
        Assert.assertEquals(new Long(2), LogicalClassifierDataGetter.getOpenflowPort(FIRST_SF_NEUTRON_PORT).get());
    }

    @Test
    public void getOpenflowPortNegative() {
        List<String> theInterfaces = new ArrayList<String>() {{
            add("openflow:1234567890");
        }} ;
        PowerMockito.when(SfcGeniusDataUtils.getInterfaceLowerLayerIf(anyString())).thenReturn(theInterfaces);
        Assert.assertFalse(LogicalClassifierDataGetter.getOpenflowPort(FIRST_SF_NEUTRON_PORT).isPresent());
    }
}

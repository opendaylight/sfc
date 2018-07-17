/*
 * Copyright (c) 2015, 2017 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ovs.provider;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.create.ovs.bridge.input.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test suite for RPCs calls.
 *
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsUtil
 * @since 2015-04-27
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({SfcOvsUtil.class, SfcDataStoreAPI.class})
public class SfcOvsRpcTest {

    private static final String IP_ADDRESS = "170.0.0.1";
    private static final String TEST_NAME = "TEST_NAME";
    private static final Integer PORT_NUMBER = 8080;
    private CreateOvsBridgeInputBuilder createOvsBridgeInputBuilder;
    private Future<RpcResult<CreateOvsBridgeOutput>> futureResult;
    private NodeBuilder nodeBuilder;
    private OvsNodeBuilder ovsNodeBuilder;
    private SfcOvsRpc sfcOvsRpcObject;

    @Before
    public void init() {
    }

    @Test
    public void testCreateOvsBridgeNullNode() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        // create "empty" node
        nodeBuilder = new NodeBuilder();

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS)));
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build());
        createOvsBridgeInputBuilder.setName(TEST_NAME);

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull("Must not be null", futureResult);
        assertNull("Result must be null", futureResult.get().getResult());
        assertFalse("Must be false", futureResult.get().isSuccessful());
    }

    @Test
    public void testCreateOvsBridgeNullNodeId() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        // create "empty" node
        nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId("NodeId"));

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS)));
        createOvsBridgeInputBuilder.setName(TEST_NAME).setOvsNode(ovsNodeBuilder.build());

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS)));
        createOvsBridgeInputBuilder.setName(TEST_NAME).setOvsNode(ovsNodeBuilder.build());

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getManagerNodeByIp")).toReturn(nodeBuilder.build());

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull("Must not be null", futureResult);
        assertNull("Result must be null", futureResult.get().getResult());
        assertFalse("Must be false", futureResult.get().isSuccessful());
    }

    @Test
    public void testCreateOvsBridgeFalseResult() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS))).setPort(new PortNumber(PORT_NUMBER));
        createOvsBridgeInputBuilder.setName(TEST_NAME).setOvsNode(ovsNodeBuilder.build());

        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS))).setPort(new PortNumber(PORT_NUMBER));
        createOvsBridgeInputBuilder.setName(TEST_NAME).setOvsNode(ovsNodeBuilder.build());

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull("Must not be null", futureResult);
        assertNull("Result must be null", futureResult.get().getResult());
        assertFalse("Must be false", futureResult.get().isSuccessful());
    }

    @Test
    public void testCreateOvsBridgeTrueResult() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        // set node ip & port
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(IP_ADDRESS))).setPort(new PortNumber(PORT_NUMBER));
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build())
                .setName(TEST_NAME);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "writeMergeTransactionAPI"))
                .toReturn(true);

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull(futureResult);

        RpcResult<CreateOvsBridgeOutput> result = futureResult.get();
        assertNotNull(result);
        assertTrue(result.isSuccessful());

        assertNotNull(result.getResult());
        assertNotNull(result.getResult().isResult());
        assertTrue(result.getResult().isResult());
    }
}

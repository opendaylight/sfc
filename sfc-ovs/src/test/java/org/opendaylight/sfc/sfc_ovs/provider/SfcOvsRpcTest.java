/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcFixedThreadPoolWrapper;
import org.opendaylight.sfc.provider.SfcProviderUtils;
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
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsUtil
 * @since 2015-04-27
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOvsUtil.class)
public class SfcOvsRpcTest extends AbstractDataBrokerTest {

    private static final String ipAddressString = "170.0.0.1";
    private static final String testName = "testName";
    private static final Integer portNumber = 8080;
    private CreateOvsBridgeInputBuilder createOvsBridgeInputBuilder;
    private Future<RpcResult<CreateOvsBridgeOutput>> futureResult;
    private NodeBuilder nodeBuilder;
    private OvsNodeBuilder ovsNodeBuilder;
    private SfcOvsRpc sfcOvsRpcObject;

    private DataBroker dataBroker;
    private static final SfcFixedThreadPoolWrapper sfcFixedThreadPoolObj =
            new SfcFixedThreadPoolWrapper(SfcProviderUtils.EXECUTOR_THREAD_POOL_SIZE, SfcProviderUtils.THREAD_FACTORY_IS_DAEMON,
                    SfcProviderUtils.THREAD_FACTORY_NAME_FORMAT);
    private static ExecutorService executor = sfcFixedThreadPoolObj.getExecutor();
    private OpendaylightSfc odlSfc;

    @Before
    public void init() {
        dataBroker = getDataBroker();
        odlSfc = new OpendaylightSfc(dataBroker);
    }

    @After
    public void after() throws ExecutionException, InterruptedException {
        odlSfc.close();
        sfcFixedThreadPoolObj.close();
    }

    @Test
    public void testCreateOvsBridgeNullNode() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc((ThreadPool) executor);

        // create "empty" node
        nodeBuilder = new NodeBuilder();

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(ipAddressString)));
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build());

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull("Must not be null", futureResult);
        assertNull("Result must be null", futureResult.get().getResult());
        assertFalse("Must be false", futureResult.get().isSuccessful());
    }

    @Test
    public void testCreateOvsBridgeNullNodeId() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc((ThreadPool) executor);

        // create "empty" node
        nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId("NodeId"));

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(ipAddressString)));
        createOvsBridgeInputBuilder.setName(testName).setOvsNode(ovsNodeBuilder.build());

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(ipAddressString)));
        createOvsBridgeInputBuilder.setName(testName).setOvsNode(ovsNodeBuilder.build());

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "getManagerNodeByIp")).toReturn(nodeBuilder.build());
        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable")).toReturn(false);

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull("Must not be null", futureResult);
        assertNull("Result must be null", futureResult.get().getResult());
        assertFalse("Must be false", futureResult.get().isSuccessful());
    }

    @Test
    public void testCreateOvsBridgeFalseResult() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc((ThreadPool) executor);

        // set node ip
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(ipAddressString))).setPort(new PortNumber(portNumber));
        createOvsBridgeInputBuilder.setName(testName).setOvsNode(ovsNodeBuilder.build());

        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(ipAddressString))).setPort(new PortNumber(portNumber));
        createOvsBridgeInputBuilder.setName(testName).setOvsNode(ovsNodeBuilder.build());

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable")).toReturn(false);

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull("Must not be null", futureResult);
        assertNull("Result must be null", futureResult.get().getResult());
        assertFalse("Must be false", futureResult.get().isSuccessful());
    }

    @Test
    public void testCreateOvsBridgeTrueResult() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc((ThreadPool) executor);

        // set node ip & port
        ovsNodeBuilder.setIp(new IpAddress(new Ipv4Address(ipAddressString))).setPort(new PortNumber(portNumber));
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build()).setName(testName);

        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build()).setName(testName);

        PowerMockito.stub(PowerMockito.method(SfcOvsUtil.class, "submitCallable")).toReturn(true);

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        assertNotNull("Must not be null", futureResult);
        assertTrue("Result must be null", futureResult.get().getResult().isResult());
        assertTrue("Must be true", futureResult.get().isSuccessful());
    }
}

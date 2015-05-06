package org.opendaylight.sfc.sfc_ovs.provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.create.ovs.bridge.input.OvsNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfcOvsUtil
 * <p/>
 * @since 2015-04-27
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOvsUtil.class)
public class SfcOvsRpcTest {

    private static final String ipAddressString = "170.0.0.1";
    private static final String testString = "testName";
    private static final Integer portNumber = 8080;
    private CreateOvsBridgeInputBuilder createOvsBridgeInputBuilder;
    private Future<RpcResult<CreateOvsBridgeOutput>> futureResult;
    private IpAddress ipAddress;
    private OvsNodeBuilder ovsNodeBuilder;
    private SfcOvsRpc sfcOvsRpcObject;

    @Before
    public void init() {
        DataBroker dataBroker = null;
        OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
        opendaylightSfc.setDataProvider(dataBroker);
    }

    @Test
    public void createOvsBridgeTestWhereInputIsNull() {
        sfcOvsRpcObject = new SfcOvsRpc();

        //OvsBridgeInput is null
        try {
            sfcOvsRpcObject.createOvsBridge(null);
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void createOvsBridgeTestWhereOvsNodeIsNull() {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        //OvsNode is null
        try {
            sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());
        } catch (NullPointerException exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void createOvsBridgeTestWhereIpAndPortAreNull() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build());

        //Ip & Port are null
        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        Assert.assertEquals(futureResult.get().isSuccessful(), false);
    }

    @Test
    public void createOvsBridgeTestWherePortAndIdAreNull() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        ipAddress = new IpAddress(new Ipv4Address(ipAddressString));
        ovsNodeBuilder.setIp(ipAddress);
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build());
        createOvsBridgeInputBuilder.setName(testString);

        PowerMockito.mockStatic(SfcOvsUtil.class);
        Mockito.when(SfcOvsUtil.submitCallable(Mockito.any(Callable.class), Mockito.any(ExecutorService.class))).thenReturn(nullId());

        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        //Port is null & nodeID is null
        Assert.assertEquals(futureResult.get().isSuccessful(), false);
    }

    @Test
    public void createOvsBridgeTestWherePortIsNull() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        ipAddress = new IpAddress(new Ipv4Address(ipAddressString));
        ovsNodeBuilder.setIp(ipAddress);
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build());
        createOvsBridgeInputBuilder.setName(testString);

        PowerMockito.mockStatic(SfcOvsUtil.class);
        Mockito.when(SfcOvsUtil.submitCallable(Mockito.any(Callable.class), Mockito.any(ExecutorService.class))).thenReturn(buildNodeId());

        //Port is null
        try {
            futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());
        } catch (Exception exception) {
            Assert.assertEquals(NullPointerException.class, exception.getClass());
        }
    }

    @Test
    public void createOvsBridgeUnsuccessfulTest() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        ipAddress = new IpAddress(new Ipv4Address(ipAddressString));
        ovsNodeBuilder.setIp(ipAddress);
        ovsNodeBuilder.setPort(new PortNumber(portNumber));
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build());
        createOvsBridgeInputBuilder.setName(testString);
        PowerMockito.mockStatic(SfcOvsUtil.class);
        Mockito.when(SfcOvsUtil.buildOvsdbNodeIID(Mockito.any(NodeId.class))).thenReturn(buildInstanceIdentifierNode());
        Mockito.when(SfcOvsUtil.submitCallable(Mockito.any(Callable.class), Mockito.any(ExecutorService.class))).thenReturn(false);
        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        //Unsuccessful test
        Assert.assertEquals(futureResult.get().isSuccessful(), false);
    }

    @Test
    public void createOvsBridgeSuccessfulTest() throws Exception {
        createOvsBridgeInputBuilder = new CreateOvsBridgeInputBuilder();
        ovsNodeBuilder = new OvsNodeBuilder();
        sfcOvsRpcObject = new SfcOvsRpc();

        ipAddress = new IpAddress(new Ipv4Address(ipAddressString));
        ovsNodeBuilder.setIp(ipAddress);
        ovsNodeBuilder.setPort(new PortNumber(portNumber));
        createOvsBridgeInputBuilder.setOvsNode(ovsNodeBuilder.build());
        createOvsBridgeInputBuilder.setName(testString);
        PowerMockito.mockStatic(SfcOvsUtil.class);
        Mockito.when(SfcOvsUtil.buildOvsdbNodeIID(Mockito.any(NodeId.class))).thenReturn(buildInstanceIdentifierNode());
        Mockito.when(SfcOvsUtil.submitCallable(Mockito.any(Callable.class), Mockito.any(ExecutorService.class))).thenReturn(true);
        futureResult = sfcOvsRpcObject.createOvsBridge(createOvsBridgeInputBuilder.build());

        //Successful test
        Assert.assertEquals(futureResult.get().isSuccessful(), true);
    }

    private Node buildNodeId() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId("1111"));
        return nodeBuilder.build();
    }

    private Node nullId() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(null);
        return nodeBuilder.build();
    }

    private InstanceIdentifier<Node> buildInstanceIdentifierNode() {
        NodeId nodeId = null;
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId));
    }
}
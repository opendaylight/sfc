/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Class for handling SFC OVS RPCs
 * <p/>
 * <p/>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-03-31
 */

package org.opendaylight.sfc.sfc_ovs.provider;


import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarderOvsService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.create.ovs.bridge.input.OvsNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsRpc implements ServiceFunctionForwarderOvsService {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsRpc.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private DataBroker dataBroker = odlSfc.getDataProvider();

    private static final String OVSDB_NODE_PREFIX = "ovsdb://";

    /**
     * This method writes a new OVS Bridge into OVSDB Config DataStore. This write event triggers
     * creation of the OVS Bridge in running OpenVSwitch instance identified by OVS Node ip:port
     * locator.
     * <p>
     * @param input RPC input including a OVS Bridge name and parent OVS Node ip:port locator
     * @return RPC output: true if write to OVSDB Config DataStore was successful, otherwise false.
     */
    @Override
    public Future<RpcResult<CreateOvsBridgeOutput>> createOvsBridge(CreateOvsBridgeInput input) {
        Preconditions.checkNotNull(input, "create-ovs-bridge RPC input must be not null!");
        Preconditions.checkNotNull(input.getOvsNode(), "create-ovs-bridge RPC input container ovs-node must be not null!");

        RpcResultBuilder<CreateOvsBridgeOutput> rpcResultBuilder;

        //create parent OVS Node InstanceIdentifier (based on ip:port locator)
        OvsNode ovsNode = input.getOvsNode();
        String nodeId = OVSDB_NODE_PREFIX + ovsNode.getIp().getIpv4Address().getValue() + ":" + ovsNode.getPort().getValue();
        InstanceIdentifier<Node> nodeIID = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(nodeId)));

        //build OVS Bridge
        //TODO: seperate into function as it will grow in future (including DP locators, etc.)
        OvsdbBridgeAugmentationBuilder ovsdbBridgeBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeBuilder.setBridgeName(new OvsdbBridgeName(input.getName()));
        ovsdbBridgeBuilder.setManagedBy(new OvsdbNodeRef(nodeIID));

        if (SfcSffToOvsMappingAPI.putOvsdbBridgeAugmentation(ovsdbBridgeBuilder.build())){
            rpcResultBuilder = RpcResultBuilder.success(new CreateOvsBridgeOutputBuilder().setResult(true).build());
        } else {
            String message = "Error writing OVS Bridge into OVSDB Configuration DataStore: " + input.getName();
            rpcResultBuilder = RpcResultBuilder.<CreateOvsBridgeOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}

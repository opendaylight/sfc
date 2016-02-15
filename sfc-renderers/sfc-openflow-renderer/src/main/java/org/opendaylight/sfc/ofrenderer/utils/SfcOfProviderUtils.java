/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class SfcOfProviderUtils extends SfcOfBaseProviderUtils {

    // Since this class can be called by multiple threads,
    // store these objects per RSP id to avoid collisions
    private class RspContext {

        // store the SFs and SFFs internally so we dont have to
        // query the DataStore repeatedly for the same thing
        private Map<SfName, ServiceFunction> serviceFunctions;
        private Map<String, ServiceFunctionGroup> serviceFunctionGroups;
        private Map<SffName, ServiceFunctionForwarder> serviceFunctionFowarders;

        public RspContext() {
            serviceFunctions = Collections.synchronizedMap(new HashMap<SfName, ServiceFunction>());
            serviceFunctionGroups = Collections.synchronizedMap(new HashMap<String, ServiceFunctionGroup>());
            serviceFunctionFowarders = Collections.synchronizedMap(new HashMap<SffName, ServiceFunctionForwarder>());
        }
    }

    private Map<Long, RspContext> rspIdToContext;

    public SfcOfProviderUtils() {
        rspIdToContext = new HashMap<Long, RspContext>();
    }

    @Override
    public void addRsp(long rspId) {
        rspIdToContext.put(rspId, new RspContext());
    }

    @Override
    public void removeRsp(long rspId) {
        rspIdToContext.remove(rspId);
    }

    /**
     * Return the named ServiceFunction
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     *
     * @param sfName - The SF Name to search for
     * @return - The ServiceFunction object, or null if not found
     */
    @Override
    public ServiceFunction getServiceFunction(final SfName sfName, long rspId) {
        RspContext rspContext = rspIdToContext.get(rspId);

        ServiceFunction sf = rspContext.serviceFunctions.get(sfName);
        if (sf == null) {
            sf = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
            if (sf != null) {
                rspContext.serviceFunctions.put(sfName, sf);
            }
        }

        return sf;
    }

    /**
     * Return the named ServiceFunctionForwarder
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     *
     * @param sffName - The SFF Name to search for
     * @return The ServiceFunctionForwarder object, or null if not found
     */
    @Override
    public ServiceFunctionForwarder getServiceFunctionForwarder(final SffName sffName, long rspId) {
        RspContext rspContext = rspIdToContext.get(rspId);

        ServiceFunctionForwarder sff = rspContext.serviceFunctionFowarders.get(sffName);
        if (sff == null) {
            sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (sff != null) {
                sff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(sff);
                rspContext.serviceFunctionFowarders.put(sffName, sff);
            }
        }

        return sff;
    }

    @Override
    public ServiceFunctionGroup getServiceFunctionGroup(final String sfgName, long rspId) {
        RspContext rspContext = rspIdToContext.get(rspId);

        ServiceFunctionGroup sfg = rspContext.serviceFunctionGroups.get(sfgName);
        if (sfg == null) {
            sfg = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroup(sfgName);
            if (sfg != null) {
                rspContext.serviceFunctionGroups.put(sfgName, sfg);
            }
        }

        return sfg;
    }

    /**
     * Currently the switch port the SF is connected to is stored by Tacker.
     * Here we take this name and convert it to the port number.
     */
    @Override
    public Long getPortNumberFromName(final String sffName, final String portName, long rspId) {

        ServiceFunctionForwarder sff = getServiceFunctionForwarder(new SffName(sffName), rspId);
        SffOvsBridgeAugmentation sffOvsBridge = sff.getAugmentation(SffOvsBridgeAugmentation.class);
        if(sffOvsBridge == null || sffOvsBridge.getOvsBridge() == null || sffOvsBridge.getOvsBridge().getBridgeName() == null) {
            throw new RuntimeException("getPortNumberFromName: SFF [" + sffName + "] does not have the expected SffOvsBridgeAugmentation.");
        }
        Node node = SfcOvsUtil.lookupTopologyNode(sff, OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (node == null || node.getAugmentation(OvsdbNodeAugmentation.class) == null) {
            throw new IllegalStateException("OVSDB node does not exist for SFF " + sffName);
        }

        Long ofPort = 0L;
        TerminationPoint tp = readTerminationPoint(node, sffOvsBridge.getOvsBridge().getBridgeName(), portName);
        if (tp != null) {
            OvsdbTerminationPointAugmentation port = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (port != null) {
                ofPort = getOFPort(port);
            }
        }
        return ofPort;
    }

    // internal support method for getPortNumberFromName()
    private Long getOFPort(OvsdbTerminationPointAugmentation port) {
        Long ofPort = 0L;
        if (port.getOfport() != null) {
            ofPort = port.getOfport();
        }
        return ofPort;
    }

    // internal support method for getPortNumberFromName() and related methods
    private OvsdbTerminationPointAugmentation extractTerminationPointAugmentation(Node bridgeNode, String portName) {
        if (bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class) != null) {
            List<OvsdbTerminationPointAugmentation> tpAugmentations = extractTerminationPointAugmentations(bridgeNode);
            for (OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation : tpAugmentations) {
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    return ovsdbTerminationPointAugmentation;
                }
            }
        }
        return null;
    }

    // internal support method for getPortNumberFromName() and related methods
    public List<OvsdbTerminationPointAugmentation> extractTerminationPointAugmentations( Node node ) {
        List<OvsdbTerminationPointAugmentation> tpAugmentations = new ArrayList<>();
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        if(terminationPoints != null && !terminationPoints.isEmpty()){
            for(TerminationPoint tp : terminationPoints){
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation != null) {
                    tpAugmentations.add(ovsdbTerminationPointAugmentation);
                }
            }
        }
        return tpAugmentations;
    }

    // internal support method for getPortNumberFromName() and related methods
    private TerminationPoint readTerminationPoint(Node ovsdbNode, String bridgeName, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
                InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(new Uri("ovsdb:1"))))
                .child(Node.class, new NodeKey(new NodeId(ovsdbNode.getKey().getNodeId().getValue() + "/bridge/" + bridgeName)))
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        return SfcDataStoreAPI.readTransactionAPI(tpIid, LogicalDatastoreType.OPERATIONAL);
    }

}

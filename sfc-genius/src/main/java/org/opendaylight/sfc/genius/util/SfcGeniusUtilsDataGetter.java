/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import java.util.Collections;
import java.util.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

import java.math.BigInteger;
import java.util.List;

class SfcGeniusUtilsDataGetter {

    private SfcGeniusUtilsDataGetter() {}

    /**
     *  Read the Interface object from the Config DS
     *
     * @param ifName the name of the neutron interface
     * @return the Interface object from the config DS
     */
    private static Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface>
    getServiceFunctionAttachedInterface(String ifName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey ifKey =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey(ifName);

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface>
            ifConfigIID = InstanceIdentifier.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class, ifKey);

        return Optional.ofNullable(SfcDataStoreAPI.readTransactionAPI(ifConfigIID, LogicalDatastoreType.CONFIGURATION));
    }

    /**
     *  Read the name of the LogicalInterface connected to a neutron port
     *
     * @param ifName the name of the neutron port
     * @return the LogicalInterface name
     */
    public static Optional<String> getServiceFunctionAttachedInterfaceName(String ifName) {
        return getServiceFunctionAttachedInterface(ifName)
                .map(theIf -> theIf.getAugmentation(ParentRefs.class))
                .map(ParentRefs::getParentInterface);
    }

    /**
     * Get the InterfaceState for that neutron port
     *
     * @param ifName    the name of the neutron port
     * @return          the InterfaceState object, read from the OPERATIONAL DS
     */
    public static Optional<Interface> getServiceFunctionAttachedInterfaceState(String ifName) {
        InstanceIdentifier<Interface> targetInterfaceIID =
                InstanceIdentifier.create(InterfacesState.class)
                        .child(Interface.class, new InterfaceKey(ifName));
        return Optional.ofNullable(
                SfcDataStoreAPI.readTransactionAPI(targetInterfaceIID, LogicalDatastoreType.OPERATIONAL));
    }

    /**
     * Fetch the BridgeRef object from GENIUS OPERATIONAL DS, when given its DpnId
     *
     * @param theDpnId  the dataplane ID for that bridge
     * @return          the BridgeRef object
     */
    public static Optional<BridgeRefEntry> getBridgeFromDpnId(BigInteger theDpnId) {
        InstanceIdentifier<BridgeRefEntry> theBridgeRefIID =
            InstanceIdentifier.create(BridgeRefInfo.class)
                .child(BridgeRefEntry.class, new BridgeRefEntryKey(theDpnId));
        return Optional.ofNullable(SfcDataStoreAPI.readTransactionAPI(theBridgeRefIID, LogicalDatastoreType.OPERATIONAL));
    }

    /**
     * Read the InterfaceExternalIds of a particular ovsDB node, and TerminationPoint
     *
     * @param ovsdbNodeId   the id of the ovsDB node
     * @param portName      the name of the TerminationPoint
     * @return              the list of externalIds of that particular TerminationPoint & ovsDB node
     */
    public static List<InterfaceExternalIds> readOvsNodeInterfaces(String ovsdbNodeId, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
            InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SfcGeniusConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(ovsdbNodeId)))
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
        return Optional.ofNullable(SfcDataStoreAPI.readTransactionAPI(tpIid, LogicalDatastoreType.OPERATIONAL))
                .map(theTp -> theTp.getAugmentation(OvsdbTerminationPointAugmentation.class))
                .map(OvsdbTerminationPointAugmentation::getInterfaceExternalIds )
                .orElse(Collections.emptyList());
    }
}

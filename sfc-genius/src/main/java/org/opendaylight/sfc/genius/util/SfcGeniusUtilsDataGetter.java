/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class SfcGeniusUtilsDataGetter {
    private SfcGeniusUtilsDataGetter() {};

/**
 *  Read the Interface object from the Config DS
 *
 * @param ifName the name of the neutron interface
 * @return the Interface object from the config DS
 * */
    private static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
    getServiceFunctionAttachedInterface(String ifName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey ifKey =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey(ifName);

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface>
            ifConfigIID = InstanceIdentifier.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class, ifKey);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface theIf =
            SfcDataStoreAPI.readTransactionAPI(ifConfigIID, LogicalDatastoreType.CONFIGURATION);
        return theIf;
    }

    public static String getServiceFunctionAttachedInterfaceName(String ifName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface theIf =
            getServiceFunctionAttachedInterface(ifName);

        if (theIf == null)
            return null;

        String theLogicalInterfaceName;
        ParentRefs parentRefs = theIf.getAugmentation(ParentRefs.class);
        if (parentRefs != null) {
            theLogicalInterfaceName = parentRefs.getParentInterface();
        } else {
            return null;
        }

        return theLogicalInterfaceName;
    }

    public static Interface getServiceFunctionAttachedInterfaceState(String ifName) {
        InstanceIdentifier<Interface> targetInterfaceIID =
                InstanceIdentifier.create(InterfacesState.class)
                        .child(Interface.class, new InterfaceKey(ifName));

        return SfcDataStoreAPI.readTransactionAPI(targetInterfaceIID, LogicalDatastoreType.OPERATIONAL);
    }

    public static BridgeRefEntry getBridgeFromDpnId(String theDpnId) {
        InstanceIdentifier<BridgeRefEntry> theBridgeRefIID =
            InstanceIdentifier.create(BridgeRefInfo.class)
                .child(BridgeRefEntry.class, new BridgeRefEntryKey(new BigInteger(theDpnId)));

            return SfcDataStoreAPI.readTransactionAPI(theBridgeRefIID, LogicalDatastoreType.OPERATIONAL);
    }

    public static List<InterfaceExternalIds> readOvsNodeInterfaces(String ovsdbNodeId, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
            InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(new Uri("ovsdb:1"))))
                .child(Node.class, new NodeKey(new NodeId(ovsdbNodeId)))
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        TerminationPoint theTp = SfcDataStoreAPI.readTransactionAPI(tpIid, LogicalDatastoreType.OPERATIONAL);
        OvsdbTerminationPointAugmentation port = null;

        if (theTp != null) {
            port = theTp.getAugmentation(OvsdbTerminationPointAugmentation.class);
        }

        return port != null ? port.getInterfaceExternalIds() : new ArrayList<>();
    }

}

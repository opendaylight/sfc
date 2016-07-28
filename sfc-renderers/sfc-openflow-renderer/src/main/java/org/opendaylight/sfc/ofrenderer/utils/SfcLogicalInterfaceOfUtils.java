/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.utils;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by emigdua on 30/08/2016.
 */
public class SfcLogicalInterfaceOfUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcLogicalInterfaceOfUtils.class);

    // hide the constructor
    private SfcLogicalInterfaceOfUtils() {
    }

    /**
     * Get the termination point from the topology OPERATIONAL datastore
     *
     * @param ovsdbNodeId the ID of the ovsDB augmented node - as a string
     * @param portName the name of the interface
     * @return the TerminationPoint object
     */
    public static TerminationPoint readTerminationPoint(String ovsdbNodeId, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
                InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(new TopologyId(new Uri("ovsdb:1"))))
                        .child(Node.class, new NodeKey(new NodeId(ovsdbNodeId)))
                        .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        return SfcDataStoreAPI.readTransactionAPI(tpIid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Read an Interface from the CONFIG datastore
     *
     * @param ifName the neutron interface name
     * @return the interface-state object
     */
    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
    getServiceFunctionAttachedInterface(String ifName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey ifKey =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey(ifName);

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface>
                ifConfigIID = InstanceIdentifier.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class, ifKey);

        return SfcDataStoreAPI.readTransactionAPI(ifConfigIID, LogicalDatastoreType.CONFIGURATION);
    }

    /**
     * Read an InterfaceState from the OPERATIONAL datastore
     *
     * @param ifName the neutron interface name
     * @return the interface object
     */
    public static Interface getServiceFunctionAttachedInterfaceState(String ifName) {
        InstanceIdentifier<Interface> targetInterfaceIID =
                InstanceIdentifier.create(InterfacesState.class)
                        .child(Interface.class, new InterfaceKey(ifName));

        return SfcDataStoreAPI.readTransactionAPI(targetInterfaceIID, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Extract the dataplane ID of an interface-state object
     *
     * @param theIf the neutron interface object, fetched from the OPERATIONAL DS
     * @return the interface object
     */
    public static String getDpnIdFromInterfaceState(Interface theIf) {
        List<String> theLowerLayerIfs = theIf.getLowerLayerIf();
        String theOvsDpId, theOvsLowerLayerIf;
        if (theLowerLayerIfs.size() != 1) {
            return null;
        } else {
            theOvsDpId = theLowerLayerIfs.get(0);
            // the lower layer if looks like "openflow:<dpID>:<port-number>"
            theOvsLowerLayerIf = theOvsDpId.split(":")[1];
        }
        return theOvsLowerLayerIf;
    }

    /**
     * Get the bridge given its dataplane ID
     *
     * @param theDpnId the dataplane ID of the bridge
     * @return the reference to the bridge in the topology operational DS
     */
    public static BridgeRefEntry getBridgeFromDpnId(String theDpnId) {
        InstanceIdentifier<BridgeRefEntry> theBridgeRefIID =
                InstanceIdentifier.create(BridgeRefInfo.class)
                        .child(BridgeRefEntry.class, new BridgeRefEntryKey(new BigInteger(theDpnId)));

        return SfcDataStoreAPI.readTransactionAPI(theBridgeRefIID, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Get the MAC address of a service function
     *
     * @param ifName the neutron interface name
     * @return the MAC address of the service function
     */
    public static MacAddress getServiceFunctionMacAddress(String ifName) {
        LOG.debug("getServiceFunctionMacAddress - Enter - ifName: {}", ifName);

        // get the interface info from the CONFIG datastore
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
                theConfigIf = getServiceFunctionAttachedInterface(ifName);

        if (theConfigIf == null) {
            LOG.error("getServiceFunctionMacAddress - Could not retrieve the interface from CONFIG DS");
        }

        String theLogicalInterfaceName;
        ParentRefs parentRefs = theConfigIf.getAugmentation(ParentRefs.class);
        if (parentRefs != null) {
            theLogicalInterfaceName = parentRefs.getParentInterface();
        } else {
            LOG.error("getServiceFunctionMacAddress - Could not augment the interface object to parentRef");
            return null;
        }

        // assure the interface exists in the OPERATIONAL datastore, and also get the dpID
        Interface theIf = getServiceFunctionAttachedInterfaceState(ifName);

        // assure the interface exits in the OPERATIONAL DS
        if (theIf == null) {
            LOG.error("getServiceFunctionMacAddress - The bridge is not in the OPERATIONAL DS");
            return null;
        }

        // get the neutron OVS node id from genius bridge-ref OPERATIONAL DS
        String theDpnId = getDpnIdFromInterfaceState(theIf);
        BridgeRefEntry bridge = getBridgeFromDpnId(theDpnId);

        if (bridge == null) {
            LOG.error("getServiceFunctionMacAddress - Could not fetch the bridge from the OPERATIONAL DS");
            return null;
        }
        // use the BridgeRef object to get the ovsDB node id
        String theBridge =
                bridge.getBridgeReference().getValue().firstKeyOf(Node.class).getNodeId().getValue();

        TerminationPoint theTp = readTerminationPoint(theBridge, theLogicalInterfaceName);
        MacAddress theMac = null;
        if (theTp != null) {
            LOG.debug("getServiceFunctionMacAddress - Read the TerminationPoint");
            OvsdbTerminationPointAugmentation port =
                    theTp.getAugmentation(OvsdbTerminationPointAugmentation.class);

            if (port != null) {
                LOG.debug("getServiceFunctionMacAddress - Augmented the termination point to an ovsDB node");
                List<InterfaceExternalIds> externalIds = port.getInterfaceExternalIds();
                Predicate<InterfaceExternalIds> nonNullPredicate =
                        Objects::nonNull;
                Predicate<InterfaceExternalIds> hasAttachedMac =
                        externalId -> externalId.getExternalIdKey().compareTo("attached-mac") == 0;
                Predicate<InterfaceExternalIds> thePolicy = nonNullPredicate
                        .and(hasAttachedMac);
                List<InterfaceExternalIds> sfMacList =
                        externalIds.stream().filter(thePolicy).collect(Collectors.toList());

                // can there be more than 1
                if (sfMacList.size() == 0) {
                    LOG.error("getServiceFunctionMacAddress - No MAC address was found for the service function");
                    return null;
                } else {
                    theMac = new MacAddress(sfMacList.get(0).getExternalIdValue());
                }
            }
        }
        return theMac;
    }

    /**
     * The method checks whether a service function is using a logical interface
     * @param sf     A service function
     * @return bolean    true when the service function data plane locator is a logical interface; false otherwise
     */
    public static boolean isSfUsingALogicalInterface(ServiceFunction sf) {
        return (getSfLogicalInterface(sf) != null);
    }

    /**
     * Given a SF, the method returns the logical interface name
     * @param sf    The service function to analyze
     * @return      The interface name, or null when it cannot be retrieved
     */
    public static String getSfLogicalInterface(ServiceFunction sf) {
        SfDataPlaneLocator sfDpl = sf.getSfDataPlaneLocator().get(0);
        if (sfDpl != null
                && sfDpl.getLocatorType().getImplementedInterface() == LogicalInterface.class) {
            LOG.debug("getSfLogicalInterface: logical interface found");
            LogicalInterface sfNeutronInterface = ((LogicalInterface)sfDpl.getLocatorType());
            String targetInterfaceName = sfNeutronInterface.getInterfaceName();
            return targetInterfaceName;
        }
        return null;
    }

}

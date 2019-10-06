/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.Uint64;

public final class SfcGeniusDataUtils {
    // hide the default constructor
    private SfcGeniusDataUtils() {
    }

    /**
     * Fetches the SF's MAC address, given a neutron port name.
     *
     * @param ifName
     *            the name of the neutron port to which the SF is connected
     * @return the MAC address used by the SF, when available
     */
    public static Optional<MacAddress> getServiceFunctionMacAddress(String ifName) {
        String theLogicalInterfaceName = SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceName(ifName)
                .orElseThrow(() -> new RuntimeException("Interface is not present in the CONFIG DS"));
        Interface theIf = SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName)
                .orElseThrow(() -> new RuntimeException("Interface is not present in the OPERATIONAL DS"));
        Uint64 theDataplaneId = getDpnIdFromLowerLayerIfList(theIf.getLowerLayerIf());

        return SfcGeniusUtilsDataGetter.getBridgeFromDpnId(theDataplaneId).map(BridgeRefEntry::getBridgeReference)
                .map(OvsdbBridgeRef::getValue).map(iid -> iid.firstKeyOf(Node.class)).map(NodeKey::getNodeId)
                .map(NodeId::getValue)
                .map(theBridgeName -> SfcGeniusUtilsDataGetter.readOvsNodeInterfaces(theBridgeName,
                        theLogicalInterfaceName))
                .orElseThrow(() -> new RuntimeException(
                        String.format("Could not get ovsDB information for IF: %s", theLogicalInterfaceName)))
                .stream().filter(ifExternalId -> ifExternalId.getExternalIdKey() != null)
                .filter(theExternalId -> theExternalId.getExternalIdKey().equals(SfcGeniusConstants.MAC_KEY))
                .map(theMac -> new MacAddress(theMac.getExternalIdValue())).findFirst();
    }

    /**
     * Fetches the MAC address for the switch port to which a given SF is
     * connected.
     *
     * @param ifName
     *            the name of the neutron port to which the SF is connected
     * @return the MAC address used by the SFF port to which the SF is
     *         connected, when available
     */
    public static Optional<MacAddress> getServiceFunctionForwarderPortMacAddress(String ifName) {
        Interface theIf = SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName)
                .orElseThrow(() -> new RuntimeException("Interface is not present in the OPERATIONAL DS"));
        if (theIf.getPhysAddress() == null) {
            throw new RuntimeException("Interface is present in the OPER DS, but it doesn't have a mac address");
        }
        return Optional.of(new MacAddress(theIf.getPhysAddress().getValue()));
    }

    /**
     * Get the information of all physical interfaces mapped to a given logical
     * interface.
     *
     * @param theIfName
     *            the name of the logical interface
     * @return a list of all the physical interfaces mapped to the given logical
     *         interface
     */
    public static List<String> getInterfaceLowerLayerIf(String theIfName) {
        return SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(theIfName)
                .map(Interface::getLowerLayerIf).orElse(Collections.emptyList());
    }

    /**
     * Fetches the logical interface dataplane locators of a given Service
     * Function.
     *
     * @param sf
     *            The service function from which we want to extract the
     *            dataplane locators
     * @return The list of logical interface dataplane locators attached to the
     *         Service Function
     */
    public static List<String> getSfLogicalInterfaces(@NonNull ServiceFunction sf) {
        return sf.getSfDataPlaneLocator()
                .stream()
                .map(SfcGeniusDataUtils::getLogicalInterfaceNameFromLocator)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the logical interface name from a SF data plane locator.
     *
     * @param  sfDataPlaneLocator the SF data plane locator.
     * @return the logical interface name or {@code Null} if locator is not a
     *         logical interface.
     */
    public static String getLogicalInterfaceNameFromLocator(@NonNull SfDataPlaneLocator sfDataPlaneLocator) {
        return Optional.ofNullable(sfDataPlaneLocator.getLocatorType())
                .filter(locatorType -> LogicalInterface.class.equals(locatorType.implementedInterface()))
                .map(locatorType -> ((LogicalInterface) locatorType).getInterfaceName())
                .orElse(null);
    }

    /**
     * Extracts the data plane node Id from a lower layer interface list.
     *
     * @param lowerLayerIfList
     *            to extract the data plane node Id from.
     * @return the data plane node Id.
     * @throws SfcGeniusRuntimeException
     *             wrapping an {@link IllegalArgumentException} if the input
     *             list does not contain one item only, or if the format of the
     *             item is invalid.
     */
    public static Uint64 getDpnIdFromLowerLayerIfList(List<String> lowerLayerIfList) {
        if (lowerLayerIfList == null || lowerLayerIfList.size() != 1) {
            throw new SfcGeniusRuntimeException(
                    new IllegalArgumentException("Expected 1 and only 1 item in lower layer interface list"));
        }
        long nodeId = MDSALUtil.getDpnIdFromPortName(new NodeConnectorId(lowerLayerIfList.get(0)));
        if (nodeId < 0L) {
            throw new SfcGeniusRuntimeException(
                    new IllegalArgumentException("Unexpected format of lower layer interface list"));
        }
        return Uint64.valueOf(nodeId);
    }
}

/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SfcGeniusDataUtils {
    // hide the default constructor
    private SfcGeniusDataUtils() {}

    /**
     * Fetches the SF's MAC address, given a neutron port name
     * @param ifName    the name of the neutron port to which the SF is connected
     * @return          the MAC address used by the SF, when available
     */
    public static Optional<MacAddress> getServiceFunctionMacAddress(String ifName) {
        String theLogicalInterfaceName = SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceName(ifName)
                        .orElseThrow(() -> new RuntimeException("Interface is not present in the CONFIG DS"));
        Interface theIf = SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName)
                        .orElseThrow(() -> new RuntimeException("Interface is not present in the OPERATIONAL DS"));
        BigInteger theDataplaneId = SfcGeniusUtils.getDpnIdFromLowerLayerIfList(theIf.getLowerLayerIf());

        return SfcGeniusUtilsDataGetter.getBridgeFromDpnId(theDataplaneId)
                    .map(BridgeRefEntry::getBridgeReference)
                    .map(OvsdbBridgeRef::getValue)
                    .map(iid -> iid.firstKeyOf(Node.class))
                    .map(NodeKey::getNodeId)
                    .map(NodeId::getValue)
                    .map(theBridgeName ->
                            SfcGeniusUtilsDataGetter.readOvsNodeInterfaces(theBridgeName, theLogicalInterfaceName))
                    .orElseThrow(() -> new RuntimeException(
                            String.format("Could not get ovsDB information for IF: %s", theLogicalInterfaceName)))
                    .stream()
                        .filter(ifExternalId -> ifExternalId.getExternalIdKey() != null)
                        .filter(theExternalId ->
                                theExternalId.getExternalIdKey().equals(SfcGeniusConstants.MAC_KEY))
                        .map(theMac -> new MacAddress(theMac.getExternalIdValue()))
                .findFirst();
    }

    /**
     * Fetches the MAC address for the switch port to which a given SF is connected
     * @param ifName    the name of the neutron port to which the SF is connected
     * @return          the MAC address used by the SFF port to which
     *      the SF is connected, when available
     */
    public static Optional<MacAddress> getServiceFunctionForwarderPortMacAddress(String ifName) {
        Interface theIf = SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName)
                        .orElseThrow(() -> new RuntimeException("Interface is not present in the OPERATIONAL DS"));
        if (theIf.getPhysAddress() == null) {
            throw new RuntimeException(
                    "Interface is present in the OPER DS, but it doesn't have a mac address");
        }
        return Optional.of(new MacAddress(theIf.getPhysAddress().getValue()));
    }

    /**
     * Checks if a given Service Function has logical interfaces attached to it
     * @param sf    The service function we want to check
     * @return      True if it has a LogicalInterface attached attached, False otherwise
     */
    public static boolean isSfUsingALogicalInterface(ServiceFunction sf) {
        return Optional.ofNullable(sf)
                .map(SfcGeniusDataUtils::getSfLogicalInterfaceDataplaneLocators)
                .filter(logicalIfDpls -> logicalIfDpls.size() > 0)
                .isPresent();
    }

    /**
     * Fetches the logical interface name, when given a ServiceFunction
     * @param   sf  The service function whose logical interface name we're interested on
     * @throws  IllegalArgumentException if the Service Function is not attached to a single Logical Interface
     * @return  The interface name, when available
     */
    public static String getSfLogicalInterface(ServiceFunction sf) {
        List<LogicalInterface> theDpls = Optional.ofNullable(sf)
                .map(SfcGeniusDataUtils::getSfLogicalInterfaceDataplaneLocators)
                .orElse(Collections.emptyList());

        if (theDpls.size() == 1) {
            return theDpls.get(0).getInterfaceName();
        }
        else {
            throw new IllegalArgumentException(
                    String.format("We *must* have a single LogicalInterface locator. Got %d", theDpls.size()));
        }
    }

    /**
     * Get the information of all physical interfaces mapped to a given logical interface
     * @param theIfName     the name of the logical interface
     * @return              a list of all the physical interfaces mapped to the given logical interface
     */
    public static List<String> getInterfaceLowerLayerIf(String theIfName) {
        return SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(theIfName)
                .map(Interface::getLowerLayerIf)
                .orElse(Collections.emptyList());
    }

    /**
     * Fetches the logical interface dataplane locators of a given Service Function
     * @param   sf   The service function from which we want to extract the dataplane locators
     * @return       The list of logical interface dataplane locators attached to the Service Function
     */
    private static List<LogicalInterface> getSfLogicalInterfaceDataplaneLocators(ServiceFunction sf) {
        return Optional.ofNullable(sf)
                .orElseThrow(() -> new RuntimeException("ServiceFunction is null"))
                .getSfDataPlaneLocator()
                .stream()
                .filter(dpl -> dpl.getLocatorType() != null)
                .filter(dpl -> dpl.getLocatorType().getImplementedInterface().equals(LogicalInterface.class))
                .map(logicalIfDpl -> (LogicalInterface) logicalIfDpl.getLocatorType())
                .collect(Collectors.toList());
    }
}

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
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SfcGeniusDataUtils {
    /**
     * Fetches the SF's MAC address, given a neutron port name
     *
     * @param ifName    the name of the neutron port to which the SF is connected
     * @return          the MAC address used by the SF, when available
     */
    public static Optional<MacAddress> getServiceFunctionMacAddress(String ifName) {
        String theLogicalInterfaceName =
                Optional.ofNullable(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceName(ifName))
                        .orElseThrow(() -> new RuntimeException("Interface is not present in the CONFIG DS"));
        Interface theIf =
                Optional.ofNullable(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName))
                        .orElseThrow(() -> new RuntimeException("Interface is not present in the OPERATIONAL DS"));

        return Optional.ofNullable(
                SfcGeniusUtils.getDpnIdFromLowerLayerIfList(theIf.getLowerLayerIf()))
                    .map(theDpnId -> SfcGeniusUtilsDataGetter.getBridgeFromDpnId(theDpnId))
                    .map(BridgeRefEntry::getBridgeReference)
                    .map(OvsdbBridgeRef::getValue)
                    .map(iid -> iid.firstKeyOf(Node.class))
                    .map(node -> node.getNodeId())
                    .map(nodeId -> nodeId.getValue())
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
     * Fetches the logical interface name, when given a ServiceFunction
     * @param sf    The service function to analyze
     * @return      The interface name, when available
     */
    public static String getSfLogicalInterface(ServiceFunction sf) {
        List<String> theDpls = Optional.ofNullable(sf)
                .orElseThrow(() -> new RuntimeException("ServiceFunction is null"))
                .getSfDataPlaneLocator().stream()
                .filter(dpl -> dpl.getLocatorType() != null &&
                        dpl.getLocatorType().getImplementedInterface().equals(LogicalInterface.class))
                .map(logicalIfDpl -> ((LogicalInterface)logicalIfDpl.getLocatorType()).getInterfaceName())
                .collect(Collectors.toList());
        if (theDpls.size() == 1) {
            return theDpls.get(0);
        }
        else {
            throw new IllegalArgumentException(
                    String.format("We *must* have a single LogicalInterface locator. Got %d", theDpls.size()));
        }
    }
}

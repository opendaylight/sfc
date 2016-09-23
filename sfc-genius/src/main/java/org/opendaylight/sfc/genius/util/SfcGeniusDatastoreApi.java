/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.util;

import org.opendaylight.sfc.genius.impl.utils.SfcGeniusUtils;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import java.util.Optional;


public class SfcGeniusDatastoreApi {
    public static MacAddress getServiceFunctionMacAddress(String ifName) {
        String theLogicalInterfaceName =
                Optional.ofNullable(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceName(ifName))
                        .orElseThrow(() -> new RuntimeException("Interface does not feature in the CONFIG DS"));

        Interface theIf =
                Optional.ofNullable(SfcGeniusUtilsDataGetter.getServiceFunctionAttachedInterfaceState(ifName))
                        .orElseThrow(() -> new RuntimeException("Interface does not feature in the OPERATIONAL DS"));

        String theDpnId = Optional.ofNullable(
                SfcGeniusUtils.getDpnIdFromLowerLayerIfList(theIf.getLowerLayerIf()).toString())
                .orElseThrow(() -> new RuntimeException("Could not get the dataplane id from the InterfaceState"));

        return Optional.ofNullable(SfcGeniusUtilsDataGetter.getBridgeFromDpnId(theDpnId))
                .map(bridgeObject ->
                        Optional.ofNullable(bridgeObject.getBridgeReference().getValue().firstKeyOf(Node.class).getNodeId().getValue()))
                .orElseThrow(() -> new RuntimeException("Could not get the BridgeReference object"))
                .map(theBridgeName ->
                        SfcGeniusUtilsDataGetter.readOvsNodeInterfaces(theBridgeName, theLogicalInterfaceName))
                .orElseThrow(() -> new RuntimeException("Could not get the ovsDB information")).stream()
                .filter(ifExternalId -> ifExternalId.getExternalIdKey().compareTo("attached-mac") == 0)
                .map(theMac -> new MacAddress(theMac.getExternalIdValue())).findFirst().get();
    }

    /**
     * The method checks whether a service function is using a logical interface
     * @param sf     A service function
     * @return bolean    true when the service function data plane locator is a logical interface; false otherwise
     */
    public static boolean isSfUsingALogicalInterface(ServiceFunction sf) {
        try {
            return (getSfLogicalInterface(sf) != null);
        } catch (java.util.NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Given a SF, the method returns the logical interface name
     * @param sf    The service function to analyze
     * @return      The interface name, or null when it cannot be retrieved
     */
    public static String getSfLogicalInterface(ServiceFunction sf) {
        return Optional.ofNullable(sf)
                .orElseThrow(() -> new RuntimeException(""))
                .getSfDataPlaneLocator().stream()
                .filter(dpl -> dpl.getLocatorType() != null &&
                        dpl.getLocatorType().getImplementedInterface().equals(LogicalInterface.class))
                .map(logicalIfDpl -> ((LogicalInterface)logicalIfDpl.getLocatorType()).getInterfaceName())
                .findFirst().get();
    }
}

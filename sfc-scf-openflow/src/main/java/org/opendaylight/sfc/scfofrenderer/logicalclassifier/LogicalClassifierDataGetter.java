/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.logicalclassifier;


import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.scfofrenderer.SfcNshHeader;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LogicalClassifierDataGetter {

    SfcGeniusRpcClient theGeniusRpcClient;

    private LogicalClassifierDataGetter() {}

    public LogicalClassifierDataGetter(SfcGeniusRpcClient theRpcClient) {
        theGeniusRpcClient = theRpcClient;
    }

    /**
     * Get the name of the tunnel interface connecting these two dataplane IDs
     * @param srcDpnId  the dataplane ID of the source switch
     * @param dstDpnId  the dataplane ID of the destination switch
     * @return          the name of the tunnel interface connecting these two dataplane IDs, if any
     */
    public Optional<String> getInterfaceBetweenDpnIds(DpnIdType srcDpnId, DpnIdType dstDpnId) {
        return theGeniusRpcClient.getTargetInterfaceFromGeniusRPC(srcDpnId, dstDpnId);
    }

    /**
     * Get the list of egress actions to output packets to the given tunnel interface
     * @param theTunnelInterface    the name of the tunnel interface
     * @param actionOffset          the position where we want to start writing the actions into the action set
     * @return                      the action list containing the necessary actions to output a packet to the
     *                              given interface
     */
    public List<Action> getEgressActionsForTunnelInterface(String theTunnelInterface, int actionOffset) {
        return theGeniusRpcClient.getEgressActionsFromGeniusRPC(theTunnelInterface, true, actionOffset)
                .orElse(Collections.emptyList());
    }

    /**
     * Get the name of the compute node connected to the supplied interfaceName - for logical SFF scenarios - or
     * the name of the compute node hosting the supplied SFF
     * @param theInterfaceName  the interface for which we want to retrieve the name of the node who owns it.
     * @return                  the name of the compute node hosting the supplied SF - format: "openflow:xxx"
     */
    public Optional<String> getNodeName(String theInterfaceName) {
        return theGeniusRpcClient
                .getDpnIdFromInterfaceNameFromGeniusRPC(theInterfaceName)
                .map(dpnID -> String.format("openflow:%s", dpnID.getValue().toString()));
    }

    /**
     * Get the node name of the first SF in the RSP featured in the given NSH header
     * @param theHeader     the NSH header from which we want to compute the dpnID of the first SF
     * @return              the node name (ex: "openflow:dpnID") of the first SF in the chain specified
     *                      within the NSH header
     */
    public Optional<String> getFirstHopNodeName(SfcNshHeader theHeader) {
        return Optional.ofNullable(SfcProviderServiceFunctionAPI.readServiceFunction(theHeader.getFirstSfName()))
                .map(SfcGeniusDataUtils::getSfLogicalInterface)
                .map(this::getNodeName)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    /**
     * Get the openflow port associated to a logical interface
     * @param theInterfaceName  the name of the interface
     * @return                  the openflow port number
     */
    public static Optional<Long> getOpenflowPort(String theInterfaceName) {
        return SfcGeniusDataUtils.getInterfaceLowerLayerIf(theInterfaceName)
                .stream()
                .map(str -> str.split(":"))
                .filter(strArray -> strArray.length == 3)
                .map(strArray -> strArray[2])
                .map(Long::parseLong)
                .findFirst();
    }

    /**
     * Get the {@link DpnIdType} out of a node name
     * @param   theNodeName                 the name of the compute node, whose format is: "openflow:dpnID"
     * @throws  IllegalArgumentException    if the supplied node name format is incorrect
     * @return                              the {@link DpnIdType} of the compute node
     */
    public static DpnIdType getDpnIdFromNodeName(String theNodeName) {
        return Optional.of(theNodeName)
                .map(nodeName -> nodeName.split(":"))
                .filter(splitNodeName -> splitNodeName.length == 2)
                .map(splitNodeName -> splitNodeName[1])
                .map(BigInteger::new)
                .map(DpnIdType::new)
                .orElseThrow(IllegalArgumentException::new);
    }
}

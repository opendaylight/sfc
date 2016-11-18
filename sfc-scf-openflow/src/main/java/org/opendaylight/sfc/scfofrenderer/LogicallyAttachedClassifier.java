/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import java.util.Optional;

public class LogicallyAttachedClassifier {

    private final ServiceFunctionForwarder sff;

    private final LogicalClassifierDataGetter logicalSffDataGetter;

    public LogicallyAttachedClassifier(ServiceFunctionForwarder theSff, LogicalClassifierDataGetter theDataGetter) {
        sff = theSff;
        logicalSffDataGetter = theDataGetter;
    }

    public boolean usesLogicalInterfaces() {
        // TODO
        return false;
    }

    public FlowBuilder initClassifierTable() {
        // TODO
        return new FlowBuilder();
    }

    /**
     * Create the flows for the genius integrated classifier
     * @param flowKey               the key for the flow objects
     * @param match                 the Match object
     * @param sfcNshHeader          all related NSH info is encapsulated within this object
     * @param classifierNodeName    the node name of the classifier (ex: "openflow:dpnID")
     * @return                      a FlowBuilder object containing the desired flow
     */
    public FlowBuilder createClassifierOutFlow(String flowKey, Match match, SfcNshHeader sfcNshHeader,
                                               String classifierNodeName) {
        // TODO
        return new FlowBuilder();
    }

    /**
     * Get the name of the compute node connected to the supplied interfaceName - for logical SFF scenarios - or
     * the name of the compute node hosting the supplied SFF
     * @param theInterfaceName  the interface for which we want to retrieve the name of the node who owns it.
     * @return                  the name of the compute node hosting the supplied SF - format: "openflow:xxx"
     */
    public Optional<String> getNodeName(String theInterfaceName) {
        // TODO - also handle the logical SFF scenario
        return Optional.ofNullable(sff)
                .filter(sff -> sff.getAugmentation(SffOvsBridgeAugmentation.class) != null)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);
    }
}

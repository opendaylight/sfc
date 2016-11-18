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

public class BareClassifier implements ClassifierInterface {
    private ServiceFunctionForwarder sff;

    private BareClassifier() {}

    public BareClassifier(ServiceFunctionForwarder theSff) {
        sff = theSff;
    }

    public FlowBuilder initClassifierTable() {
        return SfcScfOfUtils.initClassifierTable();
    }

    @Override
    public FlowBuilder createClassifierOutFlow(String flowKey, Match match, SfcNshHeader sfcNshHeader,
                                               String classifierNodeName) {
        Long outPort = SfcOvsUtil.getVxlanGpeOfPort(classifierNodeName);
        return SfcScfOfUtils.createClassifierOutFlow(flowKey, match, sfcNshHeader, outPort);
    }

    @Override
    public Optional<String> getNodeName(String theInterfaceName) {
        return Optional.ofNullable(sff)
                .filter(sff -> sff.getAugmentation(SffOvsBridgeAugmentation.class) != null)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);
    }

    @Override
    public Optional<Long> getInPort(String ifName, String nodeName) {
        return Optional.ofNullable(SfcOvsUtil.getOfPortByName(nodeName, ifName));
    }
}

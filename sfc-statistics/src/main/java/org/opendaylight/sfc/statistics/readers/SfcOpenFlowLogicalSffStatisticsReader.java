/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.readers;

import java.util.List;
import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.RspLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/**
 * This class just extends SfcOpenFlowStatisticsReader and overrides the getSffNodeId()
 * method, since the NodeId is stored differently for a Logical SFF.
 */
public class SfcOpenFlowLogicalSffStatisticsReader extends SfcOpenFlowStatisticsReader {
    private RenderedServicePath rsp;

    public SfcOpenFlowLogicalSffStatisticsReader(ServiceFunctionForwarder sff, RenderedServicePath rsp) {
        super(sff);
        this.rsp = rsp;
    }

    @Override
    protected Optional<NodeId> getSffNodeId(ServiceFunctionForwarder sff, long nsp, short nsi) {
        List<RenderedServicePathHop> rspHops = this.rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop rspHop : rspHops) {
            if (rspHop.getServiceIndex() != nsi) {
                continue;
            }

            RspLogicalSffAugmentation logicalSffAugmentation = rspHop.augmentation(RspLogicalSffAugmentation.class);
            if (logicalSffAugmentation != null) {
                if (logicalSffAugmentation.getDpnId() != null) {
                    return Optional.of(new NodeId("openflow:" + logicalSffAugmentation.getDpnId().getValue()));
                }
            }
        }

        return Optional.empty();
    }
}

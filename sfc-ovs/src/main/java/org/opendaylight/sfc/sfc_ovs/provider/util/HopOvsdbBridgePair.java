/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Rendered Service Path Hop - OvsdbBridgeAugmentation pair
 * <p/>
 * <p/>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-04-14
 */

package org.opendaylight.sfc.sfc_ovs.provider.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Preconditions;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.info.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HopOvsdbBridgePair {

    private static final Logger LOG = LoggerFactory.getLogger(HopOvsdbBridgePair.class);

    public final RenderedServicePathHop renderedServicePathHop;
    public final OvsdbBridgeAugmentation ovsdbBridgeAugmentation;

    public HopOvsdbBridgePair(RenderedServicePathHop renderedServicePathHop, OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        Preconditions.checkNotNull(renderedServicePathHop, "Passed argument renderedServicePathHop cannot be null");
        Preconditions.checkNotNull(ovsdbBridgeAugmentation, "Passed argument ovsdbBridgeAugmentation cannot be null");

        this.renderedServicePathHop = renderedServicePathHop;
        this.ovsdbBridgeAugmentation = ovsdbBridgeAugmentation;
    }

    public static List<HopOvsdbBridgePair> buildHopOvsdbBridgePairList(RenderedServicePath renderedServicePath, ExecutorService executor) {
        Preconditions.checkNotNull(renderedServicePath);
        Preconditions.checkNotNull(renderedServicePath.getRenderedServicePathHop(),
                "Cannot build HopOvsdbBridgePairList for RSP '"+ renderedServicePath.getName() + "', the RSP does not contain any HOPS!");
        Preconditions.checkNotNull(executor);

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList = new ArrayList<>();

        for (RenderedServicePathHop hop : renderedServicePath.getRenderedServicePathHop()) {
            Object[] methodParams = {SfcOvsUtil.buildOvsdbBridgeIID(hop.getServiceFunctionForwarder())};
            SfcOvsDataStoreAPI readOvsdbBridge =
                    new SfcOvsDataStoreAPI(
                            SfcOvsDataStoreAPI.Method.READ_OVSDB_BRIDGE,
                            methodParams
                    );

            OvsdbBridgeAugmentation ovsdbBridge =
                    (OvsdbBridgeAugmentation) SfcOvsUtil.submitCallable(readOvsdbBridge, executor);

            if (ovsdbBridge != null) {
                if (hop.getHopNumber() >= 0 && hop.getHopNumber() <= hopOvsdbBridgePairList.size()) {
                    hopOvsdbBridgePairList.add(hop.getHopNumber(), new HopOvsdbBridgePair(hop, ovsdbBridge));
                } else {
                    LOG.warn("Some of the hops in RSP: '{}' are not using OVS SFF. Hybrid chains are not supported yet",
                            renderedServicePath.getName());
                    return Collections.emptyList();
                }
            }
        }

        if (hopOvsdbBridgePairList.size() != renderedServicePath.getRenderedServicePathHop().size()) {
            LOG.warn("Some of the hops in RSP: '{}' are not using OVS SFF. Hybrid chains are not supported yet",
                    renderedServicePath.getName());
            return Collections.emptyList();
        }

        return hopOvsdbBridgePairList;
    }

}

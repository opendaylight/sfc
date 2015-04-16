/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Rendered Service Path Hop - OvsdbBridgeAugmentation pair
 *
 * <p>
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-04-14
 */

package org.opendaylight.sfc.sfc_ovs.provider.util;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Preconditions;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;

public class HopOvsdbBridgePair {

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

            hopOvsdbBridgePairList.add(hop.getHopNumber(), new HopOvsdbBridgePair(hop, ovsdbBridge));
        }

        return hopOvsdbBridgePairList;
    }

}

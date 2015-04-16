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


import com.google.common.base.Preconditions;
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

}

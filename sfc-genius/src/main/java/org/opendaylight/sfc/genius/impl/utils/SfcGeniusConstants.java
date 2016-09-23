/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * Holds constants that are used throughout the sfc-genius module.
 */
public class SfcGeniusConstants {

    /**
     * Virtual network identifier used for SFC service: {@value SFC_VNID}.
     */
    public static final int SFC_VNID = 0;

    /**
     * Root node for OVS DB entities in the topology
     */
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    /**
     * Key for the MAC address of the SF within the external interface info map
     */
    public static final String MAC_KEY = "attached-mac";
}

/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * Holds constants that are used throughout the sfc-genius module.
 */
// TODO move appropriate constants to genius NwConstants
public class SfcGeniusConstants {

    /**
     * Cookie used in the genius dispatcher flow for SFC
     */
    public static final BigInteger COOKIE_SFC_INGRESS_TABLE = new BigInteger("8030000", 16);

    /**
     * Priority used in the genius dispatcher flow for SFC
     */
    public static final int SFC_SERVICE_PRIORITY = 6;

    /**
     * Virtual network identifier used for SFC service.
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

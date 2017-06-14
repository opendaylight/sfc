/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow;

public final class OpenflowConstants {

    // identifies initialization flows installed in the SFFs - since they don't belong
    // to a particular NSP / RSP path ID
    public static final long SFC_FLOWS = 0xdeadbeef;

    // Constant for marking next protocol=NSH in GPE
    public static final short TUN_GPE_NP_NSH = 0x4;

    // Ethernet NextProtocol/EtherType for NSH
    public static final long ETHERTYPE_NSH = 0x894F;

    public static final short NSH_NP_ETH = 0x3;

    // Flow table names: used by both OpenflowRenderer and SFC statistics
    public static final String FLOW_NAME_DELIMITER = "_";
    public static final String FLOW_NAME_MATCH_ANY = "matchAny";
    public static final String FLOW_NAME_MATCH_ANY_DROP = "matchAnyDrop";
    public static final String FLOW_NAME_MATCH_ANY_RESUBMIT = "matchAnyResubmit";
    public static final String FLOW_NAME_CLASS_DPDK_OUT = "classifierDpdkOutput";
    public static final String FLOW_NAME_CLASS_DPDK_IN = "classifierDpdkInput";
    public static final String FLOW_NAME_TRANSPORT_INGRESS = "transportIngress";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_ARP = "transportIngressArp";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_MAC = "transportIngressMac";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_MPLS = "transportIngressMpls";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_TCP = "transportIngressTcp";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_UDP = "transportIngressUdp";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_VLAN = "transportIngressVlan";
    public static final String FLOW_NAME_PATH_MAPPER = "pathMapper";
    public static final String FLOW_NAME_PATH_MAPPER_ACL = "pathMapperAcl";
    public static final String FLOW_NAME_NEXT_HOP = "nextHop";
    public static final String FLOW_NAME_TRANSPORT_EGRESS = "transportEgress";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS = "transportEgressLastHop";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_PIPELINE = "transportEgressLastHop_Pipeline";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_REMOTE = "transportEgressLastHop_TunnelRemote";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_LOCAL = "transportEgressLastHop_TunnelLocal";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_REMOTE = "transportEgressLastHop_NshRemote";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_LOCAL = "transportEgressLastHop_NshLocal";
    public static final String FLOW_NAME_APPCOEXIST_TRANSPORT_EGRESS = "transportEgressAppCoexist";
    public static final String FLOW_NAME_SF_LOOPBACK_INGRESS = "sfLoopbackIngress";
    public static final String FLOW_NAME_SF_LOOPBACK_EGRESS = "sfLoopbackEgress";

    private OpenflowConstants() {
    }
}

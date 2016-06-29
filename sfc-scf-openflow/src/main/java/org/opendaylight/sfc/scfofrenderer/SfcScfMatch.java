/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

/*
 * This class is to convert IETF ACL Match to openflow Match
 * @author Ruijing Guo
 */

public class SfcScfMatch {
    private MatchBuilder mb;

    public SfcScfMatch () {
        mb = new MatchBuilder();
    }

   /**
    * set port match
    * @param port ovs port
    * @return scf match
    */
    public SfcScfMatch setPortMatch(NodeConnectorId port) {
        if (port != null) {
            mb.setInPort(port);
        }
        return this;
    }

   /**
    * set IETF Acl matches
    * @param matches IETF ACL matches
    * @return scf match
    */
    public SfcScfMatch setAclMatch(Matches matches) {
        if (matches == null) {
            return this;
        }

        if (matches.getAceType() instanceof AceEth) {
            AceEth eth = (AceEth) matches.getAceType();

            // don't support mac mask

            if (eth.getSourceMacAddress() != null) {
                SfcOpenflowUtils.addMatchSrcMac(mb, eth.getSourceMacAddress().getValue());
            }
            if (eth.getDestinationMacAddress() != null) {
                SfcOpenflowUtils.addMatchDstMac(mb, eth.getDestinationMacAddress().getValue());
            }

        } else if (matches.getAceType() instanceof AceIp) {
            AceIp aceip = (AceIp) matches.getAceType();

            if (aceip.getDscp() != null) {
                SfcOpenflowUtils.addMatchDscp(mb, aceip.getDscp().getValue());
            }

            if (aceip.getProtocol() != null) {
                SfcOpenflowUtils.addMatchIpProtocol(mb, aceip.getProtocol());

                Integer srcPort = null;
                Integer dstPort = null;

                if (aceip.getSourcePortRange() != null &&
                    aceip.getSourcePortRange().getLowerPort() != null &&
                    aceip.getSourcePortRange().getLowerPort().getValue() != null &&
                    aceip.getSourcePortRange().getLowerPort().getValue().intValue() != 0) {
                    srcPort = aceip.getSourcePortRange().getLowerPort().getValue();
                }
                if (aceip.getDestinationPortRange() != null &&
                    aceip.getDestinationPortRange().getLowerPort() != null &&
                    aceip.getDestinationPortRange().getLowerPort().getValue() != null &&
                    aceip.getDestinationPortRange().getLowerPort().getValue().intValue() != 0) {
                    dstPort = aceip.getDestinationPortRange().getLowerPort().getValue();
                }

                // don't support port range
                switch (aceip.getProtocol()) {
                    case SfcOpenflowUtils.IP_PROTOCOL_UDP:

                        if (srcPort != null) {
                            SfcOpenflowUtils.addMatchSrcUdpPort(mb, srcPort.intValue());
                        }
                        if (dstPort != null) {
                            SfcOpenflowUtils.addMatchDstUdpPort(mb, dstPort.intValue());
                        }
                        break;

                    case SfcOpenflowUtils.IP_PROTOCOL_TCP:

                        if (srcPort != null) {
                            SfcOpenflowUtils.addMatchSrcTcpPort(mb, srcPort.intValue());
                        }
                        if (dstPort != null) {
                            SfcOpenflowUtils.addMatchDstTcpPort(mb, dstPort.intValue());
                        }
                        break;

                    case SfcOpenflowUtils.IP_PROTOCOL_SCTP:
                        if (srcPort != null) {
                            SfcOpenflowUtils.addMatchSrcSctpPort(mb, srcPort.intValue());
                        }
                        if (dstPort != null) {
                            SfcOpenflowUtils.addMatchDstSctpPort(mb, dstPort.intValue());
                        }
                        break;
                }
            }

            if (aceip.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 ipv4 = (AceIpv4) aceip.getAceIpVersion();
                SfcOpenflowUtils.addMatchEtherType(mb, SfcOpenflowUtils.ETHERTYPE_IPV4);

                Ipv4Prefix src = ipv4.getSourceIpv4Network();
                if (src != null) {
                    String s[] = src.getValue().split("/");
                    SfcOpenflowUtils.addMatchSrcIpv4(mb, s[0], Integer.valueOf(s[1]).intValue());
                }

                Ipv4Prefix dst = ipv4.getDestinationIpv4Network();
                if (dst != null) {
                    String d[] = dst.getValue().split("/");
                    SfcOpenflowUtils.addMatchDstIpv4(mb, d[0], Integer.valueOf(d[1]).intValue());
                }
            }
            if (aceip.getAceIpVersion() instanceof AceIpv6) {
                AceIpv6 ipv6 = (AceIpv6) aceip.getAceIpVersion();
                SfcOpenflowUtils.addMatchEtherType(mb, SfcOpenflowUtils.ETHERTYPE_IPV6);

                Ipv6Prefix src = ipv6.getSourceIpv6Network();
                if (src != null) {
                    String s[] = src.getValue().split("/");
                    SfcOpenflowUtils.addMatchSrcIpv6(mb, s[0], Integer.valueOf(s[1]).intValue());
                }

                Ipv6Prefix dst = ipv6.getDestinationIpv6Network();
                if (dst != null ) {
                    String d[] = dst.getValue().split("/");
                    SfcOpenflowUtils.addMatchDstIpv6(mb, d[0], Integer.valueOf(d[1]).intValue());
                }
            }
        }
        return this;
    }

   /**
    * build openflow match
    * @return flow match
    */
    public Match build() {
        return mb.build();
    }
}

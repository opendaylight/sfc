/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.SourcePortRangeBuilder;

import java.util.ArrayList;
import java.util.List;

public class ClassifierAclDataBuilder {
    public  List<Ace> mockAces(final int nMatches) {
        String srcNetwork = "192.168.2.0/24";
        String dstNetwork = "192.168.2.0/24";
        String rspPrefix = "RSP_";
        int srcLowerPort = 80;
        int dstLowerPort = 80;
        short protocol = SfcOpenflowUtils.IP_PROTOCOL_TCP;

        List<Ace> theAces = new ArrayList<>();
        for (int i = 0; i < nMatches; i++) {
            String rspName = rspPrefix + Integer.toString(i / 2 + 1);
            theAces.add(new AceBuilder()
                    .setRuleName(String.format("ACE%d", i))
                    .setActions(buildActions(rspName))
                    .setMatches(buildMatches(srcNetwork, dstNetwork, srcLowerPort, dstLowerPort, protocol)).build());
        }

        return theAces;
    }

    private Matches buildMatches(String srcNetwork, String dstNetwork, int srcLowerPort, int dstLowerPort, short protocol) {
        AceIpv4 ipv4  = new AceIpv4Builder()
                .setSourceIpv4Network(new Ipv4Prefix(srcNetwork))
                .setDestinationIpv4Network(new Ipv4Prefix(dstNetwork))
                .build();

        AceIp ip = new AceIpBuilder()
                .setAceIpVersion(ipv4)
                .setProtocol(protocol)
                .setSourcePortRange(new SourcePortRangeBuilder()
                        .setLowerPort(new PortNumber(srcLowerPort))
                        .build())
                .setDestinationPortRange(new DestinationPortRangeBuilder()
                        .setLowerPort(new PortNumber(dstLowerPort))
                        .build())
                .build();

        return new MatchesBuilder()
                .setAceType(ip)
                .build();
    }

    private Actions buildActions(String rspName) {
        Actions1Builder actions1Builder = new Actions1Builder()
                .setSfcAction(new AclRenderedServicePathBuilder()
                        .setRenderedServicePath(rspName).build());

        return new ActionsBuilder()
                .addAugmentation(Actions1.class, actions1Builder.build())
                .build();
    }
}

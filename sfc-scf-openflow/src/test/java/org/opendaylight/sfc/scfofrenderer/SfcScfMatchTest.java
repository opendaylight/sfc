/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;

public class SfcScfMatchTest {

    @Test
    public void testSetPortMatch() {
        Match match;
        match = new SfcScfMatch()
                    .setPortMatch(null)
                    .build();
        Assert.assertNull(match.getInPort());

        NodeConnectorId inPort = new NodeConnectorId("1");
        match = new SfcScfMatch()
                    .setPortMatch(inPort)
                    .build();
        Assert.assertEquals(match.getInPort(), inPort);
    }

    @Test
    public void testSetEthMatch() {
        Match match;
        match = new SfcScfMatch()
                     .setAclMatch(null)
                     .build();
        Assert.assertNull(match.getEthernetMatch());

        AceEth ace;
        Matches matches;

        ace  = new AceEthBuilder()
                    .setSourceMacAddress(null)
                    .setDestinationMacAddress(null)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ace)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();

        Assert.assertNull(match.getEthernetMatch());

        ace  = new AceEthBuilder()
                    .setSourceMacAddress(null)
                    .setDestinationMacAddress(new MacAddress("00:00:00:00:00:01"))
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ace)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();

        Assert.assertNull(match.getEthernetMatch().getEthernetSource());
        Assert.assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress(),
                    new MacAddress("00:00:00:00:00:01"));

        ace  = new AceEthBuilder()
                    .setSourceMacAddress(new MacAddress("00:00:00:00:00:01"))
                    .setDestinationMacAddress(null)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ace)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();

        Assert.assertEquals(match.getEthernetMatch().getEthernetSource().getAddress(),
                    new MacAddress("00:00:00:00:00:01"));
        Assert.assertNull(match.getEthernetMatch().getEthernetDestination());

        ace  = new AceEthBuilder()
                    .setSourceMacAddress(new MacAddress("00:00:00:00:00:01"))
                    .setDestinationMacAddress(new MacAddress("00:00:00:00:00:01"))
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ace)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();

        Assert.assertEquals(match.getEthernetMatch().getEthernetSource().getAddress(),
                    new MacAddress("00:00:00:00:00:01"));
        Assert.assertEquals(match.getEthernetMatch().getEthernetDestination().getAddress(),
                    new MacAddress("00:00:00:00:00:01"));
    }

    @Test
    public void testSetIpMatch() {
        Match match;
        match = new SfcScfMatch()
                     .setAclMatch(null)
                     .build();

        Assert.assertNull(match.getIpMatch());
        Assert.assertNull(match.getLayer3Match());
        Assert.assertNull(match.getLayer4Match());
        Assert.assertNull(match.getIcmpv4Match());
        Assert.assertNull(match.getIcmpv6Match());

        AceIp ip;
        AceIpv4 ipv4;
        Matches matches;

        ipv4  = new AceIpv4Builder()
                    .setSourceIpv4Network(null)
                    .setDestinationIpv4Network(new Ipv4Prefix("1.1.1.1/24"))
                    .build();

        ip = new AceIpBuilder()
                    .setAceIpVersion(ipv4)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ip)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();

        Assert.assertNull(((Ipv4Match)match.getLayer3Match()).getIpv4Source());
        Assert.assertEquals(((Ipv4Match)match.getLayer3Match()).getIpv4Destination(),
                    new Ipv4Prefix("1.1.1.1/24"));

        ipv4  = new AceIpv4Builder()
                    .setSourceIpv4Network(new Ipv4Prefix("1.1.1.1/24"))
                    .setDestinationIpv4Network(null)
                    .build();

        ip = new AceIpBuilder()
                    .setAceIpVersion(ipv4)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ip)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();
        Assert.assertEquals(((Ipv4Match)match.getLayer3Match()).getIpv4Source(),
                    new Ipv4Prefix("1.1.1.1/24"));
        Assert.assertNull(((Ipv4Match)match.getLayer3Match()).getIpv4Destination());

        ipv4  = new AceIpv4Builder()
                    .setSourceIpv4Network(new Ipv4Prefix("1.1.1.1/24"))
                    .setDestinationIpv4Network(new Ipv4Prefix("2.2.2.2/24"))
                    .build();

        ip = new AceIpBuilder()
                    .setAceIpVersion(ipv4)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ip)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();
        Assert.assertEquals(((Ipv4Match)match.getLayer3Match()).getIpv4Source(),
                    new Ipv4Prefix("1.1.1.1/24"));
        Assert.assertEquals(((Ipv4Match)match.getLayer3Match()).getIpv4Destination(),
                    new Ipv4Prefix("2.2.2.2/24"));
    }

    @Test
    public void testSetIpv6Match() {
        Match match;
        match = new SfcScfMatch()
                     .setAclMatch(null)
                     .build();

        Assert.assertNull(match.getIpMatch());
        Assert.assertNull(match.getLayer3Match());
        Assert.assertNull(match.getLayer4Match());
        Assert.assertNull(match.getIcmpv6Match());
        Assert.assertNull(match.getIcmpv6Match());

        AceIp ip;
        AceIpv6 ipv6;
        Matches matches;

        ipv6  = new AceIpv6Builder()
                    .setSourceIpv6Network(null)
                    .setDestinationIpv6Network(new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0001/128"))
                    .build();

        ip = new AceIpBuilder()
                    .setAceIpVersion(ipv6)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ip)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();

        Assert.assertNull(((Ipv6Match)match.getLayer3Match()).getIpv6Source());
        Assert.assertEquals(((Ipv6Match)match.getLayer3Match()).getIpv6Destination(),
                    new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0001/128"));

        ipv6  = new AceIpv6Builder()
                    .setSourceIpv6Network(new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0001/128"))
                    .setDestinationIpv6Network(null)
                    .build();

        ip = new AceIpBuilder()
                    .setAceIpVersion(ipv6)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ip)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();
        Assert.assertEquals(((Ipv6Match)match.getLayer3Match()).getIpv6Source(),
                    new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0001/128"));
        Assert.assertNull(((Ipv6Match)match.getLayer3Match()).getIpv6Destination());

        ipv6  = new AceIpv6Builder()
                    .setSourceIpv6Network(new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0001/128"))
                    .setDestinationIpv6Network(new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0002/128"))
                    .build();

        ip = new AceIpBuilder()
                    .setAceIpVersion(ipv6)
                    .build();

        matches = new MatchesBuilder()
                    .setAceType(ip)
                    .build();

        match = new SfcScfMatch()
                    .setAclMatch(matches)
                    .build();
        Assert.assertEquals(((Ipv6Match)match.getLayer3Match()).getIpv6Source(),
                     new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0001/128"));
        Assert.assertEquals(((Ipv6Match)match.getLayer3Match()).getIpv6Destination(),
                     new Ipv6Prefix("0000:0000:0000:0000:0000:0000:0000:0002/128"));
    }
}

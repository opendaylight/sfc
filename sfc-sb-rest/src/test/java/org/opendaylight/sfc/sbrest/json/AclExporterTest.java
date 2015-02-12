/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceEthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRangeBuilder;

import static org.junit.Assert.*;

/**
 * This class contains unit tests for AclExporterFactory
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-10
 */
public class AclExporterTest {

    public static final String IPV4_TEST_JSON = "/AclJsonStrings/Ipv4Test.json";
    public static final String IPV6_TEST_JSON = "/AclJsonStrings/Ipv6Test.json";
    public static final String ETH_TEST_JSON = "/AclJsonStrings/EthTest.json";
    public static final String NAME_ONLY_JSON = "/AclJsonStrings/NameOnly.json";

    public enum AccessListTestValues {
        ACL_NAME("ACL1"),
        RULE_NAME("ACE1"),
        DESTINATION_IPV4_ADDRESS("127.0.0.1/0"),
        SOURCE_IPV4_ADDRESS("127.0.0.1/0"),
        DESTINATION_IPV6_ADDRESS("abcd:abcd::2222/0"),
        SOURCE_IPV6_ADDRESS("abcd:abcd::2222"),
        FLOW_LABEL("1234"),
        LOWER_PORT("80"),
        UPPER_PORT("80"),
        IP_PROTOCOL("17"),
        DSCP("10"),
        DESTINATION_MAC_ADDRESS("00:11:22:33:44:55"),
        DESTINATION_MAC_ADDRESS_MASK("00:11:22:33:44:55"),
        SOURCE_MAC_ADDRESS("00:11:22:33:44:55"),
        SOURCE_MAC_ADDRESS_MASK("00:11:22:33:44:55"),
        SERVICE_FUNCTION_ACL_RENDERED_SERVICE_PATH("SFC1-SFP1");

        private String value;

        AccessListTestValues(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    private String gatherAccessListJsonStringFromFile(String testFileName) {
        String jsonString = null;

        try {
            URL fileURL = getClass().getResource(testFileName);
            jsonString = TestUtil.readFile(fileURL.toURI(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        for (AccessListTestValues accessListTestValue : AccessListTestValues.values()) {
            jsonString = jsonString.replaceAll("\\b" + accessListTestValue.name() + "\\b", accessListTestValue.getValue());
        }

        return jsonString;
    }

    private boolean testExportAclJson(String accessListType, String expectedResultFile, boolean nameOnly) throws IOException {
        AccessList accessList;
        String exportedAclString;
        AclExporterFactory aclExporterFactory = new AclExporterFactory();

        if (nameOnly) {
            accessList = this.buildAccessListNameOnly();
            exportedAclString = aclExporterFactory.getExporter().exportJsonNameOnly(accessList);
        } else {
            accessList = this.buildAccessList(accessListType);
            exportedAclString = aclExporterFactory.getExporter().exportJson(accessList);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedAclJson = objectMapper.readTree(this.gatherAccessListJsonStringFromFile(expectedResultFile));
        JsonNode exportedAclJson = objectMapper.readTree(exportedAclString);

        return expectedAclJson.equals(exportedAclJson);
    }

    @Test
    public void testExportAclJsonIpv4() throws IOException {
        assertTrue(testExportAclJson(AclExporter.ACE_IPV4, IPV4_TEST_JSON, false));
    }

    @Test
    public void testExportAclJsonIpv6() throws IOException {
        assertTrue(testExportAclJson(AclExporter.ACE_IPV6, IPV6_TEST_JSON, false));
    }

    @Test
    public void testExportAclJsonEth() throws IOException {
        assertTrue(testExportAclJson(AclExporter.ACE_ETH, ETH_TEST_JSON, false));
    }

    @Test
    public void testExportAclJsonNameOnly() throws IOException {
        assertTrue(testExportAclJson(null, NAME_ONLY_JSON, true));
    }

    private AccessList buildAccessListNameOnly() {
        AccessListBuilder accessListBuilder = new AccessListBuilder();
        accessListBuilder.setAclName(AccessListTestValues.ACL_NAME.getValue());
        return accessListBuilder.build();
    }

    private AccessList buildAccessList(String accessListTestType) {
        AccessListBuilder accessListBuilder = new AccessListBuilder();
        accessListBuilder.setAclName(AccessListTestValues.ACL_NAME.getValue());

        List<AccessListEntries> accessListEntriesList = new ArrayList<>();
        accessListBuilder.setAccessListEntries(accessListEntriesList);

        //build access list entry
        accessListEntriesList.add(this.buildAccessListEntries(accessListTestType));

        //build access list
        return accessListBuilder.build();
    }

    private SourcePortRange buildSourcePortRange() {
        SourcePortRangeBuilder sourcePortRangeBuilder = new SourcePortRangeBuilder();
        sourcePortRangeBuilder.setLowerPort(new PortNumber(Integer.parseInt(AccessListTestValues.LOWER_PORT.getValue())));
        sourcePortRangeBuilder.setUpperPort(new PortNumber(Integer.parseInt(AccessListTestValues.UPPER_PORT.getValue())));

        return sourcePortRangeBuilder.build();
    }

    private DestinationPortRange buildDestinationPortRange() {
        DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder();
        destinationPortRangeBuilder.setLowerPort(new PortNumber(Integer.parseInt(AccessListTestValues.LOWER_PORT.getValue())));
        destinationPortRangeBuilder.setUpperPort(new PortNumber(Integer.parseInt(AccessListTestValues.UPPER_PORT.getValue())));

        return destinationPortRangeBuilder.build();
    }

    private Actions buildActions() {
        ActionsBuilder actionsBuilder = new ActionsBuilder();
        Actions1Builder actions1Builder = new Actions1Builder();
        AclRenderedServicePathBuilder aclRenderedServicePathBuilder = new AclRenderedServicePathBuilder();

        aclRenderedServicePathBuilder.setRenderedServicePath(AccessListTestValues.SERVICE_FUNCTION_ACL_RENDERED_SERVICE_PATH.getValue());
        actions1Builder.setSfcAction(aclRenderedServicePathBuilder.build());
        actionsBuilder.addAugmentation(Actions1.class, actions1Builder.build());

        return actionsBuilder.build();
    }

    private AceIp buildAceIp(String accessListTestType) {
        AceIpBuilder aceIpBuilder = new AceIpBuilder();

        aceIpBuilder.setSourcePortRange(this.buildSourcePortRange());
        aceIpBuilder.setDestinationPortRange(this.buildDestinationPortRange());
        aceIpBuilder.setIpProtocol(Short.parseShort(AccessListTestValues.IP_PROTOCOL.getValue()));
        aceIpBuilder.setDscp(new Dscp(Short.parseShort(AccessListTestValues.DSCP.getValue())));

        switch (accessListTestType) {
            case AclExporter.ACE_IPV4:
                aceIpBuilder.setAceIpVersion(this.buildAceIpv4());
                break;
            case AclExporter.ACE_IPV6:
                aceIpBuilder.setAceIpVersion(this.buildAceIpv6());
                break;
        }

        return aceIpBuilder.build();
    }

    private AceIpv4 buildAceIpv4() {
        AceIpv4Builder aceIpv4Builder = new AceIpv4Builder();
        aceIpv4Builder.setDestinationIpv4Address(new Ipv4Prefix(AccessListTestValues.DESTINATION_IPV4_ADDRESS.getValue()));
        aceIpv4Builder.setSourceIpv4Address(new Ipv4Prefix(AccessListTestValues.SOURCE_IPV4_ADDRESS.getValue()));

        return aceIpv4Builder.build();
    }

    private AceIpv6 buildAceIpv6() {
        AceIpv6Builder aceIpv6Builder = new AceIpv6Builder();
        aceIpv6Builder.setDestinationIpv6Address(new Ipv6Prefix(AccessListTestValues.DESTINATION_IPV6_ADDRESS.getValue()));
        aceIpv6Builder.setSourceIpv6Address(new Ipv6Address(AccessListTestValues.SOURCE_IPV6_ADDRESS.getValue()));
        aceIpv6Builder.setFlowLabel(new Ipv6FlowLabel(Long.parseLong(AccessListTestValues.FLOW_LABEL.getValue())));

        return aceIpv6Builder.build();
    }

    private AceEth buildAceEth() {
        AceEthBuilder aceEthBuilder = new AceEthBuilder();
        aceEthBuilder.setDestinationMacAddress(new MacAddress(AccessListTestValues.DESTINATION_MAC_ADDRESS.getValue()));
        aceEthBuilder.setDestinationMacAddressMask(new MacAddress(AccessListTestValues.DESTINATION_MAC_ADDRESS_MASK.getValue()));
        aceEthBuilder.setSourceMacAddress(new MacAddress(AccessListTestValues.SOURCE_MAC_ADDRESS.getValue()));
        aceEthBuilder.setSourceMacAddressMask(new MacAddress(AccessListTestValues.SOURCE_MAC_ADDRESS_MASK.getValue()));

        return aceEthBuilder.build();
    }

    private Matches buildMatches(String accessListTestType) {
        MatchesBuilder matchesBuilder = new MatchesBuilder();

        switch (accessListTestType) {
            case AclExporter.ACE_ETH:
                matchesBuilder.setAceType(this.buildAceEth());
                break;
            case AclExporter.ACE_IPV4:
                matchesBuilder.setAceType(this.buildAceIp(accessListTestType));
                break;
            case AclExporter.ACE_IPV6:
                matchesBuilder.setAceType(this.buildAceIp(accessListTestType));
                break;
        }

        return matchesBuilder.build();
    }

    private AccessListEntries buildAccessListEntries(String accessListTestType) {
        AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();
        accessListEntriesBuilder.setRuleName(AccessListTestValues.RULE_NAME.getValue());

        //build matches
        accessListEntriesBuilder.setMatches(this.buildMatches(accessListTestType));

        //build actions
        accessListEntriesBuilder.setActions(this.buildActions());

        return accessListEntriesBuilder.build();
    }

}

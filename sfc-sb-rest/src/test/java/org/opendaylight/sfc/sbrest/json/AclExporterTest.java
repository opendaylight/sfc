/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;


import java.io.IOException;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRangeBuilder;

import static org.junit.Assert.*;

/**
 * This class contains unit tests for AclExporterFactory
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * <p/>
 * @since 2015-02-10
 */
public class AclExporterTest {

    public static final String ACL_NAME = "ACL1";
    public static final String ACE_NAME = "ACE1";
    public static final String DESTINATION_IPV4_ADDRESS = "127.0.0.1/0";
    public static final String SOURCE_IPV4_ADDRESS = "127.0.0.1/0";
    public static final int LOWER_PORT = 80;
    public static final int UPPER_PORT = 80;
    public static final String RENDERED_SERVICE_PATH_NAME = "SFC1-SFP1";

    public static final String IETF_ACL =
    "{" +
        "\"access-list\": [" +
        "{" +
            "\"acl-name\":\"" + ACL_NAME + "\"," +
            "\"access-list-entries\": [" +
            "{" +
                "\"rule-name\":\"" + ACE_NAME + "\"," +
                "\"matches\": {" +
                    "\"destination-ipv4-address\":\"" + DESTINATION_IPV4_ADDRESS + "\"," +
                    "\"source-ipv4-address\":\"" + SOURCE_IPV4_ADDRESS + "\"," +
                    "\"source-port-range\": {" +
                        "\"upper-port\":" + UPPER_PORT + "," +
                        "\"lower-port\":" + LOWER_PORT +
                    "}" +
                "}," +
                "\"actions\": {" +
                    "\"service-function-acl:rendered-service-path\":\"" + RENDERED_SERVICE_PATH_NAME +
                "\"}" +
            "}" +
            "]" +
        "}" +
        "]" +
    "}";

    public static final String IETF_ACL_NAME_ONLY =
    "{" +
        "\"access-list\": [" +
        "{" +
            "\"acl-name\":\"" + ACL_NAME + "\"" +
        "}]" +
    "}";


    @Test
    public void testExportAclJson() throws IOException {
        AccessList accessList = this.buildAccessList();

        AclExporterFactory aclExporterFactory = new AclExporterFactory();
        String exportedAclString = aclExporterFactory.getExporter().exportJson(accessList);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedAclJson = objectMapper.readTree(IETF_ACL);
        JsonNode exportedAclJson = objectMapper.readTree(exportedAclString);

        assertTrue(expectedAclJson.equals(exportedAclJson));
    }

    @Test
    public void testExportAclJsonNameOnly() throws IOException {
        AccessList accessList = this.buildAccessList();

        AclExporterFactory aclExporterFactory = new AclExporterFactory();
        String exportedAclString = aclExporterFactory.getExporter().exportJsonNameOnly(accessList);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedAclJson = objectMapper.readTree(IETF_ACL_NAME_ONLY);
        JsonNode exportedAclJson = objectMapper.readTree(exportedAclString);

        assertTrue(expectedAclJson.equals(exportedAclJson));
    }

    private AccessList buildAccessList () {
        AccessListBuilder accessListBuilder = new AccessListBuilder();
        accessListBuilder.setAclName(ACL_NAME);

        List<AccessListEntries> accessListEntriesList = new ArrayList<>();
        accessListBuilder.setAccessListEntries(accessListEntriesList);

        AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();
        accessListEntriesBuilder.setRuleName(ACE_NAME);

        //build matches
        MatchesBuilder matchesBuilder = new MatchesBuilder();
        AceIpBuilder aceIpBuilder = new AceIpBuilder();

        AceIpv4Builder aceIpv4Builder = new AceIpv4Builder();
        aceIpv4Builder.setDestinationIpv4Address(new Ipv4Prefix(DESTINATION_IPV4_ADDRESS));
        aceIpv4Builder.setSourceIpv4Address(new Ipv4Prefix(SOURCE_IPV4_ADDRESS));
        aceIpBuilder.setAceIpVersion(aceIpv4Builder.build());

        SourcePortRangeBuilder sourcePortRangeBuilder = new SourcePortRangeBuilder();
        sourcePortRangeBuilder.setLowerPort(new PortNumber(LOWER_PORT));
        sourcePortRangeBuilder.setUpperPort(new PortNumber(UPPER_PORT));
        aceIpBuilder.setSourcePortRange(sourcePortRangeBuilder.build());

        matchesBuilder.setAceType(aceIpBuilder.build());
        accessListEntriesBuilder.setMatches(matchesBuilder.build());

        //build actions
        ActionsBuilder actionsBuilder = new ActionsBuilder();
        Actions1Builder actions1Builder = new Actions1Builder();
        AclRenderedServicePathBuilder aclRenderedServicePathBuilder = new AclRenderedServicePathBuilder();

        aclRenderedServicePathBuilder.setRenderedServicePath(RENDERED_SERVICE_PATH_NAME);
        actions1Builder.setSfcAction(aclRenderedServicePathBuilder.build());
        actionsBuilder.addAugmentation(Actions1.class, actions1Builder.build());

        accessListEntriesBuilder.setActions(actionsBuilder.build());

        //build access list entry
        accessListEntriesList.add(accessListEntriesBuilder.build());

        //build access list
        return accessListBuilder.build();
    }

}

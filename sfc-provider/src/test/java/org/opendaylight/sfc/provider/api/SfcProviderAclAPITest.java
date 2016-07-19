/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.AccessListsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.AccessListStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.AccessListStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.state.access.list.state.AclServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * This class contains unit tests for SfcProviderAclAPI
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-06-19
 */

public class SfcProviderAclAPITest extends AbstractDataStoreManager {

    private final String ACL_NAME = "aclName";
    private final java.lang.Class<? extends AclBase> ACL_TYPE = Ipv4Acl.class;
    private final String CLASSIFIER_NAME = "classifier";

    private final List<String> IP_V4_ADDRESS = new ArrayList<String>(){

        {
            add("192.168.1.");
            add("10.66.20.");
        }
    };

    private final List<String> IP_PREFIX = new ArrayList<String>(){

        {
            add("/16");
            add("/32");
            add("/48");
        }
    };

    private final List<String> IP_V6_ADDRESS = new ArrayList<String>(){

        {
            add("12:34:56:78:90:AB:AD:E");
            add("12:34:56:78:90:AB:AD:E");
        }
    };

    @Before
    public void init() {
        setOdlSfc();
    }

    @Test
    //read existing access list from data store
    public void testReadAccessList() throws Exception {


        //create Access List with entries and IID, then write transaction to data store
        AclBuilder aclBuilder = new AclBuilder();
        aclBuilder.setAclName(ACL_NAME)
                .setKey(new AclKey(ACL_NAME, ACL_TYPE))
                .setAccessListEntries(createAccessListEntries());

        InstanceIdentifier<Acl> aclIID = InstanceIdentifier.builder(AccessLists.class)
                .child(Acl.class, new AclKey(ACL_NAME, ACL_TYPE)).build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(aclIID, aclBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        assertTrue("must be true", transactionSuccessful);

        //read access list from data store
        Acl accessList = SfcProviderAclAPI.readAccessList(ACL_NAME, ACL_TYPE);

        assertNotNull("Must not be null", accessList);
        assertNotNull("Must not be null", accessList.getAccessListEntries());
        assertNotNull("Must not be null", accessList.getAccessListEntries().getAce());
        assertEquals("Must be equal", accessList.getAclName(), ACL_NAME);
        assertEquals("Must be equal", accessList.getKey().getAclName(), ACL_NAME);
        assertEquals("Must be equal", accessList.getAccessListEntries().getAce().size(), 4);
        assertTrue("Must be equal", accessList.getAccessListEntries().getAce().get(0).equals(createAccessListEntries().getAce().get(0)));
        assertTrue("Must be equal", accessList.getAccessListEntries().getAce().get(1).equals(createAccessListEntries().getAce().get(1)));
        assertTrue("Must be equal", accessList.getAccessListEntries().getAce().get(2).equals(createAccessListEntries().getAce().get(2)));
        assertTrue("Must be equal", accessList.getAccessListEntries().getAce().get(3).equals(createAccessListEntries().getAce().get(3)));

        //delete transaction
        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(aclIID, LogicalDatastoreType.CONFIGURATION);

        assertTrue("must be true", transactionSuccessful);
    }

    @Test
    //read existing access list from data store
    public void testReadAccessListState() throws Exception {

        //create Access List state and IID, then write transaction to data store
        String ACL_STATE_NAME = "aclStateName";
        AccessListStateBuilder accessListStateBuilder = new AccessListStateBuilder();
        accessListStateBuilder.setAclName(ACL_STATE_NAME)
                .setKey(new AccessListStateKey(ACL_STATE_NAME, ACL_TYPE))
                .setAclServiceFunctionClassifier(createAclServiceFunctionClassifier());

        InstanceIdentifier<AccessListState> aclStateIID = InstanceIdentifier.builder(AccessListsState.class)
                .child(AccessListState.class, new AccessListStateKey(ACL_STATE_NAME, ACL_TYPE)).build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(aclStateIID, accessListStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);

        assertTrue("must be true", transactionSuccessful);

        //read access list state from data store
        AccessListState accessListState = SfcProviderAclAPI.readAccessListState(ACL_STATE_NAME, ACL_TYPE);

        assertNotNull("Must not be null", accessListState);
        assertEquals("Must be equal", accessListState.getAclName(), ACL_STATE_NAME);
        assertEquals("Must be equal", accessListState.getKey().getAclName(), ACL_STATE_NAME);
        assertEquals("Must be equal", accessListState.getAclServiceFunctionClassifier().get(0).getName(), CLASSIFIER_NAME);

        //delete transaction
        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(aclStateIID, LogicalDatastoreType.OPERATIONAL);

        assertTrue("must be true", transactionSuccessful);
    }

    @Test
    //add classifier to access list
    public void testAddAndDeleteClassifier() throws Exception {

        //add classifier
        boolean result = SfcProviderAclAPI.addClassifierToAccessListState(ACL_NAME, ACL_TYPE, CLASSIFIER_NAME);

        assertTrue("Must be true", result);

        //delete classifier
        result = SfcProviderAclAPI.deleteClassifierFromAccessListState(ACL_NAME, ACL_TYPE, CLASSIFIER_NAME);

        assertTrue("Must be true", result);
    }

    //create classifier list
    private List<AclServiceFunctionClassifier> createAclServiceFunctionClassifier() {
        List<AclServiceFunctionClassifier> aclServiceFunctionClassifierList = new ArrayList<>();

        AclServiceFunctionClassifierBuilder aclServiceFunctionClassifierBuilder = new AclServiceFunctionClassifierBuilder();
        aclServiceFunctionClassifierBuilder.setName(CLASSIFIER_NAME);
        aclServiceFunctionClassifierList.add(aclServiceFunctionClassifierBuilder.build());

        return aclServiceFunctionClassifierList;
    }

    //create entries for access list
    private AccessListEntries createAccessListEntries() {
        String ACE_RULE_NAME = "aceRule";
        AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();
        List<Ace> aceList = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {

            //build actions
            PermitBuilder permitBuilder = new PermitBuilder();
            permitBuilder.setPermit(true);

            ActionsBuilder actionsBuilder = new ActionsBuilder();
            actionsBuilder.setPacketHandling(permitBuilder.build());

            //build matches
            //create src & dst port ranges
            SourcePortRangeBuilder sourcePortRangeBuilder = new SourcePortRangeBuilder();
            sourcePortRangeBuilder.setLowerPort(new PortNumber(1000 + i))
                    .setUpperPort(new PortNumber(2000 + i));
            DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder();
            destinationPortRangeBuilder.setLowerPort(new PortNumber(3000 + i))
                    .setUpperPort(new PortNumber(4000 + i));

            //build ip ace
            AceIpBuilder aceIpBuilder = new AceIpBuilder();
            aceIpBuilder.setDscp(new Dscp((short) i))
                    .setSourcePortRange(sourcePortRangeBuilder.build())
                    .setDestinationPortRange(destinationPortRangeBuilder.build())
                    .setDscp(new Dscp((short) i));

            if (i <= 2) {
                //create ip address & prefix
                AceIpv4Builder aceIpv4Builder = new AceIpv4Builder();
                aceIpv4Builder.setSourceIpv4Network(new Ipv4Prefix(IP_V4_ADDRESS.get(0) + i + IP_PREFIX.get(0)))
                        .setDestinationIpv4Network(new Ipv4Prefix(IP_V4_ADDRESS.get(1) + i + IP_PREFIX.get(1)));
                aceIpBuilder.setAceIpVersion(aceIpv4Builder.build())
                        .setProtocol((short) 4);

            } else {
                //create ip address & prefix
                AceIpv6Builder aceIpv6Builder = new AceIpv6Builder();
                aceIpv6Builder.setSourceIpv6Network(new Ipv6Prefix(IP_V6_ADDRESS.get(0) + i + IP_PREFIX.get(1)))
                        .setDestinationIpv6Network(new Ipv6Prefix(IP_V6_ADDRESS.get(1) + i + IP_PREFIX.get(2)));
                aceIpBuilder.setAceIpVersion(aceIpv6Builder.build())
                        .setProtocol((short) 41);
            }

            MatchesBuilder matchesBuilder = new MatchesBuilder();
            matchesBuilder.setInputInterface("interface-" + i)
                    .setAceType(aceIpBuilder.build());

            //set matches and actions
            AceBuilder ace = new AceBuilder();
            ace.setRuleName(ACE_RULE_NAME + i)
                    .setActions(actionsBuilder.build())
                    .setMatches(matchesBuilder.build());

            aceList.add(ace.build());
        }

        accessListEntriesBuilder.setAce(aceList);

        return accessListEntriesBuilder.build();
    }
}

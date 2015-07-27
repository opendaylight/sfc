package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessListsState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntriesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListStateKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.timerange.AbsoluteBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.*;

/**
 * This class contains unit tests for SfcProviderAclAPI
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-06-19
 */

public class SfcProviderAclAPITest extends AbstractDataStoreManager {

    private final String ACL_NAME = "aclName";
    private final String CLASSIFIER_NAME = "classifier";
    private final String[] IP_V4_ADDRESS = {"192.168.1.", "10.66.20."};
    private final String[] IP_PREFIX = {"/16", "/32", "/48"};
    private final String[] IP_V6_ADDRESS = {"12:34:56:78:90:AB:AD:E", "12:34:56:78:90:AB:AD:E"};

    @Before
    public void init() {
        setOdlSfc();
    }

    @Test
    //tes SfcProviderAclAPI constructor
    public void constructorTest() {
        String methodName = "test string";
        Object[] params = {methodName};
        SfcProviderAclAPI sfcProviderAclAPI = new SfcProviderAclAPI(params, methodName);

        assertNotNull("Must not be null", sfcProviderAclAPI);
        assertEquals("Must be equal", sfcProviderAclAPI.getMethodName(), methodName);
        assertEquals("Must be  equal", sfcProviderAclAPI.getParameters().getClass(), Object[].class);
    }

    @Test
    //read existing access list from data store
    public void testReadAccessList() throws Exception {

        //create Access List with entries and IID, then write transaction to data store
        AccessListBuilder accessListBuilder = new AccessListBuilder();
        accessListBuilder.setAclName(ACL_NAME)
                .setKey(new AccessListKey(ACL_NAME))
                .setAccessListEntries(createAccessListEntries());

        InstanceIdentifier<AccessList> aclIID = InstanceIdentifier.builder(AccessLists.class)
                .child(AccessList.class, new AccessListKey(ACL_NAME)).build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(aclIID, accessListBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        assertTrue("must be true", transactionSuccessful);

        //read access list from data store
        AccessList accessList = SfcProviderAclAPI.readAccessListExecutor(ACL_NAME);

        assertNotNull("Must not be null", accessList);
        assertEquals("Must be equal", accessList.getAclName(), ACL_NAME);
        assertEquals("Must be equal", accessList.getKey().getAclName(), ACL_NAME);
        assertEquals("Must be equal", accessList.getAccessListEntries().size(), 4);
        assertTrue("Must be equal", accessList.getAccessListEntries().get(0).equals(createAccessListEntries().get(0)));
        assertTrue("Must be equal", accessList.getAccessListEntries().get(1).equals(createAccessListEntries().get(1)));
        assertTrue("Must be equal", accessList.getAccessListEntries().get(2).equals(createAccessListEntries().get(2)));
        assertTrue("Must be equal", accessList.getAccessListEntries().get(3).equals(createAccessListEntries().get(3)));

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
                .setKey(new AccessListStateKey(ACL_STATE_NAME))
                .setAclServiceFunctionClassifier(createAclServiceFunctionClassifier());

        InstanceIdentifier<AccessListState> aclStateIID = InstanceIdentifier.builder(AccessListsState.class)
                .child(AccessListState.class, new AccessListStateKey(ACL_STATE_NAME)).build();

        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(aclStateIID, accessListStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);

        assertTrue("must be true", transactionSuccessful);

        //read access list state from data store
        AccessListState accessListState = SfcProviderAclAPI.readAccessListStateExecutor(ACL_STATE_NAME);

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
        boolean result = SfcProviderAclAPI.addClassifierToAccessListStateExecutor(ACL_NAME, CLASSIFIER_NAME);

        assertTrue("Must be true", result);

        //delete classifier
        result = SfcProviderAclAPI.deleteClassifierFromAccessListStateExecutor(ACL_NAME, CLASSIFIER_NAME);

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
    private List<AccessListEntries> createAccessListEntries() {
        String ACE_RULE_NAME = "aceRule";
        List<AccessListEntries> accessListEntriesList = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();

            //build actions
            PermitBuilder permitBuilder = new PermitBuilder();
            permitBuilder.setPermit(true);

            ActionsBuilder actionsBuilder = new ActionsBuilder();
            actionsBuilder.setPacketHandling(permitBuilder.build());

            AbsoluteBuilder absoluteBuilder = new AbsoluteBuilder();
            absoluteBuilder.setActive(true);

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
                aceIpv4Builder.setSourceIpv4Address(new Ipv4Prefix(IP_V4_ADDRESS[0] + i + IP_PREFIX[0]))
                        .setDestinationIpv4Address(new Ipv4Prefix(IP_V4_ADDRESS[1] + i + IP_PREFIX[1]));
                aceIpBuilder.setAceIpVersion(aceIpv4Builder.build())
                        .setIpProtocol((short) 4);

            } else {
                //create ip address & prefix
                AceIpv6Builder aceIpv6Builder = new AceIpv6Builder();
                aceIpv6Builder.setSourceIpv6Address(new Ipv6Address(IP_V6_ADDRESS[0] + i))
                        .setDestinationIpv6Address(new Ipv6Prefix(IP_V6_ADDRESS[1] + i + IP_PREFIX[2]));
                aceIpBuilder.setAceIpVersion(aceIpv6Builder.build())
                        .setIpProtocol((short) 41);
            }

            MatchesBuilder matchesBuilder = new MatchesBuilder();
            matchesBuilder.setInputInterface("interface-" + i)
                    .setAceType(aceIpBuilder.build());

            //set matches and actions for ipv6
            accessListEntriesBuilder.setRuleName(ACE_RULE_NAME + i)
                    .setKey(new AccessListEntriesKey(ACE_RULE_NAME + i))
                    .setActions(actionsBuilder.build())
                    .setMatches(matchesBuilder.build());

            accessListEntriesList.add(accessListEntriesBuilder.build());
        }

        return accessListEntriesList;
    }
}
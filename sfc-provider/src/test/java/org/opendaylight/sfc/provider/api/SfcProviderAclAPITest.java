package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessListsState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListStateKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcDataStoreAPI.class)
public class SfcProviderAclAPITest extends AbstractDataBrokerTest {

    private static final String ACL_NAME = "aclName";
    private static final String ACL_STATE_NAME = "aclStateName";
    private static final String ACL_RULE_NAME = "aclRule";
    private static final String INPUT_INTERFACE = "interface-";
    private static final String CLASSIFIER_NAME = "classifier-";
    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    @Before
    public void init() {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
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
        assertEquals("Must be equal", accessList.getAccessListEntries().get(0).getRuleName(), ACL_RULE_NAME + 1);
        assertEquals("Must be equal", accessList.getAccessListEntries().get(2).getMatches().getInputInterface(), INPUT_INTERFACE + 3);

        //delete transaction
        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(aclIID, LogicalDatastoreType.CONFIGURATION);

        assertTrue("must be true", transactionSuccessful);
    }

    @Test
    //read existing access list from data store
    public void testReadAccessListState() throws Exception {

        //create Access List state and IID, then write transaction to data store
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
        assertEquals("Must be equal", accessListState.getAclServiceFunctionClassifier().get(2).getName(), CLASSIFIER_NAME + 1);

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

        //test unsuccessful transactions
        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "writeMergeTransactionAPI")).toReturn(false);

        result = SfcProviderAclAPI.addClassifierToAccessListStateExecutor(ACL_NAME, CLASSIFIER_NAME);

        assertFalse("Must be false", result);

        PowerMockito.stub(PowerMockito.method(SfcDataStoreAPI.class, "deleteTransactionAPI")).toReturn(false);

        result = SfcProviderAclAPI.deleteClassifierFromAccessListStateExecutor(ACL_NAME, CLASSIFIER_NAME);

        assertFalse("Must be false", result);
    }

    //create classifier list
    private List<AclServiceFunctionClassifier> createAclServiceFunctionClassifier() {
        List<AclServiceFunctionClassifier> aclServiceFunctionClassifierList = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            AclServiceFunctionClassifierBuilder aclServiceFunctionClassifierBuilder = new AclServiceFunctionClassifierBuilder();
            aclServiceFunctionClassifierBuilder.setName(CLASSIFIER_NAME + i);
            aclServiceFunctionClassifierList.add(aclServiceFunctionClassifierBuilder.build());
        }

        return aclServiceFunctionClassifierList;
    }

    //create entries for access list
    private List<AccessListEntries> createAccessListEntries() {
        List<AccessListEntries> accessListEntriesList = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();

            //build packet handling for actions
            PermitBuilder permitBuilder = new PermitBuilder();
            permitBuilder.setPermit(true);

            ActionsBuilder actionsBuilder = new ActionsBuilder();
            actionsBuilder.setPacketHandling(permitBuilder.build());

            //build ace ip for matches
            AceIpBuilder aceIpBuilder = new AceIpBuilder();
            aceIpBuilder.setDscp(new Dscp((short) i));

            MatchesBuilder matchesBuilder = new MatchesBuilder();
            matchesBuilder.setInputInterface(INPUT_INTERFACE + i);
            matchesBuilder.setAceType(aceIpBuilder.build());

            //set matches and actions
            accessListEntriesBuilder.setRuleName(ACL_RULE_NAME + i)
                    .setActions(actionsBuilder.build())
                    .setMatches(matchesBuilder.build());
            accessListEntriesList.add(accessListEntriesBuilder.build());
        }

        return accessListEntriesList;
    }
}
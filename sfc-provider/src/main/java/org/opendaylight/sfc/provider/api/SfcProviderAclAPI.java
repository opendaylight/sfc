/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessListsState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.state.AccessListStateKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.state.access.list.state.AclServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.state.access.list.state.AclServiceFunctionClassifierKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class has the APIs to operate on the ACL
 * datastore.
 * <p>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Andrej Kincel (akincel@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 * <p>
 * @since 2014-11-04
 */
public class SfcProviderAclAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderAclAPI.class);

    /**
     * This method reads a Access List from DataStore
     * <p>
     * @param accessListName Access List name
     * @return ACL object or null if not found
     */
    public static Acl readAccessList(String accessListName) {
        printTraceStart(LOG);
        Acl acl;
        InstanceIdentifier<Acl> aclIID;
        AclKey aclKey = new AclKey(accessListName);
        aclIID = InstanceIdentifier.builder(AccessLists.class)
                .child(Acl.class, aclKey).build();

        acl = SfcDataStoreAPI.readTransactionAPI(aclIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return acl;
    }


    /**
     * This method reads a Access List state from Operational DataStore
     * <p>
     * @param accessListName Access List name
     * @return ACL state object or null if not found
     */
    public static AccessListState readAccessListState(String accessListName) {
        printTraceStart(LOG);
        AccessListState aclState;
        InstanceIdentifier<AccessListState> aclStateIID;
        AccessListStateKey accessListStateKey = new AccessListStateKey(accessListName);
        aclStateIID = InstanceIdentifier.builder(AccessListsState.class)
                .child(AccessListState.class, accessListStateKey).build();

        aclState = SfcDataStoreAPI.readTransactionAPI(aclStateIID, LogicalDatastoreType.OPERATIONAL);

        printTraceStop(LOG);
        return aclState;
    }


    /**
     * Adds Classifier to Access List state
     * <p>
     * @param accessListName Access List name
     * @param serviceClassifierName Service Classifier name
     * @return true if success.
     */
    public static boolean addClassifierToAccessListState (String accessListName, String serviceClassifierName) {

        printTraceStart(LOG);
        InstanceIdentifier<AclServiceFunctionClassifier> aclIID;
        boolean ret = false;

        AclServiceFunctionClassifierBuilder aclServiceClassifierBuilder = new AclServiceFunctionClassifierBuilder();
        AclServiceFunctionClassifierKey aclServiceClassifierKey = new AclServiceFunctionClassifierKey(serviceClassifierName);
        aclServiceClassifierBuilder.setKey(aclServiceClassifierKey).setName(serviceClassifierName);

        AccessListStateKey accessListStateKey = new AccessListStateKey(accessListName);

        aclIID = InstanceIdentifier.builder(AccessListsState.class)
                .child(AccessListState.class, accessListStateKey)
                .child(AclServiceFunctionClassifier.class, aclServiceClassifierKey).toInstance();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(aclIID, aclServiceClassifierBuilder.build(),
                LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to create Access List {} state. Service Function CLassifier: {}",
                    Thread.currentThread().getStackTrace()[1], accessListName, serviceClassifierName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Deletes Classifier from Access List state
     * <p>
     * @param accessListName Access List name
     * @param serviceClassifierName Service Classifier name
     * @return true if success.
     */
    public static boolean deleteClassifierFromAccessListState (String accessListName, String serviceClassifierName) {

        printTraceStart(LOG);
        InstanceIdentifier<AclServiceFunctionClassifier> aclIID;
        boolean ret = false;

        AclServiceFunctionClassifierKey aclServiceClassifierKey = new AclServiceFunctionClassifierKey(serviceClassifierName);

        AccessListStateKey accessListStateKey = new AccessListStateKey(accessListName);

        aclIID = InstanceIdentifier.builder(AccessListsState.class)
                .child(AccessListState.class, accessListStateKey)
                .child(AclServiceFunctionClassifier.class, aclServiceClassifierKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(aclIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to delete Access List {} state. Service Function CLassifier: {}",
                    Thread.currentThread().getStackTrace()[1], accessListName, serviceClassifierName);
        }
        printTraceStop(LOG);
        return ret;
    }
}

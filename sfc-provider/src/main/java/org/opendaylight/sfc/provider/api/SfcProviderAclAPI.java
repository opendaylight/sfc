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


import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessListsState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.AccessListStateKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.state.access.list.state.AclServiceFunctionClassifierKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * This class has the APIs to operate on the ACL
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Andrej Kincel (akincel@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 * <p/>
 * <p/>
 * <p/>
 * @since 2014-11-04
 */
public class SfcProviderAclAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderAclAPI.class);
    private static final String FAILED_TO_STR = "failed to ...";

    SfcProviderAclAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderAclAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderAclAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "putAcl");
    }

    public static SfcProviderAclAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "readAcl");
    }

    public static SfcProviderAclAPI getReadAccessListState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "readAccessListState");
    }

    public static SfcProviderAclAPI getAddClassifierToAccessListStateExecutor(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "addClassifierToAccessListState");
    }

    public static SfcProviderAclAPI getDeleteClassifierFromAccessListStateExecutor(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "deleteClassifierFromAccessListState");
    }

    @Deprecated
    public static SfcProviderAclAPI getSetAclEntriesSfcAction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "setAclEntriesSfcAction");
    }

    protected boolean putAcl(AccessList acl) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<AccessList> aclEntryIID =
                    InstanceIdentifier.builder(AccessLists.class)
                            .child(AccessList.class, acl.getKey())
                            .toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    aclEntryIID, acl, true);
            writeTx.commit();
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected AccessList readAcl(String aclName) {
        printTraceStart(LOG);
        AccessList acl = null;
        InstanceIdentifier<AccessList> aclIID;
        AccessListKey aclKey =
                new AccessListKey(aclName);
        aclIID = InstanceIdentifier.builder(AccessLists.class)
                .child(AccessList.class, aclKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<AccessList> aclDataObject;
            try {
                aclDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, aclIID).get();
                if (aclDataObject != null
                        && aclDataObject.isPresent()) {
                    acl = aclDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read ACL " +
                        "configuration {}", aclName);
            }
        }
        printTraceStop(LOG);
        return acl;
    }

    /**
     * This method reads a Access List state from Operational DataStore
     * <p>
     * @param accessListName Access List name
     * @return ACL state object or null if not found
     */
    @SuppressWarnings("unused")
    @SfcReflection
    protected AccessListState readAccessListState(String accessListName) {
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
     * Wrapper API to read access list state from Operational Datastore
     * <p>
     * @param accessListName Access List name
     * @return an AccessListState object that is a list of all classifiers using
     * this access list, null otherwise
     */
    public static AccessListState readAccessListStateExecutor(String accessListName) {

        printTraceStart(LOG);
        AccessListState ret = null;
        Object[] functionParamsObj = {accessListName};
        Class[] functionParamsClass = {String.class};
        Future future  = ODL_SFC.getExecutor().submit(SfcProviderAclAPI
                .getReadAccessListState(functionParamsObj, functionParamsClass));
        try {
            ret = (AccessListState) future.get();
            LOG.debug("getReadAccessListState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Adds Classifier to Access List state
     * <p>
     * @param accessListName Access List name
     * @param serviceClassifierName Service Classifier name
     * @return true if success.
     */
    @SuppressWarnings("unused")
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
     * Wrapper API used to add Classifier to Access List state
     * <p>
     * @param accessListName Service Function Classifier name
     * @param serviceClassifierName Rendered Path name
     * @return true if success.
     */
    @SuppressWarnings("unused")
    public static boolean addClassifierToAccessListStateExecutor (String accessListName, String serviceClassifierName) {

        printTraceStart(LOG);
        boolean ret = true;
        Object[] functionParams = {accessListName, serviceClassifierName};
        Class[] functionParamsTypes = {String.class, String.class};
        Future future = ODL_SFC.getExecutor().submit(SfcProviderAclAPI
                .getAddClassifierToAccessListStateExecutor(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddClassifierToAccessListStateExecutor returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
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
    @SuppressWarnings("unused")
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

    /**
     * Wrapper API used to delete Classifier from Access List state
     * <p>
     * @param accessListName Service Function Classifier name
     * @param serviceClassifierName Rendered Path name
     * @return true if success.
     */
    @SuppressWarnings("unused")
    public static boolean deleteClassifierFromAccessListStateExecutor (String accessListName, String serviceClassifierName) {

        printTraceStart(LOG);
        boolean ret = true;
        Object[] functionParams = {accessListName, serviceClassifierName};
        Class[] functionParamsTypes = {String.class, String.class};
        Future future = ODL_SFC.getExecutor().submit(SfcProviderAclAPI
                .getDeleteClassifierFromAccessListStateExecutor(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteClassifierFromAccessListStateExecutor returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method updates the AclEntries sfcAction to
     * the target ServiceFunctionPath name.
     * <p/>
     *
     * @param aclName Access List name
     * @param sfpName Service Function Path name
     * @return Nothing.
     */
    @Deprecated
    protected void setAclEntriesSfcAction(String aclName, String sfpName) {
        AccessList accessList = null;
        accessList = this.readAcl(aclName);

        if (accessList != null) {
            ArrayList<AccessListEntries> aceArrayList = new ArrayList<>();
            aceArrayList.addAll(accessList.getAccessListEntries());

            for (AccessListEntries ace : aceArrayList) {
                AclRenderedServicePathBuilder aclRenderedServicePathBuilder = new AclRenderedServicePathBuilder();
                if (sfpName != null) {
                    aclRenderedServicePathBuilder.setRenderedServicePath(sfpName);
                } else {
                    aclRenderedServicePathBuilder.setRenderedServicePath("");
                }

                Actions1Builder actions1Builder = new Actions1Builder();
                actions1Builder.setSfcAction(aclRenderedServicePathBuilder.build());

                ActionsBuilder actionsBuilder = new ActionsBuilder();
                actionsBuilder.addAugmentation(Actions1.class, actions1Builder.build());

                AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder(ace);
                accessListEntriesBuilder.setActions(actionsBuilder.build());

                InstanceIdentifier<AccessListEntries> aceIID = InstanceIdentifier.builder(AccessLists.class)
                        .child(AccessList.class, accessList.getKey())
                        .child(AccessListEntries.class, ace.getKey()).build();

                WriteTransaction aceWriteTx = ODL_SFC.getDataProvider().newWriteOnlyTransaction();
                aceWriteTx.merge(LogicalDatastoreType.CONFIGURATION,
                        aceIID, accessListEntriesBuilder.build(), true);
                aceWriteTx.commit();
            }
        }
    }
}

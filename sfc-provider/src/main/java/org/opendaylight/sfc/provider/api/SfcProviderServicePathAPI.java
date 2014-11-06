/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.SfcProviderRestAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf
        .rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf
        .rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf
        .rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf
        .rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc
        .rev140701.ServiceFunctionChainsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc
        .rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc
        .rev140701.service.function.chain.grouping.service.function.chain
        .SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc
        .rev140701.service.function.chains.state.ServiceFunctionChainState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc
        .rev140701.service.function.chains.state
        .ServiceFunctionChainStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc
        .rev140701.service.function.chains.state.ServiceFunctionChainStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp
        .rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp
        .rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp
        .rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp
        .rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp
        .rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp
        .rev140701.service.function.paths.service.function.path
        .ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft
        .rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft
        .rev140701.service.function.types.service.function.type
        .SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.ActionsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfpEntryDataListener
 * <p/>
 * <p/>
 * <p/>
 * @since 2014-06-30
 */
public class SfcProviderServicePathAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);
    static final Comparator<SfcServiceFunction> SF_ORDER =
            new Comparator<SfcServiceFunction>() {
                public int compare(SfcServiceFunction e1, SfcServiceFunction e2) {
                    return e2.getOrder().compareTo(e1.getOrder());
                }
            };

    SfcProviderServicePathAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "putServiceFunctionPath");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "readServiceFunctionPath");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServiceFunctionPath");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "putAllServiceFunctionPaths");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "readAllServiceFunctionPaths");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteAllServiceFunctionPaths");
    }

    public static SfcProviderServicePathAPI getDeleteServicePathContainingFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathContainingFunction");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDeleteServicePathInstantiatedFromChain(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathInstantiatedFromChain");
    }

    public static SfcProviderServicePathAPI getCreateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createServiceFunctionPathEntry");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getUpdateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServiceFunctionPathEntry");
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getUpdateServicePathInstantiatedFromChain(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathInstantiatedFromChain");
    }

    public static SfcProviderServicePathAPI getUpdateServicePathContainingFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathContainingFunction");
    }

    @SuppressWarnings("unused")
    public static int numCreatedPathGetValue() {
        return numCreatedPath.get();
    }

    public int numCreatedPathIncrementGet() {
        return numCreatedPath.incrementAndGet();
    }

    @SuppressWarnings("unused")
    public int numCreatedPathDecrementGet() {
        return numCreatedPath.decrementAndGet();
    }

    @SuppressWarnings("unused")
    protected boolean putServiceFunctionPath(ServiceFunctionPath sfp) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionPath> sfpEntryIID =
                    InstanceIdentifier.builder(ServiceFunctionPaths.class).
                            child(ServiceFunctionPath.class, sfp.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sfpEntryIID, sfp, true);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionPath readServiceFunctionPath(String serviceFunctionPathName) {
        printTraceStart(LOG);
        ServiceFunctionPath sfp = null;
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionPath> serviceFunctionPathDataObject = null;
            try {
                serviceFunctionPathDataObject = readTx.read(LogicalDatastoreType
                        .CONFIGURATION, sfpIID).get();
                if (serviceFunctionPathDataObject != null
                        && serviceFunctionPathDataObject.isPresent()) {
                    sfp = serviceFunctionPathDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Path configuration data \n");
            }
        }
        printTraceStop(LOG);
        return sfp;
    }

    protected boolean deleteServiceFunctionPath(String serviceFunctionPathName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).toInstance();

        if (dataBroker != null) {
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sfpEntryIID);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected boolean putAllServiceFunctionPaths(ServiceFunctionPaths sfps) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.
                    builder(ServiceFunctionPaths.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sfpsIID, sfps);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionPaths readAllServiceFunctionPaths() {
        ServiceFunctionPaths sfps = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionPaths> serviceFunctionPathsDataObject = null;
            try {
                serviceFunctionPathsDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpsIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read top-level Service Function Path " +
                        "container \n");
            }
            if (serviceFunctionPathsDataObject != null
                    && serviceFunctionPathsDataObject.isPresent()) {
                sfps = serviceFunctionPathsDataObject.get();
            }
        }
        printTraceStop(LOG);
        return sfps;
    }

    protected boolean deleteAllServiceFunctionPaths() {
        boolean ret = false;
        printTraceStart(LOG);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                    InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sfpsIID);
            writeTx.commit();
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /* Today A Service Function Chain modification is catastrophic. We delete all Paths
     * and recreate them. Maybe a real patch is possible but given the complexities of the possible
     * modifications, this is the safest approach.
     */
    @SuppressWarnings("unused")
    private void updateServicePathInstantiatedFromChain(ServiceFunctionPath serviceFunctionPath) {
        deleteServicePathInstantiatedFromChain(serviceFunctionPath);
        createServiceFunctionPathEntry(serviceFunctionPath);
    }

    // TODO:Needs change
    private void deleteServicePathInstantiatedFromChain(ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);
        ServiceFunctionChain serviceFunctionChain = null;
        String serviceChainName = serviceFunctionPath.getServiceChainName();
        try {
            serviceFunctionChain = serviceChainName != null ?
                    (ServiceFunctionChain) odlSfc.executor
                            .submit(SfcProviderServiceChainAPI.getRead(
                                    new Object[]{serviceChainName},
                                    new Class[]{String.class})).get()
                    : null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("\n Could not read Service Function Chain configuration");
        }
        if (serviceFunctionChain == null) {
            LOG.error("\n########## ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }


        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionChainState serviceFunctionChainState;
        ServiceFunctionChainStateKey serviceFunctionChainStateKey =
                new ServiceFunctionChainStateKey(serviceFunctionChain.getName());
        InstanceIdentifier<ServiceFunctionChainState> sfcStateIID =
                InstanceIdentifier.builder(ServiceFunctionChainsState.class)
                        .child(ServiceFunctionChainState.class, serviceFunctionChainStateKey)
                        .build();

        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionChainState> serviceFunctionChainStateObject = null;
        try {
            serviceFunctionChainStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfcStateIID).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("\n Could not read Service Function Chain operational data \n");
        }
        // TODO: Remove path name from Service Function path list
        if (serviceFunctionChainStateObject instanceof ServiceFunctionChainState) {
            serviceFunctionChainState = (ServiceFunctionChainState) serviceFunctionChainStateObject;
            List<String> sfcServiceFunctionPathList =
                    serviceFunctionChainState.getSfcServiceFunctionPath();
            List<String> removedPaths = new ArrayList<>();
            for (String pathName : sfcServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
                writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                        sfpIID);
                writeTx.commit();

            }

            sfcServiceFunctionPathList.removeAll(removedPaths);

            /* After we are done removing all paths from the datastore we commit the updated the path list
             * under the Service Chain operational tree
             */
            ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder = new ServiceFunctionChainStateBuilder();
            serviceFunctionChainStateBuilder.setName(serviceFunctionChain.getName());
            serviceFunctionChainStateBuilder.setSfcServiceFunctionPath(sfcServiceFunctionPathList);
            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                    sfcStateIID, serviceFunctionChainStateBuilder.build(), true);
            writeTx.commit();

        } else {
            LOG.error("Failed to get reference to Service Function Chain State {} ", serviceFunctionChain.getName());
        }
        printTraceStop(LOG);
    }

    @SuppressWarnings("unused")
    protected void updateServiceFunctionPathEntry(ServiceFunctionPath serviceFunctionPath) {
        this.createServiceFunctionPathEntry(serviceFunctionPath);
    }

    /*
     * This function is actually an updated to a previously created SFP where only
     * the service chain name was given. In this function we patch the SFP with the
     * names of the chosen SFs
     */

    /**
     * This function is called whenever a SFP is created or updated. It recomputes
     * the SFP information and merges any missing data
     * <p/>
     *
     * @param serviceFunctionPath Service Function Path Object
     * @return Nothing.
     */
    protected void createServiceFunctionPathEntry(ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        long pathId;
        short posIndex = 0;
        int serviceIndex;
        ServiceFunctionChain serviceFunctionChain = null;
        String serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        try {
            serviceFunctionChain = serviceFunctionChainName != null ?
                    (ServiceFunctionChain) odlSfc.executor
                            .submit(SfcProviderServiceChainAPI.getRead(
                                    new Object[]{serviceFunctionChainName},
                                    new Class[]{String.class})).get()
                    : null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(" \n Could not read Service Function Chain configuration for Service Path {}",
                    serviceFunctionPath.getName());
        }
        if (serviceFunctionChain == null) {
            LOG.error("\n ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }


        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ArrayList<ServicePathHop> servicePathHopArrayList = new ArrayList<>();
        ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        //List<SfcServiceFunction> sfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(serviceFunctionChain.getSfcServiceFunction());

        Collections.sort(sfcServiceFunctionList, Collections.reverseOrder(SF_ORDER));
        serviceIndex = sfcServiceFunctionList.size();
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("\n########## ServiceFunction name: {}", sfcServiceFunction.getName());

            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType;
            try {
                serviceFunctionType = (ServiceFunctionType) odlSfc.executor.submit(SfcProviderServiceTypeAPI.getRead(
                        new Object[]{sfcServiceFunction.getType()}, new Class[]{String.class})).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(" Could not get list of Service Functions of type {} \n", sfcServiceFunction.getType());
                return;
            }
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                        // TODO: API to select suitable Service Function
                        String serviceFunctionName = sftServiceFunctionName.getName();
                        ServiceFunction serviceFunction = null;
                        try {
                            serviceFunction =
                                    (ServiceFunction) odlSfc.executor.submit(SfcProviderServiceFunctionAPI
                                            .getRead(new Object[]{serviceFunctionName}, new Class[]{String.class})).get();
                        } catch (InterruptedException | ExecutionException e) {
                            LOG.error(" Could not read Service Function {} " +
                                    "\n", serviceFunctionName);
                        }
                        if (serviceFunction != null) {
                            servicePathHopBuilder.setHopNumber(posIndex)
                                    .setServiceFunctionName(serviceFunctionName)
                                    .setServiceIndex((short) serviceIndex)
                                    .setServiceFunctionForwarder(serviceFunction.getSfDataPlaneLocator()
                                            .get(0)
                                            .getServiceFunctionForwarder());
                            servicePathHopArrayList.add(posIndex, servicePathHopBuilder.build());
                            serviceIndex--;
                            posIndex++;
                            break;
                        } else {
                            LOG.error("\n####### Could not find suitable SF of type in data store: {}",
                                    sfcServiceFunction.getType());
                            return;
                        }
                    }
                } else {
                    LOG.error("\n########## No configured SFs of type: {}", sfcServiceFunction.getType());
                    return;
                }
            } else {
                LOG.error("\n########## No configured SFs of type: {}", sfcServiceFunction.getType());
                return;
            }

        }

        //Build the service function path so it can be committed to datastore


        pathId = (serviceFunctionPath.getPathId() != null) ? serviceFunctionPath.getPathId()
                : numCreatedPathIncrementGet();
        serviceFunctionPathBuilder.setServicePathHop(servicePathHopArrayList);
        if (serviceFunctionPath.getName().isEmpty()) {
            serviceFunctionPathBuilder.setName(serviceFunctionChainName + "-Path-" + pathId);
        } else {
            serviceFunctionPathBuilder.setName(serviceFunctionPath.getName());

        }

        serviceFunctionPathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        serviceFunctionPathBuilder.setStartingIndex((short) servicePathHopArrayList.size());
        serviceFunctionPathBuilder.setServiceChainName(serviceFunctionChainName);

        ServiceFunctionPathKey serviceFunctionPathKey = new
                ServiceFunctionPathKey(serviceFunctionPathBuilder.getName());
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                .build();

        ServiceFunctionPath newServiceFunctionPath =
                serviceFunctionPathBuilder.build();
        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sfpIID, newServiceFunctionPath, true);
        writeTx.commit();
        //SfcProviderServiceForwarderAPI.addPathIdtoServiceFunctionForwarder(newServiceFunctionPath);
        SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(newServiceFunctionPath);

        /* Process classifier */
        updateServicePathAclEntries(newServiceFunctionPath);

        /* Prepare REST invocation */

        invokeServicePathRest(serviceFunctionPath.getName(), HttpMethod.PUT);

        printTraceStop(LOG);

    }

    /**
     * This method updates the SFP ACL entries according to classifier, reproducing these steps:
     * 1. Classifier is derived from parent SFC
     * 2. One ACL (from available Classifier ACLs) is selected according to SFP ACL
     * 3. All ACL entries (ACEs) SfcAction are set to point to the SFP
     * <p/>
     *
     * @param serviceFunctionPath Service Function Path Object
     * @return Nothing.
     */
    private void updateServicePathAclEntries(ServiceFunctionPath serviceFunctionPath) {

        ServiceFunctionClassifier serviceFunctionChainClassifier = null;
        try {
            serviceFunctionChainClassifier =
                    (ServiceFunctionClassifier) odlSfc.executor.submit(SfcProviderServiceClassifierAPI
                            .getReadBySfcName(new Object[]{serviceFunctionPath.getServiceChainName()},
                                    new Class[]{String.class})).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(" Could not read Service Function Classifier for Service Chain {} " +
                    "\n", serviceFunctionPath.getServiceChainName());
        }

        if (serviceFunctionChainClassifier != null) {
            LOG.info(" {} Classifier = {} \n", serviceFunctionPath.getServiceChainName(),
                    serviceFunctionChainClassifier.getName());

            AccessList acl = null;
            try {
                acl = (AccessList) odlSfc.executor.submit(SfcProviderAclAPI
                        .getRead(new Object[]{serviceFunctionChainClassifier.getAccessList()},
                                new Class[]{String.class})).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(" Could not read ACL {} " +
                        "\n", serviceFunctionChainClassifier.getAccessList());
            }

            if (acl != null) {
                ArrayList<AccessListEntries> aceArrayList = new ArrayList<>();
                aceArrayList.addAll(acl.getAccessListEntries());

                for (AccessListEntries ace : aceArrayList) {
//                    SfcAction sfcAction = ace.getActions().getAugmentation(Actions1.class).getSfcAction();
//                    String aclServicePathName = ((AclServiceFunctionPath) sfcAction).getServiceFunctionPath();
//                    LOG.info(" SFC Action points to SFP = {} \n", aclServicePathName);

                    AclServiceFunctionPathBuilder aclServiceFunctionPathBuilder = new AclServiceFunctionPathBuilder();
                    aclServiceFunctionPathBuilder.setServiceFunctionPath(serviceFunctionPath.getName());

                    Actions1Builder actions1Builder = new Actions1Builder();
                    actions1Builder.setSfcAction(aclServiceFunctionPathBuilder.build());

                    ActionsBuilder actionsBuilder = new ActionsBuilder();
                    actionsBuilder.addAugmentation(Actions1.class, actions1Builder.build());

                    AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder(ace);
                    accessListEntriesBuilder.setActions(actionsBuilder.build());

                    InstanceIdentifier<AccessListEntries> aceIID = InstanceIdentifier.builder(AccessLists.class)
                            .child(AccessList.class, acl.getKey())
                            .child(AccessListEntries.class, ace.getKey()).build();

                    WriteTransaction aceWriteTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
                    aceWriteTx.merge(LogicalDatastoreType.CONFIGURATION,
                            aceIID, accessListEntriesBuilder.build(), true);
                    aceWriteTx.commit();
                }
            }
        }
    }

    /**
     * This method decouples the SFP API from the SouthBound REST client.
     * SFP APIs call this method to convey SFP information to REST southbound
     * devices
     * <p/>
     *
     * @param sfpName    Service Function Path Name
     * @param httpMethod HTTP method such as GET, PUT, POST..
     * @return Nothing.
     */
    private void invokeServicePathRest(String sfpName, String httpMethod) {

     /* Invoke SB REST API */

        ServiceFunctionPath serviceFunctionPath = readServiceFunctionPath(sfpName);

        if (serviceFunctionPath != null) {
            if (httpMethod.equals(HttpMethod.PUT)) {
                Object[] servicePathObj = {serviceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                odlSfc.executor.execute(SfcProviderRestAPI.
                        getPutServiceFunctionPath(servicePathObj,
                                servicePathClass));
            }
        } else {
            LOG.error("Could not find Service Function path: {}", sfpName);
        }

    }


    /*
    private void deleteServiceFunctionPathEntry (ServiceFunctionChain serviceFunctionChain) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        String serviceFunctionChainName = serviceFunctionChain.getName();
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionChainName + "-Path");
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                .build();

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sfpIID);
        writeTx.commit();
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }
    */


    /*
     * We iterate through all service paths that use this service function and remove them.
     * In the end since there is no more operational state, we remove the state tree.
     */

    @SuppressWarnings("unused")
    public void deleteServicePathContainingFunction(ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionState serviceFunctionState;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunction.getName());
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionState> serviceFunctionStateObject = null;
        try {
            serviceFunctionStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfStateIID).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read Service Function State operational data \n");
            return;
        }

        if ((serviceFunctionStateObject != null) &&
                (serviceFunctionStateObject.get() instanceof ServiceFunctionState)) {
            serviceFunctionState = serviceFunctionStateObject.get();
            List<String> sfServiceFunctionPathList =
                    serviceFunctionState.getSfServiceFunctionPath();
            List<String> removedPaths = new ArrayList<>();
            for (String pathName : sfServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
                writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                        sfpIID);
                writeTx.commit();
                // TODO: Need to consider failure of transaction
                removedPaths.add(pathName);
            }

            // If no more SFP associated with this SF, remove the state.
            if (removedPaths.containsAll(sfServiceFunctionPathList)) {
                SfcProviderServiceFunctionAPI.deleteServiceFunctionState(serviceFunction.getName());
            } else {
                LOG.error("Could not remove all paths containing function: {} ", serviceFunction.getName());
            }
        } else {
            LOG.warn("Failed to get reference to Service Function State {} ", serviceFunction.getName());
        }
        printTraceStop(LOG);
    }


    /*
     * When a SF is updated, meaning key remains the same, but other fields change we need to
     * update all affected SFPs. We need to do that because admin can update critical fields
     * as SFC type, rendering the path unfeasible. The update reads the current path from
     * data store, keeps pathID intact and rebuild the SF list.
     *
     * The update can or not work.
     */
    private void updateServicePathContainingFunction(ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPath> sfpIID;

        ServiceFunctionState serviceFunctionState = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        if (serviceFunctionState != null) {
            List<String> sfServiceFunctionPathList =
                    serviceFunctionState.getSfServiceFunctionPath();
            for (String pathName : sfServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
                Optional<ServiceFunctionPath> serviceFunctionPathObject = null;
                try {
                    serviceFunctionPathObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpIID).get();
                    if (serviceFunctionPathObject != null &&
                            (serviceFunctionPathObject.get() instanceof ServiceFunctionPath)) {
                        ServiceFunctionPath servicefunctionPath = serviceFunctionPathObject.get();
                        createServiceFunctionPathEntry(servicefunctionPath);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Could not read Service Function Path configuration data \n");
                }
            }
        } else {
            LOG.error("Failed to get reference to Service Function State {} ", serviceFunction.getName());
        }
        printTraceStop(LOG);
        return;
    }
}

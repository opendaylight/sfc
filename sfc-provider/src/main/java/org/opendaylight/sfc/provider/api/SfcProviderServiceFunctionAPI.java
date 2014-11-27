/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunction
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 * @since 2014-06-30
 */
public class SfcProviderServiceFunctionAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionAPI.class);

    SfcProviderServiceFunctionAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceFunctionAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceFunctionAPI getDeleteServicePathFromServiceFunctionState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "deleteServicePathFromServiceFunctionState");
    }
    public static SfcProviderServiceFunctionAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "putServiceFunction");
    }
    public static SfcProviderServiceFunctionAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readServiceFunction");
    }
    public static SfcProviderServiceFunctionAPI getReadServiceFunctionState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readServiceFunctionState");
    }
    public static SfcProviderServiceFunctionAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "deleteServiceFunction");
    }
    public static SfcProviderServiceFunctionAPI getDeleteServiceFunctionState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "deleteServiceFunctionState");
    }
    public static SfcProviderServiceFunctionAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "putAllServiceFunctions");
    }
    public static SfcProviderServiceFunctionAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readAllServiceFunctions");
    }
    public static SfcProviderServiceFunctionAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "deleteAllServiceFunctions");
    }
    public static SfcProviderServiceFunctionAPI getAddPathToServiceFunctionState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "addPathToServiceFunctionState");
    }

    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static List<SfServicePath> readServiceFunctionState(String serviceFunctionName) {
        printTraceStart(LOG);

        List<SfServicePath> ret = null;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ServiceFunctionState dataSfcStateObject;
        dataSfcStateObject = SfcDataStoreAPI.readTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
        // Read the list of Service Function Path anchored by this SFF
        if (dataSfcStateObject != null) {
            ret = dataSfcStateObject.getSfServicePath();
        }

/*
        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();

        try {
            dataSfcStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfStateIID).get();
            if (dataSfcStateObject.isPresent()) {
                ret = dataSfcStateObject.get().getSfServicePath();
            } else {
                LOG.warn("Could not find Service Function State for service function {}",
                        serviceFunctionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read Service Function State for" +
                            " service function {} from DataStore",
                    serviceFunctionName);
        }*/
        printTraceStop(LOG);
        return ret;

    }


    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static List<SfServicePath> readServiceFunctionStateExecutor(String serviceFunctionName) {

        printTraceStart(LOG);
        List<SfServicePath> ret = null;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getReadServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (List<SfServicePath>) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method deletes the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static boolean deleteServiceFunctionStateExecutor(String serviceFunctionName) {
        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getDeleteServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method deletes the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    @SuppressWarnings("unused")
    public static boolean deleteServiceFunctionState(String serviceFunctionName) {
        printTraceStart(LOG);
        boolean ret = false;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID,LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("Could not delete operational state for SF: {}", serviceFunctionName);
        }
        return ret;
    }

    /**
     * This method adds a SFP name to the corresponding SF operational state.
     * <p>
     * @param serviceFunctionPath SFP object
     * @return true if SFP was added, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean addPathToServiceFunctionState(ServiceFunctionPath serviceFunctionPath) {

        boolean ret =  false;
        printTraceStart(LOG);

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        String rspName = serviceFunctionPath.getName();
        SfServicePathKey sfServicePathKey = new SfServicePathKey(rspName);
        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setKey(sfServicePathKey);
        sfServicePathBuilder.setName(rspName);

        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(serviceFunctionPath.getName());
        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(renderedServicePathHop.getServiceFunctionName());

            InstanceIdentifier<SfServicePath> sfStateIID = InstanceIdentifier
                    .builder(ServiceFunctionsState.class)
                    .child(ServiceFunctionState.class, serviceFunctionStateKey)
                    .child(SfServicePath.class, sfServicePathKey).build();
            serviceFunctionStateBuilder.setName(renderedServicePathHop.getServiceFunctionName());

            if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sfServicePathBuilder.build(),
                    LogicalDatastoreType.OPERATIONAL)) {
                ret = true;
            } else {
                LOG.error("Could not add SFP {} to operational state of SF: {}",
                        serviceFunctionPath.getName(), renderedServicePathHop.getServiceFunctionName());
            }
            printTraceStop(LOG);
        }
        return ret;
    }

    /**
     * This method adds a SFP name to the corresponding SF operational state.
     * <p>
     * @param serviceFunctionPath SFP object
     * @return true if SFP was added, false otherwise
     */
    public static boolean addPathToServiceFunctionStateExecutor(ServiceFunctionPath serviceFunctionPath) {
        boolean ret =  false;
        printTraceStart(LOG);

        Object[] servicePathObj = {serviceFunctionPath};
        Class[] servicePathClass = {ServiceFunctionPath.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getAddPathToServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddPathToServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

    protected static boolean putServiceFunction(ServiceFunction sf) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                child(ServiceFunction.class, sf.getKey()).toInstance();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfEntryIID, sf, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    protected static boolean mergeServiceFunction(ServiceFunction sf) {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                child(ServiceFunction.class, sf.getKey()).toInstance();

        ret = SfcDataStoreAPI.writeMergeTransactionAPI(sfEntryIID, sf, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads a SF from the datastore
     * <p>
     * @param serviceFunctionName SF name
     * @return SF object or null if not found
     */
    @SuppressWarnings("unused")
    @SfcReflection
    protected ServiceFunction readServiceFunction(String serviceFunctionName) {
        printTraceStart(LOG);
        ServiceFunction sf;
        InstanceIdentifier<ServiceFunction> sfIID;
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey).build();

        sf = SfcDataStoreAPI.readTransactionAPI(sfIID, LogicalDatastoreType.CONFIGURATION);

/*        if (odlSfc.getDataProvider() != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunction> serviceFunctionDataObject;
            try {
                serviceFunctionDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
                if (serviceFunctionDataObject != null
                        && serviceFunctionDataObject.isPresent()) {
                    sf = serviceFunctionDataObject.get();
                } else {
                    LOG.error("Could not find Service Function {}", serviceFunctionName);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function {} from DataStore", serviceFunctionName);
            }
        }*/
        printTraceStop(LOG);
        return sf;
    }

    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static ServiceFunction readServiceFunctionExecutor(String serviceFunctionName) {

        printTraceStart(LOG);
        ServiceFunction ret = null;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getRead(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (ServiceFunction) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * This method deletes a SF from the datastore
     * <p>
     * @param serviceFunctionName SF name
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    protected boolean deleteServiceFunction(String serviceFunctionName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SF: {}", serviceFunctionName);
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    protected boolean putAllServiceFunctions(ServiceFunctions sfs) {
        boolean ret = false;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).toInstance();
        if (SfcDataStoreAPI.writeSynchPutTransactionAPI(sfsIID, sfs, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not add all Service Functions: {}", sfs.toString());
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctions readAllServiceFunctions() {
        ServiceFunctions sfs = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctions> sfsIID =
                InstanceIdentifier.builder(ServiceFunctions.class).toInstance();

        sfs = SfcDataStoreAPI.readTransactionAPI(sfsIID, LogicalDatastoreType.CONFIGURATION);

/*        if (odlSfc.getDataProvider() != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunctions> serviceFunctionsDataObject;
            try {
                serviceFunctionsDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfsIID).get();
                LOG.error("serviceFunctionsDataObject: {}", serviceFunctionsDataObject);
                if (serviceFunctionsDataObject != null
                        && serviceFunctionsDataObject.isPresent()) {
                    sfs = serviceFunctionsDataObject.get();
                } else {
                    LOG.error("Could not find Service Functions");
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Functions from DataStore");
            }
        }*/
        printTraceStop(LOG);
        return sfs;
    }

    protected boolean deleteAllServiceFunctions() {
        boolean ret = false;
        printTraceStart(LOG);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sfsIID);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Wrapper API to delete the given service path name from the all Service Functions
     * that are used by the associated path. It includes Executes creation and response
     * management.
     * <p>
     * @param serviceFunctionPath SFP object
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean deleteServicePathFromServiceFunctionStateExecutor(ServiceFunctionPath serviceFunctionPath) {

        boolean ret =  false;
        printTraceStart(LOG);

        Object[] servicePathObj = {serviceFunctionPath};
        Class[] servicePathClass = {ServiceFunctionPath.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getDeleteServicePathFromServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.info("getDeleteServicePathFromServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Wrapper API to delete the given service path name from the all Service Functions
     * that are used by the associated path. It includes Executes creation and response
     * management.
     *
     * <p>
     * @param rspList List of service path names
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean deleteServicePathFromServiceFunctionStateExecutor(List<String> rspList) {

        boolean ret =  false;
        printTraceStart(LOG);

        Object[] servicePathObj = {rspList};
        Class[] servicePathClass = {List.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getDeleteServicePathFromServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathFromServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * Delete the given service path name from the all Service Functions that are used by
     * the associated path
     * <p>
     * @param serviceFunctionPath RSP object
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    @SfcReflection
    public boolean deleteServicePathFromServiceFunctionState(ServiceFunctionPath serviceFunctionPath) {

        boolean ret = true;
        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(serviceFunctionPath.getName());
        if (renderedServicePath != null) {
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
                String sfName = renderedServicePathHop.getServiceFunctionName();

                String rspName = renderedServicePath.getName();
                SfServicePathKey sfServicePathKey = new SfServicePathKey(rspName);
                SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
                sfServicePathBuilder.setKey(sfServicePathKey);
                sfServicePathBuilder.setName(rspName);

                ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
                InstanceIdentifier<SfServicePath> sfStateIID = InstanceIdentifier
                        .builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .child(SfServicePath.class, sfServicePathKey).build();
                if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
                    ret = ret && true;
                } else {
                    ret = ret && false;
                    LOG.error("Could not delete Service Path {} from SF {} operational state",
                            rspName, sfName);
                }
            }
        } else {
            LOG.error("Rendered Service Path {} already deleted", serviceFunctionPath.getName());
        }
        return ret;
    }

    /**
     * When a Service Path is deleted directly (not as a consequence of deleting a SF), we need
     * to remove its reference from all the ServiceFunction states.
     * <p>
     * @param sfpName RSP List
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean deleteServicePathFromServiceFunctionStateExecutor(String sfpName) {

        boolean ret =  false;
        printTraceStart(LOG);

        Object[] servicePathObj = {sfpName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getDeleteServicePathFromServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathFromServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }
    /**
     * This method removes the given Service Path from the all SF operational
     * states that use it.
     *
     * It assumes that the associated Rendered Service Path has not been deletes
     * yet since it reads it in order to have access to all SFs that are used
     * by this RSP.
     *
     * <p>
     * @param sfpName SFP name
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean deleteServicePathFromServiceFunctionState(String sfpName) {

        printTraceStart(LOG);
        boolean ret = true;

        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(sfpName);

        if (renderedServicePath != null) {
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
                String sfName = renderedServicePathHop.getServiceFunctionName();
                String rspName = renderedServicePath.getName();
                SfServicePathKey sfServicePathKey = new SfServicePathKey(rspName);
                SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
                sfServicePathBuilder.setKey(sfServicePathKey);
                sfServicePathBuilder.setName(rspName);

                ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
                InstanceIdentifier<SfServicePath> sfStateIID = InstanceIdentifier
                        .builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .child(SfServicePath.class, sfServicePathKey).build();
                if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
                    ret = true;
                } else {
                    ret = false;
                    LOG.error("Could not delete Service Path {} from SF {} operational state",
                            rspName, sfName);
                }
            }
        } else {
            LOG.error("{}: Rendered Service Path {} does not exist",
                    Thread.currentThread().getStackTrace()[1], sfpName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Removes a single Service Path name from the given Service Function operational state
     *
     * <p>
     * @param rspName SF name
     * @param sfName  SF name
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    @SfcReflection
    public static boolean deleteServicePathFromServiceFunctionState(String rspName, String sfName) {

        boolean ret = true;

        SfServicePathKey sfServicePathKey = new SfServicePathKey(rspName);
        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setKey(sfServicePathKey);
        sfServicePathBuilder.setName(rspName);

        ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
        InstanceIdentifier<SfServicePath> sfStateIID = InstanceIdentifier
                .builder(ServiceFunctionsState.class)
                .child(ServiceFunctionState.class, serviceFunctionStateKey)
                .child(SfServicePath.class, sfServicePathKey).build();
        if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            ret = false;
            LOG.error("Could not delete Service Path {} from SF {} operational state",
                    rspName, sfName);
        }

        return ret;
    }
}

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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    public static ServiceFunctionState readServiceFunctionState(String serviceFunctionName) {
        printTraceStart(LOG);

        ServiceFunctionState serviceFunctionState = null;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionState> dataSfcStateObject;
        try {
            dataSfcStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfStateIID).get();
            if ((dataSfcStateObject != null) && (dataSfcStateObject.isPresent())) {
                serviceFunctionState = dataSfcStateObject.get();
            } else {
                LOG.warn("Could not find Service Function State for service function {}",
                        serviceFunctionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read Service Function State for" +
                            " service function {} from DataStore",
                    serviceFunctionName);
        }
        printTraceStop(LOG);
        return serviceFunctionState;

    }


    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static ServiceFunctionState readServiceFunctionStateExecutor(String serviceFunctionName) {

        printTraceStart(LOG);
        ServiceFunctionState ret = null;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getRead(servicePathObj, servicePathClass);
        Future future  = odlSfc.executor.submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (ServiceFunctionState) future.get();
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
    public static boolean addPathToServiceFunctionState(ServiceFunctionPath serviceFunctionPath) {

        boolean ret =  false;
        printTraceStart(LOG);

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        ArrayList<String> sfcServiceFunctionPathArrayList = new ArrayList<>();
        sfcServiceFunctionPathArrayList.add(serviceFunctionPath.getName());

        serviceFunctionStateBuilder.setSfServiceFunctionPath(sfcServiceFunctionPathArrayList);
        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(serviceFunctionPath.getName());
        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(renderedServicePathHop.getServiceFunctionName());
            InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
                    .child(ServiceFunctionState.class, serviceFunctionStateKey).build();
            serviceFunctionStateBuilder.setName(renderedServicePathHop.getServiceFunctionName());

            if (SfcDataStoreAPI.writeMergeTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(),
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
        boolean ret = false;
        printTraceStart(LOG);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION,
                    sfEntryIID, sf, true);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected static boolean mergeServiceFunction(ServiceFunction sf) {
        boolean ret = false;
        printTraceStart(LOG);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sfEntryIID, sf, true);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads a SF from the datastore
     * <p>
     * @param serviceFunctionName SF name
     * @return SF object or null if not found
     */
    protected ServiceFunction readServiceFunction(String serviceFunctionName) {
        printTraceStart(LOG);
        ServiceFunction sf = null;
        InstanceIdentifier<ServiceFunction> sfIID;
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey).build();

        if (odlSfc.getDataProvider() != null) {
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
        }
        printTraceStop(LOG);
        return sf;
    }

    /**
     * This method deletes a SF from the datastore
     * <p>
     * @param serviceFunctionName SF name
     * @return true if SF was deleted, false otherwise
     */
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

        if (odlSfc.getDataProvider() != null) {
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
        }
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
     * When a Service Path is deleted directly (not as a consequence of deleting a SF), we need
     * to remove its reference from all the ServiceFunction states.
     * <p>
     * @param serviceFunctionPath SFP object that was removed from datastore
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
     * When a Service Path is deleted directly (not as a consequence of deleting a SF), we need
     * to remove its reference from all the ServiceFunction states.
     * <p>
     * @param rspList RSP List
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
     * When a Service Path is deleted directly (not as a consequence of deleting a SF), we need
     * to remove its reference from all the ServiceFunction states.
     * <p>
     * @param renderedServicePath RSP object
     * @return true if SF was deleted, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean deleteServicePathFromServiceFunctionState(RenderedServicePath renderedServicePath) {

        boolean ret = true;
        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            String serviceFunctionName = renderedServicePathHop.getServiceFunctionName();
            ServiceFunctionState serviceFunctionState = readServiceFunctionState(serviceFunctionName);
            if (serviceFunctionState != null) {
                ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(serviceFunctionName);
                InstanceIdentifier<ServiceFunctionState> sfStateIID =
                        InstanceIdentifier.builder(ServiceFunctionsState.class)
                                .child(ServiceFunctionState.class, serviceFunctionStateKey)
                                .build();

                List<String> sfServiceFunctionPathList = serviceFunctionState.getSfServiceFunctionPath();
                List<String> newPathList = new ArrayList<>();
                newPathList.addAll(sfServiceFunctionPathList);
                newPathList.remove(renderedServicePath.getName());
                // If no more SFPs associated with this SF, remove list
                if (newPathList.size() == 0) {
                    if (deleteServiceFunctionState(serviceFunctionName)) {
                        ret = ret && true;
                    } else {
                        ret = ret && false;
                    }
                } else {
                    ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
                    serviceFunctionStateBuilder.setName(serviceFunctionName);
                    serviceFunctionStateBuilder.setSfServiceFunctionPath(newPathList);

                    if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(),
                            LogicalDatastoreType.OPERATIONAL)) {
                        ret = ret && true;
                    } else {
                        LOG.error("Could not delete path {} from operational state of SF: {}",
                                renderedServicePath.getName(), serviceFunctionName);
                        ret = ret && false;
                    }
                }
            } else {
                ret = ret && true;
            }
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

        boolean ret = true;

        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(sfpName);

        if (renderedServicePath != null) {
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
                String serviceFunctionName = renderedServicePathHop.getServiceFunctionName();
                ServiceFunctionState serviceFunctionState = readServiceFunctionState(serviceFunctionName);
                if (serviceFunctionState != null) {
                    ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(serviceFunctionName);
                    InstanceIdentifier<ServiceFunctionState> sfStateIID =
                            InstanceIdentifier.builder(ServiceFunctionsState.class)
                                    .child(ServiceFunctionState.class, serviceFunctionStateKey)
                                    .build();

                    List<String> sfServiceFunctionPathList = serviceFunctionState.getSfServiceFunctionPath();
                    List<String> newPathList = new ArrayList<>();
                    newPathList.addAll(sfServiceFunctionPathList);
                    newPathList.remove(sfpName);
                    // If no more SFPs associated with this SF, remove list
                    if (newPathList.size() == 0) {
                        if (deleteServiceFunctionState(serviceFunctionName)) {
                            ret = ret && true;
                        } else {
                            ret = ret && false;
                        }
                    } else {
                        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
                        serviceFunctionStateBuilder.setName(serviceFunctionName);
                        serviceFunctionStateBuilder.setSfServiceFunctionPath(newPathList);

                        if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(),
                                LogicalDatastoreType.OPERATIONAL)) {
                            ret = ret && true;
                        } else {
                            LOG.error("Could not delete path {} from operational state of SF: {}",
                                    sfpName, serviceFunctionName);
                            ret = ret && false;
                        }
                    }
                } else {
                    ret = ret && true;
                }
            }
        } else {
            LOG.error("Rendered Service Path {} is null", sfpName);
        }
        return ret;
    }

    /**
     * Removes a single Service Path name from the given Service Function operational state
     *
     * <p>
     * @param sfpName SF name
     * @param sfName  SF name
     * @return true if SF was deleted, false otherwise
     */
    public static boolean deleteServicePathFromServiceFunctionState(String sfpName, String sfName) {

        boolean ret = true;

        ServiceFunctionState serviceFunctionState = readServiceFunctionState(sfName);
        if (serviceFunctionState != null) {
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
            InstanceIdentifier<ServiceFunctionState> sfStateIID =
                    InstanceIdentifier.builder(ServiceFunctionsState.class)
                            .child(ServiceFunctionState.class, serviceFunctionStateKey)
                            .build();

            List<String> sfServiceFunctionPathList = serviceFunctionState.getSfServiceFunctionPath();
            List<String> newPathList = new ArrayList<>();
            newPathList.addAll(sfServiceFunctionPathList);
            newPathList.remove(sfpName);
            // If no more SFPs associated with this SF, remove list
            if (newPathList.size() == 0) {
                if (deleteServiceFunctionState(sfName)) {
                    ret = ret && true;
                } else {
                    ret = ret && false;
                }
            } else {
                ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
                serviceFunctionStateBuilder.setName(sfName);
                serviceFunctionStateBuilder.setSfServiceFunctionPath(newPathList);

                if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(),
                        LogicalDatastoreType.OPERATIONAL)) {
                    ret = ret && true;
                } else {
                    LOG.error("Could not delete path {} from operational state of SF: {}",
                            sfpName, sfName);
                    ret = ret && false;
                }
            }
        } else {
            ret = ret && true;
        }

        return ret;
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcProviderRestAPI;
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfpEntryDataListener
 * <p/>
 * <p/>
 * <p/>
 * @since       2014-06-30
 */
public class SfcProviderServicePathAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();
    private static final String FAILED_TO_STR = "failed to ...";


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
    public static  SfcProviderServicePathAPI getDeleteServicePathContainingFunction (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathContainingFunction");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getDeleteServicePathInstantiatedFromChain (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathInstantiatedFromChain");
    }
    public static  SfcProviderServicePathAPI getCreateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createServiceFunctionPathEntry");
    }

    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getUpdateServicePathInstantiatedFromChain(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathContainingFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathContainingFunction");
    }
    public static SfcProviderServicePathAPI getCheckServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "checkServiceFunctionPath");
    }
    public static SfcProviderServicePathAPI getAddRenderedPathToServicePathStateExecutor(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "addRenderedPathToServicePathState");
    }
    public static SfcProviderServicePathAPI getDeleteServicePathStateExecutor(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathState");
    }
    public static SfcProviderServicePathAPI getReadServicePathStateExecutor(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "readServicePathState");
    }


    /**
     * API to read the Service Function Path operational state
     *
     * <p>
     * @param servicePathName Service Path Name
     * @return List of RSP name objects
     */
    @SuppressWarnings("unused")
    public static List<SfpRenderedServicePath> readServicePathState(String servicePathName) {

        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPathState> sfpIID;
        List<SfpRenderedServicePath> ret = null;

        ServiceFunctionPathStateKey serviceFunctionPathStateKey = new ServiceFunctionPathStateKey(servicePathName);

        sfpIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, serviceFunctionPathStateKey).toInstance();

        ServiceFunctionPathState serviceFunctionPathState = SfcDataStoreAPI.readTransactionAPI(sfpIID,
                LogicalDatastoreType.OPERATIONAL);
        if (serviceFunctionPathState != null) {
            ret = serviceFunctionPathState.getSfpRenderedServicePath();
        }
        printTraceStop(LOG);

        return ret;
    }

    /**
     * Wrapper API to read the Service Function Path operational state
     *
     * <p>
     * @param servicePathName Service Path Name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static List<SfpRenderedServicePath> readServicePathStateExecutor(String servicePathName) {

        printTraceStart(LOG);
        List<SfpRenderedServicePath> ret = null;

        Object[] servicePathObj = {servicePathName};
        Class[] servicePathClass = {String.class};

        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getReadServicePathStateExecutor(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderServicePathAPI);
        try {
            ret = (List<SfpRenderedServicePath>) future.get();
            LOG.debug("getReadServicePathStateExecutor: {}", ret);
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Wrapper API to delete the Service Function Path operational state
     *
     * <p>
     * @param servicePathName Service Path Name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean deleteServicePathState(String servicePathName) {

        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPathState> sfpIID;
        boolean ret = false;

        ServiceFunctionPathStateKey serviceFunctionPathStateKey = new ServiceFunctionPathStateKey(servicePathName);

        sfpIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, serviceFunctionPathStateKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfpIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to delete Service Function Path {} state.",
                    Thread.currentThread().getStackTrace()[1], servicePathName);
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * Wrapper API to delete the Service Function Path operational state
     *
     * <p>
     * @param servicePathName Service Path Name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean deleteServicePathStateExecutor(String servicePathName) {

        printTraceStart(LOG);
        boolean ret = true;

        // SFF deletion is a critical event. If a SFF is deleted we delete all associated SFPs
        Object[] servicePathObj = {servicePathName};
        Class[] servicePathClass = {String.class};

        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getDeleteServicePathStateExecutor(servicePathObj, servicePathClass);
        Future future = ODL_SFC.getExecutor().submit(sfcProviderServicePathAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathStateExecutor: {}", ret);
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     * <p>
     * @param servicePathName Service Function Path name
     * @param renderedPathName Rendered Path name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean addRenderedPathToServicePathState (String servicePathName, String renderedPathName) {

        printTraceStart(LOG);
        InstanceIdentifier<SfpRenderedServicePath> rspIID;
        boolean ret = false;

        SfpRenderedServicePathBuilder sfpRenderedServicePathBuilder = new SfpRenderedServicePathBuilder();
        SfpRenderedServicePathKey sfpRenderedServicePathKey = new SfpRenderedServicePathKey(renderedPathName);
        sfpRenderedServicePathBuilder.setKey(sfpRenderedServicePathKey).setName(renderedPathName);

        ServiceFunctionPathStateKey serviceFunctionPathStateKey = new ServiceFunctionPathStateKey(servicePathName);

        rspIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, serviceFunctionPathStateKey)
                .child(SfpRenderedServicePath.class, sfpRenderedServicePathKey).toInstance();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, sfpRenderedServicePathBuilder.build(),
                LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to create Service Function Path {} state. Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], servicePathName, renderedPathName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     * <p>
     * @param servicePathName Service Function Path name
     * @param renderedPathName Rendered Path name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean addRenderedPathToServicePathStateExecutor (String servicePathName, String renderedPathName) {

        printTraceStart(LOG);
        boolean ret = true;
        Object[] functionParams = {servicePathName, renderedPathName};
        Class[] functionParamsTypes = {String.class, String.class};
        Future future = ODL_SFC.getExecutor().submit(SfcProviderServicePathAPI
                .getAddRenderedPathToServicePathStateExecutor(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddRenderedPathToServicePathStateExecutor returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }


    @SuppressWarnings("unused")
    protected boolean putServiceFunctionPath(ServiceFunctionPath sfp) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).
                        child(ServiceFunctionPath.class, sfp.getKey()).toInstance();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sfpEntryIID, sfp, LogicalDatastoreType.CONFIGURATION)) {
            LOG.debug("Created Service Function Path: {}", sfp.getName());
            ret = true;
        } else {
            LOG.error("Failed to create Service Function Path: {}", sfp.getName());
        }

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This function reads a SFP from the datastore
     * <p>
     * @param serviceFunctionPathName RSP name
     * @return Nothing.
     */
    public static ServiceFunctionPath readServiceFunctionPath(String serviceFunctionPathName) {
        printTraceStart(LOG);
        ServiceFunctionPath sfp;
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).build();

        sfp = SfcDataStoreAPI.readTransactionAPI(sfpIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sfp;
    }

    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static ServiceFunctionPath readServiceFunctionPathExecutor(String serviceFunctionName) {

        printTraceStart(LOG);
        ServiceFunctionPath ret = null;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServicePathAPI sfcProviderServicePathAPI = SfcProviderServicePathAPI
                .getRead(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServicePathAPI);
        try {
            ret = (ServiceFunctionPath) future.get();
            LOG.debug("readServiceFunctionPathExecutor: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }



    /**
     * This function deletes a SFP from the datastore
     * <p>
     * @param serviceFunctionPathName SFP name
     * @return Nothing.
     */
    @SfcReflection
    @SuppressWarnings("unused")
    public static boolean deleteServiceFunctionPath(String serviceFunctionPathName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).toInstance();

        if (!SfcDataStoreAPI.deleteTransactionAPI(sfpEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to delete SFP: {}", serviceFunctionPathName);
        } else {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    protected boolean putAllServiceFunctionPaths(ServiceFunctionPaths sfps) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.
                builder(ServiceFunctionPaths.class).toInstance();

        if (SfcDataStoreAPI.writePutTransactionAPI(sfpsIID, sfps, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    public static ServiceFunctionPaths readAllServiceFunctionPaths() {
        ServiceFunctionPaths sfps;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        sfps = SfcDataStoreAPI.readTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sfps;
    }

    @SuppressWarnings("unused")
    protected boolean deleteAllServiceFunctionPaths() {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param renderedServicePath RSP object
     * @param httpMethod HttpMethod
     * @return Nothing
     */
    public boolean checkServiceFunctionPath(RenderedServicePath renderedServicePath, String httpMethod) {

        printTraceStart(LOG);
        boolean ret;

        ret = invokeServicePathRest(renderedServicePath, httpMethod);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method decouples the SFP API from the SouthBound REST client.
     * SFP APIs call this method to convey SFP information to REST southbound
     * devices
     * <p>
     * @param renderedServicePath Rendered Service Paths
     * @param httpMethod  HTTP method such as GET, PUT, POST..
     * @return Nothing.
     */
    private boolean invokeServicePathRest(RenderedServicePath renderedServicePath, String httpMethod) {

     /* Invoke SB REST API */
        boolean ret = true;

        if (renderedServicePath != null)
        {
            if (httpMethod.equals(HttpMethod.PUT))
            {
                Object[] servicePathObj = {renderedServicePath};
                Class[] servicePathClass = {RenderedServicePath.class};
                 ODL_SFC.getExecutor().execute(SfcProviderRestAPI.
                        getPutRenderedServicePath(servicePathObj,
                                servicePathClass));
            } else if (httpMethod.equals(HttpMethod.DELETE))
            {
                Object[] servicePathObj = {renderedServicePath};
                Class[] servicePathClass = {RenderedServicePath.class};
                ODL_SFC.getExecutor().execute(SfcProviderRestAPI.
                        getDeleteRenderedServicePath(servicePathObj,
                                servicePathClass));
            }
        } else {
            LOG.error("Data object is null");
        }
        return ret;

    }

    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param serviceFunctionPath SFP object
     * @param operation HttpMethod
     * @return Nothing
     */
    public static boolean checkServiceFunctionPathExecutor(ServiceFunctionPath serviceFunctionPath, String operation) {

        printTraceStart(LOG);
        boolean ret = false;

        //Send to SB REST
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI
                .readRenderedServicePath(serviceFunctionPath.getName());
        if (renderedServicePath == null) {
            LOG.error("Failed to find RSP:  {}", serviceFunctionPath.getName());
            return ret;
        }
        Object[] renderedPathObj = {renderedServicePath, operation};
        Class[] renderedPathClass = {RenderedServicePath.class, String.class};
        Future future = ODL_SFC.getExecutor().submit(SfcProviderServicePathAPI.getCheckServicePathAPI(
                renderedPathObj, renderedPathClass));
        try {
            ret = (boolean) future.get();
            LOG.debug("getCheckServicePathAPI returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param renderedServicePath RSP object
     * @param operation HttpMethod
     * @return Nothing
     */
    public static boolean checkServiceFunctionPathExecutor(RenderedServicePath renderedServicePath, String operation) {

        printTraceStart(LOG);
        boolean ret = false;

        Object[] renderedPathObj = {renderedServicePath, operation};
        Class[] renderedPathClass = {RenderedServicePath.class, String.class};
        Future future = ODL_SFC.getExecutor().submit(SfcProviderServicePathAPI.getCheckServicePathAPI(
                renderedPathObj, renderedPathClass));
        try {
            ret = (boolean) future.get();
            LOG.debug("getCheckServicePathAPI returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR, e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR, e);
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them. Additionally, since we are delete the RSP, we also
     * <p>
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    @SfcReflection
    public boolean deleteServicePathContainingFunction (ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = true;
        List<SfServicePath> sfServicePathList;

        sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        if (sfServicePathList != null) {
            for (SfServicePath sfServicePath : sfServicePathList) {

                String rspName = sfServicePath.getName();
                if (SfcProviderRenderedPathAPI.readRenderedServicePath(rspName) != null) {
                    if (SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName)) {
                        ret = ret && true;
                    } else {
                        LOG.error("Failed to delete Path {} from Service Function {} state",
                                rspName, serviceFunction.getName());
                        ret = ret && false;
                    }
                } else {
                    LOG.debug("{}: SFP {} already deleted by another thread or client",
                            Thread.currentThread().getStackTrace()[1], rspName);
                }
            }
        } else {
            LOG.debug("Could not find Service function Paths using Service Function: {} ",
                    serviceFunction.getName());
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * We iterate through all service paths that use this service function and if
     * necessary, remove them.
     * <p>
     * @param serviceFunction Service Function Object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean deleteServicePathContainingFunctionExecutor (ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = true;
        Object[] functionParams = {serviceFunction};
        Class[] functionParamsTypes = {ServiceFunction.class};
        Future future = ODL_SFC.getExecutor().submit(SfcProviderServicePathAPI
                .getDeleteServicePathContainingFunction(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathContainingFunction returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }
}

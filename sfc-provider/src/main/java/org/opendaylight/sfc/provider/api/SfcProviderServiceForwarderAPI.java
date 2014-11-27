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
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.service.function.forwarder.state.SffServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * <p/>
 * <p/>
 * <p/>
 * @since 2014-06-30
 */
public class SfcProviderServiceForwarderAPI extends SfcProviderAbstractAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceForwarderAPI.class);

    SfcProviderServiceForwarderAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceForwarderAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }


    public static SfcProviderServiceForwarderAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "putServiceFunctionForwarder");
    }
    public static SfcProviderServiceForwarderAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "readServiceFunctionForwarder");
    }
    public static SfcProviderServiceForwarderAPI getReadSffState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "readSffState");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServiceForwarderAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteServiceFunctionForwarder");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServiceForwarderAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "putAllServiceFunctionForwarders");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServiceForwarderAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "readAllServiceFunctionForwarders");
    }
    public static SfcProviderServiceForwarderAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteAllServiceFunctionForwarders");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServiceForwarderAPI getDeleteServiceFunctionFromForwarder(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteServiceFunctionFromForwarder");
    }
    public static SfcProviderServiceForwarderAPI getCheckServiceForwarderAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "checkServiceFunctionForwarder");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServiceForwarderAPI getUpdateServiceForwarderAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "updateServiceFunctionForwarder");
    }
    public static SfcProviderServiceForwarderAPI getAddPathToServiceForwarderState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "addPathToServiceForwarderState");
    }
    public static SfcProviderServiceForwarderAPI getDeletePathFromServiceForwarderState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deletePathFromServiceForwarderState");
    }
    public static SfcProviderServiceForwarderAPI getDeletePathsUsedByServiceForwarder(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteRenderedPathsUsedByServiceForwarder");
    }
    public static SfcProviderServiceForwarderAPI getDeleteServiceFunctionForwarderState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteServiceFunctionForwarderState");
    }

    protected boolean putServiceFunctionForwarder(ServiceFunctionForwarder sff) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).
                    child(ServiceFunctionForwarder.class, sff.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sffEntryIID, sff, true);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads a SFF from the datastore
     * <p>
     * @param serviceFunctionForwarderName SFF name
     * @return SF object or null if not found
     */
    protected ServiceFunctionForwarder readServiceFunctionForwarder(String serviceFunctionForwarderName) {
        printTraceStart(LOG);
        ServiceFunctionForwarder sff = null;
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionForwarder> serviceFunctionForwarderDataObject = null;
            try {
                serviceFunctionForwarderDataObject = readTx.
                        read(LogicalDatastoreType.CONFIGURATION, sffIID).get();
                if (serviceFunctionForwarderDataObject != null
                        && serviceFunctionForwarderDataObject.isPresent()) {
                    sff = serviceFunctionForwarderDataObject.get();
                } else {
                    LOG.error("Failed to read Service Function Forwarder: {}", serviceFunctionForwarderName);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Forwarder {} " +
                        "configuration", serviceFunctionForwarderName);
            }
        }
        printTraceStop(LOG);
        return sff;
    }

    /**
     * This method deletes a SFF from the datastore
     * <p>
     * @param serviceFunctionForwarderName SFF name
     * @return true if SF was deleted, false otherwise
     */
    protected boolean deleteServiceFunctionForwarder(String serviceFunctionForwarderName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).
                child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sffEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SFF: {}", serviceFunctionForwarderName);
        }
        printTraceStop(LOG);
        return ret;
    }

    protected boolean putAllServiceFunctionForwarders(ServiceFunctionForwarders sffs) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                    InstanceIdentifier.builder(ServiceFunctionForwarders.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sffsIID, sffs);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionForwarders readAllServiceFunctionForwarders() {
        ServiceFunctionForwarders sffs = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class).toInstance();

        if (odlSfc.getDataProvider() != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunctionForwarders> serviceFunctionForwardersDataObject;
            try {
                serviceFunctionForwardersDataObject = readTx.
                        read(LogicalDatastoreType.CONFIGURATION, sffsIID).get();
                if (serviceFunctionForwardersDataObject != null
                        && serviceFunctionForwardersDataObject.isPresent()) {
                    sffs = serviceFunctionForwardersDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Forwarder " +
                        "configuration data");
            }

        }
        printTraceStop(LOG);
        return sffs;
    }

    protected boolean deleteAllServiceFunctionForwarders() {
        boolean ret = false;
        printTraceStart(LOG);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                    InstanceIdentifier.builder(ServiceFunctionForwarders.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sffsIID);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param serviceFunctionForwarder SFF object
     * @return Nothing
     */
    public void checkServiceFunctionForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {

        printTraceStart(LOG);

        invokeServiceForwarderRest(serviceFunctionForwarder, HttpMethod.PUT);

        printTraceStop(LOG);
    }

    /**
     * This method decouples the SFP API from the SouthBound REST client.
     * SFP APIs call this method to convey SFP information to REST southbound
     * devices
     * <p>
     * @param serviceFunctionForwarder SFF object
     * @param httpMethod  HTTP method such as GET, PUT, POST..
     */
    private void invokeServiceForwarderRest(ServiceFunctionForwarder serviceFunctionForwarder,
                                            String httpMethod) {

     /* Invoke SB REST API */

        if (serviceFunctionForwarder != null)
        {
            if (httpMethod.equals(HttpMethod.PUT))
            {
                Object[] servicePathObj = {serviceFunctionForwarder};
                Class[] serviceForwarderClass = {ServiceFunctionForwarder.class};
                odlSfc.executor.execute(SfcProviderRestAPI.getPutServiceFunctionForwarder
                        (servicePathObj,
                                serviceForwarderClass));
            } else if (httpMethod.equals(HttpMethod.DELETE))
            {
                Object[] servicePathObj = {serviceFunctionForwarder};
                Class[] serviceForwarderClass = {ServiceFunctionForwarder.class};
                odlSfc.executor.execute(SfcProviderRestAPI.getDeleteServiceFunctionForwarder
                        (servicePathObj,
                                serviceForwarderClass));
            }
        } else {
            LOG.error("SFF object is null");
        }

    }

    /**
     * When a SF is deleted, we need to remove it from the corresponding SFF
     * service function dictionary
     * devices
     * <p>
     * @param serviceFunction SF object
     * @return true is SF was deleted, false otherwise
     */
    public boolean deleteServiceFunctionFromForwarder(ServiceFunction serviceFunction) {
        printTraceStart(LOG);
        boolean ret = false;
        String serviceFunctionForwarderName = serviceFunction.getSfDataPlaneLocator()
                .get(0).getServiceFunctionForwarder();
        InstanceIdentifier<ServiceFunctionDictionary> sffIID;
        ServiceFunctionDictionaryKey serviceFunctionDictionaryKey =
                new ServiceFunctionDictionaryKey(serviceFunction.getName());
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(ServiceFunctionDictionary.class, serviceFunctionDictionaryKey)
                .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sffIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SF {} from SFF {}", serviceFunction.getName(), serviceFunctionForwarderName);
        }
        printTraceStop(LOG);
        return ret;

    }

    @SuppressWarnings("unused")
    public void updateServiceFunctionForwarder(ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        printTraceStop(LOG);

    }

    /**
     * We add the path name to the operational store of each SFF.
     *
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     * @return Nothing.
     */
    public boolean addPathToServiceForwarderState(ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        boolean ret = true;
        ServiceFunctionForwarderStateBuilder serviceFunctionForwarderStateBuilder =
                new ServiceFunctionForwarderStateBuilder();

        //ArrayList<SffServicePath> sffServicePathArrayList = new ArrayList<>();
        String rspName = serviceFunctionPath.getName();
        SffServicePathKey sffServicePathKey = new SffServicePathKey(rspName);
        SffServicePathBuilder sffServicePathBuilder = new SffServicePathBuilder();
        sffServicePathBuilder.setKey(sffServicePathKey);
        sffServicePathBuilder.setName(rspName);
        //sffServicePathArrayList.add(sffServicePathBuilder.build());
        //serviceFunctionForwarderStateBuilder.setSffServicePath(sffServicePathArrayList);

        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(rspName);
        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                    new ServiceFunctionForwarderStateKey(renderedServicePathHop.getServiceFunctionForwarder());
            InstanceIdentifier<SffServicePath> sfStateIID =
                    InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                    .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                    .child(SffServicePath.class, sffServicePathKey).build();
            serviceFunctionForwarderStateBuilder.setName(renderedServicePathHop.getServiceFunctionForwarder());

            if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sffServicePathBuilder.build(),
                    LogicalDatastoreType.OPERATIONAL)) {
                ret = ret && true;
            } else {
                ret = ret && false;
                LOG.error("Failed to add path {} to SFF {} state.",
                        rspName, renderedServicePathHop.getServiceFunctionForwarder());
            }
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * We add the path name to the operational store of each SFF.
     *
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     * @return Nothing.
     */
    public static boolean addPathToServiceForwarderStateExecutor(ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunctionPath};
        Class[] servicePathClass = {ServiceFunctionPath.class};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                .getAddPathToServiceForwarderState(servicePathObj, servicePathClass);
        Future future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddPathToServiceForwarderState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * When a SFF is deleted we need to delete all SFPs from the
     * associated SFF operational state
     *
     * <p>
     * @param serviceFunctionPath SFP object
     * @return true if all paths were deleted, false otherwise.
     */
    @SuppressWarnings("unused")
    public boolean deletePathFromServiceForwarderState(ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        boolean ret = true;
        ServiceFunctionForwarderStateBuilder serviceFunctionForwarderStateBuilder =
                new ServiceFunctionForwarderStateBuilder();

        String rspName = serviceFunctionPath.getName();
        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(serviceFunctionPath.getName());

        if (renderedServicePath != null) {
            Set<String> sffNameSet = new HashSet<>();
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {

                String sffName = renderedServicePathHop.getServiceFunctionForwarder();
                if (sffNameSet.add(sffName)) {

                    SffServicePathKey sffServicePathKey = new SffServicePathKey(rspName);
                    ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                            new ServiceFunctionForwarderStateKey(sffName);
                    InstanceIdentifier<SffServicePath> sfStateIID =
                            InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                                    .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                                    .child(SffServicePath.class, sffServicePathKey).build();
                    if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
                        ret = true;
                    } else {
                        ret = false;
                        LOG.error("Could not delete Service Path {} from SFF {} operational state : {}",
                                rspName, sffName);
                    }
                    List<SffServicePath> sffServicePathList = readSffState(sffName);
                    if ((sffServicePathList != null) && sffServicePathList.isEmpty()) {
                        if (deleteServiceFunctionForwarderState(sffName)) {
                            ret = ret && true;
                        } else {
                            ret = ret && false;
                        }
                    }
                }
            }
        } else {
            LOG.error("{}: Rendered Service Path {} does not exist",
                    Thread.currentThread().getStackTrace()[1], serviceFunctionPath.getName());
        }
        printTraceStop(LOG);
        return ret;
    }



    /**
     * Creates a executor and calls appropriate function to to remove
     * SFF operational state for given Service Path
     *
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     * @return Nothing.
     */
    public static boolean deletePathFromServiceForwarderStateExecutor(ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunctionPath};
        Class[] servicePathClass = {ServiceFunctionPath.class};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                .getDeletePathFromServiceForwarderState(servicePathObj, servicePathClass);
        Future future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddPathToServiceForwarderState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * Creates a executor and calls appropriate function to to remove
     * SFF operational state for given Service Path
     *
     * <p>
     * @param rspName Service Function Path Object
     * @return Nothing.
     */
    public static boolean deletePathFromServiceForwarderStateExecutor(String rspName) {

        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {rspName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                .getDeletePathFromServiceForwarderState(servicePathObj, servicePathClass);
        Future future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddPathToServiceForwarderState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }


    /**
     * When a SFF is deleted we need to delete all SFPs from the
     * associated SFF operational state
     *
     * <p>
     * @param rspName SFP object
     * @return true if all path was deleted, false otherwise.
     */
    @SuppressWarnings("unused")
    @SfcReflection
    public boolean deletePathFromServiceForwarderState(String rspName) {

        printTraceStart(LOG);

        boolean ret = true;
        ServiceFunctionForwarderStateBuilder serviceFunctionForwarderStateBuilder =
                new ServiceFunctionForwarderStateBuilder();

        RenderedServicePath renderedServicePath = SfcProviderServicePathAPI.readRenderedServicePath(rspName);

        if (renderedServicePath != null) {
            Set<String> sffNameSet = new HashSet<>();
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {

                String sffname = renderedServicePathHop.getServiceFunctionForwarder();
                if (sffNameSet.add(sffname)) {

                    SffServicePathKey sffServicePathKey = new SffServicePathKey(rspName);
                    ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                            new ServiceFunctionForwarderStateKey(sffname);
                    InstanceIdentifier<SffServicePath> sfStateIID =
                            InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                                    .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                                    .child(SffServicePath.class, sffServicePathKey).build();
                    if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
                        ret = ret && true;
                    } else {
                        ret = ret && false;
                        LOG.error("Could not delete Service Path {} from SFF {} operational state : {}",
                                rspName, sffname);
                    }
                    List<SffServicePath> sffServicePathList = readSffState(sffname);
                    if (sffServicePathList.isEmpty()) {
                        if (deleteServiceFunctionForwarderState(sffname)) {
                            ret = ret && true;
                        } else {
                            ret = ret && false;
                        }
                    }
                }
            }
        } else {
            LOG.error("{}: Rendered Service Path {} does not exist",
                    Thread.currentThread().getStackTrace()[1], rspName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method deletes the operational state for a service function.
     * <p>
     * @param sffName SFF name
     * @return A ServiceFunctionState object that is a list of all paths using
     * this service function, null otherwise
     */
    public static boolean deleteServiceFunctionForwarderState(String sffName) {
        printTraceStart(LOG);
        boolean ret = false;
        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey(sffName);
        InstanceIdentifier<ServiceFunctionForwarderState> sffStateIID =
                InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                        .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sffStateIID,LogicalDatastoreType.OPERATIONAL)) {
            ret = true;
        } else {
            LOG.error("Could not delete operational state for SFF: {}", sffName);
        }
        return ret;
    }

    /**
     * This method deletes the operational state for the given SFF name.
     * <p>
     * @param sffName SFF name
     * @return true if state was deletes, false otherwise
     */
    public static boolean deleteServiceFunctionForwarderStateExecutor(String sffName) {
        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {sffName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                .getDeleteServiceFunctionForwarderState(servicePathObj, servicePathClass);
        Future future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddPathToServiceForwarderState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Returns the list of SFPs anchored by a SFF
     *
     * <p>
     * @param sffName SFF name
     * @return SffServicePath
     */
    public static List<SffServicePath> readSffState(String sffName) {
        printTraceStart(LOG);
        List<SffServicePath> ret = null;

        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey(sffName);

        InstanceIdentifier<ServiceFunctionForwarderState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey).build();

        // Read the list of Service Function Path anchored by this SFF
        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionForwarderState> sffStateDataObject;
        try {
            sffStateDataObject = readTx
                    .read(LogicalDatastoreType.OPERATIONAL, sfStateIID).get();
            if (sffStateDataObject.isPresent()) {
                ret = sffStateDataObject.get().getSffServicePath();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read operation state for SFF: {}", sffName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Wrapper API to read the list of SFPs anchored by the given SFF name. It
     * includes Executor creation and response management
     *
     * <p>
     * @param sffName SFF name
     * @return ServiceFunctionState.
     */
    public static List<SffServicePath> readSffStateExecutor(String sffName) {
        printTraceStart(LOG);
        List<SffServicePath> ret = null;

        // SFF deletion is a critical event. If a SFF is deleted we delete all associated SFPs
        Object[] serviceForwarderObj = {sffName};
        Class[] serviceForwarderClass = {String.class};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                .getReadSffState(serviceForwarderObj, serviceForwarderClass);
        Future future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
        try {
            ret = (List<SffServicePath>) future.get();
            LOG.info("getDeletePathsUsedByServiceForwarder: {}", ret);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Deletes all RSPs used by the given SFF object
     *
     * <p>
     * @param serviceFunctionForwarder SFF Object
     * @return true if all RSPs were deleted, false otherwise
     */
    @SuppressWarnings("unused")
    @SfcReflection
    public static boolean deleteRenderedPathsUsedByServiceForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {

        printTraceStart(LOG);

        boolean ret = false;
        List<SffServicePath> sffServicePathList = readSffState(serviceFunctionForwarder.getName());
        if (!sffServicePathList.isEmpty()) {
            for (SffServicePath sffServicePath : sffServicePathList)
            {
                String rspName = sffServicePath.getName();
                if (SfcProviderServicePathAPI.readRenderedServicePath(rspName) != null) {
                    if (SfcProviderServicePathAPI.deleteRenderedServicePath(rspName)) {
                        ret = true;
                    } else {
                        LOG.error("Could not delete RSP: {}", rspName);
                        ret = false;
                    }
                } else {
                    LOG.error("SFP {} already deleted by another thread or client", rspName);
                    ret = true;
                }
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Attaches the path name to the operational store of each SFF.
     *
     * <p>
     * @param sfpName SF name
     * @param sffName SFF name
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    @SfcReflection
    public static boolean deletePathFromServiceForwarderStateExecutor(String sfpName, String sffName) {

        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {sfpName, sffName};
        Class[] servicePathClass = {String.class, String.class};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                .getDeletePathFromServiceForwarderState(servicePathObj, servicePathClass);
        Future future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeletePathFromServiceForwarderState: {}", future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method deletes all RSPs used by the given SFF
     *
     * <p>
     * @param serviceFunctionForwarder SFF object
     * @return Nothing.
     */
    @SuppressWarnings("unused")
    public static boolean deletePathsUsedByServiceForwarderExecutor(ServiceFunctionForwarder serviceFunctionForwarder) {

        printTraceStart(LOG);
        boolean ret = true;

        // SFF deletion is a critical event. If a SFF is deleted we delete all associated SFPs
        Object[] serviceForwarderObj = {serviceFunctionForwarder};
        Class[] serviceForwarderClass = {ServiceFunctionForwarder.class};
        SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI = SfcProviderServiceForwarderAPI
                .getDeletePathsUsedByServiceForwarder(serviceForwarderObj, serviceForwarderClass);
        Future future = odlSfc.executor.submit(sfcProviderServiceForwarderAPI);
        try {
            ret = (boolean) future.get();
            LOG.info("getDeletePathsUsedByServiceForwarder: {}", ret);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        printTraceStop(LOG);
        return ret;
    }

  /**
     * When a SFF is deleted we need to delete all SFPs from the
     * associated SFF operational state
     *
     * <p>
     * @param rspName SFP name
     * @param sffName SFF name
     * @return true if all paths were deleted, false otherwise.
     */
    @SuppressWarnings("unused")
    @SfcReflection
    public static boolean deletePathFromServiceForwarderState(String rspName, String sffName) {

        printTraceStart(LOG);

        boolean ret = true;
        ServiceFunctionForwarderStateBuilder serviceFunctionForwarderStateBuilder =
                new ServiceFunctionForwarderStateBuilder();

        SffServicePathKey sffServicePathKey = new SffServicePathKey(rspName);
        ServiceFunctionForwarderStateKey serviceFunctionForwarderStateKey =
                new ServiceFunctionForwarderStateKey(sffName);
        InstanceIdentifier<SffServicePath> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class, serviceFunctionForwarderStateKey)
                        .child(SffServicePath.class, sffServicePathKey).build();
        if (SfcDataStoreAPI.deleteTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL)) {
            ret = ret && true;
        } else {
            ret = ret && false;
            LOG.error("Could not delete Service Path {} from SFF {} operational state : {}",
                    rspName, sffName);
        }
        List<SffServicePath> sffServicePathList = readSffState(sffName);
        if ((sffServicePathList != null) && sffServicePathList.isEmpty()) {
            if (deleteServiceFunctionForwarderState(sffName)) {
                ret = ret && true;
            } else {
                ret = ret && false;
            }
        }

        printTraceStop(LOG);
        return ret;
    }
}

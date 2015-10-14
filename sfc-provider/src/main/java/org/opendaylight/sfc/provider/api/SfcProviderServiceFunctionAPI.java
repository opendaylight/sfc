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

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
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
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to operate on the ServiceFunction
 * datastore.
 * <p>
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
        } else {
            LOG.warn("readServiceFunctionState() Service Function {} has no operational state", serviceFunctionName);
        }
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
    public static List<String> readServiceFunctionStateAsStringList(String serviceFunctionName) {
        printTraceStart(LOG);

        List<SfServicePath> sfServicePathList = null;
        List<String> rspList = new ArrayList<>();
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ServiceFunctionState dataSfcStateObject = SfcDataStoreAPI.
                readTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
        if (dataSfcStateObject != null) {
            sfServicePathList = dataSfcStateObject.getSfServicePath();
            for (SfServicePath sfServicePath : sfServicePathList) {
                String rspName = sfServicePath.getName();
                rspList.add(rspName);
            }
        } else {
            LOG.warn("readServiceFunctionStateAsStringList() Service Function {} has no operational state", serviceFunctionName);
        }

        printTraceStop(LOG);
        return rspList;

    }

    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return service function description and monitor information
     * null otherwise
     */
    public static SfcSfDescMon readServiceFunctionDescriptionMonitor(String serviceFunctionName) {
        printTraceStart(LOG);

        SfcSfDescMon ret = null;
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
            ret = dataSfcStateObject.getAugmentation(ServiceFunctionState1.class).getSfcSfDescMon();
        } else {
            LOG.warn("readServiceFunctionDescriptionMonitor() Service Function {} has no operational state", serviceFunctionName);
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionsState readAllServiceFunctionStates() {
        ServiceFunctionsState sfsState = null;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionsState> sfStateIID = InstanceIdentifier
                .builder(ServiceFunctionsState.class)
                .build();

        sfsState = SfcDataStoreAPI.readTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);

        printTraceStop(LOG);
        return sfsState;
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
            LOG.error("{}: Could not delete operational state for SF: {}",
                    Thread.currentThread().getStackTrace()[1], serviceFunctionName);
        }
        return ret;
    }

    /**
     * This method adds a SFP name to the corresponding SF operational state.
     * <p>
     * @param pathName SFP name
     * @return true if SFP was added, false otherwise
     */
    public static boolean addPathToServiceFunctionState(String pathName) {

        boolean ret =  false;
        printTraceStart(LOG);

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        SfServicePathKey sfServicePathKey = new SfServicePathKey(pathName);
        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setKey(sfServicePathKey);
        sfServicePathBuilder.setName(pathName);

        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(pathName);
        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            String serviceFunctionName = renderedServicePathHop.getServiceFunctionName();
            String serviceFunctionGroupName = renderedServicePathHop.getServiceFunctionGroupName();
            LOG.debug("handling hop index: {}, sf: {}, sfg: {}", renderedServicePathHop.getHopNumber(), serviceFunctionName, serviceFunctionGroupName);
            if(serviceFunctionName != null){
                ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(serviceFunctionName);

                InstanceIdentifier<SfServicePath> sfStateIID = InstanceIdentifier
                        .builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .child(SfServicePath.class, sfServicePathKey).build();
                serviceFunctionStateBuilder.setName(renderedServicePathHop.getServiceFunctionName());

                if (SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sfServicePathBuilder.build(),
                        LogicalDatastoreType.OPERATIONAL)) {
                    ret = true;
                } else {
                    LOG.error("{}: Could not add SFP {} to operational state of SF: {}",
                            Thread.currentThread().getStackTrace()[1], pathName,
                            renderedServicePathHop.getServiceFunctionName());
                }
            } else if(serviceFunctionGroupName != null){
                LOG.info("{}: Could not add SFP {} to operational state of SFG: {}", Thread.currentThread().getStackTrace()[1],
                        pathName, serviceFunctionGroupName);
                ret = true;

            }
        }
        printTraceStop(LOG);
        return ret;

    }


    /**
     * This method adds a RSP name to the corresponding SF operational state.
     * <p>
     * @param renderedServicePath RSP object
     * @return true if SFP was added, false otherwise
     */
    public static boolean addPathToServiceFunctionState(RenderedServicePath renderedServicePath) {

        boolean ret =  false;
        printTraceStart(LOG);

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        SfServicePathKey sfServicePathKey = new SfServicePathKey(renderedServicePath.getName());
        SfServicePathBuilder sfServicePathBuilder = new SfServicePathBuilder();
        sfServicePathBuilder.setKey(sfServicePathKey);
        sfServicePathBuilder.setName(renderedServicePath.getName());

        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            String serviceFunctionName = renderedServicePathHop.getServiceFunctionName();
            String serviceFunctionGroupName = renderedServicePathHop.getServiceFunctionGroupName();
            LOG.debug("handling hop index: {}, sf: {}, sfg: {}", renderedServicePathHop.getHopNumber(), serviceFunctionName, serviceFunctionGroupName);
            if(serviceFunctionName != null){
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
                    LOG.error("{}: Could not add SFP {} to operational state of SF: {}",
                            Thread.currentThread().getStackTrace()[1], renderedServicePath.getName(),
                            renderedServicePathHop.getServiceFunctionName());
                }
            } else if(serviceFunctionGroupName != null){
                LOG.info("{}: Could not add SFP {} to operational state of SFG: {}", Thread.currentThread().getStackTrace()[1],
                        renderedServicePath.getName(), serviceFunctionGroupName);
                ret = true;
            }

        }
        printTraceStop(LOG);
        return ret;

    }



    /**
     * This method puts a SF to data store.
     * <p>
     * @param sf Service Function
     * @return true if SF was added, false otherwise
     */
    public static boolean putServiceFunction(ServiceFunction sf) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                child(ServiceFunction.class, sf.getKey()).build();

        ret = SfcDataStoreAPI.writeMergeTransactionAPI(sfEntryIID, sf, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Add ServiceFunctionState to datastore
     * <p>
     * @param sfState ServiceFunctionState Object
     * @return true if state was added, false otherwise
     */
    public static boolean putServiceFunctionState(ServiceFunctionState sfState) {
        boolean ret;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, sfState.getKey())
                        .build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, sfState, LogicalDatastoreType.OPERATIONAL);

        printTraceStop(LOG);
        return ret;
    }

    public static boolean mergeServiceFunctionState(ServiceFunctionState sfState) {
        boolean ret;
        printTraceStart(LOG);

        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(sfState.getKey());
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ret = SfcDataStoreAPI.writeMergeTransactionAPI(sfStateIID, sfState, LogicalDatastoreType.OPERATIONAL);

        printTraceStop(LOG);
        return ret;
    }

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
    protected static ServiceFunction readServiceFunction(String serviceFunctionName) {
        printTraceStart(LOG);
        ServiceFunction sf;
        InstanceIdentifier<ServiceFunction> sfIID;
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey).build();

        sf = SfcDataStoreAPI.readTransactionAPI(sfIID, LogicalDatastoreType.CONFIGURATION);

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
                .child(ServiceFunction.class, serviceFunctionKey).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SF: {}", serviceFunctionName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method adds a ServiceFunctions Object (collection of SFs) to the data store
     *
     * <p>
     * @param  sfs Object holding ServiceFunctions
     * @return true is ServiceFunctions added to the datatore, false otherwise
     */
    protected boolean putAllServiceFunctions(ServiceFunctions sfs) {
        boolean ret = false;
        printTraceStart(LOG);
        //InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).build();
        if (SfcDataStoreAPI.writePutTransactionAPI(OpendaylightSfc.SF_IID, sfs, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not add all Service Functions: {}", sfs.toString());
        }
        printTraceStop(LOG);
        return ret;
    }

    protected static ServiceFunctions readAllServiceFunctions() {
        ServiceFunctions sfs = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctions> sfsIID =
                InstanceIdentifier.builder(ServiceFunctions.class).build();

        sfs = SfcDataStoreAPI.readTransactionAPI(sfsIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sfs;
    }

    protected boolean deleteAllServiceFunctions() {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).toInstance();
        if (SfcDataStoreAPI.deleteTransactionAPI(sfsIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
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
    public boolean deleteServicePathFromServiceFunctionState(ServiceFunctionPath serviceFunctionPath) {

        boolean ret = true;
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(serviceFunctionPath.getName());
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
                    LOG.error("{}: Could not delete Service Path {} from SF {} operational state",
                            Thread.currentThread().getStackTrace()[1], rspName, sfName);
                }
                List<SfServicePath> sfServicePathList = readServiceFunctionState(sfName);
                if ((sfServicePathList != null) && sfServicePathList.isEmpty()) {
                    if (deleteServiceFunctionState(sfName)) {
                        ret = ret && true;
                    } else {
                        ret = ret && false;
                    }
                }
            }
        } else {
            LOG.error("{}: Rendered Service Path {} already deleted",
                    Thread.currentThread().getStackTrace()[1], serviceFunctionPath.getName());
        }
        return ret;
    }

    /**
     * This method removes the given Service Path from the all SF operational
     * states that use it.
     *
     * It assumes that the associated Rendered Service Path has not been deleted
     * yet since it reads it in order to have access to all SFs that are used
     * by this RSP.
     *
     * <p>
     * @param sfpName SFP name
     * @return true if SF was deleted, false otherwise
     */
    public static boolean deleteServicePathFromServiceFunctionState(String sfpName) {

        printTraceStart(LOG);
        boolean ret = true;

        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(sfpName);

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
                    LOG.error("{}: Could not delete Service Path {} from SF {} operational state",
                            Thread.currentThread().getStackTrace()[1], rspName, sfName);
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
    public static boolean deleteServicePathFromServiceFunctionState(String rspName, String sfName) {

        boolean ret;

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
            LOG.error("{}: Could not delete Service Path {} from SF {} operational state",
                    Thread.currentThread().getStackTrace()[1], rspName, sfName);
        }

        return ret;
    }

}

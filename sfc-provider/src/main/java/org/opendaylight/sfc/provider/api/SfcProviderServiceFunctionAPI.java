/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

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
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1Builder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMonBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.Capabilities;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.Map;
import java.util.HashMap;

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
    private SfcProviderSfDescriptionMonitorAPI getSfDescMon = new SfcProviderSfDescriptionMonitorAPI();

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
    public static SfcProviderServiceFunctionAPI getReadServiceFunctionStateAsStringList(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readServiceFunctionStateAsStringList");
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
    public static SfcProviderServiceFunctionAPI getPutServiceFunctionDescriptionState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "putServiceFunctionDescription");
    }
    public static SfcProviderServiceFunctionAPI getPutServiceFunctionMonitorState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "putServiceFunctionMonitor");
    }
    public static SfcProviderServiceFunctionAPI getReadServiceFunctionDescriptionMonitor(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readServiceFunctionDescriptionMonitor");
    }
    public static SfcProviderServiceFunctionAPI getReadAllServiceFunctionsState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readAllServiceFunctionStates");
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
    public static List<SfServicePath> readServiceFunctionStateExecutor(String serviceFunctionName) {

        printTraceStart(LOG);
        List<SfServicePath> ret = null;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getReadServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (List<SfServicePath>) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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
     * This method reads the operational state for a service function as a
     * string list of service function paths
     * <p>
     * @param serviceFunctionName SF name
     * @return A string list of service function paths
     */
    public static List<String> readServiceFunctionStateAsStringListExecutor(String serviceFunctionName) {

        printTraceStart(LOG);
        List<String> ret = null;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getReadServiceFunctionStateAsStringList(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (List<String>) future.get();
            LOG.debug("getReadServiceFunctionStateAsStringList: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
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

    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceFunctionName SF name
     * @return service function description and monitor information
     * null otherwise
     */
    public static SfcSfDescMon readServiceFunctionDescriptionMonitorExecutor(String serviceFunctionName) {

        printTraceStart(LOG);
        SfcSfDescMon ret = null;
        Object[] serviceFunctionNameObj = {serviceFunctionName};
        Class[] serviceFunctionNameClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getReadServiceFunctionDescriptionMonitor(serviceFunctionNameObj, serviceFunctionNameClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (SfcSfDescMon) future.get();
            LOG.debug("getReadServiceFunctionDescriptionMonitor: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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
     * @return True if SF state was deleted, false otherwise
     */
    public static boolean deleteServiceFunctionStateExecutor(String serviceFunctionName) {
        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunctionName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getDeleteServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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
    @SuppressWarnings("unused")
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
                        Thread.currentThread().getStackTrace()[1], pathName,
                        renderedServicePathHop.getServiceFunctionName());
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
    @SuppressWarnings("unused")
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
        }
        printTraceStop(LOG);
        return ret;

    }



    /**
     * This method adds a RSP name to the corresponding SF operational state.
     * <p>
     * @param pathName RSP name
     * @return true if RSP name was added, false otherwise
     */
    public static boolean addPathToServiceFunctionStateExecutor(String pathName) {
        boolean ret =  false;
        printTraceStart(LOG);

        Object[] servicePathObj = {pathName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getAddPathToServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddPathToServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method adds a SFP name to the corresponding SF operational state.
     * <p>
     * @param renderedServicePath RSP object
     * @return true if SFP was added, false otherwise
     */
    public static boolean addPathToServiceFunctionStateExecutor(RenderedServicePath renderedServicePath) {
        boolean ret =  false;
        printTraceStart(LOG);

        Object[] servicePathObj = {renderedServicePath};
        Class[] servicePathClass = {RenderedServicePath.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getAddPathToServiceFunctionState(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getAddPathToServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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

    protected static boolean putServiceFunctionState(ServiceFunctionState sfState) {
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

    protected static boolean mergeServiceFunctionState(ServiceFunctionState sfState) {
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

/*        if (ODL_SFC.getDataProvider() != null) {
            ReadOnlyTransaction readTx = ODL_SFC.getDataProvider().newReadOnlyTransaction();
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
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (ServiceFunction) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathFromServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathFromServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteServicePathFromServiceFunctionState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
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
    @SuppressWarnings("unused")
    @SfcReflection
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

    /**
     * put the service function description information gotten through netconf
     * mountpoint into the OPERATIONAL datastore.
     *
     * <p>
     * @param serviceFunction  SF Object
     * @return true if SF description information was put into OPERATIONAL datastore
     * false otherwise
     */
    protected boolean putServiceFunctionDescription(ServiceFunction serviceFunction) {
        boolean ret = false;
        printTraceStart(LOG);
        SfcSfDescMon sfDescMon = null;
        ServiceFunctionState dataSfcStateObject;
        try {
            if (ODL_SFC.getDataProvider() != null) {
                //get mount point
                String mountpoint = serviceFunction.getIpMgmtAddress().getIpv4Address().getValue();
                //get ServiceFunctionState
                ServiceFunctionStateKey serviceFunctionStateKey =
                    new ServiceFunctionStateKey(serviceFunction.getName());
                InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier
                        .builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

                dataSfcStateObject = SfcDataStoreAPI.readTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
                //get sf description information from netconf
                GetSFDescriptionOutput sfDescInfoOutput = getSfDescMon.getSFDescriptionInfoFromNetconf(mountpoint);
                if(sfDescInfoOutput == null) {
                    return false;
                }
                
                DescriptionInfo descInfo = new DescriptionInfoBuilder(sfDescInfoOutput.getDescriptionInfo()).build();
                //build the service function capbility and utilization
                if(dataSfcStateObject!=null) {
                    if(dataSfcStateObject.getAugmentation(ServiceFunctionState1.class)!=null) {
                        ServiceFunctionState1 sf1Temp = dataSfcStateObject.getAugmentation(ServiceFunctionState1.class);
                        SfcSfDescMon sfDescMonTemp = sf1Temp.getSfcSfDescMon();
                        sfDescMon = new SfcSfDescMonBuilder()
                            .setMonitoringInfo(sfDescMonTemp.getMonitoringInfo())
                            .setDescriptionInfo(descInfo).build();
                    } else {
                        sfDescMon = new SfcSfDescMonBuilder()
                            .setDescriptionInfo(descInfo).build();
                    }
                }  else {
                    sfDescMon = new SfcSfDescMonBuilder()
                        .setDescriptionInfo(descInfo).build();
                }

                ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
                ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                    .setKey(serviceFunctionStateKey)
                    .addAugmentation(ServiceFunctionState1.class,sfState1).build();

                if(dataSfcStateObject!=null) {
                    ret = mergeServiceFunctionState(serviceFunctionState);
                } else {
                    ret = putServiceFunctionState(serviceFunctionState);
                }
            } else {
                LOG.error("Data Provider is null.");
            }
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * put the service function monitor information gotten through netconf
     * mountpoint into the OPERATIONALdatastore.
     *
     * <p>
     * @param serviceFunction  SF name
     * @return true if SF's monitor information was put into OPERATIONAL datastore
     * false otherwise
     */
    protected boolean putServiceFunctionMonitor(ServiceFunction serviceFunction) {
        boolean ret = false;
        printTraceStart(LOG);
        SfcSfDescMon sfDescMon = null;
        ServiceFunctionState dataSfcStateObject;
        try {
            if (ODL_SFC.getDataProvider() != null) {
                //get mount point
                String mountpoint = serviceFunction.getIpMgmtAddress().getIpv4Address().getValue();
                //get ServiceFunctionState
                ServiceFunctionStateKey serviceFunctionStateKey =
                    new ServiceFunctionStateKey(serviceFunction.getName());
                InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier
                        .builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

                dataSfcStateObject = SfcDataStoreAPI.readTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
                //get sf monitor data from netconf
                GetSFMonitoringInfoOutput sfMonInfoMap = getSfDescMon.getSFMonitorInfoFromNetconf(mountpoint);
                if(sfMonInfoMap == null) {
                    return false;
                }
                MonitoringInfo monInfo = new MonitoringInfoBuilder(sfMonInfoMap.getMonitoringInfo()).build();

                //build the service function capbility and utilization
                if(dataSfcStateObject!=null) {
                    if(dataSfcStateObject.getAugmentation(ServiceFunctionState1.class)!=null) {
                        ServiceFunctionState1 sf1Temp = dataSfcStateObject.getAugmentation(ServiceFunctionState1.class);
                        SfcSfDescMon sfDescMonTemp = sf1Temp.getSfcSfDescMon();
                        sfDescMon = new SfcSfDescMonBuilder()
                            .setMonitoringInfo(monInfo)
                            .setDescriptionInfo(sfDescMonTemp.getDescriptionInfo()).build();
                    } else {
                        sfDescMon = new SfcSfDescMonBuilder()
                            .setMonitoringInfo(monInfo).build();
                    }
                } else {
                    sfDescMon = new SfcSfDescMonBuilder()
                        .setMonitoringInfo(monInfo).build();
                }

                ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
                ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                    .setKey(serviceFunctionStateKey)
                    .addAugmentation(ServiceFunctionState1.class,sfState1).build();

                if(dataSfcStateObject!=null) {
                    ret = mergeServiceFunctionState(serviceFunctionState);
                } else {
                    ret = putServiceFunctionState(serviceFunctionState);
                }
            } else {
                LOG.error("Data Provider is null.");
            }
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

     /**
     * This method reads the Description information for a service function.
     * <p>
     * @param serviceFunction SF object
     * @true if SF description information was put into datastore
     * false otherwise
     */
    public static boolean putServiceFunctionDescriptionExecutor(ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunction};
        Class[] servicePathClass = {ServiceFunction.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getPutServiceFunctionDescriptionState(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean)future.get();
            LOG.debug("getPutServiceFunctionDescriptionState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads the monitor information for a service function.
     * <p>
     * @param serviceFunction SF object
     * @return true if SF's monitor information was put into datastore
     * false otherwise
     */
    public static boolean putServiceFunctionMonitorExecutor(ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = false;
        Object[] servicePathObj = {serviceFunction};
        Class[] servicePathClass = {ServiceFunction.class};
        SfcProviderServiceFunctionAPI sfcProviderServiceFunctionAPI = SfcProviderServiceFunctionAPI
                .getPutServiceFunctionMonitorState(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionAPI);
        try {
            ret = (boolean) future.get();
            LOG.debug("getPutServiceFunctionMonitorState: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }
}

/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.ServiceFunctionGroups;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to operate on the ServiceFunctionGroup datastore. <p/> It is normally called from onDataChanged() through a executor service. We need to use an executor service because we can not operate on a datastore while on onDataChanged() context.
 * @author Kfir Yeshayahu (kfir.yeshayahu@contextream.com)
 * @version 0.1 <p/>
 * @since 2015-02-14
 */
public class SfcProviderServiceFunctionGroupAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceTypeAPI.class);

    SfcProviderServiceFunctionGroupAPI(Object[] params, String m) {
        super(params, m);
    }

    @SuppressWarnings("rawtypes")
    SfcProviderServiceFunctionGroupAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "putServiceFunctionGroup");
    }

    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "readServiceFunctionGroup");
    }

    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAPI getByType(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "getServiceFunctionGroupByType");
    }

    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "deleteServiceFunctionGroup");
    }

    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAPI getAddSF(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "addServiceFunctionToGroup");
    }

    @SuppressWarnings("rawtypes")
    public static SfcProviderServiceFunctionGroupAPI getRemoveSF(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "removeServiceFunctionFromGroup");
    }

    /**
     * Reads a SFG from the datastore <p>
     * @param serviceFunctionGroup name
     * @return ServiceFunctionGroup object or null if not found
     */
    protected ServiceFunctionGroup readServiceFunctionGroup(String serviceFunctionGroupName) {
        printTraceStart(LOG);
        ServiceFunctionGroup sfg;
        InstanceIdentifier<ServiceFunctionGroup> sfgIID;
        ServiceFunctionGroupKey serviceFunctionGroupKey = new ServiceFunctionGroupKey(serviceFunctionGroupName);
        sfgIID = InstanceIdentifier.builder(ServiceFunctionGroups.class).child(ServiceFunctionGroup.class, serviceFunctionGroupKey).build();

        sfg = SfcDataStoreAPI.readTransactionAPI(sfgIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sfg;
    }

    /**
     * Reads a SFG from the datastore <p>
     * @param serviceFunctionGroup name
     * @return ServiceFunctionGroup object or null if not found
     */
    protected ServiceFunctionGroup getServiceFunctionGroupByType(String serviceFunctionType) {
        printTraceStart(LOG);
        ServiceFunctionGroup sfg = null;
        InstanceIdentifier<ServiceFunctionGroups> sfgIID;
        sfgIID = InstanceIdentifier.builder(ServiceFunctionGroups.class).build();

        ServiceFunctionGroups sfgs = SfcDataStoreAPI.readTransactionAPI(sfgIID, LogicalDatastoreType.CONFIGURATION);

        for (ServiceFunctionGroup element : sfgs.getServiceFunctionGroup()) {
            if(element.getType().getName().equals(serviceFunctionType)){
                sfg = element;
                LOG.debug("found group " + sfg + " that matches type " + serviceFunctionType);
                break;
            }
        }
        if(sfg == null){
            LOG.debug("didn't found group " + sfg + " that matches type " + serviceFunctionType);
        }
        printTraceStop(LOG);
        return sfg;
    }

    /**
     * Puts a SFG in the datastore <p>
     * @param sfg the ServiceFunctionGroup to put
     * @return boolean success or failure
     */
    protected boolean putServiceFunctionGroup(ServiceFunctionGroup sfg) {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionGroup> sfgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroups.class).child(ServiceFunctionGroup.class, sfg.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfgEntryIID, sfg, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Deletes a SFG from the datastore <p>
     * @param serviceFunctionGroupName SFG name
     * @return boolean success of failure
     */
    protected boolean deleteServiceFunctionGroup(String serviceFunctionGroupName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionGroupKey serviceFunctionGroupKey = new ServiceFunctionGroupKey(serviceFunctionGroupName);
        InstanceIdentifier<ServiceFunctionGroup> sfgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroups.class).child(ServiceFunctionGroup.class, serviceFunctionGroupKey).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfgEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("{}: Could not delete SFG: {}", Thread.currentThread().getStackTrace()[1], serviceFunctionGroupName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Adds a SF to SFG <p>
     * @param serviceFunctionGroupName SFG name
     * @param serviceFunctionName name of SF to add
     * @return boolean success of failure
     */
    protected boolean addServiceFunctionToGroup(String serviceFunctionGroupName, String serviceFunctionName) {
        boolean ret = false;
        printTraceStart(LOG);

        // TODO Implement

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Removes a SF from SFG <p>
     * @param serviceFunctionGroupName SFG name
     * @param serviceFunctionName name of SF to remove
     * @return boolean success of failure
     */
    protected boolean removeServiceFunctionFromGroup(String serviceFunctionGroupName, String serviceFunctionName) {
        boolean ret = false;
        printTraceStart(LOG);

        // TODO Implement

        printTraceStop(LOG);
        return ret;
    }

    public static ServiceFunctionGroup getServiceFunctionGroupbyTypeExecutor(Class<? extends ServiceFunctionTypeIdentity> sft) {

        printTraceStart(LOG);
        ServiceFunctionGroup ret = null;
        Object[] servicePathObj = {sft.getName()};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionGroupAPI sfcProviderServiceFunctionGroupAPI = SfcProviderServiceFunctionGroupAPI
                .getByType(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionGroupAPI);
        try {
            ret = (ServiceFunctionGroup) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
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
    public static ServiceFunctionGroup readServiceFunctionGroupExecutor(String serviceFunctionGroupName) {

        printTraceStart(LOG);
        ServiceFunctionGroup ret = null;
        Object[] servicePathObj = {serviceFunctionGroupName};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceFunctionGroupAPI sfcProviderServiceFunctionGroupAPI = SfcProviderServiceFunctionGroupAPI
                .getRead(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceFunctionGroupAPI);
        try {
            ret = (ServiceFunctionGroup) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }
        printTraceStop(LOG);
        return ret;
    }
}
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

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev160202.ServiceFunctionGroups;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev160202.service.function.group.grouping.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev160202.service.function.group.grouping.ServiceFunctionGroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to operate on the ServiceFunctionGroup datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor service. We
 * need to use an executor service because we can not operate on a datastore
 * while on onDataChanged() context.
 *
 * @author Kfir Yeshayahu (kfir.yeshayahu@contextream.com)
 * @version 0.1
 *          <p/>
 * @since 2015-02-14
 */
public class SfcProviderServiceFunctionGroupAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceTypeAPI.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();
    private static final String FAILED_TO_STR = "failed to ...";

    SfcProviderServiceFunctionGroupAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceFunctionGroupAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceFunctionGroupAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "putServiceFunctionGroup");
    }

    public static SfcProviderServiceFunctionGroupAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "readServiceFunctionGroup");
    }

    public static SfcProviderServiceFunctionGroupAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "deleteServiceFunctionGroup");
    }

    public static SfcProviderServiceFunctionGroupAPI getAddSF(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "addServiceFunctionToGroup");
    }

    public static SfcProviderServiceFunctionGroupAPI getRemoveSF(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionGroupAPI(params, paramsTypes, "removeServiceFunctionFromGroup");
    }

    /**
     * Reads a SFG from the datastore
     * <p>
     *
     * @param serviceFunctionGroup
     *            name
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
     * Puts a SFG in the datastore
     * <p>
     *
     * @param sfg
     *            the ServiceFunctionGroup to put
     * @return boolean success or failure
     */
    protected boolean putServiceFunctionGroup(ServiceFunctionGroup sfg) {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionGroup> sfgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroups.class)
                .child(ServiceFunctionGroup.class, sfg.getKey()).toInstance();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfgEntryIID, sfg, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Deletes a SFG from the datastore
     * <p>
     *
     * @param serviceFunctionGroupName
     *            SFG name
     * @return boolean success of failure
     */
    protected boolean deleteServiceFunctionGroup(String serviceFunctionGroupName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionGroupKey serviceFunctionGroupKey = new ServiceFunctionGroupKey(serviceFunctionGroupName);
        InstanceIdentifier<ServiceFunctionGroup> sfgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroups.class)
                .child(ServiceFunctionGroup.class, serviceFunctionGroupKey).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfgEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Could not delete SFG Algorithm: {}", serviceFunctionGroupName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Adds a SF to SFG
     * <p>
     *
     * @param serviceFunctionGroupName
     *            SFG name
     * @param serviceFunctionName
     *            name of SF to add
     * @return boolean success of failure
     */
    protected boolean addServiceFunctionToGroup(String serviceFunctionGroupName, String serviceFunctionName) {
        boolean ret = false;
        printTraceStart(LOG);

        //TODO Implement

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Removes a SF from SFG
     * <p>
     *
     * @param serviceFunctionGroupName
     *            SFG name
     * @param serviceFunctionName
     *            name of SF to remove
     * @return boolean success of failure
     */
    protected boolean removeServiceFunctionFromGroup(String serviceFunctionGroupName, String serviceFunctionName) {
        boolean ret = false;
        printTraceStart(LOG);

        //TODO Implement

        printTraceStop(LOG);
        return ret;
    }
}
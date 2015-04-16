/*
 * Copyright (c) 2015 Guangzhou Research Institute of China Telecom. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class has the APIs to operate on the ServiceNode datastore.
 *
 * <p>
 * @author Peng Li (chinatelecom.sdn.group@gmail.com)

 * @version 0.1
 * @since       2015-03-26
 */
public class SfcProviderServiceNodeAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceNodeAPI.class);
    private static final OpendaylightSfc ODL_SFC = OpendaylightSfc.getOpendaylightSfcObj();
    private static final String FAILED_TO_STR = "failed to ...";

    SfcProviderServiceNodeAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceNodeAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceNodeAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceNodeAPI(params, paramsTypes, "putServiceNode");
    }

    public static SfcProviderServiceNodeAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceNodeAPI(params, paramsTypes, "readServiceNode");
    }


    /**
     * This method updates a SN from the datastore.
     * <p>
     * @param serviceNode SN object
     * @return true if SN was updated, false otherwise
     */
    protected boolean putServiceNode(ServiceNode serviceNode) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceNode> sfcEntryIID =
                InstanceIdentifier.builder(ServiceNodes.class)
                        .child(ServiceNode.class,serviceNode.getKey())
                        .toInstance();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sfcEntryIID, serviceNode, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to create Service Node: {}", serviceNode);
        }

        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method updates a service node by Executor, it includes
     * Executor creation and response management.
     *
     * <p>
     * @param serviceNode ServiceNode object
     * @return true if serviceNode was updated, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean putServiceNodeExecutor(ServiceNode serviceNode) {
        boolean ret = false;
        Object[] sfcParameters = {serviceNode};
        Class[] sfcParameterTypes = {ServiceNode.class};

        printTraceStart(LOG);
        try {
            Object result = ODL_SFC.getExecutor().submit(SfcProviderServiceNodeAPI.getPut(sfcParameters, sfcParameterTypes)).get();
            ret = (boolean)result;
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * This method reads the service node specified by the given name from
     * the datastore
     * <p>
     * @param serviceNode SN name
     * @return A ServiceNode object that is a list of all paths using
     * this service node, null otherwise
     */
    public static ServiceNode readServiceNode(String serviceNode) {
        printTraceStart(LOG);
        ServiceNode sn = null;
        InstanceIdentifier<ServiceNode> sfcIID;
        ServiceNodeKey serviceFunctionChainKey =
                new ServiceNodeKey(serviceNode);
        sfcIID = InstanceIdentifier.builder(ServiceNodes.class)
                .child(ServiceNode.class, serviceFunctionChainKey).build();

        sn = SfcDataStoreAPI.readTransactionAPI(sfcIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sn;
    }


    /**
     * This method reads the operational state for a service function.
     * <p>
     * @param serviceNode SN name
     * @return A ServiceNode object that is a list of all paths using
     * this service function, null otherwise
     */
    public static ServiceNode readServiceNodeExecutor(String serviceNode) {

        printTraceStart(LOG);
        ServiceNode ret = null;
        Object[] servicePathObj = {serviceNode};
        Class[] servicePathClass = {String.class};
        SfcProviderServiceNodeAPI sfcProviderServiceChainAPI = SfcProviderServiceNodeAPI
                .getRead(servicePathObj, servicePathClass);
        Future future  = ODL_SFC.getExecutor().submit(sfcProviderServiceChainAPI);
        try {
            ret = (ServiceNode) future.get();
            LOG.debug("getRead: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);         }
        printTraceStop(LOG);
        return ret;
    }
    /**
     * This method reads the operational state for a service function.
     * <p>
     *
     * @return A ServiceNode names that is a list of all paths using
     * this service node, null otherwise
     */
    public static List<String> readAllServiceNodes() {
        ServiceNodes sns = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceNodes> snsIID =
                InstanceIdentifier.builder(ServiceNodes.class).toInstance();

        sns = SfcDataStoreAPI.readTransactionAPI(snsIID, LogicalDatastoreType.CONFIGURATION);
        List<String> serviceNodeNames = new ArrayList<>();
        List<ServiceNode> serviceNodesLst = sns.getServiceNode();
        for (ServiceNode serviceNodetmp : serviceNodesLst) {
            String serviceNodeNameTmp = serviceNodetmp.getName();
            serviceNodeNames.add(serviceNodeNameTmp);
        }
        Collections.sort(serviceNodeNames);
        printTraceStop(LOG);
        return serviceNodeNames;
    }

}

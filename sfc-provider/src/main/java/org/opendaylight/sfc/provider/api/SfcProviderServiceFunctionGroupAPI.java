/*
 * Copyright (c) 2014 Contextream, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.ServiceFunctionGroups;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunctionGroup datastore.
 * <p>
 * It is normally called
 * from onDataChanged() through a executor service. We need to use an executor service because we
 * can
 * not operate on a datastore while on onDataChanged() context.
 *
 * @author Kfir Yeshayahu (kfir.yeshayahu@contextream.com)
 * @version 0.1
 *          <p>
 * @since 2015-02-14
 */
public class SfcProviderServiceFunctionGroupAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionGroupAPI.class);

    public static List<String> getSfgNameList(ServiceFunctionChain serviceFunctionChain) {
        List<String> ret = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunction = serviceFunctionChain.getSfcServiceFunction();
        LOG.debug("searching groups for chain {} which has the elements {}", serviceFunctionChain.getName(),
                serviceFunctionChain.getSfcServiceFunction());
        if (sfcServiceFunction != null) {
            for (SfcServiceFunction sf : sfcServiceFunction) {
                ServiceFunctionGroup sfg =
                        SfcProviderServiceFunctionGroupAPI.getServiceFunctionGroupByType(sf.getType());
                LOG.debug("look for service function group of type {} and found {}", sf.getType(), sfg);
                if (sfg != null) {
                    ret.add(sfg.getName());
                } else {
                    return null;
                }
            }
        }
        return ret;
    }

    /**
     * Reads a SFG from the datastore
     * <p>
     *
     * @param serviceFunctionGroupName name
     * @return ServiceFunctionGroup object or null if not found
     */
    public static ServiceFunctionGroup readServiceFunctionGroup(String serviceFunctionGroupName) {
        printTraceStart(LOG);
        ServiceFunctionGroup sfg;
        InstanceIdentifier<ServiceFunctionGroup> sfgIID;
        ServiceFunctionGroupKey serviceFunctionGroupKey = new ServiceFunctionGroupKey(serviceFunctionGroupName);
        sfgIID = InstanceIdentifier.builder(ServiceFunctionGroups.class)
            .child(ServiceFunctionGroup.class, serviceFunctionGroupKey)
            .build();

        sfg = SfcDataStoreAPI.readTransactionAPI(sfgIID, LogicalDatastoreType.CONFIGURATION);
        printTraceStop(LOG);
        return sfg;
    }

    /**
     * Reads a SFG from the datastore
     * <p>
     *
     * @param serviceFunctionType function type
     * @return ServiceFunctionGroup object or null if not found
     */
    protected static ServiceFunctionGroup getServiceFunctionGroupByType(SftTypeName serviceFunctionType) {
        printTraceStart(LOG);
        ServiceFunctionGroup sfg = null;
        InstanceIdentifier<ServiceFunctionGroups> sfgIID;
        sfgIID = InstanceIdentifier.builder(ServiceFunctionGroups.class).build();

        ServiceFunctionGroups sfgs = SfcDataStoreAPI.readTransactionAPI(sfgIID, LogicalDatastoreType.CONFIGURATION);

        if (sfgs != null) {
            for (ServiceFunctionGroup element : sfgs.getServiceFunctionGroup()) {
                if (element.getType().equals(serviceFunctionType)) {
                    sfg = element;
                    LOG.debug("found group " + sfg + " that matches type " + serviceFunctionType);
                    break;
                }
            }
        }
        if (sfg == null) {
            LOG.debug("didn't found group " + sfg + " that matches type " + serviceFunctionType);
        }
        printTraceStop(LOG);
        return sfg;
    }

    /**
     * Puts a SFG in the datastore
     * <p>
     *
     * @param sfg the ServiceFunctionGroup to put
     * @return boolean success or failure
     */
    public static boolean putServiceFunctionGroup(ServiceFunctionGroup sfg) {
        boolean ret;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionGroup> sfgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroups.class)
            .child(ServiceFunctionGroup.class, sfg.getKey())
            .build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(sfgEntryIID, sfg, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Deletes a SFG from the datastore
     * <p>
     *
     * @param serviceFunctionGroupName SFG name
     * @return boolean success of failure
     */
    protected static boolean deleteServiceFunctionGroup(String serviceFunctionGroupName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionGroupKey serviceFunctionGroupKey = new ServiceFunctionGroupKey(serviceFunctionGroupName);
        InstanceIdentifier<ServiceFunctionGroup> sfgEntryIID = InstanceIdentifier.builder(ServiceFunctionGroups.class)
            .child(ServiceFunctionGroup.class, serviceFunctionGroupKey)
            .build();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfgEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("{}: Could not delete SFG: {}", Thread.currentThread().getStackTrace()[1],
                    serviceFunctionGroupName);
        }
        printTraceStop(LOG);
        return ret;
    }

    /**
     * Adds a SF to SFG
     * <p>
     *
     * @param serviceFunctionGroupName SFG name
     * @param serviceFunctionName name of SF to add
     * @return boolean success of failure
     */
    protected static boolean addServiceFunctionToGroup(String serviceFunctionGroupName, SfName serviceFunctionName) {
        boolean ret = false;
        printTraceStart(LOG);

        // TODO Implement

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Removes a SF from SFG
     * <p>
     *
     * @param serviceFunctionGroupName SFG name
     * @param serviceFunctionName name of SF to remove
     * @return boolean success of failure
     */
    protected static boolean removeServiceFunctionFromGroup(String serviceFunctionGroupName,
            SfName serviceFunctionName) {
        boolean ret = false;
        printTraceStart(LOG);

        // TODO Implement

        printTraceStop(LOG);
        return ret;
    }
}

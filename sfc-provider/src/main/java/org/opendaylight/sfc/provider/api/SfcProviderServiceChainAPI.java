/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class has the APIs to operate on the ServiceFunctionChain
 * datastore.
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @author Vladimir Lavor (vladimir.lavor@pantheon.sk)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 * @since 2014-06-30
 */
public class SfcProviderServiceChainAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPI.class);

    /**
     * This method reads the service function chain specified by the given name from
     * the datastore
     *
     * @param serviceFunctionChainName SF name
     * @return A ServiceFunctionState object that is a list of all paths using
     *         this service function, null otherwise
     */
    public static ServiceFunctionChain readServiceFunctionChain(SfcName serviceFunctionChainName) {
        printTraceStart(LOG);
        ServiceFunctionChain sfc;
        InstanceIdentifier<ServiceFunctionChain> sfcIID;
        ServiceFunctionChainKey serviceFunctionChainKey = new ServiceFunctionChainKey(serviceFunctionChainName);
        sfcIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
            .child(ServiceFunctionChain.class, serviceFunctionChainKey)
            .build();

        sfc = SfcDataStoreAPI.readTransactionAPI(sfcIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return sfc;
    }

    /**
     * This method creates a SFC from the datastore.
     *
     * @param serviceFunctionChain SFC object
     * @return true if SFC was created, false otherwise
     */
    public static boolean putServiceFunctionChain(ServiceFunctionChain serviceFunctionChain) {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionChain> sfcEntryIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
            .child(ServiceFunctionChain.class, serviceFunctionChain.getKey())
            .build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(sfcEntryIID, serviceFunctionChain,
                LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("Failed to create Service Function Chain: {}", serviceFunctionChain);
        }

        printTraceStop(LOG);
        return ret;
    }
}

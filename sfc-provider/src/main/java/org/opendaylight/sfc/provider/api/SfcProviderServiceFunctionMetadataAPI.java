/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.ServiceFunctionMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadataKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.VariableMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.VariableMetadataKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the Service Function Metadata
 * <p>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Ruijing Guo (ruijing.guo@intel.com)
 * @version 0.1
 * <p>
 * @since 2015-10-10
 */
public class SfcProviderServiceFunctionMetadataAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionMetadataAPI.class);

    public static boolean putContextMetadata(ContextMetadata md) {
        boolean ret;
        InstanceIdentifier<ContextMetadata> mdIID;

        printTraceStart(LOG);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(ContextMetadata.class, md.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(mdIID, md, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    public static ContextMetadata readContextMetadata(String mdName) {
        printTraceStart(LOG);
        ContextMetadata md;
        InstanceIdentifier<ContextMetadata> mdIID;
        ContextMetadataKey mdKey = new ContextMetadataKey(mdName);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(ContextMetadata.class, mdKey).build();

        md = SfcDataStoreAPI.readTransactionAPI(mdIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return md;
    }

    public static boolean deleteContextMetadata (String mdName) {
        boolean ret = false;
        printTraceStart(LOG);
        InstanceIdentifier<ContextMetadata> mdIID;
        ContextMetadataKey mdKey = new ContextMetadataKey(mdName);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(ContextMetadata.class, mdKey).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(mdIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to delete service function context metadata {}",
                    Thread.currentThread().getStackTrace()[1], mdName);
        }

        printTraceStop(LOG);
        return ret;
    }

    public static boolean putVariableMetadata(VariableMetadata md) {
        boolean ret;
        InstanceIdentifier<VariableMetadata> mdIID;

        printTraceStart(LOG);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(VariableMetadata.class, md.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(mdIID, md, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    public static VariableMetadata readVariableMetadata(String mdName) {
        VariableMetadata md;
        InstanceIdentifier<VariableMetadata> mdIID;

        printTraceStart(LOG);
        VariableMetadataKey mdKey = new VariableMetadataKey(mdName);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(VariableMetadata.class, mdKey).build();

        md = SfcDataStoreAPI.readTransactionAPI(mdIID, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return md;
    }

    public static boolean deleteVariableMetadata (String mdName) {
        boolean ret = false;
        InstanceIdentifier<VariableMetadata> mdIID;

        printTraceStart(LOG);
        VariableMetadataKey mdKey = new VariableMetadataKey(mdName);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(VariableMetadata.class, mdKey).build();

        if (SfcDataStoreAPI.deleteTransactionAPI(mdIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        } else {
            LOG.error("{}: Failed to delete variable metadata: {}",
                    Thread.currentThread().getStackTrace()[1], mdName);
        }

        printTraceStop(LOG);
        return ret;
    }
}

/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.SfcReflection;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.ServiceFunctionMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadataKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.VariableMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.VariableMetadataKey;


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
public class SfcProviderServiceFunctionMetadataAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionMetadataAPI.class);
    private static final String FAILED_TO_STR = "failed to ...";

    SfcProviderServiceFunctionMetadataAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceFunctionMetadataAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceFunctionMetadataAPI getPutContextMetadata(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionMetadataAPI(params, paramsTypes, "putContextMetadata");
    }

    public static SfcProviderServiceFunctionMetadataAPI getReadContextMetadata(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionMetadataAPI(params, paramsTypes, "readContextMetadata");
    }

    public static SfcProviderServiceFunctionMetadataAPI getDeleteContextMetadata(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionMetadataAPI(params, paramsTypes, "deleteContextMetadata");
    }

    public static SfcProviderServiceFunctionMetadataAPI getPutVariableMetadata(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionMetadataAPI(params, paramsTypes, "putVariableMetadata");
    }

    public static SfcProviderServiceFunctionMetadataAPI getReadVariableMetadata(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionMetadataAPI(params, paramsTypes, "readVariableMetadata");
    }


    public static SfcProviderServiceFunctionMetadataAPI getDeleteVariableMetadata(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionMetadataAPI(params, paramsTypes, "deleteVariableMetadata");
    }

    @SuppressWarnings("unused")
    @SfcReflection
    protected boolean putContextMetadata(ContextMetadata md) {
        boolean ret = false;
        InstanceIdentifier<ContextMetadata> mdIID;

        printTraceStart(LOG);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(ContextMetadata.class, md.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(mdIID, md, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    public static boolean putContextMetadataExecutor(ContextMetadata md) {
        boolean ret = false;
        Object[] functionParamsObj = {md};
        Class[] functionParamsClass = {ContextMetadata.class};

        printTraceStart(LOG);
        Future future  = ODL_SFC.getExecutor().submit(SfcProviderServiceFunctionMetadataAPI
                .getPutContextMetadata(functionParamsObj, functionParamsClass));
        try {
            ret = (boolean) future.get();
            LOG.debug("getPutContextMetadata: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    @SfcReflection
    protected ContextMetadata readContextMetadata(String mdName) {
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

    public static ContextMetadata readContextMetadataExecutor(String mdName) {
        printTraceStart(LOG);
        ContextMetadata ret = null;
        Object[] functionParamsObj = {mdName};
        Class[] functionParamsClass = {String.class};
        Future future  = ODL_SFC.getExecutor().submit(SfcProviderServiceFunctionMetadataAPI
                .getReadContextMetadata(functionParamsObj, functionParamsClass));
        try {
            ret = (ContextMetadata) future.get();
            LOG.debug("getReadContextMetadata: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static boolean deleteContextMetadataExecutor (String mdName) {
        boolean ret = false;
        Object[] functionParams = {mdName};
        Class[] functionParamsTypes = {String.class};

        printTraceStart(LOG);
        Future future = ODL_SFC.getExecutor().submit(SfcProviderServiceFunctionMetadataAPI
                .getDeleteContextMetadata(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteContextMetadata returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    @SfcReflection
    protected boolean putVariableMetadata(VariableMetadata md) {
        boolean ret = false;
        InstanceIdentifier<VariableMetadata> mdIID;

        printTraceStart(LOG);
        mdIID = InstanceIdentifier.builder(ServiceFunctionMetadata.class)
                   .child(VariableMetadata.class, md.getKey()).build();

        ret = SfcDataStoreAPI.writePutTransactionAPI(mdIID, md, LogicalDatastoreType.CONFIGURATION);

        printTraceStop(LOG);
        return ret;
    }

    public static boolean putVariableMetadataExecutor(VariableMetadata md) {
        boolean ret = false;
        Object[] functionParamsObj = {md};
        Class[] functionParamsClass = {VariableMetadata.class};

        printTraceStart(LOG);
        Future future  = ODL_SFC.getExecutor().submit(SfcProviderServiceFunctionMetadataAPI
                .getPutVariableMetadata(functionParamsObj, functionParamsClass));
        try {
            ret = (boolean) future.get();
            LOG.debug("getPutVariableMetadata: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }

    @SuppressWarnings("unused")
    @SfcReflection
    protected VariableMetadata readVariableMetadata(String mdName) {
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

    public static VariableMetadata readVariableMetadataExecutor(String mdName) {
        VariableMetadata ret = null;
        Object[] functionParamsObj = {mdName};
        Class[] functionParamsClass = {String.class};

        printTraceStart(LOG);
        Future future  = ODL_SFC.getExecutor().submit(SfcProviderServiceFunctionMetadataAPI
                .getReadVariableMetadata(functionParamsObj, functionParamsClass));
        try {
            ret = (VariableMetadata) future.get();
            LOG.debug("getReadVariableMetadata: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn("failed to ...." , e);
        } catch (ExecutionException e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return ret;
    }


    @SuppressWarnings("unused")
    public static boolean deleteVariableMetadata (String mdName) {
        boolean ret = false;
        VariableMetadata md;
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

    @SuppressWarnings("unused")
    public static boolean deleteVariableMetadataExecutor (String mdName) {
        boolean ret = false;
        Object[] functionParams = {mdName};
        Class[] functionParamsTypes = {String.class};

        printTraceStart(LOG);
        Future future = ODL_SFC.getExecutor().submit(SfcProviderServiceFunctionMetadataAPI
                .getDeleteVariableMetadata(functionParams, functionParamsTypes));
        try {
            ret = (boolean) future.get();
            LOG.debug("getDeleteVariableMetadata returns: {}", future.get());
        } catch (InterruptedException e) {
            LOG.warn(FAILED_TO_STR , e);
        } catch (ExecutionException e) {
            LOG.warn(FAILED_TO_STR , e);
        }
        printTraceStop(LOG);
        return ret;
    }
}

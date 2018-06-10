/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcServicePathId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.AllocatePathIdInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.AllocatePathIdOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.DeletePathIdInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.DeletePathIdOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ReadPathIdInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ReadPathIdOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ReservePathIdRangeInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ReservePathIdRangeOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ServicePathIdService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.SetGenerationAlgorithmInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.SetGenerationAlgorithmOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.SetGenerationAlgorithmOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePathService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.TraceRenderedServicePathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.TraceRenderedServicePathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.DeleteAllServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.DeleteAllServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.DeleteServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.DeleteServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ReadServiceFunctionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.InstantiateServiceFunctionChainInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.InstantiateServiceFunctionChainOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds all RPCs methods for SFC Provider.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-06-30
 */

public class SfcProviderRpc implements ServiceFunctionService, ServiceFunctionChainService, RenderedServicePathService,
        ServicePathIdService {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderRpc.class);
    private final DataBroker dataBroker;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor("SfcProviderRpc", LOG);

    public SfcProviderRpc(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    // This RPC call is Deprecated Fluorine, and will be removed in Neon.
    // This same operation can be performed with a simple RESTconf DELETE.
    @Deprecated
    @Override
    public ListenableFuture<RpcResult<DeleteAllServiceFunctionOutput>> deleteAllServiceFunction(
            DeleteAllServiceFunctionInput input) {
        return null;
    }

    // This RPC call is Deprecated Fluorine, and will be removed in Neon.
    // This same operation can be performed with a simple RESTconf DELETE.
    @Deprecated
    @Override
    public ListenableFuture<RpcResult<DeleteServiceFunctionOutput>> deleteServiceFunction(
            DeleteServiceFunctionInput input) {
        return null;
    }

    // This RPC call is Deprecated Fluorine, and will be removed in Neon.
    // This same operation can be performed with a simple RESTconf PUT.
    @Deprecated
    @Override
    public ListenableFuture<RpcResult<PutServiceFunctionOutput>> putServiceFunction(PutServiceFunctionInput input) {
        printTraceStart(LOG);
        LOG.info("\n####### Input: {}", input);

        if (dataBroker == null) {
            return RpcResultBuilder.<PutServiceFunctionOutput>failed()
                    .withError(ErrorType.APPLICATION, "No data provider.").buildFuture();
        }

        // Data PLane Locator
        List<SfDataPlaneLocator> sfDataPlaneLocatorList = input.getSfDataPlaneLocator();

        ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
        ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
        ServiceFunction sf = sfbuilder.setName(input.getName()).setType(input.getType()).withKey(sfkey)
                .setIpMgmtAddress(input.getIpMgmtAddress()).setSfDataPlaneLocator(sfDataPlaneLocatorList).build();

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, sf.key()).build();

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, sfEntryIID, sf, true);
        printTraceStop(LOG);
        return Futures.transform(writeTx.submit(), unused ->
            RpcResultBuilder.<PutServiceFunctionOutput>success(new PutServiceFunctionOutputBuilder().build()).build(),
            MoreExecutors.directExecutor());
    }

    // This RPC call is Deprecated Fluorine, and will be removed in Neon.
    // This same operation can be performed with a simple RESTconf GET.
    @Deprecated
    @Override
    public ListenableFuture<RpcResult<ReadServiceFunctionOutput>> readServiceFunction(ReadServiceFunctionInput input) {
        LOG.info("Input: {}", input);

        if (dataBroker != null) {
            ServiceFunctionKey sfkey = new ServiceFunctionKey(new SfName(input.getName()));
            InstanceIdentifier<ServiceFunction> sfIID =
                    InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class, sfkey).build();

            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunction> dataObject = Optional.absent();
            try {
                dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.debug("Failed to readServiceFunction", e);
            }
            if (dataObject.isPresent()) {
                ServiceFunction serviceFunction = dataObject.get();
                LOG.debug("readServiceFunction Success: {}", serviceFunction.getName());
                ReadServiceFunctionOutput readServiceFunctionOutput;
                ReadServiceFunctionOutputBuilder outputBuilder = new ReadServiceFunctionOutputBuilder();
                outputBuilder.setName(serviceFunction.getName()).setType(serviceFunction.getType())
                        .setIpMgmtAddress(serviceFunction.getIpMgmtAddress())
                        .setSfDataPlaneLocator(serviceFunction.getSfDataPlaneLocator());
                readServiceFunctionOutput = outputBuilder.build();
                return RpcResultBuilder.success(readServiceFunctionOutput).buildFuture();
            }
            return RpcResultBuilder.<ReadServiceFunctionOutput>success().buildFuture();
        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
            return RpcResultBuilder.<ReadServiceFunctionOutput>success().buildFuture();
        }
    }

    // This RPC call is Deprecated Fluorine, and will be removed in Neon.
    // This same operation can be performed with a simple RESTconf PUT.
    @Deprecated
    @Override
    public ListenableFuture<RpcResult<InstantiateServiceFunctionChainOutput>> instantiateServiceFunctionChain(
            InstantiateServiceFunctionChainInput input) {
        return null;
    }

    // This RPC call is Deprecated Fluorine, and will be removed in Neon.
    // This same operation can be performed with a simple RESTconf PUT.
    @Deprecated
    @Override
    public ListenableFuture<RpcResult<PutServiceFunctionChainsOutput>> putServiceFunctionChains(
            PutServiceFunctionChainsInput input) {
        printTraceStart(LOG);
        ServiceFunctionChainsBuilder serviceFunctionChainsBuilder = new ServiceFunctionChainsBuilder();
        serviceFunctionChainsBuilder = serviceFunctionChainsBuilder
                .setServiceFunctionChain(input.getServiceFunctionChain());
        ServiceFunctionChains sfcs = serviceFunctionChainsBuilder.build();

        if (!SfcDataStoreAPI.writeMergeTransactionAPI(SfcInstanceIdentifiers.SFC_IID, sfcs,
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to create service function chain: {}", input.getServiceFunctionChain().toString());
        }
        return RpcResultBuilder.<PutServiceFunctionChainsOutput>success(
                new PutServiceFunctionChainsOutputBuilder().build()).buildFuture();
    }

    // This runnable will be scheduled with periodic delay as a result of
    // create/delete Rendered Path RPC. It is in charge of dealing with the
    // config RSP and waiting to be reflected in the operational data store.
    // Once it happens, it will provide the result of the RPC and cancel itself.
    private abstract static class RenderedPathOperImpl<T extends DataObject> implements Runnable {
        private volatile Future backingFuture = null;
        private final SettableFuture<RpcResult<T>> result;
        private int retriesLeft;

        private RenderedPathOperImpl(SettableFuture<RpcResult<T>> result, int retries) {
            Preconditions.checkArgument(retries > 0, "retries must be greater than 0");
            this.result = result;
            this.retriesLeft = retries;
        }

        @Override
        @SuppressWarnings("checkstyle:illegalcatch")
        public void run() {
            // if we are done but somehow still running, cancel ourselves.
            if (result.isDone()) {
                cancelBackingFuture();
                return;
            }

            RpcResult<T> operationResult = null;
            try {
                operationResult = doOperation();
            } catch (RuntimeException e) {
                result.setException(e);
                cancelBackingFuture();
                return;
            }

            if (operationResult != null) {
                result.set(operationResult);
                cancelBackingFuture();
                return;
            }

            if (!result.isDone() && retriesLeft-- <= 0) {
                result.set(completeError("Unexpected timeout while waiting for operation to complete"));
                cancelBackingFuture();
            }
        }

        void setBackingFuture(Future backingFuture) {
            this.backingFuture = backingFuture;
        }

        boolean cancelBackingFuture() {
            return backingFuture != null && backingFuture.cancel(false);
        }

        // Should return null if result not available yet.
        protected abstract RpcResult<T> doOperation();

        RpcResult<T> completeError(String errorMsg) {
            return RpcResultBuilder.<T>failed().withError(ErrorType.APPLICATION, errorMsg).build();
        }
    }

    /**
     * This method gets all necessary information for a system to construct a
     * NSH header and associated overlay packet to target the first service hop
     * of a Rendered Service Path.
     *
     * <p>
     * @param input
     *            RPC input including a Rendered Service Path name
     * @return RPC output including a renderedServicePathFirstHop.
     */
    @Override
    public ListenableFuture<RpcResult<ReadRenderedServicePathFirstHopOutput>> readRenderedServicePathFirstHop(
            ReadRenderedServicePathFirstHopInput input) {

        RenderedServicePathFirstHop renderedServicePathFirstHop;
        RpcResultBuilder<ReadRenderedServicePathFirstHopOutput> rpcResultBuilder;

        renderedServicePathFirstHop = SfcProviderRenderedPathAPI
                .readRenderedServicePathFirstHop(new RspName(input.getName()));

        ReadRenderedServicePathFirstHopOutput renderedServicePathFirstHopOutput;
        if (renderedServicePathFirstHop != null) {
            ReadRenderedServicePathFirstHopOutputBuilder renderedServicePathFirstHopOutputBuilder =
                    new ReadRenderedServicePathFirstHopOutputBuilder();
            renderedServicePathFirstHopOutputBuilder.setRenderedServicePathFirstHop(renderedServicePathFirstHop);
            renderedServicePathFirstHopOutput = renderedServicePathFirstHopOutputBuilder.build();

            rpcResultBuilder = RpcResultBuilder.success(renderedServicePathFirstHopOutput);
        } else {
            String message = "Error Reading RSP First Hop from DataStore: " + input.getName();
            rpcResultBuilder = RpcResultBuilder.<ReadRenderedServicePathFirstHopOutput>failed()
                    .withError(ErrorType.APPLICATION, message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<TraceRenderedServicePathOutput>> traceRenderedServicePath(
            TraceRenderedServicePathInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<ReservePathIdRangeOutput>> reservePathIdRange(ReservePathIdRangeInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<ReadPathIdOutput>> readPathId(ReadPathIdInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AllocatePathIdOutput>> allocatePathId(AllocatePathIdInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeletePathIdOutput>> deletePathId(DeletePathIdInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<SetGenerationAlgorithmOutput>> setGenerationAlgorithm(
            SetGenerationAlgorithmInput input) {

        boolean result = SfcServicePathId.setGenerationAlgorithm(input.getGenerationAlgorithm());

        SetGenerationAlgorithmOutputBuilder setGenerationAlgorithmOutputBuilder =
                new SetGenerationAlgorithmOutputBuilder();
        setGenerationAlgorithmOutputBuilder.setResult(result);
        RpcResultBuilder<SetGenerationAlgorithmOutput> rpcResultBuilder = RpcResultBuilder
                .success(setGenerationAlgorithmOutputBuilder.build());

        return rpcResultBuilder.buildFuture();
    }
}

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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.DeleteRenderedPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRspFirstHopBySftListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePathService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.TraceRenderedServicePathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.TraceRenderedServicePathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.DeleteServiceFunctionInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.PutServiceFunctionInput;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
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

    @Override
    public Future<RpcResult<Void>> deleteAllServiceFunction() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> deleteServiceFunction(DeleteServiceFunctionInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunction(PutServiceFunctionInput input) {
        printTraceStart(LOG);
        LOG.info("\n####### Input: {}", input);

        if (dataBroker == null) {
            return Futures.immediateFuture(
                    RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "No data provider.").build());
        }

        // Data PLane Locator
        List<SfDataPlaneLocator> sfDataPlaneLocatorList = input.getSfDataPlaneLocator();

        ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
        ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
        ServiceFunction sf = sfbuilder.setName(input.getName()).setType(input.getType()).setKey(sfkey)
                .setIpMgmtAddress(input.getIpMgmtAddress()).setSfDataPlaneLocator(sfDataPlaneLocatorList).build();

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, sf.getKey()).build();

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, sfEntryIID, sf, true);
        printTraceStop(LOG);
        return Futures.transform(writeTx.submit(), input1 -> RpcResultBuilder.<Void>success().build(),
                MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<ReadServiceFunctionOutput>> readServiceFunction(ReadServiceFunctionInput input) {
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

    @Override
    public Future<RpcResult<InstantiateServiceFunctionChainOutput>> instantiateServiceFunctionChain(
            InstantiateServiceFunctionChainInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunctionChains(PutServiceFunctionChainsInput input) {
        printTraceStart(LOG);
        ServiceFunctionChainsBuilder serviceFunctionChainsBuilder = new ServiceFunctionChainsBuilder();
        serviceFunctionChainsBuilder = serviceFunctionChainsBuilder
                .setServiceFunctionChain(input.getServiceFunctionChain());
        ServiceFunctionChains sfcs = serviceFunctionChainsBuilder.build();

        if (!SfcDataStoreAPI.writeMergeTransactionAPI(SfcInstanceIdentifiers.SFC_IID, sfcs,
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to create service function chain: {}", input.getServiceFunctionChain().toString());
        }
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    /**
     * Create an RSP via an RPC operation. As of Oxygen, this method is
     * deprecated. Now, instead of using the RPC, the RSP creation will
     * be triggered via SFP creation.
     * If the supplied RspName is not present, then the RSP would have
     * been created when the SFP was created, so this already created
     * RSP name will be returned.
     * If the supplied RspName is present, then this will create an RSP
     * in the config data store, which will trigger creating the RSP
     * in the operational data store. This will be for the case where
     * the end-user wants multiple RPSs for 1 SFP. This methodology
     * will no longer be supported when this deprecated RPC is removed.
     *
     * <p>
     * @param createRenderedPathInput
     *        Input information used to create the RSP.
     * @return RPC Output
     */
    @Deprecated
    @Override
    public Future<RpcResult<CreateRenderedPathOutput>> createRenderedPath(
            CreateRenderedPathInput createRenderedPathInput) {
        SettableFuture<RpcResult<CreateRenderedPathOutput>> futureResult = SettableFuture.create();
        CreateRenderedPathImpl runnable = new CreateRenderedPathImpl(createRenderedPathInput, futureResult);
        ScheduledFuture<?> scheduledFuture = executor.scheduleWithFixedDelay(
                runnable,
                0,
                100,
                TimeUnit.MILLISECONDS);
        runnable.setBackingFuture(scheduledFuture);
        return futureResult;
    }

    // This runnable will be scheduled with periodic delay
    // as a result createRenderedPath RPC. It is in charge of creating
    // the config RSP and waiting for the corresponding oper RSP.
    // Once it happens, it will provide the result of the RPC
    // and cancel itself.
    private static class CreateRenderedPathImpl implements Runnable {

        private CreateRenderedPathInput createRenderedPathInput;
        private SettableFuture<RpcResult<CreateRenderedPathOutput>> result;
        private volatile Future backingFuture = null;

        CreateRenderedPathImpl(CreateRenderedPathInput createRenderedPathInput,
                               SettableFuture<RpcResult<CreateRenderedPathOutput>> result) {
            this.createRenderedPathInput = createRenderedPathInput;
            this.result = result;
        }

        @Override
        public void run() {

            // if we are done but somehow still running, cancel ourself.
            if (result.isDone()) {
                cancelBackingFuture();
                return;
            }

            final String inputRspNameValue = createRenderedPathInput.getName();
            final RspName inputRspName = new RspName(inputRspNameValue);

            // If the operational RSP already exists, give it back and complete
            RenderedServicePath operRSp = SfcProviderRenderedPathAPI.readRenderedServicePath(
                    inputRspName,
                    LogicalDatastoreType.OPERATIONAL);
            if (operRSp != null) {
                completeSuccess(inputRspNameValue);
                return;
            }

            // If the config RSP already exists, we just have to wait for the operational RSP
            RenderedServicePath configRsp = SfcProviderRenderedPathAPI.readRenderedServicePath(
                    inputRspName,
                    LogicalDatastoreType.CONFIGURATION);
            if (configRsp != null) {
                return;
            }

            // Fail if the SFP doesnt exist
            ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI
                    .readServiceFunctionPath(new SfpName(createRenderedPathInput.getParentServiceFunctionPath()));
            if (serviceFunctionPath == null) {
                completeError("Service Function Path does not exist");
                return;
            }

            // If the input name is empty, then the RSP was already created when
            // the SFP was created, so nothing to do but to return the RSP name
            if (inputRspNameValue == null || inputRspNameValue.isEmpty()) {
                // Iterate the RPSs created for this SFP looking for the correct name to return
                // If the RspName isnt found, then fall through and create the RSP
                List<SfpRenderedServicePath> sfpRspList =
                        SfcProviderServicePathAPI.readServicePathState(serviceFunctionPath.getName());
                // In case this RPC was called before the RSP listeners complete and
                // the sfpRspList hasnt been created yet, git it a chance to complete.
                // This RPC will be removed in Fluorine.
                if (sfpRspList == null || sfpRspList.isEmpty()) {
                    return;
                }

                for (SfpRenderedServicePath sfpRsp : sfpRspList) {
                    RspName rspName = sfpRsp.getName();
                    if (rspName.getValue().startsWith(serviceFunctionPath.getName().getValue())
                            && !rspName.getValue().endsWith("-Reverse")) {
                        completeSuccess(rspName.getValue());
                        return;
                    }
                }
            }

            // Go ahead and create the RSP with the provided inputRspNameValue
            // The symmetric RSP will optionally be created in createRenderedServicePathInConfig()
            configRsp = SfcProviderRenderedPathAPI.createRenderedServicePathInConfig(
                    serviceFunctionPath,
                    inputRspNameValue);
            if (configRsp == null) {
                completeError("Failed to create RSP");
            }
        }

        private void setBackingFuture(Future backingFuture) {
            this.backingFuture = backingFuture;
        }

        private boolean cancelBackingFuture() {
            return backingFuture != null && backingFuture.cancel(false);
        }

        private boolean completeSuccess(String rspName) {
            CreateRenderedPathOutput createRenderedPathOutput = new CreateRenderedPathOutputBuilder()
                    .setName(rspName)
                    .build();
            RpcResult<CreateRenderedPathOutput> rpcResult = RpcResultBuilder.success(createRenderedPathOutput).build();
            result.set(rpcResult);
            return cancelBackingFuture();
        }

        private boolean completeError(String errorMsg) {
            RpcResult<CreateRenderedPathOutput> rpcResult = RpcResultBuilder.<CreateRenderedPathOutput>failed()
                    .withError(ErrorType.APPLICATION, errorMsg).build();
            result.set(rpcResult);
            return cancelBackingFuture();
        }

    }

    /**
     * When a RSP is deleted, it has to be removed from: SFF, SF and RSP
     * operational state.
     *
     * <p>
     * @param rspName
     *            RspName object with the Rendered Service Path
     * @return true if all path was deleted, false otherwise.
     */
    private boolean deleteRenderedPathWithRspName(RspName rspName) {

        boolean ret;
        ret = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName);
        ret = ret && SfcProviderServiceFunctionAPI.deleteRspFromServiceFunctionState(rspName);

        ret = ret && SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName);
        return ret;
    }

    /**
     * Remove RSP from all the operational state.
     *
     * <p>
     * @param input
     *            schema path
     *            <i>rendered-service-path/delete-rendered-path/input</i>
     * @return RPC output
     */
    @Override
    public Future<RpcResult<DeleteRenderedPathOutput>> deleteRenderedPath(DeleteRenderedPathInput input) {

        boolean ret = true;
        RpcResultBuilder<DeleteRenderedPathOutput> rpcResultBuilder;
        RspName rspName = new RspName(input.getName());
        RspName reverseRspName = SfcProviderRenderedPathAPI.getReversedRspName(rspName);
        if (reverseRspName != null) {
            // The RSP has a symmetric ("Reverse") Path
            ret = this.deleteRenderedPathWithRspName(reverseRspName);
        }
        ret = ret && this.deleteRenderedPathWithRspName(rspName);

        DeleteRenderedPathOutputBuilder deleteRenderedPathOutputBuilder = new DeleteRenderedPathOutputBuilder();
        deleteRenderedPathOutputBuilder.setResult(ret);
        if (ret) {
            rpcResultBuilder = RpcResultBuilder.success(deleteRenderedPathOutputBuilder.build());
        } else {
            String message = "Error Deleting Rendered Service Path: " + input.getName();
            rpcResultBuilder = RpcResultBuilder.<DeleteRenderedPathOutput>failed().withError(ErrorType.APPLICATION,
                    message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
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
    public Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> readRenderedServicePathFirstHop(
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

    /**
     * This method reads all the necessary information for the first hop of a
     * Rendered Service Path by ServiceFunctionTypeIdentity list.
     *
     * <p>
     * @param input
     *            RPC input including a ServiceFunctionTypeIdentity list
     * @return RPC output including a renderedServicePathFirstHop.
     */
    @Override
    public Future<RpcResult<ReadRspFirstHopBySftListOutput>> readRspFirstHopBySftList(
            ReadRspFirstHopBySftListInput input) {
        RenderedServicePathFirstHop renderedServicePathFirstHop;
        renderedServicePathFirstHop = SfcProviderRenderedPathAPI.readRspFirstHopBySftList(input.getSfst(),
                input.getSftList());
        ReadRspFirstHopBySftListOutput readRspFirstHopBySftListOutput = null;
        if (renderedServicePathFirstHop != null) {
            ReadRspFirstHopBySftListOutputBuilder readRspFirstHopBySftListOutputBuilder =
                    new ReadRspFirstHopBySftListOutputBuilder();
            readRspFirstHopBySftListOutputBuilder.setRenderedServicePathFirstHop(renderedServicePathFirstHop);
            readRspFirstHopBySftListOutput = readRspFirstHopBySftListOutputBuilder.build();
        }

        RpcResultBuilder<ReadRspFirstHopBySftListOutput> rpcResultBuilder = RpcResultBuilder
                .success(readRspFirstHopBySftListOutput);
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<TraceRenderedServicePathOutput>> traceRenderedServicePath(
            TraceRenderedServicePathInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<ReservePathIdRangeOutput>> reservePathIdRange(ReservePathIdRangeInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<ReadPathIdOutput>> readPathId(ReadPathIdInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<AllocatePathIdOutput>> allocatePathId(AllocatePathIdInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<DeletePathIdOutput>> deletePathId(DeletePathIdInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<SetGenerationAlgorithmOutput>> setGenerationAlgorithm(SetGenerationAlgorithmInput input) {

        boolean result = SfcServicePathId.setGenerationAlgorithm(input.getGenerationAlgorithm());

        SetGenerationAlgorithmOutputBuilder setGenerationAlgorithmOutputBuilder =
                new SetGenerationAlgorithmOutputBuilder();
        setGenerationAlgorithmOutputBuilder.setResult(result);
        RpcResultBuilder<SetGenerationAlgorithmOutput> rpcResultBuilder = RpcResultBuilder
                .success(setGenerationAlgorithmOutputBuilder.build());

        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}

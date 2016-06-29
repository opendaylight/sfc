/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class holds all RPCs methods for SFC Provider.
 * <p>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-06-30
 */

public class SfcProviderRpc implements ServiceFunctionService, ServiceFunctionChainService, RenderedServicePathService,
        ServicePathIdService {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderRpc.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private DataBroker dataBroker = odlSfc.getDataProvider();

    public static SfcProviderRpc getSfcProviderRpc() {
        return new SfcProviderRpc();
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
        LOG.info("\n####### Input: " + input);

        if (dataBroker == null) {
            return Futures.immediateFuture(
                    RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "No data provider.").build());
        }

        // Data PLane Locator
        List<SfDataPlaneLocator> sfDataPlaneLocatorList = input.getSfDataPlaneLocator();

        ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
        ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
        ServiceFunction sf = sfbuilder.setName(input.getName())
            .setType(input.getType())
            .setKey(sfkey)
            .setIpMgmtAddress(input.getIpMgmtAddress())
            .setSfDataPlaneLocator(sfDataPlaneLocatorList)
            .build();

        InstanceIdentifier<ServiceFunction> sfEntryIID =
                InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class, sf.getKey()).build();

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, sfEntryIID, sf, true);
        printTraceStop(LOG);
        return Futures.transform(writeTx.submit(), new Function<Void, RpcResult<Void>>() {

            @Override
            public RpcResult<Void> apply(Void input) {
                return RpcResultBuilder.<Void>success().build();
            }
        });
    }

    @Override
    public Future<RpcResult<ReadServiceFunctionOutput>> readServiceFunction(ReadServiceFunctionInput input) {
        printTraceStart(LOG);
        LOG.info("Input: " + input);

        if (dataBroker != null) {
            ServiceFunctionKey sfkey = new ServiceFunctionKey(new SfName(input.getName()));
            InstanceIdentifier<ServiceFunction> sfIID;
            sfIID = InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class, sfkey).build();

            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunction> dataObject = null;
            try {
                dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.debug("Failed to readServiceFunction : {}", e.getMessage());
            }
            if (dataObject != null && dataObject.isPresent()) {
                ServiceFunction serviceFunction = dataObject.get();
                LOG.debug("readServiceFunction Success: {}", serviceFunction.getName());
                ReadServiceFunctionOutput readServiceFunctionOutput = null;
                ReadServiceFunctionOutputBuilder outputBuilder = new ReadServiceFunctionOutputBuilder();
                outputBuilder.setName(serviceFunction.getName())
                    .setType(serviceFunction.getType())
                    .setIpMgmtAddress(serviceFunction.getIpMgmtAddress())
                    .setSfDataPlaneLocator(serviceFunction.getSfDataPlaneLocator());
                readServiceFunctionOutput = outputBuilder.build();
                printTraceStop(LOG);
                return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true,
                        readServiceFunctionOutput, Collections.<RpcError>emptySet()));
            }
            printTraceStop(LOG);
            return Futures.immediateFuture(
                    Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
            printTraceStop(LOG);
            return Futures.immediateFuture(
                    Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
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
        serviceFunctionChainsBuilder =
                serviceFunctionChainsBuilder.setServiceFunctionChain(input.getServiceFunctionChain());
        ServiceFunctionChains sfcs = serviceFunctionChainsBuilder.build();

        if (!SfcDataStoreAPI.writeMergeTransactionAPI(OpendaylightSfc.SFC_IID, sfcs,
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to create service function chain: {}", input.getServiceFunctionChain().toString());
        }
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true, Collections.<RpcError>emptySet()));
    }

    @SuppressWarnings("unused")
    private ServiceFunctionChain findServiceFunctionChain(SfcName name) {
        ServiceFunctionChainKey key = new ServiceFunctionChainKey(name);
        InstanceIdentifier<ServiceFunctionChain> serviceFunctionChainInstanceIdentifier =
                InstanceIdentifier.builder(ServiceFunctionChains.class).child(ServiceFunctionChain.class, key).build();

        ServiceFunctionChain serviceFunctionChain = SfcDataStoreAPI
            .readTransactionAPI(serviceFunctionChainInstanceIdentifier, LogicalDatastoreType.CONFIGURATION);
        if (serviceFunctionChain == null) {
            LOG.error("Failed to find Service Function Chain: {}", name);
        }
        return serviceFunctionChain;
    }

    @Override
    public Future<RpcResult<CreateRenderedPathOutput>> createRenderedPath(
            CreateRenderedPathInput createRenderedPathInput) {

        ServiceFunctionPath createdServiceFunctionPath;
        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;
        CreateRenderedPathOutputBuilder createRenderedPathOutputBuilder = new CreateRenderedPathOutputBuilder();
        RpcResult<CreateRenderedPathOutput> rpcResult;
        RspName retRspName = null;

        createdServiceFunctionPath = SfcProviderServicePathAPI
            .readServiceFunctionPath(new SfpName(createRenderedPathInput.getParentServiceFunctionPath()));

        if (createdServiceFunctionPath != null) {
            renderedServicePath = SfcProviderRenderedPathAPI
                .createRenderedServicePathAndState(createdServiceFunctionPath, createRenderedPathInput);
            if (renderedServicePath != null) {
                retRspName = renderedServicePath.getName();
                createRenderedPathOutputBuilder.setName(retRspName.getValue());
                rpcResult = RpcResultBuilder.success(createRenderedPathOutputBuilder.build()).build();
                if ((createdServiceFunctionPath.isSymmetric() != null) && createdServiceFunctionPath.isSymmetric()) {

                    revRenderedServicePath =
                            SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
                    if (revRenderedServicePath == null) {
                        LOG.error("Failed to create symmetric service path: {}");
                    } else {
                        SfcProviderRenderedPathAPI.setSymmetricPathId(renderedServicePath,
                                revRenderedServicePath.getPathId());
                    }
                }
            } else {
                rpcResult = RpcResultBuilder.<CreateRenderedPathOutput>failed()
                    .withError(ErrorType.APPLICATION, "Failed to create RSP")
                    .build();
            }

        } else {
            rpcResult = RpcResultBuilder.<CreateRenderedPathOutput>failed()
                .withError(ErrorType.APPLICATION, "Service Function Path does not exist")
                .build();
        }
        return Futures.immediateFuture(rpcResult);
    }

    @Override
    public Future<RpcResult<DeleteRenderedPathOutput>> deleteRenderedPath(DeleteRenderedPathInput input) {

        boolean ret;
        RpcResultBuilder<DeleteRenderedPathOutput> rpcResultBuilder;
        // If a RSP is deleted we delete both SF and SFF operational states.
        // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
        /*
         * XXX TODO Another example of Method overloading signature type confusion brought about by
         * Strings. Fixing typing in RPC input/output will be another patch.
         * Suggest after changing RPC inputs to be typedefs, that method names be cleaned up too.
         * For instance:
         * deletePathFromServiceForwarderState
         * has signatures (ServiceFunctionPath) and what used to be (String) which is now (RspName)
         * and another for SfpName so I *assume* I'm supposed to use SfpName here
         * and test and hope for the best but unintended consequences if people see things like
         * rspName used in place of sfpName and re-use same pattern elsewhere.
         */
        SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(new RspName(input.getName()));
        SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(new SfpName(input.getName()));

        ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(new RspName(input.getName()));
        DeleteRenderedPathOutputBuilder deleteRenderedPathOutputBuilder = new DeleteRenderedPathOutputBuilder();
        deleteRenderedPathOutputBuilder.setResult(ret);
        if (ret) {
            rpcResultBuilder = RpcResultBuilder.success(deleteRenderedPathOutputBuilder.build());
        } else {
            String message = "Error Deleting Rendered Service Path: " + input.getName();
            rpcResultBuilder =
                    RpcResultBuilder.<DeleteRenderedPathOutput>failed().withError(ErrorType.APPLICATION, message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    /**
     * This method gets all necessary information for a system to construct
     * a NSH header and associated overlay packet to target the first
     * service hop of a Rendered Service Path
     * <p>
     *
     * @param input RPC input including a Rendered Service Path name
     * @return RPC output including a renderedServicePathFirstHop.
     */
    @Override
    public Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> readRenderedServicePathFirstHop(
            ReadRenderedServicePathFirstHopInput input) {

        RenderedServicePathFirstHop renderedServicePathFirstHop = null;
        RpcResultBuilder<ReadRenderedServicePathFirstHopOutput> rpcResultBuilder;

        renderedServicePathFirstHop =
                SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(new RspName(input.getName()));

        ReadRenderedServicePathFirstHopOutput renderedServicePathFirstHopOutput = null;
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
     * <p>
     *
     * @param input RPC input including a ServiceFunctionTypeIdentity list
     * @return RPC output including a renderedServicePathFirstHop.
     */
    @Override
    public Future<RpcResult<ReadRspFirstHopBySftListOutput>> readRspFirstHopBySftList(
            ReadRspFirstHopBySftListInput input) {
        RenderedServicePathFirstHop renderedServicePathFirstHop = null;
        renderedServicePathFirstHop =
                SfcProviderRenderedPathAPI.readRspFirstHopBySftList(input.getSfst(), input.getSftList());
        ReadRspFirstHopBySftListOutput readRspFirstHopBySftListOutput = null;
        if (renderedServicePathFirstHop != null) {
            ReadRspFirstHopBySftListOutputBuilder readRspFirstHopBySftListOutputBuilder =
                    new ReadRspFirstHopBySftListOutputBuilder();
            readRspFirstHopBySftListOutputBuilder.setRenderedServicePathFirstHop(renderedServicePathFirstHop);
            readRspFirstHopBySftListOutput = readRspFirstHopBySftListOutputBuilder.build();
        }

        RpcResultBuilder<ReadRspFirstHopBySftListOutput> rpcResultBuilder =
                RpcResultBuilder.success(readRspFirstHopBySftListOutput);
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
        RpcResultBuilder<SetGenerationAlgorithmOutput> rpcResultBuilder =
                RpcResultBuilder.success(setGenerationAlgorithmOutputBuilder.build());

        return Futures.immediateFuture(rpcResultBuilder.build());
    }

}

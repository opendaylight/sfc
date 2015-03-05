/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.util.SfcSftMapper;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
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
 * This class holds all RPCs methods for SFC Provider.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since       2014-06-30
 */

public class SfcProviderRpc implements ServiceFunctionService,
        ServiceFunctionChainService, RenderedServicePathService {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcProviderRpc.class);
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

        if (dataBroker != null) {

            // Data PLane Locator
            List<SfDataPlaneLocator> sfDataPlaneLocatorList = input.getSfDataPlaneLocator();

            ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            ServiceFunction sf = sfbuilder.setName(input.getName()).setType(input.getType())
                    .setKey(sfkey).setIpMgmtAddress(input.getIpMgmtAddress())
                    .setSfDataPlaneLocator(sfDataPlaneLocatorList).build();

            InstanceIdentifier<ServiceFunction>  sfEntryIID =
                    InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sfEntryIID, sf);
            writeTx.commit();

        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
        }
        printTraceStop(LOG);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<ReadServiceFunctionOutput>> readServiceFunction(ReadServiceFunctionInput input) {
        printTraceStart(LOG);
        LOG.info("Input: " + input);

        if (dataBroker != null) {
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            InstanceIdentifier<ServiceFunction> sfIID;
            sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sfkey).toInstance();

            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunction> dataObject = null;
            try {
                dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.debug("Failed to readServiceFunction : {}",
                        e.getMessage());
            }
            if (dataObject instanceof ServiceFunction) {
                LOG.debug("readServiceFunction Success: {}",
                        ((ServiceFunction) dataObject).getName());
                ServiceFunction serviceFunction = (ServiceFunction) dataObject;
                ReadServiceFunctionOutput readServiceFunctionOutput = null;
                ReadServiceFunctionOutputBuilder outputBuilder = new ReadServiceFunctionOutputBuilder();
                outputBuilder.setName(serviceFunction.getName())
                        .setIpMgmtAddress(serviceFunction.getIpMgmtAddress())
                        .setType(serviceFunction.getType());
                readServiceFunctionOutput = outputBuilder.build();
                printTraceStop(LOG);
                return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>
                        getRpcResult(true, readServiceFunctionOutput, Collections.<RpcError>emptySet()));
            }
            printTraceStop(LOG);
            return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
            printTraceStop(LOG);
            return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        }
    }

    @Override
    public Future<RpcResult<InstantiateServiceFunctionChainOutput>> instantiateServiceFunctionChain(InstantiateServiceFunctionChainInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunctionChains(PutServiceFunctionChainsInput input) {
        printTraceStart(LOG);
        ServiceFunctionChainsBuilder builder = new ServiceFunctionChainsBuilder();
        builder = builder.setServiceFunctionChain(input.getServiceFunctionChain());
        ServiceFunctionChains sfcs = builder.build();

        if (dataBroker != null) {
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    OpendaylightSfc.SFC_IID, sfcs, true);
            writeTx.commit();
        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
        }
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    private ServiceFunctionChain findServiceFunctionChain(String name) {
        ServiceFunctionChainKey key = new ServiceFunctionChainKey(name);
        InstanceIdentifier<ServiceFunctionChain> iid =
                InstanceIdentifier.builder(ServiceFunctionChains.class)
                        .child(ServiceFunctionChain.class, key)
                        .toInstance();

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<ServiceFunctionChain> dataObject = null;
        try {
            dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, iid).get();
            if (dataObject != null) {
                return dataObject.get();
            } else {
                LOG.error("\nFailed to findServiceFunctionChain");
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("\nFailed to findServiceFunctionChain: {}", e.getMessage());
            return null;
        }
    }

    private List<ServicePathHop> findInstancesByType(Class<? extends ServiceFunctionTypeIdentity> sfType) {
        List<ServicePathHop> ret = new ArrayList<>();

        SfcSftMapper mapper = new SfcSftMapper(odlSfc);
        List<ServiceFunction> sfList = mapper.getSfList(sfType);
        short hopCount = 0;
        for(ServiceFunction sf : sfList){
            ServicePathHopBuilder builder = new ServicePathHopBuilder();
            ret.add(builder.setHopNumber(hopCount)
                    .setServiceFunctionName(sf.getName())
                    .setServiceFunctionForwarder(sf.getSfDataPlaneLocator()
                            .get(0).getServiceFunctionForwarder())
                    .build());
            hopCount++;
        }
        return ret;
    }

    private ServiceFunctionPaths buildServiceFunctionPaths(List<ServiceFunctionPath> list) {

        ServiceFunctionPathsBuilder builder = new ServiceFunctionPathsBuilder();
        builder.setServiceFunctionPath(list);
        return builder.build();
    }

    @Override
    public Future<RpcResult<CreateRenderedPathOutput>> createRenderedPath(CreateRenderedPathInput input) {

        ServiceFunctionPath createdServiceFunctionPath;
        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;
        CreateRenderedPathOutputBuilder createRenderedPathOutputBuilder = new CreateRenderedPathOutputBuilder();
        RpcResult <CreateRenderedPathOutput> rpcResult;
        boolean ret = false;
        createRenderedPathOutputBuilder.setResult(ret);

        createdServiceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPathExecutor(input.getParentServiceFunctionPath());

        if (createdServiceFunctionPath != null) {
            renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(createdServiceFunctionPath);
            if (renderedServicePath != null) {
                ret = true;
                createRenderedPathOutputBuilder.setResult(ret);
                rpcResult = RpcResultBuilder.success(createRenderedPathOutputBuilder.build()).build();

                if ((createdServiceFunctionPath.getClassifier() != null) &&
                        SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(createdServiceFunctionPath.getClassifier()) != null) {
                    SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                            (createdServiceFunctionPath.getClassifier(), renderedServicePath.getName());
                } else {
                    LOG.warn("Classifier not provided or does not exist");
                }

                if ((createdServiceFunctionPath.isSymmetric() != null) && createdServiceFunctionPath.isSymmetric()) {

                    revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
                    if (revRenderedServicePath == null) {
                        LOG.error("Failed to create symmetric service path: {}");
                    } else if ((createdServiceFunctionPath.getSymmetricClassifier() != null) &&
                            SfcProviderServiceClassifierAPI
                                    .readServiceClassifierExecutor(createdServiceFunctionPath.getSymmetricClassifier()) != null) {
                        SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                                (createdServiceFunctionPath.getSymmetricClassifier(), revRenderedServicePath.getName());

                    } else {
                        LOG.warn("Symmetric Classifier not provided or does not exist");
                    }
                }
            } else {
                rpcResult =  RpcResultBuilder.<CreateRenderedPathOutput>failed()
                        .withError(ErrorType.APPLICATION, "Failed to create RSP").build();
            }

        } else {
            rpcResult = RpcResultBuilder.<CreateRenderedPathOutput>failed()
                    .withError(ErrorType.APPLICATION, "Service Function Path does not exist").build();
        }
        return Futures.immediateFuture(rpcResult);
    }


    @Override
    public Future<RpcResult<DeleteRenderedPathOutput>> deleteRenderedPath(DeleteRenderedPathInput input) {


        boolean ret;

        ret = SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(input.getName());
        DeleteRenderedPathOutputBuilder deleteRenderedPathOutputBuilder = new DeleteRenderedPathOutputBuilder();
        deleteRenderedPathOutputBuilder.setResult(ret);
        RpcResultBuilder<DeleteRenderedPathOutput> rpcResultBuilder = RpcResultBuilder.success(deleteRenderedPathOutputBuilder.build());


        return Futures.immediateFuture(rpcResultBuilder.build());

    }

    /**
     * This method gets all necessary information for a system to construct
     * a NSH header and associated overlay packet to target the first
     * service hop of a Rendered Service Path
     * <p>
     * @param input RPC input including a Rendered Service Path name
     * @return RPC output including a renderedServicePathFirstHop.
     */
    @Override
    public Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> readRenderedServicePathFirstHop(ReadRenderedServicePathFirstHopInput input) {

        RenderedServicePathFirstHop renderedServicePathFirstHop = null;

        renderedServicePathFirstHop =
                            SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(input.getName());

        ReadRenderedServicePathFirstHopOutput output = null;
        if (renderedServicePathFirstHop != null) {
            ReadRenderedServicePathFirstHopOutputBuilder outputBuilder = new ReadRenderedServicePathFirstHopOutputBuilder();
            outputBuilder.setRenderedServicePathFirstHop(renderedServicePathFirstHop);
            output = outputBuilder.build();
        }
        return Futures.immediateFuture(Rpcs.<ReadRenderedServicePathFirstHopOutput>
                        getRpcResult(true, output, Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<TraceRenderedServicePathOutput>> traceRenderedServicePath(TraceRenderedServicePathInput input) {
        return null;
    }

}

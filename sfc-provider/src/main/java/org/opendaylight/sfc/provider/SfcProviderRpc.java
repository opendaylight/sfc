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
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.sfc.provider.util.SfcSftMapper;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.PutServiceNodeInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodeService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
        ServiceFunctionChainService, ServiceNodeService {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcProviderRpc.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();


    public static SfcProviderRpc getSfcProviderRpc() {
        return new SfcProviderRpc();
    }


    @Override
    public Future<RpcResult<Void>> putServiceFunction(PutServiceFunctionInput input) {
        printTraceStart(LOG);
        LOG.info("\n####### Input: " + input);

        if (odlSfc.dataProvider != null) {

            // Data PLane Locator
            List<SfDataPlaneLocator> sfDataPlaneLocatorList = input.getSfDataPlaneLocator();

            ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            ServiceFunction sf = sfbuilder.setName(input.getName()).setType(input.getType())
                    .setKey(sfkey).setIpMgmtAddress(input.getIpMgmtAddress())
                    .setSfDataPlaneLocator(sfDataPlaneLocatorList).build();

            InstanceIdentifier<ServiceFunction>  sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
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

        if (odlSfc.dataProvider != null) {
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            InstanceIdentifier<ServiceFunction> sfIID;
            sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sfkey).toInstance();

            ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
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
            LOG.warn("dataProvider is null");
            printTraceStop(LOG);
            return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        }
    }

    @Override
    public Future<RpcResult<Void>> deleteAllServiceFunction() {
        printTraceStart(LOG);
        if (odlSfc.dataProvider != null) {

            WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                    OpendaylightSfc.sfsIID);
            writeTx.commit();

        } else {
            LOG.warn("dataProvider is null");
        }
        printTraceStop(LOG);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> deleteServiceFunction(DeleteServiceFunctionInput input) {
        printTraceStart(LOG);
        LOG.info("Input: " + input);
        if (odlSfc.dataProvider != null) {

            ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
            Optional<ServiceFunctions> dataObject = null;
            try {
                dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.sfsIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to deleteServiceFunction");
                return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                        Collections.<RpcError>emptySet()));
            }
            if (dataObject instanceof ServiceFunctions) {

                ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
                InstanceIdentifier<ServiceFunction> sfIID;
                sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                        child(ServiceFunction.class, sfkey).toInstance();

                WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
                writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                        sfIID);
                writeTx.commit();
            }
        } else {
            LOG.warn("dataProvider is null");
        }
        printTraceStop(LOG);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunctionChains(PutServiceFunctionChainsInput input) {
        printTraceStart(LOG);
        ServiceFunctionChainsBuilder builder = new ServiceFunctionChainsBuilder();
        builder = builder.setServiceFunctionChain(input.getServiceFunctionChain());

        ServiceFunctionChains sfcs = builder.build();

        if (odlSfc.dataProvider != null) {
            WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    OpendaylightSfc.sfcIID, sfcs, true);
            writeTx.commit();

        } else {
            LOG.warn("dataProvider is null");
        }
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> putServiceNode(PutServiceNodeInput input) {
        printTraceStart(LOG);
        LOG.info("\n####### Input: " + input);

        if (odlSfc.dataProvider != null) {
            ServiceNodeKey snKey = new ServiceNodeKey(input.getName());
            ServiceNodeBuilder builder = new ServiceNodeBuilder();
            ServiceNode sn = builder.setKey(snKey)
                    .setName(input.getName())
                    .setIpMgmtAddress(input.getIpMgmtAddress())
                    .setServiceFunction(input.getServiceFunction())
                    .build();

            InstanceIdentifier<ServiceNode> snEntryIID = InstanceIdentifier.builder(ServiceNodes.class).
                    child(ServiceNode.class, sn.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    snEntryIID, sn, true);
            writeTx.commit();
        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
        }
        printTraceStop(LOG);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<InstantiateServiceFunctionChainOutput>> instantiateServiceFunctionChain(InstantiateServiceFunctionChainInput input) {
        if (odlSfc.dataProvider != null) {
            ServiceFunctionChain chain = findServiceFunctionChain(input.getName());

            if (chain != null) {
                List<SfcServiceFunction> sfRefList = chain.getSfcServiceFunction();
                LOG.debug("\n********** sfRefList ***********\n" + sfRefList);
                if (sfRefList != null && sfRefList.size() > 0) {

                    ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
                    List<ServicePathHop> instances = new ArrayList<>();

                    Random rand = new Random(); // temporarily
                    for (SfcServiceFunction ref : sfRefList) {
                        List<ServicePathHop> instanceList = findInstancesByType(ref.getType());
                        LOG.debug("\n********** instanceList ***********\n" +
                                instanceList);
                        if (instanceList != null && instanceList.size() > 0) {
                            // select instance
                            // for now, takes an element randomly
                            instances.add(instanceList.get(rand.nextInt(instanceList.size())));
                        } else {
                            throw new IllegalStateException("No instances found for Service Function \"" + ref.getName() + "\"");
                        }
                    }
                    String pathName = input.getName() + "-" + java.lang.System.currentTimeMillis();
                    ServiceFunctionPath path = pathBuilder.setName(pathName)
                            .setServicePathHop(instances)
                            .setServiceChainName(input.getName())
                            .build();
                    List<ServiceFunctionPath> list = new ArrayList<>();
                    list.add(path);

                    ServiceFunctionPaths paths = buildServiceFunctionPaths(list);

                    WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
                    writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                            OpendaylightSfc.sfpIID, paths, true);
                    writeTx.commit();
                    InstantiateServiceFunctionChainOutputBuilder outputBuilder = new InstantiateServiceFunctionChainOutputBuilder();
                    outputBuilder.setName(pathName);
                    return Futures.immediateFuture(Rpcs.getRpcResult(true,
                            outputBuilder.build(),
                            Collections.<RpcError>emptySet()));

                } else {
                    throw new IllegalStateException("Service function chain's SF list is null or empty.");
                }
            } else {
                throw new IllegalStateException("Service function chain \"" + input.getName() + "\" not found.");
            }

        } else {
            LOG.warn("\n####### dataProvider is null");
        }
        return Futures.immediateFuture(Rpcs.<InstantiateServiceFunctionChainOutput>getRpcResult(false,
                Collections.<RpcError>emptySet()));
    }

    private ServiceFunctionChain findServiceFunctionChain(String name) {
        ServiceFunctionChainKey key = new ServiceFunctionChainKey(name);
        InstanceIdentifier<ServiceFunctionChain> iid =
                InstanceIdentifier.builder(ServiceFunctionChains.class)
                        .child(ServiceFunctionChain.class, key)
                        .toInstance();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
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

    // TODO this is duplicated in SFCSftMapper (and used only there, not here; better to DRY
    private ServiceFunction findServiceFunction(String name) {
        ServiceFunctionKey key = new ServiceFunctionKey(name);
        InstanceIdentifier<ServiceFunction> iid =
                InstanceIdentifier.builder(ServiceFunctions.class)
                        .child(ServiceFunction.class, key)
                        .toInstance();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunction> dataObject = null;
        try {
            dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, iid).get();
            if (dataObject != null) {
                return dataObject.get();
            } else {
                LOG.error("\nFailed to findServiceFunction");
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("\nFailed to findServiceFunction: {}", e.getMessage());
            return null;
        }
    }

    private List<ServicePathHop> findInstancesByType(String sfType) {
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

}

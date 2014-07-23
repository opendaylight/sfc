/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.PutServiceFunctionForwarderInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarderService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.PutServiceNodeInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodeService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
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


/**
 * This class holds all RPCs methods for SFC Provider.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov ()
 * @version 0.1
 * @since       2014-06-30
 */

public class SfcProviderRpc implements ServiceFunctionService,
        ServiceFunctionChainService, ServiceNodeService, ServiceFunctionForwarderService {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcProviderRpc.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private SfcProviderRpc sfcRpcObj;

    /*
    public SfcProviderRpc () {
        this.sfcRpcObj = this;
    }
    */

    public static SfcProviderRpc getSfcProviderRpc() {
        return new SfcProviderRpc();
    }


    private ServiceFunctions buildServiceFunctions(List<ServiceFunction> list) {

        ServiceFunctionsBuilder builder = new ServiceFunctionsBuilder();
        builder.setServiceFunction(list);
        return builder.build();
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunction(PutServiceFunctionInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        LOG.info("\n####### Input: " + input);

        if (odlSfc.dataProvider != null) {

            // Data PLane Locator
            SfDataPlaneLocator sfDataPlaneLocator = input.getSfDataPlaneLocator();
            SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
            sfDataPlaneLocatorBuilder = sfDataPlaneLocatorBuilder.setLocatorType(sfDataPlaneLocator.getLocatorType());

            ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            ServiceFunction sf = sfbuilder.setName(input.getName()).setType(input.getType())
                    .setKey(sfkey).setIpMgmtAddress(input.getIpMgmtAddress())
                    .setSfDataPlaneLocator(sfDataPlaneLocatorBuilder.build())
                    .setServiceFunctionForwarder(input.getServiceFunctionForwarder())
                    .build();

            InstanceIdentifier<ServiceFunction>  sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();

            t.putConfigurationData(sfEntryIID, sf);
            try {
                t.commit().get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.warn("Failed to update-function, operational otherwise", e);
            }

        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<ReadServiceFunctionOutput>> readServiceFunction(ReadServiceFunctionInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        LOG.info("Input: " + input);

        if (odlSfc.dataProvider != null) {
            String name = input.getName();
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            InstanceIdentifier<ServiceFunction> sfIID;
            sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sfkey).toInstance();
            DataObject dataObject = odlSfc.dataProvider.readConfigurationData(sfIID);
            if (dataObject instanceof ServiceFunction) {
                LOG.info("readServiceFunction Success: {}", ((ServiceFunction) dataObject).getName());
                ServiceFunction serviceFunction = (ServiceFunction) dataObject;
                ReadServiceFunctionOutput readServiceFunctionOutput = null;
                ReadServiceFunctionOutputBuilder outputBuilder = new ReadServiceFunctionOutputBuilder();
                outputBuilder.setName(serviceFunction.getName())
                        .setIpMgmtAddress(serviceFunction.getIpMgmtAddress())
                        .setType(serviceFunction.getType());
                readServiceFunctionOutput = outputBuilder.build();
                LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
                return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>
                        getRpcResult(true, readServiceFunctionOutput, Collections.<RpcError>emptySet()));
            }
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        } else {
            LOG.warn("dataProvider is null");
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        }
    }

    @Override
    public Future<RpcResult<Void>> deleteAllServiceFunction() {
        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.dataProvider != null) {
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();
            t.removeConfigurationData(OpendaylightSfc.sfsIID);
            try {
                t.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("deleteServiceFunction failed", e);
            }
        } else {
            LOG.warn("dataProvider is null");
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> deleteServiceFunction(DeleteServiceFunctionInput input) {
        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        LOG.info("Input: " + input);
        if (odlSfc.dataProvider != null) {
            DataObject dataObject = odlSfc.dataProvider.readConfigurationData(OpendaylightSfc.sfsIID);
            if (dataObject instanceof ServiceFunctions) {

                ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
                InstanceIdentifier<ServiceFunction> sfIID;
                sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                        child(ServiceFunction.class, sfkey).toInstance();

                final DataModificationTransaction t = odlSfc.dataProvider
                        .beginTransaction();
                t.removeConfigurationData(sfIID);
                try {
                    t.commit().get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("deleteServiceFunction failed", e);
                }
            }
        } else {
            LOG.warn("dataProvider is null");
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunctionChains(PutServiceFunctionChainsInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionChainsBuilder builder = new ServiceFunctionChainsBuilder();
        builder = builder.setServiceFunctionChain(input.getServiceFunctionChain());

        ServiceFunctionChains sfcs = builder.build();

        if (odlSfc.dataProvider != null) {
            final DataModificationTransaction t = odlSfc.dataProvider.beginTransaction();
            t.putConfigurationData(OpendaylightSfc.sfcIID, sfcs);

            try {
                t.commit().get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.warn("putServiceFunctionChain failed, operational otherwise", e);
            }
        } else {
            LOG.warn("dataProvider is null");
        }
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> putServiceNode(PutServiceNodeInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
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
        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();

        t.putConfigurationData(snEntryIID, sn);
        try {
            t.commit().get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("\n########## {} failed, operational otherwise", Thread.currentThread().getStackTrace()[1], e);
        }
        } else {
            LOG.warn("\n####### Data Provider is null : {}", Thread.currentThread().getStackTrace()[1]);
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunctionForwarder(PutServiceFunctionForwarderInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        LOG.info("\n####### Input: " + input);

        if (odlSfc.dataProvider != null) {
            ServiceFunctionForwarderKey sffKey = new ServiceFunctionForwarderKey(input.getName());
            ServiceFunctionForwarderBuilder builder = new ServiceFunctionForwarderBuilder();
            ServiceFunctionForwarder sff = builder.setKey(sffKey)
                    .setName(input.getName())
                    .setPathId(input.getPathId())
                    .setServiceFunctionDictionary(input.getServiceFunctionDictionary())
                    .setSffDataPlaneLocator(input.getSffDataPlaneLocator())
                    .setTransport(input.getTransport())
                    .build();

            InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).
                    child(ServiceFunctionForwarder.class, sff.getKey()).toInstance();
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();

            t.putConfigurationData(sffEntryIID, sff);
            try {
                t.commit().get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.warn("\n########## {} failed, operational otherwise", Thread.currentThread().getStackTrace()[1], e);
            }
        } else {
            LOG.warn("\n####### Data Provider is null : {}", Thread.currentThread().getStackTrace()[1]);
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<InstantiateServiceFunctionChainOutput>> instantiateServiceFunctionChain(InstantiateServiceFunctionChainInput input) {
        if (odlSfc.dataProvider != null) {
            ServiceFunctionChain chain = findServiceFunctionChain(input.getName());

            if (chain != null) {
                List<SfcServiceFunction> sfRefList = chain.getSfcServiceFunction();
                LOG.info("\n********** sfRefList ***********\n" + sfRefList);
                if (sfRefList != null && sfRefList.size() > 0) {

                    ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
                    List<SfpServiceFunction> instances = new ArrayList<>();

                    Random rand = new Random(); // temporarily
                    for (SfcServiceFunction ref : sfRefList) {
                        List<SfpServiceFunction> instanceList = findInstancesByType(ref.getType());
                        LOG.info("\n********** instanceList ***********\n" + instanceList);
                        if (instanceList != null && instanceList.size() > 0) {
                            // select instance
                            // for now, takes an element randomly
                            instances.add(instanceList.get(rand.nextInt(instanceList.size())));
                        } else {
                            throw new IllegalStateException("No instances found for Service Function \"" + ref.getName() + "\"");
                        }
                    }
                    LOG.info("\n********** instances ***********\n" + instances);
                    String pathName = input.getName() + "-" + java.lang.System.currentTimeMillis();
                    ServiceFunctionPath path = pathBuilder.setName(pathName)
                            .setSfpServiceFunction(instances)
                            .setServiceChainName(input.getName())
                            .build();
                    List<ServiceFunctionPath> list = new ArrayList<>();
                    list.add(path);

                    ServiceFunctionPaths paths = buildServiceFunctionPaths(list);
                    final DataModificationTransaction t = odlSfc.dataProvider.beginTransaction();
                    t.putConfigurationData(OpendaylightSfc.sfpIID, paths);

                    try {
                        t.commit().get();
                        InstantiateServiceFunctionChainOutputBuilder outputBuilder = new InstantiateServiceFunctionChainOutputBuilder();
                        outputBuilder.setName(pathName);
                        return Futures.immediateFuture(Rpcs.getRpcResult(true,
                                outputBuilder.build(),
                                Collections.<RpcError>emptySet()));
                    } catch (ExecutionException | InterruptedException e) {
                        LOG.warn("\n####### instantiateServiceFunctionChain failed, operational otherwise", e);
                    }
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
        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(iid);
        if (dataObject instanceof ServiceFunctionChain) {
            return (ServiceFunctionChain) dataObject;
        } else {
            throw new IllegalStateException("Wrong dataObject instance (expected ServiceFunctionChain).");
        }
    }

    // TODO this is duplicated in SFCSftMapper (and used only there, not here; better to DRY
    private ServiceFunction findServiceFunction(String name) {
        ServiceFunctionKey key = new ServiceFunctionKey(name);
        InstanceIdentifier<ServiceFunction> iid =
                InstanceIdentifier.builder(ServiceFunctions.class)
                        .child(ServiceFunction.class, key)
                        .toInstance();
        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(iid);
        if (dataObject instanceof ServiceFunction) {
            return (ServiceFunction) dataObject;
        } else {
            throw new IllegalStateException("Wrong dataObject instance (expected ServiceFunction).");
        }
    }

    private List<SfpServiceFunction> findInstancesByType(String sfType) {
        List<SfpServiceFunction> ret = new ArrayList<>();

        SfcSftMapper mapper = new SfcSftMapper(odlSfc);
        List<ServiceFunction> sfList = mapper.getSfList(sfType);
        for(ServiceFunction sf : sfList){
            SfpServiceFunctionBuilder builder = new SfpServiceFunctionBuilder();
            ret.add(builder.setName(sf.getName())
                    .setServiceFunctionForwarder(sf.getServiceFunctionForwarder())
                    .build());
        }
        return ret;
    }

    private ServiceFunctionPaths buildServiceFunctionPaths(List<ServiceFunctionPath> list) {

        ServiceFunctionPathsBuilder builder = new ServiceFunctionPathsBuilder();
        builder.setServiceFunctionPath(list);
        return builder.build();
    }

}
